package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import java.util.Random

object StampGenerator {
    fun generateRealisticStamp(text: String): Bitmap {
        val width = 800
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val upperText = text.uppercase()

        val isRed = upperText in listOf("APPROVED", "REJECTED", "FINAL", "TERMINATED", "CONFIDENTIAL", "VOID", "PAID")
        val isBlue = upperText in listOf("VERIFIED", "TRUSTED", "OFFICIAL", "SECURE", "COPY")
        val isOrange = upperText in listOf("CERTIFIED", "URGENT", "WARNING")
        
        val colorObj = when {
            isRed -> Color.parseColor("#C62828")
            isBlue -> Color.parseColor("#1565C0")
            isOrange -> Color.parseColor("#E65100")
            else -> Color.parseColor("#3E2723")
        }

        val style = when {
            upperText in listOf("APPROVED", "REJECTED", "TRUSTED", "OFFICIAL", "TERMINATED", "PAID") -> "BANNER"
            upperText in listOf("VERIFIED", "CERTIFIED", "WANTED", "SECURE", "URGENT") -> "CIRCLE"
            else -> "RECTANGLE"
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorObj
            strokeWidth = 20f
            this.style = Paint.Style.STROKE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorObj
            textSize = 100f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            // add slight shadow for 3d depth feel
            setShadowLayer(2f, 1f, 1f, Color.argb(100, 0, 0, 0))
        }

        val cx = width / 2f
        val cy = height / 2f

        canvas.save()
        canvas.rotate(-15f, cx, cy)

        when (style) {
            "BANNER" -> {
                paint.strokeWidth = 24f
                canvas.drawCircle(cx, cy, 300f, paint)
                paint.strokeWidth = 8f
                canvas.drawCircle(cx, cy, 270f, paint)

                val bannerHeight = 160f
                val bannerRect = RectF(cx - 320f, cy - bannerHeight / 2f, cx + 320f, cy + bannerHeight / 2f)
                val fillPaint = Paint(paint).apply { this.style = Paint.Style.FILL }
                fillPaint.setShadowLayer(4f, 2f, 2f, Color.argb(80, 0,0,0))
                canvas.drawRoundRect(bannerRect, 10f, 10f, fillPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 85f
                textPaint.setShadowLayer(2f, 1f, 1f, Color.argb(150, 0,0,0))
                while (textPaint.measureText(upperText) > 600f && textPaint.textSize > 20f) {
                    textPaint.textSize -= 2f
                }
                val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(upperText, cx, cy - textOffset, textPaint)
            }
            "CIRCLE" -> {
                paint.strokeWidth = 24f
                canvas.drawCircle(cx, cy, 300f, paint)
                paint.strokeWidth = 8f
                canvas.drawCircle(cx, cy, 270f, paint)
                
                // Draw inner circle
                paint.strokeWidth = 4f
                canvas.drawCircle(cx, cy, 180f, paint)

                // TEXT ON PATH (CURVED)
                val textPath = Path()
                // the path for text is a circle. 
                // We add arc from 180 degrees sweep angle 180 so it draws on top half
                val pathRect = RectF(cx - 225f, cy - 225f, cx + 225f, cy + 225f)
                textPath.addArc(pathRect, 180f, 180f)
                
                textPaint.textSize = 65f
                while (textPaint.measureText(upperText) > 650f && textPaint.textSize > 20f) {
                    textPaint.textSize -= 2f
                }
                
                // Draw curved text top
                canvas.drawTextOnPath(upperText, textPath, 0f, 20f, textPaint)
                
                // Provide a bottom star line
                val bottomPath = Path()
                bottomPath.addArc(pathRect, 0f, 180f) // bottom half
                canvas.drawTextOnPath("★ ★ ★", bottomPath, 0f, -20f, textPaint)

                // draw center large text if there is one, else just a star
                textPaint.textSize = 80f
                val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText("ORIGINAL", cx, cy - textOffset, textPaint)
            }
            "RECTANGLE" -> {
                paint.strokeWidth = 24f
                val rw = 680f
                val rh = 280f
                val rect = RectF(cx - rw / 2f, cy - rh / 2f, cx + rw / 2f, cy + rh / 2f)
                canvas.drawRoundRect(rect, 30f, 30f, paint)

                paint.strokeWidth = 8f
                val innerRect = RectF(cx - rw / 2f + 20f, cy - rh / 2f + 20f, cx + rw / 2f - 20f, cy + rh / 2f - 20f)
                canvas.drawRoundRect(innerRect, 15f, 15f, paint)

                textPaint.textSize = 90f
                while (textPaint.measureText(upperText) > rw - 60f && textPaint.textSize > 20f) {
                    textPaint.textSize -= 2f
                }
                val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(upperText, cx, cy - textOffset, textPaint)
            }
        }
        canvas.restore()

        // GRUNGE EFFECT (Smooth transparency mask for realism)
        addRealisticGrunge(bitmap, width, height)

        return bitmap
    }

    private fun addRealisticGrunge(bitmap: Bitmap, width: Int, height: Int) {
        val grungeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val gCanvas = Canvas(grungeBitmap)
        val gPaint = Paint()
        gPaint.color = Color.BLACK
        
        val random = Random(bitmap.hashCode().toLong())
        // Draw many randomized specs and lines
        for (i in 0 until 5000) {
            val x = random.nextInt(width).toFloat()
            val y = random.nextInt(height).toFloat()
            val r = random.nextFloat() * 2f
            gPaint.alpha = random.nextInt(150)
            gCanvas.drawCircle(x, y, r, gPaint)
        }
        // Draw some larger streaks
        for (i in 0 until 50) {
            val startX = random.nextInt(width).toFloat()
            val startY = random.nextInt(height).toFloat()
            val endX = startX + (random.nextInt(30) - 15)
            val endY = startY + (random.nextInt(30) - 15)
            gPaint.strokeWidth = random.nextFloat() * 3f
            gPaint.alpha = random.nextInt(100)
            gCanvas.drawLine(startX, startY, endX, endY, gPaint)
        }

        // Apply grunge map as SRC_OUT to cut out pixels from original bitmap
        val mainCanvas = Canvas(bitmap)
        val clearPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        mainCanvas.drawBitmap(grungeBitmap, 0f, 0f, clearPaint)
        grungeBitmap.recycle()
    }
}
