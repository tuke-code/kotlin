plugins {
    kotlin("jvm")
}

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

sourceSets {
    "main" { projectDefault() }
}

kotlin {
    explicitApi()

    target.compilations.getByName("main").compileTaskProvider.configure {
        val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames)
        val diagnosticNamesArg = if (renderDiagnosticNames) "-Xrender-internal-diagnostic-names" else null

        compilerOptions {
            freeCompilerArgs.set(
                listOfNotNull(
                    "-Xallow-kotlin-package",
                    "-Xsuppress-missing-builtins-error",
                    diagnosticNamesArg
                )
            )
        }
    }

}

/**
 * Allow those declarations to compile against a bootstrap version of the Kotlin Stdlib
 */
dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion")
}