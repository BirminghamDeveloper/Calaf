pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Calaf"
include(":app", ":core:model", ":core:designsystem")
include(":core:data", ":core:ui", ":core:testing")
include(":feature:auth", ":feature:onboarding", ":feature:discovery", ":feature:matches")
include(":feature:chat", ":feature:settings", ":feature:admin")
