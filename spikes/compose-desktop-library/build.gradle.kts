// Phase 0 spike: upstream's Library game list rendered by Compose Multiplatform on the desktop.
// Standalone build; not part of the root Android build.
//   src/upstream/kotlin  upstream files, copied unchanged by copy-sources.sh
//   src/extracted/kotlin  single declarations copied verbatim by extract_decls.py
//   src/main/kotlin      spike code: stubs for what is Android-bound, fake data, entry points
plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.compose") version "1.12.2"
}

kotlin {
    jvmToolchain(17)
    sourceSets["main"].kotlin.srcDirs("src/upstream/kotlin", "src/extracted/kotlin", "src/shims/kotlin", "src/generated/kotlin")
    sourceSets["main"].resources.srcDir("src/generated/resources")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    // Entities carry Room annotations; the KMP runtime artifact provides them on the JVM.
    implementation("androidx.room:room-runtime:2.7.1")
}

compose.desktop {
    application { mainClass = "spike.MainKt" }
}

// Headless render of the Library list to library.png.
val renderPng by tasks.registering(JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "spike.RenderKt"
    args(file("library.png").absolutePath)
}

// Upstream's Android resources and assets, unchanged, on the classpath for the shims to read.
tasks.processResources {
    from("../../app/src/main/res/drawable") { into("drawable") }
    from("../../app/src/main/assets/platforms") { into("assets/platforms") }
    // AssetManager.list() needs a directory index; the classpath has none.
    doLast {
        val dir = destinationDir.resolve("assets/platforms")
        dir.resolve(".index").writeText(dir.list()!!.filter { it != ".index" }.sorted().joinToString("\n"))
    }
}

// R.string / R.plurals / R.drawable and the English string table, generated from app/src/main/res.
val generateResources by tasks.registering(Exec::class) {
    inputs.dir("../../app/src/main/res/values"); inputs.dir("../../app/src/main/res/drawable"); inputs.file("gen_resources.py")
    outputs.dir("src/generated")
    commandLine("python3", "-I", "gen_resources.py")
}
tasks.compileKotlin { dependsOn(generateResources) }
tasks.processResources { dependsOn(generateResources) }
