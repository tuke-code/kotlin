/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceParameters
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService

private const val statisticsIsEnabled: Boolean = true //KT-59629 Wait for user confirmation before start to collect metrics
private const val FUS_STATISTICS_PATH = "kotlin.session.logger.root.path"
private val serviceClass = GradleBuildFusStatisticsService::class.java
internal val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"
private val log = Logging.getLogger(GradleBuildFusStatisticsService::class.java)

fun registerGradleBuildFusStatisticsServiceIfAbsent(project: Project, uidService: Provider<BuildUidService>): Provider<out GradleBuildFusStatisticsService<out BuildServiceParameters>> {
    return registerIfAbsent(project, uidService).also { service ->
        project.tasks.withType(UsesGradleBuildFusStatisticsService::class.java).configureEach { task ->
            task.fusStatisticsBuildService.value(service).disallowChanges()
            task.usesService(service)
        }
    }
}

private fun registerIfAbsent(project: Project, uidService: Provider<BuildUidService>): Provider<out GradleBuildFusStatisticsService<out BuildServiceParameters>> {
    project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<GradleBuildFusStatisticsService<out BuildServiceParameters>>
    }
    val customPath: String =
        project.providers.gradleProperty(FUS_STATISTICS_PATH).orNull ?: project.gradle.gradleUserHomeDir.path


    return (if (!statisticsIsEnabled || customPath.isBlank()) {
        log.info(
            "Fus metrics wont be collected as statistic was " +
                    (if (statisticsIsEnabled) "enabled" else "disabled") +
                    if (customPath.isBlank()) " and custom path is blank" else ""
        )
        project.gradle.sharedServices.registerIfAbsent(serviceName, NoConsentGradleBuildFusService::class.java) {}
    } else {
        project.gradle.sharedServices.registerIfAbsent(serviceName, InternalGradleBuildFusStatisticsService::class.java) {
            it.parameters.fusStatisticsRootDirPath.set(customPath)
            it.parameters.fusStatisticsRootDirPath.disallowChanges()
            it.parameters.fusStatisticIsEnabled.set(statisticsIsEnabled)
            it.parameters.fusStatisticIsEnabled.disallowChanges()
            it.parameters.configurationMetrics.empty()
            it.parameters.useBuildFinishFlowAction.set(GradleVersion.current().baseVersion >= GradleVersion.version("8.1"))
            it.parameters.buildUidService.set(uidService)
            it.parameters.buildUidService.disallowChanges()
        }
    })
}

