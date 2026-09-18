# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state — read this first

This repo is a very early, unsettled prototype, not a finished app. All code lives under `Auto2FA/`, a single Kotlin Multiplatform (KMP) Gradle project — an earlier duplicate native-Android project (`android-app/`) and a Chrome-extension-based relay (`chrome-extension/`) have both been removed; the desktop app + server described below is their replacement.

**Read [PLAN.md](./PLAN.md) before starting product work.** It has the full product spec (settings UI, RSA key exchange, SMS interception, server protocol, popup UI), the architecture decisions made for it, and a phased roadmap. The "Not yet built" note below is superseded by that plan — treat PLAN.md as the source of truth for what's next.

`Auto2FA/` has three Gradle modules:

- **`androidApp`** — the phone-side app (package `org.example.auto2fa`). Captures incoming SMS.
- **`desktopApp`** — the desktop-side app and server host (package `org.example.auto2fa`).
- **`shared`** — a KMP module (`android` + `jvm` targets) holding the Compose Multiplatform UI (`App.kt`, `Greeting.kt`, both in `commonMain`) and the server (`Server.kt`).

## Commands

Run from inside `Auto2FA/` (this is the Gradle project root):

```bash
./gradlew :androidApp:assembleDebug   # build the Android debug APK
./gradlew :androidApp:compileDebugKotlin  # quick compile check, no full build
./gradlew :desktopApp:run             # run the desktop app (also starts the server)
./gradlew :desktopApp:hotRun --auto   # run the desktop app with hot reload
./gradlew :desktopApp:compileKotlin   # quick compile check for desktopApp + shared's jvm target
./gradlew test                        # run all tests
```

## Architecture

- **SMS capture (Android):** `SmsReceiver` (a `BroadcastReceiver` registered for `android.provider.Telephony.SMS_RECEIVED`) in `androidApp/src/main/kotlin/org/example/auto2fa/SmsReceiver.kt` currently only logs incoming SMS to Logcat — it does not yet parse OTPs or forward them anywhere. This is the extension point for any "detect + forward the 2FA code" logic; it still needs to be wired up to actually POST to the desktop server below.
- **Android UI:** `MainActivity` requests `RECEIVE_SMS` permission at runtime, then delegates to the shared Compose `App()` composable (`shared/src/commonMain/kotlin/org/example/auto2fa/App.kt`), which is currently just the KMP project template's placeholder UI — no real screens exist yet.
- **Desktop UI + server:** `desktopApp/src/main/kotlin/org/example/auto2fa/main.kt` calls `launchServer()` before opening the Compose desktop `Window`, so the server starts alongside the UI in one process.
- **Server (`shared/Server.kt`):** `launchServer(port, onOtpReceived)` is declared `expect` in `commonMain` and only actually implemented on the JVM target (`shared/src/jvmMain/kotlin/org/example/auto2fa/Server.jvm.kt`), where it starts a Ktor/Netty server exposing `POST /push` (form params `sender`, `message`) and invokes the callback with an `OtpPush`. The `androidMain` actual (`Server.android.kt`) throws `UnsupportedOperationException` — the server is desktop-only by design; the phone is meant to be the client that POSTs to it, not the host. Ktor deps (`ktor`, `ktor-netty` in `gradle/libs.versions.toml`) are added only to `shared`'s `jvmMain` source set for this reason — adding them to `commonMain` would break the Android target, since `ktor-server-netty-jvm` isn't resolvable there.
- **Not yet built:** the Android side has no networking code to actually POST captured SMS/OTP text to the desktop server, and there's no pairing/auth/discovery mechanism (e.g. how the phone learns the desktop's IP). This is the main open design problem.
