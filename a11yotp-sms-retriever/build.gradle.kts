import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
}

// Optional Android-only module: Play Services stays off the classpath of a11yotp consumers.
kotlin {
    android {
        namespace = "me.vprogrammers.a11yotp.smsretriever"
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
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":a11yotp"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.androidx.core)
            implementation(libs.play.services.auth.api.phone)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

composeCompiler {
    targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
}
