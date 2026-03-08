package com.takehome.twinmind.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.data.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalizationViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
) : ViewModel() {

    fun save(name: String, role: String, language: String, additionalInfo: String) {
        viewModelScope.launch {
            userPrefsRepository.savePersonalization(
                name = name,
                bio = additionalInfo,
                role = role,
                interests = language,
            )
        }
    }
}
