/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.components.KtCompilationFacility
import org.jetbrains.kotlin.analysis.api.components.KtCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KtCompilationTarget
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.FileDependencyCollectorVisitor
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.pipeline.signatureComposerForJvmFir2Ir
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

internal class KtFirCompilationFacility(override val analysisSession: KtFirAnalysisSession) : KtCompilationFacility() {
    private companion object {
        private val IGNORED_ERRORS = setOf(
            FirErrors.INVISIBLE_REFERENCE,
            FirErrors.INVISIBLE_SETTER,
            FirErrors.DEPRECATION_ERROR,
            FirErrors.DIVISION_BY_ZERO,
            FirErrors.OPT_IN_USAGE_ERROR,
            FirErrors.OPT_IN_OVERRIDE_ERROR,
            FirErrors.UNSAFE_CALL,
            FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL,
            FirErrors.UNSAFE_INFIX_CALL,
            FirErrors.UNSAFE_OPERATOR_CALL,
            FirErrors.ITERATOR_ON_NULLABLE,
            FirErrors.UNEXPECTED_SAFE_CALL,
            FirErrors.DSL_SCOPE_VIOLATION,
        )
    }

    private val project: Project
        get() = analysisSession.project

    private val resolveSession: LLFirResolveSession
        get() = analysisSession.firResolveSession

    private val useSiteSession: LLFirResolvableModuleSession
        get() = analysisSession.useSiteSession as LLFirResolvableModuleSession

