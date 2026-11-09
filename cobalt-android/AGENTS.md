# cobalt-android — agent notes

## Build environment (VERIFIED working — Session 15)
This repo now has a real, working Android build in this sandbox. It compiled
for the first time ever here (BUILD SUCCESSFUL, 3m3s), which also confirms the
ffmpeg-kit `6.1.4` pin and the Phase 22 Compose toolchain resolve and compile.

Toolchain install (once per fresh sandbox):
```bash
sudo apt-get install -y openjdk-21-jdk-headless unzip
mkdir -p /workspace/android-sdk/cmdline-tools && cd /workspace/android-sdk
curl -sSL -o t.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q t.zip -d cmdline-tools && mv cmdline-tools/cmdline-tools cmdline-tools/latest && rm t.zip
export ANDROID_HOME=/workspace/android-sdk
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
echo "sdk.dir=/workspace/android-sdk" > /workspace/cobalt-android/local.properties
```
Build:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/android-sdk
cd /workspace/cobalt-android && ./gradlew :app:assembleDebug --console=plain
# unit tests: ./gradlew :app:testDebugUnitTest --console=plain
```
JDK 21 works with Gradle 8.7 + AGP 8.5.2 (do NOT downgrade to 17; 17 isn't in apt here).

## Project shape
- Kotlin. Compose is now the primary UI toolkit. The app shell is a VidMate-style
  four-tab bottom bar — Home / Library / Shorts / Me — with Settings reached from
  the Me tab (not the bar), plus a floating download-queue FAB. New Compose screens
  since Session 16: `ui/library/` (LibraryScreen/ViewModel/Fragment), `ui/me/`
  (MeScreen/ViewModel/Fragment), and a redesigned `ui/home/HomeScreen` (pill search
  bar + "Quick download" card + "Recommended" grid). `viewBinding` is still on
  because `MainActivity`/`activity_main.xml`, the binary dialogs
  (`ResolutionPickerDialog`, `QualitySelectionSheet`, `VideoPlayerDialogFragment`),
  `HistoryFragment`, and the Shorts `ViewPager2` host are still Views/XML — Phase 26
  (full Compose Navigation + `viewBinding` off) is only reachable once those migrate.
- Palette (XML `colors.xml` + Compose `ui/theme/Theme.kt`) mirrors the VidMate
  decompile: near-black surfaces, brand blue `#0099FF` accent, red `#EA3F40`
  secondary. Both are kept in sync so Compose and still-XML surfaces match.
- `buildConfig = true` is enabled in `app/build.gradle.kts` (MeScreen shows
  `BuildConfig.VERSION_NAME`).
- `HANDOVER.md` = session history; `ARCHITECTURE.md` = the 27-phase plan (no Phase 21).
- Compose screens are hosted incrementally via `ComposeView` inside existing
  Fragments (the standard adoption path), not a big-bang rewrite.
- `feed/` = the Home feed pipeline (model/source/repository/db), distinct from
  `shorts/` which serves the Shorts feed. Home items resolve a stream on demand
  (`InnertubeStreamResolver`); Shorts use the merged multi-source pipeline.
- No LICENSE file. echo-music reference is GPLv3 — do not copy code from it.
- Tests: `./gradlew :app:testDebugUnitTest`. Feed-parser tests use a test-only
  `org.json:json` (Android's bundled stubs throw off-device).

## Remote access (BLOCKED — Session 15)
There is no git repo/remote in `/workspace/cobalt-android`; it is the copy the
user uploaded from Termux. Attempts to mirror-clone the upstream
`github.com/Zapier-codes/cobalt-android` fail: the Zapier-codes account is
**suspended** — `GITHUB_PUSH_TOKEN` (which belongs to that account) returns
`403 {"message":"Sorry. Your account was suspended"}` for every API call and
`git clone` returns `remote: Your account is suspended`. The available
`GITHUB_TOKEN` belongs to `claudeone7492` and cannot see `Zapier-codes/*`.
`OH_GIT_CREDENTIAL_ACCESS_TOKEN_GITHUB` is `Bad credentials`. Until a working
Zapier-codes credential is supplied, work stays local to this uploaded tree —
do not assume a fetch/push target exists.
