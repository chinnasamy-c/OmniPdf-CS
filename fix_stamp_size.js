const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');

code = code.replace(
    /data class StampData\(val text: String, val customImageUri: Uri\?, val x: Float, val y: Float\)/,
    "data class StampData(val text: String, val customImageUri: Uri?, val x: Float, val y: Float, val size: Float = 1f)"
);

code = code.replace(
    /fun addStamp\(pageIndex: Int, text: String, customImageUri: Uri\?, x: Float, y: Float\) \{/,
    "fun addStamp(pageIndex: Int, text: String, customImageUri: Uri?, x: Float, y: Float, size: Float = 1f) {"
);

code = code.replace(
    /currentStamps\.add\(StampData\(text, customImageUri, x, y\)\)/,
    "currentStamps.add(StampData(text, customImageUri, x, y, size))"
);

code = code.replace(
    /private val _processStatus = MutableStateFlow\(""\)/,
    "val globalStampSize = kotlinx.coroutines.flow.MutableStateFlow(1f)\n    private val _processStatus = MutableStateFlow(\"\")"
);

fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', code);
console.log("Fixed main view model stamp size");
