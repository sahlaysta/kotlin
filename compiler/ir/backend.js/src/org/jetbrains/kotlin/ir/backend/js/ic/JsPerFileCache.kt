/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.cityHash64HexString
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.File

class JsPerFileCache(private val moduleArtifacts: List<ModuleArtifact>) : JsMultiArtifactCache<JsPerFileCache.CachedFileInfo>() {
    companion object {
        private const val JS_MODULE_HEADER = "js.module.header.bin"
        private const val CACHED_FILE_JS = "file.js"
        private const val CACHED_EXPORT_FILE_JS = "file.export.js"
        private const val CACHED_FILE_JS_MAP = "file.js.map"
        private const val CACHED_FILE_D_TS = "file.d.ts"
    }

    class CachedFileInfo(
        val moduleArtifact: ModuleArtifact,
        val fileArtifact: SrcFileArtifact,
        val isExportFileCachedInfo: Boolean = false,
    ) : CacheInfo {
        var crossFileReferencesHash: ICHash = ICHash()
        var exportFileCachedInfo: CachedFileInfo? = null
        override lateinit var jsIrHeader: JsIrModuleHeader

        constructor(
            jsIrModuleHeader: JsIrModuleHeader,
            moduleArtifact: ModuleArtifact,
            fileArtifact: SrcFileArtifact,
            isExportFileCachedInfo: Boolean = false,
        ) : this(moduleArtifact, fileArtifact, isExportFileCachedInfo) {
            jsIrHeader = jsIrModuleHeader
        }

        val artifactsDir: File? = moduleArtifact.artifactsDir?.let {
            val path = "${moduleArtifact.moduleSafeName}/${fileArtifact.srcFilePath}"
            val pathHash = path.cityHash64HexString()
            File(moduleArtifact.artifactsDir, "${fileArtifact.srcFilePath.substringAfterLast('/')}$pathHash")
        }
    }

    private val headerToCachedInfo = hashMapOf<JsIrModuleHeader, CachedFileInfo>()
    private val moduleFragmentToExternalName = ModuleFragmentToExternalName(emptyMap())

    private fun JsIrProgramFragment.getMainFragmentExternalName(moduleArtifact: ModuleArtifact) =
        moduleFragmentToExternalName.getExternalNameFor(name, packageFqn, moduleArtifact.moduleExternalName)

    private fun JsIrProgramFragment.getExportFragmentExternalName(moduleArtifact: ModuleArtifact) =
        moduleFragmentToExternalName.getExternalNameForExporterFile(name, packageFqn, moduleArtifact.moduleExternalName)

    private fun JsIrProgramFragment.asIrModuleHeader(moduleName: String): JsIrModuleHeader {
        return JsIrModuleHeader(
            moduleName = moduleName,
            externalModuleName = moduleName,
            definitions = definitions,
            nameBindings = nameBindings.mapValues { v -> v.value.toString() },
            optionalCrossModuleImports = optionalCrossModuleImports,
            associatedModule = null
        )
    }

    private fun SrcFileArtifact.loadJsIrModuleHeaders(moduleArtifact: ModuleArtifact) = with(loadJsIrFragments()) {
        LoadedJsIrModuleHeaders(
            mainFragment.run { asIrModuleHeader(getMainFragmentExternalName(moduleArtifact)) },
            exportFragment?.run { asIrModuleHeader(mainFragment.getExportFragmentExternalName(moduleArtifact)) },
        )
    }

    private fun CodedInputStream.loadSingleCachedFileInfo(cachedFileInfo: CachedFileInfo) = cachedFileInfo.also {
        val moduleName = readString()

        it.crossFileReferencesHash = ICHash.fromProtoStream(this)

        val (definitions, nameBindings, optionalCrossModuleImports) = fetchJsIrModuleHeaderNames()

        it.jsIrHeader = JsIrModuleHeader(
            moduleName = moduleName,
            externalModuleName = moduleName,
            definitions = definitions,
            nameBindings = nameBindings,
            optionalCrossModuleImports = optionalCrossModuleImports,
            associatedModule = null
        )
    }

    private fun <T> CachedFileInfo.readModuleHeaderCache(f: CodedInputStream.() -> T): T? =
        artifactsDir?.let { File(it, JS_MODULE_HEADER).useCodedInputIfExists(f) }

    private fun ModuleArtifact.fetchFileInfoFor(fileArtifact: SrcFileArtifact): List<CachedFileInfo>? {
        val moduleArtifact = this
        val mainFileCachedFileInfo = CachedFileInfo(moduleArtifact, fileArtifact)

        return mainFileCachedFileInfo.readModuleHeaderCache {
            mainFileCachedFileInfo.run {
                exportFileCachedInfo = fetchFileInfoForExportedPart(this)
                loadSingleCachedFileInfo(this)
                listOfNotNull(exportFileCachedInfo, this)
            }
        }
    }

    private fun CodedInputStream.fetchFileInfoForExportedPart(mainCachedFileInfo: CachedFileInfo): CachedFileInfo? {
        return ifTrue {
            loadSingleCachedFileInfo(
                CachedFileInfo(
                    mainCachedFileInfo.moduleArtifact,
                    mainCachedFileInfo.fileArtifact,
                    isExportFileCachedInfo = true
                )
            )
        }
    }

