/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.psi.KtFile

/**
 * Result of compilation (not necessarily successful).
 *
 * @property outputFiles Output files produced by the compiler.
 *  For the JVM target, these are class files and '.kotlin_module'.
 *  [outputFiles] might be empty in the presence of compilation errors.
 *
 * @property errors Compilation errors.
 */
public class KtCompilationResult(
    public val outputFiles: List<OutputFile>,
    public val errors: List<KtPsiDiagnostic>
) {
    public val hasErrors: Boolean
        get() = errors.isNotEmpty()
}

/**
 * Compilation target platform.
 */
public sealed class KtCompilationTarget {
    /** JVM target (produces '.class' files). */
    public class Jvm(public val classBuilderFactory: ClassBuilderFactory) : KtCompilationTarget()
}

public abstract class KtCompilationFacility : KtAnalysisSessionComponent() {
    public abstract fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KtCompilationTarget,
        languageVersionSettings: LanguageVersionSettings,
    ): KtCompilationResult
}

public interface KtCompilationFacilityMixIn : KtAnalysisSessionMixIn {
    /**
     * Compile the given [file].
     */
    public fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KtCompilationTarget,
        languageVersionSettings: LanguageVersionSettings,
    ): KtCompilationResult = withValidityAssertion {
        analysisSession.compilationFacility.compile(file, configuration, target, languageVersionSettings)
    }
}