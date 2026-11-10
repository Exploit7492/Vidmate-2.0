# VidMate 2.0

An ad-free, native Android downloader **rebranded from the open-source Cobalt
engine**, built to reach feature parity with VidMate — plus the full
reverse-engineering analysis of the original VidMate app that drove the work.

> VidMate 2.0 is an independent project. It is **not** affiliated with, endorsed
> by, or derived from the original `com.nemo.vidmate` application or the cobalt
> team. The name and icon are used here for a personal, side-by-side comparison
> build only.

---

## What is in this repo

| Path | What it is |
|---|---|
| `cobalt-android/` | The VidMate 2.0 app source (Kotlin, Gradle, Material 3). Package `com.vidmate2.android`. |
| `VIDMATE-REVERSE-ENGINEERING.md` | Static reverse-engineering report of VidMate `com.nemo.vidmate` 6.0204. |
| `VIDMATE-VS-COBALT-MATRIX.md` | Row-by-row feature matrix (the authoritative one). |
| `VIDMATE-VS-COBALT-FULL.md` | The same comparison in the UX + capability framing. |
| `VIDMATE-VS-COBALT-EVERYTHING.md` | Single flat master table. |
| `VIDMATE-VS-COBALT-COMPARISON.md`, `-COMPARISON-FULL.md` | Earlier comparison passes, kept for history. |
| `COBALT-GAP-TASKBOARD.md` | The gap-closure task board (Phases A–J) and status. |
| `COBALT-FEATURES-COVERED.md` | The list of capabilities now covered. |
| `COBALT-ANDROID-BUILD.md` | Build notes. |

The large RE artifacts (`artifacts/`, `decompiled/`, `apktool/`) are not
committed — they are regenerated per `VIDMATE-REVERSE-ENGINEERING.md`.

## Building

```bash
cd cobalt-android
./gradlew :app:assembleDebug        # per-ABI + universal debug APKs
./gradlew :app:testDebugUnitTest    # 140 JVM tests
```

Debug APKs land in `cobalt-android/app/build/outputs/apk/debug/`:

- `app-arm64-v8a-debug.apk` — most modern phones
- `app-armeabi-v7a-debug.apk` — older 32-bit phones
- `app-x86_64-debug.apk` / `app-x86-debug.apk` — emulators
- `app-universal-debug.apk` — all four ABIs in one file

Requires Android SDK 35 and JDK 17.

## Install the debug build on a device (Termux)

Grab the per-ABI APK straight into `storage/downloads`:

```bash
# arm64 phones (most common)
curl -L -o /storage/downloads/VidMate2.0-arm64.apk \
  https://github.com/Exploit7492/Vidmate-2.0/releases/latest/download/app-arm64-v8a-debug.apk

# 32-bit arm phones
curl -L -o /storage/downloads/VidMate2.0-arm32.apk \
  https://github.com/Exploit7492/Vidmate-2.0/releases/latest/download/app-armeabi-v7a-debug.apk

# if unsure which, the universal build works everywhere
curl -L -o /storage/downloads/VidMate2.0-universal.apk \
  https://github.com/Exploit7492/Vidmate-2.0/releases/latest/download/app-universal-debug.apk
```

Then install:

```bash
pkg install android-tools        # provides `adb` (optional)
# or just tap the APK in your file manager
termux-open /storage/downloads/VidMate2.0-arm64.apk
```

The debug build installs side-by-side with the original VidMate
(`com.nemo.vidmate`) and with Cobalt (`com.cobalt.android`) because its package
id is `com.vidmate2.android`.

## Feature-parity snapshot

Counted from the comparison docs (buildable rows, excluding the monetisation
layer and the security-posture table):

| | VidMate | VidMate 2.0 |
|---|:--:|:--:|
| Covered | 132 | 130 ✅ (+4 ⚠️) |
| Not built | — | 4 |

The four remaining gaps are BitTorrent/magnet downloads, the account-only
YouTube state (needs an OAuth client), the 83 translated language files, and a
dislike button.
