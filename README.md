# Kairos

Calm tools for hard moments. Free, offline, no ads, no accounts.

Website: https://kairos.toolcrate.cc (source in `docs/`).

- **Urge mode** — a 10 or 20 minute countdown with a calm message, a short breathing
  guide and a few distractions. Afterwards, note whether the urge passed.
- **Panic mode** — slow paced breathing (longer out than in, never holding), a 5-4-3-2-1
  grounding walkthrough, reassurance cards in your own words, and one-tap calls to people
  you trust or a crisis line.

No ads, no accounts, no analytics. Everything stays on your phone unless you turn on the
encrypted backup to your own Google Drive.

Kairos is a self-help tool, not medical care. If you have chest pain or trouble
breathing, call your local emergency number.

## Getting there fast

Home-screen widget, a Quick Settings tile (works from the lock screen), and app
shortcuts for panic help or a 10/20 minute urge timer. Panic help is never behind
the optional app lock.

## Stagira

If [Stagira](https://github.com/admintoolcrate/stagira) is installed, Kairos can offer to
note an urge you rode out there. It uses an Android intent on your phone; nothing goes
through a server, and Stagira asks you to confirm.

## Build

```
./gradlew assembleDebug testDebugUnitTest
```

Needs the Android SDK (platform 36) with `sdk.dir` in `local.properties`.
See `CLAUDE.md` for conventions.
