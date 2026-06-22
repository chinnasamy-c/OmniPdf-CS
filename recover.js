const fs = require('fs');

let sigUtils = fs.readFileSync('app/src/main/java/com/example/util/SignatureUtils.kt', 'utf8');

let functions = [];
let currentIndex = 0;
while (true) {
    let funIndex = sigUtils.indexOf(" fun ", currentIndex);
    // Find the modifiers before "fun"
    if (funIndex === -1) break;
    
    // Look backwards to see if it's "suspend fun" or "private fun"
    let startLineIndex = sigUtils.lastIndexOf("\n", funIndex);
    if (startLineIndex === -1) startLineIndex = 0;
    
    // Check next fun
    let nextFunIndex = sigUtils.indexOf(" fun ", funIndex + 5);
    let endIndex = nextFunIndex === -1 ? sigUtils.lastIndexOf("}") : sigUtils.lastIndexOf("\n", nextFunIndex);
    
    if (endIndex > startLineIndex) {
        let funcStr = sigUtils.substring(startLineIndex, endIndex);
        functions.push(funcStr);
        currentIndex = endIndex;
    } else {
        break;
    }
}

let keepInSigUtils = [];
let backToPdfUtils = [];

for (let func of functions) {
    if (func.includes("fun extractPdfSignatures") || 
        func.includes("fun digitallySignPdf") || 
        func.includes("fun getProvider")) {
        keepInSigUtils.push(func);
    } else {
        backToPdfUtils.push(func);
    }
}

let oldPdfUtils = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');
oldPdfUtils = oldPdfUtils.replace("    suspend fun processWatermark", backToPdfUtils.join("\n") + "\n\n    suspend fun processWatermark");

fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', oldPdfUtils);

let newSigUtils = `package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.Calendar
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.security.*

object SignatureUtils {
${keepInSigUtils.join("\n")}
}
`;

fs.writeFileSync('app/src/main/java/com/example/util/SignatureUtils.kt', newSigUtils);
console.log("Recovery parsed by function bounds.");
