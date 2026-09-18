# Auto2FA — Product Plan

This is the product spec and implementation roadmap for Auto2FA, formalized for future sessions to work from. Nothing described here is implemented yet unless a phase is explicitly marked done — check the actual code before assuming a phase is complete.

## Product spec

**Android app (client):** a settings screen (Compose Multiplatform, Material3, modern look) with two required fields:
- the desktop server's IP (host:port)
- the desktop's RSA public key (pasted in as text)

When an SMS containing a 2FA code arrives, the app intercepts it automatically, extracts the code, encrypts it with the configured RSA public key, and sends it to the configured server. No user interaction is required for a successful push.

**Desktop app (server + client):** runs both a background HTTP server and a minimal UI, in one process.
- On first run it generates an RSA keypair and persists it. The private key never leaves the machine.
- A settings screen shows the current public key (for the user to copy into the Android app) — this is display-only, not editable.
- When the server receives a push, it decrypts the payload with the private key and shows a small popup with the plaintext code and a "Copy to clipboard" button.

## Architecture decisions

These are the concrete choices this plan commits to. Where the product spec didn't pin something down, a default is chosen and flagged in **Open questions** below rather than left ambiguous.

- **Crypto:** RSA-2048, OAEP padding with SHA-256 (not PKCS1v1.5). OTP codes are short (a handful of digits), well within OAEP's ~190-byte plaintext limit for a 2048-bit key, so no hybrid/AES envelope is needed.
- **Key material:**
  - Desktop generates the keypair via `java.security.KeyPairGenerator` on first launch if none is persisted, stores it locally (private key PKCS8 DER, public key X.509 DER, both base64), and never transmits the private key.
  - Public key exchange is manual for now: the user copies the base64 string shown in the desktop Settings screen and pastes it into the Android Settings screen. (QR-code pairing is a nice-to-have, see Open questions.)
  - Android crypto code only needs the public key and `Cipher`/`KeyFactory` to encrypt — no keypair generation on the phone.
- **Where crypto code lives:** `java.security`/`javax.crypto` APIs are available on both the `android` and `jvm` targets of `shared` (both are JVM-flavored targets), but not in `commonMain` (which compiles against Kotlin-common metadata, not the JVM stdlib). So crypto helpers must live in an actual per target, not `commonMain`. To avoid duplicating the same `java.security` code twice, prefer adding a shared intermediate source set (e.g. `shared/src/jvmCommon`, depended on by both `androidMain` and `jvmMain`) over copy-pasting — Kotlin Gradle plugin supports custom intermediate source sets for exactly this case.
- **Settings persistence:** a `Settings` data model (server IP, server port, public key string on Android; nothing user-editable on desktop besides what's shown) behind an `expect`/`actual` repository — DataStore Preferences on Android, a simple local JSON/properties file (e.g. under the user's home/app-data dir) on Desktop.
- **Wire protocol (finalized with the user):** `POST http://<serverIp>:<serverPort>/push`. Body is a small JSON envelope, `{"code": "<base64 RSA-OAEP ciphertext>"}`, `Content-Type: application/json` — chosen over a raw base64 text body for room to add fields later (timestamp, device id) without a breaking change. Implemented on the Android side via `kotlinx.serialization` + Ktor's `ContentNegotiation`/`json()` plugin (client) — see `androidApp/build.gradle.kts` and `OtpPushClient.kt`. **`shared/src/jvmMain/kotlin/org/example/auto2fa/Server.jvm.kt` has NOT been updated to match yet** — it still reads the old plaintext `sender`/`message` form params from the original scaffold. Updating it to parse this JSON body (and eventually decrypt it) is desktop-side work, tracked under Roadmap phase 4 below. Desktop should respond `200 OK` on successful decrypt, and should NOT pop up a window or throw on a payload that fails to decrypt (log and drop) since anyone on the LAN who knows the IP can POST to this endpoint.
- **Android networking:** a Ktor client using the `Android` engine (`ktor-client-android`), added to `androidApp`'s dependencies (not `shared` — this is Android-specific, unlike the server). Implemented in `androidApp/src/main/kotlin/org/example/auto2fa/OtpPushClient.kt`.
- **No retry on push failure (finalized with the user):** `SmsReceiver` fire-and-forgets the push with a short client timeout (5s) and logs+drops on failure. No `WorkManager` retry queue — `goAsync()`'s execution window is short anyway, and this was an explicit choice over building a persisted retry queue.
- **SMS → network handoff:** `BroadcastReceiver.onReceive` cannot block on network I/O. `SmsReceiver` must call `goAsync()` and do the extract/encrypt/POST work in a coroutine (or hand off to `WorkManager`), not inline on the main thread.
- **Desktop popup rendering:** the Ktor handler runs on Ktor's own thread pool, not the Compose UI thread. `onOtpReceived` should push the decrypted code into a UI-observable state holder (e.g. a `MutableSharedFlow<String>` or a `mutableStateListOf<String>` collected via `LaunchedEffect` in the Compose `application {}` block), which then opens a new small always-on-top `Window`/`DialogWindow` per code received — don't touch Compose state directly from the server thread.