    override fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KtCompilationTarget,
        languageVersionSettings: LanguageVersionSettings
    ): KtCompilationResult {
        val classBuilderFactory = when (target) {
            is KtCompilationTarget.Jvm -> target.classBuilderFactory
            else -> throw UnsupportedOperationException("Unsupported compilation target: $target")
        }

        val effectiveConfiguration = configuration
            .copy()
            .apply { put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true) }

        val scopeSession = useSiteSession.getScopeSession()

        val mainFirFile = file.getOrBuildFirFile(resolveSession)
        val dependencyCollector = FileDependencyCollectorVisitor().apply { process(mainFirFile) }

        val filesToCompile = dependencyCollector.files

        val firFilesToCompile = filesToCompile
            .map { it.getOrBuildFirFile(resolveSession) }
            .onEach { it.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE) }

        val diagnostics = file.collectDiagnosticsForFile(resolveSession, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        if (diagnostics.any { it.severity == Severity.ERROR && it.factory !in IGNORED_ERRORS }) {
            return KtCompilationResult(listOf(), diagnostics.toList())
        }

        val generateClassFilter = SingleFileGenerateClassFilter(file, dependencyCollector.inlinedClasses)
        val fir2IrExtensions = JvmFir2IrExtensions(effectiveConfiguration, JvmIrDeserializerImpl(), JvmIrMangler)

        val fir2IrConfiguration = Fir2IrConfiguration(
            languageVersionSettings,
            linkViaSignatures = false,
            effectiveConfiguration[CommonConfigurationKeys.EVALUATED_CONST_TRACKER] ?: EvaluatedConstTracker.create(),
            effectiveConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER]
        )

        val irGenerationExtensions = IrGenerationExtension.getInstances(project)

        val fir2IrResult = Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
            useSiteSession,
            scopeSession,
            firFilesToCompile,
            fir2IrExtensions,
            fir2IrConfiguration,
            JvmIrMangler,
            IrFactoryImpl,
            FirJvmVisibilityConverter,
            Fir2IrJvmSpecialAnnotationSymbolProvider(),
            irGenerationExtensions,
            DefaultBuiltIns.Instance,
            Fir2IrCommonMemberStorage(signatureComposerForJvmFir2Ir(false), FirJvmKotlinMangler()),
            initializedIrBuiltIns = null
        )

        ProgressManager.checkCanceled()

        val bindingContext = NoScopeRecordCliBindingTrace().bindingContext
        val codegenFactory = createJvmIrCodegenFactory(effectiveConfiguration)

        val generationState = GenerationState.Builder(
            project,
            classBuilderFactory,
            fir2IrResult.irModuleFragment.descriptor,
            bindingContext,
            filesToCompile,
            effectiveConfiguration,
        ).generateDeclaredClassFilter(generateClassFilter)
            .codegenFactory(codegenFactory)
            .build()

        try {
            generationState.beforeCompile()

            ProgressManager.checkCanceled()

            codegenFactory.generateModuleInFrontendIRMode(
                generationState,
                fir2IrResult.irModuleFragment,
                fir2IrResult.components.symbolTable,
                fir2IrResult.components.irProviders,
                JvmFir2IrExtensions(effectiveConfiguration, JvmIrDeserializerImpl(), JvmIrMangler),
                FirJvmBackendExtension(fir2IrResult.components, null),
                fir2IrResult.pluginContext
            )

            CodegenFactory.doCheckCancelled(generationState)
            generationState.factory.done()

            val outputFiles = generationState.factory.asList()

            val backendDiagnostics = buildList {
                for (diagnostic in generationState.collectedExtraJvmDiagnostics.all()) {
                    if (diagnostic.severity == Severity.ERROR) {
                        add(diagnostic as KtPsiDiagnostic)
                    }
                }
            }

            return KtCompilationResult(outputFiles, backendDiagnostics)
        } finally {
            generationState.destroy()
        }
    }

    private class SingleFileGenerateClassFilter(
        private val file: KtFile,
        private val inlinedClasses: Set<KtClassOrObject>
    ) : GenerationState.GenerateClassFilter() {
        private val filesWithInlinedClasses = inlinedClasses.mapTo(mutableSetOf()) { it.containingKtFile }

        override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
            return file === ktFile || ktFile in filesWithInlinedClasses
        }

        override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean {
            return true
        }

        override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
            return processingClassOrObject.containingKtFile === file ||
                    processingClassOrObject is KtObjectDeclaration && processingClassOrObject in inlinedClasses
        }

        override fun shouldGenerateScript(script: KtScript): Boolean {
            return script.containingKtFile === file
        }

        override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
    }

    private fun createJvmIrCodegenFactory(configuration: CompilerConfiguration): JvmIrCodegenFactory {
        val jvmGeneratorExtensions = object : JvmGeneratorExtensionsImpl(configuration) {
            override fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? {
                // Stubbed top-level function IR symbols (from other source files in the module) require a parent facade class to be
                // generated, which requires a container source to be provided. Without a facade class, function IR symbols will have
                // an `IrExternalPackageFragment` parent, which trips up code generation during IR lowering.
                val psiSourceFile =
                    descriptor.toSourceElement.containingFile as? PsiSourceFile ?: return super.getContainerSource(descriptor)
                return FacadeClassSourceShimForFragmentCompilation(psiSourceFile)
            }
        }

        val ideCodegenSettings = JvmIrCodegenFactory.IdeCodegenSettings(
            shouldStubAndNotLinkUnboundSymbols = true,
            shouldDeduplicateBuiltInSymbols = true,

            // Because the file to compile may be contained in a "common" multiplatform module, an `expect` declaration doesn't necessarily
            // have an obvious associated `actual` symbol. `shouldStubOrphanedExpectSymbols` generates stubs for such `expect` declarations.
            shouldStubOrphanedExpectSymbols = true,

            // Likewise, the file to compile may be contained in a "platform" multiplatform module, where the `actual` declaration is
            // referenced in the symbol table automatically, but not its `expect` counterpart, because it isn't contained in the files to
            // compile. `shouldReferenceUndiscoveredExpectSymbols` references such `expect` symbols in the symbol table so that they can
            // subsequently be stubbed.
            shouldReferenceUndiscoveredExpectSymbols = false, // TODO it was true
        )

        return JvmIrCodegenFactory(
            configuration,
            PhaseConfig(jvmPhases),
            jvmGeneratorExtensions = jvmGeneratorExtensions,
            ideCodegenSettings = ideCodegenSettings,
        )
    }
}