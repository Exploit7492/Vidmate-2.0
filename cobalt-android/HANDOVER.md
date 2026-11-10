# Cobalt-Android — Handover (Session 14, this session)

**Start by cloning the repo fresh: `git clone https://github.com/Zapier-codes/cobalt-android.git`**
Do not trust any file content quoted in this document as current —
clone, then read the real files. `git fetch` + `git log --oneline -10`
before starting anything, even mid-session — parallel manual sessions
happen often on this repo (see Actors section).

## Session 15: Phases 22–25 built and verified (Compose migration)

**Everything below was built and verified by running the real toolchain in
this session's sandbox — `./gradlew :app:assembleDebug` produced an APK, and
`:app:testDebugUnitTest` ran green (44 tests). This is the first session where
the project actually compiles and builds against a real Android SDK.**

- **Phase 23 — Home rebuilt in Compose, with a real feed + inline streaming.**
  New `feed/` package: `FeedItem`, `InnertubeFeedSource` (reuses the Shorts
  pipeline's Innertube integration; lists regular-length video, filters ≤60s),
  `FeedQueryFeeder`, `InnertubeStreamResolver` (on-demand ANDROID-client
  `player` call → progressive/HLS URL for inline playback), `FeedRepository`
  (per-source timeout + backoff, de-dupe, Room cache fallback), `feed/db/`.
  Compose UI: `HomeScreen`/`HomeFeed` (2-column tile grid, Coil thumbnails,
  durations, view counts) and `HomePlayer` (real ExoPlayer). `HomeViewModel`
  serves both the feed and the unchanged paste → resolve → picker flow.
- **Phase 24 — Shorts chrome in Compose.** `ShortsChrome.kt` (avatar,
  `@username`, like/save/share + caption) replaces the stock-Android-icon XML
  chrome; new vector icons; `item_short_video.xml` = `PlayerView` + a
  `ComposeView` slot. `ShortsFragment`'s player lifecycle untouched.
- **Phase 25 — Downloads + Settings in Compose.** `DownloadsScreen` (queue
  hosted via `DownloadQueueSheet`'s `ComposeView`; `DownloadAdapter` +
  `item_download.xml` deleted). One consolidated Compose `SettingsScreen` +
  `SettingsViewModel` replaces BOTH the old XML Settings tab and the separate
  `SettingsSheet` bottom sheet (files deleted; gear icon now opens the tab).
- **Phase 26 — PARTIAL, deliberately not overclaimed.** Dead View code and
  layouts removed; all primary screens render through Compose. But the phase's
  actual spec (MainActivity → `ComponentActivity` + Compose Navigation,
  `viewBinding` off) is only reachable once *every* screen is Compose, which is
  not yet true — `MainActivity`/`activity_main.xml`, `ResolutionPickerDialog`,
  `QualitySelectionSheet`, `HistoryFragment`, `VideoPlayerDialogFragment`, and
  the RecyclerView `ViewPager2` Shorts host are still Views/XML. Turning
  `viewBinding` off now would break the app, so it was left as the real
  remaining work.
- **Also this session:** README rewritten to describe the real native app (the
  old one described a WebView that no longer exists); `DownloadStatusTest`
  corrected (5 → 6 statuses); `AGENTS.md` created with the verified build
  recipe; test-only `org.json` dependency added; real feed-parser tests added
  (they caught and fixed a malformed-JSON crash that emptied the whole feed).

**Verified build recipe** — see `AGENTS.md` (JDK 21, SDK at
`/workspace/android-sdk`, `local.properties` with `sdk.dir`).

### What remains (updated)

1. **Phase 26 for real** — migrate `MainActivity`, the binary dialogs
   (`ResolutionPickerDialog`/`QualitySelectionSheet`/`VideoPlayerDialogFragment`),
   `HistoryFragment`, and the Shorts `ViewPager2` host to Compose, THEN switch
   to Compose Navigation and turn `viewBinding` off. This is the main remaining
   phase. It was not faked.
2. **"Drama shorts only" filter** — still needs the user's intent clarified
   (see Session 14). The plumbing exists: `SettingsRepository.customShortsQueries`
   + `ShortsQueryFeeder.applyCustomQueries` let the user set the Shorts search
   pool via Settings; a hard "drama" default is a one-line pool change, but
   which reading is meant is unresolved.
3. **"Videos aren't playing"** — this session added inline Home playback
   (Phase 23) and kept the Shorts player untouched; without a specific symptom
   from the user (sandbox can't reproduce) a targeted fix isn't possible.
4. **Phase 20 follow-ups** — Settings UI for the two default-quality keys, a
   cancel button for an in-flight `CONVERTING` row, wiring the quality sheet
   into Shorts' save action.
5. **End-to-end resolve against the deployed Render instance
   (`cobalt-api-yuol.onrender.com`) still unconfirmed** by any session.
6. **Hermes pipeline files** (`cycle-prompt.txt`, `watch-loop.md`, the
   `~/.hermes-pipeline/state.json` references in docs) are now stale — the
   pipeline was retired in Session 10. Not deleted here (they're harmless repo
   docs and removing them wasn't requested), but they don't describe this
   session's workflow.
7. Phase 13's light-theme-looks-identical-to-dark gap — documented, accepted.

## Session 14: real UI feedback → full Compose migration decision (major
## pivot, read this whole section before touching Home/Shorts UI again)

**What happened, in order:**
1. User gave direct, specific negative feedback on the actual shipped
   UI: Home doesn't look like a real app (no feed — confirmed by reading
   `fragment_home.xml`, which has only a paste-link box and an
   intentionally-empty `feedContainer` `FrameLayout`, unchanged since
   Phase 3), Shorts "isn't professional" (confirmed by reading
   `item_short_video.xml`/`ShortsAdapter.kt`: the like button is
   literally `android.R.drawable.btn_star_big_off`, save/share are
   `ic_menu_save`/`ic_menu_share` — stock Android-API-1 system icons, no
   avatar, no username styling, no follow affordance, no counts, no
   progress bar), videos "aren't playing," and Shorts should be
   filterable to "drama" content only.
2. User then gave a concrete reference: `github.com/phoenix-boss/echo-music`,
   cloned into a sandbox and read directly (not assumed from its
   README) — asked for it to be copied verbatim, "every component,
   layout, feel, design."
3. **Checked before acting on "verbatim," found three real mismatches**
   (full detail in `ARCHITECTURE.md`'s new "Major pivot after Phase 20"
   section — this is the authoritative version, don't re-derive it):
   toolkit (echo-music is 100% Jetpack Compose, cobalt-android is
   Views/XML — not a file-copy operation across that boundary), domain
   (echo-music is a 14-module music app — lyrics/karaoke/song-recognition/
   EQ modules have no video/Shorts equivalent), and license (echo-music
   is GPLv3, cobalt-android has no LICENSE file at all — copying its code
   wholesale is a real copyleft decision, not just technical).
4. **Asked the user directly** (via a real either/or, not assumed):
   rebuild in Views to match the *feel*, or actually migrate to Compose.
   **User chose: migrate to Compose.** This is the standing direction for
   all UI work from here — new screens go in Compose, not new XML.
5. Landed **Phase 22 (Compose migration foundation)** this same session
   — see below for what that actually is.

**Don't re-litigate step 3's reasoning or step 4's decision in a future
session** — the tradeoff was surfaced honestly, the user chose with full
information, and re-asking "are you sure you don't want to just copy the
files" wastes their time on a question they already answered.

## Phase 22 landed: Compose toolchain, verified version-by-version (not
## guessed — this project has a real scar from guessed versions, see
## Session 13's ffmpeg-kit saga below)

cobalt-android was on Kotlin 1.9.23; echo-music (the reference) is on
Kotlin 2.3.10 + Compose 1.10.2 — copying those numbers straight over
would have broken the build immediately (Compose compiler versions are
tightly locked to Kotlin versions pre-2.0). Checked the real
compatibility chain instead of guessing, against primary sources:

- `developer.android.com/jetpack/androidx/releases/compose-compiler` —
  confirmed compose-compiler `1.5.14` states "This compiler release is
  targeting Kotlin 1.9.24" directly. So: Kotlin bumped `1.9.23` →
  `1.9.24` (a one-patch-version bump, not a leap), specifically to land
  on this confirmed pairing.
- KSP (already used throughout this project for Room codegen) bumped to
  match: `1.9.24-1.0.20` — confirmed as a real, existing, resolvable
  release on Maven Central (published May 7 2024), not assumed to exist
  because the pattern looked right.
- Compose BOM pinned to `2024.06.00` — confirmed via mvnrepository.com's
  own listing (published June 12 2024, immediately after Kotlin 1.9.24's
  release) rather than pinning "latest," which would pull Compose
  runtime artifacts built against newer compose-compiler feature sets
  than 1.5.14 actually supports.
- `io.coil-kt:coil-compose:2.6.0` added for real remote image loading —
  this project had **zero** image-loading library before this phase
  (checked by grepping `app/build.gradle.kts` for "Coil"/"Glide" first),
  which is part of *why* Home never had real thumbnails. Coil 2.x chosen
  over 3.x/`coil3` deliberately (3.x relocated groupId for a Compose-
  Multiplatform architecture this Android-only app doesn't need); 2.6.0
  is era-matched to the rest of this pin set (Feb 2024).

Landed: `buildFeatures.compose = true` alongside the existing
`viewBinding = true` (not a switch — every existing screen keeps working
unchanged; Compose lands one screen at a time via `ComposeView`
interop, per Phase 22's write-up in `ARCHITECTURE.md`), plus
`ui/theme/Theme.kt` — a real, working `CobaltTheme` composable adapted
(not copied, see the licensing reasoning above) from echo-music's
verified Material3-dynamic-color-with-fallback pattern, built on this
app's own existing `cobalt_*` brand colors.

**Not done, and deliberately not faked:** no screen has actually been
migrated to Compose yet. `ARCHITECTURE.md` outlines Phases 23–26 (Home
feed, Shorts redesign, Downloads/Settings, legacy-system removal) as a
plan only — each gets its own real Definition-of-Done write-up once
actually built, not a confident-sounding spec for work that hasn't
started. **Not built against a real Android toolchain** — same standing
sandbox limitation as always; confirming this whole dependency chain
actually resolves via GitHub Actions CI is the single most important
next step, before any further Compose work compounds on top of an
unverified foundation.

**The "drama shorts only" filter and the "videos aren't playing" bug
report are both still open, untouched this session** — this session's
scope was entirely the toolkit-pivot decision and its foundation, per
the user's own sequencing (the reference-repo request came before either
of those got picked up). Worth flagging for whoever picks this up:
"drama shorts" needs the same clarification a prior session already
raised for a different but related ask — `ARCHITECTURE.md`'s Phase 2
addendum explicitly declined integrating paywalled apps like
ReelShort/DramaBox (unofficial reseller APIs, not a legitimate public
API), so "drama shorts" likely means tuning `ShortsQueryFeeder`'s query
pool toward drama-genre YouTube content on the existing free sources —
confirm with the user rather than assuming either reading. "Videos
aren't playing" was not diagnosed this session — no logcat/symptom
detail was available, and this sandbox cannot run the app to reproduce
it; get a specific symptom (black screen? infinite spinner? crash? one
source or all four?) before guessing at a fix.

## Continuation (VidMate 2.0 rebrand + Shorts drama rail + floating tab)

The project was rebranded to **VidMate 2.0** and published at
`github.com/Exploit7492/Vidmate-2.0` (branch `shorts-drama-source`, PR #1),
built on the VidMate reverse-engineering work.

Shorts drama rail (`shorts/source/DramaShortsSource.kt`) is now an aggregator
over public, key-free catalogues: **Dailymotion** keyword search and a second
Dailymotion **channels** provider (complete-series compilations), **iptv-org**
category playlists as the stable floor, and best-effort **ChartDrama** plus the
bot-walled sites (DramaBox / DramaShorts / Shortical / FlickReels /
shorts-drama.com). Each provider fails closed independently and only emits
items with a usable stream URL. New test coverage keeps the suite at 147 tests,
0 failures.

App shell: the Shorts entry in the bottom bar is now a **floating rounded pill
lifted above the bar** with a circular accent flare behind it. The bar's own
middle slot is hidden (its space reserved) so the pill is the sole Shorts
affordance; Home and Settings sit either side. See
`MainActivity.setupFloatingShortsTab`, `activity_main.xml`,
`res/drawable/bg_shorts_tab.xml`, `res/drawable/bg_shorts_flare.xml`.

Debug APKs (all ABIs) are attached to the GitHub release `v2.0-debug` and also
served over the workspace host for Termux installs.

## Continuation (part 4, same session): startup crash fixed, icon + header corrected

The user reported the app **crashed before the home page**, that the splash
logo still had a **white background**, and that the **static "VidMate 2.0"
header was still there**. All three reproduced/root-caused and fixed, each
now guarded by a test.

- **Crash — reproduced on the JVM, not guessed.** Added Robolectric
  (`StartupSmokeTest`) and launched the real activities. `MainActivity.onCreate`
  threw `IllegalStateException: Activity MainActivity does not have a
  NavController set on <R.id.navHost>` at the old
  `findNavController(R.id.navHost)` call: `FragmentContainerView` inflates the
  `NavHostFragment` in a *pending* transaction, so the fragment isn't attached
  yet when the activity asks for the controller. Fix: flush pending transactions
  and read `navHost.navController` off the fragment, cached in a field and
  reused everywhere. `everyBottomTab_destination_loads_withoutCrash` now
  navigates every bottom destination as a regression guard.
- **Static header — root cause was the theme.** `Theme.Cobalt`'s parent was
  `Theme.Material3.DayNight` (which has an ActionBar), so Material3 drew a
  title strip labelled with `app_name`. Parent is now
  `Theme.Material3.DayNight.NoActionBar`; the Compose `HeaderBar` now applies
  `WindowInsets.statusBars` padding itself (nothing else occupies that inset).
  `mainActivity_starts_withoutCrash` asserts `supportActionBar == null`.
- **Icon / splash logo — the uploaded `vid.png` is now the real source.** The
  adaptive icon's foreground pointed at a leftover 592-byte white-chevron
  vector (`@drawable/ic_launcher_foreground`); it was deleted and every launcher
  tile + adaptive foreground is regenerated from `incoming/vid.png` (art
  centred, transparent adaptive foreground, `@color/ic_launcher_background`
  behind it). The splash logo (`drawable-nodpi/vidmate_logo.png`) was flattened
  onto white — now regenerated with a **transparent** background, plus a
  `drawable-night-nodpi` variant whose ink is inverted so it reads on the dark
  background. The typewriter splash (`TypewriterText`) and Compose splash were
  already present and are unchanged.
- **Prompt cleanup.** Removed the orphaned `nav_me` / `nav_explore`
  destinations from the nav graph (prompt 22: Me is gone, Library lives in the
  downloads/library page reached from the bell). No code referenced them.

Suite is now **149 tests, 0 failures** (`:app:assembleDebug` green).

## Continuation (part 3, same session): the real ffmpeg-kit fix + a CI
## workflow bug that was making runs look inconsistent

The user pasted a live CI failure log (run "Update handover with the real
ffmpeg-kit fix and Session 13's full history" — ironic, since that commit's
own claimed fix hadn't actually been verified against a real build yet):
`Could not find io.github.jamaismagic.ffmpeg:ffmpeg-kit-lts-full-gpl-16kb:6.1.7`.

**Root cause, confirmed not guessed:** the artifact/name pinned in
`app/build.gradle.kts` was always correct — `ffmpeg-kit-lts-full-gpl-16kb`
genuinely exists as a published Maven Central artifact (fetched the real
mvnrepository.com page directly, not a search snippet, and confirmed it in
a full 10-artifact group listing). The version was wrong: `6.1.7` had been
inferred from a *sibling* artifact in the same group
(`ffmpeg-kit-lts-16kb`, which genuinely is at `6.1.7`) rather than read off
`ffmpeg-kit-lts-full-gpl-16kb`'s own page — different artifacts in the same
Maven group are not guaranteed to share a version, and here they didn't.
Fetched that artifact's own page directly: it has exactly **one** published
version, **`6.1.4`** (Feb 27, 2026) — now pinned.

**Before committing to this fix, seriously considered swapping to a
different package entirely** (`com.moizhassan.ffmpeg:ffmpeg-kit-16kb`,
confirmed live on Maven Central, found during the same research pass) but
ruled it out for a real, checked reason, not a hunch: its license metadata
is LGPL-only, meaning no GPL codecs (x264/x265/etc). Read
`FfmpegTranscoder.buildArgs()` directly and confirmed it genuinely emits
`-c:v libx264` for `TranscodeProfile.Video` entries with
`videoCodec == "libx264"` — an LGPL-only package would silently fail every
MP4 tier in `TranscodeProfile.ALL_VIDEO` at runtime ("Unknown encoder
'libx264'"), not at compile time, so this would NOT have shown up as a CI
failure — it would have shipped broken and only surfaced when a real user
tried an MP4 download. Checked `minSdk` (26) too, confirming `main` vs
`lts` doesn't matter for this project (both exceed 26) in case a future
session considers that swap for other reasons.

**Second, separate fix this same pass**: the user asked why CI seemed to
"succeed right after failing, almost as if something else is building a
stale or separate version." Read `.github/workflows/build.yml` directly —
found `on: push` AND `on: pull_request` both configured for
`branches: ["**"]`. This project's real workflow is direct commits (patch
+ `git am` + push from a device), not PR-gated merges, so a commit that's
also part of an open PR (the log the user pasted referenced "PR #44")
triggers **two separate builds**: one for the exact pushed commit, one for
a GitHub-synthesized PR merge commit. These are genuinely different trees
and can diverge in Gradle cache warm-state, producing different pass/fail
results for what looks like "the same" commit. **Removed the
`pull_request` trigger** — one trigger, one build, one unambiguous result
per commit. (The `actions/cache` `restore-keys` fallback in this same file
was also considered as a contributor — a primary-key cache miss falls back
to the most recent same-OS cache regardless of which Gradle files produced
it — but this doesn't fully explain a *nonexistent* artifact resolving,
since Gradle always re-checks declared repositories for anything not
already present at the exact requested coordinate+version; flagged as a
secondary, unconfirmed possibility, not fixed, since removing the
dual-trigger bug alone should already resolve the confusing behavior
described.)

**Not yet done:** an actual green Actions run confirming this — same
standing limitation as always, this sandbox can't watch or trigger CI.
This is genuinely the fourth ffmpeg-kit dependency attempt; if a
`Could not find` error appears again on this exact line, do not guess a
fifth fork — fetch `ffmpeg-kit-lts-full-gpl-16kb`'s own mvnrepository.com
or central.sonatype.com page directly first, the same way this fix did.

---

## Workflow (unchanged since Session 9)

- **No local build possible in any sandbox** — no route to
  `services.gradle.org`, no Android SDK. GitHub Actions CI (`build.yml`)
  is the only real build authority. Every fix in this repo's history is
  reasoned from source + external verification (Maven Central, official
  docs), never from an actual compile — say so plainly when reporting
  a fix, and don't assume a prior fix claim is correct just because it
  says so in a commit message. **This session found a third example of
  this** (see below) — a fix that looked verified (confirmed the
  dependency existed, confirmed its docs mentioned matching class names)
  but had verified the wrong thing.
- **Only `git add` + `git commit`. Never `git push`.** Generate patches
  (`git format-patch <base>..HEAD -o /mnt/user-data/outputs`) and
  present them — the user applies via `git am` + `git push` on-device.
- When a CI log is pasted, **verify every claimed fix against the
  actual current file content and, for dependency issues, what the
  package is actually built for** — not just whether it resolves.
  `73c7514`/`decefa3` both claimed the same fix that a later CI log
  showed still failing. This session's `b74b1c1` is a second instance:
  the *previous* "fix" (`c055efb`, "verified against sources not
  assumed") really did confirm the antonkarpenko coordinate resolves and
  contains GPL codecs — it just never checked whether that artifact was
  built to be imported directly by a plain Android app at all, and it
  isn't (see below).

## Actors on this repo (unchanged from Session 10/12, still true)

Two kinds of actor, no others:
1. **Manual chat sessions** (this one, "Session N") — a human opens a
   chat, works one step, hands off a patch. This is the only thing
   moving the project forward.
2. **The autonomous Hermes watch-loop** — confirmed fully deleted from
   the user's device as of Session 10. Not verified from any sandbox —
   take on the user's word, but the architecture conclusion (only manual
   sessions now) holds.

## What Session 13 did, most recent work first

### Root-caused and fixed the ffmpeg-kit dependency for real (commit
### `b74b1c1`) — the earlier fix in this same session was incomplete

A pasted live CI log for commit `1807e61` (this session's own prior
handover commit — i.e. the CI run *for* the "three CI fixes root-caused
and landed" commit) showed the build still failing, but on a
**different** error than any of the three this session had already
fixed: `Unresolved reference: arthenica` on every `FFmpegKit`/
`FFmpegKitConfig`/`FFmpegSession`/`ReturnCode`/`Statistics` import in
`FfmpegTranscoder.kt`. The dependency itself resolved without error —
CI's log shows the native `.so` files (`libavcodec.so`, `libffmpegkit.so`,
etc.) being packaged successfully — so this wasn't a "could not find the
coordinate" problem like the earlier `2.2.1`-vs-`2.1.0` version-pin bug.

Root cause, confirmed by fetching the actual artifact's own listing
(not re-reasoning from the same GitHub issue the previous fix leaned
on): `com.antonkarpenko:ffmpeg-kit-full-gpl` (pub.dev:
`ffmpeg_kit_flutter_new_full`, repo `sk3llo/ffmpeg-kit-flutter`) is a
**Flutter plugin build** — meant to be consumed through Flutter's Dart
bridge, not imported directly as a plain Android Kotlin/Java library.
The earlier fix's verification (a crash log from that fork's issue
tracker mentioning `com.arthenica.ffmpegkit.NativeLoader`/
`FFmpegKitConfig` in a stack trace) was real, but didn't actually prove
what it was used to justify — a class name appearing in someone else's
crash trace isn't the same claim as "this AAR exports that class for
direct import," and it doesn't.

**Fixed**: swapped to
`io.github.jamaismagic.ffmpeg:ffmpeg-kit-lts-full-gpl-16kb:6.1.7`
(`JamaisMagic/ffmpeg-kit-16KB`) — verified this one differently and more
directly: fetched its `android/README.md` and confirmed it's the
**unmodified original** arthenica Android documentation (same `import
com.arthenica.ffmpegkit.FFmpegKit;` line, same full API), and confirmed
via Maven Central's group listing that `ffmpeg-kit-lts-full-gpl-16kb` is
a real, separately-published artifact under that group (distinct from
the plain `ffmpeg-kit-lts-16kb`, which is LGPL-only and does NOT include
x264/x265 — picking that one instead would have repeated the exact
"silently drop the GPL codecs" mistake `ARCHITECTURE.md` already warns
about for a different reason). Full detail, including the honestly-
flagged uncertainty around the exact `6.1.7` version number (a best-
match against sibling artifacts, not read directly off this specific
artifact's own Maven page — that page didn't render its version table
in this sandbox's fetch tooling), is in `FfmpegTranscoder.kt`'s
`DEPENDENCY_NOTE` KDoc and `app/build.gradle.kts`'s inline comments —
read those before touching this dependency again.

**Not yet confirmed by an actual CI run** — same standing caveat as
every fix in this file's history. If CI fails again on this exact line
with a "could not find" error (not an unresolved-reference compile
error), that means the `6.1.7` version guess was wrong, not that the
whole approach was — see the DEPENDENCY_NOTE for what to check.

### Earlier this session: fixed three real CI compile errors (commit
### `c055efb`), root-caused independently rather than trusting two prior
### "fixed" claims

A pasted CI log (from PR/commit `decefa3`) showed the build still
failing on three errors `73c7514` had already claimed to fix.
Investigated each from scratch and fixed:
1. `app/build.gradle.kts`: `com.antonkarpenko:ffmpeg-kit-full-gpl` was
   pinned to a version (`2.2.1`) that only exists for the plain non-GPL
   `ffmpeg-kit-full` artifact, not the `-gpl` one — confirmed against
   Maven Central's own repository index. Fixed to `2.1.0` (a real
   version for the `-gpl` artifact) — **this fix was necessary but not
   sufficient**; see above, the coordinate itself turned out to be wrong
   for a different reason discovered later this same session.
2. `MainActivity.kt`: wrong `findNavController` import (Fragment-
   receiver extension imported but called from an Activity). Fixed to
   the Activity-receiver overload. **Confirmed fixed** — this error does
   not appear in the later CI log.
3. `QualityOptionAdapter.kt`: invalid Kotlin "definitely non-null type"
   syntax (`TranscodeProfile & Any`) used against a concrete class, not
   a type parameter. Fixed with a plain non-null override. **Confirmed
   fixed** — this error does not appear in the later CI log either.

## Found and reported, not yet acted on (carried over, unchanged)

- **`README.md` describes a completely different app** than what's
  actually built — a WebView wrapper around cobalt.tools, not the
  native app 20+ phases have actually built. Also still points `git
  clone` at the upstream fork parent, not this repo. Flagged to the
  user, no response yet on priority.
- **`cycle-prompt.txt` and `watch-loop.md`** at repo root are leftover
  Hermes-automation infrastructure files, harmless but arguably
  shouldn't still be there. Low priority.

## Standing open items (carried forward)

1. **New top priority: confirm Phase 22's Compose toolchain actually
   resolves and compiles via GitHub Actions CI.** Every version in it was
   checked against a primary source (see Session 14's section above), not
   guessed — but "checked by reading docs" and "confirmed by a real
   Gradle run" are still two different things, and this project has a
   real history (the ffmpeg-kit saga below) of the former not always
   catching what the latter would. This is more consequential than past
   dependency checks — a broken Compose toolchain blocks every UI phase
   from here (23–26), not just one feature.
2. **Phase 23 (Home feed in Compose) needs a real data-source decision
   before any UI work** — reuse/extend `InnertubeShortsSource`'s pattern
   for regular-length videos, or something else; not decided yet, don't
   assume the Shorts pipeline's exact shape transfers unmodified.
3. **Clarify "drama shorts only" with the user** before building
   anything for it — see Session 14's section above for the two readings
   and why the paywalled-app one was already declined in Phase 2.
4. **Get a specific symptom for "videos aren't playing"** before
   attempting a fix — this sandbox cannot reproduce it.
5. **A real end-to-end resolve against the deployed Render instance
   (`cobalt-api-yuol.onrender.com`) has still never been confirmed by
   any session.** Carried over since Session 10.
6. **Phase 20 (FFmpeg transcoding) follow-ups**: Settings UI for the two
   default-quality preference keys, a cancel button for an in-flight
   `CONVERTING` row, wiring the quality sheet into Shorts' save action —
   all lower priority than the Compose migration now.
7. **`state.json`'s `architecture_complete` flag** — likely moot now
   that the Hermes loop is gone, never explicitly confirmed moot.
8. Phase 13's light-theme-looks-identical-to-dark gap — documented,
   accepted, not a blocker (Compose's `CobaltTheme` does define a real
   light scheme now, per Phase 22 — worth revisiting once a screen
   actually uses it).
9. README.md rewrite and Hermes-file cleanup, below.

## Verify this session's work landed

```
cd cobalt-android
git fetch origin
git log --oneline origin/master -6
```
Expect two new commits on top of `621bdab` once this session's patch is
applied and pushed: a Phase 22 commit (Compose toolchain + `Theme.kt`)
and this handover/ARCHITECTURE.md documentation commit.
