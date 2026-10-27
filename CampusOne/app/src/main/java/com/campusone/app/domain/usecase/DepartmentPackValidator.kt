package com.campusone.app.domain.usecase

import com.campusone.app.domain.model.DepartmentPackPayload

data class DepartmentPackValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val summaryLabel: String = ""
)

object DepartmentPackValidator {
    const val CURRENT_SCHEMA_VERSION = 1
    private val SUPPORTED_VERSIONS = setOf(1, 2)

    fun validate(payload: DepartmentPackPayload): DepartmentPackValidationResult {
        if (payload.version !in SUPPORTED_VERSIONS) {
            return DepartmentPackValidationResult(
                isValid = false,
                errorMessage = "Unsupported department pack version (${payload.version}). Expected version 1 or 2."
            )
        }

        if (payload.course.isBlank() || payload.departmentName.isBlank()) {
            return DepartmentPackValidationResult(
                isValid = false,
                errorMessage = "Department pack is missing required departmentName or course field."
            )
        }

        if (payload.semester !in 1..10) {
            return DepartmentPackValidationResult(
                isValid = false,
                errorMessage = "Invalid semester (${payload.semester}) in department pack. Must be between 1 and 10."
            )
        }

        if (payload.subjects.isEmpty() && payload.timetableSlots.isEmpty() &&
            payload.notices.isEmpty() && payload.syllabusTopics.isEmpty()
        ) {
            return DepartmentPackValidationResult(
                isValid = false,
                errorMessage = "Department pack is empty (contains zero subjects, slots, notices, or syllabus units)."
            )
        }

        for (subject in payload.subjects) {
            if (subject.name.isBlank() || subject.code.isBlank()) {
                return DepartmentPackValidationResult(
                    isValid = false,
                    errorMessage = "A subject in the department pack has a blank code or name."
                )
            }
            if (subject.targetPercentage !in 1..99) {
                return DepartmentPackValidationResult(
                    isValid = false,
                    errorMessage = "Subject '${subject.code}' has invalid target percentage (${subject.targetPercentage}%)."
                )
            }
        }

        for (slot in payload.timetableSlots) {
            if (slot.dayOfWeek !in 1..7) {
                return DepartmentPackValidationResult(
                    isValid = false,
                    errorMessage = "Timetable slot '${slot.subjectCode}' has invalid dayOfWeek (${slot.dayOfWeek})."
                )
            }
            if (!TimetableEngine.isValidSlotRange(slot.startTime, slot.endTime)) {
                return DepartmentPackValidationResult(
                    isValid = false,
                    errorMessage = "Timetable slot '${slot.subjectCode}' has invalid time range (${slot.startTime} - ${slot.endTime})."
                )
            }
        }

        val summary = "${payload.course} Sem ${payload.semester}: " +
            "${payload.subjects.size} subjects, ${payload.timetableSlots.size} slots, " +
            "${payload.notices.size} notices, ${payload.syllabusTopics.size} syllabus topics"
        return DepartmentPackValidationResult(
            isValid = true,
            errorMessage = null,
            summaryLabel = summary
        )
    }
}
