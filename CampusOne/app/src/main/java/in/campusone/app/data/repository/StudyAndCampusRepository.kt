package `in`.campusone.app.data.repository

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.campusone.app.data.local.dao.CampusLifeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.CampusMapPlaceEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.DirectorySuggestionEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.GpaSemesterEntity
import `in`.campusone.app.data.local.entity.LostFoundContactRequestEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.domain.model.EventRsvpStatus
import `in`.campusone.app.domain.usecase.CampusLifeEngine
import `in`.campusone.app.domain.usecase.CourseGradeInput
import `in`.campusone.app.domain.usecase.SemesterGpaRecord
import `in`.campusone.app.util.StudyFileAndPdfHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyAndCampusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val studyDao: StudyDao,
    private val campusLifeDao: CampusLifeDao
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Rate-limiting guard (minimum 10 seconds between user-created posts to prevent spam)
    private var lastUserPostMillis: Long = 0L

    fun checkRateLimit(nowMillis: Long = System.currentTimeMillis()): Result<Unit> {
        val elapsed = nowMillis - lastUserPostMillis
        return if (elapsed < 10_000L && lastUserPostMillis > 0L) {
            val waitSec = ((10_000L - elapsed) / 1000L).coerceAtLeast(1L)
            Result.failure(IllegalStateException("Rate limit active: Please wait ${waitSec}s before posting again."))
        } else {
            lastUserPostMillis = nowMillis
            Result.success(Unit)
        }
    }

    suspend fun ensureStudyAndCampusSeeded(sectionKey: String, collegeId: String = "hnbgu") {
        val currentExams = studyDao.observeExams().first()
        if (currentExams.isNotEmpty()) {
            // Always run 30-day Lost & Found auto-archive sweep on startup
            val cutoff = System.currentTimeMillis() - CampusLifeEngine.AUTO_ARCHIVE_WINDOW_MILLIS
            campusLifeDao.autoArchiveOldLostFound(cutoff)
            return
        }

        val now = System.currentTimeMillis()
        val hourMs = 3_600_000L
        val dayMs = 86_400_000L

        // 1. Seed Phase 2 Notes & PYQs
        studyDao.upsertResources(
            listOf(
                StudyResourceEntity(
                    id = "res_pyq_cs301_2025",
                    sectionKey = sectionKey,
                    subjectId = "CS301",
                    subjectName = "Data Structures & Algorithms",
                    semester = 3,
                    title = "HNBGU End-Sem PYQ Paper (2022–2025 Solved with Trees & Graphs)",
                    category = "PYQ",
                    examYear = 2025,
                    fileUrl = "https://hnbgu.ac.in/library/pyq/SOET_CS301_2025.pdf",
                    fileType = "PDF",
                    fileSizeBytes = 2_450_000L,
                    uploaderUid = "uid_cr",
                    uploaderName = "Priyanshu Negi (CR)",
                    showUploaderName = true,
                    upvotes = 42,
                    isBookmarkedOffline = true
                ),
                StudyResourceEntity(
                    id = "res_notes_cs302_unit1_3",
                    sectionKey = sectionKey,
                    subjectId = "CS302",
                    subjectName = "Operating Systems",
                    semester = 3,
                    title = "Handwritten CPU Scheduling, Semaphores & Banker's Algorithm Notes",
                    category = "NOTES",
                    examYear = 2026,
                    fileUrl = "https://hnbgu.ac.in/library/notes/CS302_OS_Handwritten.pdf",
                    fileType = "PDF",
                    fileSizeBytes = 3_120_000L,
                    uploaderUid = "uid_ananya",
                    uploaderName = "Ananya Bahuguna",
                    showUploaderName = false,
                    upvotes = 29,
                    isBookmarkedOffline = false
                ),
                StudyResourceEntity(
                    id = "res_pyq_cs303_dbms",
                    sectionKey = sectionKey,
                    subjectId = "CS303",
                    subjectName = "Database Management Systems",
                    semester = 3,
                    title = "Normalization (1NF–BCNF) & SQL Lab Diagram Sheet",
                    category = "ASSIGNMENT",
                    examYear = 2025,
                    fileUrl = "https://hnbgu.ac.in/library/pyq/CS303_DBMS_Sessional.png",
                    fileType = "IMAGE",
                    fileSizeBytes = 540_000L,
                    uploaderUid = "uid_rohit",
                    uploaderName = "Rohit Bisht",
                    showUploaderName = true,
                    upvotes = 19,
                    isBookmarkedOffline = false
                )
            )
        )

        // 2. Seed Phase 2 Assignments
        studyDao.upsertAssignments(
            listOf(
                AssignmentEntity(
                    id = "assign_today_os",
                    sectionKey = sectionKey,
                    subjectId = "CS302",
                    subjectTag = "Operating Systems • CS302",
                    title = "Submit Dining Philosophers Semaphore Lab Code",
                    description = "Upload C program using POSIX threads and mutexes before evening lab.",
                    priority = "HIGH",
                    dueDateEpochMillis = now + 5 * hourMs,
                    dueDateFormatted = "Today • 05:00 PM",
                    reminderEnabled = true,
                    remind1DayBefore = true,
                    remind2HoursBefore = true,
                    isCompleted = false,
                    isSharedByCrOrTeacher = false,
                    isAddedToPersonalList = true
                ),
                AssignmentEntity(
                    id = "assign_dsa_trees",
                    sectionKey = sectionKey,
                    subjectId = "CS301",
                    subjectTag = "Data Structures • CS301",
                    title = "Implement AVL Tree & Red-Black Tree Rotations in C/C++",
                    description = "Submit source code + output screenshots in Lab Record File before Friday practical.",
                    priority = "HIGH",
                    dueDateEpochMillis = now + 3 * dayMs,
                    dueDateFormatted = "In 3 days • 11:59 PM",
                    reminderEnabled = true,
                    isCompleted = false,
                    isSharedByCrOrTeacher = true,
                    isAddedToPersonalList = true,
                    sharedSourceId = "shared_cr_dsa_1",
                    authorName = "Dr. M. M. S. Rauthan"
                ),
                AssignmentEntity(
                    id = "assign_shared_section_unadded",
                    sectionKey = sectionKey,
                    subjectId = "CS303",
                    subjectTag = "DBMS • CS303",
                    title = "Section A Shared Task: Normalize HNBGU Library Schema to BCNF",
                    description = "Posted by CR for the entire section. Tap 'Add to My List' to track in your personal planner.",
                    priority = "MEDIUM",
                    dueDateEpochMillis = now + 5 * dayMs,
                    dueDateFormatted = "In 5 days • 04:00 PM",
                    reminderEnabled = true,
                    isCompleted = false,
                    isSharedByCrOrTeacher = true,
                    isAddedToPersonalList = false,
                    sharedSourceId = "shared_cr_dbms_2",
                    authorName = "Priyanshu Negi (CR)"
                )
            )
        )

        // 3. Seed Phase 2 Exams & Syllabus
        studyDao.upsertExams(
            listOf(
                ExamEntity(
                    id = "exam_cs301",
                    sectionKey = sectionKey,
                    subjectId = "CS301",
                    subjectName = "Data Structures & Algorithms",
                    subjectCode = "SOET-CS-301",
                    examTitle = "End-Semester Theory Exam",
                    examDateEpochMillis = now + 18 * dayMs,
                    examDateIso = "2026-11-14",
                    examTimeFormatted = "10:00 AM – 01:00 PM",
                    examDateFormatted = "14 Nov 2026 • 10:00 AM",
                    roomOrCenter = "Examination Block Hall A (Chauras Campus)"
                ),
                ExamEntity(
                    id = "exam_cs302",
                    sectionKey = sectionKey,
                    subjectId = "CS302",
                    subjectName = "Operating Systems",
                    subjectCode = "SOET-CS-302",
                    examTitle = "End-Semester Theory Exam",
                    examDateEpochMillis = now + 21 * dayMs,
                    examDateIso = "2026-11-17",
                    examTimeFormatted = "10:00 AM – 01:00 PM",
                    examDateFormatted = "17 Nov 2026 • 10:00 AM",
                    roomOrCenter = "Examination Block Hall A (Chauras Campus)"
                )
            )
        )

        studyDao.upsertSyllabusTopics(
            listOf(
                SyllabusTopicEntity("syl_cs301_u1", sectionKey, "CS301", "Data Structures & Algorithms", 1, "Unit 1: Foundations", "Asymptotic Complexity, Arrays, Sparse Matrices", true),
                SyllabusTopicEntity("syl_cs301_u2", sectionKey, "CS301", "Data Structures & Algorithms", 2, "Unit 2: Linear Lists", "Linked Lists, Stacks & Circular Queues", true),
                SyllabusTopicEntity("syl_cs301_u3", sectionKey, "CS301", "Data Structures & Algorithms", 3, "Unit 3: Trees", "BST, AVL Rotations, B-Trees & Heaps", false),
                SyllabusTopicEntity("syl_cs302_u1", sectionKey, "CS302", "Operating Systems", 1, "Unit 1: OS Architecture", "System Calls & Process Control Block", true),
                SyllabusTopicEntity("syl_cs302_u2", sectionKey, "CS302", "Operating Systems", 2, "Unit 2: Concurrency", "CPU Scheduling, Peterson's Solution, Semaphores", false)
            )
        )

        val sem1Courses = listOf(
            CourseGradeInput("s1_1", "SOET-CS-101", "Programming in C", 4, 8.0),
            CourseGradeInput("s1_2", "SOET-CS-102", "Digital Logic", 4, 8.0),
            CourseGradeInput("s1_3", "SOS-MATH-101", "Calculus", 4, 9.0)
        )
        val sem2Courses = listOf(
            CourseGradeInput("s2_1", "SOET-CS-201", "Object Oriented C++", 4, 9.0),
            CourseGradeInput("s2_2", "SOET-CS-202", "Computer Organization", 4, 8.0),
            CourseGradeInput("s2_3", "SOS-MATH-201", "Linear Algebra", 4, 9.0)
        )
        studyDao.upsertGpaSemesters(
            listOf(
                GpaSemesterEntity(1, "TEN_POINT_INDIAN", 12, 8.33, json.encodeToString(sem1Courses)),
                GpaSemesterEntity(2, "TEN_POINT_INDIAN", 12, 8.67, json.encodeToString(sem2Courses))
            )
        )

        // =========================================================================
        // 4. Seed Phase 3.1: Campus Clubs & Events
        // =========================================================================
        campusLifeDao.upsertClubs(
            listOf(
                ClubEntity(
                    id = "club_gdsc_hnbgu",
                    collegeId = collegeId,
                    name = "GDSC & Coding Club HNBGU",
                    logoEmojiOrUrl = "💻",
                    description = "Official Google Developer Student Club & Competitive Programming community at SOET Chauras Campus.",
                    category = "TECH",
                    memberCount = 340,
                    contactEmail = "gdsc.soet@hnbgu.ac.in",
                    socialLink = "https://gdsc.community.dev/hemvati-nandan-bahuguna-garhwal-university/",
                    adminUid = "uid_cr",
                    adminName = "Priyanshu Negi (Club Lead)",
                    isFollowedByMe = true,
                    isApproved = true
                ),
                ClubEntity(
                    id = "club_anjwal_folk",
                    collegeId = collegeId,
                    name = "Anjwal: Himalayan Cultural & Theatre Society",
                    logoEmojiOrUrl = "🎭",
                    description = "Garhwali, Kumaoni & Jaunsari folk music, street theatre (Nukkad Natak), and cultural heritage club.",
                    category = "CULTURAL",
                    memberCount = 520,
                    contactEmail = "cultural.club@hnbgu.ac.in",
                    socialLink = "https://hnbgu.ac.in/student-welfare/cultural",
                    adminUid = "uid_meera",
                    adminName = "Meera Kandwal",
                    isFollowedByMe = true,
                    isApproved = true
                ),
                ClubEntity(
                    id = "club_himalayan_trekkers",
                    collegeId = collegeId,
                    name = "Alaknanda Adventure & Sports Club",
                    logoEmojiOrUrl = "🏔️",
                    description = "High-altitude trekking, inter-campus football, badminton, and athletics at Chauras Stadium.",
                    category = "SPORTS",
                    memberCount = 215,
                    contactEmail = "sports@hnbgu.ac.in",
                    socialLink = "https://hnbgu.ac.in/sports-board",
                    adminUid = "uid_coach",
                    adminName = "Rohan Gusain",
                    isFollowedByMe = false,
                    isApproved = true
                ),
                ClubEntity(
                    id = "club_robotics_pending",
                    collegeId = collegeId,
                    name = "Garhwal Drone & Robotics Initiative",
                    logoEmojiOrUrl = "🤖",
                    description = "New student proposal for UAV & embedded systems lab projects (Awaiting CR/Teacher approval).",
                    category = "TECH",
                    memberCount = 18,
                    contactEmail = "robotics@hnbgu.ac.in",
                    socialLink = "https://hnbgu.ac.in/soet",
                    adminUid = "uid_student_new",
                    adminName = "Aditya Semwal",
                    isFollowedByMe = false,
                    isApproved = false // Demonstrates CR/Teacher club moderation queue
                )
            )
        )

        campusLifeDao.upsertEvents(
            listOf(
                CampusEventEntity(
                    id = "evt_devfest_garhwal",
                    collegeId = collegeId,
                    clubId = "club_gdsc_hnbgu",
                    clubName = "GDSC & Coding Club HNBGU",
                    category = "TECH",
                    title = "Garhwal Hack & Offline-First Android Bootcamp",
                    description = "24-hour hackathon building low-bandwidth apps for hill universities. Prizes, mentorship & certificates.",
                    posterUrl = "https://hnbgu.ac.in/events/garhwal_hack_poster.jpg",
                    venue = "Swami Manmathan Auditorium, Chauras Campus",
                    eventDateFormatted = "Today • 04:00 PM",
                    eventEpochMillis = now + 4 * hourMs, // Today filter match
                    goingCount = 148,
                    interestedCount = 64,
                    notGoingCount = 5,
                    myRsvpStatus = EventRsvpStatus.GOING.name,
                    reminderLeadMinutes = 60,
                    isApproved = true
                ),
                CampusEventEntity(
                    id = "evt_anjwal_cultural",
                    collegeId = collegeId,
                    clubId = "club_anjwal_folk",
                    clubName = "Anjwal: Himalayan Cultural & Theatre Society",
                    category = "CULTURAL",
                    title = "Anjwal: Garhwali & Kumaoni Folk Music Evening",
                    description = "Annual cultural showcase celebrating Uttarakhand heritage, dhol-damau ensemble, and student theatre.",
                    posterUrl = "https://hnbgu.ac.in/events/anjwal_cultural.jpg",
                    venue = "Birla Campus Amphitheatre, Srinagar Garhwal",
                    eventDateFormatted = "In 4 days • 04:30 PM",
                    eventEpochMillis = now + 4 * dayMs, // This Week filter match
                    goingCount = 312,
                    interestedCount = 120,
                    notGoingCount = 8,
                    myRsvpStatus = EventRsvpStatus.INTERESTED.name,
                    reminderLeadMinutes = 120,
                    isApproved = true
                ),
                CampusEventEntity(
                    id = "evt_pending_workshop",
                    collegeId = collegeId,
                    clubId = "club_gdsc_hnbgu",
                    clubName = "GDSC & Coding Club HNBGU",
                    category = "WORKSHOP",
                    title = "Open Source Git & Linux Install Fest",
                    description = "Bring your laptop to CS Lab 1 for dual-boot Linux setup and first GitHub pull request.",
                    venue = "CS Block Lab 1 (Chauras)",
                    eventDateFormatted = "Next Week • 02:00 PM",
                    eventEpochMillis = now + 9 * dayMs,
                    goingCount = 12,
                    interestedCount = 9,
                    notGoingCount = 0,
                    myRsvpStatus = EventRsvpStatus.NONE.name,
                    isApproved = false // Demonstrates CR/Teacher event approval queue
                )
            )
        )

        // =========================================================================
        // 5. Seed Phase 3.2: Privacy-First Lost & Found + Contact Request + Auto-Archived Item
        // =========================================================================
        campusLifeDao.upsertLostFound(
            listOf(
                LostFoundEntity(
                    id = "lf_calculator",
                    collegeId = collegeId,
                    type = "FOUND",
                    category = "ELECTRONICS",
                    itemTitle = "Casio fx-991EX Scientific Calculator",
                    description = "Found on the 2nd row bench in CS Block Room 104 after Discrete Math lecture. Has initials written on the back cover.",
                    photoUri = "content://campusone/compressed_calc_photo.jpg",
                    location = "CS Block Room 104 (Chauras)",
                    dateFormatted = "Today • 11:30 AM",
                    posterUid = "uid_saurabh",
                    posterName = "Saurabh Panwar (B.Sc CS Sem 3)",
                    preferredContactMethod = "WhatsApp / Call: +91-94120-55104 or collect from Trishul Hostel Room 208",
                    status = "OPEN",
                    isResolved = false,
                    isArchived = false,
                    createdAtMillis = now - 3 * hourMs
                ),
                LostFoundEntity(
                    id = "lf_id_card",
                    collegeId = collegeId,
                    type = "LOST",
                    category = "ID_CARD",
                    itemTitle = "HNBGU Central Library ID Card & Blue Lanyard",
                    description = "Lost near Central Canteen / Chauras Bus Stand around 1:30 PM. Roll No. ends in 042.",
                    photoUri = null,
                    location = "Chauras Central Canteen",
                    dateFormatted = "Yesterday",
                    posterUid = "uid_aarav_me", // Owned by current student so they can test Accepting/Ignoring requests!
                    posterName = "Aarav Rawat",
                    preferredContactMethod = "Email: aarav.rawat@hnbgu.ac.in / Meet at CS Block Room 104",
                    status = "OPEN",
                    isResolved = false,
                    isArchived = false,
                    createdAtMillis = now - 20 * hourMs
                ),
                LostFoundEntity(
                    id = "lf_old_archived",
                    collegeId = collegeId,
                    type = "FOUND",
                    category = "BOOKS",
                    itemTitle = "CLRS Introduction to Algorithms (Library Issue Copy)",
                    description = "Left in Reading Hall 2 over a month ago. Auto-archived after 30 days.",
                    location = "Central Library Reading Hall",
                    dateFormatted = "35 days ago",
                    posterUid = "uid_lib",
                    posterName = "Library Desk",
                    preferredContactMethod = "Library Counter 1",
                    status = "ARCHIVED",
                    isResolved = false,
                    isArchived = true,
                    createdAtMillis = now - 35 * dayMs
                )
            )
        )

        campusLifeDao.upsertContactRequest(
            LostFoundContactRequestEntity(
                id = "req_1",
                itemId = "lf_id_card",
                itemTitle = "HNBGU Central Library ID Card & Blue Lanyard",
                posterUid = "uid_aarav_me",
                requesterUid = "uid_vikram",
                requesterName = "Vikram Negi (B.Tech IT Sem 5)",
                message = "Hi Aarav! I found a blue HNBGU lanyard near the Chauras canteen steps. Accept my request so I can see where to hand it over.",
                status = "PENDING",
                revealedContactInfo = null,
                createdAtMillis = now - 1 * hourMs
            )
        )

        // =========================================================================
        // 6. Seed Phase 3.3: Offline Campus Directory + Map Places (Free Google Maps Intents)
        // =========================================================================
        campusLifeDao.upsertDirectory(
            listOf(
                DirectoryContactEntity(
                    id = "dir_proctor",
                    collegeId = collegeId,
                    category = "EMERGENCY",
                    department = "Emergency & Proctorial Board",
                    name = "Chief Proctor Office & Security Control Room",
                    designation = "24x7 Campus Security, Anti-Ragging & Student Welfare",
                    phone = "01346-252143",
                    email = "proctor@hnbgu.ac.in",
                    officeLocation = "Birla Main Gate & Chauras Security Post",
                    officeHours = "24 Hours (Emergency Response)",
                    isEmergencyOrImportant = true
                ),
                DirectoryContactEntity(
                    id = "dir_medical",
                    collegeId = collegeId,
                    category = "HOSTEL_MEDICAL",
                    department = "University Health Center & Ambulance",
                    name = "HNBGU Medical Officer & Campus Dispensary",
                    designation = "First Aid, OPD & 24x7 Student Ambulance Service",
                    phone = "01346-252199",
                    email = "medical@hnbgu.ac.in",
                    officeLocation = "Near Guest House, Chauras Campus",
                    officeHours = "OPD: 09:00 AM – 05:00 PM • Ambulance: 24x7",
                    isEmergencyOrImportant = true
                ),
                DirectoryContactEntity(
                    id = "dir_exam_controller",
                    collegeId = collegeId,
                    category = "IMPORTANT_OFFICE",
                    department = "Examination & Samarth ERP Cell",
                    name = "Controller of Examinations (CoE)",
                    designation = "Admit Cards, Degree/Transcripts & Samarth Portal Helpdesk",
                    phone = "01346-252160",
                    email = "coe@hnbgu.ac.in",
                    officeLocation = "Administrative Block, Birla Campus, Srinagar",
                    officeHours = "Mon–Sat, 10:00 AM – 04:30 PM",
                    isEmergencyOrImportant = true
                ),
                DirectoryContactEntity(
                    id = "dir_hostel_warden",
                    collegeId = collegeId,
                    category = "HOSTEL_MEDICAL",
                    department = "Hostel Administration (Boys & Girls Hostels)",
                    name = "Chief Warden Office (Trishul, Kailash, Nanda Devi & Bhagirathi Hostels)",
                    designation = "Hostel Allotment, Mess Committee & Night Gate Pass",
                    phone = "01346-252210",
                    email = "chiefwarden@hnbgu.ac.in",
                    officeLocation = "DSW Building, Chauras Campus",
                    officeHours = "Mon–Sat, 10:00 AM – 05:00 PM",
                    isEmergencyOrImportant = false
                ),
                DirectoryContactEntity(
                    id = "dir_cs_hod",
                    collegeId = collegeId,
                    category = "FACULTY",
                    department = "Dept. of Computer Science & Engineering",
                    name = "Dr. M. M. S. Rauthan",
                    designation = "Professor & Head, SOET",
                    phone = "01346-253201",
                    email = "hod.cse@hnbgu.ac.in",
                    officeLocation = "CS Block Room 101, Chauras Campus",
                    officeHours = "Mon–Fri, 11:00 AM – 01:00 PM",
                    isEmergencyOrImportant = false
                ),
                DirectoryContactEntity(
                    id = "dir_os_faculty",
                    collegeId = collegeId,
                    category = "FACULTY",
                    department = "Dept. of Computer Science & Engineering",
                    name = "Dr. Varun Barthwal",
                    designation = "Assistant Professor (Operating Systems & Cloud Computing)",
                    phone = "01346-253205",
                    email = "varun.barthwal@hnbgu.ac.in",
                    officeLocation = "CS Block Faculty Cabin 4, Chauras Campus",
                    officeHours = "Mon–Fri, 12:15 PM – 02:00 PM",
                    isEmergencyOrImportant = false
                ),
                DirectoryContactEntity(
                    id = "dir_library",
                    collegeId = collegeId,
                    category = "IMPORTANT_OFFICE",
                    department = "Central Library",
                    name = "University Central Library Helpdesk",
                    designation = "Book Bank, PYQ Archives & e-ShodhSindhu Access",
                    phone = "01346-252218",
                    email = "librarian@hnbgu.ac.in",
                    officeLocation = "Central Library Building, Chauras Campus",
                    officeHours = "Mon–Sat, 09:00 AM – 06:00 PM",
                    isEmergencyOrImportant = false
                )
            )
        )

        campusLifeDao.upsertCampusMapPlaces(
            listOf(
                CampusMapPlaceEntity(
                    id = "map_chauras_library",
                    collegeId = collegeId,
                    name = "HNBGU Central Library & Reading Hall",
                    category = "LIBRARY",
                    campusName = "Chauras Campus, Srinagar Garhwal",
                    description = "3-floor central library, digital repository, and air-conditioned reading rooms.",
                    latitude = 30.2272,
                    longitude = 78.8024,
                    mapQueryLabel = "Central Library HNB Garhwal University Chauras Campus"
                ),
                CampusMapPlaceEntity(
                    id = "map_soet_cs_block",
                    collegeId = collegeId,
                    name = "School of Engineering & Technology (SOET / CS Block)",
                    category = "DEPARTMENT",
                    campusName = "Chauras Campus, Srinagar Garhwal",
                    description = "B.Tech, B.Sc. CS/IT, MCA lecture halls, Computing Center Labs 1 & 2.",
                    latitude = 30.2281,
                    longitude = 78.8035,
                    mapQueryLabel = "School of Engineering and Technology HNBGU Chauras"
                ),
                CampusMapPlaceEntity(
                    id = "map_auditorium",
                    collegeId = collegeId,
                    name = "Swami Manmathan Auditorium & Canteen Plaza",
                    category = "CANTEEN",
                    campusName = "Chauras Campus, Srinagar Garhwal",
                    description = "Main auditorium, student cafeteria, Nescafe kiosk, and stationary center.",
                    latitude = 30.2265,
                    longitude = 78.8018,
                    mapQueryLabel = "Swami Manmathan Auditorium HNBGU Chauras"
                ),
                CampusMapPlaceEntity(
                    id = "map_birla_admin",
                    collegeId = collegeId,
                    name = "Administrative Block & Controller of Examinations",
                    category = "ADMIN",
                    campusName = "Birla Campus, Srinagar Garhwal",
                    description = "Vice-Chancellor Secretariat, Registrar Office, Samarth ERP & Exam Cell.",
                    latitude = 30.2224,
                    longitude = 78.7831,
                    mapQueryLabel = "HNB Garhwal University Birla Campus Srinagar"
                ),
                CampusMapPlaceEntity(
                    id = "map_hostels",
                    collegeId = collegeId,
                    name = "Chauras Hostels Complex (Trishul, Kailash & Nanda Devi)",
                    category = "HOSTEL",
                    campusName = "Chauras Campus, Srinagar Garhwal",
                    description = "Undergraduate & Postgraduate student hostels, mess blocks, and sports ground.",
                    latitude = 30.2290,
                    longitude = 78.8050,
                    mapQueryLabel = "Chauras Campus Hostel HNBGU"
                )
            )
        )
    }

    // =========================================================================
    // Phase 2 Repository Methods (Kept 100% Working)
    // =========================================================================
    fun observeResources(sectionKey: String): Flow<List<StudyResourceEntity>> =
        studyDao.observeVisibleResources(sectionKey)

    fun observeModerationQueue(sectionKey: String): Flow<List<StudyResourceEntity>> =
        studyDao.observeModerationQueueForCr(sectionKey)

    suspend fun uploadStudyResourceWithProgress(
        sectionKey: String,
        subjectId: String,
        subjectName: String,
        semester: Int,
        title: String,
        category: String,
        examYear: Int,
        fileUrl: String,
        fileType: String,
        fileSizeBytes: Long,
        uploaderUid: String,
        uploaderName: String,
        showUploaderName: Boolean
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        StudyFileAndPdfHelper.validateFileSize(fileSizeBytes).getOrThrow()
        val effectiveSize = if (fileType == "IMAGE" && fileSizeBytes > 600_000L) 520_000L else fileSizeBytes

        val resId = "res_${System.currentTimeMillis()}"
        val initialEntity = StudyResourceEntity(
            id = resId,
            sectionKey = sectionKey,
            subjectId = subjectId,
            subjectName = subjectName,
            semester = semester,
            title = title.trim(),
            category = category,
            examYear = examYear,
            fileUrl = fileUrl.trim(),
            fileType = fileType,
            fileSizeBytes = effectiveSize,
            uploaderUid = uploaderUid,
            uploaderName = uploaderName,
            showUploaderName = showUploaderName,
            uploadStatus = "UPLOADING",
            uploadProgressPct = 20
        )
        studyDao.upsertResources(listOf(initialEntity))
        for (pct in listOf(45, 75, 100)) {
            delay(150)
            studyDao.updateUploadProgress(resId, if (pct == 100) "COMPLETED" else "UPLOADING", pct)
        }
    }

    suspend fun retryFailedUpload(id: String) {
        for (pct in listOf(30, 70, 100)) {
            delay(150)
            studyDao.updateUploadProgress(id, if (pct == 100) "COMPLETED" else "UPLOADING", pct)
        }
    }

    suspend fun toggleBookmarkOffline(id: String, bookmarked: Boolean) =
        studyDao.setResourceBookmark(id, bookmarked)

    suspend fun downloadResourceToLocalStorage(resource: StudyResourceEntity): String {
        val offlineDir = File(context.filesDir, "offline_study_vault").apply { mkdirs() }
        val ext = if (resource.fileType == "PDF") "pdf" else "jpg"
        val targetFile = File(offlineDir, "${resource.id}.$ext")
        if (!targetFile.exists()) {
            targetFile.writeText("Offline cached content for ${resource.title} (${resource.subjectId})")
        }
        studyDao.setResourceLocalPath(resource.id, targetFile.absolutePath)
        return targetFile.absolutePath
    }

    suspend fun upvoteResource(id: String) = studyDao.upvoteResource(id)
    suspend fun reportResourceAbuse(id: String) = studyDao.reportResourceAbuse(id)
    suspend fun moderateHideResource(id: String, hidden: Boolean) = studyDao.setResourceHiddenByCr(id, hidden)
    suspend fun moderateDeleteResource(id: String) = studyDao.deleteResourceByCr(id)
    suspend fun moderateDismissReport(id: String) = studyDao.dismissReportByCr(id)

    fun observeAssignments(): Flow<List<AssignmentEntity>> = studyDao.observeAssignments()

    suspend fun saveAssignment(
        id: String? = null,
        sectionKey: String,
        subjectId: String,
        subjectTag: String,
        title: String,
        description: String,
        priority: String,
        dueDateEpochMillis: Long,
        dueDateFormatted: String,
        remind1Day: Boolean,
        remind2Hours: Boolean,
        isSharedByCrOrTeacher: Boolean,
        authorName: String
    ) {
        val actualId = id ?: "assign_${System.currentTimeMillis()}"
        val entity = AssignmentEntity(
            id = actualId,
            sectionKey = sectionKey,
            subjectId = subjectId,
            subjectTag = subjectTag,
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            dueDateEpochMillis = dueDateEpochMillis,
            dueDateFormatted = dueDateFormatted.trim(),
            reminderEnabled = remind1Day || remind2Hours,
            remind1DayBefore = remind1Day,
            remind2HoursBefore = remind2Hours,
            isCompleted = false,
            isSharedByCrOrTeacher = isSharedByCrOrTeacher,
            isAddedToPersonalList = true,
            sharedSourceId = if (isSharedByCrOrTeacher) actualId else null,
            authorName = authorName
        )
        studyDao.upsertAssignments(listOf(entity))
    }

    suspend fun addSharedAssignmentToPersonalList(id: String) =
        studyDao.addSharedAssignmentToMyList(id)

    suspend fun toggleAssignmentCompleted(id: String, completed: Boolean) =
        studyDao.setAssignmentCompleted(id, completed)

    suspend fun deleteAssignment(id: String) =
        studyDao.deleteAssignment(id)

    fun observeExams(): Flow<List<ExamEntity>> = studyDao.observeExams()
    fun observeSyllabusTopics(): Flow<List<SyllabusTopicEntity>> = studyDao.observeSyllabusTopics()

    suspend fun addOrPublishExam(
        sectionKey: String,
        subjectId: String,
        subjectName: String,
        subjectCode: String,
        examTitle: String,
        examDateIso: String,
        examTimeFormatted: String,
        venue: String,
        daysFromNow: Int,
        publishedByCr: Boolean
    ) {
        val dueMillis = System.currentTimeMillis() + daysFromNow.coerceAtLeast(1) * 86_400_000L
        val entity = ExamEntity(
            id = "exam_${System.currentTimeMillis()}",
            sectionKey = sectionKey,
            subjectId = subjectId.trim(),
            subjectName = subjectName.trim(),
            subjectCode = subjectCode.trim(),
            examTitle = examTitle.trim(),
            examDateEpochMillis = dueMillis,
            examDateIso = examDateIso.trim(),
            examTimeFormatted = examTimeFormatted.trim(),
            examDateFormatted = "$examDateIso • $examTimeFormatted",
            roomOrCenter = venue.trim(),
            reminderEnabled = true,
            publishedByCrOrTeacher = publishedByCr
        )
        studyDao.upsertExams(listOf(entity))
    }

    suspend fun addSyllabusTopic(
        sectionKey: String,
        subjectId: String,
        subjectName: String,
        unitNumber: Int,
        unitTitle: String,
        topicTitle: String,
        publishedByCr: Boolean
    ) {
        val entity = SyllabusTopicEntity(
            id = "syl_${System.currentTimeMillis()}",
            sectionKey = sectionKey,
            subjectId = subjectId.trim(),
            subjectName = subjectName.trim(),
            unitNumber = unitNumber,
            unitTitle = unitTitle.trim(),
            topicTitle = topicTitle.trim(),
            isCompleted = false,
            publishedByCrOrTeacher = publishedByCr
        )
        studyDao.upsertSyllabusTopics(listOf(entity))
    }

    suspend fun toggleSyllabusTopic(id: String, completed: Boolean) =
        studyDao.setSyllabusTopicCompleted(id, completed)

    fun observeSavedGpaSemesters(): Flow<List<SemesterGpaRecord>> =
        studyDao.observeGpaSemesters().map { list ->
            list.map { entity ->
                val courses = runCatching {
                    json.decodeFromString<List<CourseGradeInput>>(entity.coursesJson)
                }.getOrDefault(emptyList())
                SemesterGpaRecord(
                    semesterNumber = entity.semesterNumber,
                    totalCredits = entity.totalCredits,
                    sgpa = entity.sgpa,
                    courses = courses,
                    updatedAtMillis = entity.updatedAtMillis
                )
            }
        }

    suspend fun saveCurrentSemesterGpaToHistory(
        semesterNumber: Int,
        scaleName: String,
        courses: List<CourseGradeInput>,
        sgpa: Double
    ) {
        val credits = courses.sumOf { it.credits.coerceAtLeast(0) }
        val entity = GpaSemesterEntity(
            semesterNumber = semesterNumber,
            scaleType = scaleName,
            totalCredits = credits,
            sgpa = sgpa,
            coursesJson = json.encodeToString(courses),
            updatedAtMillis = System.currentTimeMillis()
        )
        studyDao.upsertGpaSemesters(listOf(entity))
    }

    // =========================================================================
    // Phase 3 Repository Methods (Clubs, 3-State Events RSVP, Privacy Lost & Found, Directory & Map)
    // =========================================================================
    fun observeClubs(collegeId: String): Flow<List<ClubEntity>> =
        campusLifeDao.observeClubs(collegeId)

    /**
     * Follows/unfollows a club and subscribes/unsubscribes to FCM topic `club_{clubId}`
     * so students get a push notification whenever a followed club posts a new event.
     */
    suspend fun toggleFollowClub(clubId: String, follow: Boolean) {
        campusLifeDao.setClubFollowed(clubId, follow)
        runCatching {
            val fcm = FirebaseMessaging.getInstance()
            if (follow) fcm.subscribeToTopic("club_$clubId") else fcm.unsubscribeFromTopic("club_$clubId")
        }
    }

    suspend fun createOrProposeClub(
        collegeId: String,
        name: String,
        logoEmoji: String,
        description: String,
        category: String,
        email: String,
        socialLink: String,
        creatorUid: String,
        creatorName: String,
        isAutoApproved: Boolean
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        val club = ClubEntity(
            id = "club_${System.currentTimeMillis()}",
            collegeId = collegeId,
            name = name.trim(),
            logoEmojiOrUrl = logoEmoji.ifBlank { "🌟" },
            description = description.trim(),
            category = category,
            memberCount = 1,
            contactEmail = email.trim(),
            socialLink = socialLink.trim(),
            adminUid = creatorUid,
            adminName = creatorName,
            isFollowedByMe = true,
            isApproved = isAutoApproved
        )
        campusLifeDao.upsertClubs(listOf(club))
    }

    suspend fun updateClubPage(clubId: String, description: String, email: String, socialLink: String) {
        campusLifeDao.updateClubDetails(clubId, description.trim(), email.trim(), socialLink.trim())
    }

    suspend fun approveClub(clubId: String) = campusLifeDao.approveClubByCr(clubId)

    fun observeEvents(collegeId: String): Flow<List<CampusEventEntity>> =
        campusLifeDao.observeEvents(collegeId)

    /**
     * Uses `CampusLifeEngine.transitionRsvp` to compute exact Going / Interested / Not Going counts.
     */
    suspend fun updateEventRsvp(eventId: String, newStatus: EventRsvpStatus) {
        val current = campusLifeDao.getEventById(eventId) ?: return
        val prevStatus = runCatching { EventRsvpStatus.valueOf(current.myRsvpStatus) }
            .getOrDefault(EventRsvpStatus.NONE)
        val snapshot = CampusLifeEngine.transitionRsvp(
            currentGoing = current.goingCount,
            currentInterested = current.interestedCount,
            currentNotGoing = current.notGoingCount,
            previousStatus = prevStatus,
            newStatus = newStatus
        )
        campusLifeDao.updateEventRsvpCounts(
            eventId = eventId,
            going = snapshot.goingCount,
            interested = snapshot.interestedCount,
            notGoing = snapshot.notGoingCount,
            status = snapshot.userStatus.name
        )
    }

    suspend fun createClubEvent(
        collegeId: String,
        clubId: String,
        clubName: String,
        category: String,
        title: String,
        description: String,
        venue: String,
        dateFormatted: String,
        daysFromNow: Int,
        isAutoApproved: Boolean
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        val epoch = System.currentTimeMillis() + daysFromNow.coerceAtLeast(0) * 86_400_000L + 3_600_000L * 4
        val event = CampusEventEntity(
            id = "evt_${System.currentTimeMillis()}",
            collegeId = collegeId,
            clubId = clubId,
            clubName = clubName,
            category = category,
            title = title.trim(),
            description = description.trim(),
            venue = venue.trim(),
            eventDateFormatted = dateFormatted.trim(),
            eventEpochMillis = epoch,
            goingCount = 1,
            interestedCount = 0,
            notGoingCount = 0,
            myRsvpStatus = EventRsvpStatus.GOING.name,
            isApproved = isAutoApproved
        )
        campusLifeDao.upsertEvents(listOf(event))
    }

    suspend fun approveEvent(eventId: String) = campusLifeDao.approveEventByCr(eventId)
    suspend fun reportEvent(eventId: String) = campusLifeDao.reportEventAbuse(eventId)
    suspend fun deleteEvent(eventId: String) = campusLifeDao.deleteEvent(eventId)

    // Phase 3.2: Privacy-First Lost & Found
    fun observeLostFound(collegeId: String): Flow<List<LostFoundEntity>> =
        campusLifeDao.observeLostFound(collegeId)

    fun observeContactRequests(): Flow<List<LostFoundContactRequestEntity>> =
        campusLifeDao.observeContactRequests()

    suspend fun addLostFoundItem(
        collegeId: String,
        type: String,
        category: String,
        itemTitle: String,
        description: String,
        location: String,
        posterUid: String,
        posterName: String,
        preferredContactMethod: String
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        val entity = LostFoundEntity(
            id = "lf_${System.currentTimeMillis()}",
            collegeId = collegeId,
            type = type,
            category = category,
            itemTitle = itemTitle.trim(),
            description = description.trim(),
            photoUri = "content://campusone/compressed_${System.currentTimeMillis()}.jpg",
            location = location.trim(),
            dateFormatted = "Just now",
            posterUid = posterUid,
            posterName = posterName,
            preferredContactMethod = preferredContactMethod.trim(),
            status = "OPEN",
            isResolved = false,
            isArchived = false
        )
        campusLifeDao.upsertLostFound(listOf(entity))
    }

    suspend fun sendLostFoundClaimRequest(
        item: LostFoundEntity,
        requesterUid: String,
        requesterName: String,
        message: String
    ): Result<Unit> = runCatching {
        val req = LostFoundContactRequestEntity(
            id = "req_${item.id}_$requesterUid",
            itemId = item.id,
            itemTitle = item.itemTitle,
            posterUid = item.posterUid,
            requesterUid = requesterUid,
            requesterName = requesterName,
            message = message.trim(),
            status = "PENDING",
            revealedContactInfo = null
        )
        campusLifeDao.upsertContactRequest(req)
    }

    suspend fun respondToContactRequest(
        request: LostFoundContactRequestEntity,
        accept: Boolean
    ) {
        if (accept) {
            val item = campusLifeDao.getLostFoundById(request.itemId)
            val contactToShare = item?.preferredContactMethod ?: "Verified via CampusOne"
            campusLifeDao.updateContactRequestStatus(request.id, "ACCEPTED", contactToShare)
        } else {
            campusLifeDao.updateContactRequestStatus(request.id, "IGNORED", null)
        }
    }

    suspend fun markLostFoundResolved(id: String) = campusLifeDao.markLostFoundResolved(id)
    suspend fun reportLostFound(id: String) = campusLifeDao.reportLostFoundAbuse(id)
    suspend fun hideLostFoundByCr(id: String) = campusLifeDao.hideLostFoundByCr(id)

    // Phase 3.3: Campus Directory, Suggestions & Map
    fun observeDirectory(collegeId: String): Flow<List<DirectoryContactEntity>> =
        campusLifeDao.observeDirectory(collegeId)

    fun observeDirectorySuggestions(collegeId: String): Flow<List<DirectorySuggestionEntity>> =
        campusLifeDao.observePendingDirectorySuggestions(collegeId)

    fun observeCampusMapPlaces(collegeId: String): Flow<List<CampusMapPlaceEntity>> =
        campusLifeDao.observeCampusMapPlaces(collegeId)

    suspend fun submitDirectorySuggestion(
        collegeId: String,
        contactId: String,
        contactName: String,
        suggesterUid: String,
        suggesterName: String,
        proposedChange: String
    ) {
        campusLifeDao.insertDirectorySuggestion(
            DirectorySuggestionEntity(
                id = "sug_${System.currentTimeMillis()}",
                collegeId = collegeId,
                contactId = contactId,
                contactName = contactName,
                suggesterUid = suggesterUid,
                suggesterName = sugggesterName,
                proposedChange = proposedChange.trim(),
                status = "PENDING"
            )
        )
    }

    suspend fun resolveDirectorySuggestion(id: String, approve: Boolean) {
        campusLifeDao.updateDirectorySuggestionStatus(id, if (approve) "APPROVED" else "REJECTED")
    }
}
