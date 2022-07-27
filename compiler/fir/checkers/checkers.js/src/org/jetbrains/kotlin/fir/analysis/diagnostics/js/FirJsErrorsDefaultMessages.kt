/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL
import org.jetbrains.kotlin.fir.analysis.diagnostics.checkMissingMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_ANONYMOUS_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_BUILTIN_NAME_CLASH
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_NOT_SUPPORTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_JS_QUALIFIER

@Suppress("unused")
object FirJsErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(WRONG_JS_QUALIFIER, "Qualifier contains illegal characters")
        map.put(JS_MODULE_PROHIBITED_ON_VAR, "@JsModule and @JsNonModule annotations prohibited for 'var' declarations. Use 'val' instead.")
        map.put(JS_BUILTIN_NAME_CLASH, "JavaScript name generated for this declaration clashes with built-in declaration {1}", STRING)
        map.put(IMPLEMENTING_FUNCTION_INTERFACE, "Implementing function interface is prohibited in JavaScript")
        map.put(OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS, "Overriding `external` function with optional parameters")
        map.put(
            OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
            "Overriding `external` function with optional parameters by declaration from superclass: {0}",
            SYMBOL
        )
        map.put(CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION, "This property can only be used from external declarations")
        map.put(RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION, "Runtime annotation can't be put on external declaration")
        map.put(
            RUNTIME_ANNOTATION_NOT_SUPPORTED,
            "Reflection is not supported in JavaScript target, therefore you won't be able to read this annotation in run-time"
        )
        map.put(EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER, "External class constructor cannot have a property parameter")
        map.put(EXTERNAL_ENUM_ENTRY_WITH_BODY, "Entry of external enum class can't have body")
        map.put(EXTERNAL_ANONYMOUS_INITIALIZER, "Anonymous initializer is not allowed in external classes")
        map.put(EXTERNAL_DELEGATION, "Can't use delegate on external declaration")
        map.put(EXTERNAL_DELEGATED_CONSTRUCTOR_CALL, "Delegated constructor call in external class is not allowed")
        map.put(
            WRONG_BODY_OF_EXTERNAL_DECLARATION,
            "Wrong body of external declaration. Must be either ' = definedExternally' or { definedExternally }"
        )
        map.put(WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION, "Wrong initializer of external declaration. Must be ' = definedExternally'")
        map.put(
            WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER,
            "Wrong default value for parameter of external function. Must be ' = definedExternally'"
        )
        map.put(NESTED_EXTERNAL_DECLARATION, "Non top-level `external` declaration")
        map.put(WRONG_EXTERNAL_DECLARATION, "Declaration of such kind ({0}) can''t be external", STRING)
        map.put(NESTED_CLASS_IN_EXTERNAL_INTERFACE, "Interface can't contain nested classes and objects")
        map.put(EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE, "External type extends non-external type")
        map.put(INLINE_EXTERNAL_DECLARATION, "Inline external declaration")
        map.put(
            INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING,
            "Using value classes as parameter type or return type of external declarations is experimental"
        )
        map.put(
            INLINE_CLASS_IN_EXTERNAL_DECLARATION,
            "Using value classes as parameter type or return type of external declarations is not supported"
        )
        map.put(EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, "Function types with receiver are prohibited in external declarations")
        map.put(
            NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE,
            "Only nullable properties of external interfaces are allowed to be non-abstract"
        )

        map.checkMissingMessages(FirJsErrors)
    }
}