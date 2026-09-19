# Running the desktop server at boot (Windows)

The desktop app registers itself for Windows login automatically — no
manual Startup-folder or Task Scheduler setup needed. This doc explains how
that works, what it takes to trigger it, and how to undo it.

## How it works

`WindowsAutostart.ensureRegistered()` (`src/main/kotlin/org/example/auto2fa/WindowsAutostart.kt`)
runs first thing in `main()`. Every time the app launches as a **packaged
build** — either the raw app-image `.exe` from `createDistributable`, or an
installed MSI — it writes a value under
`HKCU\Software\Microsoft\Windows\CurrentVersion\Run` pointing at
`"<exe path>" --autostart`, so Windows starts it at every subsequent login.
This is the same mechanism most tray/background apps use (Slack, Discord,
Docker Desktop, etc.): per-user, no admin rights needed, and it shows up in
Windows' **Settings → Apps → Startup** for you to see or disable.

It does nothing when run via `./gradlew :desktopApp:run` or from an IDE —
it only fires when `jpackage.app-path` (a system property jpackage sets on
its generated native launchers) is present, which is only true for a
packaged build.

This lives in the app rather than the installer on purpose: jpackage's
Windows/MSI bundler has no built-in "run at login" option, and the Compose
Gradle plugin doesn't expose a way to customize the installer's generated
WiX sources to add one either (see the comment atop `WindowsAutostart.kt`
for details). Self-registering on first launch gets the same practical
result — install/run once, it's on for every boot after — without needing
to fight that unsupported plumbing.

The `--autostart` argument is what tells the app this particular launch was
Windows starting it at login, as opposed to you opening it yourself: `main`
only defaults the Settings window to hidden when that argument is present.
Launching any other way — the Start Menu shortcut, typing "auto2fa" into
taskbar search, double-clicking the exe — still opens the window as normal.
So after a reboot, the app starts straight into the tray with no window;
open it from the Start Menu or the tray icon whenever you actually want it.

Opening it again while it's already running (e.g. from the Start Menu,
after autostart already launched it hidden) doesn't start a second copy —
`SingleInstance` (`SingleInstance.kt`) detects the already-running instance
via a file lock and just asks it to show its window instead. Without this,
a second launch would otherwise crash trying to bind the OTP server's port,
which is already held by the first instance.

## Getting it registered

Just build and run the packaged app once:

```bash
./gradlew :desktopApp:createDistributable
```

then run the `.exe` at
`builtPackages/main/app/auto2fa/auto2fa.exe`
(all packaging output lands under the repo-root `builtPackages/` directory,
not `desktopApp/build/` — see `desktopApp/build.gradle.kts`'s
`outputBaseDir`) — or build and install the MSI instead
(`./gradlew :desktopApp:packageReleaseMsi`, output under
`builtPackages/main-release/msi/`) and launch the app from its Start Menu
shortcut. Either way, that one launch is enough to add the Run entry; no
further steps.

Only the installed MSI gets a Start Menu shortcut (and therefore shows up
in Windows' Start/taskbar search when you type "auto2fa") — `windows { menu
= true }` in `desktopApp/build.gradle.kts` turns that on; jpackage's
default is no shortcut at all. The plain `createDistributable` app-image
exe has no installer step, so it never gets one — run it directly or pin it
yourself if you want it launchable without a full install.

## Verifying it worked

Log off and back on (or reboot). No window should appear — find the
Auto2FA icon in the system tray instead (it may be under the "hidden
icons" chevron the first time; drag it out if you want it always visible).
Right-click it or double-click it to confirm "Open Settings" actually opens
the window. You can also check **Settings → Apps → Startup** in Windows,
where "Auto2FA" should now be listed as enabled.

Remember **Quit (from the tray menu) is the only thing that stops the
server** — closing the window does not.

## Disabling autostart

There's no in-app toggle for this yet. To turn it off:

- Windows Settings → Apps → Startup → switch "Auto2FA" off, **or**
- delete the registry value yourself:

  ```bash
  reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v Auto2FA /f
  ```

Either way, don't launch the app again afterward — since registration runs
on every launch, opening it once re-adds the entry.
