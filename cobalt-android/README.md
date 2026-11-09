# Cobalt Android

A native Android client for [cobalt.tools](https://cobalt.tools) — Home feed,
Shorts feed, share sheet, clipboard trigger, and an on-device download queue.

> **Not affiliated with or endorsed by the cobalt team.** Downloads are
> performed by a cobalt instance; all credit for the actual media extraction
> goes to [imputnet/cobalt](https://github.com/imputnet/cobalt). The feeds use
> YouTube's public/internal endpoints, the same sources the cobalt project
> itself targets.

---

## What it does

- **Home feed** — a real, YouTube-backed feed of regular-length videos with
  thumbnails, titles, channels and view counts. Tap a tile to stream it inline
  (Media3/ExoPlayer) or use its "download" action to send it through cobalt.
- **Paste a link** — paste any supported URL; the app resolves formats via
  cobalt and shows a picker.
- **Shorts feed** — a vertical, swipeable short-form feed (NewPipe/Invidious/
  Innertube sources merged and cached), with like/save/share chrome.
- **Share from any app** — Share a link from YouTube, TikTok, Instagram, X,
  Reddit, etc. to Cobalt; the URL is resolved and the picker appears.
- **Clipboard trigger** — copy a link, open the app, tap the snackbar action.
- **Long-press shortcuts** — "Paste & Download" and "Open Queue".
- **Native download queue** — active progress, history, open/retry, and
  FFmpeg-based quality/format transcode tiers.
- **Settings** — theme (light/dark/dynamic), cobalt API key, audio-only mode,
  clipboard trigger, download location, Invidious instance pool, and the
  Shorts search-query pool.

Completed downloads save to **Downloads/Cobalt/** on your device.

---

## Install

Debug/sideload build; no Play Store listing.

1. Build (below) or grab `app-debug.apk`.
2. On your device: **Settings → Apps → Special app access → Install unknown
   apps** → enable for your file manager.
3. Open the APK and install.
4. On first launch: grant notification permission and allow the battery
   optimization exemption (keeps downloads running in the background).

minSdk 26 (Android 8.0) and up.

---

## Build from source

Requirements: Android SDK (platform 35, build-tools 35.0.0), JDK 21 (the
project targets JVM 17), internet for the first Gradle sync.

```bash
git clone https://github.com/Andro-Meta/cobalt-android
cd cobalt-android
# point Gradle at your SDK if it isn't picked up automatically:
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests with:

```bash
./gradlew :app:testDebugUnitTest
```

---

## Tech stack

- Kotlin · Jetpack Compose + Material 3 · Room · ViewModel/LiveData · WorkManager
- Media3/ExoPlayer for inline + local playback
- NewPipeExtractor, Invidious, and Innertube for feed extraction
- ffmpeg-kit (LTS 16 KB fork) for on-device transcode tiers
- OkHttp → MediaStore for downloads
- Coil for remote image loading
- AGP 8.5.2 · Gradle 8.7 · Kotlin 1.9.24 · minSdk 26 · targetSdk 35

---

## Known limitations

- **No signed/Play Store release** — sideload only.
- Feeds depend on YouTube's internal endpoints and public Instances, which
  change over time; when a source drifts the feed degrades to its Room cache
  rather than emptying (each source is independently failable).
- Light and dark themes currently render with the same palette (the app ships
  a single dark palette); "dynamic" is the option that visibly changes colour
  on Android 12+.
- Cancelling an in-flight FFmpeg transcode is not wired yet (it is shown in
  the queue without a cancel button rather than one that does nothing).

---

## Credits

Built on [cobalt](https://github.com/imputnet/cobalt) by
[imputnet](https://github.com/imputnet) and contributors; short-form extraction
via [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor); the
16 KB-page-size ffmpeg-kit fork is
[JamaisMagic/ffmpeg-kit-16KB](https://github.com/JamaisMagic/ffmpeg-kit-16KB).
