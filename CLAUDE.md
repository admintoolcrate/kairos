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
- Never tell the person what they're experiencing ("this is a panic attack", "a false
  alarm") and never promise they're safe or that it will pass: Kairos can't know.
  Built-in copy talks about breathing, noticing and waiting, in the person's own voice.
  `CopyTest` keeps those phrases out; the person's own cards may say anything.

## Tone and safety rules (enforced in review)
- Nothing that raises anxiety: no loud or sudden sounds, no red, no alert styling, no
  shaking/flashing, no "hurry" language, no countdown that turns urgent near zero.
  The colour scheme's `error` role is ink on purpose; do not add red.
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
Kairos follows the ToolCrate family look shared with Stagira, Lyceum and Telos:
**read `docs/family-look.md`** (the same file in all four repos; change it in all four).
In short: Mono palette (`ui/theme/Color.kt`: black on white, true black at night, no red),
EB Garamond only for display moments (name, timer, panic text, passages, `ScreenTitle`),
Inter for everything else, 24 dp cards and pill buttons, hairline dividers. Fonts are OFL
(licences in `third_party/`). Play icon and feature graphic come from `tools/family_graphics.py`.

## Workflow
- Before every commit: `./gradlew assembleDebug testDebugUnitTest` must pass.
- Behaviour tests in `app/src/test/.../ui` drive the real activities under Robolectric + Hilt
  (panic never locked, no breath hold, check-in skippable, settings locked, urge flow).
  The Compose clock is manual (`mainClock.autoAdvance = false`) because the breathing
  guide animates forever; use `settle()` from `ui/Harness.kt` after each action.
- After visual changes, draw and look at the screens (light, dark, 2x font):
  `./gradlew testDebugUnitTest -Pscreenshots=true --tests 'app.kairos.screenshots.*'`
  → `app/build/screenshots/`.
- Commit after each milestone on branch `claude/kairos-android-architecture-ap2l0b`.
- The SDK path comes from `local.properties` (`sdk.dir=`), which is git-ignored.

## Releasing
- Play steps, policy answers and what only the owner can do: `docs/play-store/RELEASE.md`.
  Privacy policy, Data safety and store listing drafts sit beside it; keep them true to the
  code (if the app starts storing or sending something new, update them in the same change).
- `versionCode` comes from the CI run number; bump `versionName` by hand.
- Signing: `keystore.properties` locally or the `UPLOAD_*` GitHub secrets. `app/debug.keystore`
  is a public debug-only key (password `android`) so Drive sign-in can be registered for
  debug builds; never sign a release with it.
- Store screenshots (1080x1920): `--tests 'app.kairos.screenshots.StoreScreenshots'`
  with `-Pscreenshots=true`, then copy `app/build/store-screenshots/` to
  `docs/play-store/screenshots/`.

## Stagira bridge (contract)
- Kairos sends an explicit intent: action `cc.toolcrate.stagira.action.RECORD_URGE`,
  package `cc.toolcrate.stagira`, extras `source` = `"kairos"`, `at` = epoch millis.
- Stagira's `RecordUrgeActivity` always asks the person to confirm and pick a goal,
  respects Stagira's app lock, accepts only today/yesterday, and returns only
  `RESULT_OK`/`RESULT_CANCELED`. Kairos marks `sentToStagira` only on `RESULT_OK`.
- The option is offered only when Stagira resolves the intent and the setting is on.
  Never send anything else to Stagira, and never add a server.

## Testing on a device (can't be done in CI here)
- Cold start: `adb shell am force-stop cc.toolcrate.kairos` then
  `adb shell am start -W -n cc.toolcrate.kairos/app.kairos.panic.PanicActivity`; `TotalTime` < 2000.
- Lock the phone, open Quick Settings, tap Kairos: panic help must appear over the lock screen.
- Turn on app lock, then confirm panic help, urge mode, the tile, widget and shortcuts all
  open without asking for the PIN, while History and Settings do ask.
- TalkBack: breathing phases are announced ("Breathe in" / "Breathe out"), the per-second
  count is not. Font size at largest: nothing clipped, everything scrolls.

## Website (kairos.toolcrate.cc)
- `docs/` is the public website, served by GitHub Pages from `main` (`docs/CNAME`). It is Jekyll:
  `index.html` (home), `support.html`, `404.html`, the privacy policy and delete-your-data pages,
  all in `_layouts/default.html`; names, links and the family list live in `docs/_config.yml`.
- **Keep it true to the app.** When a change adds, removes or renames something a person would
  notice (a feature, a screen, a setting, what's stored or sent, supported Android versions,
  Play availability), update the home page's features, steps, FAQ or private panel in the same
  change, along with the README, store listing and privacy policy where they say the same thing.
  Internal refactors and fixes need no site change.
- The phone on the home page shows `docs/play-store/screenshots/` directly; when you redraw the
  store screenshots, check each slide's alt text and caption (`data-name`, `data-desc`) still fits,
  and add or drop a slide (and its dot) if the set changes.
- Follow the family look and voice (docs/family-look.md and the tone rules above). Quote only built-in copy or
  verified passages; no claims the app doesn't back up.
- The site obeys the same safety rules as the app: no medical claims, no promises that a feeling
  will pass, no red, no exclamation marks, and the self-help disclaimer stays on the home page.
- `docs/assets/site.css`, `site.js` and `_layouts/default.html` are the same in Lyceum, Telos and
  Kairos; change them in all three. Preview: `cd docs && jekyll build` (or `jekyll serve`), then
  look at the home page in light, dark and at phone width.
