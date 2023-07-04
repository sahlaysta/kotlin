/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.*
import org.jetbrains.kotlin.library.abi.impl.ClassImpl
import org.jetbrains.kotlin.library.abi.impl.RegularProjectionImpl
import org.jetbrains.kotlin.library.abi.impl.SimpleTypeImpl
import org.jetbrains.kotlin.library.abi.impl.StarProjectionImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalLibraryAbiReader
class AbiTypeArgumentRenderingTest {
    @Test
    fun test() {
        val mockLibraryAbi = mockLibraryAbi(
            mockClass(
                name = "FinalClass",
                mockType(
                    "sample/OpenClass",
                    mockType("sample/InvariantClass") to AbiVariance.INVARIANT,
                    mockType("sample/InClass") to AbiVariance.IN_VARIANCE,
                    mockType("sample/OutClass") to AbiVariance.OUT_VARIANCE,
                    null
                )
            )
        )

        val renderedClass = mockLibraryAbi.render(AbiRenderingSettings(setOf(AbiSignatureVersion.V1)))
            .lineSequence()
            .filter(String::isNotBlank)
            .last()

        assertEquals(
            "final class sample/FinalClass|null[0] : sample/OpenClass<sample/InvariantClass, in sample/InClass, out sample/OutClass, *>",
            renderedClass
        )
    }

    private fun mockLibraryAbi(vararg declarations: AbiPossiblyTopLevelDeclaration): LibraryAbi =
        LibraryAbi(
            manifest = LibraryManifest(null, emptyList(), null, null, null, null),
            uniqueName = "type-argument-rendering-test",
            supportedSignatureVersions = setOf(AbiSignatureVersion.V1),
            topLevelDeclarations = object : AbiTopLevelDeclarations {
                override val declarations = declarations.toList()
            }
        )

    private fun mockClass(name: String, vararg superTypes: AbiType): AbiClass =
        AbiClassImpl(
            name = name,
            signatures = AbiSignaturesImpl("sample/$name|null[0]", null),
            modality = AbiModality.FINAL,
            kind = AbiClassKind.CLASS,
            isInner = false,
            isValue = false,
            isFunction = false,
            superTypes = superTypes.toList(),
            declarations = emptyList()
        )

    private fun mockType(className: String, vararg arguments: Pair<AbiType, AbiVariance>?): AbiType =
        SimpleTypeImpl(
            classifier = ClassImpl(className),
            arguments = arguments.map { argument ->
                if (argument == null)
                    StarProjectionImpl
                else
                    RegularProjectionImpl(argument.first, argument.second)
            },
            nullability = AbiTypeNullability.NOT_SPECIFIED
        )
}