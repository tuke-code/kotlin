import gradle.enableKotlinSerializationPlugin

plugins {
    java
    kotlin("jvm")
    id("test-inputs-check")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()
enableKotlinSerializationPlugin()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation(kotlin("stdlib", coreDepsVersion))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit", coreDepsVersion))
}
