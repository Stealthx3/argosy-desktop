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

val generateAndroidViewModelModules by tasks.registering {
    val commonSources = layout.projectDirectory.dir("src/commonMain/kotlin")
    val outDir = layout.buildDirectory.dir("generated/androidViewModelModules")
    inputs.dir(commonSources)
    outputs.dir(outDir)
    doLast {
        val annotated = Regex("""^@HiltViewModel\s*(?:\([^)]*\)\s*)?(?:@\w+\s*)*class (\w+)""", RegexOption.MULTILINE)
        val viewModels = commonSources.asFile.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val source = file.readText()
                val pkg = Regex("""^package ([\w.]+)""", RegexOption.MULTILINE).find(source)?.groupValues?.get(1)
                annotated.findAll(source).map { "$pkg.${it.groupValues[1]}" }
            }
            .sorted()
            .toList()
        val names = viewModels.mapIndexed { index, fqName ->
            val simple = fqName.substringAfterLast('.').replaceFirstChar { it.lowercase() }
            if (viewModels.count { it.substringAfterLast('.') == fqName.substringAfterLast('.') } > 1) "$simple$index" else simple
        }
        val lines = buildList {
            add("package com.nendo.argosy.di")
            add("")
            add("import androidx.lifecycle.ViewModel")
            add("import dagger.Binds")
            add("import dagger.Module")
            add("import dagger.Provides")
            add("import dagger.hilt.InstallIn")
            add("import dagger.hilt.android.components.ActivityRetainedComponent")
            add("import dagger.hilt.android.components.ViewModelComponent")
            add("import dagger.hilt.android.internal.lifecycle.HiltViewModelMap")
            add("import dagger.multibindings.IntoMap")
            add("import dagger.multibindings.LazyClassKey")
            add("")
            add("@Module")
            add("@InstallIn(ViewModelComponent::class)")
            add("abstract class SharedViewModelBindsModule {")
            viewModels.zip(names).forEach { (fqName, name) ->
                add("    @Binds")
                add("    @IntoMap")
                add("    @LazyClassKey($fqName::class)")
                add("    @HiltViewModelMap")
                add("    abstract fun $name(viewModel: $fqName): ViewModel")
            }
            add("}")
            add("")
            add("@Module")
            add("@InstallIn(ActivityRetainedComponent::class)")
            add("object SharedViewModelKeyModule {")
            viewModels.zip(names).forEach { (fqName, name) ->
                add("    @Provides")
                add("    @IntoMap")
                add("    @LazyClassKey($fqName::class)")
                add("    @HiltViewModelMap.KeySet")
                add("    fun $name(): Boolean = true")
            }
            add("}")
        }
        val dir = outDir.get().dir("androidMain/com/nendo/argosy/di").asFile
        dir.mkdirs()
        dir.resolve("SharedViewModelModules.kt").writeText(lines.joinToString("\n") + "\n")
    }
}

tasks.matching { it.name.matches(Regex("ksp(Debug|Release)KotlinAndroid")) }.configureEach {
    doLast {
        outputs.files.asFileTree.matching { include("**/*.java") }.forEach { file ->
            val source = file.readText()
            if ("com.nendo.argosy.platform.PlatformContext" !in source) return@forEach
            file.writeText(
                source
                    .replace("import com.nendo.argosy.platform.PlatformContext;", "import android.content.Context;")
                    .replace(Regex("""\bcom\.nendo\.argosy\.platform\.PlatformContext\b"""), "android.content.Context")
                    .replace(Regex("""\bPlatformContext\b"""), "Context")
            )
        }
    }
}

val androidResources = layout.projectDirectory.dir("src/androidMain/res")

val prepareDesktopComposeResources by tasks.registering(Sync::class) {
    from(androidResources) {
        include("values*/strings*.xml", "drawable/*.xml", "raw/*")
        eachFile {
            path = path
                .replace(Regex("^values-b\\+zh\\+Hans/"), "values-zh/")
                .replace(Regex("^values-b\\+zh\\+Hant/"), "values-zh-rTW/")
                .replace(Regex("^raw/"), "files/raw/")
        }
    }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("generated/desktopComposeResources"))
}

val prepareDesktopAssets by tasks.registering {
    val assets = layout.projectDirectory.dir("src/androidMain/assets")
    val outDir = layout.buildDirectory.dir("generated/desktopAssets")
    inputs.dir(assets)
    outputs.dir(outDir)
    doLast {
        val root = outDir.get().asFile.resolve("argosy-assets")
        root.deleteRecursively()
        assets.asFile.copyRecursively(root)
        val index = root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.sorted()
        root.resolve("index.txt").writeText(index.joinToString("\n") + "\n")
    }
}

