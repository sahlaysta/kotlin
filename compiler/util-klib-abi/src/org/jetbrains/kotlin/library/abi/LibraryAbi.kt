/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME

/**
 * @property manifest Information from manifest that might be useful.
 * @property uniqueName Library unique name that is a part of library ABI. Corresponds to [KLIB_PROPERTY_UNIQUE_NAME] manifest property.
 * @property supportedSignatureVersions The versions of signatures supported by the given KLIB.
 * @property topLevelDeclarations Top-level declarations.
 */
@ExperimentalLibraryAbiReader
class LibraryAbi(
    val manifest: LibraryManifest,
    val uniqueName: String,
    val supportedSignatureVersions: Set<AbiSignatureVersion>,
    val topLevelDeclarations: AbiTopLevelDeclarations
)

@ExperimentalLibraryAbiReader
enum class AbiSignatureVersion(val alias: String) {
    /**
     *  The signatures with hashes.
     */
    V1("1"),

    /**
     * The self-descriptive signatures (with mangled names).
     */
    V2("2"),
}

@ExperimentalLibraryAbiReader
interface AbiSignatures {
    /** Returns the signature of the specified [AbiSignatureVersion] **/
    operator fun get(signatureVersion: AbiSignatureVersion): String?
}

/**
 * Simple (unqualified) name.
 * Examples: "TopLevelClass", "topLevelFun", "List", "EMPTY".
 */
@ExperimentalLibraryAbiReader
@JvmInline
value class AbiSimpleName(val value: String) {
    init {
        require(value.isNotEmpty()) { "Empty simple name" }
        require(value.none { ch -> ch == AbiDottedName.SEPARATOR || ch == AbiQualifiedName.SEPARATOR }) {
            "Simple name contains illegal characters: $value"
        }
    }

    override fun toString() = value
}

/**
 * Dotted name. An equivalent of one or more [AbiSimpleName] which are concatenated with dots.
 * Examples: "TopLevelClass", "topLevelFun", "List", "CharRange.Companion.EMPTY".
 */
@ExperimentalLibraryAbiReader
@JvmInline
value class AbiDottedName(val value: String) {
    init {
        require(AbiQualifiedName.SEPARATOR !in value) { "Dotted name contains illegal characters: $value" }
    }

    override fun toString() = value

    companion object {
        const val SEPARATOR = '.'
    }
}


/**
 * Fully qualified name.
 * Examples: "/TopLevelClass", "/topLevelFun", "kotlin.collections/List", "kotlin.ranges/CharRange.Companion.EMPTY".
 */
@ExperimentalLibraryAbiReader
data class AbiQualifiedName(val packageName: AbiDottedName, val relativeName: AbiDottedName) {
    init {
        require(relativeName.value.isNotEmpty()) { "Empty relative name" }
    }

    override fun toString() = "$packageName$SEPARATOR$relativeName"

    companion object {
        const val SEPARATOR = '/'
    }
}

@ExperimentalLibraryAbiReader
sealed interface AbiDeclaration {
    val name: AbiSimpleName
    val signatures: AbiSignatures
}

@ExperimentalLibraryAbiReader
sealed interface AbiPossiblyTopLevelDeclaration : AbiDeclaration {
    val modality: AbiModality
}

@ExperimentalLibraryAbiReader
enum class AbiModality {
    FINAL, OPEN, ABSTRACT, SEALED
}

/**
 * Important: The order of [declarations] is preserved exactly as in serialized IR.
 */
@ExperimentalLibraryAbiReader
interface AbiDeclarationContainer {
    val declarations: List<AbiDeclaration>
}

@ExperimentalLibraryAbiReader
interface AbiTopLevelDeclarations : AbiDeclarationContainer

/**
 * Important: The order of [superTypes] is preserved exactly as in serialized IR.
 */
@ExperimentalLibraryAbiReader
interface AbiClass : AbiPossiblyTopLevelDeclaration, AbiDeclarationContainer {
    val kind: AbiClassKind
    val isInner: Boolean
    val isValue: Boolean
    val isFunction: Boolean

    /** The set of non-trivial supertypes (i.e. excluding [kotlin.Any]). */
    val superTypes: List<AbiType>
}

@ExperimentalLibraryAbiReader
enum class AbiClassKind {
    CLASS, INTERFACE, OBJECT, ENUM_CLASS, ANNOTATION_CLASS
}

@ExperimentalLibraryAbiReader
interface AbiEnumEntry : AbiDeclaration

@ExperimentalLibraryAbiReader
interface AbiFunction : AbiPossiblyTopLevelDeclaration {
    val isConstructor: Boolean
    val isInline: Boolean
    val isSuspend: Boolean

    /**
     * Additional value parameter flags that might affect binary compatibility and that should be rendered
     * along with the function itself.
     *
     * Important: Only regular value parameters are included in [valueParameters]. The extension receiver and
     * context receivers, which formally are also value parameters, are not included in [valueParameters].
     * That's because none of the existing flags in [AbiValueParameter] such as [AbiValueParameter.hasDefaultArg]
     * or [AbiValueParameter.isNoinline] is relevant for extension and context receivers.
     */
    val valueParameters: List<AbiValueParameter>
}

@ExperimentalLibraryAbiReader
interface AbiValueParameter {
    val hasDefaultArg: Boolean
    val isNoinline: Boolean
    val isCrossinline: Boolean
}

@ExperimentalLibraryAbiReader
interface AbiProperty : AbiPossiblyTopLevelDeclaration {
    val kind: AbiPropertyKind
    val getter: AbiFunction?
    val setter: AbiFunction?
}

@ExperimentalLibraryAbiReader
enum class AbiPropertyKind { VAL, CONST_VAL, VAR }

@ExperimentalLibraryAbiReader
sealed interface AbiType {
    interface Dynamic : AbiType
    interface Error : AbiType
    interface Simple : AbiType {
        val classifier: AbiClassifier
        val arguments: List<AbiTypeArgument>
        val nullability: AbiTypeNullability
    }
}

@ExperimentalLibraryAbiReader
sealed interface AbiTypeArgument {
    interface StarProjection : AbiTypeArgument
    interface RegularProjection : AbiTypeArgument {
        val type: AbiType
        val projectionKind: AbiVariance
    }
}

@ExperimentalLibraryAbiReader
sealed interface AbiClassifier {
    interface Class : AbiClassifier {
        val className: AbiQualifiedName
    }

    interface TypeParameter : AbiClassifier {
        val declaringClassName: AbiQualifiedName
        val index: Int
    }
}

@ExperimentalLibraryAbiReader
enum class AbiTypeNullability {
    MARKED_NULLABLE, NOT_SPECIFIED, DEFINITELY_NOT_NULL
}

@ExperimentalLibraryAbiReader
enum class AbiVariance {
    INVARIANT, IN_VARIANCE, OUT_VARIANCE
}
