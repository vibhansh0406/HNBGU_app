package com.campusone.app.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleSignInIdTokenResult(
    val idToken: String,
    val email: String,
    val displayName: String?
)

object GoogleCredentialHelper {

    suspend fun requestGoogleIdToken(
        activityContext: Context,
        googleWebClientId: String
    ): Result<GoogleSignInIdTokenResult> {
        val cleanClientId = googleWebClientId.trim()
        if (cleanClientId.isBlank() ||
            !cleanClientId.endsWith(".googleusercontent.com") ||
            cleanClientId.contains("YOUR_GOOGLE")
        ) {
            return Result.failure(
                IllegalStateException(
                    "GOOGLE_WEB_CLIENT_ID is not configured yet. Add your Google Web OAuth 2.0 Client ID in CampusOne/local.properties or Settings -> Cloud & Supabase Setup."
                )
            )
        }

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(cleanClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(
                    GoogleSignInIdTokenResult(
                        idToken = googleIdTokenCredential.idToken,
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName
                    )
                )
            } else {
                Result.failure(
                    IllegalStateException("Unexpected credential type returned by Google Identity Services.")
                )
            }
        } catch (cancelled: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Google sign-in was cancelled."))
        } catch (noCred: NoCredentialException) {
            Result.failure(
                IllegalStateException(
                    "No Google account is signed in on this device, or the Android SHA-1 / Web Client ID does not match your Google Cloud Console OAuth configuration."
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Google Sign-In failed: ${e.message ?: "Unknown error"}")
            )
        }
    }
}
