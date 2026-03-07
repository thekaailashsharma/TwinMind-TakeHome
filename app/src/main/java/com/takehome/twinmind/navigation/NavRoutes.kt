package com.takehome.twinmind.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SignInRoute : NavKey

@Serializable
data object LocationPermissionRoute : NavKey

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object RecordingRoute : NavKey

@Serializable
data object PersonalizationRoute : NavKey
