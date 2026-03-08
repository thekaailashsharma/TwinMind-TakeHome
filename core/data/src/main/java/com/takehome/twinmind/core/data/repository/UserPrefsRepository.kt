package com.takehome.twinmind.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.dataStore

    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    val userName: Flow<String> =
        dataStore.data.map { it[USER_NAME].orEmpty() }

    val userBio: Flow<String> =
        dataStore.data.map { it[USER_BIO].orEmpty() }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun setUserBio(bio: String) {
        dataStore.edit { it[USER_BIO] = bio }
    }

    suspend fun savePersonalization(
        name: String,
        bio: String,
        role: String,
        interests: String,
    ) {
        dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_BIO] = bio
            prefs[USER_ROLE] = role
            prefs[USER_INTERESTS] = interests
        }
    }

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_BIO = stringPreferencesKey("user_bio")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val USER_INTERESTS = stringPreferencesKey("user_interests")
    }
}
