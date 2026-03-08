package com.takehome.twinmind.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingStateHolder @Inject constructor() {

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun updateState(transform: RecordingState.() -> RecordingState) {
        _state.update { it.transform() }
    }

    fun reset() {
        _state.value = RecordingState()
    }
}
