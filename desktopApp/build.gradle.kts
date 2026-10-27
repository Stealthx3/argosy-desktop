import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.jetbrains.lifecycle.runtime.compose)
}

val sigilJniDir = layout.buildDirectory.dir("sigil-jni")

val sigilJni by tasks.registering(Exec::class) {
    val source = rootProject.file("sigil")
    val buildDir = sigilJniDir.get().asFile
    inputs.dir(source.resolve("src"))
    inputs.dir(source.resolve("bindings/android/src/main/cpp"))
    outputs.file(buildDir.resolve("bindings/libsigil-jni.so"))
    environment("JAVA_HOME", System.getProperty("java.home"))
    commandLine(
        "sh", "-c",
        "cmake -S \"$source\" -B \"$buildDir\" -DSIGIL_BUILD_JNI_HOST=ON -DCMAKE_BUILD_TYPE=Release >/dev/null " +
            "&& cmake --build \"$buildDir\" --target sigil_jni_host -j >/dev/null",
    )
}

val packagedLibraryPath = "-Djava.library.path=\$APPDIR"

tasks.withType<JavaExec>().matching { it.name == "run" }.configureEach {
    dependsOn(sigilJni)
    jvmArgumentProviders.add(
        CommandLineArgumentProvider { listOf("-Djava.library.path=${sigilJniDir.get().asFile.resolve("bindings")}") },
    )
}

compose.desktop {
    application {
        mainClass = "com.nendo.argosy.desktop.MainKt"
        jvmArgs += packagedLibraryPath

        nativeDistributions {
            packageName = "argosy-desktop"
            packageVersion = "0.3.0"
            modules(
                "java.instrument",
                "java.management",
                "java.naming",
                "java.sql",
                "jdk.accessibility",
                "jdk.charsets",
                "jdk.crypto.ec",
                "jdk.localedata",
                "jdk.net",
                "jdk.security.auth",
                "jdk.unsupported",
                "jdk.zipfs",
            )
        }
    }
}
