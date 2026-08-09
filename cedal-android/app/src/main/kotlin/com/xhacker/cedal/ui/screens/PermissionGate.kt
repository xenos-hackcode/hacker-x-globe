package com.xhacker.cedal.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton

// Every runtime-permission request in this app (mic for voice notes, camera,
// contacts, notifications) used to just do nothing on denial - the
// permission launcher's callback only ever handled the granted branch (see
// e.g. the old micPermissionLauncher in MemberChatThreadScreen.kt: `{ granted
// -> if (granted) voiceRecorderActive = true }`). A normal "no" is fine to
// leave silent (the user can just try again), but a "Don't ask again" denial
// means every future request silently no-ops forever with zero feedback -
// looks exactly like a broken feature. Android gives no direct API for
// "was this a permanent decline" - the standard trick is checking
// shouldShowRequestPermissionRationale AFTER a denial: it's still true after
// an ordinary "no" (system will ask again later) but flips to false once
// "Don't ask again" was checked (or a device policy blocks it outright) -
// same false value a never-yet-asked permission also has, which is why this
// can only be read meaningfully inside the result callback, not before
// launching.
private fun permissionLabel(permission: String) = when (permission) {
    android.Manifest.permission.RECORD_AUDIO -> "microphone"
    android.Manifest.permission.CAMERA -> "camera"
    android.Manifest.permission.READ_CONTACTS -> "contacts"
    android.Manifest.permission.POST_NOTIFICATIONS -> "notifications"
    else -> "this"
}

@Stable
class PermissionGateState internal constructor(
    private val launcher: ActivityResultLauncher<String>,
    private val activity: Activity?,
) {
    var blockedPermission by mutableStateOf<String?>(null)
        private set
    private var pendingPermission: String? = null
    private var pendingOnGranted: (() -> Unit)? = null

    // Skips the launcher entirely when already granted, same short-circuit
    // every call site already did by hand before this existed.
    fun request(context: android.content.Context, permission: String, onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
            return
        }
        pendingPermission = permission
        pendingOnGranted = onGranted
        launcher.launch(permission)
    }

    internal fun onResult(granted: Boolean) {
        val permission = pendingPermission
        if (granted) {
            pendingOnGranted?.invoke()
        } else if (permission != null && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            blockedPermission = permission
        }
        pendingPermission = null
        pendingOnGranted = null
    }

    fun dismissBlockedDialog() { blockedPermission = null }
}

@Composable
fun rememberPermissionGate(): PermissionGateState {
    val activity = LocalContext.current as? Activity
    var state: PermissionGateState? = null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state?.onResult(granted)
    }
    return remember { PermissionGateState(launcher, activity).also { state = it } }
}

// Call once per screen that uses a PermissionGateState, anywhere in the
// composable body - only actually renders when a request just came back
// permanently denied.
@Composable
fun PermissionGateState.PermissionBlockedDialog() {
    val permission = blockedPermission ?: return
    val context = LocalContext.current
    androidx.compose.ui.window.Dialog(onDismissRequest = { dismissBlockedDialog() }) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .padding(20.dp),
        ) {
            Text("Permission needed", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                "Because \"Don't ask again\" was chosen for ${permissionLabel(permission)} access, Cedal can't ask for it again. To use this feature, go to your phone's Settings and allow it manually.",
                color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
            )
            CedalPrimaryButton(
                text = "OPEN SETTINGS",
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                    dismissBlockedDialog()
                },
            )
            CedalGhostButton(text = "CANCEL", onClick = { dismissBlockedDialog() })
        }
    }
}
