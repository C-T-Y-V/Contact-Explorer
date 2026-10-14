pluginManagement {
    repositories {
        google()
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

rootProject.name = "ContactExplorer"
include(":app")
include(":core:api")
include(":core:contacts")
include(":core:database")
include(":core:permissions")
include(":core:plugin-api")
include(":core:plugin-registry")
include(":feature:calling")
include(":feature:groups")
include(":feature:appearance")
include(":feature:search")
include(":feature:ai")
