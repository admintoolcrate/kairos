# Data safety form — suggested answers

Play Console → Policy → App content → Data safety. These follow what the app actually does
(see `privacy-policy.md`) and are deliberately conservative: when in doubt, declare.

## Overview

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes**, only when the user turns on Google Drive backup |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (HTTPS to Google Drive; the file is also encrypted on the phone first) |
| Do you provide a way for users to request that their data is deleted? | **Yes** (delete hidden app data in Google Drive, or uninstall) |

## Data types

What leaves the phone is the encrypted backup, and only to the user's own Drive. Declare each
as **Collected**, **not shared**, **optional**, purpose **App functionality** only:

| Category | Type | Why |
|---|---|---|
| Health and fitness | Health info | The session history (urges, optional 1 to 5 intensity) could be read as health information |
| Contacts | Contacts | Names and numbers of trusted contacts the user chose |
| App activity | Other user-generated content | Reassurance cards and other text the user wrote |

Everything else (location, identifiers, app interactions, diagnostics, financial info,
messages, photos, audio, web history): **not collected**. Advertising ID: **No**.

### Why declare at all

Play may treat data that only the user can decrypt differently, but this hasn't been checked
against the current form text. Declaring is the safe choice; it can be narrowed later.

### Why "not shared"

The backup goes to the user's own Google Drive at their request. The developer never receives it.
