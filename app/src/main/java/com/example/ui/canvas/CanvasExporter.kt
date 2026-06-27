package com.example.ui.canvas

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

object CanvasExporter {

    /**
     * Renders the current canvas state into a high-definition Bitmap and saves it to the gallery.
     */
    fun exportToStorage(
        context: Context,
        title: String,
        width: Int,
        height: Int,
        bgColor: Int,
        elements: List<CanvasElement>,
        drawingPaths: List<DrawPath>,
        onResult: (String?, String?) -> Unit
    ) {
        try {
            // 1. Create a high-definition Bitmap matching the project dimensions
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 2. Draw background color
            canvas.drawColor(bgColor)

            // 3. Draw canvas elements (images, shapes, stickers, texts)
            for (elem in elements) {
                canvas.save()
                
                // Move, scale, and rotate according to coordinates
                canvas.translate(elem.x, elem.y)
                canvas.rotate(elem.rotation)
                canvas.scale(elem.scale, elem.scale)

                when (elem) {
                    is ImageElement -> {
                        drawImageElement(context, canvas, elem)
                    }
                    is ShapeElement -> {
                        drawShapeElement(canvas, elem)
                    }
                    is StickerElement -> {
                        drawStickerElement(canvas, elem)
                    }
                    is TextElement -> {
                        drawTextElement(canvas, elem)
                    }
                }
                canvas.restore()
            }

            // 4. Draw paintbrush sketch paths
            drawSketchPaths(canvas, drawingPaths)

            // 5. Save the Bitmap to local storage via MediaStore (API 29+) or legacy files
            val fileName = "Artify_${title.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val savedDetails = saveBitmapToGallery(context, bitmap, fileName)
            
            onResult(savedDetails.first, savedDetails.second) // (fileAbsoluteRawPath, galleryDisplayName)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null, e.message)
        }
    }

    private fun drawImageElement(context: Context, canvas: Canvas, elem: ImageElement) {
        try {
            val uri = Uri.parse(elem.imageUriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                var srcBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (srcBitmap != null) {
                    // Limit bitmap size to keep drawing efficient
                    val maxDimension = 800
                    if (srcBitmap.width > maxDimension || srcBitmap.height > maxDimension) {
                        val ratio = srcBitmap.width.toFloat() / srcBitmap.height
                        val newW = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                        val newH = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                        srcBitmap = Bitmap.createScaledBitmap(srcBitmap, newW, newH, true)
                    }

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (elem.opacity * 255).toInt()
                    }

                    // Apply visual color matrix filters
                    when (elem.filterType) {
                        "Grayscale" -> {
                            val cm = ColorMatrix().apply { setSaturation(0f) }
                            paint.colorFilter = ColorMatrixColorFilter(cm)
                        }
                        "Sepia" -> {
                            val cm = ColorMatrix().apply {
                                setScale(1f, 0.95f, 0.82f, 1.0f)
                            }
                            paint.colorFilter = ColorMatrixColorFilter(cm)
                        }
                        "Invert" -> {
                            val cm = ColorMatrix(floatArrayOf(
                                -1f,  0f,  0f, 0f, 255f,
                                 0f, -1f,  0f, 0f, 255f,
                                 0f,  0f, -1f, 0f, 255f,
                                 0f,  0f,  0f, 1f,   0f
                            ))
                            paint.colorFilter = ColorMatrixColorFilter(cm)
                        }
                        "Vintage" -> {
                            val cm = ColorMatrix(floatArrayOf(
                                0.9f, 0.1f, 0.1f, 0f, 10f,
                                0.1f, 0.8f, 0.1f, 0f, 5f,
                                0.0f, 0.1f, 0.6f, 0f, 0f,
                                0f,   0f,   0f,   1f, 0f
                            ))
                            paint.colorFilter = ColorMatrixColorFilter(cm)
                        }
                        "Warm" -> {
                            val cm = ColorMatrix().apply {
                                setScale(1.1f, 1.0f, 0.85f, 1f)
                            }
                            paint.colorFilter = ColorMatrixColorFilter(cm)
                        }
                        "Cool" -> {
                            val cm = ColorMatrix().apply {
                                setScale(0.85f, 1.0f, 1.15f, 1f)
                            }
                            paint.colorFilter = ColorMatrixColorFilter(cm)
                        }
                    }

                    // Draw image centered at coordinates
                    val dx = -srcBitmap.width / 2f
                    val dy = -srcBitmap.height / 2f
                    canvas.drawBitmap(srcBitmap, dx, dy, paint)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // draw placeholder if load fails
            val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                textSize = 24f
            }
            canvas.drawText("[Failed to Load Photo]", -100f, 0f, errorPaint)
        }
    }

