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
include(":sigil-jvm")
project(":sigil-jvm").projectDir = file("desktop-libs/sigil-jvm")
include(":graphics-shim-android")
project(":graphics-shim-android").projectDir = file("desktop-libs/graphics-shim-android")
include(":graphics-shim-desktop")
project(":graphics-shim-desktop").projectDir = file("desktop-libs/graphics-shim-desktop")
include(":libretrodroid-api")
project(":libretrodroid-api").projectDir = file("desktop-libs/libretrodroid-api")
// Flathub builds the desktop app offline with no Android SDK or NDK: -Pargosy.desktopOnly=true leaves
// out the two modules that need the NDK to configure (Android tasks cannot run in that mode).
if (providers.gradleProperty("argosy.desktopOnly").orNull != "true") {
    include(":sigil")
    project(":sigil").projectDir = file("sigil/bindings/android")
    include(":libretrodroid")
}
