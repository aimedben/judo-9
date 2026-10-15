package com.example.util

import android.content.Context
import android.graphics.Bitmap
import com.example.data.model.JudoGroup
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SportCardScanner {

    suspend fun processCardBitmap(context: Context, bitmap: Bitmap): Result<ExtractedMemberInfo> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Save bitmap photo locally in cache directory
                val photoFile = try {
                    val dir = File(context.cacheDir, "judoka_photos").apply { mkdirs() }
                    val file = File(dir, "card_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    file.absolutePath
                } catch (_: Exception) {
                    ""
                }

                // 2. Attempt decoding QR / Barcode via ZXing
                var decodedText: String? = null
                try {
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    val source = RGBLuminanceSource(width, height, pixels)
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                    val reader = MultiFormatReader()
                    val result = reader.decode(binaryBitmap)
                    decodedText = result.text
                } catch (_: Exception) {
                    // QR decode failed or no barcode present
                }

                val extracted = if (!decodedText.isNullOrBlank()) {
                    CardDataParser.parseCardData(decodedText).let {
                        if (it.photoUrl.isBlank() && photoFile.isNotBlank()) it.copy(photoUrl = photoFile) else it
                    }
                } else {
                    // Fallback: Generates a new pending member entry with the photographed card
                    val randomId = (1000..9999).random().toString()
                    ExtractedMemberInfo(
                        firstName = "",
                        lastName = "",
                        cardNumber = randomId,
                        qrCode = "JCS-$randomId",
                        photoUrl = photoFile,
                        season = "2024-2025",
                        group = JudoGroup.POUSSINS,
                        rawScannedData = "CARTE_PHOTO_$randomId"
                    )
                }

                Result.success(extracted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
