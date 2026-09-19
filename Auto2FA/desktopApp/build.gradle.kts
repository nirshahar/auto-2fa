import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.example.auto2fa.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "auto2fa"
            packageVersion = "1.0.0"

            // Default is buildDir/compose/binaries (i.e. buried under desktopApp/build/) --
            // redirected to a common top-level directory so built installers land somewhere
            // predictable regardless of which module produced them.
            outputBaseDir.set(rootProject.layout.projectDirectory.dir("builtPackages"))

            windows {
                // jpackage's --win-menu default is off -- without a Start Menu shortcut, the
                // installed app has no entry for Windows' Start/taskbar search to index, so it
                // never shows up when you type "auto2fa" there no matter how long it's installed.
                menu = true
            }
        }

        // Ktor/Netty, BouncyCastle, and Skiko all lean on reflection ProGuard's default rules
        // don't account for -- packageReleaseMsi's proguardReleaseJars task fails outright
        // (883 unresolved references) without this. Same tradeoff androidApp's release build
        // already makes (isMinifyEnabled = false): a bigger unshrunk jar, but no risk of
        // ProGuard silently stripping something reflection depends on at runtime.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}