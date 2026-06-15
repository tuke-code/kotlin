plugins {
    kotlin("jvm")
}

val commonCompilerModules: Array<String> by rootProject.extra
val descriptorsCompilerModules: Array<String> by rootProject.extra
val analysisApiModules = listOf(
    ":analysis:analysis-api",
    ":analysis:analysis-api-fir",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-platform-interface",
    ":analysis:analysis-api-standalone",
    ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
    ":analysis:analysis-api-standalone:analysis-api-standalone-base",
    ":analysis:analysis-internal-utils",
    ":analysis:decompiled",
    ":analysis:decompiled:decompiler-js",
    ":analysis:decompiled:decompiler-native",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:light-classes-for-decompiled",
    ":analysis:low-level-api-fir",
    ":analysis:stubs",
)

val excludedAnalysisApiModules = listOf(
    ":analysis:decompiled",
)

val projects = commonCompilerModules.asList() + descriptorsCompilerModules + analysisApiModules - excludedAnalysisApiModules + listOf(
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
