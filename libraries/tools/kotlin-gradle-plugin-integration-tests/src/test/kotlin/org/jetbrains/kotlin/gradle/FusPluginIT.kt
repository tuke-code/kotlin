/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import kotlin.io.path.pathString
import kotlin.streams.toList
import kotlin.test.assertTrue

@DisplayName("FUS statistic")
@JvmGradlePluginTests
class FusPluginIT : KGPBaseTest() {
    private val reportRelativePath = "reports"

    @DisplayName("smoke test for fus-statistics-gradle-plugin")
    @GradleTest
    fun smokeTestForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        val metricValue = 1
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                 ${applyFusPluginAndCreateTestFusTask(it)}
                
                 ${registerTaskAndReportMetric("test-fus", metricName, metricValue)}
               
                """.trimIndent()
            }

            build(
                "test-fus",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
            ) {
                validateFusReportFileContains(
                    gradleVersion,
                    "$metricName=$metricValue",
                    "BUILD FINISHED"
                )
            }
            projectPath.resolve(reportRelativePath).toFile().deleteRecursively()
        }
    }

    @DisplayName("with configuration cache and project isolation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0]
    )
    @GradleTest
    fun withConfigurationCacheAndProjectIsolation(gradleVersion: GradleVersion) {
        val executionTimeValue = "EXECUTION_METRIC_VALUE"
        val configurationTimeMetricName = "CONFIGURATION_METRIC_NAME"
        project(
            "incrementalMultiproject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
            )
        ) {
            listOf(subProject("lib"), subProject("app")).forEach { project ->
                project.buildGradle.modify {
                    """
                    ${applyFusPluginAndCreateTestFusTask(it)}
                    
                    ${registerTaskAndReportMetric("test-fus", project.projectName, executionTimeValue)}
                    
                    import org.jetbrains.kotlin.gradle.fus.ConfigurationMetricsKt
                    import org.jetbrains.kotlin.gradle.fus.Metric
                    import org.jetbrains.kotlin.gradle.fus.UniqueId
                    
