/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.JavaVersion
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.File

/**
 * Interface indicating Kotlin task is using [Gradle JDK toolchain](https://docs.gradle.org/current/userguide/toolchains.html)
 * feature to run task action where parts of JDK are required (for example to compile Kotlin Java target). Different tasks could
 * use different JDK versions which could be configured using this interface
 * via [Gradle Tasks API](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:configuring_tasks).
 */
interface UsesKotlinJavaToolchain : Task {

    /**
     * Kotlin task configured JDK toolchain.
     *
     * Never returns `null`.
     */
    @get:Nested
    val kotlinJavaToolchainProvider: Provider<out KotlinJavaToolchain>

    /**
     * Helper shortcut to get [KotlinJavaToolchain] from [kotlinJavaToolchainProvider] without calling `.get()` method.
     */
    @get:Internal
    val kotlinJavaToolchain: KotlinJavaToolchain
        get() = kotlinJavaToolchainProvider.get()
}

/**
 * Kotlin JDK toolchain.
 *
 * Provides ways to configure JDK either via [JdkSetter] by providing path to JDK directly or
 * via [JavaToolchainSetter] using configured [JavaLauncher].
 *
 * Configured JDK Java version is exposed as task input to distinguish stored in the
 * [build cache](https://docs.gradle.org/current/userguide/build_cache.html) task outputs produced using different JDK versions.
 */
interface KotlinJavaToolchain {

    /**
     * Configured JDK toolchain [JavaVersion].
     *
     * This property represents the configured JDK toolchain [JavaVersion] used for the task.
     * If toolchain does not explicitly set, it defaults to the version of the JDK with which Gradle is running.
     */
    @get:Input
    val javaVersion: Provider<JavaVersion>

    /**
     * Provides access to the [JdkSetter] to configure JDK toolchain for the task using explicit JDK location.
     */
    @get:Internal
    val jdk: JdkSetter

    /**
     * Provides access to the [JavaToolchainSetter] to configure JDK toolchain for the task
     * using [Gradle JDK toolchain](https://docs.gradle.org/current/userguide/toolchains.html).
     */
    @get:Internal
    val toolchain: JavaToolchainSetter

    /**
     * Provides methods to configure task using explicit JDK location.
     */
    interface JdkSetter {
        /**
         * Configures JDK toolchain to use JDK located under [jdkHomeLocation] absolute path. Major JDK version from [javaVersion]
         * is considered as task input to avoid Gradle [build cache](https://docs.gradle.org/current/userguide/build_cache.html)
         * hits for different versions.
         *
         * *Note*: project build will fail on providing here JRE instead of JDK!
         *
         * @param jdkHomeLocation path to JDK location on the machine
         * @param jdkVersion JDK version located under [jdkHomeLocation] path
         */
        fun use(
            jdkHomeLocation: File,
            jdkVersion: JavaVersion
        )

        /**
         * Configures JDK toolchain to use JDK located under [jdkHomeLocation] absolute path. Major JDK version from [javaVersion]
         * is considered as task input to avoid Gradle remote [build cache](https://docs.gradle.org/current/userguide/build_cache.html)
         * hits for different versions.
         *
         * *Note*: project build will fail on providing here JRE instead of JDK!
         *
         * @param jdkHomeLocation path to JDK location on the machine
         * @param jdkVersion JDK version located under [jdkHomeLocation] path, accepts any type accepted by [JavaVersion.toVersion]
         * @throws IllegalArgumentException when the provided value cannot be converted.
         */
        fun use(
            jdkHomeLocation: String,
            jdkVersion: Any
        ) = use(File(jdkHomeLocation), JavaVersion.toVersion(jdkVersion))
    }

    /**
     * Provides methods to configure task using [Gradle JDK toolchain](https://docs.gradle.org/current/userguide/toolchains.html).
     */
    interface JavaToolchainSetter {
        /**
         * Configures JDK toolchain for a task using a [JavaLauncher] obtained from [org.gradle.jvm.toolchain.JavaToolchainService] via
         * [org.gradle.api.plugins.JavaPluginExtension] extension.
         */
        fun use(
            javaLauncher: Provider<JavaLauncher>
        )
    }
}
