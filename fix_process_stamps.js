const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');

code = code.replace(
    /private fun drawStamp\(stamp: StampData, canvas: android\.graphics\.Canvas, w: Float, h: Float\) \{[\s\S]*?canvas\.restore\(\)\n    \}/,
    `private fun drawStamp(stamp: StampData, canvas: android.graphics.Canvas, w: Float, h: Float, context: Context) {
        val baseSize = 400f
        val sizeVal = baseSize * stamp.size
        val cx = stamp.x * w
        val cy = stamp.y * h

        if (stamp.text == "CUSTOM" && stamp.customImageUri != null) {
            try {
                val input = context.contentResolver.openInputStream(stamp.customImageUri)
                val bmp = android.graphics.BitmapFactory.decodeStream(input)
                input?.close()
                if (bmp != null) {
                    val rect = android.graphics.RectF(cx - sizeVal / 2f, cy - sizeVal / 2f, cx + sizeVal / 2f, cy + sizeVal / 2f)
                    canvas.drawBitmap(bmp, null, rect, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
                    bmp.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val bmp = com.example.util.StampGenerator.generateRealisticStamp(stamp.text)
            val rect = android.graphics.RectF(cx - sizeVal / 2f, cy - sizeVal / 2f, cx + sizeVal / 2f, cy + sizeVal / 2f)
            canvas.drawBitmap(bmp, null, rect, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
            bmp.recycle()
        }
    }`
);

code = code.replace(
    /drawStamp\(stamp, pageCanvas, outWidth\.toFloat\(\), outHeight\.toFloat\(\)\)/g,
    "drawStamp(stamp, pageCanvas, outWidth.toFloat(), outHeight.toFloat(), context)"
);

fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', code);
console.log("Updated drawStamp");
