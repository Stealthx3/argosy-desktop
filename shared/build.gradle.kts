import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.gradle.AppExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
}

evaluationDependsOn(":app")

val appAndroid = project(":app").extensions.getByName("android") as AppExtension
val appBuildTypes = listOf("debug", "release")

val appBuildConfigFields: Map<String, Map<String, Pair<String, String>>> = appBuildTypes.associateWith { type ->
    val declared = appAndroid.defaultConfig.buildConfigFields + appAndroid.buildTypes.getByName(type).buildConfigFields
    declared.mapValues { (_, field) -> field.type to field.value } + mapOf(
        "DEBUG" to ("boolean" to (type == "debug").toString()),
        "VERSION_NAME" to ("String" to "\"${appAndroid.defaultConfig.versionName}\""),
        "VERSION_CODE" to ("int" to appAndroid.defaultConfig.versionCode.toString()),
    )
}

val generateAppBuildConfig by tasks.registering {
    val fields = appBuildConfigFields
    val outDir = layout.buildDirectory.dir("generated/appBuildConfig")
    inputs.property("fields", fields.toString())
    outputs.dir(outDir)
    doLast {
        val kotlinType = mapOf(
            "String" to "String", "boolean" to "Boolean", "Boolean" to "Boolean", "int" to "Int",
            "Integer" to "Int", "long" to "Long", "Long" to "Long", "float" to "Float", "double" to "Double",
        )
        fun literal(type: String, value: String) =
            if (type == "String") value.replace("$", "\\$") else value
        val release = fields.getValue("release")
        val names = release.keys.sorted()
        val varying = names.filter { name -> appBuildTypes.map { fields.getValue(it)[name] }.distinct().size > 1 }
        val pkg = "package com.nendo.argosy.config"
        fun write(sourceSet: String, fileName: String, lines: List<String>) {
            val dir = outDir.get().dir("$sourceSet/com/nendo/argosy/config").asFile
            dir.mkdirs()
            dir.resolve(fileName).writeText((listOf(pkg, "") + lines).joinToString("\n") + "\n")
        }
        write("commonMain", "AppBuildConfig.kt", buildList {
            add("object AppBuildConfig {")
            names.forEach { name ->
                val (type, value) = release.getValue(name)
                if (name in varying) {
                    add("    val $name: ${kotlinType.getValue(type)} get() = AppBuildVariant.$name")
                } else {
                    add("    const val $name: ${kotlinType.getValue(type)} = ${literal(type, value)}")
                }
            }
            add("}")
            add("")
            add("internal expect object AppBuildVariant {")
            varying.forEach { add("    val $it: ${kotlinType.getValue(release.getValue(it).first)}") }
            add("}")
        })
        write("androidMain", "AppBuildVariant.android.kt", buildList {
            add("internal actual object AppBuildVariant {")
            varying.forEach {
                add("    actual val $it: ${kotlinType.getValue(release.getValue(it).first)} = com.nendo.argosy.shared.BuildConfig.$it")
            }
            add("}")
        })
        write("desktopMain", "AppBuildVariant.desktop.kt", buildList {
            add("internal actual object AppBuildVariant {")
            varying.forEach {
                val (type, value) = release.getValue(it)
                add("    actual val $it: ${kotlinType.getValue(type)} = ${literal(type, value)}")
            }
            add("}")
        })
    }
}

val generateDesktopDaoModule by tasks.registering {
    val database = layout.projectDirectory.file("src/commonMain/kotlin/com/nendo/argosy/data/local/ALauncherDatabase.kt")
    val outDir = layout.buildDirectory.dir("generated/desktopDaoModule")
    inputs.file(database)
    outputs.dir(outDir)
    doLast {
        val source = database.asFile.readText()
        val imports = Regex("""^import ([\w.]+)$""", RegexOption.MULTILINE).findAll(source)
            .associate { it.groupValues[1].substringAfterLast('.') to it.groupValues[1] }
        val accessors = Regex("""abstract fun (\w+)\(\): ([\w.]+)""").findAll(source).map { match ->
            val type = match.groupValues[2]
            match.groupValues[1] to (if ('.' in type) type else imports[type] ?: "com.nendo.argosy.data.local.$type")
        }.toList()
        val lines = buildList {
            add("package com.nendo.argosy.di")
            add("")
            add("import com.nendo.argosy.data.local.ALauncherDatabase")
            add("import dagger.Module")
            add("import dagger.Provides")
            add("")
            add("@Module")
            add("object DesktopDaoModule {")
            accessors.forEach { (name, type) ->
                add("    @Provides")
                add("    fun $name(database: ALauncherDatabase): $type = database.$name()")
            }
            add("}")
        }
        val dir = outDir.get().dir("desktopMain/com/nendo/argosy/di").asFile
        dir.mkdirs()
        dir.resolve("DesktopDaoModule.kt").writeText(lines.joinToString("\n") + "\n")
    }
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
        listOf("commonMain", "androidMain", "desktopMain").forEach { name ->
            getByName(name).kotlin.srcDir(generateAppBuildConfig.map { it.outputs.files.singleFile.resolve(name) })
        }
        getByName("desktopMain").kotlin.srcDir(
            generateDesktopDaoModule.map { it.outputs.files.singleFile.resolve("desktopMain") }
        )
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
            implementation(libs.dagger)
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

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        appBuildTypes.forEach { type ->
            getByName(type) {
                appBuildConfigFields.getValue(type).filterKeys { it != "DEBUG" }.forEach { (name, field) ->
                    buildConfigField(field.first, name, field.second)
                }
            }
        }
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
    add("kspAndroid", libs.hilt.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspDesktop", libs.dagger.compiler)
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

tasks.withType<JavaCompile>().matching { it.name.startsWith("compileDesktop") }.configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}
