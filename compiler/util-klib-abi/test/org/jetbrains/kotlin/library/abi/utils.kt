/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.klib.compileModuleToAnalyzedFirWithPsi
import org.jetbrains.kotlin.cli.js.klib.serializeFirKlib
import org.jetbrains.kotlin.cli.js.klib.transformFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.createForParallelTests
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.util.KtTestUtil.getHomeDirectory
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.random.Random
import org.jetbrains.kotlin.konan.file.File as KFile

private val TestInfo.className: String get() = testClass.orElseGet { fail("Can't get test class name") }.simpleName
private val TestInfo.methodName: String get() = testMethod.orElseGet { fail("Can't get test method name") }.name

internal fun getTestName(testInfo: TestInfo): String = testInfo.methodName

internal fun setUpBuildDir(testInfo: TestInfo): File {
    val projectBuildDir = System.getenv(ENV_VAR_PROJECT_BUILD_DIR)
        ?.let { File(it) }
        ?: fail<Nothing>("$ENV_VAR_PROJECT_BUILD_DIR environment variable not specified")

    assertTrue(projectBuildDir.isDirectory) { "Project build dir does not exist: $projectBuildDir" }

    return projectBuildDir.resolve("t").resolve("${testInfo.className}.${testInfo.methodName}").apply {
        deleteRecursively()
        mkdirs()
    }
}

internal fun computeTestFiles(relativePath: String, goldFileExtension: String): Pair<File, File> {
    val sourceFile = File(getHomeDirectory()).resolve(relativePath)
    assertEquals("kt", sourceFile.extension) { "Invalid source file: $sourceFile" }
    assertTrue(sourceFile.isFile) { "Source file does not exist: $sourceFile" }

    val dumpFile = sourceFile.withReplacedExtensionOrNull("kt", goldFileExtension)!!
    assertTrue(dumpFile.isFile) { "Dump file does not exist: $dumpFile" }

    return sourceFile to dumpFile
}

internal fun buildLibrary(sourceFile: File, libraryName: String, buildDir: File): File {
    val configuration = CompilerConfiguration()
    val environment = createForParallelTests(TestDisposable(), configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)

    configuration.put(CommonConfigurationKeys.MODULE_NAME, libraryName)
    configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
    configuration.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)
    configuration.put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, ZipFileSystemCacheableAccessor(2))

    val psiManager = PsiManager.getInstance(environment.project)
    val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

    val virtualFile = fileSystem.findFileByIoFile(sourceFile) ?: error("VirtualFile for $sourceFile not found")
    val ktFiles = SingleRootFileViewProvider(psiManager, virtualFile).allFiles.filterIsInstance<KtFile>()

    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

    val jsStdlib = File("libraries/stdlib/js-ir/build/classes/kotlin/js/main").absoluteFile
    val dependencies = listOf(jsStdlib.absolutePath)

    val moduleStructure = ModulesStructure(
        project = environment.project,
        mainModule = MainModule.SourceFiles(ktFiles),
        compilerConfiguration = configuration,
        dependencies = dependencies,
        friendDependenciesPaths = emptyList()
    )

    val outputStream = ByteArrayOutputStream()
    val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)

    val analyzedOutput = compileModuleToAnalyzedFirWithPsi(
        moduleStructure = moduleStructure,
        ktFiles = ktFiles,
        libraries = dependencies,
        friendLibraries = emptyList(),
        diagnosticsReporter = diagnosticsReporter,
        incrementalDataProvider = null,
        lookupTracker = null
    )

    val fir2IrActualizedResult = transformFirToIr(moduleStructure, analyzedOutput.output, diagnosticsReporter)

    if (analyzedOutput.reportCompilationErrors(moduleStructure, diagnosticsReporter, messageCollector)) {
        val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
        fail<Nothing>("The following errors occurred compiling test:\n$messages")
    }

    val libraryDir = KFile(buildDir.resolve(libraryName).absolutePath)
    val libraryFile = KFile(libraryDir.absolutePath + ".klib")

    serializeFirKlib(
        moduleStructure = moduleStructure,
        firOutputs = analyzedOutput.output,
        fir2IrActualizedResult = fir2IrActualizedResult,
        outputKlibPath = libraryDir.absolutePath,
        messageCollector = messageCollector,
        diagnosticsReporter = diagnosticsReporter,
        jsOutputName = libraryName
    )

    if (messageCollector.hasErrors()) {
        val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
        fail<Nothing>("The following errors occurred serializing test klib:\n$messages")
    }

    libraryDir.zipDirAs(libraryFile)
    libraryDir.deleteRecursively()

    return File(libraryFile.absolutePath)
}

@ExperimentalLibraryAbiReader
internal fun patchManifest(libraryFile: File, customManifest: LibraryManifest) {
    val libraryKFile = KFile(libraryFile.absolutePath)
    val manifestProperties = resolveSingleFileKlib(libraryKFile).manifestProperties

    fun set(name: String, value: String?) {
        assertTrue(!value.isNullOrBlank()) { "$name has invalid value: [$value]" }
        manifestProperties[name] = value!!
    }

    set(KLIB_PROPERTY_BUILTINS_PLATFORM, customManifest.platform)
    set(KLIB_PROPERTY_NATIVE_TARGETS, customManifest.nativeTargets.joinToString(" "))
    set(KLIB_PROPERTY_COMPILER_VERSION, customManifest.compilerVersion)
    set(KLIB_PROPERTY_ABI_VERSION, customManifest.abiVersion)
    set(KLIB_PROPERTY_LIBRARY_VERSION, customManifest.libraryVersion)
    set(KLIB_PROPERTY_IR_PROVIDER, customManifest.irProviderName)

    assertEquals(libraryFile.extension, "klib")
    val libraryDir = libraryFile.resolveSibling(libraryFile.nameWithoutExtension)
    val libraryKDir = KFile(libraryDir.absolutePath)
    libraryKFile.unzipTo(libraryKDir)
    libraryKFile.deleteRecursively()

    libraryDir.walkTopDown()
        .filter { it.isFile && it.name == "manifest" }
        .forEach { manifestProperties.saveToFile(KFile(it.absolutePath)) }

    libraryKDir.zipDirAs(libraryKFile)
}

internal fun generateRandomString(length: Int): String = buildString {
    assertTrue(length > 0)
    repeat(length) { append(Random.nextInt('a'.code, 'z'.code).toChar()) }
}

internal fun generateRandomVersion(): String = buildString {
    append(Random.nextInt(1, 100))
    append('.')
    append(Random.nextInt(1, 100))
    append('.')
    append(Random.nextInt(1, 100))
}

private const val ENV_VAR_PROJECT_BUILD_DIR = "PROJECT_BUILD_DIR"
