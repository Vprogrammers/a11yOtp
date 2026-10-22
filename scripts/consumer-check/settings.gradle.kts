// A clean consumer of the published artifacts, built by scripts/verify-publication.sh.
// It is a separate build: the library comes from Maven Local, never from project(...).
rootProject.name = "a11yotp-consumer-check"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal {
            content { includeGroup("me.vprogrammers") }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
