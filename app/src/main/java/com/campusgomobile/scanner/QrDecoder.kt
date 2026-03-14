package com.campusgomobile.scanner

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer

/**
 * Parsed payload from a quest QR code. Supports:
 * - Short form: "questId:stageId" (e.g. "5:12") — stage_id is the numeric stage ID from API.
 * - URL form: "https://domain/quests/5/stages/12" — backend accepts as `qr` param.
 */
data class QuestQrPayload(
    val questId: Int,
    val stageId: Int,
    val raw: String
)

/**
 * Parse QR string into quest ID and stage ID. Returns null if not valid.
 * Accepts: "questId:stageId" or URL with path /quests/{id}/stages/{id}.
 */
fun parseQuestQrPayload(raw: String): QuestQrPayload? {
    val trimmed = raw.trim()
    // Short form: "5:12"
    val colon = trimmed.indexOf(':')
    if (colon > 0 && colon < trimmed.length - 1) {
        val a = trimmed.substring(0, colon).trim().toIntOrNull()
        val b = trimmed.substring(colon + 1).trim().toIntOrNull()
        if (a != null && b != null && a > 0 && b > 0) return QuestQrPayload(questId = a, stageId = b, raw = raw)
    }
    // URL form: .../quests/5/stages/12
    val questStagesRegex = Regex("""/quests/(\d+)/stages/(\d+)""")
    val match = questStagesRegex.find(trimmed)
    if (match != null) {
        val q = match.groupValues[1].toIntOrNull()
        val s = match.groupValues[2].toIntOrNull()
        if (q != null && s != null && q > 0 && s > 0) return QuestQrPayload(questId = q, stageId = s, raw = raw)
    }
    return null
}

/**
 * Lightweight QR decoder using ZXing. Decodes only QR codes (quest ID + stage ID).
 * Works with CameraX ImageProxy and with bitmap (e.g. from ARCore camera frame for AR anchor flow).
 * See: https://developers.google.com/ar/develop/anchors
 */
object QrDecoder {

    private val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )

    /**
     * Decode QR from CameraX ImageProxy (YUV_420_888). Safe to call from image analysis executor.
     * Caller must ensure [ImageProxy.close] is invoked after this returns.
     */
    fun decodeFromImageProxy(image: ImageProxy): String? {
        if (image.format != ImageFormat.YUV_420_888) return null
        val yBuffer = image.planes[0].buffer
        val ySize = yBuffer.remaining()
        val width = image.width
        val height = image.height
        val rowStride = image.planes[0].rowStride
        val pixelStride = image.planes[0].pixelStride
        val yuvData = ByteArray(ySize)
        yBuffer.get(yuvData)
        // PlanarYUVLuminanceSource expects full YUV; for Y-only we use the Y plane as contiguous luminance.
        // ZXing's PlanarYUVLuminanceSource: first part is Y with rowStride. We have Y in yuvData.
        val source = YPlaneLuminanceSource(yuvData, width, height, rowStride, pixelStride)
        return decodeFromLuminanceSource(source)
    }

    /**
     * Decode QR from a grayscale/opaque bitmap (e.g. from ARCore frame Y plane).
     * Use this when you have a Bitmap from the AR camera image for anchor-at-QR flow.
     */
    fun decodeFromBitmap(bitmap: Bitmap): String? = decodeFromBitmapWithCenter(bitmap)?.first

    /**
     * Decode QR from bitmap and return (text, center in image pixels [x, y]) for anchoring AR object to QR.
     * Center is derived from ResultPoint positions; use with Frame.transformCoordinates2d(IMAGE_PIXELS -> VIEW) then hitTest.
     */
    fun decodeFromBitmapWithCenter(bitmap: Bitmap): Pair<String, FloatArray>? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val luminance = ByteArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            luminance[i] = ((p and 0xFF) * 0.299 + ((p shr 8) and 0xFF) * 0.587 + ((p shr 16) and 0xFF) * 0.114).toInt().toByte()
        }
        val source = YPlaneLuminanceSource(luminance, width, height, width, 1)
        return try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result: Result = MultiFormatReader().decode(binaryBitmap, hints)
            val points = result.resultPoints ?: return result.text to floatArrayOf(width / 2f, height / 2f)
            val cx = points.map { it.x }.average().toFloat()
            val cy = points.map { it.y }.average().toFloat()
            result.text to floatArrayOf(cx, cy)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeFromLuminanceSource(source: YPlaneLuminanceSource): String? {
        return try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result: Result = MultiFormatReader().decode(binaryBitmap, hints)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Luminance source backed by a single plane (Y or grayscale) with optional row stride.
     */
    private class YPlaneLuminanceSource(
        private val data: ByteArray,
        private val dataWidth: Int,
        private val dataHeight: Int,
        private val rowStride: Int,
        private val pixelStride: Int
    ) : com.google.zxing.LuminanceSource(dataWidth, dataHeight) {

        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            if (y < 0 || y >= dataHeight) throw IllegalArgumentException("Requested row is outside the image: $y")
            val width = dataWidth
            val result = row ?: ByteArray(width)
            if (rowStride == width && pixelStride == 1) {
                System.arraycopy(data, y * rowStride, result, 0, width)
            } else {
                var offset = y * rowStride
                for (x in 0 until width) {
                    result[x] = data[offset]
                    offset += pixelStride
                }
            }
            return result
        }

        override fun getMatrix(): ByteArray {
            if (rowStride == dataWidth && pixelStride == 1) {
                return data.copyOf()
            }
            val matrix = ByteArray(dataWidth * dataHeight)
            var destOffset = 0
            var srcOffset = 0
            for (_y in 0 until dataHeight) {
                for (x in 0 until dataWidth) {
                    matrix[destOffset++] = data[srcOffset]
                    srcOffset += pixelStride
                }
                srcOffset += rowStride - dataWidth * pixelStride
            }
            return matrix
        }
    }
}
