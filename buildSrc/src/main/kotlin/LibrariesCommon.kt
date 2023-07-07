/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("LibrariesCommon")

import gradle.kotlin.dsl.accessors._a448c82b4669f5dc55622c27b71461fb.base
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.java.archives.Manifest
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

@JvmOverloads
fun Project.configureJava9Compilation(
    moduleName: String,
    moduleOutputs: Collection<FileCollection> = setOf(sourceSets["main"].output),
    compileClasspathConfiguration: Configuration = configurations["compileClasspath"],
) {
    configurations["java9CompileClasspath"].extendsFrom(compileClasspathConfiguration)

    tasks.withType(KotlinCompile::class.java).matching {
        it.name == "compileJava9Kotlin" || it.name == "compileJava9KotlinJvm"
    }.configureEach {
        configureTaskToolchain(JdkMajorVersion.JDK_9_0)
        @Suppress("DEPRECATION")
        kotlinOptions.jvmTarget = JdkMajorVersion.JDK_9_0.targetName
    }

    tasks.named("compileJava9Java", JavaCompile::class.java) {
        dependsOn(moduleOutputs)

        targetCompatibility = JavaVersion.VERSION_1_9.toString()
        sourceCompatibility = JavaVersion.VERSION_1_9.toString()
        configureTaskToolchain(JdkMajorVersion.JDK_9_0)

        // module-info.java should be in java9 source set by convention
        val java9SourceSet = sourceSets["java9"].java
        destinationDirectory.set(java9SourceSet.destinationDirectory.asFile.get().resolve("META-INF/versions/9"))
        options.sourcepath = files(java9SourceSet.srcDirs)
        val compileClasspath = configurations["java9CompileClasspath"]
        val moduleFiles = objects.fileCollection().from(moduleOutputs)
        val modulePath = compileClasspath.filter { it !in moduleFiles.files }
        classpath = objects.fileCollection().from()
        options.compilerArgumentProviders.add(
            Java9AdditionalArgumentsProvider(
                moduleName,
                moduleFiles,
                modulePath
            )
        )
    }
}

private class Java9AdditionalArgumentsProvider(
    private val moduleName: String,
    private val moduleFiles: FileCollection,
    private val modulePath: FileCollection
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf(
        "--module-path", modulePath.asPath,
        "--patch-module", "$moduleName=${moduleFiles.asPath}",
        "-Xlint:-requires-transitive-automatic" // suppress automatic module transitive dependencies in kotlin.test
    )
}

fun Project.configureFrontendIr() = tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        if (project.kotlinBuildProperties.useFirForLibraries) {
            freeCompilerArgs.add("-Xuse-k2")
            allWarningsAsErrors.set(false)
        } else if (project.kotlinBuildProperties.useFir) {
            freeCompilerArgs.add("-Xskip-prerelease-check")
        }

        val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames)
        if (renderDiagnosticNames) {
            freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
        }
    }
}

@JvmOverloads
fun Project.manifestAttributes(
    manifest: Manifest,
    component: String? = null,
    multiRelease: Boolean = false
) {
    manifest.attributes(
        "Implementation-Vendor" to "JetBrains",
        "Implementation-Title" to base.archivesName,
        "Implementation-Version" to project.rootProject.extra["buildNumber"] as String
    )

    if (component != null) {
        val kotlinLanguageVersion: String by rootProject.extra
        manifest.attributes(
            "Kotlin-Runtime-Component" to component,
            "Kotlin-Version" to kotlinLanguageVersion
        )
    }

    if (multiRelease) {
        manifest.attributes(
            "Multi-Release" to "true"
        )
    }
}
