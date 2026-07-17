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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ARC's Labs tab - a real local-network scanner (unchanged logic from the
// original build), reskinned as a military-radar-style HUD "the one you see
// in movies" per the request - sweeping green radar, target blips, terminal
// log. Still strictly scoped to the phone's own connected Wi-Fi subnet only.

private data class ScanResult(val ip: String, val elapsedMs: Long)

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

@Composable
fun ArcLabsBody(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<ScanResult>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val networkInfo = remember { localIpAndSubnetPrefix(context) }

    fun startScan() {
        val prefix = networkInfo?.second
        if (prefix == null) {
            errorMessage = "Couldn't read your Wi-Fi network - make sure Wi-Fi is on and connected, then try again."
            return
        }
        errorMessage = null
        results = emptyList()
        scanning = true
        scope.launch {
            scanSubnet(prefix) { found -> results = (results + found).sortedBy { it.ip } }
            scanning = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MemberBackBar(title = "ARC", onBack = onBack)
        Text("LABS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "A real local-network scanner - scoped only to the Wi-Fi network your phone is already connected to. Never point network scanning at a network you don't own or don't have written permission to test (see Learn).",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
        )

        RadarDisplay(results = results, scanning = scanning, modifier = Modifier.padding(bottom = 14.dp))

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
                    networkInfo?.first?.let { "Your device: $it" } ?: "Not connected to Wi-Fi",
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
            if (results.isEmpty()) "NO CONTACTS LOGGED" else "${results.size} CONTACT${if (results.size == 1) "" else "S"} DETECTED",
            color = CedalColors.Success, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        results.forEach { r ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .border(1.dp, CedalColors.Success.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(12.dp),
            ) {
                Text(
                    "TARGET ACQUIRED · ${r.ip}",
                    color = CedalColors.Success, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text("${r.elapsedMs}ms", color = CedalColors.Success.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun RadarDisplay(results: List<ScanResult>, scanning: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar-sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "sweep-angle",
    )

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
                val angle = (result.ip.hashCode().mod(360)).toDouble()
                val distanceFraction = (0.3f + (result.elapsedMs % 50) / 100f).coerceIn(0.25f, 0.9f)
                val rad = Math.toRadians(angle)
                val bx = center.x + radius * distanceFraction * cos(rad).toFloat()
                val by = center.y + radius * distanceFraction * sin(rad).toFloat()
                drawCircle(color = green, radius = 6f, center = Offset(bx, by))
                drawCircle(color = green.copy(alpha = 0.3f), radius = 12f, center = Offset(bx, by))
            }
        }
        Text(
            if (scanning) "SCANNING…" else if (results.isEmpty()) "STANDBY" else "${results.size} CONTACT${if (results.size == 1) "" else "S"}",
            color = Color(0xFF33FF66), fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        )
    }
}
