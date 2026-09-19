# Handing this to friends (no paid certificate)

This covers building both apps for people outside your own dev machine.
There's no Play Store, no App Store, no paid code-signing certificate —
just the built artifacts, handed over directly, with the one-time warning
clicks that come with that.

## Android

```bash
./gradlew :androidApp:assembleRelease
```

Output: `androidApp/build/outputs/apk/release/androidApp-release.apk`.

This is signed with a real release key (`androidApp/release.keystore`,
generated locally, gitignored — see `androidApp/keystore.properties.example`
if you need to regenerate it) rather than the Android Studio debug key.
That matters for anyone who'll get updates over time: Android refuses to
install an update whose signature doesn't match the one already on the
device, and debug keystores aren't meant to be stable/shared. **Keep
`androidApp/release.keystore` and `androidApp/keystore.properties`** (they're
gitignored on purpose — never commit them) — losing them means any future
release can no longer update existing installs; you'd have to ship a new
package and have everyone uninstall/reinstall.

Hand the `.apk` to a friend however's convenient (Drive, a direct link,
Telegram, USB) — there's no store involved. Play Store isn't a realistic
option here anyway: apps that read SMS are restricted to apps registered as
the phone's default SMS handler, which this app isn't and doesn't want to
be.

**What your friend sees:** Android (Play Protect) will likely flag it as
"unrecognized app" or similar on install, since it's not from the Play
Store and has no install history — this is normal for any sideloaded app,
not specific to this one. They'll need to enable "Install unknown apps" for
whatever app they downloaded it through, and tap through one warning. No
way around this without a Play Store listing.

## Desktop (Windows)

```bash
./gradlew :desktopApp:packageReleaseMsi
```

Output: `builtPackages/main-release/msi/auto2fa-1.0.0.msi`
(also printed at the end of the task output) — all `desktopApp` packaging
tasks (`createDistributable`, `packageMsi`, `packageDmg`, `packageDeb`, and
their `release` variants) write under this shared top-level `builtPackages/`
directory (configured via `nativeDistributions.outputBaseDir` in
`desktopApp/build.gradle.kts`) rather than the default
`desktopApp/build/compose/binaries/`, so built installers land somewhere
predictable regardless of which module produced them. `builtPackages/` is
gitignored, same as `build/`.

The installer also creates a Start Menu shortcut (`windows { menu = true }`
in `desktopApp/build.gradle.kts`), so once your friend installs it, it
shows up when they search for "auto2fa" from the Start menu/taskbar — not
just as an icon they have to hunt for in Program Files.

This is **unsigned** — a real Windows code-signing certificate costs money
(recurring, ~$100+/yr from a CA) and isn't worth it for handing something
to a handful of friends. A self-signed certificate doesn't actually fix
this either: Windows SmartScreen's trust decision is based on Microsoft's
own reputation telemetry for a given signer, not just "is it signed at
all" — a self-signed cert mostly just swaps "Unknown Publisher" for your
own name in the dialog, it doesn't remove the warning.

**What your friend sees:** running the installer, Windows SmartScreen will
show "Windows protected your PC." They click **More info → Run anyway**.
One click, one time, per machine. Let them know to expect it so it doesn't
look like something's wrong.

Once installed, the app registers itself to start at login automatically —
see `desktopApp/README.md`. No extra setup needed there.

## Desktop (macOS), if you ever build the `.dmg`

`./gradlew :desktopApp:packageReleaseDmg` produces an unsigned/unnotarized
app. macOS Gatekeeper is stricter about this than Windows — a plain double
click on first launch will refuse to open it ("can't be opened because the
developer cannot be verified" / "app is damaged"). The friend needs to
**right-click the app → Open** (not double-click) and confirm — that
one-time override bypasses Gatekeeper for that app. No Apple Developer
account ($99/yr) needed for this, but it's a slightly less friendly click
than the Windows path, and Apple has been tightening how discoverable that
right-click-Open bypass is in newer macOS versions.

## Versioning

`desktopApp/build.gradle.kts`'s `packageVersion` and `androidApp`'s
`versionCode`/`versionName` are still hardcoded at `1.0.0`/`1`/`1.0`. If you
plan to actually send friends updates over time, bump these before each
build you hand out — nothing currently does it for you, and Android in
particular won't offer/allow an "update" install if `versionCode` doesn't
increase.
