const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/DrawPdfScreen.kt', 'utf8');

code = code.replace(
    /package com\.example\n\nenum class ToolMode \{ PEN, HIGHLIGHT, ERASER, TEXT \}\n/,
    "package com.example\n"
);

code = code.replace(
    /data class DrawStrokeLocal\(/,
    "enum class ToolMode { PEN, HIGHLIGHT, ERASER, TEXT }\n\ndata class DrawStrokeLocal("
);

fs.writeFileSync('app/src/main/java/com/example/DrawPdfScreen.kt', code);
console.log("Fixed DrawPdfScreen enum import issue");
