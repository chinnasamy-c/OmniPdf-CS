const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');

code = code.replace(
    /img\.scaleToFit\(150f, 150f\)/g,
    "img.scaleToFit(150f * stamp.size, 150f * stamp.size)"
);

fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
console.log("Updated PdfUtils applyStamps size scaling");
