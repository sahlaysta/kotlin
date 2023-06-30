/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.mpp.TypeAliasSymbolMarker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.RequireKotlinConstants

object AbstractExpectActualAnnotationMatchChecker {
    private val SKIPPED_FQ_NAMES = setOf(
        RequireKotlinConstants.FQ_NAME,
        StandardClassIds.Annotations.SinceKotlin.asSingleFqName(),
        StandardClassIds.Annotations.WasExperimental.asSingleFqName(),
        StandardNames.FqNames.deprecated,
        StandardNames.FqNames.deprecatedSinceKotlin,
        StandardNames.FqNames.optionalExpectation,
        StandardNames.FqNames.suppress,
    )

    class Incompatibility(val expectSymbol: DeclarationSymbolMarker, val actualSymbol: DeclarationSymbolMarker)

    fun areAnnotationsCompatible(
        expected: DeclarationSymbolMarker,
        actual: DeclarationSymbolMarker,
        context: ExpectActualMatchingContext<*>,
    ): Incompatibility? = with(context) { areAnnotationsCompatible(expected, actual) }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsCompatible(
        expected: DeclarationSymbolMarker,
        originalActual: DeclarationSymbolMarker,
    ): Incompatibility? {
        val actual = if (originalActual is TypeAliasSymbolMarker) {
            originalActual.expandToRegularClass() ?: return null
        } else {
            originalActual
        }
        // TODO(Roman.Efremov, KT-58551): properly handle repeatable annotations
        // TODO(Roman.Efremov, KT-58551): check other annotation targets (constructors, types, value parameters, etc)
        // TODO(Roman.Efremov, KT-58551): fix actual typealias class members not checked in FE checkers

        val skipSourceAnnotations = !actual.hasSource
        val actualAnnotations = actual.annotations
        for (expectAnnotation in expected.annotations) {
            if (expectAnnotation.fqName in SKIPPED_FQ_NAMES || expectAnnotation.isOptIn) {
                continue
            }
            if (expectAnnotation.isRetentionSource && skipSourceAnnotations) {
                continue
            }
            if (actualAnnotations.none { areAnnotationsMatch(expectAnnotation, it) }) {
                return Incompatibility(expected, actual)
            }
        }
        return null
    }


    context(ExpectActualMatchingContext<*>)
    private fun areAnnotationsMatch(
        expect: ExpectActualMatchingContext.AnnotationDelegate,
        actual: ExpectActualMatchingContext.AnnotationDelegate,
    ): Boolean {
        if (expect.fqName != actual.fqName) {
            return false
        }

        val allExplicitArgumentNames: Set<Name> = expect.allValueArgumentNames + actual.allValueArgumentNames
        return allExplicitArgumentNames.all { areArgumentsEqual(it, expect, actual) }
    }
}