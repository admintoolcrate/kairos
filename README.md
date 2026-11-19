# Kairos

A calm, free, offline companion for riding out urges, cravings and panic attacks.

- **Urge mode** — a 10 or 20 minute countdown with a calm message, a short breathing
  guide and a few distractions. Afterwards, note whether the urge passed.
- **Panic mode** — slow paced breathing (longer out than in, never holding), a 5-4-3-2-1
  grounding walkthrough, reassurance cards in your own words, and one-tap calls to people
  you trust or a crisis line.

No ads, no accounts, no analytics. Everything stays on your phone unless you turn on the
encrypted backup to your own Google Drive.

Kairos is a self-help tool, not medical care. If you have chest pain or trouble
breathing, call your local emergency number.

## Build

```
./gradlew assembleDebug testDebugUnitTest
```

Needs the Android SDK (platform 36) with `sdk.dir` in `local.properties`.
See `CLAUDE.md` for conventions.
