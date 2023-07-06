/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import java.io.File
import kotlin.io.path.appendText
import kotlin.io.path.writeText

@MppGradlePluginTests
class MppDiagnosticsIt : KGPBaseTest() {
    @GradleTest
    fun testDiagnosticsRenderingSmoke(gradleVersion: GradleVersion) {
        project("diagnosticsRenderingSmoke", gradleVersion) {
            build {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testDeprecatedMppProperties(gradleVersion: GradleVersion) {
        project("mppDeprecatedProperties", gradleVersion) {
            checkDeprecatedProperties(isDeprecationExpected = false)

            this.gradleProperties.appendText(
                defaultFlags.entries.joinToString(
                    prefix = System.lineSeparator(),
                    postfix = System.lineSeparator(),
                    separator = System.lineSeparator(),
                ) { (prop, value) -> "$prop=$value" }
            )
            checkDeprecatedProperties(isDeprecationExpected = true)

            // remove the MPP plugin from the top-level project and check the warnings are still reported in subproject
            this.buildGradleKts.writeText("")
            checkDeprecatedProperties(isDeprecationExpected = true)

            this.gradleProperties.appendText("kotlin.mpp.deprecatedProperties.nowarn=true${System.lineSeparator()}")
            checkDeprecatedProperties(isDeprecationExpected = false)
        }
    }

    @GradleTest
    fun testErrorDiagnosticBuildFails(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildFails", gradleVersion) {
            // 'assemble' (triggers compileKotlin-tasks indireectly): fail
            buildAndFail("assemble") {
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirVerboseDiagnostics())
            }

            // 'clean', not directly relevant to Kotlin tasks: build is OK
            build("clean") {
                assertEqualsToFile(expectedOutputFile("clean"), extractProjectsAndTheirVerboseDiagnostics())
            }

            // Custom task, irrelevant to Kotlin tasks: build is OK
            build("myTask", "--rerun-tasks") {
                assertEqualsToFile(expectedOutputFile("customTask"), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testErrorDiagnosticBuildSucceeds(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildSucceeds", gradleVersion) {
            build("assemble") {
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirVerboseDiagnostics())
            }
            build("myTask", "--rerun-tasks") {
                assertEqualsToFile(expectedOutputFile("customTask"), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    private fun TestProject.expectedOutputFile(suffix: String? = null): File {
        val suffixIfAny = if (suffix != null) "-$suffix" else ""
        return projectPath.resolve("expectedOutput$suffixIfAny.txt").toFile()
    }

    private fun TestProject.checkDeprecatedProperties(isDeprecationExpected: Boolean) {
        build {
            if (isDeprecationExpected)
                output.assertHasDiagnostic(KotlinToolingDiagnostics.HierarchicalMultiplatformFlagsWarning)
            else
                output.assertNoDiagnostic(KotlinToolingDiagnostics.HierarchicalMultiplatformFlagsWarning)
        }
    }

    private val defaultFlags: Map<String, String>
        get() = mapOf(
            "kotlin.mpp.enableGranularSourceSetsMetadata" to "true",
            "kotlin.mpp.enableCompatibilityMetadataVariant" to "false",
            "kotlin.internal.mpp.hierarchicalStructureByDefault" to "true",
            "kotlin.mpp.hierarchicalStructureSupport" to "true",
            "kotlin.native.enableDependencyPropagation" to "false",
        )
}
