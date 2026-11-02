# Dayblock — a private diary, one day at a time

**Dayblock** is an offline-first Android diary app with a clean, blocky,
pixel-inspired design. Every day you write is a block you place: write daily,
keep the streak alive, and watch your month turn green.

> First working version: **v0.1.0**

## Features in v0.1.0

- **Today editor** — one entry per day with a rotating writing prompt, title,
  mood picker (Great / Good / Okay / Low / Rough), tags and a live word count
- **Entries** — search across titles, text and tags; filter by mood or starred;
  star favourites; delete with confirmation from the editor
- **Calendar** — month view where written days show as green blocks; tap any
  day to preview it and jump into the editor; mood overview per month
- **Stats** — total entries ("blocks placed"), current day streak, entries
  this week, total words, and a mood breakdown
- **Private by design** — no account, no network permission, no analytics.
  Entries are stored only in the app's private on-device database (Room)

## Tech

- Kotlin + Jetpack Compose (Material 3)
- Room for local storage, one entry per calendar day
- MVVM with a `StateFlow` of entries and a small, unit-tested stats engine
  (streaks, weekly counts, word counts, mood totals)
- Blocky visual identity: squared corners, grass / dirt / stone / diamond
  palette, custom pixel-style launcher icon

## Build

With JDK 17 and Gradle 8.10.2+ (or Android Studio):

```bash
gradle assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Every push to `main` also builds the APK in GitHub Actions
(**Build APK** workflow) and uploads it as the `Dayblock-debug-apk` artifact.

## Updates and signing

Every build is signed with one fixed development key
(kept encoded as `signing/dayblock.keystore.b64` and decoded during the
build), and GitHub Actions builds get an
automatically rising version code from the workflow run number. That
means a new APK always installs **over** the installed app as an update —
same signature, higher version — with your entries kept. No need to
uninstall between versions.

The key is stored in this repo on purpose to keep personal builds
hassle-free, so **this repository must stay private**: anyone with the
key could sign a fake "update" that Android would accept. If Dayblock is
ever published, first switch to a proper release key kept outside the
repo (for example in GitHub Secrets); that switch would need one final
uninstall and reinstall.

## Roadmap

- PIN and biometric app lock
- Encrypted entries and encrypted backup / restore (Google Tink, Android Keystore)
- Daily reminder notification
- Export / import (Markdown and JSON)
- Year recap: your year in blocks
- Optional, bring-your-own-key AI weekly summary — off by default, always opt-in

## Privacy note for v0.1.0

Entries live in Android's app-private storage, which other apps cannot read,
but they are not yet encrypted at rest. Encryption is the first roadmap item;
until then, treat Dayblock like a paper diary kept in a locked drawer, not a
bank vault.

## License

MIT — see [LICENSE](LICENSE).
