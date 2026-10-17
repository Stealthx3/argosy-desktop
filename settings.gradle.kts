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
        maven { url = uri("${rootDir}/libs/maven") }
    }
}

rootProject.name = "argosy-launcher"
include(":app")
include(":shared")
include(":desktopApp")
include(":sigil")
project(":sigil").projectDir = file("sigil/bindings/android")
include(":sigil-jvm")
project(":sigil-jvm").projectDir = file("desktop-libs/sigil-jvm")
include(":libretrodroid-api")
project(":libretrodroid-api").projectDir = file("desktop-libs/libretrodroid-api")
include(":libretrodroid")
