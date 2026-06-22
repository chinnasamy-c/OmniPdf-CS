package com.example.ui

import java.util.UUID

data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val professionalTitle: String = "",
    val socialMedia: String = "",
    val photoUri: String = ""
)

enum class SummaryType { MANUAL, AUTO }
enum class AutoSummaryTone { ENTRY_LEVEL, BALANCED, ACTION }

data class SummaryInfo(
    val type: SummaryType = SummaryType.MANUAL,
    val manualText: String = "",
    val autoJobTitle: String = "",
    val autoTone: AutoSummaryTone = AutoSummaryTone.ENTRY_LEVEL
)

data class WorkExperience(
    val id: String = UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val duration: String = "",
    val responsibilities: List<String> = listOf("")
)

data class Education(
    val id: String = UUID.randomUUID().toString(),
    val qualification: String = "",
    val institute: String = "",
    val board: String = "",
    val yearOfCompletion: String = "",
    val percentage: String = ""
)

data class Skill(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val level: String = "Foundational"
)

data class Language(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val level: String = "Beginner"
)

data class AdditionalInfo(
    val languages: List<Language> = emptyList(),
    val dob: String = "",
    val gender: String = "",
    val maritalStatus: String = "",
    val hobby: String = ""
)

data class DeclarationInfo(
    val role: String = ""
)

enum class ResumeTemplate { CLASSIC, MODERN, ELEGANT, MINIMALIST, PROFESSIONAL, EXECUTIVE, CREATIVE, TECH, CLEAN, CORPORATE }

data class ResumeData(
    val selectedTemplate: ResumeTemplate = ResumeTemplate.CLASSIC,
    val personalInfo: PersonalInfo = PersonalInfo(),
    val summaryInfo: SummaryInfo = SummaryInfo(),
    val workExperiences: List<WorkExperience> = listOf(WorkExperience()),
    val educations: List<Education> = listOf(Education()),
    val skills: List<Skill> = listOf(Skill()),
    val additionalInfo: AdditionalInfo = AdditionalInfo(),
    val declaration: DeclarationInfo = DeclarationInfo()
)

val ENTRY_LEVEL_PATTERN = "Highly motivated and reliable [%s] with a strong foundation in teamwork, communication, and task organization. Fast learner with a proven capability to master new concepts, technical procedures, and operational systems quickly. Eager to bring a positive attitude, strong focus, and dedicated work ethic to a dynamic team environment."
val BALANCED_PATTERN = "Dependable and detail-oriented [%s] with a strong work ethic and a proven track record of delivering high-quality results. Exceptional problem-solving skills combined with the ability to adapt quickly to new environments, tools, and workflows. A collaborative team player dedicated to supporting organizational goals and ensuring daily operations run smoothly."
val ACTION_PATTERN = "Results-driven [%s] skilled in managing daily tasks efficiently while maintaining high safety and quality standards. Strong analytical and troubleshooting abilities with a focus on practical problem-solving. Recognized for reliability, strong time-management skills, and a proactive approach to handling workplace challenges both independently and in team settings."
