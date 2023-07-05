/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.memoryOptimizedMap

abstract class AbstractDeepCopyIrTreeWithSymbolsForInliner(
    val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
    val parent: IrDeclarationParent?,
) {
    fun copy(irElement: IrElement): IrElement {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        // Make symbol remapper aware of the callsite's type arguments.
        symbolRemapper.typeArguments = typeArguments

        // Copy IR.
        val result = irElement.transform(copier, data = null)

        result.patchDeclarationParents(parent)
        return result
    }

    protected class SymbolRemapperImpl(descriptorsRemapper: DescriptorsRemapper) : DeepCopySymbolRemapper(descriptorsRemapper) {

        var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
            set(value) {
                if (field != null) return
                field = value?.asSequence()?.associate {
                    (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
                }
            }

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments?.get(result)?.classifierOrNull ?: result
        }
    }

    protected val symbolRemapper = SymbolRemapperImpl(NullDescriptorsRemapper)
    abstract val copier: IrTreeWithSymbolsCopier
}

class DeepCopyIrTreeWithSymbolsForInliner(
    typeArguments: Map<IrTypeParameterSymbol, IrType?>?, parent: IrDeclarationParent?,
) : AbstractDeepCopyIrTreeWithSymbolsForInliner(typeArguments, parent) {

    override val copier: IrTreeWithSymbolsCopier

    init {
        val typeRemapper = InlinerTypeRemapper(symbolRemapper, typeArguments)
        copier = IrTreeWithSymbolsCopier(symbolRemapper, typeRemapper)
        typeRemapper.copier = copier
    }
}

open class IrTreeWithSymbolsCopier(protected val symbolRemapper: SymbolRemapper, open val typeRemapper: InlinerTypeRemapper) :
    DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
    private fun IrType.remapTypeAndErase() = typeRemapper.remapTypeAndOptionallyErase(this, erase = true)

    override fun visitTypeOperator(expression: IrTypeOperatorCall) =
        IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapTypeAndErase(),
            expression.operator,
            expression.typeOperand.remapTypeAndErase(),
            expression.argument.transform()
        ).copyAttributes(expression)
}

open class InlinerTypeRemapper(
    val symbolRemapper: SymbolRemapper,
    val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
) : TypeRemapper {

    lateinit var copier: DeepCopyIrTreeWithSymbols

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

    override fun leaveScope() {}

    private fun remapTypeArguments(
        arguments: List<IrTypeArgument>,
        erasedParameters: MutableSet<IrTypeParameterSymbol>?) =
        arguments.memoryOptimizedMap { argument ->
            (argument as? IrTypeProjection)?.let { proj ->
                remapTypeAndOptionallyErase(proj.type, erasedParameters)?.let { newType ->
                    makeTypeProjection(newType, proj.variance)
                } ?: IrStarProjectionImpl
            }
                ?: argument
        }

    override fun remapType(type: IrType) = remapTypeAndOptionallyErase(type, erase = false)

    fun remapTypeAndOptionallyErase(type: IrType, erase: Boolean): IrType {
        val erasedParams = if (erase) mutableSetOf<IrTypeParameterSymbol>() else null
        return remapTypeAndOptionallyErase(type, erasedParams) ?: error("Cannot substitute type ${type.render()}")
    }

    private fun remapTypeAndOptionallyErase(
        type: IrType,
        erasedParameters: MutableSet<IrTypeParameterSymbol>?
    ): IrType? {
        if (type !is IrSimpleType) return type

        val classifier = type.classifier
        val substitutedType = typeArguments?.get(classifier)?.let { transformBackendSpecific(it) }

        // Erase non-reified type parameter if asked to.
        if (erasedParameters != null && substitutedType != null && (classifier as? IrTypeParameterSymbol)?.owner?.isReified == false) {

            if (classifier in erasedParameters) {
                return null
            }

            erasedParameters.add(classifier)

            // Pick the (necessarily unique) non-interface upper bound if it exists.
            val superTypes = classifier.owner.superTypes
            val superClass = superTypes.firstOrNull {
                it.classOrNull?.owner?.isInterface == false
            }

            val upperBound = superClass ?: superTypes.first()

            // TODO: Think about how to reduce complexity from k^N to N^k
            val erasedUpperBound = remapTypeAndOptionallyErase(upperBound, erasedParameters)
                ?: error("Cannot erase upperbound ${upperBound.render()}")

            erasedParameters.remove(classifier)

            return erasedUpperBound.mergeNullability(type)
        }

        if (substitutedType is IrDynamicType) return substitutedType

        if (substitutedType is IrSimpleType) {
            return substitutedType.mergeNullability(type)
        }

        return type.buildSimpleType {
            kotlinType = null
            this.classifier = symbolRemapper.getReferencedClassifier(classifier)
            arguments = remapTypeArguments(type.arguments, erasedParameters)
            annotations = type.annotations.memoryOptimizedMap { it.transform(copier, null) as IrConstructorCall }
        }
    }

    protected open fun transformBackendSpecific(type: IrType) = type
}