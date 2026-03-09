package com.takehome.twinmind.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute : NavKey

@Serializable
data object OnboardingRoute : NavKey

@Serializable
data object SignInRoute : NavKey

@Serializable
data object LocationPermissionRoute : NavKey

@Serializable
data object DashboardRoute : NavKey

@Serializable
data class RecordingRoute(val recordingId: String) : NavKey

@Serializable
data object PersonalizationRoute : NavKey

@Serializable
data class SessionDetailRoute(val sessionId: String) : NavKey

@Serializable
data class ChatRoute(val sessionId: String) : NavKey

@Serializable
data class MemoriesRoute(val initialTab: Int = 0) : NavKey
