/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.MemoryModel
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class AtomicArrayIntrinsicLowering(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrBuildingTransformer(context) {
            override fun visitClass(declaration: IrClass): IrStatement {
                declaration.transformChildrenVoid()
                processDeclarationList(declaration.declarations)
                return declaration
            }

            override fun visitFile(declaration: IrFile): IrFile {
                declaration.transformChildrenVoid()
                processDeclarationList(declaration.declarations)
                return declaration
            }

            private fun processDeclarationList(declarations: MutableList<IrDeclaration>) {
                val generatedIntrinsics = mutableListOf<IrDeclaration>()
                declarations.forEach {
                    // for atomic array properties generate IR of intrinsics:
                    // val intArr = AtomicIntegerArray(10)
                    // <getArrayElement-intArr>
                    // <compareAndSetArrayElement-intArr>
                    if (it is IrProperty && it.backingField?.type?.classFqName?.asString()?.contains("AtomicIntegerArray") == true) { // todo: more reliable check of the field type
                        val field = it.backingField!!
                        generatedIntrinsics.addAll(
                            getArrayElementFunction(field) // todo: here generate IR for compareAndSet and set intrinsics
                        )
                    }
                }
                declarations.addAll(generatedIntrinsics)
            }

            private val intrinsicMap = mapOf(
                    IntrinsicType.COMPARE_AND_SET_FIELD to ::compareAndSetFunction,
                    IntrinsicType.COMPARE_AND_EXCHANGE_FIELD to ::compareAndExchangeFunction,
                    IntrinsicType.GET_AND_SET_FIELD to ::getAndSetFunction,
                    IntrinsicType.GET_AND_ADD_FIELD to ::getAndAddFunction,
            )

            override fun visitCall(expression: IrCall): IrExpression {
                // here we transform the call to the intrinsic -> replacing it eith the generated IR declaration
                expression.transformChildrenVoid(this)
                val intrinsicType = tryGetIntrinsicType(expression).takeIf { it in intrinsicMap } ?: return expression
                builder.at(expression)
                val reference = expression.extensionReceiver as? IrPropertyReference
                        ?: return unsupported("Only compile-time known IrProperties supported for $intrinsicType")
                val property = reference.symbol.owner
                val backingField = property.backingField
                if (backingField?.type?.binaryTypeIsReference() == true && context.memoryModel != MemoryModel.EXPERIMENTAL) {
                    return unsupported("Only primitives are supported for $intrinsicType with legacy memory model")
                }
                if (backingField?.hasAnnotation(KonanFqNames.volatile) != true) {
                    return unsupported("Only volatile properties are supported for $intrinsicType")
                }
                val function = intrinsicMap[intrinsicType]!!(backingField)
                return builder.irCall(function).apply {
                    dispatchReceiver = reference.dispatchReceiver
                    putValueArgument(0, expression.getValueArgument(0))
                    if (intrinsicType == IntrinsicType.COMPARE_AND_SET_FIELD || intrinsicType == IntrinsicType.COMPARE_AND_EXCHANGE_FIELD) {
                        putValueArgument(1, expression.getValueArgument(1))
                    }
                }.let {
                    if (backingField.requiresBooleanConversion()) {
                        for (arg in 0 until it.valueArgumentsCount) {
                            it.putValueArgument(arg, builder.irBoolToByte(it.getValueArgument(arg)!!))
                        }
                        if (intrinsicType == IntrinsicType.COMPARE_AND_EXCHANGE_FIELD || intrinsicType == IntrinsicType.GET_AND_SET_FIELD) {
                            builder.irByteToBool(it)
                        } else {
                            it
                        }
                    } else {
                        it
                    }
                }
            }
        })
    }
}
