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

- **Android app — implemented end-to-end except the desktop side:** `SmsReceiver` (`androidApp/src/main/kotlin/org/example/auto2fa/SmsReceiver.kt`) extracts a candidate OTP code from incoming SMS via regex, encrypts it with `RsaCrypto` (RSA/OAEP-SHA256, `RsaCrypto.kt`) using the public key from `SettingsRepository` (DataStore, `Settings.kt`), and POSTs it via `OtpPushClient` (Ktor client, `OtpPushClient.kt`) — all inside `goAsync()` + a coroutine, fire-and-forget with a 5s timeout and no retry. `MainActivity` requests `RECEIVE_SMS` at runtime, then shows `SettingsScreen` (IP, port, public-key paste fields) once granted.
- **Desktop UI + server — not yet updated to match:** `desktopApp/src/main/kotlin/org/example/auto2fa/main.kt` calls `launchServer()` before opening the Compose desktop `Window`. The shared `App()` composable (`shared/src/commonMain/kotlin/org/example/auto2fa/App.kt`) is still the KMP template placeholder — no real desktop UI exists yet.
- **Server (`shared/Server.kt`) — payload shape is now stale:** `launchServer(port, onOtpReceived)` is `expect` in `commonMain`, implemented only on the JVM target (`shared/src/jvmMain/kotlin/org/example/auto2fa/Server.jvm.kt`) via Ktor/Netty exposing `POST /push`. **It still reads the original scaffold's plaintext `sender`/`message` form params — it has NOT been updated to parse the JSON body (`{"code": "<base64>"}`) the Android app now actually sends.** See PLAN.md's "Wire protocol" note and roadmap phase 4. The `androidMain` actual (`Server.android.kt`) throws `UnsupportedOperationException` — the server is desktop-only by design.
- **Not yet built:** desktop-side RSA keypair generation/persistence, the settings screen showing the public key, JSON parsing + decryption on `/push`, and the popup UI. See PLAN.md for the full roadmap.
