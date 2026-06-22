const fs = require('fs');
let s = fs.readFileSync('app/src/main/java/com/example/util/SignatureUtils.kt', 'utf8');
s = s.replace(/createCorporateSealBitmap\(/g, "com.example.util.PdfUtils.createCorporateSealBitmap(");
fs.writeFileSync('app/src/main/java/com/example/util/SignatureUtils.kt', s);
console.log("Fixed unresolved refs 2");
