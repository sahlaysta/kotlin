import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))

    implementation(project(":kotlin-annotation-processing-compiler"))
    embedded(project(":kotlin-annotation-processing-compiler")) { isTransitive = false }


//    api(project(":compiler:util"))
//    api(project(":compiler:cli"))
//    api(project(":compiler:backend"))
//    api(project(":compiler:frontend"))
//    api(project(":compiler:frontend.java"))
//    api(project(":compiler:plugin-api"))
//
//    implementation(project(":analysis:analysis-api-standalone"))
//    embedded(project(":analysis:analysis-api-standalone")) {
//        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
//        exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
//    }
//
//    implementation(project(":kotlin-annotation-processing-compiler"))
//    embedded(project(":kotlin-annotation-processing-compiler")) {
//        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
//        exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
//    }
//
//    implementation(project(":compiler:backend.jvm.entrypoint"))
//
//    compileOnly(toolsJarApi())
//    compileOnly(intellijCore())
//    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
//
//    testImplementation(intellijCore())
//    testRuntimeOnly(intellijResources()) { isTransitive = false }
//
//    testApiJUnit5()
//    testApi(projectTests(":compiler:tests-common-new"))
//    testApi(projectTests(":compiler:test-infrastructure"))
//    testApi(projectTests(":compiler:test-infrastructure-utils"))
//    testApi(projectTests(":kotlin-annotation-processing-compiler"))
//    testApi(projectTests(":kotlin-annotation-processing-cli"))
//
//    testApi(projectTests(":kotlin-annotation-processing-base"))
//
//    testCompileOnly(toolsJarApi())
//    testRuntimeOnly(toolsJar())
//    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
//    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

kaptTestTask("test", JavaLanguageVersion.of(8))
kaptTestTask("testJdk11", JavaLanguageVersion.of(11))
kaptTestTask("testJdk17", JavaLanguageVersion.of(17))

fun Project.kaptTestTask(name: String, javaLanguageVersion: JavaLanguageVersion) {
    val service = extensions.getByType<JavaToolchainService>()

    projectTest(taskName = name, parallel = true) {
        useJUnitPlatform {
            excludeTags = setOf("IgnoreJDK11")
        }
        workingDir = rootDir
        dependsOn(":dist")
        javaLauncher.set(service.launcherFor { languageVersion.set(javaLanguageVersion) })
    }
}

publish()
runtimeJar()
sourcesJar()
javadocJar()


allprojects {
    tasks.withType(KotlinCompile::class).all {
        kotlinOptions {
            freeCompilerArgs += "-Xcontext-receivers"
        }
    }
}
