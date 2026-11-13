package `in`.campusone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.campusone.app.data.repository.ModeratorRepository
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.util.security.SecurityUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ModeratorViewModel @Inject constructor(
    private val repository: ModeratorRepository
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(replay = 1)
    val errors = _errors.asSharedFlow()

    data class ModeratorUiState(
        val currentProfile: UserProfile = UserProfile(),
        val moderators: List<UserProfile> = emptyList(),
        val isAdmin: Boolean = false,
        val isModerator: Boolean = false,
        val isActiveModerator: Boolean = false,
        val selectedModerator: UserProfile? = null,
        val newModeratorName: String = "",
        val newModeratorEmail: String = "",
        val newModeratorScope: String = "",
        val isLoading: Boolean = false
    )

    private val _uiState = MutableSharedFlow<ModeratorUiState>(replay = 1)
    val uiState: StateFlow<ModeratorUiState> = _uiState.asStateFlow(initialValue = ModeratorUiState())

    fun init() {
        viewModelScope.launch {
            combine(repository.currentProfile, repository.moderators) { profile, mods ->
                Triple(profile, mods, profile.role == UserRole.ADMIN)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Triple(UserProfile(), emptyList(), false)
            ).collect { (profile, mods, isAdmin) ->
                _uiState.value = ModeratorUiState(
                    currentProfile = profile,
                    moderators = mods,
                    isAdmin = isAdmin,
                    isModerator = profile.role == UserRole.MODERATOR,
                    isActiveModerator = profile.isActiveModerator,
                    isLoading = false
                )
            }
        }
    }

    // =========================================================================
    // APPOINT MODERATOR (Admin only)
    // =========================================================================
    fun appointModerator(name: String, email: String, scope: String) {
        val state = _uiState.value
        if (!state.isAdmin) {
            _errors.emit("Access denied: only App Admin can appoint moderators.")
            return
        }

        val nameResult = SecurityUtils.validateName(name)
        val emailResult = SecurityUtils.validateEmail(email)
        val scopeResult = SecurityUtils.validateScope(scope)

        if (nameResult.isFailure || emailResult.isFailure || scopeResult.isFailure) {
            val errs = listOfNotNull(
                nameResult.exceptionOrNull()?.message,
                emailResult.exceptionOrNull()?.message,
                scopeResult.exceptionOrNull()?.message
            )
            _errors.emit(errs.joinToString("\n"))
            return
        }

        viewModelScope.launch {
            val newUid = "mod_${System.currentTimeMillis()}"
            val profile = UserProfile(
                uid = newUid,
                name = nameResult.getOrNull() ?: "",
                email = emailResult.getOrNull() ?: "",
                role = UserRole.MODERATOR,
                moderatorScope = scopeResult.getOrNull() ?: "",
                isModeratorApproved = true,
                verifiedRole = true,
                collegeId = state.currentProfile.collegeId,
                collegeName = state.currentProfile.collegeName,
                courseId = "mod_${scopeResult.getOrNull() ?: "dept".lowercase()}",
                courseName = scopeResult.getOrNull() ?: "",
                semester = 3,
                section = "A"
            )
            repository.appointModerator(profile, scopeResult.getOrNull() ?: "", true)
            _messages.emit("Moderator appointed: ${profile.name} (${profile.moderatorScope})")
            // reset form
            _uiState.value = state.copy(
                newModeratorName = "",
                newModeratorEmail = "",
                newModeratorScope = ""
            )
        }
    }

    /** Revoke a moderator (back to student). Admin only. */
    fun revokeModerator(uid: String) {
        val state = _uiState.value
        if (!state.isAdmin) {
            _errors.emit("Access denied: only App Admin can revoke moderators.")
            return
        }
        viewModelScope.launch {
            repository.revokeModerator(uid)
            _messages.emit("Moderator revoked.")
        }
    }

    /** Approve/un-approve a moderator. Admin only. */
    fun setModeratorApproval(uid: String, approved: Boolean) {
        val state = _uiState.value
        if (!state.isAdmin) {
            _errors.emit("Access denied: only App Admin can approve moderators.")
            return
        }
        viewModelScope.launch {
            repository.setModeratorApproval(uid, approved)
            _messages.emit(if (approved) "Moderator approved." else "Moderator un-approved.")
        }
    }

    /** Update moderator scope. Admin only. */
    fun updateModeratorScope(uid: String, newScope: String) {
        val state = _uiState.value
        if (!state.isAdmin) {
            _errors.emit("Access denied: only App Admin can change moderator scope.")
            return
        }
        val scopeResult = SecurityUtils.validateScope(newScope)
        if (scopeResult.isFailure) {
            _errors.emit(scopeResult.exceptionOrNull()?.message ?: "Invalid scope")
            return
        }
        viewModelScope.launch {
            repository.updateModeratorScope(uid, scopeResult.getOrNull() ?: "")
            _messages.emit("Moderator scope updated.")
        }
    }

    fun selectModerator(profile: UserProfile?) {
        _uiState.value = _uiState.value.copy(selectedModerator = profile)
    }

    fun updateNewModeratorName(value: String) {
        _uiState.value = _uiState.value.copy(newModeratorName = SecurityUtils.sanitize(value))
    }

    fun updateNewModeratorEmail(value: String) {
        _uiState.value = _uiState.value.copy(newModeratorEmail = SecurityUtils.sanitize(value))
    }

    fun updateNewModeratorScope(value: String) {
        _uiState.value = _uiState.value.copy(newModeratorScope = SecurityUtils.sanitize(value))
    }

    /** Returns moderators filtered by whether they are active (approved). */
    fun activeModerators(): List<UserProfile> =
        _uiState.value.moderators.filter { it.isActiveModerator }

    /** Returns pending moderator appointments. */
    fun pendingModerators(): List<UserProfile> =
        _uiState.value.moderators.filter { it.role == UserRole.MODERATOR && !it.isModeratorApproved }
}
