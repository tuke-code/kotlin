import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "2.0.0-Beta1-1.0.15"
}

group = "komem.litmus"

kotlin {
    val armEnabled = findProperty("arm") != null
    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> if (armEnabled) macosArm64() else macosX64()
        hostOs == "Linux" -> linuxX64()
        else -> throw GradleException("Host OS is not supported")
    }
//    jvm {
//        mainRun {
//            mainClass.set("JvmMainKt")
//        }
//    }

    val affinitySupported = hostOs == "Linux"
    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                create("barrier") {
                    defFile(project.file("src/nativeInterop/barrier.def"))
                    headers(project.file("src/nativeInterop/barrier.h"))
                }
                if (affinitySupported) {
                    create("affinity") {
                        defFile(project.file("src/nativeInterop/kaffinity.def"))
                        headers(project.file("src/nativeInterop/kaffinity.h"))
                    }
                }
            }
            if (gradle.startParameter.taskNames.any { it.contains("bitcode") }) {
                val tempDir = projectDir.resolve("temp/bitcode")
                if (!tempDir.exists()) tempDir.createDirectory()
                kotlinOptions.freeCompilerArgs = listOf("-Xtemporary-files-dir=${tempDir.absolutePath}")
            }
        }
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.20.2")
                implementation("com.github.ajalt.clikt:clikt:4.2.1")
            }
            kotlin.srcDir(buildDir.resolve("generated/ksp/metadata/commonMain/kotlin/")) // ksp
        }

        commonTest {
            dependencies {
                // Dependencies for Kotlin/Native test infra:
                implementation(project(":native:native.tests"))
                implementation(project(":native:kotlin-native-utils"))
//                implementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
            }
        }
    }
}

val setupCinterop by tasks.register("setupCinterop") {
    group = "interop"
    doFirst {
        val interopFolder = project.projectDir.resolve("src/nativeInterop")
        if (!interopFolder.resolve("kaffinity.def").exists()) {
            exec {
                executable = interopFolder.resolve("setup.sh").absolutePath
                args = listOf(interopFolder.absolutePath)
            }
        }
    }
}

tasks.matching { it.name.contains("cinterop") && it.name.contains("Linux") }
    .forEach { it.dependsOn(setupCinterop) }

val bitcodeInternal by tasks.register("bitcodeInternal") {
    val tempDir = projectDir.resolve("temp/bitcode")
    doLast {
        exec {
            executable = "sh"
            args = listOf(
                "-c", """
                llvm-dis -o ${tempDir.resolve("bitcode.txt")} ${tempDir.resolve("out.bc")}
            """.trimIndent()
            )
        }
    }
}

tasks.register("bitcodeDebug") {
    dependsOn(tasks.matching { it.name.startsWith("linkDebugExecutable") })
    finalizedBy(bitcodeInternal)
}

tasks.register("bitcodeRelease") {
    dependsOn(tasks.matching { it.name.startsWith("linkReleaseExecutable") })
    finalizedBy(bitcodeInternal)
}

// ======== ksp ========

dependencies {
    add("kspCommonMainMetadata", project(":litmuskt-codegen"))
}

tasks.whenTaskAdded {
    if (name == "kspCommonMainKotlinMetadata") {
        val kspTask = this
        tasks.matching { it.name.startsWith("compileKotlin") }.forEach { it.dependsOn(kspTask) }
    }
}

// compiler test

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
val nativeTargetName = HostManager.host.name

val litmusktNativeKlib by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

// TODO: is it necessary?
//sourceSets {
//    "main" { projectDefault() }
//    "test" { projectDefault() }
//}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val nativeTest = nativeTest(
    taskName = "litmusktNativeTest",
    tag = "litmuskt-native", // Should be equal to the tag in GenerateNativeTests.kt
    requirePlatformLibs = true,
    customTestDependencies = listOf(litmusktNativeKlib),
)

