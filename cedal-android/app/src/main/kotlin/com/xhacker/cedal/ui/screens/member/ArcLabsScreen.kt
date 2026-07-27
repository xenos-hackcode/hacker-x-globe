package com.xhacker.cedal.ui.screens.member

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ARC's Labs tab - a real local-network scanner reskinned as a military-radar
// HUD. Still strictly scoped to the phone's own connected Wi-Fi subnet only.

private data class ScanResult(val ip: String, val elapsedMs: Long)

// Common ports mapped to a plain-English guess about what's likely running
// there - real OS/service fingerprinting (a genuine, teachable skill), not
// a brand/model/serial lookup. Nothing here can identify a device's exact
// make, serial number, or manufacture date - that data is never broadcast
// over the network by any device, and Android itself has blocked apps from
// reading other devices' MAC addresses (the one thing that could even guess
// a hardware vendor) since Android 10 - so this deliberately doesn't
// pretend to offer that.
private val PORT_SIGNATURES = listOf(
    22 to "SSH open - likely Linux/Mac/a network device",
    23 to "Telnet open - likely an older router or IoT device",
    80 to "Web interface open (port 80)",
    443 to "Web interface open (port 443, HTTPS)",
    445 to "Windows file sharing open (SMB) - likely Windows",
    3389 to "Remote Desktop open - likely Windows",
    5555 to "ADB open - an Android device with USB/network debugging on",
    9100 to "Printer port open - likely a network printer",
    62078 to "Apple sync port open - likely an iPhone/iPad",
)

private data class DeviceInfo(val hostname: String?, val signatures: List<String>, val loading: Boolean = false)

private fun localIpAndSubnetPrefix(context: Context): Pair<String, String>? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val ipInt = wifiManager.connectionInfo?.ipAddress ?: return null
    if (ipInt == 0) return null
    val bytes = intArrayOf(
        ipInt and 0xff,
        (ipInt shr 8) and 0xff,
        (ipInt shr 16) and 0xff,
        (ipInt shr 24) and 0xff,
    )
    val ip = bytes.joinToString(".")
    val prefix = "${bytes[0]}.${bytes[1]}.${bytes[2]}."
    return ip to prefix
}

// A ping sweep, not a port scanner - InetAddress.isReachable() is the
// standard no-root way to ask "is anything at this address answering right
// now", same technique consumer LAN-scanner apps (Fing etc.) use. Scoped
// entirely to the phone's own connected Wi-Fi subnet - there is no field to
// type in an arbitrary target.
private suspend fun scanSubnet(prefix: String, onHostFound: suspend (ScanResult) -> Unit) = withContext(Dispatchers.IO) {
    // onHostFound gets called concurrently from up to 254 in-flight checks -
    // the Mutex serializes those calls so a caller doing a
    // read-current-list/append/write-back against Compose state never loses
    // an entry to two hosts resolving in the same instant.
    val updateLock = Mutex()
    val jobs = (1..254).map { host ->
        async {
            val ip = "$prefix$host"
            val start = System.currentTimeMillis()
            try {
                if (InetAddress.getByName(ip).isReachable(400)) {
                    val result = ScanResult(ip, System.currentTimeMillis() - start)
                    updateLock.withLock { onHostFound(result) }
                }
            } catch (_: Exception) {
                // Unreachable/host lookup failure just means nothing's there - not an error worth surfacing.
            }
        }
    }
    jobs.awaitAll()
}

// "More Info" - a best-effort reverse-DNS hostname lookup plus a quick
// probe of a handful of well-known ports (see PORT_SIGNATURES). Neither of
// these is guaranteed to return anything - most consumer devices don't
// answer either - but when they do, it's real information, not a guess
// dressed up as fact.
private suspend fun probeDevice(ip: String): DeviceInfo = withContext(Dispatchers.IO) {
    val hostname = try {
        InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
    } catch (_: Exception) {
        null
    }
    val openSignatures = PORT_SIGNATURES.map { (port, label) ->
        async {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 200)
                    label
                }
            } catch (_: Exception) {
                null
            }
        }
    }.awaitAll().filterNotNull()
    DeviceInfo(hostname, openSignatures)
}

