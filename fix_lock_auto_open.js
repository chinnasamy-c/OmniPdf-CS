const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');

code = code.replace(
    /                \/\/ Auto open\n                openFile\(context, outputFile\)/,
    "                // Removed Auto open"
);

fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', code);
console.log("Removed auto open from lock pdf");
