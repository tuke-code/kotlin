import gradle.enableKotlinSerializationPlugin

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
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
