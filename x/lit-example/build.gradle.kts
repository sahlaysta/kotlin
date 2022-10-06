plugins {
//    kotlin("js") version "1.8.255-SNAPSHOT"
    kotlin("js") version "1.8.20-dev-728"
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

dependencies {
    compileOnly(npm("lit", "2.3.1"))
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                devServer = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer(
                    open = mapOf(
                        "app" to mapOf(
                            "name" to "google chrome canary",
                        )
                    ),
                    static = devServer?.static
                )
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf("-Xes-next")
}
