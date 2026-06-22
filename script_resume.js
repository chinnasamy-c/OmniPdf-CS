const fs = require('fs');

let code = fs.readFileSync('app/src/main/java/com/example/util/ResumeExportUtils.kt', 'utf8');

// Replace the when block
const whenBlockReg = /when \(data\.selectedTemplate\) \{[\s\S]*?\}/;
const newWhenBlock = `when (data.selectedTemplate) {
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
            }`;

code = code.replace(whenBlockReg, newWhenBlock);

const newMethods = `
    private fun buildExecutive(document: Document, data: ResumeData) {
        val topColor = BaseColor(0, 34, 68)
        val nameFont = Font(Font.FontFamily.TIMES_ROMAN, 24f, Font.BOLD, topColor)
        val titleFont = Font(Font.FontFamily.TIMES_ROMAN, 14f, Font.ITALIC, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.TIMES_ROMAN, 14f, Font.BOLD, topColor)
        val normalFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.NORMAL, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.TIMES_ROMAN, 11f, Font.ITALIC, BaseColor.DARK_GRAY)
        val contactFont = Font(Font.FontFamily.TIMES_ROMAN, 10f, Font.NORMAL, BaseColor(54, 69, 79))

        document.add(Paragraph(data.personalInfo.fullName.uppercase(), nameFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 4f })
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont).apply { alignment = Element.ALIGN_CENTER })
        
        val contactItems = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contactItems.isNotEmpty()) {
            document.add(Paragraph(contactItems.joinToString("   |   "), contactFont).apply { alignment = Element.ALIGN_CENTER; spacingBefore = 4f; spacingAfter = 10f })
            document.add(LineSeparator(2f, 100f, topColor, Element.ALIGN_CENTER, -2f))
        }

        addSections(document, data, normalFont, sectionFont, boldFont, italicFont, contactFont, centerHeaders = true, lineColor = topColor, uppercaseHeaders = true, customSpacing = 12f)
    }

    private fun buildCreative(document: Document, data: ResumeData) {
        val mainColor = BaseColor(220, 53, 69) // Accent red
        val nameFont = Font(Font.FontFamily.HELVETICA, 26f, Font.BOLD, mainColor)
        val titleFont = Font(Font.FontFamily.HELVETICA, 13f, Font.NORMAL, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, mainColor)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val contactFont = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL, BaseColor(100, 100, 100))

        document.add(Paragraph(data.personalInfo.fullName, nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont))
        
        val contactItems = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contactItems.isNotEmpty()) {
            document.add(Paragraph(contactItems.joinToString(" • "), contactFont).apply { spacingBefore = 6f; spacingAfter = 14f })
        }

        addSections(document, data, normalFont, sectionFont, boldFont, italicFont, contactFont, centerHeaders = false, lineColor = mainColor, uppercaseHeaders = true, customSpacing = 8f)
    }

    private fun buildTech(document: Document, data: ResumeData) {
        val techColor = BaseColor(0, 150, 136) // Teal
        val nameFont = Font(Font.FontFamily.COURIER, 20f, Font.BOLD, techColor)
        val titleFont = Font(Font.FontFamily.COURIER, 12f, Font.NORMAL, BaseColor.DARK_GRAY)
        val sectionFont = Font(Font.FontFamily.COURIER, 13f, Font.BOLD, techColor)
        val normalFont = Font(Font.FontFamily.COURIER, 10f, Font.NORMAL, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.COURIER, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.COURIER, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val contactFont = Font(Font.FontFamily.COURIER, 9f, Font.NORMAL, BaseColor.GRAY)

        document.add(Paragraph("< " + data.personalInfo.fullName.uppercase() + " />", nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph("// " + data.personalInfo.professionalTitle, titleFont))
        
        val contactItems = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contactItems.isNotEmpty()) {
            document.add(Paragraph("/* " + contactItems.joinToString(" | ") + " */", contactFont).apply { spacingBefore = 6f; spacingAfter = 12f })
        }

        addSections(document, data, normalFont, sectionFont, boldFont, italicFont, contactFont, centerHeaders = false, lineColor = techColor, uppercaseHeaders = true, customSpacing = 8f)
    }

    private fun buildClean(document: Document, data: ResumeData) {
        val nameFont = Font(Font.FontFamily.HELVETICA, 20f, Font.NORMAL, BaseColor(50, 50, 50))
        val titleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor(150, 150, 150))
        val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor(50, 50, 50))
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val contactFont = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL, BaseColor(120, 120, 120))

        document.add(Paragraph(data.personalInfo.fullName, nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont))
        
        val contactItems = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contactItems.isNotEmpty()) document.add(Paragraph(contactItems.joinToString("   "), contactFont).apply { spacingBefore = 4f; spacingAfter = 12f })

        addSections(document, data, normalFont, sectionFont, boldFont, italicFont, contactFont, centerHeaders = false, lineColor = null, uppercaseHeaders = true, customSpacing = 16f)
    }

    private fun buildCorporate(document: Document, data: ResumeData) {
        val corpColor = BaseColor(44, 62, 80)
        val nameFont = Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD, corpColor)
        val titleFont = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, BaseColor(127, 140, 141))
        val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        val boldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.BLACK)
        val italicFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val contactFont = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL, BaseColor.DARK_GRAY)

        document.add(Paragraph(data.personalInfo.fullName.uppercase(), nameFont))
        if (data.personalInfo.professionalTitle.isNotBlank()) document.add(Paragraph(data.personalInfo.professionalTitle, titleFont))
        
        val contactItems = listOf(data.personalInfo.email, data.personalInfo.phone, data.personalInfo.location, data.personalInfo.socialMedia).filter { it.isNotBlank() }
        if (contactItems.isNotEmpty()) {
            document.add(Paragraph(contactItems.joinToString(" | "), contactFont).apply { spacingBefore = 4f; spacingAfter = 10f })
            document.add(LineSeparator(1f, 100f, corpColor, Element.ALIGN_CENTER, -2f))
        }

        // We use a custom section drawing for corporate which adds a colored background row for headers
        fun addCorpSec(title: String) {
            val table = PdfPTable(1)
            table.widthPercentage = 100f
            val cell = PdfPCell(Phrase(title.uppercase(), sectionFont))
            cell.backgroundColor = corpColor
            cell.horizontalAlignment = Element.ALIGN_LEFT
            cell.paddingBottom = 4f
            cell.paddingLeft = 4f
            cell.border = PdfPCell.NO_BORDER
            table.addCell(cell)
            table.spacingBefore = 8f
            table.spacingAfter = 4f
            document.add(table)
        }

        // We inline a mini addSections so we can override the header
        addSectionsWithHeader(document, data, normalFont, boldFont, italicFont, contactFont) { addCorpSec(it) }
    }

    private fun addSectionsWithHeader(document: Document, data: ResumeData, normalFont: Font, boldFont: Font, italicFont: Font, contactFont: Font, headerFunc: (String) -> Unit) {
        var summaryText = ""
        if (data.summaryInfo.type == SummaryType.MANUAL) {
            summaryText = data.summaryInfo.manualText
        } else {
            val jt = data.summaryInfo.autoJobTitle.ifBlank { "Professional" }
            val pat = when(data.summaryInfo.autoTone) {
                AutoSummaryTone.ENTRY_LEVEL -> ENTRY_LEVEL_PATTERN
                AutoSummaryTone.BALANCED -> BALANCED_PATTERN
                AutoSummaryTone.ACTION -> ACTION_PATTERN
            }
            summaryText = pat.replace("[%s]", jt)
        }
        if (summaryText.isNotBlank()) {
            headerFunc("Professional Summary")
            document.add(Paragraph(summaryText, normalFont).apply { spacingBefore = 2f })
        }
        
        if (data.workExperiences.isNotEmpty() && data.workExperiences.any { it.jobTitle.isNotBlank() || it.company.isNotBlank() }) {
            headerFunc("Work Experience")
            data.workExperiences.filter { it.jobTitle.isNotBlank() || it.company.isNotBlank() }.forEach { we ->
                val p = Paragraph().apply { spacingBefore = 2f }
                p.add(Chunk(we.jobTitle, boldFont))
                if (we.company.isNotBlank()) p.add(Chunk(" - \${we.company}", italicFont))
                val dates = listOf(we.startDate, we.endDate, we.duration).filter { it.isNotBlank() }.joinToString(" - ")
                if (dates.isNotBlank()) p.add(Chunk(" | " + dates, contactFont))
                document.add(p)
                if (we.responsibilities.any { it.isNotBlank() }) {
                    val list = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { setListSymbol("\\u2022 "); symbolIndent = 8f }
                    we.responsibilities.filter { it.isNotBlank() }.forEach { list.add(ListItem(it, normalFont)) }
                    document.add(list)
                }
            }
        }
        
        if (data.educations.isNotEmpty() && data.educations.any { it.qualification.isNotBlank() || it.institute.isNotBlank() }) {
            headerFunc("Education")
            val eduList = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { setListSymbol("\\u2022 "); symbolIndent = 8f }
            data.educations.filter { it.qualification.isNotBlank() || it.institute.isNotBlank() }.sortedByDescending { it.yearOfCompletion.toIntOrNull() ?: 0 }.forEach { ed ->
                val p = Paragraph().apply { spacingBefore = 2f }
                p.add(Chunk(ed.qualification, boldFont))
                if (ed.institute.isNotBlank()) p.add(Chunk(" at \${ed.institute}", normalFont))
                if (ed.board.isNotBlank()) p.add(Chunk(", \${ed.board}", italicFont))
                val details = listOf(if(ed.yearOfCompletion.isNotBlank()) "Year: \${ed.yearOfCompletion}" else "", if (ed.percentage.isNotBlank()) "Score: \${ed.percentage}" else "").filter { it.isNotBlank() }
                if (details.isNotEmpty()) p.add(Chunk("\\n      " + details.joinToString(" | "), contactFont))
                eduList.add(ListItem(p).apply { spacingAfter = 2f })
            }
            document.add(eduList)
        }
        
        if (data.skills.isNotEmpty() && data.skills.any { it.name.isNotBlank() }) {
            headerFunc("Skills & Certifications")
            val skillList = com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED).apply { setListSymbol("\\u2022 "); symbolIndent = 8f }
            data.skills.filter { it.name.isNotBlank() }.forEach { sk ->
                val lv = if (sk.level.isNotBlank() && sk.level != "Foundational") " (\${sk.level})" else ""
                skillList.add(ListItem("\${sk.name}\$lv", normalFont))
            }
            document.add(skillList)
        }
        
        val aiItems = listOf(
            if (data.additionalInfo.dob.isNotBlank()) "DOB: \${data.additionalInfo.dob}" else "",
            if (data.additionalInfo.gender.isNotBlank()) "Gender: \${data.additionalInfo.gender}" else "",
            if (data.additionalInfo.maritalStatus.isNotBlank()) "Marital Status: \${data.additionalInfo.maritalStatus}" else "",
            if (data.additionalInfo.hobby.isNotBlank()) "Hobbies: \${data.additionalInfo.hobby}" else ""
        ).filter { it.isNotBlank() }.toMutableList()
        val validLangs = data.additionalInfo.languages.filter { it.name.isNotBlank() }
        if (validLangs.isNotEmpty()) aiItems.add("Languages: " + validLangs.joinToString(", ") { "\${it.name} (\${it.level})" })
        
        if (aiItems.isNotEmpty()) {
            headerFunc("Additional Information")
            aiItems.forEach { info -> document.add(Paragraph(info, normalFont).apply { spacingBefore = 2f }) }
        }
        
        if (data.declaration.role.isNotBlank()) {
            document.add(Paragraph(" ", normalFont))
            headerFunc("Declaration")
            val decText = "I hereby declare that the above information is true and correct to the best of my knowledge and belief. I take full responsibility for its accuracy and affirm my commitment to the \${data.declaration.role} profession with sincerity and integrity."
            document.add(Paragraph(decText, normalFont).apply { spacingBefore = 4f })
        }
    }

    private fun addSections(
`;

code = code.replace("    private fun addSections(", newMethods);

fs.writeFileSync('app/src/main/java/com/example/util/ResumeExportUtils.kt', code);
console.log('Fixed Resume templates');
