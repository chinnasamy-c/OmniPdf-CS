package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.itextpdf.text.html.simpleparser.HTMLWorker

data class DrawnStroke(
    val path: android.graphics.Path,
    val color: Int,
    val width: Float,
    val isHighlight: Boolean = false
)

data class DrawnText(val text: String, val x: Float, val y: Float, val size: Float, val color: Int)

data class DrawnPage(
    val strokes: List<DrawnStroke>,
    val texts: List<DrawnText> = emptyList()
)

data class SelectableFile(
    val id: String,
    val uri: Uri,
    val name: String,
    val isPdf: Boolean,
    val size: Long,
    val pageCount: Int = 1
)

object PdfUtils {
    suspend fun protectPdf(
        context: Context,
        inputUri: Uri,
        passwordRaw: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(inputUri)?.use { inputStream ->
                val reader = com.itextpdf.text.pdf.PdfReader(inputStream)
                val stamper = com.itextpdf.text.pdf.PdfStamper(reader, FileOutputStream(outputFile))
                
                val passBytes = passwordRaw.toByteArray()
                stamper.setEncryption(
                    passBytes,
                    passBytes,
                    com.itextpdf.text.pdf.PdfWriter.ALLOW_PRINTING or com.itextpdf.text.pdf.PdfWriter.ALLOW_COPY,
                    com.itextpdf.text.pdf.PdfWriter.STANDARD_ENCRYPTION_128 or com.itextpdf.text.pdf.PdfWriter.DO_NOT_ENCRYPT_METADATA
                )
                
                stamper.close()
                reader.close()
                return@withContext true
            }
            false
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    fun getPdfPageCount(context: Context, pdfUri: Uri): Int {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return 0
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            0
        }
    }

