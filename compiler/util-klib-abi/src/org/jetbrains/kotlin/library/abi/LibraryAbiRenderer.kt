/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.AbiRendererImpl

/**
 * The default rendering implementation.
 */
@ExperimentalLibraryAbiReader
fun LibraryAbi.render(settings: AbiRenderingSettings): String =
    buildString { render(settings, this) }

/**
 * The default rendering implementation.
 */
@ExperimentalLibraryAbiReader
fun LibraryAbi.render(settings: AbiRenderingSettings, output: Appendable) {
    AbiRendererImpl(this, settings).renderTo(output)
}

/**
 * @param renderedSignatureVersions One might want to render only some signatures, e.g.
 *   only [AbiSignatureVersion.V2] even if [AbiSignatureVersion.V1] are available.
 * @param renderManifest Whether KLIB manifest properties should be rendered.
 * @param renderDeclarations Whether declarations should be rendered.
 */
@ExperimentalLibraryAbiReader
class AbiRenderingSettings(
    renderedSignatureVersions: Set<AbiSignatureVersion>,
    val renderManifest: Boolean = false,
    val renderDeclarations: Boolean = true,
    val whenSignatureNotFound: (AbiDeclaration, AbiSignatureVersion) -> String = { declaration, signatureVersion ->
        error("No signature $signatureVersion for ${declaration::class.java}, $declaration")
    }
) {
    init {
        require(renderedSignatureVersions.isNotEmpty())
    }

    val renderedSignatureVersions: List<AbiSignatureVersion> =
        renderedSignatureVersions.sortedDescending() // The latest version always goes first.
}
