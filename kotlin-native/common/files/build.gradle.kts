import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.solib

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
}

val library = solib("orgjetbrainskotlinbackendkonanfilesstubs")

bitcode {
    hostTarget {
        module("files") {
            srcRoot.set(layout.projectDirectory.dir("src"))
            headersDirs.from(srcRoot.dir("headers"))
            sourceSets {
                main {}
            }
        }
    }
}

kotlinNativeInterop {
    create("files") {
        pkg("org.jetbrains.kotlin.backend.konan.files")
        linker("clang++")
        linkOutputs(bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get())
        headers(layout.projectDirectory.files("src/headers/Files.h"))
    }
}


dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" {
        kotlin.srcDir(kotlinNativeInterop["files"].genTask.map { layout.buildDirectory.dir("nativeInteropStubs/files/kotlin") })
    }
}

val cppLinkElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_LINK))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

val cppRuntimeElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(cppLinkElements.name, layout.buildDirectory.dir("nativelibs/${TargetWithSanitizer.host}/$library")) {
        builtBy(kotlinNativeInterop["files"].genTask)
    }
    add(cppRuntimeElements.name, layout.buildDirectory.dir("nativelibs/${TargetWithSanitizer.host}/$library")) {
        builtBy(kotlinNativeInterop["files"].genTask)
    }
}