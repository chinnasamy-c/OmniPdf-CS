const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/DrawPdfScreen.kt', 'utf8');

code = code.replace(
    /enum class ToolMode \{ PEN, HIGHLIGHT, ERASER, TEXT \}/,
    ""
);

code = code.replace(
    /package com.example\n/,
    "package com.example\n\nenum class ToolMode { PEN, HIGHLIGHT, ERASER, TEXT }\n"
);

fs.writeFileSync('app/src/main/java/com/example/DrawPdfScreen.kt', code);
console.log("Fixed DrawPdfScreen");
