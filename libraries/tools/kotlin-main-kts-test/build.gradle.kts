
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
}

val kotlinxSerializationGradlePluginClasspath by configurations.creating

dependencies {
    testApi(project(":kotlin-main-kts"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testApi(kotlinStdlib("jdk8"))
    testApi(commonDependency("junit"))
    testApi(projectTests(":kotlin-scripting-compiler")) { isTransitive = false }
    testImplementation(project(":kotlin-compiler-embeddable"))
    kotlinxSerializationGradlePluginClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable")) { isTransitive = false }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist", ":kotlinx-serialization-compiler-plugin.embeddable:embeddable")
    val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
    doFirst {
        systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
    }
    workingDir = rootDir
    systemProperty("kotlin.script.base.compiler.arguments", "-Xskip-metadata-version-check")
    systemProperty("kotlin.script.test.base.compiler.arguments", "-Xskip-metadata-version-check")
}

projectTest(taskName = "testWithK2", parallel = true) {
    dependsOn(":dist", ":kotlinx-serialization-compiler-plugin.embeddable:embeddable")
    workingDir = rootDir
    val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
    systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
    systemProperty("kotlin.script.base.compiler.arguments", "-language-version 2.0 -Xskip-metadata-version-check")
    systemProperty("kotlin.script.test.base.compiler.arguments", "-language-version 2.0 -Xskip-metadata-version-check")
}
