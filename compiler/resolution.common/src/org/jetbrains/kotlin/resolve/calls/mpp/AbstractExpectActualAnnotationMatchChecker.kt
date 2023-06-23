/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.mpp.TypeAliasSymbolMarker
import org.jetbrains.kotlin.name.Name

object AbstractExpectActualAnnotationMatchChecker {
    fun areAnnotationsCompatible(
        expected: DeclarationSymbolMarker,
        actual: DeclarationSymbolMarker,
        context: ExpectActualMatchingContext<*>,
    ): Boolean = with(context) { areAnnotationsCompatible(expected, actual) }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsCompatible(
        expected: DeclarationSymbolMarker,
        originalActual: DeclarationSymbolMarker,
    ): Boolean {
        val actual = if (originalActual is TypeAliasSymbolMarker) {
            originalActual.expandToRegularClass() ?: return true
        } else {
            originalActual
        }
        // TODO(Roman.Efremov, KT-58551): properly handle repeatable annotations
        // TODO(Roman.Efremov, KT-58551): check other annotation targets (constructors, types, value parameters, etc)
        // TODO(Roman.Efremov, KT-58551): fix actual typealias class members not checked in FE checkers

        val skipSourceAnnotations = !actual.hasSource
        val actualAnnotations = actual.annotations
        for (expectAnnotation in expected.annotations) {
            if (expectAnnotation.fqName == StandardNames.FqNames.optionalExpectation) {
                continue
            }
            if (expectAnnotation.isRetentionSource && skipSourceAnnotations) {
                continue
            }
            if (actualAnnotations.none { areAnnotationsMatch(expectAnnotation, it) }) {
                return false
            }
        }
        return true
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