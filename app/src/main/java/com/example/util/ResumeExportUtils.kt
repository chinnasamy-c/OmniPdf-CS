package com.example.util

import android.content.Context
import android.os.Environment
import com.example.ui.ResumeData
import com.example.ui.SummaryType
import com.example.ui.AutoSummaryTone
import com.example.ui.ENTRY_LEVEL_PATTERN
import com.example.ui.BALANCED_PATTERN
import com.example.ui.ACTION_PATTERN
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import java.io.File
import java.io.FileOutputStream

object ResumeExportUtils {

    fun generateResumePdf(context: Context, data: ResumeData): File? {
        return try {
            val fileDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = File(fileDir, "Resume_${System.currentTimeMillis()}.pdf")
            val document = Document(PageSize.A4, 30f, 30f, 30f, 30f)
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            when (data.selectedTemplate) {
                com.example.ui.ResumeTemplate.CLASSIC -> buildClassic(document, context, data)
                com.example.ui.ResumeTemplate.MODERN -> buildModern(document, context, data)
                com.example.ui.ResumeTemplate.ELEGANT -> buildElegant(document, context, data)
                com.example.ui.ResumeTemplate.MINIMALIST -> buildMinimalist(document, context, data)
                com.example.ui.ResumeTemplate.PROFESSIONAL -> buildProfessional(document, context, data)
                com.example.ui.ResumeTemplate.EXECUTIVE -> buildExecutive(document, context, data)
                com.example.ui.ResumeTemplate.CREATIVE -> buildCreative(document, context, data)
                com.example.ui.ResumeTemplate.TECH -> buildTech(document, context, data)
                com.example.ui.ResumeTemplate.CLEAN -> buildClean(document, context, data)
                com.example.ui.ResumeTemplate.CORPORATE -> buildCorporate(document, context, data)
            }

            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getSummaryText(data: ResumeData): String {
        return if (data.summaryInfo.type == SummaryType.MANUAL) {
            data.summaryInfo.manualText
        } else {
            val jt = data.summaryInfo.autoJobTitle.ifBlank { "Professional" }
            val pat = when(data.summaryInfo.autoTone) {
                AutoSummaryTone.ENTRY_LEVEL -> ENTRY_LEVEL_PATTERN
                AutoSummaryTone.BALANCED -> BALANCED_PATTERN
                AutoSummaryTone.ACTION -> ACTION_PATTERN
            }
            pat.replace("[%s]", jt)
        }
    }
    
    private fun getDeclarationText(role: String): String {
        return "I hereby declare that the above information is true and correct to the best of my knowledge and belief. I take full responsibility for its accuracy and affirm my commitment to the $role profession with sincerity and integrity."
    }

    // Safely reads, crops to square, and scales down user profile photo
    private fun getProfileImage(context: Context, uriString: String, targetSize: Float): Image? {
        if (uriString.isBlank()) return null
        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) return null
            
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            var scale = 1
            val maxDim = 300
            while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) {
                scale *= 2
            }
            
            val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = scale }
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
            
