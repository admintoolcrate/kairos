# Kairos — working conventions

Kairos is a free "ride it out" companion for urges, cravings and panic attacks.
Two modes: **Urge** (10/20 minute countdown, breathing, distractions, outcome log)
and **Panic** (paced breathing, 5-4-3-2-1 grounding, reassurance cards, call someone).

## Non-negotiable principles
- Free. No ads, no accounts, no analytics, no crash reporting, no tracking SDKs.
- Fully offline. The *only* network use is the optional, opt-in, passphrase-encrypted
  Google Drive backup to the user's own `appDataFolder`. Never add another network call.
- `AD_ID` stays removed in the manifest. `allowBackup` stays false.
- Self-help tool, **not medical care or diagnosis**. No medical claims, no treatment
  promises, no "cures", no "clinically proven". The disclaimer (onboarding + settings)
  must say to call emergency services for chest pain or trouble breathing.

## Tone and safety rules (enforced in review)
- Nothing that raises anxiety: no loud or sudden sounds, no red, no alert styling, no
  shaking/flashing, no "hurry" language, no countdown that turns urgent near zero.
  The colour scheme's `error` role is a muted umber on purpose; do not add red.
- **Breathing never includes a hold.** `BreathingPhase` has only `Inhale` and `Exhale`.
  Exhale is always longer than inhale (default 4 in / 6 out); `BreathingEngine`
  enforces this. Don't add a hold phase, even as an option.
- The post-session check-in (intensity 1–5) is always optional, always skippable,
  never auto-shown twice, never required to leave.
- Copy is plain, warm, second person, short sentences. No exclamation marks.

## Panic screens must always open
- Panic lives in its own `PanicActivity` with `showWhenLocked`/`turnScreenOn`.
- App lock (biometric/PIN) gates **only** `MainActivity` content (history, settings,
  export). It must never be checked from `PanicActivity`, the tile, widget or shortcuts
  that lead to panic.
- Panic start must not wait on network, Room writes, or anything slow. Read cached prefs;
  fall back to built-in defaults if data isn't ready yet.
- Once started, Panic has no menus, no top bar, no settings. Very large text.

## Launch budget: under 2 seconds from widget / tile / lock screen
- `KairosApp.onCreate` stays empty apart from Hilt. No eager DB, no WorkManager init on
  the main thread, no Play Services calls at startup.
- Room and DataStore are lazy singletons; first reads happen off the main thread.
- `profileinstaller` is included; keep startup code paths simple.
- Measure on a device: `adb shell am start -W -n cc.toolcrate.kairos/app.kairos.panic.PanicActivity`
  (look at `TotalTime`, must be < 2000 ms cold) and the same for `app.kairos.MainActivity`.

## Accessibility
- Every interactive element ≥ 48 dp, with a content description or visible label.
- Phase changes in breathing are announced via a polite live region.
- Layouts must scroll and not clip at 200 % font scale.
- Reduced motion (system animator scale 0, or the in-app setting) replaces the pulsing
  circle with a still circle and text-only phase changes. Read `LocalReducedMotion`.

## Stack
Kotlin 2.2, Jetpack Compose (Material 3), Hilt (KSP), Room, DataStore Preferences,
Glance app widgets, kotlinx.serialization, AndroidX Biometric, Play Services Identity
(only for Drive authorisation).
AGP 8.11, Gradle 8.14, compile/target SDK 36, **min SDK 26**. Versions live in
`gradle/libs.versions.toml`.

## Layout (`app/src/main/java/app/kairos/`)
- `engine/` — pure Kotlin, no Android imports: `BreathingEngine`, `GroundingEngine`,
  `UrgeTimer`, `CrisisLines`. Every engine has JVM unit tests in `app/src/test`.
- `data/` — Room entities/DAOs, `Prefs` (DataStore), repositories.
- `backup/` — JSON codec, `Envelope` (PBKDF2 + AES-GCM), Drive client.
- `lock/` — app lock.
- `panic/` — `PanicActivity` and its screens.
- `entry/` — Quick Settings tile, Glance widget, deep links.
- `stagira/` — the intent bridge to Stagira (no server, explicit intent only).
- `ui/` — theme and feature screens (home, urge, checkin, onboarding, settings, history).

## Look
"Paper" palette: warm cream page, brown-black ink, light and dark. EB Garamond for
headings (display/headline/titleLarge), Inter for body and labels. Fonts are OFL
(licences in `third_party/`). Calm, bookish, generous whitespace, hairline dividers.

## Workflow
- Before every commit: `./gradlew assembleDebug testDebugUnitTest` must pass.
- Commit after each milestone on branch `claude/kairos-android-architecture-ap2l0b`.
- The SDK path comes from `local.properties` (`sdk.dir=`), which is git-ignored.
