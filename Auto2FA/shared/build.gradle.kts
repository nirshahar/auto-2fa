import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    
    android {
       namespace = "org.example.auto2fa.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
    }
    
    sourceSets {
        // Shared between the android and jvm targets only (not iOS/JS/etc, if those are ever
        // added) -- both compile to JVM bytecode, so java.security/javax.crypto code that can't
        // live in commonMain (which compiles against Kotlin-common metadata, not the JVM stdlib)
        // can live here once instead of being duplicated per target.
        val jvmAndroidMain by creating {
            dependsOn(commonMain.get())
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        androidMain.get().dependsOn(jvmAndroidMain)

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor)
            implementation(libs.ktor.netty)
            implementation(libs.ktor.server.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            // Netty (via Ktor) logs through SLF4J; without a provider on the runtime classpath
            // it silently no-ops and prints a one-time warning. slf4j-simple just logs to stderr.
            runtimeOnly(libs.slf4j.simple)
            // The JDK has no public API for building a self-signed X.509 certificate; BC does.
            implementation(libs.bouncycastle.bcpkix)
        }
        jvmMain.get().dependsOn(jvmAndroidMain)
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}