package `in`.campusone.app.data.local

import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.UserRole

/**
 * Built-in offline seed data for the HNB Garhwal University (Chauras / Birla Campus, Srinagar Garhwal)
 * pilot so the app works immediately on first launch even with zero internet connectivity.
 */
object HnbguSeedData {
    const val DEFAULT_SECTION_KEY = "hnbgu_bsc_cs_sem3_secA"

    fun defaultSubjects(sectionKey: String = DEFAULT_SECTION_KEY): List<SubjectEntity> = listOf(
        SubjectEntity(
            subjectId = "CS301",
            sectionKey = sectionKey,
            subjectName = "Data Structures & Algorithms",
            subjectCode = "SOET-CS-301",
            teacherName = "Dr. M. M. S. Rauthan",
            isElective = false,
            credits = 4
        ),
        SubjectEntity(
            subjectId = "CS302",
            sectionKey = sectionKey,
            subjectName = "Operating Systems",
            subjectCode = "SOET-CS-302",
            teacherName = "Dr. Varun Barthwal",
            isElective = false,
            credits = 4
        ),
        SubjectEntity(
            subjectId = "CS303",
            sectionKey = sectionKey,
            subjectName = "Database Management Systems",
            subjectCode = "SOET-CS-303",
            teacherName = "Dr. Arti Rana",
            isElective = false,
            credits = 4
        ),
        SubjectEntity(
            subjectId = "MATH301",
            sectionKey = sectionKey,
            subjectName = "Discrete Mathematics (GE)",
            subjectCode = "SOS-MATH-301",
            teacherName = "Prof. R. C. Dimri",
            isElective = true,
            credits = 4
        ),
        SubjectEntity(
            subjectId = "SEC301",
            sectionKey = sectionKey,
            subjectName = "Android App Development (SEC)",
            subjectCode = "SOET-SEC-301",
            teacherName = "Er. Pradeep Semwal",
            isElective = true,
            credits = 2
        )
    )

    fun defaultTimetable(sectionKey: String = DEFAULT_SECTION_KEY): List<TimetableSlotEntity> {
        val slots = mutableListOf<TimetableSlotEntity>()
        // Monday (1) to Saturday (6) schedule
        for (day in 1..6) {
            slots += TimetableSlotEntity(
                id = "${sectionKey}_d${day}_0900",
                sectionKey = sectionKey,
                subjectId = "CS301",
                subjectName = "Data Structures & Algorithms",
                subjectCode = "SOET-CS-301",
                teacherName = "Dr. M. M. S. Rauthan",
                roomNumber = "CS Block - Room 104 (Chauras)",
                dayOfWeek = day,
                startTime = "09:00",
                endTime = "10:00"
            )
            slots += TimetableSlotEntity(
                id = "${sectionKey}_d${day}_1005",
                sectionKey = sectionKey,
                subjectId = "CS302",
                subjectName = "Operating Systems",
                subjectCode = "SOET-CS-302",
                teacherName = "Dr. Varun Barthwal",
                roomNumber = "CS Block - Room 104 (Chauras)",
                dayOfWeek = day,
                startTime = "10:05",
                endTime = "11:05"
            )
            slots += TimetableSlotEntity(
                id = "${sectionKey}_d${day}_1115",
                sectionKey = sectionKey,
                subjectId = "CS303",
                subjectName = "Database Management Systems",
                subjectCode = "SOET-CS-303",
                teacherName = "Dr. Arti Rana",
                roomNumber = "CS Lab 2 (Chauras)",
                dayOfWeek = day,
                startTime = "11:15",
                endTime = "12:15"
            )
            if (day in listOf(1, 3, 5)) {
                slots += TimetableSlotEntity(
                    id = "${sectionKey}_d${day}_1400",
                    sectionKey = sectionKey,
                    subjectId = "MATH301",
                    subjectName = "Discrete Mathematics (GE)",
                    subjectCode = "SOS-MATH-301",
                    teacherName = "Prof. R. C. Dimri",
                    roomNumber = "Science Block - LH 2",
                    dayOfWeek = day,
                    startTime = "14:00",
                    endTime = "15:00",
                    isElective = true
                )
            } else {
                slots += TimetableSlotEntity(
                    id = "${sectionKey}_d${day}_1400",
                    sectionKey = sectionKey,
                    subjectId = "SEC301",
                    subjectName = "Android App Development (SEC)",
                    subjectCode = "SOET-SEC-301",
                    teacherName = "Er. Pradeep Semwal",
                    roomNumber = "Computing Center Lab 1",
                    dayOfWeek = day,
                    startTime = "14:00",
                    endTime = "15:30",
                    isElective = true
                )
            }
        }
        return slots
    }

    fun defaultNotices(sectionKey: String = DEFAULT_SECTION_KEY): List<NoticeEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            NoticeEntity(
                id = "notice_hnbgu_sessional_1",
                sectionKey = sectionKey,
                title = "First Sessional Examination Schedule (Odd Semester)",
                body = "All B.Sc. CS & IT Sem III students are informed that First Internal Sessional Examinations will commence from 12th November. Minimum 75% attendance is mandatory as per HNBGU NEP ordinances.",
                attachmentUrl = "https://hnbgu.ac.in/notices/sessional_schedule_odd_sem.pdf",
                attachmentType = AttachmentType.PDF,
                isPinned = true,
                authorUid = "faculty_hod_cs",
                authorName = "HoD, Dept. of Computer Science",
                authorRole = UserRole.TEACHER,
                createdAtMillis = now - 3_600_000L * 5,
                isRead = false
            ),
            NoticeEntity(
                id = "notice_cr_lab_shift",
                sectionKey = sectionKey,
                title = "DBMS Lab shifted to Computing Center Lab 2 today",
                body = "Dr. Arti Rana ma'am informed that today's 11:15 AM DBMS practical will take place in Computing Center Lab 2 instead of Room 104. Bring your lab record notebooks.",
                attachmentUrl = null,
                attachmentType = AttachmentType.NONE,
                isPinned = false,
                authorUid = "cr_priyanshu",
                authorName = "Priyanshu Negi (CR - Sec A)",
                authorRole = UserRole.CR,
                createdAtMillis = now - 3_600_000L * 2,
                isRead = false
            )
        )
    }
}
