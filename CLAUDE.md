# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state — read this first

This repo is a very early, unsettled prototype, not a finished app. All code lives under `Auto2FA/`, a single Kotlin Multiplatform (KMP) Gradle project — an earlier duplicate native-Android project (`android-app/`) and a Chrome-extension-based relay (`chrome-extension/`) have both been removed; the desktop app + server described below is their replacement.

**Read [PLAN.md](./PLAN.md) before starting product work.** It has the full product spec (settings UI, RSA key exchange, SMS interception, server protocol, popup UI), the architecture decisions made for it, and a phased roadmap. The "Not yet built" note below is superseded by that plan — treat PLAN.md as the source of truth for what's next.

`Auto2FA/` has three Gradle modules:

- **`androidApp`** — the phone-side app (package `org.example.auto2fa`). Captures incoming SMS.
- **`desktopApp`** — the desktop-side app and server host (package `org.example.auto2fa`).
- **`shared`** — a KMP module (`android` + `jvm` targets) holding the Compose Multiplatform UI (`App.kt`, `PermissionContent.kt`, in `commonMain`), cross-platform data/wire types (`OtpPushRequest.kt`, `ServerApi.kt`, in `commonMain`), the RSA crypto (`RsaCrypto.kt`, in a custom `jvmAndroidMain` source set — see Architecture), and the server (`Server.kt`).

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

- **`shared`'s source-set layout is non-default — read this before adding code to `shared`.** Besides the usual `commonMain`/`androidMain`/`jvmMain`, there's a custom intermediate source set, `jvmAndroidMain` (wired via explicit `dependsOn()` in `shared/build.gradle.kts`, with `kotlin.mpp.applyDefaultHierarchyTemplate=false` set in `gradle.properties` because it conflicts with Kotlin's default hierarchy template). It exists because `java.security`/`javax.crypto` work identically on the `android` and `jvm` targets (both are JVM-flavored) but aren't visible from `commonMain` (which compiles against Kotlin-common metadata, not the JVM stdlib) — so code needing them but not tied to one specific platform belongs in `jvmAndroidMain`, not duplicated into both `androidMain` and `jvmMain`. `RsaCrypto.kt` (RSA/OAEP-SHA256 `encrypt`, using `kotlin.io.encoding.Base64` rather than `android.util.Base64` so it compiles for both targets) is the one thing there today; desktop-side `decrypt`/keygen belongs in this same file when it's built.
- **Wire-format types are shared, not duplicated per side:** `OtpPushRequest` (`shared/src/commonMain/kotlin/org/example/auto2fa/OtpPushRequest.kt`) is the `@Serializable` JSON body of `POST /push`. Both the Android client and (eventually) the desktop server should deserialize/serialize against this one type rather than each defining their own copy of the same shape.
- **Android app — implemented end-to-end except the desktop side:** `SmsReceiver` (`androidApp/src/main/kotlin/org/example/auto2fa/SmsReceiver.kt`) extracts a candidate OTP code from incoming SMS via regex, encrypts it with `RsaCrypto` (from `shared`, see above) using the public key from `SettingsRepository` (DataStore, `androidApp/.../Settings.kt` — Android-only; DataStore doesn't run on desktop JVM, and desktop's settings shape will be different anyway per PLAN.md), and POSTs it via `OtpPushClient` (Ktor client, `androidApp/.../OtpPushClient.kt`, implements `shared`'s `ServerApi` interface) — all inside `goAsync()` + a coroutine, fire-and-forget with a 5s timeout and no retry. `MainActivity` requests `RECEIVE_SMS` at runtime, then shows `SettingsScreen` (IP, port, public-key paste fields) once granted.
- **Desktop UI + server — not yet updated to match:** `desktopApp/src/main/kotlin/org/example/auto2fa/main.kt` calls `launchServer()` before opening the Compose desktop `Window`. The shared `App()` composable (`shared/src/commonMain/kotlin/org/example/auto2fa/App.kt`) is still the KMP template placeholder — no real desktop UI exists yet.
- **Server (`shared/Server.kt`) — payload shape is now stale:** `launchServer(port, onOtpReceived)` is `expect` in `commonMain`, implemented only on the JVM target (`shared/src/jvmMain/kotlin/org/example/auto2fa/Server.jvm.kt`) via Ktor/Netty exposing `POST /push`. **It still reads the original scaffold's plaintext `sender`/`message` form params — it has NOT been updated to parse `OtpPushRequest`, the shape the Android app now actually sends.** See PLAN.md's "Wire protocol" note and roadmap phase 4. The `androidMain` actual (`Server.android.kt`) throws `UnsupportedOperationException` — the server is desktop-only by design.
- **Not yet built:** desktop-side RSA keypair generation/persistence (add to `RsaCrypto` in `jvmAndroidMain` or a `jvmMain`-only actual if it turns out Android-incompatible), the settings screen showing the public key, `OtpPushRequest` parsing + decryption on `/push`, and the popup UI. See PLAN.md for the full roadmap.
