import org.jetbrains.dokka.gradle.DokkaExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mavenPublish) apply false
}

// One API reference for both published modules: ./gradlew :dokkaGenerate -> build/dokka/html.
// -PdokkaStrict=true fails on any warning, such as a broken KDoc link. CI passes it on macOS only,
// because the iOS source sets are analyzed only there; the javadoc jars build on any host.
val dokkaStrict = providers.gradleProperty("dokkaStrict").map(String::toBoolean).orElse(false)

dependencies {
    dokka(project(":a11yotp"))
    dokka(project(":a11yotp-sms-retriever"))
}

dokka {
    moduleName.set("A11yOtp")
    dokkaPublications.html {
        failOnWarning.set(dokkaStrict)
    }
}

// Published modules: strict mode fails on a broken KDoc link, and declarations link to their source on GitHub.
subprojects {
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure<DokkaExtension> {
            dokkaPublications.configureEach {
                failOnWarning.set(dokkaStrict)
            }
            dokkaSourceSets.configureEach {
                sourceLink {
                    localDirectory.set(projectDir.resolve("src"))
                    remoteUrl("https://github.com/Vprogrammers/a11yOtp/tree/main/${project.name}/src")
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
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
