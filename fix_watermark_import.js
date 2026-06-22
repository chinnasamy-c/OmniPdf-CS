const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', 'utf8');

code = code.replace(
    /import androidx\.compose\.material3\.DropdownMenuItem/,
    "import androidx.compose.material3.DropdownMenuItem\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue"
);

fs.writeFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', code);
console.log("Updated WatermarkPdfScreen imports");