    private fun drawShapeElement(canvas: Canvas, elem: ShapeElement) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = elem.color
            alpha = (elem.opacity * 255).toInt()
            style = if (elem.isFilled) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = elem.strokeWidth
        }

        // Standard bounds for drawing shapes
        val size = 120f
        val left = -size / 2f
        val top = -size / 2f
        val right = size / 2f
        val bottom = size / 2f

        when (elem.shapeType) {
            "Circle" -> {
                canvas.drawCircle(0f, 0f, size / 2f, paint)
            }
            "Square" -> {
                canvas.drawRect(left, top, right, bottom, paint)
            }
            "Star" -> {
                val path = Path()
                val outerRadius = size / 2f
                val innerRadius = size / 5f
                val pointsCount = 5
                var angle = -Math.PI / 2

                for (i in 0 until pointsCount * 2) {
                    val r = if (i % 2 == 0) outerRadius else innerRadius
                    val currX = (r * cos(angle)).toFloat()
                    val currY = (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(currX, currY) else path.lineTo(currX, currY)
                    angle += Math.PI / pointsCount
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            "Heart" -> {
                val path = Path()
                // Heart drawing cubic curve formula
                val hSize = size * 0.7f
                path.moveTo(0f, -hSize / 4f)
                path.cubicTo(-hSize / 2f, -hSize * 0.8f, -hSize, -hSize / 3f, 0f, hSize * 0.7f)
                path.cubicTo(hSize, -hSize / 3f, hSize / 2f, -hSize * 0.8f, 0f, -hSize / 4f)
                path.close()
                canvas.drawPath(path, paint)
            }
            "SpeechBubble" -> {
                val path = Path()
                val r = size * 0.4f
                path.addRoundRect(left, top, right, bottom - 15f, r, r, Path.Direction.CW)
                // Add tail
                path.moveTo(-15f, bottom - 15f)
                path.lineTo(-30f, bottom + 15f)
                path.lineTo(15f, bottom - 15f)
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawStickerElement(canvas: Canvas, elem: StickerElement) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 60f
            alpha = (elem.opacity * 255).toInt()
            textAlign = Paint.Align.CENTER
            if (elem.tintColor != null) {
                colorFilter = PorterDuffColorFilter(elem.tintColor!!, PorterDuff.Mode.SRC_IN)
            }
        }
        // Draw centered sticker text/emoji
        canvas.drawText(elem.stickerValue, 0f, 20f, paint)
    }

    private fun drawTextElement(canvas: Canvas, elem: TextElement) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = elem.color
            textSize = elem.size
            textAlign = when (elem.alignment) {
                "Left" -> Paint.Align.LEFT
                "Right" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            
            val styleFlags = when {
                elem.isBold && elem.isItalic -> Typeface.BOLD_ITALIC
                elem.isBold -> Typeface.BOLD
                elem.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            
            val tf = when (elem.fontStyle) {
                "Serif" -> Typeface.create(Typeface.SERIF, styleFlags)
                "Monospace" -> Typeface.create(Typeface.MONOSPACE, styleFlags)
                "Cursive" -> Typeface.create("cursive", styleFlags)
                else -> Typeface.create(Typeface.SANS_SERIF, styleFlags)
            }
            typeface = tf
        }

        // Draw multiple lines if text contains newline characters
        val lines = elem.text.split("\n")
        var currentY = 0f
        val fontHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
        
        for (line in lines) {
            canvas.drawText(line, 0f, currentY, paint)
            currentY += fontHeight * 1.1f
        }
    }

    private fun drawSketchPaths(canvas: Canvas, drawingPaths: List<DrawPath>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        for (drawPath in drawingPaths) {
            if (drawPath.points.size < 2) continue
            
            paint.color = drawPath.color.toArgb()
            paint.strokeWidth = drawPath.strokeWidth

            // Apply erase blending mode if eraser is active
            if (drawPath.isEraser) {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                paint.xfermode = null
            }

            val path = Path()
            val start = drawPath.points.first()
            path.moveTo(start.x, start.y)
            for (i in 1 until drawPath.points.size) {
                val pt = drawPath.points[i]
                path.lineTo(pt.x, pt.y)
            }
            canvas.drawPath(path, paint)
        }
    }

    /**
     * Saves the final high-definition art bitmap into the phone's local storage Pictures directory.
     * Accessible seamlessly in modern devices.
     */
    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Pair<String?, String?> {
        val resolver = context.contentResolver
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ArtifyStudio")
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                try {
                    resolver.openOutputStream(imageUri).use { outputStream ->
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    }
                    return Pair(imageUri.toString(), "Pictures/ArtifyStudio/$displayName")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            // Legacy Storage permission saving (Android 9 or below)
            val path = context.getExternalFilesDir("Pictures") ?: context.filesDir
            val galleryDir = File(path, "ArtifyStudio")
            if (!galleryDir.exists()) {
                galleryDir.mkdirs()
            }
            val file = File(galleryDir, displayName)
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // Add legacy saved files to Media Store
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                return Pair(file.absolutePath, "ArtifyStudio/$displayName")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Pair(null, null)
    }
}
