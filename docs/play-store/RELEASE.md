# Getting Kairos into Internal testing on Google Play

Items marked **(you)** need your accounts or decisions; everything else is set up in this repo.
This follows Stagira's `docs/play-store/RELEASE.md` (same developer account, ToolCrate) and the
Play rules it cites as of October 2026. Re-check anything marked *verify* in Play Console itself.

Internal testing is the right first step: a release reaches the testers you list within minutes,
without store review, and the store listing doesn't have to be finished. It's also the first time
Kairos runs on real phones, so use it for the on-device checks at the end.

## 1. Create the app (you)

In [Play Console](https://play.google.com/console) (the ToolCrate account): **Create app** →
name *Kairos – Calm Tools*, **App**, **Free**. The package name `cc.toolcrate.kairos` is fixed
forever once the first bundle is uploaded.

## 2. Upload key (you, once)

On your own computer:

```sh
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Keep the file and passwords in your password manager; never commit them (`.gitignore` blocks
`*.jks` and `keystore.properties`). Then either build locally (copy `keystore.properties.example`
to `keystore.properties`, run `./gradlew bundleRelease`) or add these GitHub secrets
(**Settings → Secrets and variables → Actions**) so CI builds it:

- `UPLOAD_KEYSTORE_BASE64` — output of `base64 -w0 upload-keystore.jks`
- `UPLOAD_STORE_PASSWORD`, `UPLOAD_KEY_ALIAS` (`upload`), `UPLOAD_KEY_PASSWORD`

## 3. First release, by hand (you)

Play's upload API can't make an app's very first release, so the first bundle goes in manually:

1. **Actions → Build → Run workflow**, tick **release**. Download the `kairos-play-bundle` artifact.
2. Play Console → **Testing → Internal testing → Create new release**, accept **Play App Signing**,
   upload `app-release.aab`, save, roll out.
3. **Testers** tab: create an email list (up to 100 people), copy the **opt-in link** and send it.

### Later releases, automatically (optional)

Create a Google Cloud service account, invite it in Play Console (**Users and permissions**) with
release rights for Kairos, and add its JSON key as the secret `PLAY_SERVICE_ACCOUNT_JSON`. After
that, pushing a tag like `v0.1.1` builds, signs and uploads to Internal testing on its own.
Nothing goes further than Internal testing without you pressing **Promote release**.

## 4. App content (you, before testers can install — about 20 minutes)

Play asks for these even for internal tests. Suggested answers:

| Section | Answer |
|---|---|
| Privacy policy | Public URL of `privacy-policy.md` (step 5) |
| Ads | No |
| App access | All functionality available without special access |
| Content rating | All other app types; no violence, no user-to-user content, no purchases. If asked about self-harm or suicide content: the app only offers a support-line phone number, it doesn't depict or discuss them |
| Target audience | **18 and over** (keeps it out of the Families programme; your call) |
| Data safety | See [`data-safety.md`](data-safety.md) |
| Health apps | **Decision needed — see below** |
| Government, financial features, news | No |

### Health apps declaration — decision needed (you)

Every app must complete it. Stagira answered *no health features*, which was accurate for a
habit ledger; Stagira's notes record that ticking a mental-health category made Play treat it as
an organisation-only health app and reject it.

**Kairos is different: panic support and paced breathing are plausibly health features**, so
answering "none" would be inaccurate. Before submitting, open the declaration and read the
current category list and requirements (*verify*; I couldn't load Google's help pages from the
build environment). Likely fits are a stress-management / relaxation category or a mental and
behavioural health category. If the fitting category needs an organisation account, the options
are an organisation developer account (needs a D-U-N-S number) or adjusting the positioning;
don't misdeclare.

## 5. Privacy policy URL (you)

Play needs it at a public web address. `docs/` is the Kairos website (kairos.toolcrate.cc), which
also carries the policy:

1. In this repo, **Settings → Pages → Deploy from a branch**, branch `main`, folder `/docs`.
   This needs the work merged into `main`.
2. At the DNS provider for toolcrate.cc, add a `CNAME` record: `kairos` → `admintoolcrate.github.io`.
   (`docs/CNAME` already names the domain.) Once it resolves, tick **Enforce HTTPS** under Pages.
3. The policy is then at `https://kairos.toolcrate.cc/privacy/`, and the delete-your-data page at
   `https://kairos.toolcrate.cc/delete-data/`.

## 6. Google Drive backup (you, only if testers should try it)

Without this, everything works except Drive backup, which fails with "Google Drive couldn't be
reached. Nothing was changed."

In [Google Cloud Console](https://console.cloud.google.com/) (a project for Kairos):
1. Enable the **Google Drive API**.
2. **OAuth consent screen**: app name Kairos, support email, the privacy policy URL; add the
   scope `https://www.googleapis.com/auth/drive.appdata` (non-sensitive per Stagira's notes).
   While it's in *Testing*, add testers' Google accounts as test users.
3. **Credentials → Create OAuth client ID → Android**, package `cc.toolcrate.kairos`, once for
   each SHA-1:
   - Play's app-signing key: Play Console → **Setup → App signing** (after step 3)
   - the upload key, for locally built release bundles
   - the debug key in this repo, for debug builds:
     `E7:DA:B8:2A:9B:C9:B4:4F:AC:57:84:8E:64:CF:FE:ED:C2:C1:18:D5`

## 7. On-device checks for testers

These can't be done in CI. The list in `CLAUDE.md` ("Testing on a device") is the one to follow:
cold start under 2 seconds, Quick Settings tile over the lock screen, panic help never locked,
TalkBack, largest font. Also: haptic taps feel gentle, the dialer opens from the lock screen,
the widget's two buttons open the right place, and the support line shown is right where the
tester lives.

## Before closed testing or production

- New **personal** developer accounts must run a closed test with **at least 12 testers opted in
  for 14 days** before production access (organisation accounts are exempt). Internal testing
  doesn't count toward it.
- Check every number in `app/src/main/java/app/kairos/engine/CrisisLines.kt` against the
  service's own website.
- Store listing text: [`store-listing.md`](store-listing.md); graphics in `graphics/` and
  `screenshots/`.
