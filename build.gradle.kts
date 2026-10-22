import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint)
}

val ktlintRulesetVersion = libs.versions.ktlintVersion.get()

ktlint {
    version.set(ktlintRulesetVersion)

    filter {
        exclude { fileTreeElement ->
            fileTreeElement.file.invariantSeparatorsPath.contains("/build/generated/")
        }
    }
}

subprojects {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        version.set(ktlintRulesetVersion)

        filter {
            exclude { fileTreeElement ->
                fileTreeElement.file.invariantSeparatorsPath.contains("/build/generated/")
            }
        }
    }
}
