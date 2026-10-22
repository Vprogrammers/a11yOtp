import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

val a11yOtpVersion = providers.gradleProperty("a11yOtpVersion").get()

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export("me.vprogrammers:a11yotp:$a11yOtpVersion")
        }
    }

    android {
        namespace = "consumer.check"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api("me.vprogrammers:a11yotp:$a11yOtpVersion")
        }
        androidMain.dependencies {
            implementation("me.vprogrammers:a11yotp-sms-retriever:$a11yOtpVersion")
        }
    }
}
