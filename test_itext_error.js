const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');
code = code.replace(
    /catch \(e: Exception\) \{/g,
    "catch (e: Throwable) { android.util.Log.e(\"pdf-protect-crash\", \"Error in PdfUtils: \", e);"
);
fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
