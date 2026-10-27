package `in`.campusone.app.di

import android.content.Context
import `in`.campusone.app.data.local.CampusOneDatabase
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.data.repository.OfflineCoreCampusRepository
import `in`.campusone.app.data.repository.OfflineStudyAndCampusRepository
import `in`.campusone.app.data.repository.StudyAndCampusRepository
import `in`.campusone.app.util.BackupRestoreManager

/**
 * Manual Dependency Injection container (zero Hilt/Dagger codegen risk).
 * Instantiated once inside [in.campusone.app.CampusOneApp].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: CampusOneDatabase by lazy {
        CampusOneDatabase.getInstance(appContext)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val coreCampusRepository: CoreCampusRepository by lazy {
        OfflineCoreCampusRepository(
            coreDao = database.coreDao(),
            userPrefs = userPreferencesRepository
        )
    }

    val studyAndCampusRepository: StudyAndCampusRepository by lazy {
        OfflineStudyAndCampusRepository(
            appContext = appContext,
            dao = database.studyAndCampusDao()
        )
    }

    val backupRestoreManager: BackupRestoreManager by lazy {
        BackupRestoreManager(
            context = appContext,
            database = database,
            userPreferences = userPreferencesRepository
        )
    }
}
