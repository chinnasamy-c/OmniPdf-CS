const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', 'utf8');

code = code.replace(
    /import androidx\.compose\.runtime\.Composable/,
    "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.setValue\nimport androidx.compose.runtime.getValue"
);

fs.writeFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', code);
console.log("Updated WatermarkPdfScreen missing runtime imports");
