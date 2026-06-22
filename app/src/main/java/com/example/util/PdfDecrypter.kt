package com.example.util

import android.content.Context
import android.net.Uri
import com.itextpdf.text.exceptions.BadPasswordException
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Modes of Password Search
enum class RecoveryMode {
    COMMON_DICTIONARY,
    NUMERIC_PIN_4,
    NUMERIC_PIN_6,
    CUSTOM_CHARSET
}

data class EncryptionInfo(
    val isEncrypted: Boolean,
    val canOpenWithEmptyPassword: Boolean
)

object PdfDecrypter {

    /**
     * Checks if a PDF is indeed encrypted and if it requires a password to open.
     */
    fun getEncryptionInfo(context: Context, uri: Uri): EncryptionInfo {
        var isEncrypted = false
        var canOpenWithoutPassword = false
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = PdfReader(stream)
                isEncrypted = reader.isEncrypted
                canOpenWithoutPassword = true
                reader.close()
            }
        } catch (e: BadPasswordException) {
            isEncrypted = true
            canOpenWithoutPassword = false
        } catch (e: IOException) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("bad user password") || msg.contains("password") || msg.contains("encrypted") || msg.contains("badpassword")) {
                isEncrypted = true
            }
        } catch (e: Exception) {
            // Unhandled Parse exceptions
        }
        return EncryptionInfo(isEncrypted = isEncrypted, canOpenWithEmptyPassword = canOpenWithoutPassword)
    }

    /**
     * Tries a password attempt using a preloaded byte array.
     * High performance - avoids loading from disk for each guess.
     */
    private fun testPassword(bytes: ByteArray, password: String): Boolean {
        return try {
            val reader = PdfReader(bytes, password.toByteArray(Charsets.ISO_8859_1))
            val ok = !reader.isEncrypted || reader.isOpenedWithFullPermissions
            reader.close()
            true
        } catch (e: BadPasswordException) {
            false
        } catch (e: IOException) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("bad user password") || msg.contains("password") || msg.contains("badpassword")) {
                false
            } else {
                // Some other read exception; assume incorrect decryption
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Decrypts an encrypted PDF file to an output file, using the decrypted password.
     */
    fun decryptAndSave(context: Context, uri: Uri, password: String, outputFile: File): Boolean {
        var reader: PdfReader? = null
        var stamper: PdfStamper? = null
        return try {
            val inputBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
            reader = PdfReader(inputBytes, password.toByteArray(Charsets.ISO_8859_1))
            
            // Re-inflate pages and output to unencrypted stream
            val fos = FileOutputStream(outputFile)
            stamper = PdfStamper(reader, fos)
            stamper.close()
            reader.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { reader?.close() } catch (ignored: Exception) {}
        }
    }

    fun removeRestrictions(context: Context, uri: Uri, outputFile: File): Boolean {
        var reader: PdfReader? = null
        var stamper: PdfStamper? = null
        return try {
            val inputBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
            PdfReader.unethicalreading = true
            reader = PdfReader(inputBytes)
            val fos = FileOutputStream(outputFile)
            stamper = PdfStamper(reader, fos)
            stamper.close()
            reader.close()
            true
        } catch (e: BadPasswordException) {
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { reader?.close() } catch (ignored: Exception) {}
            PdfReader.unethicalreading = false
        }
    }

    private fun indexToPassword(index: Long, charset: String): String {
        var temp = index
        val base = charset.length
        val sb = StringBuilder()
        while (temp >= 0) {
            sb.append(charset[(temp % base).toInt()])
            temp = (temp / base) - 1
        }
        return sb.reverse().toString()
    }

    /**
     * Live Core Recovery Engine supporting cancellation of task.
     */
    suspend fun tryRecoverPassword(
        context: Context,
        uri: Uri,
        mode: RecoveryMode,
        fileName: String,
        onProgress: (tried: Long, candidate: String, total: Long) -> Unit
    ): String? = withContext(Dispatchers.Default) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: return@withContext null

        val cleanName = fileName.substringBeforeLast(".").lowercase()
        val dictionary = generateDictionary(cleanName)

        val totalToSearch = when (mode) {
            RecoveryMode.COMMON_DICTIONARY -> dictionary.size.toLong()
            RecoveryMode.NUMERIC_PIN_4 -> 10000L
            RecoveryMode.NUMERIC_PIN_6 -> 1000000L
            RecoveryMode.CUSTOM_CHARSET -> 2625640L // 40 chars config up to length 4
        }

        val processors = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val foundRef = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val triedCounter = java.util.concurrent.atomic.AtomicLong(0)
        val lastProgressUpdate = java.util.concurrent.atomic.AtomicLong(0)

        when (mode) {
            RecoveryMode.COMMON_DICTIONARY -> {
                val deferreds = (0 until processors).map { workerId ->
                    async {
                        var index = workerId
                        while (index < dictionary.size && isActive && foundRef.get() == null) {
                            val candidate = dictionary[index]
                            if (testPassword(bytes, candidate)) {
                                foundRef.compareAndSet(null, candidate)
                                break
                            }
                            val currentTried = triedCounter.incrementAndGet()
                            val now = System.currentTimeMillis()
                            val lastUpdate = lastProgressUpdate.get()
                            if (now - lastUpdate >= 150L) {
                                if (lastProgressUpdate.compareAndSet(lastUpdate, now)) {
                                    onProgress(currentTried, candidate, totalToSearch)
                                }
                            }
                            index += processors
                        }
                    }
                }
                deferreds.awaitAll()
            }
            RecoveryMode.NUMERIC_PIN_4 -> {
                val deferreds = (0 until processors).map { workerId ->
                    async {
                        var num = workerId
                        while (num <= 9999 && isActive && foundRef.get() == null) {
                            val candidate = String.format("%04d", num)
                            if (testPassword(bytes, candidate)) {
                                foundRef.compareAndSet(null, candidate)
                                break
                            }
                            val currentTried = triedCounter.incrementAndGet()
                            val now = System.currentTimeMillis()
                            val lastUpdate = lastProgressUpdate.get()
                            if (now - lastUpdate >= 150L) {
                                if (lastProgressUpdate.compareAndSet(lastUpdate, now)) {
                                    onProgress(currentTried, candidate, totalToSearch)
                                }
                            }
                            num += processors
                        }
                    }
                }
                deferreds.awaitAll()
            }
            RecoveryMode.NUMERIC_PIN_6 -> {
                val deferreds = (0 until processors).map { workerId ->
                    async {
                        var num = workerId
                        while (num <= 999999 && isActive && foundRef.get() == null) {
                            val candidate = String.format("%06d", num)
                            if (testPassword(bytes, candidate)) {
                                foundRef.compareAndSet(null, candidate)
                                break
                            }
                            val currentTried = triedCounter.incrementAndGet()
                            val now = System.currentTimeMillis()
                            val lastUpdate = lastProgressUpdate.get()
                            if (now - lastUpdate >= 150L) {
                                if (lastProgressUpdate.compareAndSet(lastUpdate, now)) {
                                    onProgress(currentTried, candidate, totalToSearch)
                                }
                            }
                            num += processors
                        }
                    }
                }
                deferreds.awaitAll()
            }
            RecoveryMode.CUSTOM_CHARSET -> {
                val charset = "abcdefghijklmnopqrstuvwxyz0123456789@#_-"
                val deferreds = (0 until processors).map { workerId ->
                    async {
                        var idx = workerId.toLong()
                        while (idx < totalToSearch && isActive && foundRef.get() == null) {
                            val candidate = indexToPassword(idx, charset)
                            if (testPassword(bytes, candidate)) {
                                foundRef.compareAndSet(null, candidate)
                                break
                            }
                            val currentTried = triedCounter.incrementAndGet()
                            val now = System.currentTimeMillis()
                            val lastUpdate = lastProgressUpdate.get()
                            if (now - lastUpdate >= 150L) {
                                if (lastProgressUpdate.compareAndSet(lastUpdate, now)) {
                                    onProgress(currentTried, candidate, totalToSearch)
                                }
                            }
                            idx += processors
                        }
                    }
                }
                deferreds.awaitAll()
            }
        }

        // Final forced progress reporting
        val finalTried = triedCounter.get()
        onProgress(finalTried, foundRef.get() ?: "", totalToSearch)

        return@withContext foundRef.get()
    }

    /**
     * Standard dictionary list containing common company passwords, numeric PINs, and smart name-derived words.
     */
    private fun generateDictionary(fileNameBase: String): List<String> {
        val bases = mutableListOf(
            "1234", "12345", "123456", "0000", "1111", "2222", "8888", "9999", "12345678", "123456789",
            "password", "pdf", "root", "admin", "welcome", "123", "qwert", "qwerty", "user", "love",
            "computer", "invoice", "statement", "salary", "pay", "bank", "receipt", "secure", "locked",
            "abcd", "secret", "pass", "owner", "customer"
        )
        
        // Extended 8+ character common keywords
        val commonLongWords = listOf(
            "password123", "password1234", "password321", "welcome123", "admin123", "administrator",
            "confidential", "protected", "document", "document123", "statements", "statement123", 
            "customer123", "personal", "chemistry", "security", "accounts", "finance2026", 
            "shoppdf2026", "secret2026", "pdfpassword", "company2026", "xerox123", "office123",
            "myinvoice", "unlocked", "pass1234", "pass12345", "user1234", "userpassword"
        )
        bases.addAll(commonLongWords)
        
        // Add dynamic smart candidate based on the file name itself! (E.g. Invoice_March -> Invoice_March, invoice_march, etc.)
        val cleanName = fileNameBase.replace("[^a-zA-Z0-9]".toRegex(), " ").trim()
        val tokens = cleanName.split("\\s+".toRegex()).filter { it.length > 2 }
        for (tok in tokens) {
            val lowerTok = tok.lowercase()
            bases.add(tok)
            bases.add(lowerTok)
            bases.add(tok.uppercase())
            bases.add("${lowerTok}123")
            bases.add("${lowerTok}@123")
            bases.add("${lowerTok}2026")
            bases.add("${lowerTok}2025")
            bases.add("${lowerTok}2024")
            bases.add("${lowerTok}password")
            bases.add("password${lowerTok}")
            
            // Smart combinators for more than 8 letters
            if (lowerTok.length >= 5) {
                bases.add("${lowerTok}123456")
                bases.add("${lowerTok}12345678")
                bases.add("${lowerTok}statement")
                bases.add("${lowerTok}invoice")
                bases.add("${lowerTok}document")
            }
        }
        
        // Add current/past/future years
        for (year in 2018..2028) {
            bases.add(year.toString())
            bases.add("welcome$year")
            bases.add("password$year")
        }

        return bases.distinct()
    }
}
