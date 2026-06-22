const fs = require('fs');

let vm = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');
vm = vm.replace(/PdfUtils\.digitallySignPdf/g, "com.example.util.SignatureUtils.digitallySignPdf");
fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', vm);

let sigScreen = fs.readFileSync('app/src/main/java/com/example/SignatureMakerScreen.kt', 'utf8');
sigScreen = sigScreen.replace(/PdfUtils\.extractPdfSignatures/g, "com.example.util.SignatureUtils.extractPdfSignatures");
fs.writeFileSync('app/src/main/java/com/example/SignatureMakerScreen.kt', sigScreen);

console.log("Updated references.");
