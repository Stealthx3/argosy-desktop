import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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
            implementation(libs.room.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.javax.inject)
            implementation(libs.bundles.network)
            implementation(libs.commons.compress)
            implementation(libs.xz)
            implementation(libs.bundles.steam)
            implementation(libs.bouncycastle)
            implementation(libs.upokecenter.cbor)
            implementation(libs.datastore.preferences)
        }
        androidMain.dependencies {
            implementation(libs.hilt.android)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.zstd.jni)
            implementation(libs.org.json)
        }
        getByName("desktopTest").dependencies {
            implementation(libs.kotlin.test)
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

room {
    schemaDirectory(rootProject.layout.projectDirectory.dir("app/schemas").asFile.path)
}

dependencies {
    addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(
        "commonMainImplementation",
        libs.socketio.client
    ) {
        exclude(group = "org.json", module = "json")
    }
    addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(
        "androidMainImplementation",
        libs.zstd.jni
    ) {
        artifact { type = "aar" }
    }
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspAndroid", libs.moshi.kotlin)
    add("kspDesktop", libs.moshi.kotlin)
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
