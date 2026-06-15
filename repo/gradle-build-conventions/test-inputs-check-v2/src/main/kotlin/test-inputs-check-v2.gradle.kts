plugins {
    id("java-flight-recorder")
}

val pluginBuildDir = "test-inputs-check-v2"
val testInputsCheck = extensions.create<TestInputsCheckExtensionV2>("testInputsCheck")

tasks.withType<Test>().configureEach {
    val test = this
    configureTestTask(test, declaredInputsFile(test.name))
}

afterEvaluate {
    tasks.withType<Test>().names.forEach { testName ->
        val test = tasks.named<Test>(testName)
        configureCheckTestInputsTask(test, declaredInputsFile(test.name), undeclaredInputsFile(test.name))
    }
}

fun configureTestTask(test: Test, declaredInputsFile: Provider<RegularFile>) {
    test.systemProperty("test.instrumenter.inputs.check.enabled", true)
    test.addAbsoluteFileProperty(declaredInputsFile, "test.instrumenter.declared.inputs.file")
    test.addAbsoluteDirectoryProperty(layout.settingsDirectory, "test.instrumenter.root.dir")
    test.addAbsoluteDirectoryProperty(layout.buildDirectory, "test.instrumenter.build.dir")
    test.addLazyBooleanSystemProperty(testInputsCheck.failFast, "test.instrumenter.fail.fast")

    if (testInputsCheck.skipTests.get()) {
        checkIfTestsCanBeSkipped(test)
        logger.warn("Skipping tests for task ${test.path}. Disable testInputsCheck.skipTests after you're done debugging!")
        test.actions.clear()
    }

    test.doFirst {
        declaredInputsFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(inputs.files.asFileTree.joinToString(separator = "\n"))
        }
    }
}

fun configureCheckTestInputsTask(
    test: TaskProvider<Test>,
    declaredInputsFile: Provider<RegularFile>,
    undeclaredInputsFile: Provider<RegularFile>,
) {
    val checkTestInputs = tasks.register<CheckTestInputs>("checkInputsFor${test.name.capitalize()}") {
        group = "verification"
        description = "Check undeclared inputs from task ${test.name}"
        this.jfrFile.from(test.map { it.javaFlightRecorder.jfrFile })
        this.declaredInputsFile.set(declaredInputsFile)
        this.undeclaredInputsFile.set(undeclaredInputsFile)
        this.verificationTasksDisabled.value(kotlinBuildProperties.verificationTasksDisabled).finalizeValue()
        this.teamcityBuild.value(kotlinBuildProperties.isTeamcityBuild).finalizeValue()
    }

    test.configure {
        if (enabled && !inputs.sourceFiles.isEmpty) {
            finalizedBy(checkTestInputs)
        }
    }
}

fun checkIfTestsCanBeSkipped(test: Test) {
    val jfrFile = test.javaFlightRecorder.jfrFile.singleFile
    if (!jfrFile.exists()) {
        error(buildString {
            appendLine("Tests can't be skipped if the JFR snapshot doesn't exist!")
            appendLine("Run your tests at least once to produce this file, than you will be able to skip them.")
            appendLine("The JFR snapshot will appear here: ${jfrFile.absolutePath}")
        })
    }
    if (kotlinBuildProperties.isTeamcityBuild.get()) {
        error(buildString {
            appendLine("Tests can't be skipped on TeamCity build, this feature is only for debugging!")
            appendLine("Please set testInputsCheck.skipTests = false")
        })
    }
}

fun declaredInputsFile(testName: String) =
    layout.buildDirectory.file("$pluginBuildDir/declared-inputs-for-$testName.txt")

fun undeclaredInputsFile(testName: String) =
    layout.buildDirectory.file("$pluginBuildDir/undeclared-inputs-for-$testName.txt")
