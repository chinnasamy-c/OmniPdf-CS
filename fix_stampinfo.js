const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');

code = code.replace(
    /data class StampInfo\(val text: String, val customImageUri: String\?, val x: Float, val y: Float\)/,
    "data class StampInfo(val text: String, val customImageUri: String?, val x: Float, val y: Float, val size: Float = 1f)"
);

// update applyStamps logic
// I need to see how StampGenerator is used in PdfUtils.
fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
console.log("Updated StampInfo. Need to see applyStamps");