    private fun CodedOutputStream.commitSingleFileInfo(cachedFileInfo: CachedFileInfo) {
        writeStringNoTag(cachedFileInfo.jsIrHeader.externalModuleName)
        cachedFileInfo.crossFileReferencesHash.toProtoStream(this)
        commitJsIrModuleHeaderNames(cachedFileInfo.jsIrHeader)
    }

    private fun CachedFileInfo.commitFileInfo() = artifactsDir.takeIf { !isExportFileCachedInfo }?.let { file ->
        File(file, JS_MODULE_HEADER).useCodedOutput {
            ifNotNull(exportFileCachedInfo) { commitSingleFileInfo(it) }
            commitSingleFileInfo(this@commitFileInfo)
        }
    }

    private fun ModuleArtifact.loadFileInfoFor(fileArtifact: SrcFileArtifact): List<CachedFileInfo> {
        val moduleArtifact = this
        val headers = fileArtifact.loadJsIrModuleHeaders(moduleArtifact)

        val mainCachedFileInfo = CachedFileInfo(headers.mainHeader, this, fileArtifact)

        if (headers.exportHeader != null) {
            mainCachedFileInfo.exportFileCachedInfo =
                mainCachedFileInfo.readModuleHeaderCache { fetchFileInfoForExportedPart(mainCachedFileInfo) }
                    ?: CachedFileInfo(headers.exportHeader, moduleArtifact, fileArtifact, isExportFileCachedInfo = true)
        }

        return listOfNotNull(mainCachedFileInfo.exportFileCachedInfo, mainCachedFileInfo)
    }

    private val CachedFileInfo.cachedFiles: Triple<File, File?, File?>?
        get() = artifactsDir?.let {
            when {
                isExportFileCachedInfo -> Triple(File(it, CACHED_EXPORT_FILE_JS), null, File(it, CACHED_FILE_D_TS))
                else -> Triple(File(it, CACHED_FILE_JS), File(it, CACHED_FILE_JS_MAP), null)
            }
        }

    override fun getMainModuleAndDependencies(cacheInfo: List<CachedFileInfo>) = null to cacheInfo

    override fun fetchCompiledJsCodeForNullCacheInfo() = PerFileEntryPointCompilationOutput()

    override fun fetchCompiledJsCode(cacheInfo: CachedFileInfo) =
        cacheInfo.cachedFiles?.let { (jsCodeFile, sourceMapFile, tsDefinitionsFile) ->
            jsCodeFile.ifExists { this }
                ?.let { CompilationOutputsCached(it, sourceMapFile?.ifExists { this }, tsDefinitionsFile?.ifExists { this }) }
        }

    override fun commitCompiledJsCode(cacheInfo: CachedFileInfo, compilationOutputs: CompilationOutputsBuilt) =
        cacheInfo.cachedFiles?.let { (jsCodeFile, jsMapFile, tsDefinitionsFile) ->
            tsDefinitionsFile?.writeIfNotNull(compilationOutputs.tsDefinitions?.raw)
            compilationOutputs.writeJsCodeIntoModuleCache(jsCodeFile, jsMapFile)
        } ?: compilationOutputs

    override fun loadJsIrModule(cacheInfo: CachedFileInfo): JsIrModule {
        val fragments = cacheInfo.fileArtifact.loadJsIrFragments()
        return JsIrModule(
            cacheInfo.jsIrHeader.moduleName,
            cacheInfo.jsIrHeader.externalModuleName,
            listOf(if (cacheInfo.isExportFileCachedInfo) fragments.exportFragment!! else fragments.mainFragment)
        )
    }

    override fun loadProgramHeadersFromCache(): List<CachedFileInfo> {
        return moduleArtifacts
            .flatMap { module ->
                module.fileArtifacts.flatMap {
                    if (it.isModified())
                        module.loadFileInfoFor(it)
                    else
                        module.fetchFileInfoFor(it) ?: module.loadFileInfoFor(it)
                }
            }
            .onEach { headerToCachedInfo[it.jsIrHeader] = it }
    }

    override fun loadRequiredJsIrModules(crossModuleReferences: Map<JsIrModuleHeader, CrossModuleReferences>) {
        for ((header, references) in crossModuleReferences) {
            val cachedInfo = headerToCachedInfo[header] ?: notFoundIcError("artifact for module ${header.moduleName}")

            val actualCrossModuleHash = references.crossModuleReferencesHashForIC()

            if (header.associatedModule == null && cachedInfo.crossFileReferencesHash != actualCrossModuleHash) {
                header.associatedModule = loadJsIrModule(cachedInfo)
            }

            header.associatedModule?.let {
                cachedInfo.crossFileReferencesHash = actualCrossModuleHash
                cachedInfo.commitFileInfo()
            }
        }
    }

    private val List<JsIrProgramFragment>.mainFragment: JsIrProgramFragment get() = first()
    private val List<JsIrProgramFragment>.exportFragment: JsIrProgramFragment? get() = getOrNull(1)

    private data class LoadedJsIrModuleHeaders(val mainHeader: JsIrModuleHeader, val exportHeader: JsIrModuleHeader?)
}