package org.example.auto2fa

/**
 * Registers the app to launch at Windows login by writing a value under
 * `HKCU\...\Run` -- the same mechanism most mainstream tray/background apps use (it's per-user,
 * needs no elevation, and shows up in Windows' own Settings > Apps > Startup for the user to
 * manage or disable).
 *
 * This is done by the *app* on launch rather than by the installer: jpackage's generated MSI
 * (which `packageMsi`/`packageReleaseMsi` produce) has no supported option for adding a
 * Run-key/shortcut/scheduled-task entry, and the Compose Gradle plugin doesn't expose jpackage's
 * `--resource-dir` for customizing the installer's WiX sources either -- it's wired internally to
 * a plugin-managed scratch directory, not something a build script can redirect. Self-registering
 * on first launch gets the same end result (installed on any machine -> starts on boot) without
 * depending on that unsupported internal wiring.
 */
object WindowsAutostart {
    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val VALUE_NAME = "Auto2FA"

    /**
     * No-op unless both:
     *  - running on Windows, and
     *  - running as the actual installed jpackage launcher -- `jpackage.app-path` is a system
     *    property jpackage-generated native launchers set to their own exe path; it's absent
     *    under `gradlew run`/the IDE, which keeps this from touching the registry in dev.
     *
     * Re-registering on every launch is intentional and cheap (a single registry write) -- it
     * self-heals the entry if the app was reinstalled to a different location.
     */
    fun ensureRegistered() {
        if (!System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)) return
        val exePath = System.getProperty("jpackage.app-path") ?: return

        try {
            ProcessBuilder(
                "reg", "add", RUN_KEY,
                "/v", VALUE_NAME,
                "/t", "REG_SZ",
                "/d", "\"$exePath\"",
                "/f",
            ).start().waitFor()
        } catch (_: Exception) {
            // Best-effort -- failing to self-register for autostart shouldn't stop the app from
            // starting normally.
        }
    }
}
