package com.campusone.app.di

import android.content.Context
import com.campusone.app.data.local.CampusOneDatabase
import com.campusone.app.data.preferences.UserPreferencesRepository
import com.campusone.app.data.remote.SupabaseCampusClient
import com.campusone.app.data.repository.CoreCampusRepository
import com.campusone.app.data.repository.OfflineCoreCampusRepository
import com.campusone.app.data.repository.OfflineStudyAndCampusRepository
import com.campusone.app.data.repository.StudyAndCampusRepository
import com.campusone.app.data.repository.SupabaseSyncRepository
import com.campusone.app.util.BackupRestoreManager
import com.campusone.app.util.DepartmentPackManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: CampusOneDatabase by lazy {
        CampusOneDatabase.getInstance(appContext)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val supabaseCampusClient: SupabaseCampusClient by lazy {
        SupabaseCampusClient()
    }

    val supabaseSyncRepository: SupabaseSyncRepository by lazy {
        SupabaseSyncRepository(
            client = supabaseCampusClient,
            database = database,
            userPreferences = userPreferencesRepository
        )
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

    val departmentPackManager: DepartmentPackManager by lazy {
        DepartmentPackManager(
            context = appContext,
            database = database,
            userPreferences = userPreferencesRepository
        )
    }
}
