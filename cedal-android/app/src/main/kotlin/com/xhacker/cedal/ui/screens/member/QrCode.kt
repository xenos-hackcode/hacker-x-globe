package com.xhacker.cedal.ui.screens.member

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.xhacker.cedal.ui.theme.CedalColors

// The URI scheme every Cedal QR add-code uses - lets a scan tell "this is a
// genuine Cedal friend code" apart from some unrelated QR code someone
// points the scanner at by accident. See FriendService.findByPublicId
// server-side (the part after the prefix is just Users.publicId).
const val CEDAL_ADD_URI_PREFIX = "cedal://add/"

// ✚ > QR > My Code - renders the signed-in user's own publicId as a QR
// bitmap with a small round Cedal "C" mark punched into the center, the
// same idea as WhatsApp/Telegram stamping their own logo over their QR
// codes. High error-correction is required for this, not just nicer -
// without it, obscuring the center with the logo would make real scanners
// fail to decode the surrounding data.
fun generateCedalQrBitmap(publicId: String, sizePx: Int): Bitmap {
    val content = CEDAL_ADD_URI_PREFIX + publicId
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }

    val canvas = Canvas(bitmap)
    val logoRadius = sizePx * 0.14f
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // White circle backing (so the "C" reads clearly regardless of which
    // QR modules would've landed underneath it) plus a cyan ring matching
    // the app's accent color, then the mark itself.
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    canvas.drawCircle(cx, cy, logoRadius, circlePaint)
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CedalColors.AccentCyan.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.012f
    }
    canvas.drawCircle(cx, cy, logoRadius, ringPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CedalColors.AccentCyan.toArgb()
        textSize = logoRadius * 1.3f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText("C", cx, textY, textPaint)

    return bitmap
}
