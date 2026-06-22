const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');

code = code.replace(
    /data class DrawnPage\([\s\S]*?val strokes: List<DrawnStroke>\n\)/,
    "data class DrawnText(val text: String, val x: Float, val y: Float, val size: Float, val color: Int)\n\ndata class DrawnPage(\n    val strokes: List<DrawnStroke>,\n    val texts: List<DrawnText> = emptyList()\n)"
);

// We need to update the drawPdf rendering logic
code = code.replace(
    /document\.newPage\(\)/g,
    `document.newPage()
            for (dt in page.texts) {
                // we draw text directly onto PDF content byte
                cb?.saveState()
                cb?.beginText()
                cb?.setColorFill(com.itextpdf.text.BaseColor(dt.color, true))
                val baseFont = com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.CP1252, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED)
                cb?.setFontAndSize(baseFont, dt.size)
                cb?.setTextMatrix(dt.x, 842f - dt.y) // iText origin is bottom-left, our Y is from top
                cb?.showText(dt.text)
                cb?.endText()
                cb?.restoreState()
            }`
);

fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
console.log("Updated PdfUtils drawn text");
