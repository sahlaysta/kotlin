/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File

@OptIn(ExperimentalLibraryAbiReader::class)
abstract class AbstractLibraryAbiReaderTest {
    private lateinit var testName: String
    private lateinit var buildDir: File

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        testName = getTestName(testInfo)
        buildDir = setUpBuildDir(testInfo)
    }

    fun runTest(relativePath: String) {
        val (sourceFile, dumpFile) = computeTestFiles(relativePath, "v1.txt")

        val library = buildLibrary(sourceFile, libraryName = testName, buildDir)
        val libraryAbi = LibraryAbiReader.readAbiInfo(library)

        val abiDump = libraryAbi.render(
            AbiRenderingSettings(renderedSignatureVersions = setOf(AbiSignatureVersion.V1))
        )

        assertEqualsToFile(dumpFile, abiDump)
    }
}