@Composable
fun ArcLabsBody(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<ScanResult>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // The device currently "locked on" via tapping a row/blip - drives the
    // COD-style tracking reticle on the radar and which row's info panel
    // (if opened) shows.
    var focusedIp by remember { mutableStateOf<String?>(null) }
    var deviceInfoByIp by remember { mutableStateOf<Map<String, DeviceInfo>>(emptyMap()) }

    val networkInfo = remember { localIpAndSubnetPrefix(context) }
    // The phone running the scan will always answer its own ping instantly -
    // that's not a "found" device, just noise, so it's filtered out before
    // it ever reaches the UI.
    val visibleResults = results.filter { it.ip != networkInfo?.first }

    fun startScan() {
        val prefix = networkInfo?.second
        if (prefix == null) {
            errorMessage = "Couldn't read your Wi-Fi network - make sure Wi-Fi is on and connected, then try again."
            return
        }
        errorMessage = null
        results = emptyList()
        focusedIp = null
        deviceInfoByIp = emptyMap()
        scanning = true
        scope.launch {
            scanSubnet(prefix) { found -> results = (results + found).sortedBy { it.ip } }
            scanning = false
        }
    }

    fun toggleFocus(ip: String) {
        focusedIp = if (focusedIp == ip) null else ip
    }

    fun loadDeviceInfo(ip: String) {
        if (deviceInfoByIp[ip] != null) return
        deviceInfoByIp = deviceInfoByIp + (ip to DeviceInfo(null, emptyList(), loading = true))
        scope.launch {
            val info = probeDevice(ip)
            deviceInfoByIp = deviceInfoByIp + (ip to info)
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MemberBackBar(title = "ARC", onBack = onBack)
        Text("LABS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "A real local-network scanner - scoped only to the Wi-Fi network your phone is already connected to. Never point network scanning at a network you don't own or don't have written permission to test (see Learn).",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
        )

        RadarDisplay(results = visibleResults, scanning = scanning, focusedIp = focusedIp, modifier = Modifier.padding(bottom = 14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Column {
                Text("CONNECTED NETWORK", color = CedalColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(
                    networkInfo?.first?.let { "Your device: $it (excluded from results below)" } ?: "Not connected to Wi-Fi",
                    color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                )
                if (networkInfo != null) {
                    Text(
                        "Scan range: ${networkInfo.second}1 – ${networkInfo.second}254",
                        color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        CedalPrimaryButton(
            text = if (scanning) "SCANNING…" else "BEGIN SWEEP",
            enabled = !scanning && networkInfo != null,
            onClick = { startScan() },
            modifier = Modifier.padding(bottom = 12.dp),
        )
        errorMessage?.let {
            Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
        }

        Text(
            if (visibleResults.isEmpty()) "NO CONTACTS LOGGED" else "${visibleResults.size} CONTACT${if (visibleResults.size == 1) "" else "S"} DETECTED - tap one to lock on, then \"More Info\" for a real (best-effort) ID",
            color = CedalColors.Success, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        visibleResults.forEach { r ->
            val isFocused = r.ip == focusedIp
            val info = deviceInfoByIp[r.ip]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .border(1.dp, if (isFocused) CedalColors.Success else CedalColors.Success.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { toggleFocus(r.ip) }
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${if (isFocused) "◎ LOCKED ON" else "TARGET ACQUIRED"} · ${r.ip}",
                        color = CedalColors.Success, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${r.elapsedMs}ms", color = CedalColors.Success.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                if (isFocused) {
                    Text(
                        if (info == null) "More Info ▾" else "Hide Info ▴",
                        color = CedalColors.AccentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                if (info == null) loadDeviceInfo(r.ip) else deviceInfoByIp = deviceInfoByIp - r.ip
                            },
                    )
                }
                if (info != null) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (info.loading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = CedalColors.Success, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                                Text("Probing…", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            Text(
                                "Hostname: ${info.hostname ?: "not advertised"}",
                                color = CedalColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            )
                            if (info.signatures.isEmpty()) {
                                Text(
                                    "No common ports responded - can't guess what this device is.",
                                    color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                                )
                            } else {
                                info.signatures.forEach { sig ->
                                    Text("• $sig", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                            Text(
                                "Exact brand, model, serial number, and manufacture date aren't retrievable over the network from another device - no protocol broadcasts that, and Android blocks apps from reading other devices' MAC addresses.",
                                color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarDisplay(results: List<ScanResult>, scanning: Boolean, focusedIp: String?, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar-sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "sweep-angle",
    )
    // A separate, faster rotation just for the focused-target reticle's
    // corner brackets - the "call of duty equipment tracker" look, spinning
    // independently of the sweep so it visually reads as "actively tracking"
    // rather than just another radar element.
    val reticleSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "reticle-spin",
    )

    fun blipPosition(ip: String, elapsedMs: Long, radius: Float, center: Offset): Offset {
        val angle = (ip.hashCode().mod(360)).toDouble()
        val distanceFraction = (0.3f + (elapsedMs % 50) / 100f).coerceIn(0.25f, 0.9f)
        val rad = Math.toRadians(angle)
        return Offset(center.x + radius * distanceFraction * cos(rad).toFloat(), center.y + radius * distanceFraction * sin(rad).toFloat())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(2.dp, CedalColors.Success.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val green = Color(0xFF33FF66)

            // Concentric rings + crosshairs, classic radar-scope look.
            for (ring in 1..4) {
                drawCircle(color = green.copy(alpha = 0.25f), radius = radius * ring / 4f, center = center, style = Stroke(width = 1.5f))
            }
            drawLine(green.copy(alpha = 0.25f), Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.5f)
            drawLine(green.copy(alpha = 0.25f), Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1.5f)

            // Rotating sweep wedge, if a scan is active.
            if (scanning) {
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, green.copy(alpha = 0.35f), Color.Transparent)),
                    startAngle = sweepAngle - 40f,
                    sweepAngle = 40f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                )
                val tipX = center.x + radius * cos(sweepRad).toFloat()
                val tipY = center.y + radius * sin(sweepRad).toFloat()
                drawLine(green, center, Offset(tipX, tipY), strokeWidth = 2f)
            }

            // Each found host gets a stable-ish blip position, derived from a
            // hash of its IP (angle) and response time (distance from
            // center) - not meaningful geometry, purely a radar-style visual.
            results.forEach { result ->
                val (bx, by) = blipPosition(result.ip, result.elapsedMs, radius, center)
                val isFocused = result.ip == focusedIp
                drawCircle(color = green, radius = if (isFocused) 8f else 6f, center = Offset(bx, by))
                drawCircle(color = green.copy(alpha = 0.3f), radius = if (isFocused) 16f else 12f, center = Offset(bx, by))
            }

            // The "locked on" tracker - a rotating bracket reticle (COD-style
            // target lock) drawn around whichever blip is currently focused.
            val focused = results.firstOrNull { it.ip == focusedIp }
            if (focused != null) {
                val (fx, fy) = blipPosition(focused.ip, focused.elapsedMs, radius, center)
                val bracketRadius = 22f
                val spinRad = Math.toRadians(reticleSpin.toDouble())
                for (cornerIndex in 0 until 4) {
                    val cornerAngle = spinRad + Math.toRadians((cornerIndex * 90).toDouble())
                    val cx = fx + bracketRadius * cos(cornerAngle).toFloat()
                    val cy = fy + bracketRadius * sin(cornerAngle).toFloat()
                    drawCircle(color = Color(0xFFFFAA00), radius = 3f, center = Offset(cx, cy))
                }
                drawCircle(color = Color(0xFFFFAA00), radius = bracketRadius, center = Offset(fx, fy), style = Stroke(width = 1.5f))
            }
        }
        Text(
            when {
                scanning -> "SCANNING…"
                focusedIp != null -> "LOCKED: $focusedIp"
                results.isEmpty() -> "STANDBY"
                else -> "${results.size} CONTACT${if (results.size == 1) "" else "S"}"
            },
            color = if (focusedIp != null) Color(0xFFFFAA00) else Color(0xFF33FF66), fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        )
    }
}