## Roadmap

Each phase should be a self-contained, buildable step — verify `./gradlew :androidApp:compileDebugKotlin` and `./gradlew :desktopApp:compileKotlin` after each one.

1. ~~**Crypto & settings foundation (Android side)**~~ — done: `RsaCrypto.encrypt` (RSA/OAEP-SHA256, `androidApp/src/main/kotlin/org/example/auto2fa/RsaCrypto.kt`) and `Settings`/`SettingsRepository` (DataStore, `Settings.kt`). Desktop-side keypair generation/persistence is still open.
2. **Desktop settings UI** — replace the placeholder `App()` composable with a real settings screen showing the base64 public key and a copy button. Not started.
3. ~~**Android settings UI**~~ — done: `SettingsScreen.kt`, wired into `MainActivity.kt`.
4. **Server protocol update** — update `/push` in `Server.jvm.kt` to parse the JSON body described above (`{"code": "..."}`) instead of the old `sender`/`message` form params, decrypt on receipt, and drop (don't crash on) payloads that fail to decrypt. Not started — this is the main remaining gap for an end-to-end flow: the Android app now sends this shape, but the desktop route doesn't parse it yet.
5. ~~**Android SMS → network wiring**~~ — done: `SmsReceiver.kt` extracts the OTP code from the SMS body via regex, encrypts it, and POSTs it via `OtpPushClient` inside a `goAsync()` + coroutine, fire-and-forget with no retry.
6. **Desktop popup UI** — wire `onOtpReceived` to the shared state holder described above; render the popup window with the code and a working "Copy to clipboard" button. Not started.
7. **Polish (not required for MVP)** — input validation on the IP/port fields and connection-status feedback; encrypting the persisted private key at rest; TLS for the `/push` call; QR-code-based key pairing instead of manual paste; retry behavior.

## Open questions

Defaults were picked for these so the plan isn't blocked, but they're worth confirming before or during implementation:

- **Port number** for the server — not specified; `Server.jvm.kt` currently defaults to `8080`, kept as-is unless told otherwise.
- **Manual key paste vs. QR pairing** — spec just says "a configured public encryption key," read as a manual paste for MVP; QR is listed as a polish item, not committed to.
- **OTP extraction heuristic** — regex-based digit extraction won't handle every SMS format (alphanumeric codes, multiple number-like substrings in one message). Treated as an iterative heuristic, not a solved problem.
- **No auth beyond encryption on `/push`** — anything on the same network as the desktop can POST to it; only someone with the private key gets a meaningful decrypt, but there's no rate-limiting or replay protection. Flagged, not addressed, in this plan.
