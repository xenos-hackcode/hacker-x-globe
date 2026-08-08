package com.xhacker.cedal.ui.screens.member

import android.content.Context
import android.content.Intent
import android.net.Uri

// "Known" calling - opens the device's own dialer pre-filled with the
// number, same as tapping a phone number link anywhere else on Android.
// ACTION_DIAL (not ACTION_CALL) deliberately - it needs no CALL_PHONE
// runtime permission and leaves the actual "send call" tap to the user,
// which is both simpler and safer than this app placing calls on its own.
fun launchDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    context.startActivity(intent)
}
