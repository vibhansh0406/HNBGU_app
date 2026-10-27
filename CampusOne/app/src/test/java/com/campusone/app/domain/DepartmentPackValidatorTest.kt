package com.campusone.app.domain

import com.campusone.app.domain.model.DepartmentPackPayload
import com.campusone.app.domain.model.DepartmentPackSubject
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.usecase.DepartmentPackValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepartmentPackValidatorTest {

    @Test
    fun validDepartmentPack_passesValidation() {
        val payload = DepartmentPackPayload(
            version = 1,
            departmentName = "CSE",
            course = "B.Tech CSE",
            semester = 4,
            section = "A",
            subjects = listOf(
                DepartmentPackSubject("sub_1", "CSE-401", "Algorithms", "Prof A", 4, 75, "#0F4C81")
            ),
            timetableSlots = listOf(
                TimetableSlot("slot_1", "sub_1", "Algorithms", "CSE-401", 1, "09:30", "10:30", "102", "Prof A", 4, "A")
            )
        )
        val res = DepartmentPackValidator.validate(payload)
        assertTrue(res.isValid)
    }

    @Test
    fun unsupportedVersionOrInvalidSlotTime_failsWithFriendlyError() {
        val badVersion = DepartmentPackPayload(version = 99, departmentName = "CSE", course = "B.Tech")
        val res1 = DepartmentPackValidator.validate(badVersion)
        assertFalse(res1.isValid)
        assertTrue(res1.errorMessage!!.contains("version"))

        val badTime = DepartmentPackPayload(
            version = 1,
            departmentName = "CSE",
            course = "B.Tech",
            semester = 4,
            timetableSlots = listOf(
                TimetableSlot("slot_1", "sub_1", "Algorithms", "CSE-401", 1, "11:30", "10:30", "102", "Prof A", 4, "A")
            )
        )
        val res2 = DepartmentPackValidator.validate(badTime)
        assertFalse(res2.isValid)
        assertTrue(res2.errorMessage!!.contains("time range"))
    }
}
