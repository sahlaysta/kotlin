/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


// ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT reported because actual annotation classes have some targets
// which don't allow.
// Example: in @Test java target "ElementType.METHOD" is equivalent of Kotlin targets
// FUNCTION, PROPERTY_SETTER, PROPERTY_GETTER, but we allow only FUNCTION.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")
@file:JvmPackageName("kotlin.test.junit.annotations")

package kotlin.test

public actual typealias Test = org.junit.Test
public actual typealias Ignore = org.junit.Ignore
public actual typealias BeforeTest = org.junit.Before
public actual typealias AfterTest = org.junit.After
