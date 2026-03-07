pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TwinMind"

include(":app")

include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:database")
include(":core:data")
include(":core:audio")
include(":core:notifications")

include(":feature:auth")
include(":feature:dashboard")
include(":feature:recording")
include(":feature:summary")
