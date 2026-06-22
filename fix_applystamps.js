const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');

// I need to only revert the mistake in applyStamps
// In applyStamps:
// numPages
//             val actualEndPage = if (endPage == -1) numPages else Math.min(endPage, numPages)
//             for (i in 1..numPages) {
//                 
//                 if (i < startPage || i > actualEndPage) continue // skip pages out of range
//                 val stampsForPage = stamps[i - 1] // 0-indexed in UI, 1-indexed in iText
//                 if (stampsForPage.isNullOrEmpty()) continue

code = code.replace(
    /stamper = com\.itextpdf\.text\.pdf\.PdfStamper\(reader, FileOutputStream\(outputFile\)\)\n\n            val numPages = reader\.numberOfPages\n            val actualEndPage = if \(endPage == -1\) numPages else Math\.min\(endPage, numPages\)\n            for \(i in 1\.\.numPages\) \{\n                \n                if \(i < startPage \|\| i > actualEndPage\) continue \/\/ skip pages out of range\n                val stampsForPage = stamps\[i - 1\] \/\/ 0-indexed in UI, 1-indexed in iText\n                if \(stampsForPage\.isNullOrEmpty\(\)\) continue/,
    `stamper = com.itextpdf.text.pdf.PdfStamper(reader, FileOutputStream(outputFile))

            val numPages = reader.numberOfPages
            for (i in 1..numPages) {
                val stampsForPage = stamps[i - 1] // 0-indexed in UI, 1-indexed in iText
                if (stampsForPage.isNullOrEmpty()) continue`
);

fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
console.log("Fixed applyStamps");
