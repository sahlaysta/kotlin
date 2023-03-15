/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.*
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.base.stubs.getJavacSignature
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream

class Kapt4LineMappingCollector {
    private val lineInfo: MutableMap<String, KotlinPosition> = mutableMapOf()
    private val signatureInfo = mutableMapOf<String, String>()

    private val filePaths = mutableMapOf<PsiFile, Pair<String, Boolean>>()

    fun registerClass(lightClass: PsiClass) {
        register(lightClass, lightClass.qualifiedNameWithSlashes)
    }

    fun registerMethod(lightClass: PsiClass, method: PsiMethod) {
        register(method, lightClass.qualifiedNameWithSlashes + "#" + method.properName + method.signature)
    }

    fun registerField(lightClass: PsiClass, field: PsiField) {
        register(field, lightClass.qualifiedNameWithSlashes + "#" + field.name)
    }

    fun registerSignature(declaration: JCTree.JCMethodDecl, method: PsiMethod) {
        signatureInfo[declaration.getJavacSignature()] = method.properName + method.signature
    }

    fun getPosition(lightClass: PsiClass): KotlinPosition? {
        return lineInfo[lightClass.qualifiedNameWithSlashes]
    }

    fun getPosition(lightClass: PsiClass, method: PsiMethod): KotlinPosition? =
        lineInfo[lightClass.qualifiedNameWithSlashes + "#" + method.properName + method.signature]

    fun getPosition(lightClass: PsiClass, field: PsiField): KotlinPosition? {
        return lineInfo[lightClass.qualifiedNameWithSlashes + "#" + field.name]
    }

    private fun register(asmNode: Any, fqName: String) {
        val psiElement = (asmNode as? KtLightElement<*, *>)?.kotlinOrigin ?: return
        register(fqName, psiElement)
    }

    private fun register(fqName: String, psiElement: PsiElement) {
        val containingVirtualFile = psiElement.containingFile.virtualFile
        if (containingVirtualFile == null || FileDocumentManager.getInstance().getDocument(containingVirtualFile) == null) {
            return
        }

        val textRange = psiElement.textRange ?: return

        val (path, isRelative) = getFilePathRelativePreferred(psiElement.containingFile)
        lineInfo[fqName] = KotlinPosition(path, isRelative, textRange.startOffset)
    }

    private fun getFilePathRelativePreferred(file: PsiFile): Pair<String, Boolean> {
        return filePaths.getOrPut(file) {
            val absolutePath = file.virtualFile.canonicalPath ?: file.virtualFile.path
            val absoluteFile = File(absolutePath)
            val baseFile = file.project.basePath?.let { File(it) }

            if (absoluteFile.exists() && baseFile != null && baseFile.exists()) {
                val relativePath = absoluteFile.relativeToOrNull(baseFile)?.path
                if (relativePath != null) {
                    return@getOrPut Pair(relativePath, true)
                }
            }

            return@getOrPut Pair(absolutePath, false)
        }
    }

    fun serialize(): ByteArray {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)

        oos.writeInt(KaptStubLineInformation.METADATA_VERSION)

        oos.writeInt(lineInfo.size)
        for ((fqName, kotlinPosition) in lineInfo) {
            oos.writeUTF(fqName)
            oos.writeUTF(kotlinPosition.path)
            oos.writeBoolean(kotlinPosition.isRelativePath)
            oos.writeInt(kotlinPosition.pos)
        }

        oos.writeInt(signatureInfo.size)
        for ((javacSignature, methodDesc) in signatureInfo) {
            oos.writeUTF(javacSignature)
            oos.writeUTF(methodDesc)
        }

        oos.flush()
        return os.toByteArray()
    }

    private val PsiClass.qualifiedNameWithSlashes: String
        get() = qualifiedNameWithDollars?.replace(".", "/") ?: "<no name provided>"
}
