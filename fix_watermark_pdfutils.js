const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');

code = code.replace(
    /suspend fun processWatermark\([\s\S]*?outputFile: File\n    \): Boolean/,
    `suspend fun processWatermark(
        context: Context,
        sourceUri: Uri,
        mode: String,
        type: String,
        text: String,
        position: String,
        textSize: Float,
        font: String,
        colorHex: String,
        foreground: Boolean,
        rotation: Float,
        imageUri: Uri?,
        opacity: Float,
        cropEnabled: Boolean,
        startPage: Int = 1,
        endPage: Int = -1,
        outputFile: File
    ): Boolean`
);

code = code.replace(
    /val numPages = reader\.numberOfPages\n\n            for \(i in 1\.\.numPages\) \{/,
    `val numPages = reader.numberOfPages
            val actualEndPage = if (endPage == -1) numPages else Math.min(endPage, numPages)
            for (i in 1..numPages) {
                val pdfContentByte = if (foreground) stamper.getOverContent(i) else stamper.getUnderContent(i)
                if (pdfContentByte == null) continue
                if (i < startPage || i > actualEndPage) continue // skip pages out of range`
);

code = code.replace(
    /val pdfContentByte = if \(foreground\) stamper\.getOverContent\(i\) else stamper\.getUnderContent\(i\)\n                if \(pdfContentByte == null\) continue/,
    "" // We already inserted it above to do check properly
);

code = code.replace(
    /val baseFont = when \(font\) \{[\s\S]*?else -> com\.itextpdf\.text\.pdf\.BaseFont\.createFont\(com\.itextpdf\.text\.pdf\.BaseFont\.HELVETICA, com\.itextpdf\.text\.pdf\.BaseFont\.CP1252, com\.itextpdf\.text\.pdf\.BaseFont\.NOT_EMBEDDED\)\n                    \}/,
    `val baseFont = when (font) {
                        "Times New Roman" -> com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.TIMES_ROMAN, com.itextpdf.text.pdf.BaseFont.CP1252, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED)
                        "Cambria" -> com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.TIMES_ROMAN, com.itextpdf.text.pdf.BaseFont.CP1252, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED)
                        "Helvetica" -> com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.CP1252, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED)
                        "Sans Serif" -> com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.CP1252, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED)
                        else -> com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.CP1252, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED)
                    }`
);

fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
console.log("Updated PdfUtils Watermark");
