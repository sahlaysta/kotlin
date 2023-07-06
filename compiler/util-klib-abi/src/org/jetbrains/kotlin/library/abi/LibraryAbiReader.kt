/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.LibraryAbiReaderImpl
import java.io.File

@ExperimentalLibraryAbiReader
object LibraryAbiReader {
    /**
     * Inspect the KLIB at [library]. The KLIB can be either in a directory (unzipped) or in a file (zipped) form.
     */
    fun readAbiInfo(library: File, settings: AbiReadingSettings): LibraryAbi = LibraryAbiReaderImpl(library, settings).readAbi()
}

/**
 * @property excludedPackages Packages that are not read from library even if they contain publicly visible ABI.
 * @property excludedClasses Classes (and objects and interfaces too) that are not read from library even if they
 *   contain publicly visible ABI.
 * @property nonPublicMarkers Annotations that mark ABI as publicly invisible (internal).
 */
@ExperimentalLibraryAbiReader
class AbiReadingSettings {
    val excludedPackages: MutableSet<AbiDottedName> = hashSetOf()
    val excludedClasses: MutableSet<AbiQualifiedName> = hashSetOf()
    val nonPublicMarkers: MutableSet<AbiQualifiedName> = hashSetOf()
}
