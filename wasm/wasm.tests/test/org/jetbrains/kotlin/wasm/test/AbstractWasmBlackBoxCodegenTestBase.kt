/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibInterpreterDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.WasmIrInterpreterDumpHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.commonClassicFrontendHandlersForCodegenTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasiBoxRunner

abstract class AbstractWasmBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>, A : ResultingArtifact.Binary<A>>(
    private val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, A>>
    abstract val afterBackendFacade: Constructor<AbstractTestFacade<A, BinaryArtifacts.Wasm>>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = WasmPlatforms.Default
            dependencyKind = DependencyKind.Binary
        }

        val pathToRootOutputDir = System.getProperty("kotlin.wasm.test.root.out.dir") ?: error("'kotlin.wasm.test.root.out.dir' is not set")
        defaultDirectives {
            +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
            WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
            WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
        }

        forTestsNotMatching("compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*") {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        forTestsNotMatching("compiler/testData/codegen/boxError/*") {
            enableMetaInfoHandler()
        }

        useConfigurators(
            ::WasmEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::WasmAdditionalSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useAdditionalService(::LibraryProvider)

        useAfterAnalysisCheckers(
            ::WasmFailingTestSuppressor,
            ::BlackBoxCodegenSuppressor,
        )

        facadeStep(frontendFacade)
        classicFrontendHandlersStep {
            commonClassicFrontendHandlersForCodegenTest()
            useHandlers(::ClassicDiagnosticsHandler)
        }

        firHandlersStep {
            useHandlers(::FirDiagnosticsHandler)
        }

        facadeStep(frontendToBackendConverter)
        irHandlersStep()

        facadeStep(backendFacade)
        klibArtifactsHandlersStep()
        facadeStep(afterBackendFacade)

        forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/*") {
            enableMetaInfoHandler()
            configureKlibArtifactsHandlersStep {
                useHandlers(::KlibInterpreterDumpHandler)
            }
            configureWasmArtifactsHandlersStep {
                useHandlers(::WasmIrInterpreterDumpHandler)
            }
        }

        wasmArtifactsHandlersStep {
//            useHandlers(::WasmBoxRunner)
            useHandlers(::WasiBoxRunner)
        }
    }
}
