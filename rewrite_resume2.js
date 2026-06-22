const fs = require('fs');

const content = `package com.example.util

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
            val file = File(fileDir, "Resume_\${System.currentTimeMillis()}.pdf")
            val document = Document(PageSize.A4, 30f, 30f, 30f, 30f)
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            when (data.selectedTemplate) {
                com.example.ui.ResumeTemplate.CLASSIC -> buildClassic(document, data)
                com.example.ui.ResumeTemplate.MODERN -> buildModern(document, data)
                com.example.ui.ResumeTemplate.ELEGANT -> buildElegant(document, data)
                com.example.ui.ResumeTemplate.MINIMALIST -> buildMinimalist(document, data)
                com.example.ui.ResumeTemplate.PROFESSIONAL -> buildProfessional(document, data)
                com.example.ui.ResumeTemplate.EXECUTIVE -> buildExecutive(document, data)
                com.example.ui.ResumeTemplate.CREATIVE -> buildCreative(document, data)
                com.example.ui.ResumeTemplate.TECH -> buildTech(document, data)
                com.example.ui.ResumeTemplate.CLEAN -> buildClean(document, data)
                com.example.ui.ResumeTemplate.CORPORATE -> buildCorporate(document, data)
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
        return "I hereby declare that the above information is true and correct to the best of my knowledge and belief. I take full responsibility for its accuracy and affirm my commitment to the \$role profession with sincerity and integrity."
    }

    // 1. CLASSIC
    private fun buildClassic(document: Document, data: ResumeData) {
        val nameFont = Font(Font.FontFamily.TIMES_ROMAN, 24f, Font.BOLD, BaseColor.BLACK)
        val titleFont = Font(Font.FontFamily.TIMES_ROMAN, 14f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.TIMES_ROMAN, 14f, Font.BOLD, BaseColor.BLACK)
        val normalFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.NORMAL, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.ITALIC, BaseColor.DARK_GRAY)

        val namePhrase = Paragraph(data.personalInfo.fullName.uppercase(), nameFont).apply { alignment = Element.ALIGN_CENTER }
        document.add(namePhrase)
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter=4f })
        
        val contactItems = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contactItems.isNotEmpty()) {
            document.add(Paragraph(contactItems.joinToString("  |  "), normalFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 12f })
            document.add(LineSeparator(1f, 100f, BaseColor.BLACK, Element.ALIGN_CENTER, -2f))
        }

        renderSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "-", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = BaseColor.BLACK, sectionSpacing = 10f
        )
    }

    // 2. MODERN
    private fun buildModern(document: Document, data: ResumeData) {
        val mainColor = BaseColor(41, 128, 185)
        val nameFont = Font(Font.FontFamily.HELVETICA, 26f, Font.BOLD, mainColor)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.DARK_GRAY)
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.ITALIC, BaseColor.GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, mainColor)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.GRAY)

        document.add(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont))
        
        val contactItems = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contactItems.isNotEmpty()) {
            document.add(Paragraph(contactItems.joinToString("   •   "), normalFont).apply { spacingAfter = 8f; spacingBefore=4f })
        }

        renderSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "\\u2022", justifyText = false, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = mainColor, sectionSpacing = 12f
        )
    }

    // 3. ELEGANT
    private fun buildElegant(document: Document, data: ResumeData) {
        val nameFont = Font(Font.FontFamily.TIMES_ROMAN, 24f, Font.NORMAL, BaseColor.BLACK)
        val titleFont = Font(Font.FontFamily.TIMES_ROMAN, 12f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.TIMES_ROMAN, 14f, Font.ITALIC, BaseColor.BLACK)
        val normalFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.NORMAL, BaseColor(50,50,50))
        val boldFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.BOLD, BaseColor.BLACK)

        document.add(Paragraph(data.personalInfo.fullName, nameFont).apply { alignment = Element.ALIGN_CENTER })
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { alignment = Element.ALIGN_CENTER })
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (c.isNotEmpty()) {
            val p = Paragraph(c.joinToString("   \\u25C7   "), normalFont) // diamond
            p.alignment = Element.ALIGN_CENTER
            p.spacingAfter = 15f
            document.add(p)
        }

        renderSections(
            document, data, normalFont, sectionFont, boldFont, titleFont, normalFont,
            bullet = "\\u25C6", justifyText = true, headerAlign = Element.ALIGN_CENTER,
            headerLineColors = null, sectionSpacing = 14f
        )
    }

    // 4. MINIMALIST
    private fun buildMinimalist(document: Document, data: ResumeData) {
        val nameFont = Font(Font.FontFamily.HELVETICA, 20f, Font.NORMAL, BaseColor.BLACK)
        val titleFont = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.DARK_GRAY)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)

        document.add(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        val t = Paragraph()
        if (data.personalInfo.professionalTitle.isNotBlank()) t.add(Chunk(data.personalInfo.professionalTitle + "   ", titleFont))
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }.joinToString("  ")
        t.add(Chunk(c, normalFont))
        t.spacingAfter = 15f
        document.add(t)

        renderSections(
            document, data, normalFont, sectionFont, boldFont, titleFont, normalFont,
            bullet = "\\u25AA", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = null, sectionSpacing = 16f
        )
    }

    // 5. PROFESSIONAL
    private fun buildProfessional(document: Document, data: ResumeData) {
        val darkBlue = BaseColor(0, 51, 102)
        val nameFont = Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, darkBlue)
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, darkBlue)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)

        val table = PdfPTable(2).apply { widthPercentage = 100f; setWidths(floatArrayOf(6f, 4f)) }
        val lc = PdfPCell().apply { border = PdfPCell.NO_BORDER; addElement(Paragraph(data.personalInfo.fullName.uppercase(), nameFont)) }
        if (data.personalInfo.professionalTitle.isNotBlank()) lc.addElement(Paragraph(data.personalInfo.professionalTitle, titleFont))
        table.addCell(lc)
        
        val rc = PdfPCell().apply { border = PdfPCell.NO_BORDER; horizontalAlignment = Element.ALIGN_RIGHT }
        val rp = Paragraph().apply { alignment = Element.ALIGN_RIGHT }
        listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }.forEach { rp.add(Chunk(it + "\\n", normalFont)) }
        rc.addElement(rp)
        table.addCell(rc)
        document.add(table)
        document.add(LineSeparator(2f, 100f, darkBlue, Element.ALIGN_CENTER, -2f))

        renderSections(
            document, data, normalFont, sectionFont, boldFont, titleFont, normalFont,
            bullet = "\\u2023", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = BaseColor.LIGHT_GRAY, sectionSpacing = 12f
        )
    }

    // 6. EXECUTIVE
    private fun buildExecutive(document: Document, data: ResumeData) {
        val execColor = BaseColor(0, 34, 68)
        val nameFont = Font(Font.FontFamily.TIMES_ROMAN, 26f, Font.BOLD, execColor)
        val titleFont = Font(Font.FontFamily.TIMES_ROMAN, 14f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.TIMES_ROMAN, 14f, Font.BOLD, execColor)
        val normalFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.NORMAL, BaseColor(20,20,20))
        val boldFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.BOLD, BaseColor.BLACK)

        document.add(Paragraph(data.personalInfo.fullName.uppercase(), nameFont).apply { alignment = Element.ALIGN_CENTER })
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { alignment = Element.ALIGN_CENTER })
        
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        document.add(Paragraph(c.joinToString("   |   "), normalFont).apply { alignment = Element.ALIGN_CENTER; spacingBefore=4f; spacingAfter = 4f })
        document.add(LineSeparator(1f, 100f, execColor, Element.ALIGN_CENTER, -2f))

        renderSections(
            document, data, normalFont, sectionFont, boldFont, titleFont, normalFont,
            bullet = "\\u25B8", justifyText = true, headerAlign = Element.ALIGN_CENTER,
            headerLineColors = execColor, sectionSpacing = 16f
        )
    }

    // 7. CREATIVE
    private fun buildCreative(document: Document, data: ResumeData) {
        val red = BaseColor(220, 53, 69)
        val nameFont = Font(Font.FontFamily.HELVETICA, 28f, Font.BOLD, red)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD, red)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)

        document.add(Paragraph(data.personalInfo.fullName, nameFont).apply { alignment = Element.ALIGN_RIGHT })
        document.add(Paragraph(data.personalInfo.professionalTitle, italicFont).apply { alignment = Element.ALIGN_RIGHT })
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        document.add(Paragraph(c.joinToString("   •   "), normalFont).apply { alignment = Element.ALIGN_RIGHT; spacingAfter = 16f })
        
        renderSections(
            document, data, normalFont, sectionFont, boldFont, italicFont, normalFont,
            bullet = "*", justifyText = false, headerAlign = Element.ALIGN_RIGHT,
            headerLineColors = BaseColor.LIGHT_GRAY, sectionSpacing = 16f
        )
    }

    // 8. TECH
    private fun buildTech(document: Document, data: ResumeData) {
        val teal = BaseColor(0, 150, 136)
        val nameFont = Font(Font.FontFamily.COURIER, 20f, Font.BOLD, teal)
        val normalFont = Font(Font.FontFamily.COURIER, 10f, Font.NORMAL, BaseColor.BLACK)
        val sectionFont = Font(Font.FontFamily.COURIER, 13f, Font.BOLD, teal)
        val boldFont = Font(Font.FontFamily.COURIER, 10f, Font.BOLD, BaseColor.BLACK)

        document.add(Paragraph("< " + data.personalInfo.fullName.uppercase() + " />", nameFont))
        document.add(Paragraph("// " + data.personalInfo.professionalTitle, Font(Font.FontFamily.COURIER, 12f, Font.NORMAL, BaseColor.DARK_GRAY)))
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        document.add(Paragraph("/* " + c.joinToString(" | ") + " */", normalFont).apply { spacingAfter = 12f })
        
        renderSections(
            document, data, normalFont, sectionFont, boldFont, normalFont, normalFont,
            bullet = ">", justifyText = false, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = teal, sectionSpacing = 12f
        )
    }

    // 9. CLEAN
    private fun buildClean(document: Document, data: ResumeData) {
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor(50,50,50))
        val nameFont = Font(Font.FontFamily.HELVETICA, 22f, Font.NORMAL, BaseColor(50, 50, 50))
        val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor(50, 50, 50))
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)

        val cp = Paragraph(data.personalInfo.fullName.uppercase(), nameFont)
        val c = listOf(data.personalInfo.professionalTitle, data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location).filter { it.isNotBlank() }
        cp.add(Chunk("\\n" + c.joinToString("   \\u25E6   "), normalFont))
        cp.alignment = Element.ALIGN_CENTER
        cp.spacingAfter = 16f
        document.add(cp)

        renderSections(
            document, data, normalFont, sectionFont, boldFont, normalFont, normalFont,
            bullet = "\\u25E6", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = null, sectionSpacing = 16f
        )
    }

    // 10. CORPORATE
    private fun buildCorporate(document: Document, data: ResumeData) {
        val bg = BaseColor(44, 62, 80)
        val nameFont = Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, bg)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        val titleFont = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, BaseColor.GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)

        document.add(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        document.add(Paragraph(data.personalInfo.professionalTitle, titleFont))
        val c = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        document.add(Paragraph(c.joinToString(" | "), normalFont).apply { spacingAfter = 8f })

        // custom renderer because it has background header
        val headerRenderer = { title: String ->
            val table = PdfPTable(1).apply { widthPercentage = 100f; spacingBefore = 12f; spacingAfter = 8f }
            val cell = PdfPCell(Phrase(title.uppercase(), sectionFont)).apply { backgroundColor = bg; padding = 4f; border = PdfPCell.NO_BORDER }
            table.addCell(cell)
            document.add(table)
        }
        
        renderSections(
            document = document, data = data, normalFont = normalFont, sectionFont = sectionFont, 
            boldFont = boldFont, italicFont = titleFont, contactFont = normalFont,
            bullet = "\\u25A0", justifyText = true, headerAlign = Element.ALIGN_LEFT,
            headerLineColors = null, sectionSpacing = 0f, customHeaderRenderer = headerRenderer
        )
    }

    private fun renderSections(
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
        val alignment = if (justifyText) Element.ALIGN_JUSTIFIED else Element.ALIGN_LEFT

        fun addHeader(title: String) {
            if (customHeaderRenderer != null) {
                customHeaderRenderer(title)
                return
            }
            val p = Paragraph(title.uppercase(), sectionFont).apply { 
                this.alignment = headerAlign
                spacingBefore = sectionSpacing
                spacingAfter = 2f
            }
            document.add(p)
            if (headerLineColors != null) {
                document.add(LineSeparator(1f, 100f, headerLineColors, Element.ALIGN_CENTER, -2f))
            } else {
                document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 2f)))
            }
        }

        // Summary
        val summary = getSummaryText(data)
        if (summary.isNotBlank()) {
            addHeader("Professional Summary")
            document.add(Paragraph(summary, normalFont).apply { this.alignment = alignment })
        }

        // Work
        val validWork = data.workExperiences.filter { it.company.isNotBlank() || it.jobTitle.isNotBlank() }
        if (validWork.isNotEmpty()) {
            addHeader("Experience")
            validWork.forEach { we ->
                val wp = Paragraph().apply { spacingBefore = 4f }
                wp.add(Chunk(we.jobTitle, boldFont))
                if (we.company.isNotBlank()) wp.add(Chunk(" | \${we.company}", italicFont))
                val dateStr = listOf(we.startDate, we.endDate, we.duration).filter { it.isNotBlank() }.joinToString(" - ")
                if (dateStr.isNotBlank()) {
                    if (headerAlign == Element.ALIGN_RIGHT) wp.add(Chunk("\\n" + dateStr, contactFont))
                    else wp.add(Chunk("  (" + dateStr + ")", contactFont))
                }
                wp.alignment = if (headerAlign == Element.ALIGN_RIGHT) Element.ALIGN_RIGHT else Element.ALIGN_LEFT
                document.add(wp)

                val resps = we.responsibilities.filter { it.isNotBlank() }
                if (resps.isNotEmpty()) {
                    val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { 
                        setListSymbol("\$bullet ")
                        symbolIndent = 10f
                    }
                    resps.forEach {
                        list.add(ListItem(Paragraph(it, normalFont).apply { this.alignment = alignment }))
                    }
                    document.add(list)
                }
            }
        }

        // Education
        val validEdu = data.educations.filter { it.qualification.isNotBlank() || it.institute.isNotBlank() }
        if (validEdu.isNotEmpty()) {
            addHeader("Education")
            validEdu.sortedByDescending { it.yearOfCompletion.toIntOrNull() ?: 0 }.forEach { ed ->
                val ep = Paragraph().apply { spacingBefore = 4f; this.alignment = if (headerAlign == Element.ALIGN_RIGHT) Element.ALIGN_RIGHT else alignment }
                ep.add(Chunk(ed.qualification, boldFont))
                if (ed.institute.isNotBlank()) ep.add(Chunk(" | \${ed.institute}", normalFont))
                val details = listOf(ed.yearOfCompletion, ed.percentage, ed.board).filter { it.isNotBlank() }.joinToString(", ")
                if (details.isNotEmpty()) ep.add(Chunk("\\n" + details, contactFont))
                document.add(ep)
            }
        }

        // Skills
        val validSk = data.skills.filter { it.name.isNotBlank() }
        if (validSk.isNotEmpty()) {
            addHeader("Skills")
            val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { setListSymbol("\$bullet "); symbolIndent = 10f }
            validSk.forEach { sk ->
                val lvl = if (sk.level.isNotBlank() && sk.level != "Foundational") " (\${sk.level})" else ""
                list.add(ListItem(Paragraph("\${sk.name}\$lvl", normalFont).apply { this.alignment = if(headerAlign == Element.ALIGN_RIGHT) Element.ALIGN_RIGHT else Element.ALIGN_LEFT }))
            }
            document.add(list)
        }

        // Additional
        val infoList = mutableListOf<String>()
        if (data.additionalInfo.dob.isNotBlank()) infoList.add("DOB: \${data.additionalInfo.dob}")
        if (data.additionalInfo.gender.isNotBlank()) infoList.add("Gender: \${data.additionalInfo.gender}")
        if (data.additionalInfo.maritalStatus.isNotBlank()) infoList.add("Marital Status: \${data.additionalInfo.maritalStatus}")
        if (data.additionalInfo.hobby.isNotBlank()) infoList.add("Hobbies: \${data.additionalInfo.hobby}")
        val validLang = data.additionalInfo.languages.filter { it.name.isNotBlank() }
        if (validLang.isNotEmpty()) infoList.add("Languages: " + validLang.joinToString(", ") { "\${it.name}(\${it.level})" })
        
        if (infoList.isNotEmpty()) {
            addHeader("Additional Info")
            val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { setListSymbol("\$bullet "); symbolIndent = 10f }
            infoList.forEach { info ->
                list.add(ListItem(Paragraph(info, normalFont).apply { this.alignment = if(headerAlign == Element.ALIGN_RIGHT) Element.ALIGN_RIGHT else alignment }))
            }
            document.add(list)
        }

        // Declaration
        if (data.declaration.role.isNotBlank()) {
            addHeader("Declaration")
            document.add(Paragraph(getDeclarationText(data.declaration.role), normalFont).apply { this.alignment = alignment; spacingBefore = 4f })
        }
    }
}
`;

fs.writeFileSync('app/src/main/java/com/example/util/ResumeExportUtils.kt', content);
console.log("Resume templates rewritten successfully!");