val generateSharedR by tasks.registering {
    val baseStrings = androidResources.dir("values").asFileTree.matching { include("strings*.xml") }
    val drawables = androidResources.dir("drawable").asFileTree
    val raws = androidResources.dir("raw").asFileTree
    val outDir = layout.buildDirectory.dir("generated/sharedR")
    inputs.files(baseStrings, drawables, raws)
    outputs.dir(outDir)
    doLast {
        val xml = baseStrings.files.sortedBy { it.name }.joinToString("\n") { it.readText() }
        val strings = Regex("""<string\s+name="(\w+)"""").findAll(xml).map { it.groupValues[1] }.distinct().toList()
        val plurals = Regex("""<plurals\s+name="(\w+)"""").findAll(xml).map { it.groupValues[1] }.distinct().toList()
        val drawableNames = drawables.files.map { it.nameWithoutExtension }.sorted()
        val rawFiles = raws.files.map { it.name }.sorted()
        val groups = listOf(
            Triple("string", 0x7f010000, strings),
            Triple("plurals", 0x7f020000, plurals),
            Triple("drawable", 0x7f030000, drawableNames),
            Triple("raw", 0x7f040000, rawFiles.map { it.substringBeforeLast('.') }),
        )
        val root = outDir.get().asFile
        root.deleteRecursively()
        fun rFile(sourceSet: String, header: String, property: (String, Int, String) -> String) {
            val dir = root.resolve("$sourceSet/com/nendo/argosy/res").apply { mkdirs() }
            dir.resolve("ArgosyR.$sourceSet.kt").writeText(buildString {
                appendLine("package com.nendo.argosy.res")
                appendLine()
                appendLine("${header}object ArgosyR {")
                groups.forEach { (type, base, names) ->
                    appendLine("    ${header.replace("expect ", "")}object $type {")
                    names.forEachIndexed { i, name -> appendLine("        ${property(type, base + i, name)}") }
                    appendLine("    }")
                }
                appendLine("}")
            })
        }
        rFile("commonMain", "expect ") { _, _, name -> "val `$name`: Int" }
        rFile("androidMain", "actual ") { type, _, name ->
            "actual val `$name`: Int get() = com.nendo.argosy.shared.R.$type.`$name`"
        }
        rFile("desktopMain", "actual ") { _, id, name -> "actual val `$name`: Int get() = $id" }
        val chunk = 400
        val lookupDir = root.resolve("desktopMain/com/nendo/argosy/shared/resources").apply { mkdirs() }
        lookupDir.resolve("AndroidResourceIds.kt").writeText(buildString {
            appendLine("package com.nendo.argosy.shared.resources")
            appendLine()
            appendLine("import org.jetbrains.compose.resources.DrawableResource")
            appendLine("import org.jetbrains.compose.resources.PluralStringResource")
            appendLine("import org.jetbrains.compose.resources.StringResource")
            appendLine()
            appendLine("internal object AndroidResourceIds {")
            listOf(
                Triple("string", "StringResource", groups[0]),
                Triple("plurals", "PluralStringResource", groups[1]),
                Triple("drawable", "DrawableResource", groups[2]),
            ).forEach { (type, kind, group) ->
                val (_, base, names) = group
                val chunks = names.chunked(chunk)
                appendLine("    fun $type(id: Int): $kind = when ((id - $base) / $chunk) {")
                chunks.indices.forEach { c -> appendLine("        $c -> ${type}Chunk$c(id - $base)") }
                appendLine("        else -> throw IllegalArgumentException(\"no $type resource \${'$'}id\")")
                appendLine("    }")
                chunks.forEachIndexed { c, names ->
                    appendLine("    private fun ${type}Chunk$c(index: Int): $kind = when (index) {")
                    names.forEachIndexed { i, name -> appendLine("        ${c * chunk + i} -> Res.$type.`$name`") }
                    appendLine("        else -> throw IllegalArgumentException(\"no $type resource \${'$'}index\")")
                    appendLine("    }")
                }
            }
            appendLine("    fun rawPath(id: Int): String = when (id - ${groups[3].second}) {")
            rawFiles.forEachIndexed { i, file -> appendLine("        $i -> \"files/raw/$file\"") }
            appendLine("        else -> throw IllegalArgumentException(\"no raw resource \${'$'}id\")")
            appendLine("    }")
            appendLine("}")
        })
    }
}

compose.resources {
    packageOfResClass = "com.nendo.argosy.shared.resources"
    publicResClass = false
    generateResClass = always
    customDirectory(
        sourceSetName = "desktopMain",
        directoryProvider = layout.dir(prepareDesktopComposeResources.map { it.destinationDir })
    )
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
        getByName("androidMain").kotlin.srcDir(
            generateAndroidViewModelModules.map { it.outputs.files.singleFile.resolve("androidMain") }
        )
        listOf("commonMain", "androidMain", "desktopMain").forEach { name ->
            getByName(name).kotlin.srcDir(generateSharedR.map { it.outputs.files.singleFile.resolve(name) })
        }
        getByName("desktopMain").resources.srcDir(prepareDesktopAssets.map { it.outputs.files.singleFile })
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.room.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.javax.inject)
            implementation(libs.dagger)
            implementation(libs.bundles.network)
            implementation(libs.commons.compress)
            implementation(libs.xz)
            implementation(libs.bundles.steam)
            implementation(libs.bouncycastle)
            implementation(libs.upokecenter.cbor)
            implementation(libs.datastore.preferences)
            implementation(libs.androidx.annotation)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.hilt.android)
            implementation(libs.hilt.navigation.compose)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.coil.compose)
            implementation(libs.coil.svg)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.ui)
            implementation(libs.media3.datasource.okhttp)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.zstd.jni)
            implementation(libs.org.json)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.okhttp)
            implementation(libs.coil3.svg)
        }
        getByName("desktopTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(compose.desktop.currentOs)
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

    lint {
        error += listOf(
            "MissingTranslation",
            "ExtraTranslation",
            "StringFormatMatches",
            "StringFormatInvalid",
            "ImpliedQuantity",
            "Untranslatable"
        )
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
