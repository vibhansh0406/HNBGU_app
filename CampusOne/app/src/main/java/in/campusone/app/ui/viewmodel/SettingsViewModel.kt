package `in`.campusone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    val languageTag = preferences.languageTag.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "en"
    )
    val notificationsEnabled = preferences.notificationsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val announcementsEnabled = preferences.announcementsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val noticesEnabled = preferences.noticesEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val examUpdatesEnabled = preferences.examUpdatesEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val assignmentUpdatesEnabled = preferences.assignmentUpdatesEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val studyMaterialUpdatesEnabled = preferences.studyMaterialUpdatesEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val classRemindersEnabled = preferences.classRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val assignmentRemindersEnabled = preferences.assignmentRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val examRemindersEnabled = preferences.examRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )
    val eventRemindersEnabled = preferences.eventRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )

    fun setLanguage(language: String) = viewModelScope.launch { preferences.switchLanguage(language) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setNotificationsEnabled(enabled)
    }
    fun setNotificationCategory(category: String, enabled: Boolean) = viewModelScope.launch {
        preferences.setNotificationCategoryEnabled(category, enabled)
    }
    fun setLocalReminder(category: String, enabled: Boolean) = viewModelScope.launch {
        preferences.setLocalReminderEnabled(category, enabled)
    }
}
