/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.enums.*

@TypedIntrinsic(IntrinsicType.ENUM_VALUES)
@PublishedApi
internal external fun <T : Enum<T>> enumValuesIntrinsic(): Array<T>

@TypedIntrinsic(IntrinsicType.ENUM_VALUE_OF)
@PublishedApi
internal external fun <T : Enum<T>> enumValueOfIntrinsic(name: String): T

@TypedIntrinsic(IntrinsicType.ENUM_ENTRIES)
@PublishedApi
internal external fun <T : Enum<T>> enumEntriesIntrinsicImpl(): EnumEntries<T>

@PublishedApi
/**
 * Compiler would replace enumEntries<T> stdlib function to this in Inline lowering.
 * To do that, this function needs to be inline
 */
internal inline fun <reified T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T> = enumEntriesIntrinsicImpl()