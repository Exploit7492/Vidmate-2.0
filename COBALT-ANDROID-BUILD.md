# Cobalt Android — build + VidMate-derived redesign report

## What was requested
Continue the build of the downloaded `cobalt-android` project, using the Dex and
all findings from the VidMate (`com.nemo.vidmate`) reverse-engineering, to make it
follow VidMate's design.

## Artifact intake
- Downloaded `https://work-1-.../cobalt-android-complete.tar.gz`
  (105,294,593 bytes, HTTP 200).
- **SHA-256 verified**: `5a4096354d7e7cb5f0800560ef82670d468eefe5dd4a759e73bec721b45cc31c`
  matches the expected `5a4096354d7e…b45cc31c`.
- Unpacked to `cobalt-android/`.

## Build environment (fresh sandbox, set up from scratch)
- JDK 21 (`openjdk-21-jdk-headless`), `unzip` via apt.
- Android SDK at `/workspace/android-sdk`: cmdline-tools, platform-tools,
  `platforms;android-35`, `build-tools;35.0.0`.
- `local.properties` → `sdk.dir=/workspace/android-sdk`.

## Baseline (before changes)
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (3m12s).
- `./gradlew :app:testDebugUnitTest` → 44 tests, 0 failures.

## Work performed — VidMate-derived redesign
VidMate is a two-layer app (34.7 MB Tencent Shadow plugin host + a 21k-class
hot-updated plugin). The design facts recovered from the decompile that shaped
this pass: four-tab bottom bar, near-black surface stack, brand blue `#0099FF`
+ red `#EA3F40`, pill search bar, two-column "Recommended" tile grid, and a "Me"
profile/settings hub.

Changes (new/edited):
- **Shell** — `activity_main.xml`, `bottom_nav_menu.xml`, `nav_graph.xml`,
  `MainActivity.kt`: 3 tabs → VidMate's 4 (Home / Library / Shorts / Me);
  Settings moved under Me; flat black bottom bar (no Material pill indicator).
- **Home** — `ui/home/HomeScreen.kt`: pill search bar, "Quick download" paste
  card, "Recommended" grid; `HomeFeed.kt` tile spacing tightened to 8dp.
- **Library** (new `ui/library/`) — `LibraryScreen` / `LibraryViewModel` /
  `LibraryFragment`: Downloads / Files / History / Liked tabs.
- **Me** (new `ui/me/`) — `MeScreen` / `MeViewModel` / `MeFragment`: gradient
  profile header with counters + navigation rows.
- **Palette** — `values/colors.xml` + `ui/theme/Theme.kt`: VidMate hex values,
  kept in sync across XML and Compose.
- **Icons** — new `ic_nav_home/library/shorts/me`, `ic_search`, `ic_folder`,
  `ic_history`, `ic_chevron_right`, `ic_close`, `ic_play`, `ic_trash`, `ic_back`.
- **Settings** — `SettingsScreen.kt` gained a back affordance (no longer a tab).
- **Gradle** — `buildConfig = true` (Me screen shows `BuildConfig.VERSION_NAME`).

## Verification
- `./gradlew clean :app:assembleDebug :app:testDebugUnitTest` → **BUILD SUCCESSFUL
  (26s), 44 tests / 0 failures**.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (~208 MB).
- Confirmed new classes present: `LibraryScreenKt`, `MeScreenKt`, `HomeScreenKt`;
  new strings/resources (`nav_library`, `nav_me`, `ic_nav_*`) present in the APK.

## Deliberately out of scope
- Full Compose-Navigation migration / turning `viewBinding` off (Phase 26) —
  binary dialogs, `HistoryFragment`, the Shorts `ViewPager2` host and
  `activity_main.xml` remain Views/XML.
- VidMate's private vault, floating-window player, built-in browser, and its
  6 ad SDKs / 39 ad placements — not a design pass, not requested, and this app
  has no ad system.

## Reproduce
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/android-sdk
cd cobalt-android && ./gradlew :app:assembleDebug :app:testDebugUnitTest
```
