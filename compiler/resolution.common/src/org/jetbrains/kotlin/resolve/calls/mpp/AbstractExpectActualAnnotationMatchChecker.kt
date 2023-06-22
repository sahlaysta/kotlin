/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

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

        val actualAnnotations = actual.annotations
        for (expectAnnotation in expected.annotations) {
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