/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.psi.KtElement

public abstract class KtResolveExtensionInfoProvider : KtAnalysisSessionComponent() {
    public abstract fun getResolveExtensionScopeWithTopLevelDeclarations(): KtScope
    public abstract fun isResolveExtensionFile(file: VirtualFile): Boolean
    public abstract fun getResolveExtensionNavigationElements(originalPsi: KtElement): Collection<PsiElement>
}

public interface KtSymbolFromResolveExtensionProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns [KtScope] which contains all top-level callable declarations which are generated by [KtResolveExtension]
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
     */
    public fun getResolveExtensionScopeWithTopLevelDeclarations(): KtScope = withValidityAssertion {
        analysisSession.resolveExtensionInfoProvider.getResolveExtensionScopeWithTopLevelDeclarations()
    }

    /**
     * Returns whether this [VirtualFile] was provided by a [KtResolveExtension].
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
     */
    public val VirtualFile.isResolveExtensionFile: Boolean
        get() = withValidityAssertion {
            analysisSession.resolveExtensionInfoProvider.isResolveExtensionFile(this)
        }

    /**
     * Returns whether this [KtElement] was provided by a [KtResolveExtension].
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
     */
    public val KtElement.isFromResolveExtension: Boolean
        get() = withValidityAssertion {
            containingKtFile.virtualFile?.isResolveExtensionFile ?: false
        }

    /**
     * Returns the [PsiElement]s which should be used as a navigation target in place of this [KtElement]
     * provided by a [KtResolveExtension].
     *
     * These [PsiElement]s will typically be the source item(s) that caused the given [KtElement] to be generated
     * by the [KtResolveExtension]. For example, for a [KtElement] generated by a resource compiler, this will
     * typically be a list of the [PsiElement]s of the resource items in the corresponding resource file.
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
     */
    public fun KtElement.getResolveExtensionNavigationElements(): Collection<PsiElement> = withValidityAssertion {
        analysisSession.resolveExtensionInfoProvider.getResolveExtensionNavigationElements(this)
    }
}