            val minDim = Math.min(bmp.width, bmp.height)
            val cropped = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - minDim) / 2, (bmp.height - minDim) / 2, minDim, minDim)
            val scaled = android.graphics.Bitmap.createScaledBitmap(cropped, 120, 120, true)
            
            val stream = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, stream)
            val itextImg = Image.getInstance(stream.toByteArray())
            itextImg.scaleAbsolute(targetSize, targetSize)
            itextImg
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 1. CLASSIC TEMPLATE (Center Aligned, Serif, Traditional Layout)
    private fun buildClassic(document: Document, context: Context, data: ResumeData) {
        val nameFont = Font(Font.FontFamily.TIMES_ROMAN, 24f, Font.BOLD, BaseColor.BLACK)
        val titleFont = Font(Font.FontFamily.TIMES_ROMAN, 13f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.TIMES_ROMAN, 13f, Font.BOLD, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.NORMAL, BaseColor.BLACK)

        // Title Block
        val headerTable = PdfPTable(1).apply { widthPercentage = 100f }
        
        // Circular/Square profile image at the top if present
        getProfileImage(context, data.personalInfo.photoUri, 75f)?.let { img ->
            img.alignment = Element.ALIGN_CENTER
            val cellImg = PdfPCell().apply {
                border = PdfPCell.NO_BORDER
                horizontalAlignment = Element.ALIGN_CENTER
                paddingBottom = 8f
                addElement(img)
            }
            headerTable.addCell(cellImg)
        }

        val nameCell = PdfPCell().apply {
            border = PdfPCell.NO_BORDER
            horizontalAlignment = Element.ALIGN_CENTER
            addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont).apply { alignment = Element.ALIGN_CENTER })
        }
        headerTable.addCell(nameCell)

        if (data.personalInfo.professionalTitle.isNotBlank()) {
            val titleCell = PdfPCell().apply {
                border = PdfPCell.NO_BORDER
                horizontalAlignment = Element.ALIGN_CENTER
                addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 4f })
            }
            headerTable.addCell(titleCell)
        }

        val contacts = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contacts.isNotEmpty()) {
            val contactsCell = PdfPCell().apply {
                border = PdfPCell.NO_BORDER
                horizontalAlignment = Element.ALIGN_CENTER
                addElement(Paragraph(contacts.joinToString("  •  "), normalFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 8f })
            }
            headerTable.addCell(contactsCell)
        }
        document.add(headerTable)
        document.add(LineSeparator(1f, 100f, BaseColor.BLACK, Element.ALIGN_CENTER, -4f))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "—", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = BaseColor.BLACK, sectionSpacing = 14f
        )
    }

    // 2. MODERN TEMPLATE (2-Column Left Sidebar & Right Content)
    private fun buildModern(document: Document, context: Context, data: ResumeData) {
        val sidebarBgColor = BaseColor(44, 62, 80) // Dark Slate Blue
        
        val nameFont = Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, BaseColor(41, 128, 185))
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFontRight = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, BaseColor(41, 128, 185))
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)

        val sidebarHeadingFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
        val sidebarTextFont = Font(Font.FontFamily.HELVETICA, 9.5f, Font.NORMAL, BaseColor(220, 224, 230))
        val sidebarTextBold = Font(Font.FontFamily.HELVETICA, 9.5f, Font.BOLD, BaseColor.WHITE)

        val table = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(3.2f, 6.8f))
        }

        // --- LEFT COLUMN (SIDEBAR) ---
        val sidebarCell = PdfPCell().apply {
            backgroundColor = sidebarBgColor
            border = PdfPCell.NO_BORDER
            paddingLeft = 14f
            paddingRight = 14f
            paddingTop = 15f
            paddingBottom = 15f
        }

        // Sidebar Profile Image
        getProfileImage(context, data.personalInfo.photoUri, 80f)?.let { img ->
            img.alignment = Element.ALIGN_CENTER
            sidebarCell.addElement(img)
            sidebarCell.addElement(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 6f)))
        }

        // Contact Info
        sidebarCell.addElement(Paragraph("CONTACT INFO", sidebarHeadingFont).apply { spacingBefore = 10f; spacingAfter = 4f })
        sidebarCell.addElement(LineSeparator(1f, 100f, BaseColor(52, 152, 219), Element.ALIGN_LEFT, -2f))
        sidebarCell.addElement(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 2f)))

        if (data.personalInfo.email.isNotBlank()) {
            sidebarCell.addElement(Paragraph("Email:", sidebarTextBold))
            sidebarCell.addElement(Paragraph(data.personalInfo.email, sidebarTextFont).apply { spacingAfter = 6f })
        }
        if (data.personalInfo.phone.isNotBlank()) {
            sidebarCell.addElement(Paragraph("Phone:", sidebarTextBold))
            sidebarCell.addElement(Paragraph(data.personalInfo.phone, sidebarTextFont).apply { spacingAfter = 6f })
        }
        if (data.personalInfo.location.isNotBlank()) {
            sidebarCell.addElement(Paragraph("Location:", sidebarTextBold))
            sidebarCell.addElement(Paragraph(data.personalInfo.location, sidebarTextFont).apply { spacingAfter = 6f })
        }
        if (data.personalInfo.socialMedia.isNotBlank()) {
            sidebarCell.addElement(Paragraph("LinkedIn/Web:", sidebarTextBold))
            sidebarCell.addElement(Paragraph(data.personalInfo.socialMedia, sidebarTextFont).apply { spacingAfter = 6f })
        }

        // Skills inside Sidebar
        val validSk = data.skills.filter { sk -> sk.name.isNotBlank() }
        if (validSk.isNotEmpty()) {
            sidebarCell.addElement(Paragraph("CORE SKILLS", sidebarHeadingFont).apply { spacingBefore = 15f; spacingAfter = 4f })
            sidebarCell.addElement(LineSeparator(1f, 100f, BaseColor(52, 152, 219), Element.ALIGN_LEFT, -2f))
            sidebarCell.addElement(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 2f)))
            validSk.forEach { sk ->
                val lvlText = if (sk.level != "Foundational") " (${sk.level})" else ""
                sidebarCell.addElement(Paragraph("• ${sk.name}$lvlText", sidebarTextFont).apply { spacingAfter = 4f })
            }
        }

        // Languages inside Sidebar
        val validLang = data.additionalInfo.languages.filter { it.name.isNotBlank() }
        if (validLang.isNotEmpty()) {
            sidebarCell.addElement(Paragraph("LANGUAGES", sidebarHeadingFont).apply { spacingBefore = 15f; spacingAfter = 4f })
            sidebarCell.addElement(LineSeparator(1f, 100f, BaseColor(52, 152, 219), Element.ALIGN_LEFT, -2f))
            sidebarCell.addElement(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 2f)))
            validLang.forEach { lang ->
                sidebarCell.addElement(Paragraph("• ${lang.name} (${lang.level})", sidebarTextFont).apply { spacingAfter = 4f })
            }
        }

        // DOB & Details
        val addInfo = data.additionalInfo
        if (addInfo.dob.isNotBlank() || addInfo.gender.isNotBlank() || addInfo.maritalStatus.isNotBlank() || addInfo.hobby.isNotBlank()) {
            sidebarCell.addElement(Paragraph("CREDENTIALS", sidebarHeadingFont).apply { spacingBefore = 15f; spacingAfter = 4f })
            sidebarCell.addElement(LineSeparator(1f, 100f, BaseColor(52, 152, 219), Element.ALIGN_LEFT, -2f))
            sidebarCell.addElement(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 2f)))
            if (addInfo.dob.isNotBlank()) sidebarCell.addElement(Paragraph("DOB: ${addInfo.dob}", sidebarTextFont).apply { spacingAfter = 3f })
            if (addInfo.gender.isNotBlank()) sidebarCell.addElement(Paragraph("Gender: ${addInfo.gender}", sidebarTextFont).apply { spacingAfter = 3f })
            if (addInfo.maritalStatus.isNotBlank()) sidebarCell.addElement(Paragraph("Status: ${addInfo.maritalStatus}", sidebarTextFont).apply { spacingAfter = 3f })
            if (addInfo.hobby.isNotBlank()) sidebarCell.addElement(Paragraph("Hobbies: ${addInfo.hobby}", sidebarTextFont).apply { spacingAfter = 3f })
        }

        table.addCell(sidebarCell)

        // --- RIGHT COLUMN (CONTENT) ---
        val mainCell = PdfPCell().apply {
            border = PdfPCell.NO_BORDER
            paddingLeft = 18f
            paddingRight = 10f
            paddingTop = 15f
            paddingBottom = 15f
        }

        mainCell.addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            mainCell.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { spacingAfter = 12f })
        }

        fun addRightSectionHeader(title: String) {
            mainCell.addElement(Paragraph(title.uppercase(), sectionFontRight).apply { spacingBefore = 14f; spacingAfter = 4f })
            mainCell.addElement(LineSeparator(1f, 100f, BaseColor(41, 128, 185), Element.ALIGN_LEFT, -2f))
            mainCell.addElement(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 3f)))
        }

        // Summary
        val summary = getSummaryText(data)
        if (summary.isNotBlank()) {
            addRightSectionHeader("Professional Summary")
            mainCell.addElement(Paragraph(summary, normalFont).apply { alignment = Element.ALIGN_JUSTIFIED })
        }

        // Experience
        val validWork = data.workExperiences.filter { it.company.isNotBlank() || it.jobTitle.isNotBlank() }
        if (validWork.isNotEmpty()) {
            addRightSectionHeader("Experience")
            validWork.forEach { we ->
                val wp = Paragraph().apply { spacingBefore = 4f; spacingAfter = 2f }
                wp.add(Chunk(we.jobTitle, boldFont))
                if (we.company.isNotBlank()) wp.add(Chunk(" | ${we.company}", italicFont))
                val dateStr = listOf(we.startDate, we.endDate, we.duration).filter { it.isNotBlank() }.joinToString(" - ")
                if (dateStr.isNotBlank()) wp.add(Chunk("  ($dateStr)", normalFont))
                mainCell.addElement(wp)

                val resps = we.responsibilities.filter { it.isNotBlank() }
                if (resps.isNotEmpty()) {
                    val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply {
                        setListSymbol("• ")
                        symbolIndent = 8f
                    }
                    resps.forEach {
                        list.add(ListItem(Paragraph(it, normalFont).apply { alignment = Element.ALIGN_JUSTIFIED }))
                    }
                    mainCell.addElement(list)
                }
            }
        }

        // Education
        val validEdu = data.educations.filter { it.qualification.isNotBlank() || it.institute.isNotBlank() }
        if (validEdu.isNotEmpty()) {
            addRightSectionHeader("Education")
            validEdu.sortedByDescending { it.yearOfCompletion.toIntOrNull() ?: 0 }.forEach { ed ->
                val ep = Paragraph().apply { spacingBefore = 4f; spacingAfter = 2f }
                ep.add(Chunk(ed.qualification, boldFont))
                if (ed.institute.isNotBlank()) ep.add(Chunk(" | ${ed.institute}", normalFont))
                val details = listOf(ed.yearOfCompletion, ed.percentage, ed.board).filter { it.isNotBlank() }.joinToString(", ")
                if (details.isNotEmpty()) ep.add(Chunk("\n$details", italicFont))
                mainCell.addElement(ep)
            }
        }

        // Declaration
        if (data.declaration.role.isNotBlank()) {
            addRightSectionHeader("Declaration")
            mainCell.addElement(Paragraph(getDeclarationText(data.declaration.role), normalFont).apply { alignment = Element.ALIGN_JUSTIFIED; spacingBefore = 4f })
        }

        table.addCell(mainCell)
        document.add(table)
    }

    // 3. ELEGANT TEMPLATE (Stylish serif golden-accent layout)
    private fun buildElegant(document: Document, context: Context, data: ResumeData) {
        val elegantGold = BaseColor(184, 134, 11) // Dark Goldenrod
        val nameFont = Font(Font.FontFamily.TIMES_ROMAN, 26f, Font.NORMAL, elegantGold)
        val titleFont = Font(Font.FontFamily.TIMES_ROMAN, 12f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.TIMES_ROMAN, 13f, Font.BOLD, elegantGold)
        val boldFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.NORMAL, BaseColor(60, 60, 60))

        val headerTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(7.5f, 2.5f))
        }

        val textCell = PdfPCell().apply { border = PdfPCell.NO_BORDER }
        textCell.addElement(Paragraph(data.personalInfo.fullName, nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            textCell.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { spacingAfter = 4f })
        }
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        textCell.addElement(Paragraph(c.joinToString("   ◇   "), normalFont).apply { spacingAfter = 8f })
        headerTable.addCell(textCell)

        val imgCell = PdfPCell().apply { border = PdfPCell.NO_BORDER; horizontalAlignment = Element.ALIGN_RIGHT }
        getProfileImage(context, data.personalInfo.photoUri, 75f)?.let { img ->
            img.alignment = Element.ALIGN_RIGHT
            imgCell.addElement(img)
        }
        headerTable.addCell(imgCell)

        document.add(headerTable)
        document.add(LineSeparator(1.5f, 100f, elegantGold, Element.ALIGN_CENTER, -2f))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "◇", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = null, sectionSpacing = 15f
        )
    }

    // 4. MINIMALIST TEMPLATE (Ultra-slim, sophisticated sans-serif compact layout)
    private fun buildMinimalist(document: Document, context: Context, data: ResumeData) {
        val slateGray = BaseColor(51, 65, 85)
        val nameFont = Font(Font.FontFamily.HELVETICA, 21f, Font.BOLD, slateGray)
        val titleFont = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, slateGray)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.GRAY)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.DARK_GRAY)

        val headerTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(2.2f, 7.8f))
        }

        getProfileImage(context, data.personalInfo.photoUri, 70f)?.let { img ->
            img.alignment = Element.ALIGN_LEFT
            val cellImg = PdfPCell().apply { border = PdfPCell.NO_BORDER; paddingRight = 10f }
            cellImg.addElement(img)
            headerTable.addCell(cellImg)
        } ?: run {
            headerTable.setWidths(floatArrayOf(0.01f, 9.99f))
            headerTable.addCell(PdfPCell().apply { border = PdfPCell.NO_BORDER })
        }

        val textCell = PdfPCell().apply { border = PdfPCell.NO_BORDER }
        textCell.addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            textCell.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { spacingAfter = 4f })
        }
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        textCell.addElement(Paragraph(c.joinToString("  •  "), normalFont).apply { spacingAfter = 6f })
        headerTable.addCell(textCell)

        document.add(headerTable)
        document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 4f)))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "▪", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = null, sectionSpacing = 12f
        )
    }

    // 5. PROFESSIONAL TEMPLATE (Top crisp banner design, Bold corporate Navy style)
    private fun buildProfessional(document: Document, context: Context, data: ResumeData) {
        val navyBg = BaseColor(26, 54, 93)
        val nameFont = Font(Font.FontFamily.HELVETICA, 23f, Font.BOLD, BaseColor.WHITE)
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.ITALIC, BaseColor(200, 214, 229))
        val sectionFont = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, navyBg)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)

        // Banner Table
        val bannerTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(7.8f, 2.2f))
        }

        val textCell = PdfPCell().apply {
            backgroundColor = navyBg
            border = PdfPCell.NO_BORDER
            paddingLeft = 14f
            paddingTop = 14f
            paddingBottom = 14f
        }
        textCell.addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            textCell.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont))
        }
        bannerTable.addCell(textCell)

        val imgCell = PdfPCell().apply {
            backgroundColor = navyBg
            border = PdfPCell.NO_BORDER
            horizontalAlignment = Element.ALIGN_RIGHT
            verticalAlignment = Element.ALIGN_MIDDLE
            paddingRight = 14f
            paddingTop = 10f
            paddingBottom = 10f
        }
        getProfileImage(context, data.personalInfo.photoUri, 65f)?.let { img ->
            img.alignment = Element.ALIGN_RIGHT
            imgCell.addElement(img)
        }
        bannerTable.addCell(imgCell)

        document.add(bannerTable)

        // Contacts block right under the banner
        val contactsBlock = Paragraph().apply { spacingBefore = 8f; spacingAfter = 8f }
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        contactsBlock.add(Chunk(c.joinToString("  |  "), normalFont))
        contactsBlock.alignment = Element.ALIGN_CENTER
        document.add(contactsBlock)
        document.add(LineSeparator(1.5f, 100f, navyBg, Element.ALIGN_CENTER, -4f))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "▸", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = BaseColor.LIGHT_GRAY, sectionSpacing = 14f
        )
    }

    // 6. EXECUTIVE TEMPLATE (Elegant Wall Street double line horizontal rules)
    private fun buildExecutive(document: Document, context: Context, data: ResumeData) {
        val charcoal = BaseColor(44, 62, 80)
        val nameFont = Font(Font.FontFamily.TIMES_ROMAN, 24f, Font.BOLD, charcoal)
        val titleFont = Font(Font.FontFamily.TIMES_ROMAN, 12f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.TIMES_ROMAN, 13f, Font.BOLD, charcoal)
        val boldFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.TIMES_ROMAN, 10.5f, Font.NORMAL, BaseColor.BLACK)

        val executiveHeader = PdfPTable(3).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(2.5f, 5f, 2.5f))
        }

        val leftImgCell = PdfPCell().apply { border = PdfPCell.NO_BORDER }
        getProfileImage(context, data.personalInfo.photoUri, 75f)?.let { img ->
            img.alignment = Element.ALIGN_LEFT
            leftImgCell.addElement(img)
        }
        executiveHeader.addCell(leftImgCell)

        val centerText = PdfPCell().apply { border = PdfPCell.NO_BORDER; horizontalAlignment = Element.ALIGN_CENTER }
        centerText.addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont).apply { alignment = Element.ALIGN_CENTER })
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            centerText.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 4f })
        }
        val contacts = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contacts.isNotEmpty()) {
            centerText.addElement(Paragraph(contacts.joinToString("  |  "), normalFont).apply { alignment = Element.ALIGN_CENTER })
        }
        executiveHeader.addCell(centerText)

        val rightCellDummy = PdfPCell().apply { border = PdfPCell.NO_BORDER }
        executiveHeader.addCell(rightCellDummy)

        document.add(executiveHeader)
        document.add(Paragraph(" ", Font(Font.FontFamily.TIMES_ROMAN, 4f)))
        document.add(LineSeparator(1.5f, 100f, charcoal, Element.ALIGN_CENTER, -2f))
        document.add(LineSeparator(0.5f, 100f, charcoal, Element.ALIGN_CENTER, -6f))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "●", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = charcoal, sectionSpacing = 15f
        )
    }

    // 7. CREATIVE TEMPLATE (Artistic layouts, deep crimson, high contrast)
    private fun buildCreative(document: Document, context: Context, data: ResumeData) {
        val crimsonRed = BaseColor(192, 57, 43)
        val nameFont = Font(Font.FontFamily.HELVETICA, 27f, Font.BOLD, crimsonRed)
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, crimsonRed)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10.5f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10.5f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10.5f, Font.NORMAL, BaseColor.BLACK)

        val headerTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(2.5f, 7.5f))
        }

        getProfileImage(context, data.personalInfo.photoUri, 80f)?.let { img ->
            img.alignment = Element.ALIGN_LEFT
            val cellImg = PdfPCell().apply { border = PdfPCell.NO_BORDER; paddingRight = 10f }
            cellImg.addElement(img)
            headerTable.addCell(cellImg)
        } ?: run {
            headerTable.setWidths(floatArrayOf(0.01f, 9.99f))
            headerTable.addCell(PdfPCell().apply { border = PdfPCell.NO_BORDER })
        }

        val textCell = PdfPCell().apply { border = PdfPCell.NO_BORDER }
        textCell.addElement(Paragraph(data.personalInfo.fullName, nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            textCell.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { spacingAfter = 4f })
        }
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        textCell.addElement(Paragraph(c.joinToString("   ★   "), normalFont).apply { spacingAfter = 8f })
        headerTable.addCell(textCell)

        document.add(headerTable)
        document.add(LineSeparator(2f, 100f, crimsonRed, Element.ALIGN_CENTER, -4f))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "★", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = BaseColor.LIGHT_GRAY, sectionSpacing = 16f
        )
    }

    // 8. TECH TEMPLATE (Interactive programmer feel, monospace Courier themes)
    private fun buildTech(document: Document, context: Context, data: ResumeData) {
        val techColor = BaseColor(39, 174, 96) // Terminal Green
        val nameFont = Font(Font.FontFamily.COURIER, 21f, Font.BOLD, techColor)
        val titleFont = Font(Font.FontFamily.COURIER, 12f, Font.NORMAL, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.COURIER, 13f, Font.BOLD, techColor)
        val boldFont = Font(Font.FontFamily.COURIER, 10f, Font.BOLD, BaseColor.BLACK)
        val normalFont = Font(Font.FontFamily.COURIER, 10f, Font.NORMAL, BaseColor.BLACK)

        val headerTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(7.5f, 2.5f))
        }

        val textCell = PdfPCell().apply { border = PdfPCell.NO_BORDER }
        textCell.addElement(Paragraph("< " + data.personalInfo.fullName.uppercase() + " />", nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            textCell.addElement(Paragraph("// " + data.personalInfo.professionalTitle, titleFont).apply { spacingAfter = 4f })
        }
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        textCell.addElement(Paragraph("/* " + c.joinToString(" | ") + " */", normalFont).apply { spacingAfter = 8f })
        headerTable.addCell(textCell)

        val imgCell = PdfPCell().apply { border = PdfPCell.NO_BORDER; horizontalAlignment = Element.ALIGN_RIGHT }
        getProfileImage(context, data.personalInfo.photoUri, 75f)?.let { img ->
            img.alignment = Element.ALIGN_RIGHT
            imgCell.addElement(img)
        }
        headerTable.addCell(imgCell)

        document.add(headerTable)
        document.add(LineSeparator(1.5f, 100f, techColor, Element.ALIGN_CENTER, -4f))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, normalFont, normalFont,
            bullet = ">>>", justifyText = false, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = techColor, sectionSpacing = 14f
        )
    }

    // 9. CLEAN TEMPLATE (Card layout frames, round outlines, modern slate gray)
    private fun buildClean(document: Document, context: Context, data: ResumeData) {
        val oceanBlue = BaseColor(0, 150, 136)
        val nameFont = Font(Font.FontFamily.HELVETICA, 23f, Font.BOLD, oceanBlue)
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, oceanBlue)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10.5f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10.5f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10.5f, Font.NORMAL, BaseColor.BLACK)

        val table = PdfPTable(1).apply { widthPercentage = 100f }
        
        getProfileImage(context, data.personalInfo.photoUri, 80f)?.let { img ->
            img.alignment = Element.ALIGN_CENTER
            val cellImg = PdfPCell().apply { border = PdfPCell.NO_BORDER; paddingBottom = 6f; horizontalAlignment = Element.ALIGN_CENTER }
            cellImg.addElement(img)
            table.addCell(cellImg)
        }

        val textCell = PdfPCell().apply { border = PdfPCell.NO_BORDER; horizontalAlignment = Element.ALIGN_CENTER }
        textCell.addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont).apply { alignment = Element.ALIGN_CENTER })
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            textCell.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 4f })
        }
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location).filter { it.isNotBlank() }
        textCell.addElement(Paragraph(c.joinToString("   ○   "), normalFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 8f })
        table.addCell(textCell)

        document.add(table)
        document.add(LineSeparator(1f, 100f, oceanBlue, Element.ALIGN_CENTER, -4f))

        renderGeneralSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "○", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = null, sectionSpacing = 14f
        )
    }

    // 10. CORPORATE TEMPLATE (Premium shade header block with clean corporate lines)
    private fun buildCorporate(document: Document, context: Context, data: ResumeData) {
        val corpColor = BaseColor(52, 73, 94)
        val nameFont = Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, corpColor)
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)

        val headerTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(7.5f, 2.5f))
        }

        val textCell = PdfPCell().apply { border = PdfPCell.NO_BORDER }
        textCell.addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) {
            textCell.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { spacingAfter = 4f })
        }
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        textCell.addElement(Paragraph(c.joinToString("  |  "), normalFont).apply { spacingAfter = 8f })
        headerTable.addCell(textCell)

        val imgCell = PdfPCell().apply { border = PdfPCell.NO_BORDER; horizontalAlignment = Element.ALIGN_RIGHT }
        getProfileImage(context, data.personalInfo.photoUri, 75f)?.let { img ->
            img.alignment = Element.ALIGN_RIGHT
            imgCell.addElement(img)
        }
        headerTable.addCell(imgCell)

        document.add(headerTable)

        // Custom corporate header blocks
        val headerRenderer: (String) -> Unit = { title ->
            val table = PdfPTable(1).apply { widthPercentage = 100f; spacingBefore = 12f; spacingAfter = 6f }
            val cell = PdfPCell(Phrase(title.uppercase(), sectionFont)).apply {
                backgroundColor = corpColor
                paddingTop = 5f
                paddingBottom = 5f
                paddingLeft = 7f
                paddingRight = 7f
                border = PdfPCell.NO_BORDER
            }
            table.addCell(cell)
            document.add(table)
        }

        renderGeneralSections(
            document = document, data = data, normalFont = normalFont, sectionFont = sectionFont,
            boldFont = boldFont, italicFont = italicFont, contactFont = normalFont,
            bullet = "■", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = null, sectionSpacing = 0f, customHeaderRenderer = headerRenderer
        )
    }

    // --- REUSABLE GENERAL SECTIONS RENDERER ---
    private fun renderGeneralSections(
        document: Document,
        data: ResumeData,
        normalFont: Font,
        sectionFont: Font,
        boldFont: Font,
        italicFont: Font,
        contactFont: Font,
        bullet: String,
        justifyText: Boolean,
        headerAlign: Int,
        headerLineColors: BaseColor?,
        sectionSpacing: Float,
        customHeaderRenderer: ((String) -> Unit)? = null
    ) {
        val textAlignment = if (justifyText) Element.ALIGN_JUSTIFIED else Element.ALIGN_LEFT

        fun handleHeader(title: String) {
            if (customHeaderRenderer != null) {
                customHeaderRenderer(title)
                return
            }
            val p = Paragraph(title.uppercase(), sectionFont).apply { 
                this.alignment = headerAlign
                spacingBefore = sectionSpacing
                spacingAfter = 3f
            }
            document.add(p)
            if (headerLineColors != null) {
                document.add(LineSeparator(1f, 100f, headerLineColors, Element.ALIGN_CENTER, -3f))
                document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 1f)))
            } else {
                document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 3f)))
            }
        }

        // Summary
        val summary = getSummaryText(data)
        if (summary.isNotBlank()) {
            handleHeader("Professional Summary")
            document.add(Paragraph(summary, normalFont).apply { this.alignment = textAlignment; spacingAfter = 4f })
        }

        // Experience
        val validWork = data.workExperiences.filter { it.company.isNotBlank() || it.jobTitle.isNotBlank() }
        if (validWork.isNotEmpty()) {
            handleHeader("Experience")
            validWork.forEach { we ->
                val wp = Paragraph().apply { spacingBefore = 4f; spacingAfter = 2f }
                wp.add(Chunk(we.jobTitle, boldFont))
                if (we.company.isNotBlank()) wp.add(Chunk(" | ${we.company}", italicFont))
                val dateStr = listOf(we.startDate, we.endDate, we.duration).filter { it.isNotBlank() }.joinToString(" - ")
                if (dateStr.isNotBlank()) {
                    wp.add(Chunk("  ($dateStr)", contactFont))
                }
                document.add(wp)

                val resps = we.responsibilities.filter { it.isNotBlank() }
                if (resps.isNotEmpty()) {
                    val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { 
                        setListSymbol("$bullet  ")
                        symbolIndent = 12f
                    }
                    resps.forEach {
                        list.add(ListItem(Paragraph(it, normalFont).apply { this.alignment = textAlignment }))
                    }
                    document.add(list)
                }
            }
        }

        // Education
        val validEdu = data.educations.filter { it.qualification.isNotBlank() || it.institute.isNotBlank() }
        if (validEdu.isNotEmpty()) {
            handleHeader("Education")
            validEdu.sortedByDescending { it.yearOfCompletion.toIntOrNull() ?: 0 }.forEach { ed ->
                val ep = Paragraph().apply { spacingBefore = 4f; spacingAfter = 2f; this.alignment = textAlignment }
                ep.add(Chunk(ed.qualification, boldFont))
                if (ed.institute.isNotBlank()) ep.add(Chunk(" | ${ed.institute}", normalFont))
                val details = listOf(ed.yearOfCompletion, ed.percentage, ed.board).filter { it.isNotBlank() }.joinToString(", ")
                if (details.isNotEmpty()) ep.add(Chunk("\n$details", contactFont))
                document.add(ep)
            }
        }

        // Skills
        val validSk = data.skills.filter { it.name.isNotBlank() }
        if (validSk.isNotEmpty()) {
            handleHeader("Skills")
            val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { setListSymbol("$bullet  "); symbolIndent = 12f }
            validSk.forEach { sk ->
                val lvl = if (sk.level.isNotBlank() && sk.level != "Foundational") " (${sk.level})" else ""
                list.add(ListItem(Paragraph("${sk.name}$lvl", normalFont).apply { this.alignment = Element.ALIGN_LEFT }))
            }
            document.add(list)
        }

        // Additional Profile Info (formerly 'Additional Information')
        val infoList = mutableListOf<String>()
        if (data.additionalInfo.dob.isNotBlank()) infoList.add("Date of Birth: ${data.additionalInfo.dob}")
        if (data.additionalInfo.gender.isNotBlank()) infoList.add("Gender: ${data.additionalInfo.gender}")
        if (data.additionalInfo.maritalStatus.isNotBlank()) infoList.add("Marital Status: ${data.additionalInfo.maritalStatus}")
        if (data.additionalInfo.hobby.isNotBlank()) infoList.add("Hobbies & Interests: ${data.additionalInfo.hobby}")
        
        val validLang = data.additionalInfo.languages.filter { it.name.isNotBlank() }
        if (validLang.isNotEmpty()) {
            infoList.add("Languages Spoken: " + validLang.joinToString(", ") { "${it.name} (${it.level})" })
        }
        
        if (infoList.isNotEmpty()) {
            handleHeader("Credentials & Profile Details")
            val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { setListSymbol("$bullet  "); symbolIndent = 12f }
            infoList.forEach { info ->
                list.add(ListItem(Paragraph(info, normalFont).apply { this.alignment = textAlignment }))
            }
            document.add(list)
        }

        // Declaration
        if (data.declaration.role.isNotBlank()) {
            handleHeader("Declaration")
            document.add(Paragraph(getDeclarationText(data.declaration.role), normalFont).apply { this.alignment = textAlignment; spacingBefore = 4f })
        }
    }
}
