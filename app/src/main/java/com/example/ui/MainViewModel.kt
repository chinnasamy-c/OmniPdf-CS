package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import android.graphics.pdf.PdfRenderer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryRecord
import com.example.data.HistoryRepository
import com.example.util.PdfUtils
import com.example.util.SelectableFile
import com.example.util.PdfDecrypter
import com.example.util.RecoveryMode
import com.example.util.OrganizePageItem
import com.example.util.PdfOverlay
import com.example.util.PdfRepairer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository
    val allHistory: StateFlow<List<HistoryRecord>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
        allHistory = repository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Active Screen: "HOME", "MERGE", "SPLIT"
    private val _currentScreen = MutableStateFlow("HOME")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Processing indicator
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var processingWakeLock: android.os.PowerManager.WakeLock? = null

    init {
        viewModelScope.launch {
            _isProcessing.collect { isProcessingState ->
                try {
                    if (isProcessingState) {
                        if (processingWakeLock == null) {
                            val pm = application.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                            // using SuppressLint to avoid Wakelock timeout warning if not using timeout, but here we use timeout
                            processingWakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "PdfApp::ProcessingTask")
                        }
                        if (processingWakeLock?.isHeld == false) {
                            processingWakeLock?.acquire(20 * 60 * 1000L) // max 20 mins timeout
                        }
                    } else {
                        if (processingWakeLock?.isHeld == true) {
                            processingWakeLock?.release()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Processing progress/status description (e.g. "Merging pages...", "Rendering thumbnails...")
    val globalStampSize = kotlinx.coroutines.flow.MutableStateFlow(1f)
    private val _processStatus = MutableStateFlow("")
    val processStatus: StateFlow<String> = _processStatus.asStateFlow()

    // Recent Operation complete state
    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted: StateFlow<Boolean> = _operationCompleted.asStateFlow()

    private val _lastGeneratedRecord = MutableStateFlow<HistoryRecord?>(null)
    val lastGeneratedRecord: StateFlow<HistoryRecord?> = _lastGeneratedRecord.asStateFlow()

    // --- Convert PDF state ---
    private val _selectedConvertTool = MutableStateFlow<String?>(null)
    val selectedConvertTool: StateFlow<String?> = _selectedConvertTool.asStateFlow()

    private val _convertSourceUri = MutableStateFlow<Uri?>(null)
    val convertSourceUri: StateFlow<Uri?> = _convertSourceUri.asStateFlow()

    private val _convertSourceName = MutableStateFlow("")
    val convertSourceName: StateFlow<String> = _convertSourceName.asStateFlow()

    private val _multipleConvertUris = MutableStateFlow<List<Uri>>(emptyList())
    val multipleConvertUris: StateFlow<List<Uri>> = _multipleConvertUris.asStateFlow()

    private val _convertHtmlUrl = MutableStateFlow("")
    val convertHtmlUrl: StateFlow<String> = _convertHtmlUrl.asStateFlow()

    private val _htmlPdfPageSize = MutableStateFlow("A4")
    val htmlPdfPageSize: StateFlow<String> = _htmlPdfPageSize.asStateFlow()
    fun updateHtmlPdfPageSize(size: String) { _htmlPdfPageSize.value = size }

    private val _convertTypeText = MutableStateFlow("")
    val convertTypeText: StateFlow<String> = _convertTypeText.asStateFlow()

    private val _jpgPdfOrientation = MutableStateFlow("PORTRAIT")
    val jpgPdfOrientation: StateFlow<String> = _jpgPdfOrientation.asStateFlow()

    private val _jpgPdfMarginDp = MutableStateFlow(20)
    val jpgPdfMarginDp: StateFlow<Int> = _jpgPdfMarginDp.asStateFlow()

    private val _convertOperationCompleted = MutableStateFlow(false)
    val convertOperationCompleted: StateFlow<Boolean> = _convertOperationCompleted.asStateFlow()

    private val _convertLastGeneratedRecord = MutableStateFlow<HistoryRecord?>(null)
    val convertLastGeneratedRecord: StateFlow<HistoryRecord?> = _convertLastGeneratedRecord.asStateFlow()

    fun selectConvertTool(toolId: String) {
        _selectedConvertTool.value = toolId
        _convertSourceUri.value = null
        _convertSourceName.value = ""
        _multipleConvertUris.value = emptyList()
        _convertHtmlUrl.value = ""
        _convertTypeText.value = ""
        _jpgPdfOrientation.value = "PORTRAIT"
        _jpgPdfMarginDp.value = 20
        _convertOperationCompleted.value = false
        _convertLastGeneratedRecord.value = null
        navigateTo("CONVERT_PDF")
    }

    fun setConvertSourceUri(context: Context, uri: Uri) {
        _convertSourceUri.value = uri
        _convertSourceName.value = getFileName(context, uri) ?: "Selected File"
    }

    fun setMultipleConvertUris(uris: List<Uri>) {
        _multipleConvertUris.value = uris
    }

    fun updateConvertHtmlUrl(url: String) {
        _convertHtmlUrl.value = url
    }

    fun updateConvertTypeText(text: String) {
        _convertTypeText.value = text
    }

    fun updateJpgPdfSettings(orientation: String, marginDp: Int) {
        _jpgPdfOrientation.value = orientation
        _jpgPdfMarginDp.value = marginDp
    }

    fun clearConvertState() {
        _selectedConvertTool.value = null
        _convertSourceUri.value = null
        _convertSourceName.value = ""
        _multipleConvertUris.value = emptyList()
        _convertHtmlUrl.value = ""
        _convertTypeText.value = ""
        _convertOperationCompleted.value = false
        _convertLastGeneratedRecord.value = null
    }

    // --- Auto Scroll PDF state ---
    private val _autoScrollPdfSourceUri = MutableStateFlow<Uri?>(null)
    val autoScrollPdfSourceUri: StateFlow<Uri?> = _autoScrollPdfSourceUri.asStateFlow()

    fun setAutoScrollPdfSourceFile(context: Context, uri: Uri) {
        _autoScrollPdfSourceUri.value = uri
    }

    fun clearAutoScrollPdfFile() {
        _autoScrollPdfSourceUri.value = null
    }

    // --- Restriction Remover state ---
    private val _restrictionSourceFile = MutableStateFlow<SelectableFile?>(null)
    val restrictionSourceFile: StateFlow<SelectableFile?> = _restrictionSourceFile.asStateFlow()
    
    private val _restrictionInfo = MutableStateFlow<com.example.util.EncryptionInfo?>(null)
    val restrictionInfo: StateFlow<com.example.util.EncryptionInfo?> = _restrictionInfo.asStateFlow()

    fun setRestrictionSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val name = getFileName(context, uri) ?: "document.pdf"
            val size = getFileSize(context, uri)
            _restrictionSourceFile.value = SelectableFile(java.util.UUID.randomUUID().toString(), uri, name, true, size)
            val info = com.example.util.PdfDecrypter.getEncryptionInfo(context, uri)
            _restrictionInfo.value = info
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
        }
    }

    fun clearRestrictionSource() {
        _restrictionSourceFile.value = null
        _restrictionInfo.value = null
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun clearAutoTagPdf() {
        _autoTagSourceUri.value = null
        _autoTagNodes.value = emptyList()
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    private val _autoTagSourceUri = MutableStateFlow<Uri?>(null)
    val autoTagSourceUri: StateFlow<Uri?> = _autoTagSourceUri.asStateFlow()

    private val _autoTagNodes = MutableStateFlow<List<com.example.util.DocumentTagNode>>(emptyList())
    val autoTagNodes: StateFlow<List<com.example.util.DocumentTagNode>> = _autoTagNodes.asStateFlow()

    fun setAutoTagSource(uri: Uri) { _autoTagSourceUri.value = uri }

    fun analyzeAutoTagPdf(context: Context) {
        val uri = _autoTagSourceUri.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing document structure..."
            val result = com.example.util.PdfUtils.analyzePdfTags(context, uri)
            _autoTagNodes.value = result
            _isProcessing.value = false
        }
    }

    fun saveAutoTagPdf(context: Context) {
        val uri = _autoTagSourceUri.value ?: return
        val nodes = _autoTagNodes.value
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Saving tagged PDF..."
            val outputDirectory = getOutputDirectory(context)
            val outputName = "Tagged_${System.currentTimeMillis()}.pdf"
            val outFile = File(outputDirectory, outputName)

            val success = com.example.util.PdfUtils.saveTaggedPdf(context, uri, nodes, outFile)

            if (success && outFile.exists()) {
                val record = HistoryRecord(
                    toolType = "AUTO_TAG_PDF",
                    fileName = outputName,
                    filePath = outFile.absolutePath,
                    fileSize = outFile.length(),
                    pagesCount = 1
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                openFile(context, outFile)
            } else {
                android.widget.Toast.makeText(context, "Failed to save tagged PDF", android.widget.Toast.LENGTH_SHORT).show()
            }
            _isProcessing.value = false
        }
    }

    fun removeRestrictions(context: Context) {
        val source = _restrictionSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Removing owner restrictions..."

            val outputDirectory = getOutputDirectory(context)
            val outputName = "Unrestricted_${source.name}"
            val outFile = File(outputDirectory, outputName)

            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.util.PdfDecrypter.removeRestrictions(context, source.uri, outFile)
            }

            if (success && outFile.exists()) {
                val record = HistoryRecord(
                    toolType = "RESTRICTION_REMOVER",
                    fileName = outputName,
                    filePath = outFile.absolutePath,
                    fileSize = outFile.length(),
                    pagesCount = 1
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
            } else {
                android.widget.Toast.makeText(context, "Could not remove restrictions. User password might be required.", android.widget.Toast.LENGTH_LONG).show()
            }
            _isProcessing.value = false
        }
    }

    fun saveDrawPdf(context: Context, pages: List<com.example.util.DrawnPage>) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Saving drawn PDF..."

            val outputDirectory = getOutputDirectory(context)
            val outputName = "Drawn_PDF_${System.currentTimeMillis()}.pdf"
            val outFile = File(outputDirectory, outputName)

            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.util.PdfUtils.compileDrawnPdf(context, pages, outFile)
            }

            if (success && outFile.exists()) {
                val record = HistoryRecord(
                    toolType = "DRAW_PDF",
                    fileName = outputName,
                    filePath = outFile.absolutePath,
                    fileSize = outFile.length(),
                    pagesCount = pages.size
                )
                repository.insertRecord(record)
                
                openFile(context, outFile) // auto open
            } else {
                android.widget.Toast.makeText(context, "Could not save drawing.", android.widget.Toast.LENGTH_LONG).show()
            }
            _isProcessing.value = false
            navigateTo("HOME")
        }
    }

    fun saveToDownloads(context: Context, record: HistoryRecord) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val srcFile = File(record.filePath)
                if (!srcFile.exists()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Source file not found", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val resolver = context.contentResolver
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, record.fileName)
                    val mime = if (record.fileName.lowercase().endsWith(".pdf")) "application/pdf" 
                               else if (record.fileName.lowercase().endsWith(".zip")) "application/zip" 
                               else if (record.fileName.lowercase().endsWith(".jpg")) "image/jpeg" 
                               else "application/octet-stream"
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        srcFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Saved successfully to Downloads folder!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val destFile = File(downloadsDir, record.fileName)
                    srcFile.inputStream().use { inStream ->
                        destFile.outputStream().use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Saved to ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Error saving to Downloads: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun runConversion(context: Context) {
        val tool = _selectedConvertTool.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Starting file transformation..."
            
            val outputDir = getOutputDirectory(context)
            val timestamp = System.currentTimeMillis()
            
            val ext = when (tool) {
                "pdf_to_word" -> "doc"
                "pdf_to_powerpoint" -> "rtf"
                "pdf_to_excel" -> "csv"
                "pdf_to_jpg" -> {
                    val pages = _convertSourceUri.value?.let { PdfUtils.getPdfPageCount(context, it) } ?: 1
                    if (pages > 1) "zip" else "jpg"
                }
                else -> "pdf"
            }
            
            val outputName = "converted_${timestamp}.$ext"
            val outputFile = File(outputDir, outputName)
            
            _processStatus.value = "Formatting and rendering document layout..."
            
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    when (tool) {
                        "pdf_to_word" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.pdfToWord(context, uri, outputFile)
                        }
                        "pdf_to_powerpoint" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.pdfToPowerPoint(context, uri, outputFile)
                        }
                        "pdf_to_excel" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.pdfToExcel(context, uri, outputFile)
                        }
                        "word_to_pdf" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.wordToPdf(context, uri, outputFile)
                        }
                        "powerpoint_to_pdf" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.powerPointToPdf(context, uri, outputFile)
                        }
                        "excel_to_pdf" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.excelToPdf(context, uri, outputFile)
                        }
                        "pdf_to_jpg" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.pdfToJpg(context, uri, outputFile)
                        }
                        "jpg_to_pdf" -> {
                            val uris = _multipleConvertUris.value
                            if (uris.isEmpty()) return@withContext false
                            PdfUtils.jpgToPdf(context, uris, _jpgPdfOrientation.value, _jpgPdfMarginDp.value, outputFile)
                        }
                        "html_to_pdf" -> {
                            val url = _convertHtmlUrl.value
                            if (url.isBlank()) return@withContext false
                            PdfUtils.htmlToPdf(context, url, _htmlPdfPageSize.value, outputFile)
                        }
                        "pdf_to_pdfa" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.pdfToPdfA(context, uri, outputFile)
                        }
                        "epub_to_pdf" -> {
                            val uri = _convertSourceUri.value ?: return@withContext false
                            PdfUtils.epubToPdf(context, uri, outputFile)
                        }
                        "txt_to_pdf" -> {
                            PdfUtils.txtToPdf(context, _convertTypeText.value, _convertSourceUri.value, outputFile)
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            
            if (success && outputFile.exists()) {
                val record = HistoryRecord(
                    toolType = "CONVERT",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = 1
                )
                repository.insertRecord(record)
                _convertLastGeneratedRecord.value = record
                _convertOperationCompleted.value = true
                _lastGeneratedRecord.value = record
                _operationCompleted.value = false
                _isProcessing.value = false
            } else {
                _isProcessing.value = false
                Toast.makeText(context, "Conversion failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Merge PDF state ---
    private val _selectedMergeFiles = MutableStateFlow<List<SelectableFile>>(emptyList())
    val selectedMergeFiles: StateFlow<List<SelectableFile>> = _selectedMergeFiles.asStateFlow()

    fun addFilesForMerge(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing selected files..."
            val list = _selectedMergeFiles.value.toMutableList()
            val newItems = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                uris.map { uri ->
                    val name = getFileName(context, uri) ?: "file_${UUID.randomUUID().toString().take(6)}"
                    val isPdf = name.lowercase().endsWith(".pdf") || getMimeType(context, uri)?.contains("pdf") == true
                    val size = getFileSize(context, uri)
                    val pageCount = if (isPdf) PdfUtils.getPdfPageCount(context, uri) else 1
                    SelectableFile(
                        id = UUID.randomUUID().toString(),
                        uri = uri,
                        name = name,
                        isPdf = isPdf,
                        size = size,
                        pageCount = pageCount
                    )
                }
            }
            list.addAll(newItems)
            _selectedMergeFiles.value = list
            _isProcessing.value = false
        }
    }

    fun removeFileFromMerge(fileId: String) {
        _selectedMergeFiles.value = _selectedMergeFiles.value.filter { it.id != fileId }
    }

    fun moveFileUpInMerge(index: Int) {
        if (index <= 0 || index >= _selectedMergeFiles.value.size) return
        val list = _selectedMergeFiles.value.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp
        _selectedMergeFiles.value = list
    }

    fun moveFileDownInMerge(index: Int) {
        if (index < 0 || index >= _selectedMergeFiles.value.size - 1) return
        val list = _selectedMergeFiles.value.toMutableList()
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp
        _selectedMergeFiles.value = list
    }

    fun clearMergeFiles() {
        _selectedMergeFiles.value = emptyList()
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun mergeSelectedFiles(context: Context) {
        if (_selectedMergeFiles.value.isEmpty()) return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Preparing PDF generation..."
            
            val outputDirectory = getOutputDirectory(context)
            
            val outputName = "merged_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            _processStatus.value = "Merging documents & scaling pages..."
            val success = PdfUtils.mergePdfAndImages(context, _selectedMergeFiles.value, outputFile)

            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "MERGE",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                _isProcessing.value = false
                
                openFile(context, outputFile)
            } else {
                _isProcessing.value = false
                _processStatus.value = "Merge failed. Check storage permissions."
            }
        }
    }

    // --- Split PDF state ---
    private val _splitSourceFile = MutableStateFlow<SelectableFile?>(null)
    val splitSourceFile: StateFlow<SelectableFile?> = _splitSourceFile.asStateFlow()

    // List of page thumbnails generated for the split file
    private val _splitPagePreviews = MutableStateFlow<List<Uri>>(emptyList()) // can also do indices

    private val _splitManualSelectedPages = MutableStateFlow<Set<Int>>(emptySet())
    val splitManualSelectedPages: StateFlow<Set<Int>> = _splitManualSelectedPages.asStateFlow()

    private val _splitRangeExpression = MutableStateFlow("")
    val splitRangeExpression: StateFlow<String> = _splitRangeExpression.asStateFlow()

    fun setSplitSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing PDF details..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val c = PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }
            
            _splitSourceFile.value = SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            _splitManualSelectedPages.value = emptySet()
            _splitRangeExpression.value = ""
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun toggleSplitPageSelection(pageNumber: Int) {
        val currentSet = _splitManualSelectedPages.value.toMutableSet()
        if (currentSet.contains(pageNumber)) {
            currentSet.remove(pageNumber)
        } else {
            currentSet.add(pageNumber)
        }
        _splitManualSelectedPages.value = currentSet
    }

    fun updateSplitRangeExpression(expr: String) {
        _splitRangeExpression.value = expr
    }

    fun clearSplitFile() {
        _splitSourceFile.value = null
        _splitManualSelectedPages.value = emptySet()
        _splitRangeExpression.value = ""
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun splitSourcePdf(context: Context, useManualSelection: Boolean) {
        val source = _splitSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Extracting chosen pages..."

            val pagesToExtract = if (useManualSelection) {
                _splitManualSelectedPages.value.sorted()
            } else {
                PdfUtils.parsePageExpression(_splitRangeExpression.value, source.pageCount)
            }

            if (pagesToExtract.isEmpty()) {
                _isProcessing.value = false
                _processStatus.value = "No pages selected to extract."
                return@launch
            }

            val outputDirectory = getOutputDirectory(context)

            val outputName = "split_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.splitPdf(context, source.uri, pagesToExtract, outputFile)

            if (success && outputFile.exists()) {
                val record = HistoryRecord(
                    toolType = "SPLIT",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = pagesToExtract.size
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                _isProcessing.value = false
                
                // Auto open
                openFile(context, outputFile)
            } else {
                _isProcessing.value = false
                _processStatus.value = "Split failed. Check ranges."
            }
        }
    }

    // --- PDF Password Tools state ---
    private val _passwordSourceFile = MutableStateFlow<SelectableFile?>(null)
    val passwordSourceFile: StateFlow<SelectableFile?> = _passwordSourceFile.asStateFlow()

    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    private val _recoveryTried = MutableStateFlow(0L)
    val recoveryTried: StateFlow<Long> = _recoveryTried.asStateFlow()

    private val _recoveryTotal = MutableStateFlow(0L)
    val recoveryTotal: StateFlow<Long> = _recoveryTotal.asStateFlow()

    private val _recoveryCandidate = MutableStateFlow("")
    val recoveryCandidate: StateFlow<String> = _recoveryCandidate.asStateFlow()

    private val _recoveredPassword = MutableStateFlow<String?>(null)
    val recoveredPassword: StateFlow<String?> = _recoveredPassword.asStateFlow()

    private val _passwordNotFound = MutableStateFlow(false)
    val passwordNotFound: StateFlow<Boolean> = _passwordNotFound.asStateFlow()

    private val _isPasswordProtected = MutableStateFlow(false)
    val isPasswordProtected: StateFlow<Boolean> = _isPasswordProtected.asStateFlow()

    private var recoveryJob: kotlinx.coroutines.Job? = null

    fun setPasswordSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing PDF Encryption..."
            val (name, size, info) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "locked_document.pdf"
                val s = getFileSize(context, uri)
                val inf = PdfDecrypter.getEncryptionInfo(context, uri)
                Triple(n, s, inf)
            }
            
            _passwordSourceFile.value = SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = 0 // Will resolve on decryption
            )
            _isPasswordProtected.value = info.isEncrypted
            _passwordNotFound.value = false
            _recoveredPassword.value = null
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isRecovering.value = false
            _isProcessing.value = false
        }
    }

    fun clearPasswordSource() {
        recoveryJob?.cancel()
        _passwordSourceFile.value = null
        _isPasswordProtected.value = false
        _passwordNotFound.value = false
        _recoveredPassword.value = null
        _isRecovering.value = false
        _recoveryTried.value = 0
        _recoveryTotal.value = 0
        _recoveryCandidate.value = ""
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun shareOriginalFile(context: Context, uri: Uri, fileName: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
                putExtra(android.content.Intent.EXTRA_TITLE, fileName)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Locked PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share locked PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    fun decryptAndSaveFile(context: Context, passwordString: String) {
        val file = _passwordSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Decrypting PDF contents..."
            
            val outputDirectory = getOutputDirectory(context)
            
            val cleanBaseName = file.name.substringBeforeLast(".")
            val outputName = "unlocked_${cleanBaseName}_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfDecrypter.decryptAndSave(context, file.uri, passwordString, outputFile)

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "UNLOCK",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _recoveredPassword.value = passwordString
                _passwordNotFound.value = false
                _operationCompleted.value = true
                
                // Auto open
                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Decryption failed. Incorrect password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- PDF Repair State ---
    private val _repairSourceFile = MutableStateFlow<SelectableFile?>(null)
    val repairSourceFile: StateFlow<SelectableFile?> = _repairSourceFile.asStateFlow()

    private val _repairStep = MutableStateFlow("")
    val repairStep: StateFlow<String> = _repairStep.asStateFlow()

    private val _repairProgress = MutableStateFlow(0f)
    val repairProgress: StateFlow<Float> = _repairProgress.asStateFlow()

    private val _repairResult = MutableStateFlow<PdfRepairer.RepairResult?>(null)
    val repairResult: StateFlow<PdfRepairer.RepairResult?> = _repairResult.asStateFlow()

    fun setRepairSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing damaged PDF file..."
            val (name, size) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "damaged.pdf"
                val s = getFileSize(context, uri)
                n to s
            }
            _repairSourceFile.value = SelectableFile(
                id = java.util.UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = 1
            )
            _repairStep.value = ""
            _repairProgress.value = 0f
            _repairResult.value = null
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun clearRepairFile() {
        _repairSourceFile.value = null
        _repairStep.value = ""
        _repairProgress.value = 0f
        _repairResult.value = null
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun repairCorruptPdf(context: Context) {
        val source = _repairSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Phase 1: Loading raw PDF binary streams..."
            _repairStep.value = "Phase 1: Loading raw PDF binary streams..."
            _repairProgress.value = 0.1f
            _repairResult.value = null

            val outputDirectory = getOutputDirectory(context)
            val cleanBaseName = source.name.substringBeforeLast(".")
            val outputName = "repaired_${cleanBaseName}_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val result = PdfRepairer.repairPdfFile(
                context = context,
                inputUri = source.uri,
                outputFile = outputFile
            ) { step, percentage ->
                _repairStep.value = step
                _repairProgress.value = percentage
                _processStatus.value = step
            }

            _isProcessing.value = false
            _repairResult.value = result

            if (result.success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "REPAIR",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                Toast.makeText(context, "PDF successfully repaired!", Toast.LENGTH_LONG).show()

                // Auto open
                openFile(context, outputFile)
            } else {
                _operationCompleted.value = false
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Watermark PDF State ---
    private val _watermarkMode = MutableStateFlow("ADD") // ADD, REMOVE, CHANGE
    val watermarkMode: StateFlow<String> = _watermarkMode.asStateFlow()
    fun updateWatermarkMode(mode: String) { _watermarkMode.value = mode }

    private val _watermarkSourceUri = MutableStateFlow<Uri?>(null)
    val watermarkSourceUri: StateFlow<Uri?> = _watermarkSourceUri.asStateFlow()

    private val _watermarkType = MutableStateFlow("TEXT") // TEXT, IMAGE
    val watermarkType: StateFlow<String> = _watermarkType.asStateFlow()
    fun updateWatermarkType(type: String) { _watermarkType.value = type }

    private val _watermarkStartPage = kotlinx.coroutines.flow.MutableStateFlow(1)
    val watermarkStartPage = _watermarkStartPage.asStateFlow()
    fun updateWatermarkStartPage(page: Int) { _watermarkStartPage.value = page }

    private val _watermarkEndPage = kotlinx.coroutines.flow.MutableStateFlow(-1)
    val watermarkEndPage = _watermarkEndPage.asStateFlow()
    fun updateWatermarkEndPage(page: Int) { _watermarkEndPage.value = page }

    private val _watermarkText = MutableStateFlow("CONFIDENTIAL")
    val watermarkText: StateFlow<String> = _watermarkText.asStateFlow()
    fun updateWatermarkText(text: String) { _watermarkText.value = text }

    private val _watermarkPosition = MutableStateFlow("MC") // TL, TC, TR, ML, MC, MR, BL, BC, BR
    val watermarkPosition: StateFlow<String> = _watermarkPosition.asStateFlow()
    fun updateWatermarkPosition(pos: String) { _watermarkPosition.value = pos }

    private val _watermarkTextSize = MutableStateFlow(48f)
    val watermarkTextSize: StateFlow<Float> = _watermarkTextSize.asStateFlow()
    fun updateWatermarkTextSize(size: Float) { _watermarkTextSize.value = size }

    private val _watermarkFont = MutableStateFlow("Helvetica")
    val watermarkFont: StateFlow<String> = _watermarkFont.asStateFlow()
    fun updateWatermarkFont(font: String) { _watermarkFont.value = font }

    private val _watermarkColor = MutableStateFlow("#000000") // Hex
    val watermarkColor: StateFlow<String> = _watermarkColor.asStateFlow()
    fun updateWatermarkColor(color: String) { _watermarkColor.value = color }

    private val _watermarkForeground = MutableStateFlow(true)
    val watermarkForeground: StateFlow<Boolean> = _watermarkForeground.asStateFlow()
    fun updateWatermarkForeground(fg: Boolean) { _watermarkForeground.value = fg }

    private val _watermarkRotation = MutableStateFlow(0f)
    val watermarkRotation: StateFlow<Float> = _watermarkRotation.asStateFlow()
    fun updateWatermarkRotation(rot: Float) { _watermarkRotation.value = rot }

    private val _watermarkImageUri = MutableStateFlow<Uri?>(null)
    val watermarkImageUri: StateFlow<Uri?> = _watermarkImageUri.asStateFlow()
    fun updateWatermarkImage(uri: Uri?) { _watermarkImageUri.value = uri }

    private val _watermarkOpacity = MutableStateFlow(0.5f)
    val watermarkOpacity: StateFlow<Float> = _watermarkOpacity.asStateFlow()
    fun updateWatermarkOpacity(op: Float) { _watermarkOpacity.value = op }

    private val _watermarkCropEnable = MutableStateFlow(false)
    val watermarkCropEnable: StateFlow<Boolean> = _watermarkCropEnable.asStateFlow()
    fun updateWatermarkCropEnable(en: Boolean) { _watermarkCropEnable.value = en }

    private val _watermarkPreviewBmp = MutableStateFlow<android.graphics.Bitmap?>(null)
    val watermarkPreviewBmp: StateFlow<android.graphics.Bitmap?> = _watermarkPreviewBmp.asStateFlow()

    fun setWatermarkSourceFile(context: Context, uri: Uri?) {
        _watermarkSourceUri.value = uri
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
        if (uri != null) {
            viewModelScope.launch {
                _watermarkPreviewBmp.value = PdfUtils.renderPageToBitmap(context, uri, 0)
            }
        } else {
            _watermarkPreviewBmp.value = null
        }
    }

    fun clearWatermark() {
        _watermarkSourceUri.value = null
        _watermarkImageUri.value = null
        _watermarkPreviewBmp.value = null
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun applyWatermarkOperation(context: Context) {
        val uri = _watermarkSourceUri.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = if (_watermarkMode.value == "REMOVE") "Analyzing & removing watermarks..." else "Applying custom watermark..."
            
            val success = withContext(Dispatchers.IO) {
                try {
                    val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Watermark")
                    if (!outDir.exists()) outDir.mkdirs()
                    val outFile = File(outDir, "Watermark_${System.currentTimeMillis()}.pdf")
                    val result = PdfUtils.processWatermark(
                        context = context,
                        sourceUri = uri,
                        mode = _watermarkMode.value,
                        type = _watermarkType.value,
                        text = _watermarkText.value,
                        position = _watermarkPosition.value,
                        textSize = _watermarkTextSize.value,
                        font = _watermarkFont.value,
                        colorHex = _watermarkColor.value,
                        foreground = _watermarkForeground.value,
                        rotation = _watermarkRotation.value,
                        imageUri = _watermarkImageUri.value,
                        opacity = _watermarkOpacity.value,
                        cropEnabled = _watermarkCropEnable.value,
                        startPage = _watermarkStartPage.value,
                        endPage = _watermarkEndPage.value,
                        outputFile = outFile
                    )
                    if (result) {
                        val rec = HistoryRecord(
                            toolType = if (_watermarkMode.value == "REMOVE") "REMOVE_WATERMARK" else "WATERMARK",
                            fileName = outFile.name,
                            filePath = outFile.absolutePath,
                            createdAt = System.currentTimeMillis(),
                            fileSize = outFile.length(),
                            pagesCount = PdfUtils.getPdfPageCount(context, uri)
                        )
                        repository.insertRecord(rec)
                        _lastGeneratedRecord.value = rec
                        openFile(context, outFile)
                    }
                    result
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            
            _isProcessing.value = false
            _operationCompleted.value = success
        }
    }
    // --- Organize PDF State ---
    private val _organizePages = MutableStateFlow<List<OrganizePageItem>>(emptyList())
    val organizePages: StateFlow<List<OrganizePageItem>> = _organizePages.asStateFlow()

    fun setOrganizeSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing PDF layout..."
            val (name, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val c = PdfUtils.getPdfPageCount(context, uri)
                n to c
            }
            
            val list = mutableListOf<OrganizePageItem>()
            for (i in 0 until count) {
                list.add(
                    OrganizePageItem(
                        id = UUID.randomUUID().toString(),
                        sourceUri = uri,
                        sourceName = name,
                        originalPageIndex = i
                    )
                )
            }
            _organizePages.value = list
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun addFilesToOrganize(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Appending chosen documents..."
            val currentList = _organizePages.value.toMutableList()
            val newItems = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val subList = mutableListOf<OrganizePageItem>()
                for (uri in uris) {
                    val name = getFileName(context, uri) ?: "doc.pdf"
                    val isPdf = name.lowercase().endsWith(".pdf") || getMimeType(context, uri)?.contains("pdf") == true
                    if (isPdf) {
                        val count = PdfUtils.getPdfPageCount(context, uri)
                        for (i in 0 until count) {
                            subList.add(
                                OrganizePageItem(
                                    id = UUID.randomUUID().toString(),
                                    sourceUri = uri,
                                    sourceName = name,
                                    originalPageIndex = i
                                )
                            )
                        }
                    }
                }
                subList
            }
            if (newItems.isNotEmpty() || uris.isEmpty()) {
                currentList.addAll(newItems)
                _organizePages.value = currentList
            } else {
                Toast.makeText(context, "Only PDF files are supported for organizing pages.", Toast.LENGTH_SHORT).show()
            }
            _isProcessing.value = false
        }
    }

    fun deletePageFromOrganize(itemId: String) {
        _organizePages.value = _organizePages.value.filter { it.id != itemId }
    }

    fun duplicatePageInOrganize(itemId: String) {
        val current = _organizePages.value
        val index = current.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val list = current.toMutableList()
            val original = list[index]
            list.add(index + 1, original.copy(id = UUID.randomUUID().toString()))
            _organizePages.value = list
        }
    }

    fun movePageInOrganize(fromIndex: Int, toIndex: Int) {
        val current = _organizePages.value
        if (fromIndex in current.indices && toIndex in current.indices) {
            val list = current.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _organizePages.value = list
        }
    }

    fun clearOrganize() {
        _organizePages.value = emptyList()
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun compileOrganizedPdf(context: Context) {
        val pages = _organizePages.value
        if (pages.isEmpty()) {
            Toast.makeText(context, "Cannot export an empty PDF.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Compiling new PDF layout..."
            
            val outputDirectory = getOutputDirectory(context)
            
            val outputName = "organized_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.organizeAndSavePdf(context, pages, outputFile)

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "ORGANIZE",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                
                // Auto open
                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Pdf compilation failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setRotatePdfSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing PDF for rotation..."
            
            val list = mutableListOf<OrganizePageItem>()
            val name = getFileName(context, uri) ?: "document.pdf"
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                val count = renderer.pageCount
                renderer.close()
                pfd.close()
                for (i in 0 until count) {
                    list.add(
                        OrganizePageItem(
                            id = java.util.UUID.randomUUID().toString(),
                            sourceUri = uri,
                            sourceName = name,
                            originalPageIndex = i,
                            rotationDegrees = 0f
                        )
                    )
                }
            }
            _organizePages.value = list
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun rotatePage(itemId: String, degrees: Float = 90f) {
        _organizePages.value = _organizePages.value.map {
            if (it.id == itemId) it.copy(rotationDegrees = (it.rotationDegrees + degrees) % 360f) else it
        }
    }

    fun rotateAllPages(degrees: Float = 90f) {
        _organizePages.value = _organizePages.value.map {
            it.copy(rotationDegrees = (it.rotationDegrees + degrees) % 360f)
        }
    }

    // --- Protect PDF state ---
    private val _protectSourceFile = MutableStateFlow<SelectableFile?>(null)
    val protectSourceFile: StateFlow<SelectableFile?> = _protectSourceFile.asStateFlow()

    private val _protectPassword = MutableStateFlow("")
    val protectPassword: StateFlow<String> = _protectPassword.asStateFlow()

    private val _protectConfirmPassword = MutableStateFlow("")
    val protectConfirmPassword: StateFlow<String> = _protectConfirmPassword.asStateFlow()

    fun setProtectSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing PDF for protection..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val c = PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }

            _protectSourceFile.value = SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            _protectPassword.value = ""
            _protectConfirmPassword.value = ""
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun updateProtectPasswords(password: String, confirm: String) {
        _protectPassword.value = password
        _protectConfirmPassword.value = confirm
    }

    fun clearProtectFile() {
        _protectSourceFile.value = null
        _protectPassword.value = ""
        _protectConfirmPassword.value = ""
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun executeProtectPdf(context: Context) {
        val source = _protectSourceFile.value ?: return
        if (_protectPassword.value != _protectConfirmPassword.value) {
            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }
        if (_protectPassword.value.isEmpty()) {
            Toast.makeText(context, "Password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Encrypting PDF..."

            val outputDirectory = getOutputDirectory(context)
            val outputName = "protected_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.protectPdf(context, source.uri, _protectPassword.value, outputFile)

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "PROTECT",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true

                // Cannot auto-open encrypted PDF as many viewers crash
            } else {
                Toast.makeText(context, "PDF encryption failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Pages Per Sheet state ---
    private val _ppsSourceFile = MutableStateFlow<SelectableFile?>(null)
    val ppsSourceFile: StateFlow<SelectableFile?> = _ppsSourceFile.asStateFlow()

    private val _ppsColumns = MutableStateFlow(1)
    val ppsColumns: StateFlow<Int> = _ppsColumns.asStateFlow()

    private val _ppsRows = MutableStateFlow(2)
    val ppsRows: StateFlow<Int> = _ppsRows.asStateFlow()

    private val _ppsIsPortrait = MutableStateFlow(true)
    val ppsIsPortrait: StateFlow<Boolean> = _ppsIsPortrait.asStateFlow()

    private val _ppsPageSize = MutableStateFlow("A4")
    val ppsPageSize: StateFlow<String> = _ppsPageSize.asStateFlow()

    private val _ppsMarginPercent = MutableStateFlow(0f)
    val ppsMarginPercent: StateFlow<Float> = _ppsMarginPercent.asStateFlow()

    private val _ppsAddBorder = MutableStateFlow(false)
    val ppsAddBorder: StateFlow<Boolean> = _ppsAddBorder.asStateFlow()

    private val _ppsBorderColor = MutableStateFlow("#000000")
    val ppsBorderColor: StateFlow<String> = _ppsBorderColor.asStateFlow()

    private val _ppsBorderWidth = MutableStateFlow(1f)
    val ppsBorderWidth: StateFlow<Float> = _ppsBorderWidth.asStateFlow()

    fun setPpsSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing PDF for Pages per Sheet..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val c = PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }

            _ppsSourceFile.value = SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun updatePpsLayout(columns: Int, rows: Int) {
        _ppsColumns.value = columns
        _ppsRows.value = rows
    }

    fun updatePpsOrientation(isPortrait: Boolean) {
        _ppsIsPortrait.value = isPortrait
    }

    fun updatePpsPageSize(size: String) {
        _ppsPageSize.value = size
    }

    fun updatePpsMarginPercent(percent: Float) {
        _ppsMarginPercent.value = percent
    }

    fun updatePpsAddBorder(add: Boolean) {
        _ppsAddBorder.value = add
    }

    fun updatePpsBorderColor(colorHex: String) {
        _ppsBorderColor.value = colorHex
    }

    fun updatePpsBorderWidth(width: Float) {
        _ppsBorderWidth.value = width
    }

    fun clearPpsFile() {
        _ppsSourceFile.value = null
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun compilePpsPdf(context: Context) {
        val source = _ppsSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Tiling pages per sheet..."

            val outputDirectory = getOutputDirectory(context)
            val outputName = "pages_per_sheet_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.compilePagesPerSheetPdf(
                context = context,
                sourceUri = source.uri,
                columns = _ppsColumns.value,
                rows = _ppsRows.value,
                isPortrait = _ppsIsPortrait.value,
                pageSize = _ppsPageSize.value,
                marginPercent = _ppsMarginPercent.value,
                addBorder = _ppsAddBorder.value,
                borderColorHex = _ppsBorderColor.value,
                borderWidthPt = _ppsBorderWidth.value,
                outputFile = outputFile
            )

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "PAGES_PER_SHEET",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Pages per Sheet compilation failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Metadata Remover state ---
    private val _metadataSourceFile = MutableStateFlow<SelectableFile?>(null)
    val metadataSourceFile: StateFlow<SelectableFile?> = _metadataSourceFile.asStateFlow()

    private val _extractedMetadata = MutableStateFlow<Map<String, String>>(emptyMap())
    val extractedMetadata: StateFlow<Map<String, String>> = _extractedMetadata.asStateFlow()

    private val _metaTitle = MutableStateFlow("")
    val metaTitle: StateFlow<String> = _metaTitle.asStateFlow()

    private val _metaAuthor = MutableStateFlow("")
    val metaAuthor: StateFlow<String> = _metaAuthor.asStateFlow()

    private val _metaSubject = MutableStateFlow("")
    val metaSubject: StateFlow<String> = _metaSubject.asStateFlow()

    private val _metaKeywords = MutableStateFlow("")
    val metaKeywords: StateFlow<String> = _metaKeywords.asStateFlow()

    private val _metaCreator = MutableStateFlow("")
    val metaCreator: StateFlow<String> = _metaCreator.asStateFlow()

    private val _metaProducer = MutableStateFlow("")
    val metaProducer: StateFlow<String> = _metaProducer.asStateFlow()

    fun setMetaTitle(value: String) { _metaTitle.value = value }
    fun setMetaAuthor(value: String) { _metaAuthor.value = value }
    fun setMetaSubject(value: String) { _metaSubject.value = value }
    fun setMetaKeywords(value: String) { _metaKeywords.value = value }
    fun setMetaCreator(value: String) { _metaCreator.value = value }
    fun setMetaProducer(value: String) { _metaProducer.value = value }

    fun setMetadataSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Reading PDF metadata..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val c = PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }

            val meta = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                PdfUtils.getPdfMetadata(context, uri)
            }

            _metadataSourceFile.value = SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            _extractedMetadata.value = meta
            _metaTitle.value = meta["Title"] ?: meta["title"] ?: ""
            _metaAuthor.value = meta["Author"] ?: meta["author"] ?: ""
            _metaSubject.value = meta["Subject"] ?: meta["subject"] ?: ""
            _metaKeywords.value = meta["Keywords"] ?: meta["keywords"] ?: ""
            _metaCreator.value = meta["Creator"] ?: meta["creator"] ?: ""
            _metaProducer.value = meta["Producer"] ?: meta["producer"] ?: ""
            
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun clearMetadataFile() {
        _metadataSourceFile.value = null
        _extractedMetadata.value = emptyMap()
        _metaTitle.value = ""
        _metaAuthor.value = ""
        _metaSubject.value = ""
        _metaKeywords.value = ""
        _metaCreator.value = ""
        _metaProducer.value = ""
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun stripPdfMetadata(context: Context) {
        val source = _metadataSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Stripping hidden metadata..."

            val outputDirectory = getOutputDirectory(context)
            val outputName = "metadata_stripped_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.removePdfMetadata(context, source.uri, outputFile)

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "METADATA_REMOVER",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Metadata removal failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveModifiedMetadata(context: Context, stripAllOthers: Boolean) {
        val source = _metadataSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Saving custom metadata metadata..."

            val outputDirectory = getOutputDirectory(context)
            val outputName = "metadata_edited_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.updatePdfMetadata(
                context = context,
                sourceUri = source.uri,
                title = _metaTitle.value,
                author = _metaAuthor.value,
                subject = _metaSubject.value,
                keywords = _metaKeywords.value,
                creator = _metaCreator.value,
                producer = _metaProducer.value,
                stripAllOthers = stripAllOthers,
                outputFile = outputFile
            )

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "METADATA_REMOVER",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true
                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Failed to update PDF metadata.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- PDF Editor States ---
    private val _editorSourceFile = MutableStateFlow<com.example.util.SelectableFile?>(null)
    val editorSourceFile: StateFlow<com.example.util.SelectableFile?> = _editorSourceFile.asStateFlow()

    private val _editorOverlays = MutableStateFlow<List<PdfOverlay>>(emptyList())
    val editorOverlays: StateFlow<List<PdfOverlay>> = _editorOverlays.asStateFlow()

    private val _editorPageIndex = MutableStateFlow(0)
    val editorPageIndex: StateFlow<Int> = _editorPageIndex.asStateFlow()

    fun setEditorSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Reading PDF..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val c = com.example.util.PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }

            _editorSourceFile.value = com.example.util.SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            _editorOverlays.value = emptyList()
            _editorPageIndex.value = 0
            _operationCompleted.value = false
            _lastGeneratedRecord.value = null
            _isProcessing.value = false
        }
    }

    fun clearEditorSourceFile() {
        _editorSourceFile.value = null
        _editorOverlays.value = emptyList()
        _editorPageIndex.value = 0
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun setEditorPageIndex(index: Int) {
        _editorPageIndex.value = index
    }

    fun addTextOverlay(text: String, color: Int = android.graphics.Color.BLACK) {
        val currentList = _editorOverlays.value.toMutableList()
        currentList.add(
            PdfOverlay.TextOverlay(
                id = UUID.randomUUID().toString(),
                pageIndex = _editorPageIndex.value,
                text = text,
                color = color,
                relativeX = 0.5f,
                relativeY = 0.5f,
                scale = 1.0f
            )
        )
        _editorOverlays.value = currentList
    }

    fun addImageOverlay(bitmap: android.graphics.Bitmap) {
        val currentList = _editorOverlays.value.toMutableList()
        currentList.add(
            PdfOverlay.ImageOverlay(
                id = UUID.randomUUID().toString(),
                pageIndex = _editorPageIndex.value,
                bitmap = bitmap,
                relativeX = 0.5f,
                relativeY = 0.5f,
                scale = 1.0f,
                rotation = 0f
            )
        )
        _editorOverlays.value = currentList
    }

    fun updateOverlayPosition(id: String, x: Float, y: Float) {
        val currentList = _editorOverlays.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = when (item) {
                is PdfOverlay.TextOverlay -> item.copy(relativeX = x, relativeY = y)
                is PdfOverlay.ImageOverlay -> item.copy(relativeX = x, relativeY = y)
            }
            _editorOverlays.value = currentList
        }
    }

    fun updateTextOverlayProps(id: String, text: String, color: Int, scale: Float) {
        val currentList = _editorOverlays.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1 && currentList[index] is PdfOverlay.TextOverlay) {
            currentList[index] = (currentList[index] as PdfOverlay.TextOverlay).copy(
                text = text,
                color = color,
                scale = scale.coerceIn(0.2f, 5.0f)
            )
            _editorOverlays.value = currentList
        }
    }

    fun updateImageOverlayProps(id: String, scale: Float, rotation: Float, opacity: Float) {
        val currentList = _editorOverlays.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1 && currentList[index] is PdfOverlay.ImageOverlay) {
            currentList[index] = (currentList[index] as PdfOverlay.ImageOverlay).copy(
                scale = scale.coerceIn(0.2f, 5.0f),
                rotation = rotation % 360f,
                opacity = opacity.coerceIn(0f, 1f)
            )
            _editorOverlays.value = currentList
        }
    }

    fun deleteOverlay(id: String) {
        val currentList = _editorOverlays.value.toMutableList()
        currentList.removeAll { it.id == id }
        _editorOverlays.value = currentList
    }

    fun exportEditedPdf(context: Context) {
        val fileInfo = _editorSourceFile.value ?: return
        val overlays = _editorOverlays.value
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _processStatus.value = "Applying edits to PDF..."

                val resultFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val cacheDir = context.cacheDir
                    var sanitizedName = fileInfo.name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    if (!sanitizedName.lowercase(java.util.Locale.getDefault()).endsWith(".pdf")) {
                        sanitizedName += ".pdf"
                    }
                    val fileName = "edited_${System.currentTimeMillis()}_$sanitizedName"
                    val outFile = File(cacheDir, fileName)

                    val success = com.example.util.PdfUtils.applyOverlaysToPdf(context, fileInfo.uri, outFile, overlays)
                    if (success) outFile else null
                }

                if (resultFile != null && resultFile.exists() && resultFile.length() > 0) {
                    val destFile = File(context.filesDir, resultFile.name)
                    resultFile.copyTo(destFile, overwrite = true)
                    
                    val record = HistoryRecord(
                        toolType = "PDF_EDITOR",
                        fileName = destFile.name,
                        filePath = destFile.absolutePath,
                        fileSize = destFile.length(),
                        pagesCount = fileInfo.pageCount
                    )
                    repository.insertRecord(record)
                    _lastGeneratedRecord.value = record
                    _operationCompleted.value = true
                } else {
                    Toast.makeText(context, "Failed to export edited document.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Editor error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // --- History actions ---
    fun deleteHistoryRecord(context: Context, record: HistoryRecord) {
        viewModelScope.launch {
            // Delete record
            repository.deleteRecordById(record.id)
            // also optionally delete the file on storage to keep desk clean!
            try {
                val file = File(record.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
        }
    }

    // --- Share & Open Wrappers ---
    fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                android.widget.Toast.makeText(context, "No PDF viewer app found.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareRecord(context: Context, record: HistoryRecord) {
        try {
            val file = File(record.filePath)
            if (!file.exists()) {
                android.widget.Toast.makeText(context, "File does not exist.", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Sharing PDF: ${record.fileName}")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share converted file"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error sharing file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // --- PDF Color Inverter ---
    private val _invertSourceFile = MutableStateFlow<com.example.util.SelectableFile?>(null)
    val invertSourceFile: StateFlow<com.example.util.SelectableFile?> = _invertSourceFile.asStateFlow()

    fun setInvertSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing selected file..."
            val (fileInfo, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val type = getMimeType(context, uri) ?: ""
                val isP = n.lowercase().endsWith(".pdf") || type.contains("pdf")
                val c = if (isP) PdfUtils.getPdfPageCount(context, uri) else 1
                Triple(n, s, isP) to c
            }
            val (name, size, isPdf) = fileInfo

            _invertSourceFile.value = com.example.util.SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = isPdf,
                size = size,
                pageCount = count
            )
            _isProcessing.value = false
        }
    }

    fun clearInvert() {
        _invertSourceFile.value = null
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun invertPdfColors(context: Context) {
        val source = _invertSourceFile.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Inverting PDF colors..."

            val outputDirectory = getOutputDirectory(context)

            val outputName = "inverted_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.invertPdfColor(context, source.uri, source.isPdf, outputFile)

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "INVERT",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true

                // Auto open
                openFile(context, outputFile)
            } else {
                android.widget.Toast.makeText(context, "Color inversion failed.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Signature Maker state ---
    private val _signaturePdfSource = MutableStateFlow<com.example.util.SelectableFile?>(null)
    val signaturePdfSource: StateFlow<com.example.util.SelectableFile?> = _signaturePdfSource.asStateFlow()

    private val _signatureImageUri = MutableStateFlow<Uri?>(null)
    val signatureImageUri: StateFlow<Uri?> = _signatureImageUri.asStateFlow()

    private val _signatureOriginalBitmap = MutableStateFlow<Bitmap?>(null)
    val signatureOriginalBitmap: StateFlow<Bitmap?> = _signatureOriginalBitmap.asStateFlow()

    private val _sigCropLeft = MutableStateFlow(0f)
    val sigCropLeft: StateFlow<Float> = _sigCropLeft.asStateFlow()
    private val _sigCropRight = MutableStateFlow(1f)
    val sigCropRight: StateFlow<Float> = _sigCropRight.asStateFlow()
    private val _sigCropTop = MutableStateFlow(0f)
    val sigCropTop: StateFlow<Float> = _sigCropTop.asStateFlow()
    private val _sigCropBottom = MutableStateFlow(1f)
    val sigCropBottom: StateFlow<Float> = _sigCropBottom.asStateFlow()

    private val _signaturePageIndex = MutableStateFlow(0)
    val signaturePageIndex: StateFlow<Int> = _signaturePageIndex.asStateFlow()

    private val _signatureRelativeX = MutableStateFlow(0.5f)
    val signatureRelativeX: StateFlow<Float> = _signatureRelativeX.asStateFlow()
    private val _signatureRelativeY = MutableStateFlow(0.5f)
    val signatureRelativeY: StateFlow<Float> = _signatureRelativeY.asStateFlow()

    private val _signatureScaleFactor = MutableStateFlow(1.0f)
    val signatureScaleFactor: StateFlow<Float> = _signatureScaleFactor.asStateFlow()

    private val _isSignatureDragOptionEnabled = MutableStateFlow(true)
    val isSignatureDragOptionEnabled: StateFlow<Boolean> = _isSignatureDragOptionEnabled.asStateFlow()

    // Advanced cryptographic signature settings
    private val _useCustomKeystore = MutableStateFlow(false)
    val useCustomKeystore: StateFlow<Boolean> = _useCustomKeystore.asStateFlow()

    private val _customKeystoreUri = MutableStateFlow<Uri?>(null)
    val customKeystoreUri: StateFlow<Uri?> = _customKeystoreUri.asStateFlow()

    private val _customKeystorePassword = MutableStateFlow("")
    val customKeystorePassword: StateFlow<String> = _customKeystorePassword.asStateFlow()

    private val _commonName = MutableStateFlow("")
    val commonName: StateFlow<String> = _commonName.asStateFlow()

    private val _organization = MutableStateFlow("")
    val organization: StateFlow<String> = _organization.asStateFlow()

    private val _organizationalUnit = MutableStateFlow("")
    val organizationalUnit: StateFlow<String> = _organizationalUnit.asStateFlow()

    private val _country = MutableStateFlow("")
    val country: StateFlow<String> = _country.asStateFlow()

    private val _sigReason = MutableStateFlow("")
    val sigReason: StateFlow<String> = _sigReason.asStateFlow()

    private val _sigLocation = MutableStateFlow("")
    val sigLocation: StateFlow<String> = _sigLocation.asStateFlow()

    private val _sigContact = MutableStateFlow("")
    val sigContact: StateFlow<String> = _sigContact.asStateFlow()

    private val _sigGraphicType = MutableStateFlow("IMAGE") // NONE, IMAGE, CHECKMARK, SEAL
    val sigGraphicType: StateFlow<String> = _sigGraphicType.asStateFlow()

    private val _sigShowDetailsText = MutableStateFlow(true)
    val sigShowDetailsText: StateFlow<Boolean> = _sigShowDetailsText.asStateFlow()

    private val _sigTimeSource = MutableStateFlow("LOCAL") // LOCAL, TSA
    val sigTimeSource: StateFlow<String> = _sigTimeSource.asStateFlow()

    private val _tsaServerUrl = MutableStateFlow("http://timestamp.digicert.com")
    val tsaServerUrl: StateFlow<String> = _tsaServerUrl.asStateFlow()

    // Advanced signature setters
    fun setUseCustomKeystore(valEnabled: Boolean) { _useCustomKeystore.value = valEnabled }
    fun setCustomKeystoreUri(uri: Uri?) { _customKeystoreUri.value = uri }
    fun setCustomKeystorePassword(pwd: String) { _customKeystorePassword.value = pwd }
    fun setCommonName(name: String) { _commonName.value = name }
    fun setOrganization(org: String) { _organization.value = org }
    fun setOrganizationalUnit(ou: String) { _organizationalUnit.value = ou }
    fun setCountry(c: String) { _country.value = c }
    fun setSigReason(r: String) { _sigReason.value = r }
    fun setSigLocation(loc: String) { _sigLocation.value = loc }
    fun setSigContact(c: String) { _sigContact.value = c }
    fun setSigGraphicType(type: String) { _sigGraphicType.value = type }
    fun setSigShowDetailsText(show: Boolean) { _sigShowDetailsText.value = show }
    fun setSigTimeSource(source: String) { _sigTimeSource.value = source }
    fun setTsaServerUrl(url: String) { _tsaServerUrl.value = url }

    fun setSignaturePdfSource(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing selected PDF..."
            val (fileInfo, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val type = getMimeType(context, uri) ?: ""
                val isP = n.lowercase().endsWith(".pdf") || type.contains("pdf")
                val c = if (isP) PdfUtils.getPdfPageCount(context, uri) else 1
                Triple(n, s, isP) to c
            }
            val (name, size, isPdf) = fileInfo

            _signaturePdfSource.value = com.example.util.SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = isPdf,
                size = size,
                pageCount = count
            )
            _signaturePageIndex.value = 0
            _isProcessing.value = false
        }
    }

    fun setSignatureImageUri(context: Context, uri: Uri) {
        _signatureImageUri.value = uri
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Loading signature image..."
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream, null, options)
                    }
                    var scale = 1
                    val maxSize = 1024
                    while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) {
                        scale *= 2
                    }
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = scale }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
                        _signatureOriginalBitmap.value = bmp
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _sigCropLeft.value = 0f
            _sigCropRight.value = 1f
            _sigCropTop.value = 0f
            _sigCropBottom.value = 1f
            _isProcessing.value = false
        }
    }

    fun setSigCropLeft(value: Float) { _sigCropLeft.value = value }
    fun setSigCropRight(value: Float) { _sigCropRight.value = value }
    fun setSigCropTop(value: Float) { _sigCropTop.value = value }
    fun setSigCropBottom(value: Float) { _sigCropBottom.value = value }

    fun setSignaturePageIndex(index: Int) { _signaturePageIndex.value = index }
    fun setSignatureRelativeX(x: Float) { _signatureRelativeX.value = x }
    fun setSignatureRelativeY(y: Float) { _signatureRelativeY.value = y }
    fun setSignatureScaleFactor(scale: Float) { _signatureScaleFactor.value = scale }
    fun setSignatureDragOptionEnabled(enabled: Boolean) { _isSignatureDragOptionEnabled.value = enabled }

    fun getCroppedSignatureBitmap(): Bitmap? {
        val original = _signatureOriginalBitmap.value ?: return null
        val leftFrac = _sigCropLeft.value
        val rightFrac = _sigCropRight.value
        val topFrac = _sigCropTop.value
        val bottomFrac = _sigCropBottom.value

        if (leftFrac == 0f && rightFrac == 1f && topFrac == 0f && bottomFrac == 1f) {
            return original
        }

        val origW = original.width
        val origH = original.height

        val cropX = (leftFrac * origW).toInt().coerceIn(0, origW - 1)
        val cropY = (topFrac * origH).toInt().coerceIn(0, origH - 1)
        val cropW = (origW - cropX - (rightFrac * origW).toInt()).coerceIn(1, origW - cropX)
        val cropH = (origH - cropY - (bottomFrac * origH).toInt()).coerceIn(1, origH - cropY)

        return try {
            Bitmap.createBitmap(original, cropX, cropY, cropW, cropH)
        } catch (e: Exception) {
            original
        }
    }

    fun clearSignatureMaker() {
        _signaturePdfSource.value = null
        _signatureImageUri.value = null
        _signatureOriginalBitmap.value = null
        _sigCropLeft.value = 0f
            _sigCropRight.value = 1f
            _sigCropTop.value = 0f
            _sigCropBottom.value = 1f
        _signaturePageIndex.value = 0
        _signatureRelativeX.value = 0.5f
        _signatureRelativeY.value = 0.5f
        _signatureScaleFactor.value = 1.0f
        _isSignatureDragOptionEnabled.value = true
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
        _useCustomKeystore.value = false
        _customKeystoreUri.value = null
        _customKeystorePassword.value = ""
        _commonName.value = ""
        _organization.value = ""
        _organizationalUnit.value = ""
        _country.value = ""
        _sigReason.value = ""
        _sigLocation.value = ""
        _sigContact.value = ""
        _sigGraphicType.value = "IMAGE"
        _sigShowDetailsText.value = true
        _sigTimeSource.value = "LOCAL"
        _tsaServerUrl.value = "http://timestamp.digicert.com"
    }

    fun generateSignedPdf(context: Context) {
        val pdfFile = _signaturePdfSource.value ?: return
        
        if (_sigGraphicType.value == "IMAGE" && _signatureOriginalBitmap.value == null) {
            android.widget.Toast.makeText(context, "Please select/upload a Signature Image first.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        _isProcessing.value = true
        _processStatus.value = "Creating cryptographically verified digital signature..."

        viewModelScope.launch {
            val croppedBitmap = if (_sigGraphicType.value == "IMAGE") {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        _signatureOriginalBitmap.value?.let { getCroppedSignatureBitmap() ?: it }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        _signatureOriginalBitmap.value
                    }
                }
            } else {
                null
            }

            val outputDirectory = getOutputDirectory(context)
            val outputName = "signed_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = try {
                com.example.util.SignatureUtils.digitallySignPdf(
                    context = context,
                    pdfUri = pdfFile.uri,
                    outputFile = outputFile,
                    useCustomKeystore = _useCustomKeystore.value,
                    customKeystoreUri = _customKeystoreUri.value,
                    customKeystorePassword = java.lang.String(_customKeystorePassword.value),
                    commonName = _commonName.value,
                    organization = _organization.value,
                    orgUnit = _organizationalUnit.value,
                    country = _country.value,
                    reason = _sigReason.value,
                    location = _sigLocation.value,
                    contact = _sigContact.value,
                    pageIndex = _signaturePageIndex.value,
                    relativeX = _signatureRelativeX.value,
                    relativeY = _signatureRelativeY.value,
                    scale = _signatureScaleFactor.value,
                    graphicType = _sigGraphicType.value,
                    signatureBitmap = croppedBitmap,
                    showDetailsText = _sigShowDetailsText.value,
                    timeSource = _sigTimeSource.value,
                    tsaUrl = _tsaServerUrl.value
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                false
            }

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "SIGN",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true

                openFile(context, outputFile)
            } else {
                android.widget.Toast.makeText(context, "Signing PDF failed. Please verify Keystore or TSA Server URL.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Filter PDF state ---
    private val _filterSourceFile = MutableStateFlow<com.example.util.SelectableFile?>(null)
    val filterSourceFile: StateFlow<com.example.util.SelectableFile?> = _filterSourceFile.asStateFlow()

    private val _globalFilterMode = MutableStateFlow("OMNIFIX")
    val globalFilterMode: StateFlow<String> = _globalFilterMode.asStateFlow()

    private val _pageAdjustments = MutableStateFlow<Map<Int, com.example.util.ManualAdjustSettings>>(emptyMap())
    val pageAdjustments: StateFlow<Map<Int, com.example.util.ManualAdjustSettings>> = _pageAdjustments.asStateFlow()

    private val _filterPageOrder = MutableStateFlow<List<Int>>(emptyList())
    val filterPageOrder: StateFlow<List<Int>> = _filterPageOrder.asStateFlow()

    private val _isFilterPageDragEnabled = MutableStateFlow(true)
    val isFilterPageDragEnabled: StateFlow<Boolean> = _isFilterPageDragEnabled.asStateFlow()

    fun setFilterSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing chosen PDF document..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "scanned_doc.pdf"
                val s = getFileSize(context, uri)
                val c = PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }

            _filterSourceFile.value = com.example.util.SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            _globalFilterMode.value = "OMNIFIX"
            _pageAdjustments.value = emptyMap()
            _filterPageOrder.value = (0 until count).toList()
            _isProcessing.value = false
        }
    }

    fun setGlobalFilterMode(mode: String) {
        _globalFilterMode.value = mode
    }

    fun updatePageAdjustment(pageIndex: Int, brightness: Float, contrast: Float, overrideMode: String?) {
        val currentAdjusts = _pageAdjustments.value.toMutableMap()
        currentAdjusts[pageIndex] = com.example.util.ManualAdjustSettings(
            brightness = brightness,
            contrast = contrast,
            overrideMode = overrideMode
        )
        _pageAdjustments.value = currentAdjusts
    }

    fun setFilterPageDragEnabled(enabled: Boolean) {
        _isFilterPageDragEnabled.value = enabled
    }

    fun moveFilterPageUp(index: Int) {
        if (index <= 0 || index >= _filterPageOrder.value.size) return
        val list = _filterPageOrder.value.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp
        _filterPageOrder.value = list
    }

    fun moveFilterPageDown(index: Int) {
        if (index < 0 || index >= _filterPageOrder.value.size - 1) return
        val list = _filterPageOrder.value.toMutableList()
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp
        _filterPageOrder.value = list
    }

    fun clearFilterPdf() {
        _filterSourceFile.value = null
        _globalFilterMode.value = "OMNIFIX"
        _pageAdjustments.value = emptyMap()
        _filterPageOrder.value = emptyList()
        _isFilterPageDragEnabled.value = true
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun generateFilteredPdf(context: Context) {
        val pdfFile = _filterSourceFile.value ?: return
        _isProcessing.value = true
        _processStatus.value = "Applying page-by-page filters..."

        viewModelScope.launch {
            val outputDirectory = getOutputDirectory(context)

            val outputName = "filtered_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.applyFiltersToPdf(
                context = context,
                pdfUri = pdfFile.uri,
                globalMode = _globalFilterMode.value,
                pageAdjustments = _pageAdjustments.value,
                pageOrderList = _filterPageOrder.value,
                outputFile = outputFile
            )

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "FILTER",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true

                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Filtering PDF failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- PDF Resize Tools state ---
    private val _resizeSourceFile = MutableStateFlow<com.example.util.SelectableFile?>(null)
    val resizeSourceFile: StateFlow<com.example.util.SelectableFile?> = _resizeSourceFile.asStateFlow()

    private val _resizeTargetValue = MutableStateFlow("1.0")
    val resizeTargetValue: StateFlow<String> = _resizeTargetValue.asStateFlow()

    private val _resizeTargetUnit = MutableStateFlow("MB")
    val resizeTargetUnit: StateFlow<String> = _resizeTargetUnit.asStateFlow()

    private val _resizeMode = MutableStateFlow("COMPRESS") // "COMPRESS" or "ENLARGE"
    val resizeMode: StateFlow<String> = _resizeMode.asStateFlow()

    fun setResizeSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing chosen PDF document..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val c = PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }
            _resizeSourceFile.value = com.example.util.SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            // Reset values
            _resizeTargetValue.value = "1.0"
            _resizeTargetUnit.value = "MB"
            _resizeMode.value = "COMPRESS"
            _isProcessing.value = false
        }
    }

    fun setResizeTargetValue(value: String) {
        _resizeTargetValue.value = value
    }

    fun setResizeTargetUnit(unit: String) {
        _resizeTargetUnit.value = unit
    }

    fun setResizeMode(mode: String) {
        _resizeMode.value = mode
    }

    fun clearResizeFile() {
        _resizeSourceFile.value = null
        _resizeTargetValue.value = "1.0"
        _resizeTargetUnit.value = "MB"
        _resizeMode.value = "COMPRESS"
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    // --- PDF Page Numbering state ---
    private val _pageNumberSourceFile = MutableStateFlow<com.example.util.SelectableFile?>(null)
    val pageNumberSourceFile: StateFlow<com.example.util.SelectableFile?> = _pageNumberSourceFile.asStateFlow()

    private val _pageNumberPosition = MutableStateFlow("Bottom Center")
    val pageNumberPosition: StateFlow<String> = _pageNumberPosition.asStateFlow()

    private val _pageNumberColor = MutableStateFlow("#000000")
    val pageNumberColor: StateFlow<String> = _pageNumberColor.asStateFlow()

    private val _pageNumberAlpha = MutableStateFlow(1.0f)
    val pageNumberAlpha: StateFlow<Float> = _pageNumberAlpha.asStateFlow()

    private val _pageNumberFontSize = MutableStateFlow(8)
    val pageNumberFontSize: StateFlow<Int> = _pageNumberFontSize.asStateFlow()

    private val _pageNumberFontFamily = MutableStateFlow("Sans") // Sans, Serif, Mono
    val pageNumberFontFamily: StateFlow<String> = _pageNumberFontFamily.asStateFlow()

    private val _pageNumberRanges = MutableStateFlow<List<com.example.util.PageNumberRange>>(emptyList())
    val pageNumberRanges: StateFlow<List<com.example.util.PageNumberRange>> = _pageNumberRanges.asStateFlow()

    fun setPageNumberSourceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Analyzing PDF for page count..."
            val (name, size, count) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val n = getFileName(context, uri) ?: "document.pdf"
                val s = getFileSize(context, uri)
                val c = PdfUtils.getPdfPageCount(context, uri)
                Triple(n, s, c)
            }
            _pageNumberSourceFile.value = com.example.util.SelectableFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                isPdf = true,
                size = size,
                pageCount = count
            )
            // Initialize with default Option Box 1 as specified
            _pageNumberRanges.value = listOf(
                com.example.util.PageNumberRange(
                    startPage = 1,
                    endPage = count,
                    pageTypePattern = "Page : {NUM}/{CNT}",
                    numeralsType = "Numeric",
                    startingInput = 1
                )
            )
            _isProcessing.value = false
        }
    }

    fun setPageNumberPosition(pos: String) {
        _pageNumberPosition.value = pos
    }

    fun setPageNumberColor(colorHex: String) {
        _pageNumberColor.value = colorHex
    }

    fun setPageNumberAlpha(alpha: Float) {
        _pageNumberAlpha.value = alpha
    }

    fun setPageNumberFontSize(size: Int) {
        _pageNumberFontSize.value = size
    }

    fun setPageNumberFontFamily(family: String) {
        _pageNumberFontFamily.value = family
    }

    fun addPageNumberRange() {
        val pageCount = _pageNumberSourceFile.value?.pageCount ?: 1
        val current = _pageNumberRanges.value
        val defaultStart = if (current.isNotEmpty()) {
            (current.last().endPage + 1).coerceAtMost(pageCount)
        } else {
            1
        }
        val defaultEnd = pageCount
        _pageNumberRanges.value = current + com.example.util.PageNumberRange(
            startPage = defaultStart,
            endPage = defaultEnd,
            pageTypePattern = "Page : {NUM}/{CNT}",
            numeralsType = "Numeric",
            startingInput = defaultStart
        )
    }

    fun removePageNumberRange(id: String) {
        _pageNumberRanges.value = _pageNumberRanges.value.filter { it.id != id }
    }

    fun updatePageNumberRange(updated: com.example.util.PageNumberRange) {
        _pageNumberRanges.value = _pageNumberRanges.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    fun clearPageNumberSource() {
        _pageNumberSourceFile.value = null
        _pageNumberRanges.value = emptyList()
        _pageNumberPosition.value = "Bottom Center"
        _pageNumberColor.value = "#000000"
        _pageNumberAlpha.value = 1.0f
        _pageNumberFontSize.value = 8
        _pageNumberFontFamily.value = "Sans"
        _operationCompleted.value = false
        _lastGeneratedRecord.value = null
    }

    fun autoResolveRanges() {
        val count = _pageNumberSourceFile.value?.pageCount ?: return
        val current = _pageNumberRanges.value
        if (current.isEmpty()) return

        // Sort ranges by startPage
        val sorted = current.sortedBy { it.startPage }
        val newRanges = mutableListOf<com.example.util.PageNumberRange>()

        var currentStart = 1
        for ((idx, r) in sorted.withIndex()) {
            if (currentStart > count) break

            val isLast = (idx == sorted.lastIndex)
            val computedEnd = if (isLast) {
                count
            } else {
                val nextStart = sorted[idx + 1].startPage
                if (nextStart > currentStart) {
                    (nextStart - 1).coerceAtMost(count)
                } else {
                    val pagesLeft = count - currentStart + 1
                    val boxesLeft = sorted.size - idx
                    val avgSize = (pagesLeft / boxesLeft).coerceAtLeast(1)
                    (currentStart + avgSize - 1).coerceAtMost(count)
                }
            }

            // Adjust starting input to be relative or fit
            val recommendedStartInput = if (r.numeralsType.startsWith("Roman") || r.numeralsType.startsWith("Alphabetic")) 1 else currentStart

            newRanges.add(r.copy(
                startPage = currentStart,
                endPage = computedEnd,
                startingInput = if (r.startingInput == r.startPage) recommendedStartInput else r.startingInput
            ))
            currentStart = computedEnd + 1
        }

        _pageNumberRanges.value = newRanges
    }

    fun generateNumberedPdf(context: Context) {
        val srcFile = _pageNumberSourceFile.value ?: return
        val rangesList = _pageNumberRanges.value
        if (rangesList.isEmpty()) {
            Toast.makeText(context, "Please configure at least one page numbering option range.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate ranges page-bounds
        val pageCount = srcFile.pageCount
        for ((idx, r) in rangesList.withIndex()) {
            val label = "Option Box ${idx + 1}"
            if (r.startPage <= 0 || r.endPage <= 0) {
                Toast.makeText(context, "Invalid page numbers in $label. Ensure values are positive.", Toast.LENGTH_LONG).show()
                return
            }
            if (r.startPage > pageCount || r.endPage > pageCount) {
                Toast.makeText(context, "Page ranges in $label exceed the total pages of the document ($pageCount).", Toast.LENGTH_LONG).show()
                return
            }
            if (r.startPage > r.endPage) {
                Toast.makeText(context, "Start page must be less than or equal to End page in $label.", Toast.LENGTH_LONG).show()
                return
            }
        }

        // Overlap validation
        val pageToBox = mutableMapOf<Int, Int>()
        for ((idx, r) in rangesList.withIndex()) {
            val boxNum = idx + 1
            for (p in r.startPage..r.endPage) {
                if (pageToBox.containsKey(p)) {
                    val other = pageToBox[p]!!
                    Toast.makeText(context, "Overlap Error: Page $p is assigned to multiple Option Boxes ($other and $boxNum). Please adjust your start/end pages to prevent both numbering types on the same page.", Toast.LENGTH_LONG).show()
                    return
                }
                pageToBox[p] = boxNum
            }
        }

        _isProcessing.value = true
        _processStatus.value = "Applying custom page numbering system to PDF..."

        viewModelScope.launch {
            val outputDirectory = getOutputDirectory(context)
            val outputName = "numbered_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.addPageNumbersToPdf(
                context = context,
                sourceUri = srcFile.uri,
                ranges = rangesList,
                position = _pageNumberPosition.value,
                colorHex = _pageNumberColor.value,
                alphaValue = _pageNumberAlpha.value,
                fontSize = _pageNumberFontSize.value,
                fontFamily = _pageNumberFontFamily.value,
                outputFile = outputFile
            )

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "PAGENUMBER",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true

                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Page numbering failed. Ensure source file is valid.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun generateResizedPdf(context: Context) {
        val srcFile = _resizeSourceFile.value ?: return
        val targetVal = _resizeTargetValue.value.toFloatOrNull() ?: 1.0f
        val targetUnit = _resizeTargetUnit.value
        val mode = _resizeMode.value

        _isProcessing.value = true
        _processStatus.value = "Resizing PDF & packaging content..."

        viewModelScope.launch {
            val outputDirectory = getOutputDirectory(context)
            val isCompress = mode.equals("COMPRESS", ignoreCase = true)
            val prefName = if (isCompress) "compressed" else "enlarged"
            val outputName = "${prefName}_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDirectory, outputName)

            val success = PdfUtils.resizePdf(
                context = context,
                pdfUri = srcFile.uri,
                targetValue = targetVal,
                targetUnit = targetUnit,
                mode = mode,
                outputFile = outputFile
            )

            _isProcessing.value = false
            if (success && outputFile.exists()) {
                val totalPages = countPdfPages(context, outputFile)
                val record = HistoryRecord(
                    toolType = "RESIZE",
                    fileName = outputName,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    pagesCount = totalPages
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                _operationCompleted.value = true

                openFile(context, outputFile)
            } else {
                Toast.makeText(context, "Resizing PDF failed. Please try a larger target size.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Media Helpers ---
    private fun getOutputDirectory(context: Context): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OmniPdf CS")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (!dir.exists()) {
            val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "OmniPdf CS")
            if (!fallback.exists()) {
                fallback.mkdirs()
            }
            return fallback
        }
        return dir
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        var size: Long = 0
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) {
                        size = cursor.getLong(index)
                    }
                }
            }
        }
        return size
    }

    private fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    private fun countPdfPages(context: Context, file: File): Int {
        return try {
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    // --- Stamp Badge State & Operations ---
    data class StampData(val text: String, val customImageUri: Uri?, val x: Float, val y: Float, val size: Float = 1f)
    val stampData = androidx.compose.runtime.mutableStateMapOf<Int, MutableList<StampData>>()

    fun clearStamps() {
        stampData.clear()
    }

    fun addStamp(pageIndex: Int, text: String, customImageUri: Uri?, x: Float, y: Float, size: Float = 1f) {
        val currentStamps = stampData[pageIndex] ?: mutableListOf()
        currentStamps.add(StampData(text, customImageUri, x, y, size))
        stampData[pageIndex] = currentStamps
        // Force state update map
        val ol = stampData[pageIndex]
        stampData.remove(pageIndex)
        stampData[pageIndex] = ol!!
    }

    fun removeStamp(pageIndex: Int, index: Int) {
        val list = stampData[pageIndex] ?: mutableListOf()
        if (index < list.size) list.removeAt(index)
        stampData.remove(pageIndex)
        stampData[pageIndex] = list
    }

    fun processStampsOnPdf(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Applying stamps..."
            try {
                val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "StampBadge")
                if (!outDir.exists()) outDir.mkdirs()
                val outFile = File(outDir, "Stamped_${System.currentTimeMillis()}.pdf")
                
                val sourceFile = File(context.cacheDir, "stamp_source.pdf")
                sourceFile.delete()
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    sourceFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Map state to plain data for PdfUtils
                val simpleStampMap = stampData.mapValues { entry -> 
                    entry.value.map { com.example.util.PdfUtils.StampInfo(it.text, it.customImageUri?.toString(), it.x, it.y) } 
                }

                val success = com.example.util.PdfUtils.applyStamps(context, sourceFile, outFile, simpleStampMap)

                if (success && outFile.exists() && outFile.length() > 0) {
                    val totalPages = countPdfPages(context, outFile)
                    val rec = HistoryRecord(
                        toolType = "STAMP_BADGE",
                        fileName = outFile.name,
                        filePath = outFile.absolutePath,
                        fileSize = outFile.length(),
                        pagesCount = totalPages
                    )
                    repository.insertRecord(rec)
                    _processStatus.value = "Stamps applied successfully!"
                    navigateTo("HOME")
                    openFile(context, outFile)
                } else {
                    _processStatus.value = "Failed to apply stamps."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _processStatus.value = "Error: \${e.message}"
            } finally {
                kotlinx.coroutines.delay(1000)
                _isProcessing.value = false
                _processStatus.value = ""
            }
        }
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
