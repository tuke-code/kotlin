/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal


import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.Metric
import org.jetbrains.kotlin.gradle.fus.UniqueId
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

abstract class InternalGradleBuildFusStatisticsService :
    GradleBuildFusStatisticsService<InternalGradleBuildFusStatisticsService.Parameters> {
    interface Parameters : BuildServiceParameters {
        val fusStatisticsRootDirPath: Property<String>
        val configurationMetrics: ListProperty<Metric>
        val fusStatisticIsEnabled: Property<Boolean>
        val useBuildFinishFlowAction: Property<Boolean>
        val buildUidService: Property<BuildUidService>
    }

    private val metrics = ConcurrentLinkedQueue<Metric>()
    private val log = Logging.getLogger(this.javaClass)

    //It is not possible to rely on BuildUidService in close() method,
    // so the buildId field is used for Gradle versions less than 8.2
    // for older versions [BuildFinishFlowAction] is used
    private val buildId = parameters.buildUidService.get().buildId

    init {
        log.debug("InternalGradleBuildFusStatisticsService is initialized for $buildId build")
    }

    //since Gradle
    override fun close() {
        log.debug("InternalGradleBuildFusStatisticsService is closed for $buildId build")

        //since Gradle 8.1 flow action [BuildFinishFlowAction] is used to collect all metrics and write them down in a single file
        if (parameters.useBuildFinishFlowAction.get()) {
            return
        }
        val reportDir = File(parameters.fusStatisticsRootDirPath.get(), STATISTICS_FOLDER_NAME)
        try {
            Files.createDirectories(reportDir.toPath())
        } catch (e: Exception) {
            log.warn("Failed to create directory '$reportDir' for FUS report. FUS report won't be created", e)
            return
        }
        val reportFile = reportDir.resolve(UUID.randomUUID().toString() + PROFILE_FILE_NAME_SUFFIX)
        reportFile.createNewFile()

        FileOutputStream(reportFile, true).bufferedWriter().use {
            it.appendLine("Build: $buildId")
            getAllReportedMetrics().forEach { reportedMetrics ->
                it.appendLine("$reportedMetrics")
            }
            it.appendLine(BUILD_SESSION_SEPARATOR)
        }
    }

    /**
     * Returns a list of collected metrics sets.
     *
     *
     * These sets are not going to be merged into one as no aggregation information is present here.
     * Non-thread safe
     */
    fun getAllReportedMetrics(): List<Metric> {
        val reportedMetrics = ArrayList<Metric>()
        parameters.configurationMetrics.orNull?.also { reportedMetrics.addAll(it) }
        reportedMetrics.addAll(metrics)
        return reportedMetrics
    }

    override fun reportMetric(name: String, value: Boolean, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    override fun reportMetric(name: String, value: String, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    override fun reportMetric(name: String, value: Number, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    private fun internalReportMetric(name: String, value: Any, uniqueId: UniqueId) {
        //all aggregations should be done on IDEA side
        metrics.add(Metric(name, value, uniqueId))
    }

    companion object {
        private const val STATISTICS_FOLDER_NAME = "kotlin-profile"
        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"
        private const val PROFILE_FILE_NAME_SUFFIX = ".profile"
    }
}