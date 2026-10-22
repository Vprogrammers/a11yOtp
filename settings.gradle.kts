rootProject.name = "A11yOtp"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":a11yotp")

// Optional Android-only SMS Retriever integration; keeps Play Services off a11yotp.
include(":a11yotp-sms-retriever")

// Consumer-like KMP shared module used by the sample apps.
include(":sampleShared")
project(":sampleShared").projectDir = file("sample/shared")
