/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtCompilationFacility
import org.jetbrains.kotlin.analysis.api.components.KtCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KtCompilationTarget
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.psi.KtFile

internal class KtFe10CompilationFacility(override val analysisSession: KtAnalysisSession) : KtCompilationFacility() {
    override fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KtCompilationTarget,
        languageVersionSettings: LanguageVersionSettings
    ): KtCompilationResult {
        throw UnsupportedOperationException("Compilation API is not supported for K1")
    }
}