                    use(ConfigurationMetricsKt) {
                        project.addGradleConfigurationPhaseMetric( { [ Metric.newInstance("$configurationTimeMetricName","${project.projectName}", UniqueId.@Companion.DEFAULT) ] } )
                    }
                """.trimIndent()
                }
            }

            build(
                "test-fus",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
            ) {
                assertConfigurationCacheStored()
                validateFusReportFileContains(
                    gradleVersion,
                    "app=$executionTimeValue",
                    "lib=$executionTimeValue",
                    "$configurationTimeMetricName=app",
                    "$configurationTimeMetricName=lib",
                    "BUILD FINISHED"
                )
            }

            build("clean")
            assertTrue { projectPath.resolve(reportRelativePath).toFile().deleteRecursively() }

            build(
                "test-fus",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
            ) {
                assertConfigurationCacheReused()
                validateFusReportFileContains(
                    gradleVersion,
                    "$configurationTimeMetricName=app",
                    "$configurationTimeMetricName=lib",
                    "BUILD FINISHED"
                )
            }
            projectPath.resolve(reportRelativePath).toFile().deleteRecursively()
        }
    }

    @DisplayName("test override metrics for fus-statistics-gradle-plugin")
    @GradleTest
    fun testMetricsOverrideForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${applyFusPluginAndCreateTestFusTask(it)}
                
                ${registerTaskAndReportMetric("test-fus", metricName, "1")}
                
                ${registerTaskAndReportMetric("test-fus-second", metricName, "2")}
            
                """.trimIndent()
            }

            build(
                "test-fus", "test-fus-second",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
            ) {
                //Metrics should not be overridden and both metrics should be in the file
                validateFusReportFileContains(
                    gradleVersion,
                    "Build: ",
                    "$metricName=1",
                    "$metricName=2",
                    "BUILD FINISHED"
                )
            }
            projectPath.resolve(reportRelativePath).toFile().deleteRecursively()
        }
    }

    @DisplayName("test invalid fus report directory")
    @GradleTest
    fun testInvalidFusReportDir(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${applyFusPluginAndCreateTestFusTask(it)}
                
                ${registerTaskAndReportMetric("test-fus", "metricName", "metricValue")}
                
                """.trimIndent()
            }

            build("test-fus", "-Pkotlin.session.logger.root.path=") {
                assertOutputContains("Fus metrics wont be collected")
            }
        }
    }

    @DisplayName("test configuration metrics with different classloaders")
    @GradleTest
    fun testConfigurationMetricsOnly(gradleVersion: GradleVersion) {
        project(
            "multiClassloaderProject",
            gradleVersion,
            defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED, logLevel = LogLevel.DEBUG),
            forceOutput = true
        ) {

            listOf("subProjectA", "subProjectB").forEach { subProject ->
                subProject(subProject).buildGradleKts.modify {
                    """
                        import org.jetbrains.kotlin.gradle.fus.addGradleConfigurationPhaseMetric
                        import org.jetbrains.kotlin.gradle.fus.Metric
                        import org.jetbrains.kotlin.gradle.fus.UniqueId
                        
                        ${applyFusStatisticPlugin(it)}
                        
                        project.addGradleConfigurationPhaseMetric { listOf(Metric("$subProject","value", UniqueId.DEFAULT)) }
                    """.trimIndent()
                }
            }

             build(
                "assemble",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
                enableGradleDebug = true
            ) {
                validateFusReportFileContains(
                    gradleVersion,
                    "subProjectA=value",
                    "subProjectB=value",
                    "BUILD FINISHED"
                )
            }

            projectPath.resolve(reportRelativePath).toFile().deleteRecursively()

            build(
                "assemble",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
            ) {
                validateFusReportFileContains(
                    gradleVersion,
                    "subProjectA=value",
                    "subProjectB=value",
                    "BUILD FINISHED"
                )
            }
            projectPath.resolve(reportRelativePath).toFile().deleteRecursively()
        }
    }

    private fun TestProject.validateFusReportFileContains(
        gradleVersion: GradleVersion,
        vararg expectedText: String,
    ) {
        // Since Gradle 8.2 FlowAction is used and a single FUS file is created.
        // For earlier versions [GradleBuildFusStatisticsService] creates a separate file
        if (gradleVersion < GradleVersion.version("8.2")) {
            assertFilesCombinedContains(
                Files.list(projectPath.resolve("$reportRelativePath/kotlin-profile")).toList(),
                *expectedText
            )
        } else {
            assertFileContains(
                projectPath.getSingleFileInDir("$reportRelativePath/kotlin-profile"),
                *expectedText
            )
        }
    }

    private fun addBuildScriptDependency() = """
        buildscript {
            dependencies {
                classpath "org.jetbrains.kotlin:fus-statistics-gradle-plugin:${'$'}kotlin_version"
            }
        }
    """.trimIndent()

    private fun applyFusStatisticPlugin(buildScript: String/*, apply: Boolean = true*/) = """
        $buildScript
        
        plugins.apply("org.jetbrains.kotlin.fus-statistics-gradle-plugin")
    """.trimIndent()

    private fun createTestFusTaskClass() = """
        import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
        import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService

        class TestFusTask extends DefaultTask implements UsesGradleBuildFusStatisticsService {

            private Property<GradleBuildFusStatisticsService> fusStatisticsBuildService = project.objects.property(GradleBuildFusStatisticsService.class)

            Property getFusStatisticsBuildService(){
                return fusStatisticsBuildService
            }

        }
    """.trimIndent()

    private fun applyFusPluginAndCreateTestFusTask(buildScript: String) = """
        ${addBuildScriptDependency()}    
                    
        ${applyFusStatisticPlugin(buildScript)}
                    
        ${createTestFusTaskClass()}
    """


    private fun registerTaskAndReportMetric(taskName: String, metricName: String, metricValue: Any) =
        """
            import org.jetbrains.kotlin.gradle.fus.TaskId
            tasks.register("$taskName", TestFusTask.class) {
                doLast {
                      fusStatisticsBuildService.get().reportMetric("$metricName", "$metricValue", TaskId.newInstance(null, "$taskName"))
                }
           }
           """

}