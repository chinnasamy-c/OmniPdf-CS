const fs = require('fs');

let code = fs.readFileSync('app/src/main/java/com/example/util/PdfUtils.kt', 'utf8');

const regexSign = /suspend fun digitallySignPdf\([\s\S]*?(?=suspend fun processWatermark)/;
const matchSign = code.match(regexSign);

if (matchSign) {
    let signCode = matchSign[0];
    code = code.replace(signCode, "");
    
    // Also find extractPdfSignatures. Wait, where is it?
    // Let me check my grep of bouncycastle in PdfUtils.kt
    const extractRegex = /suspend fun extractPdfSignatures\([\s\S]*?(?=suspend fun digitallySignPdf|suspend fun processWatermark)/;
    const matchExtract = code.match(extractRegex);
    let extractCode = "";
    if (matchExtract) {
        extractCode = matchExtract[0];
        code = code.replace(extractCode, "");
    }
    
    let oldProviderRegex = /private fun getProvider[\s\S]*?(?=suspend fun extractPdfSignatures|suspend fun digitallySignPdf|suspend fun processWatermark)/;
    let matchProvider = code.match(oldProviderRegex);
    let providerCode = "";
    if (matchProvider) {
        providerCode = matchProvider[0];
        code = code.replace(providerCode, "");
    }

    fs.writeFileSync('app/src/main/java/com/example/util/PdfUtils.kt', code);
    
    let signatureUtils = `package com.example.util

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
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.security.*

object SignatureUtils {
${providerCode}
${extractCode}
${signCode}
}
`;
    // We might miss some imports, but Kotlin compiler will tell us
    fs.writeFileSync('app/src/main/java/com/example/util/SignatureUtils.kt', signatureUtils);
    console.log("Extracted BouncyCastle code to SignatureUtils.kt");
} else {
    console.log("Could not find digitallySignPdf");
}
