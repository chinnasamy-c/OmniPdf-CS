const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');
code = code.replace(
    /com\.itextpdf\.text\.pdf\.PdfWriter\.ENCRYPTION_AES_128/g,
    "com.itextpdf.text.pdf.PdfWriter.STANDARD_ENCRYPTION_128"
);
fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
