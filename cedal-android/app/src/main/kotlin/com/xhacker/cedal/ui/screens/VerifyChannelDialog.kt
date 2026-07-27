package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton

// Shared "sms or email" choice popup - used both at signup (which channel
// delivers the account-activation key) and on Forgot Password (which
// channel delivers the reset key). Whichever is picked, the same key value
// goes out through just that one channel - see AuthService.signup /
// AuthService.forgotPassword server-side.
@Composable
fun VerifyChannelDialog(title: String, onDismiss: () -> Unit, onChoose: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.CardBackground)
                .padding(20.dp),
        ) {
            Text(title, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Choose where your verification key goes.",
                color = CedalColors.TextSecondary, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            CedalPrimaryButton(text = "EMAIL", onClick = { onChoose("email") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            CedalGhostButton(text = "SMS", onClick = { onChoose("sms") }, modifier = Modifier.fillMaxWidth())
        }
    }
}
