import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

sourceSets.getByName("main") {
    kotlin.srcDir(rootProject.file("libretrodroid/src/main/java"))
    kotlin.include("com/swordfish/libretrodroid/Variable.kt", "com/swordfish/libretrodroid/ShaderConfig.kt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