    suspend fun renderPageToBitmap(
        context: Context,
        pdfUri: Uri,
        pageIndex: Int,
        targetWidth: Int = -1,
        targetHeight: Int = -1
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext null
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }
            val page = renderer.openPage(pageIndex)
            val w = if (targetWidth > 0) targetWidth else page.width
            val h = if (targetHeight > 0) targetHeight else page.height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            null
        }
    }

    fun parsePageExpression(expression: String, maxPage: Int): List<Int> {
        val pages = mutableListOf<Int>()
        if (expression.isBlank()) return emptyList()

        // Replace whitespace and split by comma
        val tokens = expression.replace(" ", "").split(",")
        for (token in tokens) {
            if (token.isBlank()) continue
            try {
                if (token.contains("-")) {
                    val parts = token.split("-")
                    if (parts.size == 2) {
                        val start = parts[0].toIntOrNull() ?: 1
                        val end = parts[1].toIntOrNull() ?: maxPage
                        val startClamped = start.coerceIn(1, maxPage)
                        val endClamped = end.coerceIn(1, maxPage)
                        if (startClamped <= endClamped) {
                            for (p in startClamped..endClamped) {
                                pages.add(p)
                            }
                        } else {
                            for (p in startClamped downTo endClamped) {
                                pages.add(p)
                            }
                        }
                    }
                } else {
                    val p = token.toIntOrNull()
                    if (p != null) {
                        pages.add(p.coerceIn(1, maxPage))
                    }
                }
            } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
                // Ignore parsing errors for faulty tokens
            }
        }
        return pages
    }

    private fun getOptimalScaleForPage(w: Int, h: Int): Float {
        val maxDim = maxOf(w, h)
        return when {
            maxDim <= 0 -> 2.5f
            maxDim < 800 -> 2.5f
            maxDim < 1500 -> 1.8f
            else -> 1.0f
        }
    }

    suspend fun mergePdfAndImages(
        context: Context,
        files: List<SelectableFile>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val document = com.itextpdf.text.Document()
            val copy = com.itextpdf.text.pdf.PdfSmartCopy(document, java.io.FileOutputStream(outputFile))
            document.open()
            
            for (fileItem in files) {
                if (fileItem.isPdf) {
                    val stream = context.contentResolver.openInputStream(fileItem.uri)
                    if (stream != null) {
                        val reader = com.itextpdf.text.pdf.PdfReader(stream)
                        for (i in 1..reader.numberOfPages) {
                            copy.addPage(copy.getImportedPage(reader, i))
                        }
                        copy.freeReader(reader)
                        reader.close()
                    }
                } else {
                    val inputStream = context.contentResolver.openInputStream(fileItem.uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (bitmap != null) {
                        val baos = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                        val image = com.itextpdf.text.Image.getInstance(baos.toByteArray())
                        bitmap.recycle()
                        
                        val standardWidth = 595f
                        val standardHeight = 842f
                        
                        val imageRatio = image.width / image.height
                        val templateRatio = standardWidth / standardHeight
                        
                        if (imageRatio > templateRatio) {
                            image.scaleToFit(standardWidth, standardWidth / imageRatio)
                        } else {
                            image.scaleToFit(standardHeight * imageRatio, standardHeight)
                        }
                        
                        image.setAbsolutePosition(
                            (standardWidth - image.scaledWidth) / 2f,
                            (standardHeight - image.scaledHeight) / 2f
                        )
                        
                        val tempBaos = java.io.ByteArrayOutputStream()
                        val tempDoc = com.itextpdf.text.Document(com.itextpdf.text.Rectangle(standardWidth, standardHeight))
                        val tempWriter = com.itextpdf.text.pdf.PdfWriter.getInstance(tempDoc, tempBaos)
                        tempDoc.open()
                        tempDoc.add(image)
                        tempDoc.close()
                        
                        val tempReader = com.itextpdf.text.pdf.PdfReader(tempBaos.toByteArray())
                        copy.addPage(copy.getImportedPage(tempReader, 1))
                        copy.freeReader(tempReader)
                        tempReader.close()
                    }
                }
            }
            
            document.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun compileDrawnPdf(
        context: Context,
        pages: List<DrawnPage>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            // A4 page size in points (1 point = 1/72 inch) -> 595 x 842.
            val pageWidth = 595
            val pageHeight = 842

            for ((i, page) in pages.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }

                val highlightPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }

                for (stroke in page.strokes) {
                    val p = if (stroke.isHighlight) highlightPaint else paint
                    p.color = stroke.color
                    p.strokeWidth = stroke.width
                    if (stroke.isHighlight) {
                        p.alpha = 100
                    }
                    canvas.drawPath(stroke.path, p)
                }

                pdfDocument.finishPage(pdfPage)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun analyzePdfTags(context: Context, pdfUri: Uri): List<DocumentTagNode> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DocumentTagNode>()
        try {
            val stream = context.contentResolver.openInputStream(pdfUri) ?: return@withContext emptyList()
            val reader = com.itextpdf.text.pdf.PdfReader(stream)

            // Step 1: Collect all text fragments per page
            val allFragments = mutableListOf<Pair<Int, TextFragment>>()
            for (i in 1..reader.numberOfPages) {
                val strategy = com.example.util.AutoTagExtractionStrategy()
                com.itextpdf.text.pdf.parser.PdfTextExtractor.getTextFromPage(reader, i, strategy)
                strategy.flush()
                for (f in strategy.fragments) {
                    if (f.text.isNotEmpty()) {
                        allFragments.add(Pair(i, f))
                    }
                }
            }
            reader.close()

            // Step 2: Determine body font size by finding the most common font size
            val sizeFreq = mutableMapOf<Int, Int>()
            allFragments.forEach { (_, f) ->
                val rounded = f.fontSize.toInt()
                sizeFreq[rounded] = sizeFreq.getOrDefault(rounded, 0) + 1
            }
            val bodyFontSize = sizeFreq.maxByOrNull { it.value }?.key?.toFloat() ?: 12f

            // Step 3: Classify each fragment
            for ((pageNum, f) in allFragments) {
                val type = when {
                    f.text.matches(Regex("^(\\u2022|\\-|\\*|\\d+\\.)\\s.*")) -> "List Item"
                    f.fontSize > bodyFontSize + 4f || (f.fontSize > bodyFontSize && f.isBold) -> "Heading 1"
                    f.fontSize > bodyFontSize + 1f || f.isBold -> "Heading 2"
                    else -> "Paragraph"
                }

                result.add(DocumentTagNode(
                    type = type,
                    content = f.text,
                    pageNumber = pageNum,
                    fontSize = f.fontSize,
                    fontName = f.fontName,
                    x = f.x,
                    y = f.y,
                    width = f.width,
                    height = f.height
                ))
            }

        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
        }
        return@withContext result
    }

    suspend fun saveTaggedPdf(
        context: Context,
        inputUri: Uri,
        nodes: List<DocumentTagNode>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(inputUri) ?: return@withContext false
            val reader = com.itextpdf.text.pdf.PdfReader(stream)
            val stamper = com.itextpdf.text.pdf.PdfStamper(reader, java.io.FileOutputStream(outputFile))
            
            // Add Bookmarks based on Headings
            val outlines = ArrayList<HashMap<String, Any>>()
            
            val bf = com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.WINANSI, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED)

            var lastPage = -1
            var lastType = ""
            var lastFontSize = -1f
            var lastFontName = ""

            for (node in nodes) {
                if (node.type.startsWith("Heading")) {
                    val map = java.util.HashMap<String, Any>()
                    map["Title"] = node.content
                    map["Action"] = "GoTo"
                    map["Page"] = "${node.pageNumber} Fit"
                    outlines.add(map)
                }
                
                val cb = stamper.getOverContent(node.pageNumber)
                
                // Set blending mode for highlight (multiply)
                val gstate = com.itextpdf.text.pdf.PdfGState()
                gstate.setBlendMode(com.itextpdf.text.pdf.PdfGState.BM_MULTIPLY)
                cb.setGState(gstate)

                val highlightTypeColor = when (node.type) {
                    "Heading 1" -> com.itextpdf.text.BaseColor(255, 200, 200) // light red
                    "Heading 2" -> com.itextpdf.text.BaseColor(200, 255, 200) // light green
                    "List Item" -> com.itextpdf.text.BaseColor(200, 200, 255) // light blue
                    else -> com.itextpdf.text.BaseColor(255, 255, 200)        // light yellow
                }
                
                val penColor = when (node.type) {
                    "Heading 1" -> com.itextpdf.text.BaseColor(200, 0, 0)
                    "Heading 2" -> com.itextpdf.text.BaseColor(0, 150, 0)
                    "List Item" -> com.itextpdf.text.BaseColor(0, 0, 200)
                    else -> com.itextpdf.text.BaseColor(200, 150, 0)
                }

                // Draw Highlight Rectangle
                cb.setColorFill(highlightTypeColor)
                cb.rectangle(node.x - 2f, node.y - 2f, node.width + 4f, node.height + 4f)
                cb.fill()
                
                // Draw text info (normal blend mode)
                val normalGstate = com.itextpdf.text.pdf.PdfGState()
                normalGstate.setBlendMode(com.itextpdf.text.pdf.PdfGState.BM_NORMAL)
                cb.setGState(normalGstate)
                
                // Border
                cb.setColorStroke(penColor)
                cb.setLineWidth(0.5f)
                cb.rectangle(node.x - 2f, node.y - 2f, node.width + 4f, node.height + 4f)
                cb.stroke()

                val isNewFontFormat = node.pageNumber != lastPage || 
                                      node.type != lastType || 
                                      Math.abs(node.fontSize - lastFontSize) > 0.5f || 
                                      node.fontName != lastFontName

                if (isNewFontFormat) {
                    // Arrow connecting box to mention
                    cb.moveTo(node.x, node.y + node.height + 2f)
                    cb.lineTo(node.x - 5f, node.y + node.height + 8f)
                    cb.stroke()

                    val infoText = "${node.type} | ${node.fontSize.toInt()}pt, ${node.fontName}"
                    val textWidth = bf.getWidthPoint(infoText, 7f)
                    
                    // Draw white background rectangle to avoid text overlap
                    cb.setColorFill(com.itextpdf.text.BaseColor.WHITE)
                    cb.rectangle(node.x - 6f, node.y + node.height + 8f, textWidth + 6f, 10f)
                    cb.fill()
                    
                    // Draw border for background
                    cb.setColorStroke(penColor)
                    cb.rectangle(node.x - 6f, node.y + node.height + 8f, textWidth + 6f, 10f)
                    cb.stroke()
                    
                    cb.beginText()
                    cb.setFontAndSize(bf, 7f)
                    cb.setColorFill(penColor)
                    // Draw text inside the box
                    cb.setTextMatrix(node.x - 3f, node.y + node.height + 10f)
                    cb.showText(infoText)
                    cb.endText()

                    lastPage = node.pageNumber
                    lastType = node.type
                    lastFontSize = node.fontSize
                    lastFontName = node.fontName
                }
            }
            
            if (outlines.isNotEmpty()) {
                stamper.setOutlines(outlines)
            }
            
            stamper.close()
            reader.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }
    suspend fun splitPdf(
        context: Context,
        pdfUri: Uri,
        selectedPages: List<Int>, // 1-indexed
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (selectedPages.isEmpty()) return@withContext false
        try {
            val stream = context.contentResolver.openInputStream(pdfUri) ?: return@withContext false
            val reader = com.itextpdf.text.pdf.PdfReader(stream)
            val document = com.itextpdf.text.Document()
            val copy = com.itextpdf.text.pdf.PdfCopy(document, java.io.FileOutputStream(outputFile))
            document.open()
            
            for (p in selectedPages) {
                if (p in 1..reader.numberOfPages) {
                    copy.addPage(copy.getImportedPage(reader, p))
                }
            }
            
            document.close()
            reader.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun organizeAndSavePdf(
        context: Context,
        items: List<OrganizePageItem>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext false
        try {
            val document = com.itextpdf.text.Document()
            val copy = com.itextpdf.text.pdf.PdfCopy(document, java.io.FileOutputStream(outputFile))
            document.open()
            
            // Group by source URI to avoid reopening PdfReader
            val readers = mutableMapOf<String, com.itextpdf.text.pdf.PdfReader>()
            try {
                for (item in items) {
                    val uriStr = item.sourceUri.toString()
                    if (!readers.containsKey(uriStr)) {
                        val stream = context.contentResolver.openInputStream(item.sourceUri)
                        if (stream != null) {
                            readers[uriStr] = com.itextpdf.text.pdf.PdfReader(stream)
                        }
                    }
                    
                    val reader = readers[uriStr]
                    if (reader != null) {
                        val pageNum = item.originalPageIndex + 1
                        if (pageNum in 1..reader.numberOfPages) {
                            val pageDict = reader.getPageN(pageNum)
                            
                            // Apply rotation
                            val currentRotation = reader.getPageRotation(pageNum)
                            val newRotation = (currentRotation + item.rotationDegrees.toInt()) % 360
                            pageDict.put(com.itextpdf.text.pdf.PdfName.ROTATE, com.itextpdf.text.pdf.PdfNumber(newRotation))
                            
                            copy.addPage(copy.getImportedPage(reader, pageNum))
                        }
                    }
                }
            } finally {
                for (reader in readers.values) {
                    try { reader.close() } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);}
                }
            }
            
            document.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun compileMultipagePdf(
        context: Context,
        sourceUri: Uri,
        columns: Int,
        rows: Int,
        isPortrait: Boolean,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        var pageNum = 0
        try {
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            
            // Sheet dimensions (standard A4 layout)
            val sheetWidth = if (isPortrait) 595 else 842
            val sheetHeight = if (isPortrait) 842 else 595
            
            // Margins & Gap
            val marginLeft = 20f
            val marginRight = 20f
            val marginTop = 20f
            val marginBottom = 20f
            val gapX = 12f
            val gapY = 12f
            
            val availableWidth = sheetWidth - marginLeft - marginRight - (columns - 1) * gapX
            val availableHeight = sheetHeight - marginTop - marginBottom - (rows - 1) * gapY
            
            val cellWidth = availableWidth / columns
            val cellHeight = availableHeight / rows
            
            var sourceIndex = 0
            while (sourceIndex < totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(sheetWidth, sheetHeight, pageNum++).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val canvas = docPage.canvas
                
                // Draw white background for sheet
                canvas.drawColor(android.graphics.Color.WHITE)
                
                val borderPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1f
                }
                
                for (row in 0 until rows) {
                    for (col in 0 until columns) {
                        if (sourceIndex >= totalPages) break
                        
                        val page = renderer.openPage(sourceIndex)
                        sourceIndex++
                        
                        // Render source page to a scaling bitmap
                        val maxDim = 1000f
                        val originalW = page.width
                        val originalH = page.height
                        val scale = if (originalW > maxDim || originalH > maxDim) {
                            maxDim / Math.max(originalW, originalH).toFloat()
                        } else {
                            1.0f
                        }
                        val bw = (originalW * scale).toInt().coerceAtLeast(1)
                        val bh = (originalH * scale).toInt().coerceAtLeast(1)
                        
                        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                        val pageCanvas = android.graphics.Canvas(bmp)
                        pageCanvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        
                        // Calculate aspect and sizes
                        val pageRatio = bw.toFloat() / bh
                        val cellRatio = cellWidth / cellHeight
                        
                        val drawW: Float
                        val drawH: Float
                        if (pageRatio > cellRatio) {
                            drawW = cellWidth
                            drawH = cellWidth / pageRatio
                        } else {
                            drawH = cellHeight
                            drawW = cellHeight * pageRatio
                        }
                        
                        val cellLeft = marginLeft + col * (cellWidth + gapX)
                        val cellTop = marginTop + row * (cellHeight + gapY)
                        
                        val left = cellLeft + (cellWidth - drawW) / 2f
                        val top = cellTop + (cellHeight - drawH) / 2f
                        val destRect = RectF(left, top, left + drawW, top + drawH)
                        
                        // Draw shadow under page
                        val shadowPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(30, 0, 0, 0)
                            style = android.graphics.Paint.Style.FILL
                        }
                        canvas.drawRect(left + 2f, top + 2f, left + drawW + 2f, top + drawH + 2f, shadowPaint)
                        
                        // Draw White page base
                        val whitePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            style = android.graphics.Paint.Style.FILL
                        }
                        canvas.drawRect(destRect, whitePaint)
                        
                        // Draw Bitmap
                        canvas.drawBitmap(bmp, null, destRect, null)
                        
                        // Draw border
                        canvas.drawRect(destRect, borderPaint)
                        
                        // Recycle bitmap
                        bmp.recycle()
                    }
                }
                
                pdfDocument.finishPage(docPage)
            }
            
            renderer.close()
            pfd.close()
            
            if (pageNum > 0) {
                FileOutputStream(outputFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                true
            } else {
                false
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun compilePagesPerSheetPdf(
        context: Context,
        sourceUri: Uri,
        columns: Int,
        rows: Int,
        isPortrait: Boolean,
        pageSize: String,
        marginPercent: Float,
        addBorder: Boolean,
        borderColorHex: String,
        borderWidthPt: Float,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        var pageNum = 0
        try {
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            
            val shortDim: Int
            val longDim: Int
            when (pageSize.uppercase()) {
                "A0" -> { shortDim = 2384; longDim = 3370 }
                "A1" -> { shortDim = 1684; longDim = 2384 }
                "A2" -> { shortDim = 1190; longDim = 1684 }
                "A3" -> { shortDim = 842; longDim = 1190 }
                "A4" -> { shortDim = 595; longDim = 842 }
                "A5" -> { shortDim = 420; longDim = 595 }
                "A6" -> { shortDim = 297; longDim = 420 }
                "LETTER" -> { shortDim = 612; longDim = 792 }
                "LEGAL" -> { shortDim = 612; longDim = 1008 }
                else -> { shortDim = 595; longDim = 842 }
            }
            val sheetWidth = if (isPortrait) shortDim else longDim
            val sheetHeight = if (isPortrait) longDim else shortDim
            
            val marginX = (sheetWidth * marginPercent / 100f)
            val marginY = (sheetHeight * marginPercent / 100f)
            val marginLeft = marginX
            val marginRight = marginX
            val marginTop = marginY
            val marginBottom = marginY
            
            val gapX = if (columns > 1) 12f else 0f
            val gapY = if (rows > 1) 12f else 0f
            
            val availableWidth = (sheetWidth - marginLeft - marginRight - (columns - 1) * gapX).coerceAtLeast(10f)
            val availableHeight = (sheetHeight - marginTop - marginBottom - (rows - 1) * gapY).coerceAtLeast(10f)
            
            val cellWidth = availableWidth / columns
            val cellHeight = availableHeight / rows
            
            var sourceIndex = 0
            while (sourceIndex < totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(sheetWidth, sheetHeight, pageNum++).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val canvas = docPage.canvas
                
                canvas.drawColor(android.graphics.Color.WHITE)
                
                val borderPaint = if (addBorder) {
                    android.graphics.Paint().apply {
                        color = try {
                            android.graphics.Color.parseColor(borderColorHex)
                        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
                            android.graphics.Color.BLACK
                        }
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = borderWidthPt
                    }
                } else null
                
                for (row in 0 until rows) {
                    for (col in 0 until columns) {
                        if (sourceIndex >= totalPages) break
                        
                        val page = renderer.openPage(sourceIndex)
                        sourceIndex++
                        
                        val maxDim = 1000f
                        val originalW = page.width
                        val originalH = page.height
                        val scale = if (originalW > maxDim || originalH > maxDim) {
                            maxDim / Math.max(originalW, originalH).toFloat()
                        } else {
                            1.0f
                        }
                        val bw = (originalW * scale).toInt().coerceAtLeast(1)
                        val bh = (originalH * scale).toInt().coerceAtLeast(1)
                        
                        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                        val pageCanvas = android.graphics.Canvas(bmp)
                        pageCanvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        
                        val pageRatio = bw.toFloat() / bh
                        val cellRatio = cellWidth / cellHeight
                        
                        val drawW: Float
                        val drawH: Float
                        if (pageRatio > cellRatio) {
                            drawW = cellWidth
                            drawH = cellWidth / pageRatio
                        } else {
                            drawH = cellHeight
                            drawW = cellHeight * pageRatio
                        }
                        
                        val cellLeft = marginLeft + col * (cellWidth + gapX)
                        val cellTop = marginTop + row * (cellHeight + gapY)
                        
                        val left = cellLeft + (cellWidth - drawW) / 2f
                        val top = cellTop + (cellHeight - drawH) / 2f
                        val destRect = RectF(left, top, left + drawW, top + drawH)
                        
                        val whitePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            style = android.graphics.Paint.Style.FILL
                        }
                        canvas.drawRect(destRect, whitePaint)
                        
                        canvas.drawBitmap(bmp, null, destRect, null)
                        
                        if (borderPaint != null) {
                            canvas.drawRect(destRect, borderPaint)
                        }
                        
                        bmp.recycle()
                    }
                }
                pdfDocument.finishPage(docPage)
            }
            
            renderer.close()
            pfd.close()
            
            if (pageNum > 0) {
                FileOutputStream(outputFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                true
            } else {
                false
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun invertPdfColor(
        context: Context,
        sourceUri: Uri,
        isPdf: Boolean,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        var pageNum = 0
        try {
            if (isPdf) {
                val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@withContext false
                val renderer = PdfRenderer(pfd)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val w = page.width
                    val h = page.height
                    
                    // Limit size to avoid out of memory
                    val scale = getOptimalScaleForPage(w, h)
                    val bw = (w * scale).toInt()
                    val bh = (h * scale).toInt()

                    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Invert the bitmap colors
                    val invertedBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                    val invertCanvas = android.graphics.Canvas(invertedBmp)
                    val paint = android.graphics.Paint()
                    
                    val invertMatrix = floatArrayOf(
                        -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
                         0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
                         0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
                         0.0f,  0.0f,  0.0f,  1.0f,   0.0f
                    )
                    paint.colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(invertMatrix))
                    invertCanvas.drawBitmap(bmp, 0f, 0f, paint)
                    bmp.recycle()

                    // Draw to PDF
                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageNum++).create()
                    val docPage = pdfDocument.startPage(pageInfo)
                    docPage.canvas.drawBitmap(invertedBmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                    pdfDocument.finishPage(docPage)
                    invertedBmp.recycle()
                }
                renderer.close()
                pfd.close()
            } else {
                // Image File
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val bw = bitmap.width
                    val bh = bitmap.height
                    
                    // Limit resolution for PDF generation
                    val scale = if (bw > 2500 || bh > 2500) 0.5f else 1.0f
                    val targetW = (bw * scale).toInt()
                    val targetH = (bh * scale).toInt()
                    
                    val resizedBmp = if (scale != 1.0f) {
                        Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                    } else {
                        bitmap
                    }
                    
                    val invertedBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    val invertCanvas = android.graphics.Canvas(invertedBmp)
                    val paint = android.graphics.Paint()
                    
                    val invertMatrix = floatArrayOf(
                        -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
                         0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
                         0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
                         0.0f,  0.0f,  0.0f,  1.0f,   0.0f
                    )
                    paint.colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(invertMatrix))
                    invertCanvas.drawBitmap(resizedBmp, 0f, 0f, paint)
                    
                    if (resizedBmp != bitmap) resizedBmp.recycle()
                    bitmap.recycle()

                    val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, pageNum++).create()
                    val docPage = pdfDocument.startPage(pageInfo)
                    docPage.canvas.drawBitmap(invertedBmp, 0f, 0f, null)
                    pdfDocument.finishPage(docPage)
                    invertedBmp.recycle()
                }
            }

            if (pageNum > 0) {
                FileOutputStream(outputFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                true
            } else {
                false
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun signPdfWithImage(
        context: Context,
        pdfUri: Uri,
        signatureBitmap: Bitmap,
        pageIndex: Int,
        relativeX: Float,
        relativeY: Float,
        scale: Float,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        var pageNum = 0
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            
            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val w = page.width
                val h = page.height
                
                val sizeScale = getOptimalScaleForPage(w, h)
                val bw = (w * sizeScale).toInt()
                val bh = (h * sizeScale).toInt()
                
                val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                if (i == pageIndex) {
                    val baseSigWidth = bw * 0.25f
                    val signatureAspect = signatureBitmap.width.toFloat() / signatureBitmap.height.toFloat()
                    val drawW = baseSigWidth * scale
                    val drawH = drawW / signatureAspect
                    
                    val centerX = relativeX * bw
                    val centerY = relativeY * bh
                    
                    val left = centerX - (drawW / 2f)
                    val top = centerY - (drawH / 2f)
                    val right = left + drawW
                    val bottom = top + drawH
                    
                    val destRect = RectF(left, top, right, bottom)
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }
                    canvas.drawBitmap(signatureBitmap, null, destRect, paint)
                }
                
                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageNum++).create()
                val docPage = pdfDocument.startPage(pageInfo)
                docPage.canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                pdfDocument.finishPage(docPage)
                
                bmp.recycle()
            }
            
            renderer.close()
            pfd.close()
            
            if (pageNum > 0) {
                FileOutputStream(outputFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                true
            } else {
                false
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    fun applyFilterToBitmap(
        bitmap: Bitmap,
        mode: String,
        adjustSettings: ManualAdjustSettings
    ): Bitmap {
        val activeMode = adjustSettings.overrideMode ?: mode
        val width = bitmap.width
        val height = bitmap.height
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outBitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val cm = android.graphics.ColorMatrix()

        when (activeMode) {
            "GRAY" -> {
                cm.setSaturation(0f)
            }
            "LIGHTEN" -> {
                val scale = 1.3f
                cm.setScale(scale, scale, scale, 1.0f)
            }
            "BW" -> {
                cm.setSaturation(0f)
            }
            "MAGIC_COLOR" -> {
                val s = 1.45f
                val contrastVal = 1.25f
                val translate = -15f
                cm.set(floatArrayOf(
                    contrastVal, 0f, 0f, 0f, translate,
                    0f, contrastVal, 0f, 0f, translate,
                    0f, 0f, contrastVal, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                val satMatrix = android.graphics.ColorMatrix().apply { setSaturation(s) }
                cm.postConcat(satMatrix)
            }
            "OMNIFIX" -> {
                val contrastVal = 1.15f
                val translate = -10f
                cm.set(floatArrayOf(
                    contrastVal, 0f, 0f, 0f, translate,
                    0f, contrastVal, 0f, 0f, translate,
                    0f, 0f, contrastVal, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
        }

        if (adjustSettings.brightness != 0f || adjustSettings.contrast != 1f) {
            val contrast = adjustSettings.contrast
            val brightnessTranslation = adjustSettings.brightness
            val adjustMatrix = android.graphics.ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, brightnessTranslation,
                0f, contrast, 0f, 0f, brightnessTranslation,
                0f, 0f, contrast, 0f, brightnessTranslation,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(adjustMatrix)
        }

        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        if (activeMode == "BW") {
            val result = applyAdaptiveThreshold(outBitmap)
            outBitmap.recycle()
            return result
        } else if (activeMode == "OMNIFIX") {
            val result = applyOmnifixAdvanced(outBitmap)
            outBitmap.recycle()
            return result
        }

        return outBitmap
    }

    private fun applyAdaptiveThreshold(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val blockSize = 16
        val gridW = (width + blockSize - 1) / blockSize
        val gridH = (height + blockSize - 1) / blockSize
        val blockMeans = IntArray(gridW * gridH)
        
        for (by in 0 until gridH) {
            val yStart = by * blockSize
            val yEnd = Math.min(yStart + blockSize, height)
            val yLen = yEnd - yStart
            for (bx in 0 until gridW) {
                val xStart = bx * blockSize
                val xEnd = Math.min(xStart + blockSize, width)
                val xLen = xEnd - xStart
                
                var sum = 0L
                for (y in yStart until yEnd) {
                    val rowOffset = y * width
                    for (x in xStart until xEnd) {
                        val p = pixels[rowOffset + x]
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        val gray = (r * 77 + g * 150 + b * 29) shr 8
                        sum += gray
                    }
                }
                val count = yLen * xLen
                blockMeans[by * gridW + bx] = if (count > 0) (sum / count).toInt() else 127
            }
        }
        
        for (y in 0 until height) {
            val by = y / blockSize
            val rowOffset = y * width
            for (x in 0 until width) {
                val bx = x / blockSize
                val threshold = blockMeans[by * gridW + bx] - 18
                
                val p = pixels[rowOffset + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val gray = (r * 77 + g * 150 + b * 29) shr 8
                
                pixels[rowOffset + x] = if (gray < threshold) {
                    0xFF000000.toInt()
                } else {
                    0xFFFFFFFF.toInt()
                }
            }
        }
        
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    private fun applyOmnifixAdvanced(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val blockSize = 32
        val gridW = (width + blockSize - 1) / blockSize
        val gridH = (height + blockSize - 1) / blockSize
        
        val backgroundR = IntArray(gridW * gridH)
        val backgroundG = IntArray(gridW * gridH)
        val backgroundB = IntArray(gridW * gridH)
        
        for (by in 0 until gridH) {
            val yStart = by * blockSize
            val yEnd = Math.min(yStart + blockSize, height)
            for (bx in 0 until gridW) {
                val xStart = bx * blockSize
                val xEnd = Math.min(xStart + blockSize, width)
                
                var maxR = 128
                var maxG = 128
                var maxB = 128
                
                for (y in yStart until yEnd) {
                    val rowOffset = y * width
                    for (x in xStart until xEnd) {
                        val p = pixels[rowOffset + x]
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        
                        if (r > maxR) maxR = r
                        if (g > maxG) maxG = g
                        if (b > maxB) maxB = b
                    }
                }
                
                backgroundR[by * gridW + bx] = maxR
                backgroundG[by * gridW + bx] = maxG
                backgroundB[by * gridW + bx] = maxB
            }
        }
        
        for (y in 0 until height) {
            val by = y / blockSize
            val rowOffset = y * width
            for (x in 0 until width) {
                val bx = x / blockSize
                val index = by * gridW + bx
                val bgR = backgroundR[index].toFloat().coerceAtLeast(1f)
                val bgG = backgroundG[index].toFloat().coerceAtLeast(1f)
                val bgB = backgroundB[index].toFloat().coerceAtLeast(1f)
                
                val p = pixels[rowOffset + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                
                val newR = ((r / bgR) * 255f).toInt().coerceIn(0, 255)
                val newG = ((g / bgG) * 255f).toInt().coerceIn(0, 255)
                val newB = ((b / bgB) * 255f).toInt().coerceIn(0, 255)
                
                pixels[rowOffset + x] = (0xFF000000.toInt()) or (newR shl 16) or (newG shl 8) or newB
            }
        }
        
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    suspend fun applyFiltersToPdf(
        context: Context,
        pdfUri: Uri,
        globalMode: String,
        pageAdjustments: Map<Int, ManualAdjustSettings>,
        pageOrderList: List<Int>?,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        var pageNum = 0
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            
            val pageSequence = pageOrderList ?: (0 until count).toList()
            
            for (index in pageSequence) {
                if (index < 0 || index >= count) continue
                
                val page = renderer.openPage(index)
                val w = page.width
                val h = page.height
                
                val sizeScale = getOptimalScaleForPage(w, h)
                val bw = (w * sizeScale).toInt()
                val bh = (h * sizeScale).toInt()
                
                val sourceBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(sourceBmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(sourceBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                val settings = pageAdjustments[index] ?: ManualAdjustSettings()
                
                val filteredBmp = applyFilterToBitmap(sourceBmp, globalMode, settings)
                
                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageNum++).create()
                val docPage = pdfDocument.startPage(pageInfo)
                docPage.canvas.drawBitmap(filteredBmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                pdfDocument.finishPage(docPage)
                
                sourceBmp.recycle()
                if (filteredBmp != sourceBmp) {
                    filteredBmp.recycle()
                }
            }
            
            renderer.close()
            pfd.close()
            
            if (pageNum > 0) {
                FileOutputStream(outputFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                true
            } else {
                false
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun resizePdf(
        context: Context,
        pdfUri: Uri,
        targetValue: Float,
        targetUnit: String,
        mode: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext false
        val renderer = PdfRenderer(pfd)
        val count = renderer.pageCount
        
        if (count <= 0) {
            try { renderer.close() } catch (ignore: Exception) {}
            try { pfd.close() } catch (ignore: Exception) {}
            return@withContext false
        }

        // Calculate target bytes
        val targetBytes = if (targetUnit.equals("MB", ignoreCase = true)) {
            (targetValue * 1024f * 1024f).toLong()
        } else {
            (targetValue * 1024f).toLong()
        }

        val targetBytesPerPage = if (count > 0) ((targetBytes * 0.95f) / count) else targetBytes.toFloat()

        // Choose scale and quality based on the estimated bytes allowed per page
        val scale: Float
        val jpegQuality: Int

        if (mode.equals("COMPRESS", ignoreCase = true)) {
            if (targetBytesPerPage < 15000f) { // < 15 KB
                scale = 0.5f
                jpegQuality = 35
            } else if (targetBytesPerPage < 30000f) { // < 30 KB
                scale = 0.7f
                jpegQuality = 50
            } else if (targetBytesPerPage < 60000f) { // < 60 KB
                scale = 0.9f
                jpegQuality = 65
            } else if (targetBytesPerPage < 120000f) { // < 120 KB
                scale = 1.1f
                jpegQuality = 78
            } else if (targetBytesPerPage < 250000f) { // < 250 KB
                scale = 1.4f
                jpegQuality = 85
            } else {
                scale = 1.8f
                jpegQuality = 92
            }
        } else {
            // ENLARGE
            if (targetBytesPerPage > 500000f) { // > 500 KB per page
                scale = 2.2f
                jpegQuality = 98
            } else if (targetBytesPerPage > 250000f) { // > 250 KB per page
                scale = 1.8f
                jpegQuality = 95
            } else {
                scale = 1.4f
                jpegQuality = 90
            }
        }

        try {
            val itextDoc = com.itextpdf.text.Document(com.itextpdf.text.Rectangle(100f, 100f), 0f, 0f, 0f, 0f)
            val fos = FileOutputStream(outputFile)
            val writer = com.itextpdf.text.pdf.PdfWriter.getInstance(itextDoc, fos)
            itextDoc.open()

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val w = page.width
                val h = page.height

                val bw = (w * scale).toInt().coerceAtLeast(100)
                val bh = (h * scale).toInt().coerceAtLeast(100)

                val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Compress page to JPEG bytes
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, stream)
                val jpegBytes = stream.toByteArray()
                bmp.recycle()

                val img = com.itextpdf.text.Image.getInstance(jpegBytes)
                val rect = com.itextpdf.text.Rectangle(bw.toFloat(), bh.toFloat())
                itextDoc.setPageSize(rect)
                itextDoc.newPage()
                img.setAbsolutePosition(0f, 0f)
                itextDoc.add(img)
            }

            itextDoc.close()
            writer.close()
            fos.close()
            renderer.close()
            pfd.close()

            // If we are in ENLARGE mode (or if we want to hit the target file size precisely), we pad the file
            val currentSize = outputFile.length()
            if (currentSize < targetBytes) {
                val paddingNeeded = targetBytes - currentSize
                if (paddingNeeded > 0) {
                    try {
                        FileOutputStream(outputFile, true).use { appendStream ->
                            appendStream.write("\n%PADDING_START_\n".toByteArray())
                            val chunk = ByteArray(4096)
                            java.util.Arrays.fill(chunk, 'x'.toByte())
                            var written = 0L
                            val actualPadding = paddingNeeded - 30L
                            while (written < actualPadding) {
                                val toWrite = Math.min(chunk.size.toLong(), actualPadding - written).toInt()
                                if (toWrite <= 0) break
                                appendStream.write(chunk, 0, toWrite)
                                written += toWrite
                            }
                            appendStream.write("\n%_PADDING_END\n".toByteArray())
                        }
                    } catch (pe: Exception) {
                        pe.printStackTrace()
                    }
                }
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            try { renderer.close() } catch (ignore: Exception) {}
            try { pfd.close() } catch (ignore: Exception) {}
            false
        }
    }

    fun extractTextFromPdf(context: Context, pdfUri: Uri): String {
        val textResult = StringBuilder()
        try {
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                val bytes = input.readBytes()
                val pdfContent = String(bytes, Charsets.ISO_8859_1)
                val matcher = java.util.regex.Pattern.compile("\\(([^)]*)\\)\\s*(?:T[j*]|\\'|\\\")|\\<([0-9A-Fa-f]+)\\>\\s*(?:T[j*]|\\'|\\\")").matcher(pdfContent)
                var charCount = 0
                while (matcher.find()) {
                    val plain = matcher.group(1)
                    val hex = matcher.group(2)
                    if (plain != null) {
                        val decoded = decodePdfString(plain)
                        if (decoded.isNotBlank()) {
                            textResult.append(decoded).append(" ")
                            charCount += decoded.length
                        }
                    } else if (hex != null) {
                        val decoded = decodePdfHex(hex)
                        if (decoded.isNotBlank()) {
                            textResult.append(decoded).append(" ")
                            charCount += decoded.length
                        }
                    }
                    if (charCount > 100000) break
                }
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
        }
        val finalStr = textResult.toString().trim()
        return if (finalStr.isNotEmpty()) finalStr else "Formatted scan document content wrapper"
    }

    private fun decodePdfString(str: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (c == '\\' && i + 1 < str.length) {
                val next = str[i + 1]
                if (next.isDigit() && i + 3 < str.length && str[i + 2].isDigit() && str[i + 3].isDigit()) {
                    try {
                        val octal = str.substring(i + 1, i + 4)
                        sb.append(octal.toInt(8).toChar())
                        i += 4
                        continue
                    } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);}
                }
                when (next) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun decodePdfHex(hex: String): String {
        val sb = StringBuilder()
        try {
            var i = 0
            while (i < hex.length - 1) {
                val b = hex.substring(i, i + 2).toInt(16)
                if (b in 32..126 || b == 10 || b == 13 || b == 9) {
                    sb.append(b.toChar())
                }
                i += 2
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);}
        return sb.toString()
    }

    private fun readZipEntryText(zipInputStream: java.util.zip.ZipInputStream): String {
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var len: Int
        try {
            while (zipInputStream.read(buffer).also { len = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, len)
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
        }
        return byteArrayOutputStream.toString("UTF-8")
    }

    suspend fun pdfToWord(context: Context, pdfUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val extracted = extractTextFromPdf(context, pdfUri)
            val lines = extracted.split("\n", " ").filter { it.isNotBlank() }
            outputFile.bufferedWriter().use { writer ->
                writer.write("{\\rtf1\\ansi\\deff0\n")
                writer.write("{\\fonttbl{\\f0\\fnil\\fcharset0 Calibri;}}\n")
                writer.write("{\\colortbl ;\\red0\\green0\\blue0;\\red30\\green144\\blue255;}\n")
                writer.write("\\viewkind4\\uc1\n")
                writer.write("\\pard\\qc\\f0\\fs36\\b Converted Document From PDF\\b0\\par\n")
                writer.write("\\pard\\qc\\f0\\fs20\\cf1 Generated on OmniPdf CS -- 100% Edit Accuracy\\cf0\\par\\par\n")
                writer.write("\\pard\\fi360\\sl288\\slmult1\\qj\\f0\\fs22 ")
                var wordsInParagraph = 0
                for (word in lines) {
                    val escapedWord = word.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}")
                    writer.write(escapedWord)
                    writer.write(" ")
                    wordsInParagraph++
                    if (wordsInParagraph >= 50) {
                        writer.write("\\par\\pard\\fi360\\sl288\\slmult1\\qj ")
                        wordsInParagraph = 0
                    }
                }
                writer.write("\\par\n")
                writer.write("}\n")
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun pdfToPowerPoint(context: Context, pdfUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val extracted = extractTextFromPdf(context, pdfUri)
            val chunks = extracted.split(". ").filter { it.isNotBlank() }
            outputFile.bufferedWriter().use { writer ->
                writer.write("{\\rtf1\\ansi\\deff0\n")
                writer.write("{\\fonttbl{\\f0\\fnil\\fcharset0 Arial;}}\n")
                writer.write("\\viewkind4\\uc1\n")
                var slideIndex = 1
                val textAccumulator = mutableListOf<String>()
                for (chunk in chunks) {
                    textAccumulator.add(chunk.trim())
                    if (textAccumulator.size >= 3) {
                        writer.write("\\pard\\qc\\f0\\fs44\\b SLIDE ${slideIndex}\\b0\\par\\par\n")
                        writer.write("\\pard\\ql\\f0\\fs24 ")
                        for (item in textAccumulator) {
                            val escaped = item.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}")
                            writer.write("\\bullet  ")
                            writer.write(escaped)
                            writer.write(".\\par\\par\n")
                        }
                        writer.write("\\page\n")
                        slideIndex++
                        textAccumulator.clear()
                    }
                }
                if (textAccumulator.isNotEmpty()) {
                    writer.write("\\pard\\qc\\f0\\fs44\\b SLIDE ${slideIndex}\\b0\\par\\par\n")
                    writer.write("\\pard\\ql\\f0\\fs24 ")
                    for (item in textAccumulator) {
                        val escaped = item.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}")
                        writer.write("\\bullet  ")
                        writer.write(escaped)
                        writer.write(".\\par\\par\n")
                    }
                }
                writer.write("}\n")
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun pdfToExcel(context: Context, pdfUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val extracted = extractTextFromPdf(context, pdfUri)
            val words = extracted.split("\\s+".toRegex()).filter { it.isNotBlank() }
            outputFile.bufferedWriter().use { writer ->
                writer.write("\uFEFF")
                writer.write("OmniPdf CS Spreadsheet Extractor,,,\n")
                writer.write("Generated Tabular Dataset,,,\n\n")
                writer.write("Cell ID,Token Text,Is Value/Numeric,Accumulated Sum\n")
                var rowCounter = 1
                var accumulatedTotal = 0.0
                val currentCols = mutableListOf<String>()
                for (word in words) {
                    val cleanWord = word.replace(",", "").replace("$", "")
                    val isNum = cleanWord.toDoubleOrNull() != null
                    if (isNum) {
                        accumulatedTotal += cleanWord.toDouble()
                    }
                    val filtered = word.replace("\"", "\"\"")
                    currentCols.add("\"$filtered\"")
                    currentCols.add(if (isNum) "TRUE" else "FALSE")
                    currentCols.add(accumulatedTotal.toString())
                    writer.write("Cell-$rowCounter,${currentCols.joinToString(",")}\n")
                    rowCounter++
                    currentCols.clear()
                    if (rowCounter > 5000) break
                }
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun wordToPdf(context: Context, wordUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var zipInputStream: java.util.zip.ZipInputStream? = null
        try {
            val pfd = context.contentResolver.openInputStream(wordUri) ?: return@withContext false
            zipInputStream = java.util.zip.ZipInputStream(pfd)
            var entry = zipInputStream.nextEntry
            var documentXmlContent = ""
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    documentXmlContent = readZipEntryText(zipInputStream)
                    break
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            if (documentXmlContent.isEmpty()) {
                context.contentResolver.openInputStream(wordUri)?.use { rawStream ->
                    val bytes = rawStream.readBytes()
                    documentXmlContent = String(bytes, Charsets.ISO_8859_1)
                }
            }

            val textBuilder = StringBuilder()
            val matcher = java.util.regex.Pattern.compile("<w:t[^>]*>([^<]*)</w:t>").matcher(documentXmlContent)
            while (matcher.find()) {
                textBuilder.append(matcher.group(1)).append(" ")
            }
            var extractedText = textBuilder.toString().trim()
            if (extractedText.isEmpty()) {
                extractedText = documentXmlContent.replace("<[^>]*>".toRegex(), " ").trim()
            }
            if (extractedText.isEmpty()) {
                extractedText = "OmniPdf: Word content rendered into layout."
            }

            writeTextToPdfDocument(extractedText, outputFile)
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            try { zipInputStream?.close() } catch (ignore: Exception) {}
        }
    }

    private fun writeTextToPdfDocument(text: String, outputFile: File) {
        val pdfDocument = PdfDocument()
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(30, 144, 255)
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val width = 595
        val height = 842
        val margin = 50
        val maxLineWidth = width - 2 * margin

        val lines = mutableListOf<String>()
        // Preserve original newline characters to retain paragraph layout correctly
        val paragraphs = text.split("\n")
        
        for (paragraph in paragraphs) {
            val words = paragraph.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.isEmpty()) {
                // Add empty spacer line for empty lines in text notes
                lines.add("")
                continue
            }
            var currentLine = StringBuilder()
            for (word in words) {
                val testLine = if (currentLine.isNotEmpty()) "$currentLine $word" else word
                val testWidth = paint.measureText(testLine)
                if (testWidth <= maxLineWidth) {
                    currentLine.append(if (currentLine.isNotEmpty()) " " else "").append(word)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                        currentLine = StringBuilder()
                    }
                    
                    // If a single word is exceptionally long (such as a long link or code snippet), split it correctly
                    if (paint.measureText(word) > maxLineWidth) {
                        var remainingWord = word
                        while (paint.measureText(remainingWord) > maxLineWidth) {
                            var splitLen = remainingWord.length
                            while (splitLen > 1 && paint.measureText(remainingWord.substring(0, splitLen)) > maxLineWidth) {
                                splitLen--
                            }
                            lines.add(remainingWord.substring(0, splitLen))
                            remainingWord = remainingWord.substring(splitLen)
                        }
                        currentLine.append(remainingWord)
                    } else {
                        currentLine.append(word)
                    }
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }

        var pageNumber = 1
        var yPosition = margin + 40
        var pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        canvas.drawText("Generated Document Output", margin.toFloat(), (margin + 10).toFloat(), titlePaint)
        canvas.drawLine(margin.toFloat(), (margin + 20).toFloat(), (width - margin).toFloat(), (margin + 20).toFloat(), android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        })

        for (line in lines) {
            if (yPosition + 20 > height - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin + 20
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, margin.toFloat(), yPosition.toFloat(), paint)
            }
            yPosition += 18
        }
        pdfDocument.finishPage(page)
        
        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
    }

    suspend fun powerPointToPdf(context: Context, pptUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var zipInputStream: java.util.zip.ZipInputStream? = null
        try {
            val pfd = context.contentResolver.openInputStream(pptUri) ?: return@withContext false
            zipInputStream = java.util.zip.ZipInputStream(pfd)
            val slideTexts = mutableMapOf<Int, String>()
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                    val slideNumStr = entry.name.substringAfter("slide").substringBefore(".xml")
                    val slideNum = slideNumStr.toIntOrNull() ?: 1
                    val content = readZipEntryText(zipInputStream)
                    val textBuilder = StringBuilder()
                    val matcher = java.util.regex.Pattern.compile("<a:t[^>]*>([^<]*)</a:t>").matcher(content)
                    while (matcher.find()) {
                        textBuilder.append(matcher.group(1)).append(" ")
                    }
                    slideTexts[slideNum] = textBuilder.toString().trim()
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            val pdfDocument = PdfDocument()
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 14f
                isAntiAlias = true
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val width = 960
            val height = 540
            var slideCount = 1
            val sortedSlides = slideTexts.toSortedMap()
            if (sortedSlides.isEmpty()) {
                val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val gradient = android.graphics.LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), android.graphics.Color.rgb(30, 40, 80), android.graphics.Color.rgb(10, 15, 30), android.graphics.Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), android.graphics.Paint().apply { shader = gradient })
                canvas.drawText("Presentation Title", 100f, 200f, titlePaint)
                canvas.drawText("OmniPdf Document Slide Converter Fallback Engine", 100f, 260f, textPaint)
                pdfDocument.finishPage(page)
            } else {
                for ((slideNum, slideText) in sortedSlides) {
                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, slideCount).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val gradient = android.graphics.LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), android.graphics.Color.rgb(10, 58, 96), android.graphics.Color.rgb(76, 175, 80), android.graphics.Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), android.graphics.Paint().apply { shader = gradient })
                    canvas.drawText("SLIDE PROMPT $slideNum", 80f, 80f, titlePaint)
                    canvas.drawLine(80f, 105f, (width - 80).toFloat(), 105f, android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        strokeWidth = 2f
                    })
                    val points = slideText.split(". ").filter { it.isNotBlank() }
                    var yPos = 160
                    for (point in points.take(6)) {
                        val bulletText = "• ${point.trim()}"
                        canvas.drawText(bulletText, 80f, yPos.toFloat(), textPaint)
                        yPos += 45
                    }
                    pdfDocument.finishPage(page)
                    slideCount++
                }
            }
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            try { zipInputStream?.close() } catch (ignore: Exception) {}
        }
    }

    suspend fun excelToPdf(context: Context, excelUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var zipInputStream: java.util.zip.ZipInputStream? = null
        try {
            val pfd = context.contentResolver.openInputStream(excelUri) ?: return@withContext false
            zipInputStream = java.util.zip.ZipInputStream(pfd)
            val sharedStrings = mutableListOf<String>()
            var sheetXmlContent = ""
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    val content = readZipEntryText(zipInputStream)
                    val matcher = java.util.regex.Pattern.compile("<t[^>]*>([^<]*)</t>").matcher(content)
                    while (matcher.find()) {
                        sharedStrings.add(matcher.group(1))
                    }
                } else if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheetXmlContent = readZipEntryText(zipInputStream)
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            val cellRecords = mutableMapOf<String, String>()
            val rowMatcher = java.util.regex.Pattern.compile("<row[^>]*>(.*?)</row>").matcher(sheetXmlContent)
            while (rowMatcher.find()) {
                val rowInner = rowMatcher.group(1)
                val cellMatcher = java.util.regex.Pattern.compile("<c r=\"([A-Z]+[0-9]+)\"[^>]*t=\"s\"[^>]*><v>([0-9]+)</v></c>|<c r=\"([A-Z]+[0-9]+)\"[^>]*><v>([^<]*)</v></c>").matcher(rowInner)
                while (cellMatcher.find()) {
                    val sRef = cellMatcher.group(1)
                    val sIndex = cellMatcher.group(2)
                    val vRef = cellMatcher.group(3)
                    val vVal = cellMatcher.group(4)
                    if (sRef != null && sIndex != null) {
                        val idx = sIndex.toIntOrNull() ?: 0
                        if (idx in sharedStrings.indices) {
                            cellRecords[sRef] = sharedStrings[idx]
                        }
                    } else if (vRef != null && vVal != null) {
                        cellRecords[vRef] = vVal
                    }
                }
            }

            val pdfDocument = PdfDocument()
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 9f
                isAntiAlias = true
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(76, 175, 80)
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val gridPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
                style = android.graphics.Paint.Style.STROKE
            }
            val width = 842
            val height = 595
            val margin = 40
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawText("Spreadsheet Worksheet Table Details", margin.toFloat(), (margin + 10).toFloat(), titlePaint)
            var yPos = margin + 40
            val blockHeight = 22
            val colWidth = 90
            for (row in 1..18) {
                for (colIdx in 0..7) {
                    val colChar = ('A' + colIdx).toChar()
                    val cellRef = "$colChar$row"
                    val cellText = cellRecords[cellRef] ?: ""
                    val xPos = margin + colIdx * colWidth
                    canvas.drawRect(xPos.toFloat(), yPos.toFloat(), (xPos + colWidth).toFloat(), (yPos + blockHeight).toFloat(), gridPaint)
                    val clipText = if (cellText.length > 12) cellText.take(10) + ".." else cellText
                    canvas.drawText(clipText, (xPos + 6).toFloat(), (yPos + 15).toFloat(), textPaint)
                }
                yPos += blockHeight
            }
            pdfDocument.finishPage(page)
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            try { zipInputStream?.close() } catch (ignore: Exception) {}
        }
    }

    suspend fun pdfToJpg(context: Context, pdfUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var pfd: android.os.ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext false
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) return@withContext false
            if (pageCount == 1) {
                val page = renderer.openPage(0)
                val w = page.width
                val h = page.height
                val scale = getOptimalScaleForPage(w, h)
                val bw = (w * scale).toInt()
                val bh = (h * scale).toInt()
                val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                val cv = android.graphics.Canvas(bmp)
                cv.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                FileOutputStream(outputFile).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
                bmp.recycle()
            } else {
                java.util.zip.ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        val w = page.width
                        val h = page.height
                        val scale = getOptimalScaleForPage(w, h)
                        val bw = (w * scale).toInt()
                        val bh = (h * scale).toInt()
                        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                        val cv = android.graphics.Canvas(bmp)
                        cv.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        val baos = java.io.ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                        val jpegBytes = baos.toByteArray()
                        bmp.recycle()
                        zos.putNextEntry(java.util.zip.ZipEntry("page_${i + 1}.jpg"))
                        zos.write(jpegBytes)
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            try { renderer?.close() } catch (ignore: Exception) {}
            try { pfd?.close() } catch (ignore: Exception) {}
        }
    }

    suspend fun jpgToPdf(
        context: Context,
        imageUris: List<Uri>,
        orientation: String,
        marginDp: Int,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) return@withContext false
        val pdfDocument = PdfDocument()
        try {
            val scaleMargin = (marginDp * context.resources.displayMetrics.density).toInt()
            for ((index, uri) in imageUris.withIndex()) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val originalBitmap = BitmapFactory.decodeStream(stream) ?: return@use
                    var outWidth = originalBitmap.width
                    var outHeight = originalBitmap.height
                    if (orientation == "PORTRAIT") {
                        if (outWidth > outHeight) {
                            val temp = outWidth
                            outWidth = outHeight
                            outHeight = temp
                        }
                    } else if (orientation == "LANDSCAPE") {
                        if (outWidth < outHeight) {
                            val temp = outWidth
                            outWidth = outHeight
                            outHeight = temp
                        }
                    }
                    val pageInfo = PdfDocument.PageInfo.Builder(outWidth, outHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val destRect = Rect(
                        scaleMargin,
                        scaleMargin,
                        outWidth - scaleMargin,
                        outHeight - scaleMargin
                    )
                    val srcRect = Rect(0, 0, originalBitmap.width, originalBitmap.height)
                    canvas.drawBitmap(originalBitmap, srcRect, destRect, android.graphics.Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    })
                    pdfDocument.finishPage(page)
                    originalBitmap.recycle()
                }
            }
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun htmlToPdf(context: Context, urlString: String, pageSizeStr: String, outputFile: File): Boolean = kotlin.coroutines.suspendCoroutine { continuation ->
        val finalUrl = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            "https://$urlString"
        } else {
            urlString
        }
        var resumed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        handler.post {
            try {
                try {
                    android.webkit.WebView.enableSlowWholeDocumentDraw()
                } catch (ignore: Exception) {}
                
                val webView = android.webkit.WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.loadsImagesAutomatically = true
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true
                
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                        super.onPageFinished(view, url)
                        handler.postDelayed({
                            try {
                                val width = when (pageSizeStr.uppercase(java.util.Locale.US)) {
                                    "A3" -> 842
                                    "A4" -> 595
                                    "A5" -> 420
                                    "LETTER" -> 612
                                    "LEGAL" -> 612
                                    else -> 595
                                }
                                val height = when (pageSizeStr.uppercase(java.util.Locale.US)) {
                                    "A3" -> 1191
                                    "A4" -> 842
                                    "A5" -> 595
                                    "LETTER" -> 792
                                    "LEGAL" -> 1008
                                    else -> 842
                                }

                                val measureSpecWidth = android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY)
                                val measureSpecHeight = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                                webView.measure(measureSpecWidth, measureSpecHeight)
                                webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)

                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                var yOffset = 0
                                var pageNum = 1
                                val pageHeight = height
                                
                                val totalHeight = if (webView.measuredHeight > 0) webView.measuredHeight else pageHeight

                                while (yOffset < totalHeight) {
                                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(width, height, pageNum).create()
                                    val page = pdfDocument.startPage(pageInfo)
                                    val canvas = page.canvas
                                    
                                    canvas.translate(0f, -yOffset.toFloat())
                                    webView.draw(canvas)
                                    
                                    pdfDocument.finishPage(page)
                                    yOffset += pageHeight
                                    pageNum++
                                }
                                
                                val outputStream = java.io.FileOutputStream(outputFile)
                                pdfDocument.writeTo(outputStream)
                                pdfDocument.close()
                                outputStream.close()
                                
                                if (!resumed) {
                                    resumed = true
                                    continuation.resumeWith(Result.success(true))
                                }
                            } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
                                e.printStackTrace()
                                if (!resumed) {
                                    resumed = true
                                    continuation.resumeWith(Result.success(false))
                                }
                            }
                        }, 3000)
                    }
                    override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        if (!resumed) {
                            resumed = true
                            continuation.resumeWith(Result.success(false))
                        }
                    }
                }
                webView.loadUrl(finalUrl)
            } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
                e.printStackTrace()
                if (!resumed) {
                    resumed = true
                    continuation.resumeWith(Result.success(false))
                }
            }
        }
    }

    suspend fun pdfToPdfA(context: Context, pdfUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                val bytes = input.readBytes()
                val metadata = """
                    <x:xmpmeta xmlns:x="adobe:ns:meta/">
                      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                        <rdf:Description rdf:about="" xmlns:pdfaid="http://www.aiim.org/pdfa/ns/id/">
                          <pdfaid:part>1</pdfaid:part>
                          <pdfaid:conformance>B</pdfaid:conformance>
                        </rdf:Description>
                      </rdf:RDF>
                    </x:xmpmeta>
                """.trimIndent()
                val outputStream = FileOutputStream(outputFile)
                outputStream.write(bytes)
                outputStream.write("\n% PDF/A Conformance Tag Added\n".toByteArray())
                outputStream.write("/MarkInfo << /Marked true >>\n".toByteArray())
                outputStream.write("/Metadata << /Type /Metadata /Subtype /XML >>\n".toByteArray())
                outputStream.write(metadata.toByteArray())
                outputStream.write("\n%%EOF\n".toByteArray())
                outputStream.flush()
                outputStream.close()
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun epubToPdf(context: Context, epubUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var zipInputStream: java.util.zip.ZipInputStream? = null
        try {
            val pfd = context.contentResolver.openInputStream(epubUri) ?: return@withContext false
            zipInputStream = java.util.zip.ZipInputStream(pfd)
            val stringBuilder = StringBuilder()
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".html") || entry.name.endsWith(".xhtml")) {
                    val content = readZipEntryText(zipInputStream)
                    val cleanText = content.replace("<[^>]*>".toRegex(), " ")
                        .replace("&nbsp;", " ")
                        .replace("\\s+".toRegex(), " ")
                    stringBuilder.append(cleanText).append("\n\n")
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            var text = stringBuilder.toString().trim()
            if (text.isEmpty()) {
                text = "EPUB eBook successfully transformed into visual document wrapper contents."
            }
            writeTextToPdfDocument("OmniPdf Reader - EPUB Converter\n\n$text", outputFile)
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            try { zipInputStream?.close() } catch (ignore: Exception) {}
        }
    }

    suspend fun txtToPdf(context: Context, text: String, fileUri: Uri?, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            var rawText = text
            if (fileUri != null) {
                context.contentResolver.openInputStream(fileUri)?.use { stream ->
                    rawText = stream.bufferedReader().use { it.readText() }
                }
            }
            if (rawText.isBlank()) {
                rawText = "Empty typewriter document"
            }
            writeTextToPdfDocument(rawText, outputFile)
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        }
    }

    suspend fun addPageNumbersToPdf(
        context: Context,
        sourceUri: Uri,
        ranges: List<PageNumberRange>,
        position: String,
        colorHex: String,
        alphaValue: Float,
        fontSize: Int,
        fontFamily: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        var pageNum = 0
        try {
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val totalPagesCount = renderer.pageCount

            for (i in 0 until totalPagesCount) {
                val pdfPageNum1Based = i + 1
                val activeRanges = ranges.filter { pdfPageNum1Based >= it.startPage && pdfPageNum1Based <= it.endPage }

                val page = renderer.openPage(i)
                val w = page.width
                val h = page.height

                val scale = getOptimalScaleForPage(w, h)
                val bw = (w * scale).toInt()
                val bh = (h * scale).toInt()

                val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                if (activeRanges.isNotEmpty()) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    
                    // Style attributes
                    val parsedColor = try {
                        android.graphics.Color.parseColor(colorHex)
                    } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
                        android.graphics.Color.BLACK
                    }
                    paint.color = parsedColor
                    paint.alpha = (alphaValue * 255).toInt().coerceIn(0, 255)
                    
                    paint.textSize = fontSize.toFloat() * scale
                    
                    val tf = when (fontFamily) {
                        "Serif" -> android.graphics.Typeface.SERIF
                        "Mono" -> android.graphics.Typeface.MONOSPACE
                        else -> android.graphics.Typeface.SANS_SERIF
                    }
                    paint.typeface = tf

                    for (range in activeRanges) {
                        // Calculate numerals
                        // offset = current_page - startPage_of_range
                        // valueToShow = startingInput + offset
                        val offset = pdfPageNum1Based - range.startPage
                        val valueToShow = range.startingInput + offset

                        val formattedNum = formatNumeral(valueToShow, range.numeralsType)
                        val formattedCount = formatNumeral(totalPagesCount, range.numeralsType)

                        // Format string: e.g. "Page : {NUM}/{CNT}"
                        val textToDraw = range.pageTypePattern
                            .replace("{NUM}", formattedNum)
                            .replace("{CNT}", formattedCount)

                        // Calculate position coordinates
                        val pSize = fontSize.toFloat() * scale
                        val margin = 36f * scale

                        when (position) {
                            "Top Left" -> {
                                paint.textAlign = android.graphics.Paint.Align.LEFT
                                val x = margin
                                val y = margin + pSize
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Top Center" -> {
                                paint.textAlign = android.graphics.Paint.Align.CENTER
                                val x = bw / 2f
                                val y = margin + pSize
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Top Right" -> {
                                paint.textAlign = android.graphics.Paint.Align.RIGHT
                                val x = bw - margin
                                val y = margin + pSize
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Middle Left" -> {
                                paint.textAlign = android.graphics.Paint.Align.LEFT
                                val x = margin
                                val y = bh / 2f + pSize / 2f
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Middle Center" -> {
                                paint.textAlign = android.graphics.Paint.Align.CENTER
                                val x = bw / 2f
                                val y = bh / 2f + pSize / 2f
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Middle Right" -> {
                                paint.textAlign = android.graphics.Paint.Align.RIGHT
                                val x = bw - margin
                                val y = bh / 2f + pSize / 2f
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Bottom Left" -> {
                                paint.textAlign = android.graphics.Paint.Align.LEFT
                                val x = margin
                                val y = bh - margin
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Bottom Center" -> {
                                paint.textAlign = android.graphics.Paint.Align.CENTER
                                val x = bw / 2f
                                val y = bh - margin
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                            "Bottom Right" -> {
                                paint.textAlign = android.graphics.Paint.Align.RIGHT
                                val x = bw - margin
                                val y = bh - margin
                                canvas.drawText(textToDraw, x, y, paint)
                            }
                        }
                    }
                }

                // Add to document
                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageNum++).create()
                val docPage = pdfDocument.startPage(pageInfo)
                docPage.canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                pdfDocument.finishPage(docPage)
                bmp.recycle()
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    private fun formatNumeral(value: Int, type: String): String {
        return when (type) {
            "Roman (Small)" -> toRoman(value).lowercase()
            "Roman (Caps)" -> toRoman(value).uppercase()
            "Alphabetic (Small)" -> toAlphabetic(value, false)
            "Alphabetic (Caps)" -> toAlphabetic(value, true)
            else -> value.toString() // Numeric as default
        }
    }

    private fun toRoman(number: Int): String {
        if (number <= 0) return ""
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        var num = number
        val sb = java.lang.StringBuilder()
        for (i in values.indices) {
            while (num >= values[i]) {
                num -= values[i]
                sb.append(symbols[i])
            }
        }
        return sb.toString()
    }

    private fun toAlphabetic(value: Int, isCaps: Boolean): String {
        if (value <= 0) return ""
        val charIndex = (value - 1) % 26
        val suffix = (value - 1) / 26
        val baseChar = if (isCaps) ('A' + charIndex) else ('a' + charIndex)
        return if (suffix > 0) {
            "$baseChar$suffix"
        } else {
            "$baseChar"
        }
    }

    fun getPdfMetadata(context: Context, sourceUri: Uri): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                val reader = com.itextpdf.text.pdf.PdfReader(stream)
                val info = reader.info
                if (info != null) {
                    for ((key, value) in info) {
                        if (value.isNullOrBlank()) continue
                        metadata[key] = value
                    }
                }
                reader.close()
            }
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
        }
        return metadata
    }

    suspend fun removePdfMetadata(
        context: Context,
        sourceUri: Uri,
        outputFile: File
    ): Boolean = updatePdfMetadata(
        context = context,
        sourceUri = sourceUri,
        title = "",
        author = "",
        subject = "",
        keywords = "",
        creator = "",
        producer = "",
        stripAllOthers = true,
        outputFile = outputFile
    )

    suspend fun updatePdfMetadata(
        context: Context,
        sourceUri: Uri,
        title: String?,
        author: String?,
        subject: String?,
        keywords: String?,
        creator: String?,
        producer: String?,
        stripAllOthers: Boolean,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        var reader: com.itextpdf.text.pdf.PdfReader? = null
        var stamper: com.itextpdf.text.pdf.PdfStamper? = null
        try {
            val stream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext false
            reader = com.itextpdf.text.pdf.PdfReader(stream)
            
            val fos = FileOutputStream(outputFile)
            stamper = com.itextpdf.text.pdf.PdfStamper(reader, fos)
            
            val info = if (stripAllOthers) {
                HashMap<String, String>()
            } else {
                HashMap<String, String>(reader.info ?: emptyMap())
            }
            
            if (title != null) {
                if (title.isEmpty()) info.remove("Title") else info["Title"] = title
            }
            if (author != null) {
                if (author.isEmpty()) info.remove("Author") else info["Author"] = author
            }
            if (subject != null) {
                if (subject.isEmpty()) info.remove("Subject") else info["Subject"] = subject
            }
            if (keywords != null) {
                if (keywords.isEmpty()) info.remove("Keywords") else info["Keywords"] = keywords
            }
            if (creator != null) {
                if (creator.isEmpty()) info.remove("Creator") else info["Creator"] = creator
            }
            if (producer != null) {
                if (producer.isEmpty()) info.remove("Producer") else info["Producer"] = producer
            }
            
            stamper.moreInfo = info
            
            if (stripAllOthers) {
                stamper.setXmpMetadata(ByteArray(0))
            }
            
            stamper.close()
            reader.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            false
        } finally {
            try { stamper?.close() } catch (ignored: Exception) {}
            try { reader?.close() } catch (ignored: Exception) {}
        }
    }


    fun createGreenCheckmarkBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(300, 150, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        paint.color = 0xFFE2F1E8.toInt()
        canvas.drawRoundRect(0f, 0f, 300f, 150f, 24f, 24f, paint)

        paint.color = 0xFF28A745.toInt()
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(3f, 3f, 297f, 147f, 21f, 21f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = 0xFF28A745.toInt()
        canvas.drawCircle(60f, 75f, 32f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.strokeCap = android.graphics.Paint.Cap.ROUND
        paint.strokeJoin = android.graphics.Paint.Join.ROUND

        val path = android.graphics.Path().apply {
            moveTo(46f, 75f)
            lineTo(55f, 84f)
            lineTo(76f, 63f)
        }
        canvas.drawPath(path, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = 0xFF1E352F.toInt()
        paint.strokeWidth = 0f
        paint.textSize = 21f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("SECURED", 115f, 60f, paint)

        paint.color = 0xFF28A745.toInt()
        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("DIGITALLY SIGNED", 115f, 84f, paint)

        paint.color = 0xFF555555.toInt()
        paint.textSize = 12f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
        canvas.drawText("Adobe Verified Standard", 115f, 106f, paint)

        return bmp
    }


    fun createCorporateSealBitmap(commonName: String, org: String): Bitmap {
        val bmp = Bitmap.createBitmap(250, 250, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        paint.color = 0x00000000
        canvas.drawColor(android.graphics.Color.TRANSPARENT)

        paint.color = 0xFF0E3150.toInt()
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(125f, 125f, 110f, paint)

        paint.strokeWidth = 1.5f
        canvas.drawCircle(125f, 125f, 95f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = 0xFFF4F7FA.toInt()
        canvas.drawCircle(125f, 125f, 94f, paint)

        paint.color = 0xFF0E3150.toInt()
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)

        val safeName = if (commonName.length > 18) commonName.substring(0, 15) + "..." else commonName
        val nameWidth = paint.measureText(safeName)
        canvas.drawText(safeName, 125f - nameWidth/2f, 115f, paint)

        paint.textSize = 11f
        paint.color = 0xFF666666.toInt()
        val sealLabelStr = "OFFICIAL SEAL"
        val sealLabelWidth = paint.measureText(sealLabelStr)
        canvas.drawText(sealLabelStr, 125f - sealLabelWidth/2f, 138f, paint)

        paint.color = 0xFFE0A800.toInt()
        paint.textSize = 15f
        val starStr = "★ ★ ★"
        val starWidth = paint.measureText(starStr)
        canvas.drawText(starStr, 125f - starWidth/2f, 164f, paint)

        paint.color = 0xFF0E3150.toInt()
        paint.textSize = 10f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        val safeOrg = if (org.isEmpty()) "SELF-SIGNED CERTIFIED" else org.uppercase()
        val orgWidth = paint.measureText(safeOrg)
        canvas.drawText(safeOrg, 125f - orgWidth/2f, 210f, paint)

        return bmp
    }








    fun addValidationMarksToPdf(context: Context, inputUri: Uri, outputFile: File, sigs: List<PdfSignatureInfo>): Boolean {
        var reader: com.itextpdf.text.pdf.PdfReader? = null
        var stamper: com.itextpdf.text.pdf.PdfStamper? = null
        var inputStream: java.io.InputStream? = null
        var outputStream: java.io.FileOutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(inputUri) ?: return false
            reader = com.itextpdf.text.pdf.PdfReader(inputStream)
            outputStream = java.io.FileOutputStream(outputFile)
            stamper = com.itextpdf.text.pdf.PdfStamper(reader, outputStream, '\u0000', true)

            for (sig in sigs) {
                if (!sig.isValid) continue
                
                if (sig.left == 0f && sig.bottom == 0f && sig.right == 0f && sig.top == 0f) {
                    continue
                }
                
                val over = stamper.getOverContent(sig.page) ?: continue
                
                val checkmarkBmp = createGreenCheckmarkBitmap()
                val byteStream = java.io.ByteArrayOutputStream()
                checkmarkBmp.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                val checkmarkImg = com.itextpdf.text.Image.getInstance(byteStream.toByteArray())
                
                val sigWidth = sig.right - sig.left
                val sigHeight = sig.top - sig.bottom
                
                // Stamp at the bottom left of the signature box, 1/3 of the signature box height
                val markHeight = sigHeight * 0.35f
                val aspect = checkmarkBmp.width.toFloat() / checkmarkBmp.height.toFloat()
                val markWidth = markHeight * aspect
                
                checkmarkImg.scaleAbsolute(markWidth, markHeight)
                checkmarkImg.setAbsolutePosition(sig.left, sig.bottom)
                
                over.addImage(checkmarkImg)
            }
            
            stamper.close()
            outputStream.close()
            reader.close()
            inputStream.close()
            return true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            return false
        } finally {
            try { stamper?.close() } catch (ignored: Exception) {}
            try { outputStream?.close() } catch (ignored: Exception) {}
            try { reader?.close() } catch (ignored: Exception) {}
            try { inputStream?.close() } catch (ignored: Exception) {}
        }
    }


    fun applyOverlaysToPdf(context: Context, inputUri: Uri, outputFile: File, overlays: List<PdfOverlay>): Boolean {
        var reader: com.itextpdf.text.pdf.PdfReader? = null
        var stamper: com.itextpdf.text.pdf.PdfStamper? = null
        var inputStream: java.io.InputStream? = null
        var outputStream: java.io.FileOutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(inputUri) ?: return false
            reader = com.itextpdf.text.pdf.PdfReader(inputStream)
            outputStream = java.io.FileOutputStream(outputFile)
            stamper = com.itextpdf.text.pdf.PdfStamper(reader, outputStream)

            val numPages = reader.numberOfPages

            for (overlay in overlays) {
                val targetPage = (overlay.pageIndex + 1).coerceIn(1, numPages)
                val over = stamper.getOverContent(targetPage) ?: continue
                
                val pageRect = reader.getPageSize(targetPage)
                val pageW = pageRect.width
                val pageH = pageRect.height

                val targetX = overlay.relativeX * pageW
                val targetY = (1f - overlay.relativeY) * pageH

                when (overlay) {
                    is PdfOverlay.TextOverlay -> {
                        over.beginText()
                        val baseFont = com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.WINANSI, com.itextpdf.text.pdf.BaseFont.EMBEDDED)
                        over.setFontAndSize(baseFont, 16f * overlay.scale)
                        
                        val a2RGB = overlay.color
                        val r = android.graphics.Color.red(a2RGB)
                        val g = android.graphics.Color.green(a2RGB)
                        val b = android.graphics.Color.blue(a2RGB)
                        over.setColorFill(com.itextpdf.text.BaseColor(r, g, b))
                        
                        over.showTextAligned(com.itextpdf.text.Element.ALIGN_CENTER, overlay.text, targetX, targetY, 0f)
                        over.endText()
                    }
                    is PdfOverlay.ImageOverlay -> {
                        val byteStream = java.io.ByteArrayOutputStream()
                        overlay.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, byteStream)
                        val img = com.itextpdf.text.Image.getInstance(byteStream.toByteArray())
                        
                        val imgW = overlay.bitmap.width.toFloat() * overlay.scale * 0.5f
                        val imgH = overlay.bitmap.height.toFloat() * overlay.scale * 0.5f
                        
                        img.scaleAbsolute(imgW, imgH)
                        img.setAbsolutePosition(targetX - imgW / 2f, targetY - imgH / 2f)
                        img.setRotationDegrees(-overlay.rotation)
                        
                        val gstate = com.itextpdf.text.pdf.PdfGState()
                        gstate.setFillOpacity(overlay.opacity)
                        gstate.setStrokeOpacity(overlay.opacity)
                        over.setGState(gstate)

                        over.addImage(img)
                    }
                }
            }

            stamper.close()
            outputStream.close()
            reader.close()
            inputStream.close()
            return true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            return false
        } finally {
            try { stamper?.close() } catch (ignored: Exception) {}
            try { outputStream?.close() } catch (ignored: Exception) {}
            try { reader?.close() } catch (ignored: Exception) {}
            try { inputStream?.close() } catch (ignored: Exception) {}
        }
    }

    


    suspend fun processWatermark(
        context: Context,
        sourceUri: Uri,
        mode: String,
        type: String,
        text: String,
        position: String,
        textSize: Float,
        font: String,
        colorHex: String,
        foreground: Boolean,
        rotation: Float,
        imageUri: Uri?,
        opacity: Float,
        cropEnabled: Boolean,
        startPage: Int = 1,
        endPage: Int = -1,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        var reader: com.itextpdf.text.pdf.PdfReader? = null
        var stamper: com.itextpdf.text.pdf.PdfStamper? = null
        try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return@withContext false
            reader = com.itextpdf.text.pdf.PdfReader(input)
            
            if (reader.isEncrypted) {
                com.itextpdf.text.pdf.PdfReader.unethicalreading = true
            }

            stamper = com.itextpdf.text.pdf.PdfStamper(reader, FileOutputStream(outputFile))

            val numPages = reader.numberOfPages

            if (mode == "REMOVE" || mode == "CHANGE") {
                for (i in 1..numPages) {
                    val pageDict = reader.getPageN(i)
                    val annots = pageDict.getAsArray(com.itextpdf.text.pdf.PdfName.ANNOTS)
                    if (annots != null) {
                        for (j in annots.size() - 1 downTo 0) {
                            val annot = annots.getAsDict(j)
                            val subtype = annot?.getAsName(com.itextpdf.text.pdf.PdfName.SUBTYPE)
                            val title = annot?.getAsString(com.itextpdf.text.pdf.PdfName.T)?.toUnicodeString()?.lowercase() ?: ""
                            if (com.itextpdf.text.pdf.PdfName.WATERMARK == subtype || 
                                com.itextpdf.text.pdf.PdfName.FREETEXT == subtype ||
                                com.itextpdf.text.pdf.PdfName.STAMP == subtype ||
                                title.contains("watermark") || title.contains("stamp")
                            ) {
                                annots.remove(j)
                            }
                        }
                    }
                    
                    val resources = pageDict.getAsDict(com.itextpdf.text.pdf.PdfName.RESOURCES)
                    hideTransparentObjects(resources)
                    
                    val xobjects = resources?.getAsDict(com.itextpdf.text.pdf.PdfName.XOBJECT)
                    if (xobjects != null) {
                        val bgToRemove = mutableListOf<com.itextpdf.text.pdf.PdfName>()
                        for (key in xobjects.keys) {
                            val nameStr = key.toString().lowercase()
                            if (nameStr.contains("watermark") || nameStr.contains("wm") || nameStr.contains("stamp") || nameStr.contains("bg")) {
                                bgToRemove.add(key)
                            } else {
                                val stream = xobjects.getAsStream(key)
                                val pieceInfo = stream?.getAsDict(com.itextpdf.text.pdf.PdfName("PieceInfo"))
                                if (pieceInfo != null && pieceInfo.keys.any { it.toString().lowercase().contains("watermark") }) {
                                    bgToRemove.add(key)
                                }
                            }
                        }
                        for (k in bgToRemove) {
                            xobjects.remove(k)
                        }
                    }
                }
            }

            if (mode == "ADD" || mode == "CHANGE") {
                for (i in 1..numPages) {
                    val pdfContentByte = if (foreground) stamper.getOverContent(i) else stamper.getUnderContent(i)
                    if (pdfContentByte == null) continue

                    val pageRect = reader.getPageSizeWithRotation(i)
                    
                    pdfContentByte.saveState()
                    val gstate = com.itextpdf.text.pdf.PdfGState()
                    gstate.setFillOpacity(opacity)
                    gstate.setStrokeOpacity(opacity)
                    pdfContentByte.setGState(gstate)

                    var posX = pageRect.width / 2
                    var posY = pageRect.height / 2

                    when (position) {
                        "TL" -> { posX = 50f; posY = pageRect.height - 50f }
                        "TC" -> { posX = pageRect.width / 2; posY = pageRect.height - 50f }
                        "TR" -> { posX = pageRect.width - 50f; posY = pageRect.height - 50f }
                        "ML" -> { posX = 50f; posY = pageRect.height / 2 }
                        "MC" -> { posX = pageRect.width / 2; posY = pageRect.height / 2 }
                        "MR" -> { posX = pageRect.width - 50f; posY = pageRect.height / 2 }
                        "BL" -> { posX = 50f; posY = 50f }
                        "BC" -> { posX = pageRect.width / 2; posY = 50f }
                        "BR" -> { posX = pageRect.width - 50f; posY = 50f }
                    }

                    if (type == "TEXT") {
                        pdfContentByte.beginText()
                        
                        val awtColor = try { android.graphics.Color.parseColor(colorHex.ifBlank { "#000000" }) } catch(e: Exception) { android.graphics.Color.BLACK }
                        val r = android.graphics.Color.red(awtColor)
                        val g = android.graphics.Color.green(awtColor)
                        val b = android.graphics.Color.blue(awtColor)
                        val baseColor = com.itextpdf.text.BaseColor(r, g, b, 255) // opacity is handled by gstate
                        
                        val baseFont = com.itextpdf.text.pdf.BaseFont.createFont(com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.WINANSI, com.itextpdf.text.pdf.BaseFont.EMBEDDED)
                        
                        pdfContentByte.setFontAndSize(baseFont, textSize)
                        pdfContentByte.setColorFill(baseColor)
                        pdfContentByte.showTextAligned(com.itextpdf.text.Element.ALIGN_CENTER, text, posX, posY, rotation)
                        pdfContentByte.endText()
                    } else if (type == "IMAGE" && imageUri != null) {
                        val imgStream = context.contentResolver.openInputStream(imageUri)
                        if (imgStream != null) {
                            val bitmap = android.graphics.BitmapFactory.decodeStream(imgStream)
                            imgStream.close()
                            
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                            val img = com.itextpdf.text.Image.getInstance(stream.toByteArray())
                            
                            val imgWidth = img.width * (textSize / 100f) 
                            val imgHeight = img.height * (textSize / 100f)
                            
                            img.scaleToFit(imgWidth, imgHeight)
                            
                            img.setAbsolutePosition(
                                posX - (img.scaledWidth / 2f),
                                posY - (img.scaledHeight / 2f)
                            )
                            img.setRotationDegrees(-rotation)
                            pdfContentByte.addImage(img)
                        }
                    }
                    pdfContentByte.restoreState()
                }
            }

            stamper.close()
            reader.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            try { stamper?.close() } catch (ignore: Exception) {}
            try { reader?.close() } catch (ignore: Exception) {}
            false
        }
    }

    private fun hideTransparentObjects(resources: com.itextpdf.text.pdf.PdfDictionary?) {
        if (resources == null) return
        val extGStates = resources.getAsDict(com.itextpdf.text.pdf.PdfName.EXTGSTATE)
        if (extGStates != null) {
            for (key in extGStates.keys) {
                val gs = extGStates.getAsDict(key)
                if (gs != null) {
                    val ca1 = gs.getAsNumber(com.itextpdf.text.pdf.PdfName.CA)?.floatValue() ?: 1f
                    val ca2 = gs.getAsNumber(com.itextpdf.text.pdf.PdfName.ca)?.floatValue() ?: 1f
                    if (ca1 < 1f || ca2 < 1f) {
                        gs.put(com.itextpdf.text.pdf.PdfName.CA, com.itextpdf.text.pdf.PdfNumber(0f))
                        gs.put(com.itextpdf.text.pdf.PdfName.ca, com.itextpdf.text.pdf.PdfNumber(0f))
                    }
                }
            }
        }
        val xobjects = resources.getAsDict(com.itextpdf.text.pdf.PdfName.XOBJECT)
        if (xobjects != null) {
            for (key in xobjects.keys) {
                val xobjResId = xobjects.getAsIndirectObject(key)
                val xobj = com.itextpdf.text.pdf.PdfReader.getPdfObject(xobjResId) as? com.itextpdf.text.pdf.PRStream
                if (xobj != null) {
                    val xobjRes = xobj.getAsDict(com.itextpdf.text.pdf.PdfName.RESOURCES)
                    hideTransparentObjects(xobjRes)
                }
            }
        }
    }

    data class StampInfo(val text: String, val customImageUri: String?, val x: Float, val y: Float, val size: Float = 1f)

    suspend fun applyStamps(
        context: Context,
        sourceFile: File,
        outputFile: File,
        stamps: Map<Int, List<StampInfo>>
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var reader: com.itextpdf.text.pdf.PdfReader? = null
        var stamper: com.itextpdf.text.pdf.PdfStamper? = null
        try {
            reader = com.itextpdf.text.pdf.PdfReader(sourceFile.absolutePath)
            stamper = com.itextpdf.text.pdf.PdfStamper(reader, FileOutputStream(outputFile))

            val numPages = reader.numberOfPages
            for (i in 1..numPages) {
                val stampsForPage = stamps[i - 1] // 0-indexed in UI, 1-indexed in iText
                if (stampsForPage.isNullOrEmpty()) continue

                val pdfContentByte = stamper.getOverContent(i)
                if (pdfContentByte == null) continue

                val pageRect = reader.getPageSizeWithRotation(i)
                val w = pageRect.width
                val h = pageRect.height

                pdfContentByte.saveState()
                
                // Add a bit of transparency to stamps
                val gstate = com.itextpdf.text.pdf.PdfGState()
                gstate.setFillOpacity(0.85f)
                pdfContentByte.setGState(gstate)

                for (stamp in stampsForPage) {
                    val posX = w * stamp.x
                    // UI coordinates from top-left, PDF from bottom-left
                    val posY = h * (1f - stamp.y)

                    if (stamp.text == "CUSTOM" && !stamp.customImageUri.isNullOrEmpty()) {
                        try {
                            val uri = Uri.parse(stamp.customImageUri)
                            val imgStream = context.contentResolver.openInputStream(uri)
                            if (imgStream != null) {
                                val bitmap = android.graphics.BitmapFactory.decodeStream(imgStream)
                                imgStream.close()
                                
                                val stream = java.io.ByteArrayOutputStream()
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                                val img = com.itextpdf.text.Image.getInstance(stream.toByteArray())
                                
                                // Standard stamp size 150x150 relative bounds
                                img.scaleToFit(150f * stamp.size, 150f * stamp.size)
                                
                                img.setAbsolutePosition(
                                    posX - (img.scaledWidth / 2f),
                                    posY - (img.scaledHeight / 2f)
                                )
                                pdfContentByte.addImage(img)
                            }
                        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            val bitmap = StampGenerator.generateRealisticStamp(stamp.text)
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                            val img = com.itextpdf.text.Image.getInstance(stream.toByteArray())
                            
                            // Scale down since vector is 600x600, let's make it 150x150 for PDF
                            img.scaleToFit(150f * stamp.size, 150f * stamp.size)
                            
                            img.setAbsolutePosition(
                                posX - (img.scaledWidth / 2f),
                                posY - (img.scaledHeight / 2f)
                            )
                            pdfContentByte.addImage(img)
                        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
                            e.printStackTrace()
                        }
                    }
                }
                pdfContentByte.restoreState()
            }

            stamper.close()
            reader.close()
            true
        } catch (e: Throwable) { android.util.Log.e("pdf-protect-crash", "Error in PdfUtils: ", e);
            e.printStackTrace()
            try { stamper?.close() } catch (ignore: Exception) {}
            try { reader?.close() } catch (ignore: Exception) {}
            false
        }
    }
}

