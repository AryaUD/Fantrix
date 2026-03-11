pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        // 🔥 REQUIRED for WebRTC
        maven {
            url = uri("https://maven.webrtc.org")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Fantrix"
include(":app")