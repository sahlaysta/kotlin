/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import java.io.File

class JsPerFileCache(private val moduleArtifacts: List<ModuleArtifact>) : JsMultiArtifactCache<JsPerFileCache.CachedFileInfo>() {
    companion object {
        private const val JS_MODULE_HEADER = "js.module.header.bin"
        private const val CACHED_FILE_JS = "file.js"
        private const val CACHED_FILE_JS_MAP = "file.js.map"
        private const val CACHED_FILE_D_TS = "file.d.ts"
    }

    class CachedFileInfo(
        val moduleArtifact: ModuleArtifact,
        val fileArtifact: SrcFileArtifact,
        var crossFileReferencesHash: ICHash = ICHash()
    ) : CacheInfo {
        override lateinit var jsIrHeader: JsIrModuleHeader

        val artifactsDir: File? = moduleArtifact.artifactsDir?.let {
            val pathHash =
                "${moduleArtifact.moduleSafeName}/${fileArtifact.srcFilePath}".cityHash64().toULong().toString(Character.MAX_RADIX)
            File(it, "${fileArtifact.srcFilePath.substringAfterLast('/')}.$pathHash")
        }
    }

    private val headerToCachedInfo = hashMapOf<JsIrModuleHeader, CachedFileInfo>()
    private val moduleFragmentToExternalName = ModuleFragmentToExternalName(emptyMap())

    // TODO: Think how to leave without this dummy file
    private fun generateDummyCachedFileInfo(): CachedFileInfo {
        val moduleName = "<dummy>"
        val dummyFile = SrcFileArtifact(moduleName, listOf(JsIrProgramFragment(moduleName, "")))
        val dummyModule = ModuleArtifact(moduleName, listOf(dummyFile))
        return CachedFileInfo(dummyModule, dummyFile).apply {
            jsIrHeader = JsIrModuleHeader(
                moduleName = moduleName,
                externalModuleName = moduleName,
                definitions = emptySet(),
                nameBindings = emptyMap(),
                optionalCrossModuleImports = emptySet(),
                associatedModule = JsIrModule(moduleName, moduleName, dummyFile.loadJsIrFragments())
            )
        }
    }

    private fun List<JsIrProgramFragment>.asJsIrModuleHeaders(moduleArtifact: ModuleArtifact): List<JsIrModuleHeader> = buildList {
        val mainFile = this@asJsIrModuleHeaders.first()
        val moduleName = moduleFragmentToExternalName.getExternalNameFor(
            mainFile.name,
            mainFile.packageFqn,
            moduleArtifact.moduleExternalName
        )
        add(
            JsIrModuleHeader(
                moduleName = moduleName,
                externalModuleName = moduleName,
                definitions = mainFile.definitions,
                nameBindings = mainFile.nameBindings.mapValues { it.value.toString() },
                optionalCrossModuleImports = mainFile.optionalCrossModuleImports,
                reexportedInModuleWithName = null,
                associatedModule = JsIrModule(moduleName, moduleName, listOf(mainFile))
            )
        )

        this@asJsIrModuleHeaders.getOrNull(1)?.let { exportFile ->
            val exportFileName = moduleFragmentToExternalName.getExternalNameForExporterFile(
                mainFile.name,
                mainFile.packageFqn,
                moduleArtifact.moduleExternalName
            )
            add(
                JsIrModuleHeader(
                    moduleName = exportFileName,
                    externalModuleName = exportFileName,
                    definitions = exportFile.definitions,
                    nameBindings = exportFile.nameBindings.mapValues { it.value.toString() },
                    optionalCrossModuleImports = exportFile.optionalCrossModuleImports,
                    reexportedInModuleWithName = moduleName,
                    associatedModule = JsIrModule(exportFileName, exportFileName, listOf(exportFile))
                )
            )
        }
    }

    private fun ModuleArtifact.fetchFileInfoFor(fileArtifact: SrcFileArtifact): CachedFileInfo? {
        val cacheFileInfo = CachedFileInfo(this@fetchFileInfoFor, fileArtifact)
        return cacheFileInfo.artifactsDir?.let {
            File(it, JS_MODULE_HEADER).useCodedInputIfExists {
                val moduleName = readString()

                cacheFileInfo.crossFileReferencesHash = ICHash.fromProtoStream(this)

                val (definitions, nameBindings, optionalCrossModuleImports) = fetchJsIrModuleHeaderNames()

                cacheFileInfo.apply {
                    jsIrHeader = JsIrModuleHeader(
                        moduleName = moduleName,
                        externalModuleName = moduleName,
                        definitions = definitions,
                        nameBindings = nameBindings,
                        optionalCrossModuleImports = optionalCrossModuleImports,
                        associatedModule = null
                    )
                }
            }
        }
    }

    private fun CachedFileInfo.commitModuleInfo() = artifactsDir?.let {
        File(it, JS_MODULE_HEADER).useCodedOutput {
            writeStringNoTag(jsIrHeader.externalModuleName)
            crossFileReferencesHash.toProtoStream(this)
            commitJsIrModuleHeaderNames(jsIrHeader)
        }
    }

    private fun ModuleArtifact.loadInfoFor(fileArtifact: SrcFileArtifact) =
        fileArtifact
            .loadJsIrFragments()
            .asJsIrModuleHeaders(this)
            .map { CachedFileInfo(this, fileArtifact).apply { jsIrHeader = it } }

    override fun fetchCompiledJsCode(cacheInfo: CachedFileInfo): CompilationOutputsCached? {
        val cacheDir = cacheInfo.artifactsDir

        val jsCodeFile = File(cacheDir, CACHED_FILE_JS).ifExists { this }
        val sourceMapFile = File(cacheDir, CACHED_FILE_JS_MAP).ifExists { this }
        val tsDefinitionsFile = File(cacheDir, CACHED_FILE_D_TS).ifExists { this }

        return jsCodeFile?.let { CompilationOutputsCached(it, sourceMapFile, tsDefinitionsFile) }
    }

    override fun commitCompiledJsCode(cacheInfo: CachedFileInfo, compilationOutputs: CompilationOutputsBuilt): CompilationOutputs {
        val cacheDir = cacheInfo.artifactsDir
        val jsCodeFile = File(cacheDir, CACHED_FILE_JS)
        val jsMapFile = File(cacheDir, CACHED_FILE_JS_MAP)
        File(cacheDir, CACHED_FILE_D_TS).writeIfNotNull(compilationOutputs.tsDefinitions?.raw)

        return compilationOutputs.writeJsCodeIntoModuleCache(jsCodeFile, jsMapFile)
    }

    override fun loadJsIrModule(cacheInfo: CachedFileInfo): JsIrModule {
        return cacheInfo.jsIrHeader.associatedModule ?: cacheInfo.fileArtifact
            .loadJsIrFragments()
            .asJsIrModuleHeaders(cacheInfo.moduleArtifact)
            .first()
            .associatedModule!!
    }

    override fun loadProgramHeadersFromCache(): List<CachedFileInfo> {
        return moduleArtifacts.flatMap { module ->
            module.fileArtifacts
                .flatMap {
                    when {
                        it.isModified() -> module.loadInfoFor(it)
                        else -> module.fetchFileInfoFor(it)?.let(::listOf) ?: module.loadInfoFor(it)
                    }
                }
                .plus(generateDummyCachedFileInfo())
                .map { actualInfo ->
                    headerToCachedInfo[actualInfo.jsIrHeader] = actualInfo
                    actualInfo
                }
        }
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
                cachedInfo.commitModuleInfo()
            }
        }
    }
}