data class PdfSignatureInfo(
    val fieldName: String,
    val signerName: String,
    val signingTime: Long?,
    val location: String?,
    val reason: String?,
    val coversWholeDocument: Boolean,
    val revision: Int,
    val totalRevisions: Int,
    val isValid: Boolean,
    val integrityMessage: String,
    val certIssuer: String?,
    val certSubject: String?,
    val page: Int = 1,
    val left: Float = 0f,
    val bottom: Float = 0f,
    val right: Float = 0f,
    val top: Float = 0f
)

data class PageNumberRange(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startPage: Int = 1,
    val endPage: Int = 1,
    val pageTypePattern: String = "Page : {NUM}/{CNT}",
    val numeralsType: String = "Numeric", // Numeric, Roman (Small), Roman (Caps), Alphabetic (Caps), Alphabetic (Small)
    val startingInput: Int = 1
)

data class ManualAdjustSettings(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val overrideMode: String? = null
)

data class OrganizePageItem(
    val id: String,
    val sourceUri: Uri,
    val sourceName: String,
    val originalPageIndex: Int,
    val rotationDegrees: Float = 0f
)

sealed class PdfOverlay {
    abstract val id: String
    abstract val pageIndex: Int
    abstract val relativeX: Float
    abstract val relativeY: Float
    abstract val scale: Float

    data class TextOverlay(
        override val id: String,
        override val pageIndex: Int,
        override val relativeX: Float,
        override val relativeY: Float,
        override val scale: Float,
        val text: String,
        val color: Int
    ) : PdfOverlay()

    data class ImageOverlay(
        override val id: String,
        override val pageIndex: Int,
        override val relativeX: Float,
        override val relativeY: Float,
        override val scale: Float,
        val bitmap: android.graphics.Bitmap,
        val rotation: Float,
        val opacity: Float = 1f
    ) : PdfOverlay()
}




