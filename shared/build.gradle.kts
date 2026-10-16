import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
        }
    }
}

android {
    namespace = "com.nendo.argosy.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

composeCompiler {
    if (!project.hasProperty("noStability")) {
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("app/compose_stability_config.conf")
        )
    }
    featureFlags.add(ComposeFeatureFlag.IntrinsicRemember.disabled())
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups.disabled())
    featureFlags.add(ComposeFeatureFlag.PausableComposition.disabled())
}
