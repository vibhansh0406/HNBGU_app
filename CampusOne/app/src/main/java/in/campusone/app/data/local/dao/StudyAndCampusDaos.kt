package `in`.campusone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_resources WHERE sectionKey = :sectionKey AND isHidden = 0 ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeVisibleResources(sectionKey: String, limit: Int = 50): Flow<List<StudyResourceEntity>>

    @Query("SELECT * FROM study_resources WHERE sectionKey = :sectionKey AND (isFlagged = 1 OR isHidden = 1) ORDER BY reportCount DESC, updatedAtMillis DESC")
    fun observeModerationQueueForCr(sectionKey: String): Flow<List<StudyResourceEntity>>

    @Query("SELECT * FROM study_resources WHERE id = :id LIMIT 1")
    suspend fun getResourceById(id: String): StudyResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResources(items: List<StudyResourceEntity>)

    @Query("UPDATE study_resources SET upvotes = upvotes + 1, isUpvotedByMe = 1, updatedAtMillis = :now WHERE id = :id AND isUpvotedByMe = 0")
    suspend fun upvoteResource(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE study_resources SET isBookmarkedOffline = :bookmarked, updatedAtMillis = :now WHERE id = :id")
    suspend fun setResourceBookmark(id: String, bookmarked: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE study_resources SET localFilePath = :localPath, isBookmarkedOffline = 1, updatedAtMillis = :now WHERE id = :id")
    suspend fun setResourceLocalPath(id: String, localPath: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE study_resources SET uploadStatus = :status, uploadProgressPct = :progress, updatedAtMillis = :now WHERE id = :id")
    suspend fun updateUploadProgress(id: String, status: String, progress: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE study_resources SET reportCount = reportCount + 1, isFlagged = 1, updatedAtMillis = :now WHERE id = :id")
    suspend fun reportResourceAbuse(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE study_resources SET isHidden = :hidden, isFlagged = 0, updatedAtMillis = :now WHERE id = :id")
    suspend fun setResourceHiddenByCr(id: String, hidden: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM study_resources WHERE id = :id")
    suspend fun deleteResourceByCr(id: String)

    @Query("UPDATE study_resources SET isFlagged = 0, reportCount = 0, isHidden = 0, isApprovedByCr = 1, updatedAtMillis = :now WHERE id = :id")
    suspend fun dismissReportByCr(id: String, now: Long = System.currentTimeMillis())

    // Assignments & Deadlines
    @Query("SELECT * FROM assignments ORDER BY isCompleted ASC, dueDateEpochMillis ASC")
    fun observeAssignments(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE isCompleted = 0 ORDER BY dueDateEpochMillis ASC")
    suspend fun getPendingAssignmentsSnapshot(): List<AssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(items: List<AssignmentEntity>)

    @Query("UPDATE assignments SET isCompleted = :completed, updatedAtMillis = :now, isSynced = 0 WHERE id = :id")
    suspend fun setAssignmentCompleted(id: String, completed: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE assignments SET isAddedToPersonalList = 1, updatedAtMillis = :now WHERE id = :id")
    suspend fun addSharedAssignmentToMyList(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignment(id: String)

    // Exams & Syllabus Checklist
    @Query("SELECT * FROM exams ORDER BY examDateEpochMillis ASC")
    fun observeExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY examDateEpochMillis ASC")
    suspend fun getExamsSnapshot(): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExams(items: List<ExamEntity>)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: String)

    @Query("SELECT * FROM syllabus_topics ORDER BY subjectId ASC, unitNumber ASC, id ASC")
    fun observeSyllabusTopics(): Flow<List<SyllabusTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyllabusTopics(items: List<SyllabusTopicEntity>)

    @Query("UPDATE syllabus_topics SET isCompleted = :completed, updatedAtMillis = :now WHERE id = :id")
    suspend fun setSyllabusTopicCompleted(id: String, completed: Boolean, now: Long = System.currentTimeMillis())

    // GPA Semesters History
    @Query("SELECT * FROM gpa_semesters ORDER BY semesterNumber ASC")
    fun observeGpaSemesters(): Flow<List<GpaSemesterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGpaSemesters(items: List<GpaSemesterEntity>)
}

@Dao
interface CampusLifeDao {
    // 1. Clubs
    @Query("SELECT * FROM clubs WHERE collegeId = :collegeId ORDER BY isFollowedByMe DESC, memberCount DESC")
    fun observeClubs(collegeId: String): Flow<List<ClubEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClubs(items: List<ClubEntity>)

    @Query("UPDATE clubs SET isFollowedByMe = :followed, memberCount = CASE WHEN :followed THEN memberCount + 1 ELSE MAX(0, memberCount - 1) END WHERE id = :clubId")
    suspend fun setClubFollowed(clubId: String, followed: Boolean)

    @Query("UPDATE clubs SET isApproved = 1, isFlagged = 0 WHERE id = :clubId")
    suspend fun approveClubByCr(clubId: String)

    @Query("UPDATE clubs SET description = :description, contactEmail = :email, socialLink = :socialLink, updatedAtMillis = :now WHERE id = :clubId")
    suspend fun updateClubDetails(clubId: String, description: String, email: String, socialLink: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE clubs SET adminUid = :adminUid, adminName = :adminName WHERE id = :clubId")
    suspend fun assignClubAdmin(clubId: String, adminUid: String, adminName: String)

    // 2. Events Feed
    @Query("SELECT * FROM campus_events WHERE collegeId = :collegeId ORDER BY eventEpochMillis ASC LIMIT :limit")
    fun observeEvents(collegeId: String, limit: Int = 50): Flow<List<CampusEventEntity>>

    @Query("SELECT * FROM campus_events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): CampusEventEntity?

    @Query("SELECT * FROM campus_events WHERE myRsvpStatus = 'GOING' OR myRsvpStatus = 'INTERESTED'")
    suspend fun getRsvpedEventsSnapshot(): List<CampusEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(items: List<CampusEventEntity>)

    @Query("UPDATE campus_events SET goingCount = :going, interestedCount = :interested, notGoingCount = :notGoing, myRsvpStatus = :status, updatedAtMillis = :now WHERE id = :eventId")
    suspend fun updateEventRsvpCounts(
        eventId: String,
        going: Int,
        interested: Int,
        notGoing: Int,
        status: String,
        now: Long = System.currentTimeMillis()
    )

    @Query("UPDATE campus_events SET isApproved = 1, isFlagged = 0 WHERE id = :eventId")
    suspend fun approveEventByCr(eventId: String)

    @Query("UPDATE campus_events SET reportCount = reportCount + 1, isFlagged = 1 WHERE id = :eventId")
    suspend fun reportEventAbuse(eventId: String)

    @Query("DELETE FROM campus_events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: String)

    // 3. Privacy-First Lost & Found + Auto-Archive + Contact Requests
    @Query("SELECT * FROM lost_found_items WHERE collegeId = :collegeId AND isHidden = 0 ORDER BY isResolved ASC, createdAtMillis DESC LIMIT :limit")
    fun observeLostFound(collegeId: String, limit: Int = 50): Flow<List<LostFoundEntity>>

    @Query("SELECT * FROM lost_found_items WHERE id = :id LIMIT 1")
    suspend fun getLostFoundById(id: String): LostFoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLostFound(items: List<LostFoundEntity>)

    @Query("UPDATE lost_found_items SET isResolved = 1, status = 'RESOLVED' WHERE id = :id")
    suspend fun markLostFoundResolved(id: String)

    @Query("UPDATE lost_found_items SET isArchived = 1, status = 'ARCHIVED' WHERE createdAtMillis <= :cutoffMillis AND isArchived = 0")
    suspend fun autoArchiveOldLostFound(cutoffMillis: Long)

    @Query("UPDATE lost_found_items SET reportCount = reportCount + 1, isFlagged = 1 WHERE id = :id")
    suspend fun reportLostFoundAbuse(id: String)

    @Query("UPDATE lost_found_items SET isHidden = 1 WHERE id = :id")
    suspend fun hideLostFoundByCr(id: String)

    @Query("SELECT * FROM lost_found_contact_requests ORDER BY createdAtMillis DESC")
    fun observeContactRequests(): Flow<List<LostFoundContactRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContactRequest(request: LostFoundContactRequestEntity)

    @Query("UPDATE lost_found_contact_requests SET status = :status, revealedContactInfo = :revealedContact WHERE id = :requestId")
    suspend fun updateContactRequestStatus(requestId: String, status: String, revealedContact: String?)

    // 4. Campus Directory, Suggestions & Campus Map Places
    @Query("SELECT * FROM directory_contacts WHERE collegeId = :collegeId ORDER BY isEmergencyOrImportant DESC, department ASC, name ASC")
    fun observeDirectory(collegeId: String): Flow<List<DirectoryContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectory(items: List<DirectoryContactEntity>)

    @Query("SELECT * FROM directory_suggestions WHERE collegeId = :collegeId AND status = 'PENDING' ORDER BY createdAtMillis DESC")
    fun observePendingDirectorySuggestions(collegeId: String): Flow<List<DirectorySuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectorySuggestion(suggestion: DirectorySuggestionEntity)

    @Query("UPDATE directory_suggestions SET status = :status WHERE id = :id")
    suspend fun updateDirectorySuggestionStatus(id: String, status: String)

    @Query("SELECT * FROM campus_map_places WHERE collegeId = :collegeId ORDER BY category ASC, name ASC")
    fun observeCampusMapPlaces(collegeId: String): Flow<List<CampusMapPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCampusMapPlaces(places: List<CampusMapPlaceEntity>)
}
