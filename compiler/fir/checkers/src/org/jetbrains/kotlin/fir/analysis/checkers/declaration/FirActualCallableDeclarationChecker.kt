/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.getSingleExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.areCompatibleExpectActualTypes
import org.jetbrains.kotlin.fir.types.createExpectActualTypeParameterSubstitutor
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

object FirActualCallableDeclarationChecker : FirCallableDeclarationChecker() {
    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isActual) return

        if (declaration is FirFunction) {
            checkActualFunctionWithDefaultArguments(declaration, reporter, context)
        }
        checkReturnTypes(declaration, context, reporter)
    }

    private fun checkActualFunctionWithDefaultArguments(function: FirFunction, reporter: DiagnosticReporter, context: CheckerContext) {
        for (valueParameter in function.valueParameters) {
            if (valueParameter.defaultValue != null) {
                reporter.reportOn(valueParameter.source, FirErrors.ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS, context)
            }
        }
    }

    private fun checkReturnTypes(callableDeclaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val actualFunctionSymbol = callableDeclaration.symbol
        val expectFunctionSymbol = actualFunctionSymbol.getSingleExpectForActualOrNull() as? FirCallableSymbol ?: return

        val expectTypeParameters = expectFunctionSymbol.getContainingClassSymbol(expectFunctionSymbol.moduleData.session)
            ?.typeParameterSymbols.orEmpty()
        val actualClassTypeParameters = actualFunctionSymbol.getContainingClassSymbol(context.session)?.typeParameterSymbols.orEmpty()
        val parentSubstitutor =
            createExpectActualTypeParameterSubstitutor(expectTypeParameters, actualClassTypeParameters, context.session)

        val substitutor = createExpectActualTypeParameterSubstitutor(
            expectFunctionSymbol.typeParameterSymbols,
            actualFunctionSymbol.typeParameterSymbols,
            context.session,
            parentSubstitutor
        )

        if (!areCompatibleExpectActualTypes(
                expectFunctionSymbol.resolvedReturnType.type,
                substitutor.substituteOrSelf(actualFunctionSymbol.resolvedReturnType.type),
                context.session
            )
        ) {
            reporter.reportOn(
                callableDeclaration.source,
                FirErrors.ACTUAL_WITHOUT_EXPECT,
                actualFunctionSymbol,
                actualFunctionSymbol.expectForActual as Map<ExpectActualCompatibility<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>,
                context
            )
        }
    }
}
