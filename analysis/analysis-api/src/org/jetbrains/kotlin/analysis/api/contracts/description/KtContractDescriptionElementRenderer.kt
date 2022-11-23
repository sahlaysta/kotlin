/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal fun Context.renderKtContractEffectDeclaration(value: KtContractEffectDeclaration, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        when (value) {
            is KtContractCallsContractEffectDeclaration -> {
                appendProperty(value::valueParameterReference, ::renderKtContractDescriptionValue)
                appendSimpleProperty(value::kind, endWithNewLine)
            }
            is KtContractConditionalContractEffectDeclaration -> {
                appendProperty(value::effect, ::renderKtContractEffectDeclaration)
                appendProperty(value::condition, ::renderKtContratBooleanExpression, endWithNewLine)
            }
            is KtContractReturnsContractEffectDeclaration -> {
                appendProperty(value::value, ::renderKtContractDescriptionValue, endWithNewLine)
            }
        }
    }

internal fun Context.renderKtContractDescriptionValue(value: KtContractDescriptionValue, endWithNewLine: Boolean = true) {
    val append: (String) -> Unit = (if (endWithNewLine) printer::appendLine else printer::append)
    return when (value) {
        is KtContractConstantReference -> {
            append(value::class.simpleName ?: error("$value doesn't have simpleName"))
        }
        is KtContractAbstractValueParameterReference -> printer.appendHeader(value::class) {
            appendProperty(value::parameterSymbol, renderer = { valueParameterSymbol, _ ->
                append(with(session) { symbolRenderer.render(valueParameterSymbol) })
            })
        }
    }
}

internal fun Context.renderKtContratBooleanExpression(value: KtContractBooleanExpression, endWithNewLine: Boolean = true): Unit =
    when (value) {
        is KtContractDescriptionValue -> renderKtContractDescriptionValue(value, endWithNewLine)
        is KtContractLogicalNot -> printer.appendHeader(value::class) {
            appendProperty(value::argument, ::renderKtContratBooleanExpression, endWithNewLine)
        }
        is KtContractBinaryLogicExpression -> printer.appendHeader(value::class) {
            appendProperty(value::left, ::renderKtContratBooleanExpression)
            appendProperty(value::right, ::renderKtContratBooleanExpression)
            appendSimpleProperty(value::kind, endWithNewLine)
        }
        is KtContractIsInstancePredicate -> printer.appendHeader(value::class) {
            appendProperty(value::argument, ::renderKtContractDescriptionValue)
            appendProperty(value::type, renderer = { type, _ ->
                appendLine(with(session) { symbolRenderer.renderType(type) })
            })
            appendSimpleProperty(value::isNegated, endWithNewLine)
        }
        is KtContractIsNullPredicate -> printer.appendHeader(value::class) {
            appendProperty(value::argument, ::renderKtContractDescriptionValue)
            appendSimpleProperty(value::isNegated, endWithNewLine)
        }
    }

internal data class Context(val session: KtAnalysisSession, val printer: PrettyPrinter, val symbolRenderer: DebugSymbolRenderer)

private fun PrettyPrinter.appendHeader(clazz: KClass<*>, body: PrettyPrinter.() -> Unit) {
    appendLine(clazz.simpleName + ":")
    withIndent { body() }
}

private fun <T> PrettyPrinter.appendProperty(
    prop: KProperty<T>,
    renderer: (T, Boolean) -> Unit,
    endWithNewLine: Boolean = true
) {
    appendLine(prop.name + ":")
    withIndent {
        renderer(prop.call(), endWithNewLine)
    }
}

private fun PrettyPrinter.appendSimpleProperty(prop: KProperty<Any>, endWithNewLine: Boolean = true) {
    append(prop.name + ": ")
    append(prop.call().toString())
    if (endWithNewLine) appendLine()
}
