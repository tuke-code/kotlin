plugins {
    kotlin("multiplatform")
}

group = "com.example"

kotlin {
    jvm()
    sourceSets {
        jvmMain {
            dependencies {
                implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-Beta1-1.0.15")
            }
        }
    }
}
