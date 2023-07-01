/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider

object CommonStubBasedDeserializedSymbolProviderFactory {
    fun createCommonFirDeserializedSymbolProviders(
        project: Project,
        session: FirSession,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        return listOf(
            createStubBasedFirSymbolProviderForCommonMetadataFiles(project, scope, session, moduleDataProvider, kotlinScopeProvider),
            createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(project, scope, session, moduleDataProvider, kotlinScopeProvider),
        )
    }
}
