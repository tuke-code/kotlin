plugins {
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:fir:resolve"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
