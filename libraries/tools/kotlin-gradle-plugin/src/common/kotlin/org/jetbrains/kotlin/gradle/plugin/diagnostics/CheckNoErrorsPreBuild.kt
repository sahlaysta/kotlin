/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.withType

internal abstract class CheckNoErrorsPreBuild : DefaultTask() {
    @get:Internal
    abstract val diagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>

    @TaskAction
    fun checkNoErrors() {
        if (diagnosticsCollector.get().hasErrors()) {
            throw InvalidUserCodeException("Kotlin Gradle Plugin reported errors. Check the log for details")
        }
    }

    companion object {
        private const val TASK_NAME = "ensureNoKotlinGradlePluginErrors"

        internal fun register(tasks: TaskContainer, kotlinToolingDiagnosticsCollector: Provider<KotlinToolingDiagnosticsCollector>) {
            val ensureNoErrorsTask: CheckNoErrorsPreBuild = tasks.create(TASK_NAME, CheckNoErrorsPreBuild::class.java) {
                it.usesService(kotlinToolingDiagnosticsCollector)
                it.diagnosticsCollector.value(kotlinToolingDiagnosticsCollector)
            }

            ensureNoErrorsTask.addDependsOnFromTasksThatShouldFailWithErrors(tasks)
        }

        /**
         * Adds dependsOn from some selection of the [tasks] to the [this]-task, effectively causing them to fail
         * if the ERROR-diagnostics were reported.
         *
         * Currently, we're doing it conservatively and instrumenting only [KotlinCompileTool]-tasks.
         * The intuition here is that if the build manages to do something useful for a user without compiling any .kt-sources,
         * then it's OK for KGP to let that build pass even if it reported ERROR-diagnostics.
         * This can happen when the project configuration is so broken that no kotlin-tasks are even registered
         * (specific example: projects that cause
         * [org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.NoKotlinTargetsDeclared])
         *
         * TODO: consider 'allMetadataJar', 'generateProjectStructureMetadata'
         */
        private fun CheckNoErrorsPreBuild.addDependsOnFromTasksThatShouldFailWithErrors(tasks: TaskContainer) {
            tasks.withType<KotlinCompileTool>().all { it.dependsOn(this) }
        }
    }
}