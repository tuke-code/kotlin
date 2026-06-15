plugins {
    kotlin("jvm")
}

val commonCompilerModules: Array<String> by rootProject.extra
val descriptorsCompilerModules: Array<String> by rootProject.extra
val analysisApiModules = listOf(
    ":analysis:analysis-api",
    ":analysis:decompiled:decompiler-js",
    ":analysis:decompiled:decompiler-native",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:light-classes-for-decompiled",
    ":analysis:low-level-api-fir",
    ":analysis:stubs",
)

val projects = commonCompilerModules.asList() + descriptorsCompilerModules + analysisApiModules + listOf(
    ":compiler:arguments.common",
    ":compiler:cli-base",
    ":kotlin-build-common",
    ":kotlin-compiler-runner-unshaded",
    ":kotlin-preloader",
    ":daemon-common",
    ":kotlin-daemon-client",
    ":compiler:build-tools:kotlin-build-tools-api",
)

publishJarsForIde(
    projects = projects,
    libraryDependencies = listOf(protobufFull())
)
