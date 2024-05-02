import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("jvm")
    kotlin("plugin.power-assert")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    implementation(project(":core:util.runtime"))

    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    includedSourceSets = listOf("main")
    functions = listOf(
        "kotlin.check",
        "kotlin.require",
        "org.jetbrains.kotlin.utils.exceptions.checkWithAttachment",
        "org.jetbrains.kotlin.utils.exceptions.requireWithAttachment",
    )
}
