// Phase 0 spike: upstream's Room database (schema v214) compiled and opened on the JVM.
// Standalone build; not part of the root Android build. Versions follow gradle/libs.versions.toml.
plugins {
    kotlin("jvm") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.7"
    id("androidx.room") version "2.7.1"
}

kotlin { jvmToolchain(17) }

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.sqlite:sqlite-bundled:2.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    ksp("androidx.room:room-compiler:2.7.1")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed"); showStandardStreams = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

// Pass criterion: the schema Room generates on the JVM is the one upstream ships for v214.
val verifySchemaMatchesUpstream by tasks.registering {
    dependsOn("kspKotlin")
    doLast {
        val path = "com.nendo.argosy.data.local.ALauncherDatabase/214.json"
        val ours = file("schemas/$path").readText()
        val upstream = file("../../app/schemas/$path").readText()
        fun hash(s: String) = Regex("\"identityHash\": \"(\\w+)\"").find(s)!!.groupValues[1]
        check(hash(ours) == hash(upstream)) { "identityHash ${hash(ours)} != upstream ${hash(upstream)}" }
        check(ours.trim() == upstream.trim()) { "schema JSON differs from upstream" }
        println("schema v214 identityHash ${hash(ours)} matches upstream")
    }
}
tasks.test { dependsOn(verifySchemaMatchesUpstream) }
