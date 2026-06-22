const fs = require('fs');

let pdfUtils = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');

// Find boundaries of validatePdfSignatures and extractCommonNameFromDn
let extractRegex = /(    fun extractCommonNameFromDn[\s\S]*?\n    }(?=\n\n|\n}))/;
let validateRegex = /(    fun validatePdfSignatures[\s\S]*?\n    }(?=\n\n|\n}))/;

let extractSrc = pdfUtils.match(extractRegex)?.[1] || "";
let validateSrc = pdfUtils.match(validateRegex)?.[1] || "";

pdfUtils = pdfUtils.replace(extractSrc, "").replace(validateSrc, "");
fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', pdfUtils);

let sigUtils = fs.readFileSync('app/src/main/java/com/example/util/SignatureUtils.kt', 'utf8');

sigUtils = sigUtils.replace("object SignatureUtils {", "object SignatureUtils {\n\n" + extractSrc + "\n\n" + validateSrc);
fs.writeFileSync('app/src/main/java/com/example/util/SignatureUtils.kt', sigUtils);

// Update caller as well!
let vm = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');
vm = vm.replace(/PdfUtils\.validatePdfSignatures/g, "com.example.util.SignatureUtils.validatePdfSignatures");
fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', vm);

try {
    let mainAct = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf8');
    mainAct = mainAct.replace(/PdfUtils\.validatePdfSignatures/g, "com.example.util.SignatureUtils.validatePdfSignatures");
    fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', mainAct);
} catch (e) {}

console.log("Moved validatePdfSignatures & extractCommonNameFromDn");
