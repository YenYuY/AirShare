package com.yenyuyyy.airshare.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun rememberQrCode(content: String, sizePixels: Int = 720): ImageBitmap = remember(content, sizePixels) {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        sizePixels,
        sizePixels,
        mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    val pixels = IntArray(sizePixels * sizePixels)
    for (y in 0 until sizePixels) {
        val rowOffset = y * sizePixels
        for (x in 0 until sizePixels) {
            pixels[rowOffset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    createBitmap(sizePixels, sizePixels).apply {
        setPixels(pixels, 0, sizePixels, 0, 0, sizePixels, sizePixels)
    }.asImageBitmap()
}
