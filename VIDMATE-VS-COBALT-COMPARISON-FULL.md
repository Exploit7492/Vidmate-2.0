# VidMate vs Cobalt Android — full feature & capability comparison

Every UI/UX and feature row, in the same shape as the original `VIDMATE-VS-COBALT-COMPARISON.md`: numbered sections, one checkmark table per area. Cobalt marks are verified against the current source tree (`cobalt-android/`, 111 JVM tests green).

| Mark | Meaning |
|---|---|
| ✅ | Present / fully supported |
| ❌ | Absent |
| ⚠️ | Partial, limited, or a known gap |
| ➖ | Intentionally out of scope (the monetisation layer) |
| ❓ | Not confirmed by the static evidence in this RE |

**Evidence basis.** VidMate: `VIDMATE-REVERSE-ENGINEERING.md` (static decompile — host 2,502 classes + plugin 21,218 classes, 159 activities). Cobalt: direct inspection of `cobalt-android/`. Both static; no runtime behaviour observed for either.

---

## 1. App shell, navigation & UI

### 1a. Entry points & launch

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Launcher activity | ✅ | ✅ | VM two launchers (host + plugin) · Co `SplashActivity` |
| Branded splash screen | ✅ | ✅ | Co: 600 ms branded splash (`SplashActivity`) |
| Splash advertisement | ✅ | ➖ | VM `SplashView` / `SplashAdConfig` |
| Splash progress/countdown | ✅ | ⚠️ | VM `CountDownProgressBar` · Co fixed short delay |
| First-run onboarding / coach screens | ✅ | ✅ | Co: multi-page `OnboardingActivity` |
| Onboarding gated to first launch only | ✅ | ✅ | Co: `onboardingDone` pref |
| Share-sheet entry (`ACTION_SEND`) | ✅ | ✅ | Co: `text/plain` intent filter |
| Open-with entry (`ACTION_VIEW`) | ✅ | ❌ | VM aliases `$NewPlayerActivity`/`$HTML` |
| Music-player launch alias | ✅ | ❌ | VM `$MusicPlayerActivity` |
| Long-press app shortcuts | ✅ | ✅ | Co: "Paste & Download", "Open Queue" |
| Deep-link / URL-scheme entry | ✅ | ❌ | VM `$JUMP` / `$INVOKE` |
| Clipboard link auto-detect | ✅ | ✅ | Co: `ClipboardHelper` + trigger toggle |
| Network-offline screen | ✅ | ⚠️ | VM `NetworkDisconnectView` · Co inline status text |

### 1b. Navigation shell

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Bottom navigation bar | ✅ | ✅ | VM 4–5 tabs · Co 5 tabs |
| Tab set server-configurable | ✅ | ✅ | Co: `remote-config.json` `tabs` |
| Default tab layout | ✅ | ✅ | VM Home·Downloader·[GC]·[YTM]·Me · Co Home·Explore·Library·Shorts·Me |
| "Earn cash" alternate layout | ✅ | ➖ | VM `main_earn_cash` variant |
| Per-tab back-stack (nested nav) | ✅ | ✅ | Co: Navigation component |
| Floating action / download button | ✅ | ✅ | Co: queue FAB |
| Download-count badge | ✅ | ✅ | Co: badge on queue button |
| Pull-to-refresh on feeds | ✅ | ⚠️ | Co: reload action, not a swipe gesture |
| Settings reachable from shell | ✅ | ✅ | Co: Me → Settings |
| Persistent mini-player bar | ✅ | ✅ | Co: music mini-player |
| Bottom-bar visibility driven by config | ✅ | ✅ | Co: `applyRemoteTabs()` |

### 1c. Screens

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Home / discovery | ✅ | ✅ |  |
| Search screen | ✅ | ✅ | Co: Explore → Search |
| Trending / discovery | ✅ | ✅ | Co: Explore → Trending |
| Video detail screen | ✅ | ✅ | Co: resolve/picker flow |
| Full-screen player | ✅ | ✅ | Co: `VideoPlayerDialogFragment` |
| Downloads / files screen | ✅ | ✅ | Co: Library → Downloads |
| Download queue sheet | ✅ | ✅ | Co: `DownloadQueueSheet` |
| In-app browser | ✅ | ✅ | Co: `BrowserFragment` |
| Local media library | ✅ | ✅ | Co: `MediaLibraryFragment` |
| Private vault | ✅ | ✅ | Co: `VaultFragment` |
| WhatsApp status saver | ✅ | ✅ | Co: `StatusFragment` |
| Music screen | ✅ | ✅ | Co: `MusicFragment` |
| YouTube Music | ✅ | ✅ | Co: Explore → Music |
| Channel pages | ✅ | ✅ | Co: `ChannelFragment` |
| Comments | ✅ | ✅ | Co: comments on Home items |
| Games / web-games hub | ✅ | ✅ | Co: `GamesFragment` |
| App store (9Apps) | ✅ | ➖ |  |
| Wallet / earn-cash | ✅ | ➖ |  |
| Cleaner / speed-booster | ✅ | ➖ |  |
| Daily-tasks / rewards | ✅ | ➖ |  |
| Settings (multiple sub-screens) | ✅ | ✅ | Co: one Compose settings screen |
| History / bookmarks | ✅ | ✅ | Co: Library → History |
| Language picker screen | ✅ | ✅ | Co: in Settings |
| Region/location picker screen | ✅ | ✅ | Co: in Settings |
| Download-path picker screen | ✅ | ✅ | Co: text field in Settings |
| Push-settings screen | ✅ | ✅ | Co: notification toggles |
| Widget-settings screen | ✅ | ❌ |  |
| Safe-mode screen | ✅ | ➖ |  |
| Login / account screen | ✅ | ➖ | Co has no account by design |
| Update / "safe update" screen | ✅ | ⚠️ | Co: in-app update check, no dedicated screen |

### 1d. Localisation

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| In-app language picker | ✅ | ✅ | Co: 15 options |
| In-app region picker | ✅ | ✅ | Co: `RegionHelper` |
| Translated string resources | ✅ (33) | ❌ (1) | Co ships one `values/strings.xml` |
| Selectable UI languages | ✅ (83) | ⚠️ (15) | Co: picker switches locale, strings mostly English |
| Selectable regions | ✅ (107) | ⚠️ | Co: content-region bias |
| RTL layout support | ✅ | ✅ | Co: `supportsRtl` |
| Language-of-content rule (max 2) | ✅ | ❌ |  |

### 1e. Onboarding & permissions

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| First-run walkthrough | ✅ | ✅ |  |
| Notification-permission explainer | ✅ | ✅ | Co: request + rationale |
| Overlay-permission guide | ✅ | ➖ | VM floating window |
| Storage-permission guide | ✅ | ➖ | Co uses scoped storage |
| Battery-optimisation prompt | ✅ | ✅ | Co: exempt-battery flow |
| Install-packages prompt (self-update) | ✅ | ✅ | Co: `REQUEST_INSTALL_PACKAGES` |
| VPN/geo-failure guidance | ✅ | ✅ | Co: `ResolveFailureAdvisor` |
| Empty-state guidance | ✅ | ✅ |  |

### 1f. Sharing

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Share sheet out | ✅ | ✅ |  |
| Receive shares from other apps | ✅ | ✅ |  |
| Share to specific social apps | ✅ | ✅ | Co: system chooser |
| Offline / Bluetooth share | ✅ | ❌ |  |
| QR / proximity share | ✅ | ❌ |  |

## 2. Download engine

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Paste-a-link box | ✅ | ✅ |  |
| Link validity feedback | ✅ | ✅ | Co: "doesn't look like a valid link" |
| Format/quality picker sheet | ✅ | ✅ |  |
| Resolution labels ("1080p" etc.) | ✅ | ✅ | Co: FFmpeg profile labels |
| Audio-only option | ✅ | ✅ | Co: audio format + audio-only mode |
| Default quality preference | ✅ | ✅ |  |
| Download progress row (bytes/%) | ✅ | ✅ |  |
| Download speed display (MB/s) | ✅ | ✅ | Co: `speedBps` → "x.x MB/s" |
| Multi-select downloads | ✅ | ✅ |  |
| Playlist / batch download dialog | ✅ | ✅ | Co: real playlist expansion + line batch |
| Cancel in-progress download | ✅ | ✅ | Co: `DownloadCanceller` |
| Retry a failed download | ✅ | ✅ | Co: retry + auto-retry |
| Open a finished download | ✅ | ✅ |  |
| Delete a finished download | ✅ | ✅ |  |
| Clear download history | ✅ | ✅ |  |
| Download-in-progress notification | ✅ | ✅ |  |
| Download-complete notification | ✅ | ✅ |  |
| Pre-download interstitial ad | ✅ | ➖ | VM `task_add_inters_ad` |
| HTTP(S) download | ✅ | ✅ |  |
| Multi-segment / range downloads | ✅ | ✅ | Co: `SegmentedDownloadEngine` |
| Per-part URL fail-over (CDN fallback) | ✅ | ✅ | Co: per-`Segment` URL |
| Per-part path fail-over | ✅ | ⚠️ | Co: single temp file |
| Resume an interrupted download | ✅ | ✅ | Co: ETag-guarded |
| DASH / adaptive segmented | ✅ | ✅ |  |
| HLS download | ✅ | ✅ | Co: saves HLS |
| Audio+video merge (mux) | ✅ | ✅ | VM native codec · Co FFmpeg |
| Video → audio extraction | ✅ | ✅ |  |
| Quality/format conversion tiers | ✅ | ✅ | Co: FFmpeg ladder |
| BitTorrent / magnet | ✅ | ❌ | VM `download.bt.core.*` |
| SABR (segmented adaptive) | ✅ | ❌ |  |
| Playlist / batch download | ✅ | ✅ | Co: real playlist + line batch |
| Parallel segment count control | ✅ | ⚠️ | Co: fixed concurrency |
| Auto-retry on network failure | ✅ | ✅ | Co: 3 retries |
| Storage-full handling | ✅ | ✅ |  |
| Custom download location | ✅ | ✅ |  |
| Foreground download service | ✅ | ✅ |  |
| Downloads survive UI death | ✅ | ✅ | Co: WorkManager + service |
| Persisted download state | ✅ | ✅ | VM Room+MMKV · Co Room |
| AIDL / cross-process download service | ✅ | ❌ | Co: single-process |
| Cancel in-progress transcode | ✅ | ✅ | Co: FFmpegKit cancel |
| Download speed metering | ✅ | ✅ |  |

## 3. Site support & extraction

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| YouTube | ✅ | ✅ |  |
| TikTok | ✅ | ✅ |  |
| Twitter / X | ✅ | ✅ |  |
| Instagram | ✅ | ✅ |  |
| Facebook | ✅ | ✅ |  |
| Dailymotion | ✅ | ✅ |  |
| Vimeo | ✅ | ✅ |  |
| Pinterest | ✅ | ✅ |  |
| Reddit | ❓ | ✅ |  |
| SoundCloud | ❓ | ✅ |  |
| Twitch | ❓ | ✅ |  |
| Bilibili | ❓ | ✅ |  |
| Tumblr | ❓ | ✅ |  |
| Broad catch-all extractor | ✅ | ✅ | Co: any `http(s)` host offered |
| Extractor updatable without app update | ✅ | ✅ | Co: remote `supportedHosts` |
| Media sniffing from web pages | ✅ | ✅ | Co: `MediaSniffer` |
| VPN hint on failure | ✅ | ✅ | Co: `ResolveFailureAdvisor` |
| Playlist URL support | ✅ | ✅ | Co: `PlaylistRepository` |
| YouTube public search / trending | ✅ | ✅ | Co: InnerTube |
| YouTube Music discovery | ✅ | ✅ | Co: `FEmusic_home` |
| Channel pages | ✅ | ✅ |  |
| Comments | ✅ | ✅ |  |
| YouTube sign-in (account state) | ✅ | ❌ | Needs OAuth; public surface done |
| Subscriptions / watch-later / history | ✅ | ❌ | Account-only |
| Clipboard link detection | ✅ | ✅ |  |
| Share-sheet intake | ✅ | ✅ |  |
| JS/WebView-driven extraction | ✅ | ❌ | Co uses cobalt backend |

## 4. Media playback

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Inline player controls | ✅ | ✅ | Both Media3/ExoPlayer |
| Full-screen / landscape | ✅ | ✅ |  |
| Picture-in-picture | ✅ | ✅ | Co: `enterPictureInPictureMode` |
| Floating pop-up window | ✅ | ❌ | VM `SYSTEM_ALERT_WINDOW` layer |
| Playback-speed menu | ✅ | ✅ |  |
| Quality switch in player | ✅ | ✅ |  |
| Subtitle toggle | ✅ | ✅ |  |
| Equalizer panel | ✅ | ✅ | Co: `EqualizerController` |
| Pre-roll ad | ✅ | ➖ | VM `pre_rolls_ad` |
| Gesture controls (seek/brightness) | ✅ | ⚠️ | Co: basic seek bar |
| Next/prev in a Shorts feed | ✅ | ✅ | Co: preloads next Short |
| Resume playback position | ✅ | ⚠️ | Co: per-session, not persisted |
| Inline video playback | ✅ | ✅ |  |
| Full-screen playback | ✅ | ✅ |  |
| HLS / DASH playback | ✅ | ✅ |  |
| Floating overlay player | ✅ | ❌ |  |
| Background audio | ✅ | ✅ | Co: `MusicPlaybackService` |
| Media-button / headset control | ✅ | ✅ | Co: MediaSession |
| Playback speed | ✅ | ✅ |  |
| Subtitle / caption support | ✅ | ✅ |  |
| In-player quality switch | ✅ | ✅ |  |
| Equalizer | ✅ | ✅ |  |
| Volume / brightness gestures | ✅ | ⚠️ |  |
| Shorts autoplay + preload | ✅ | ✅ |  |
| One-live-player discipline | ✅ | ✅ |  |
| Playback-position persistence | ✅ | ⚠️ |  |

## 5. Library, local media & vault

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Downloads list with thumbnails | ✅ | ✅ |  |
| Local video library grid | ✅ | ✅ | Co: MediaStore scan |
| Local music library | ✅ | ✅ |  |
| Search within local media | ✅ | ✅ | Co: `LocalMediaQuery` |
| Sort / filter chips | ✅ | ✅ | Co: sort + kind filter |
| Multi-select + batch delete | ✅ | ✅ |  |
| File detail / info | ✅ | ⚠️ | Co: row metadata only |
| Watch history timeline | ✅ | ✅ |  |
| Liked / bookmarks list | ✅ | ✅ |  |
| Storage-usage display | ✅ | ❌ |  |
| Passcode gate | ✅ | ✅ | Co: vault passcode screen |
| Create-passcode first-run flow | ✅ | ✅ |  |
| Import into vault | ✅ | ✅ | Co: SAF picker |
| Export from vault | ✅ | ✅ |  |
| Hidden from system gallery | ✅ | ✅ | Co: app-private storage |
| "Hide/unhide" confirmation UX | ✅ | ⚠️ | Co: simpler confirm |
| Local video scanner | ✅ | ✅ | Co: MediaStore |
| Local music scanner | ✅ | ✅ |  |
| Local media search | ✅ | ✅ |  |
| Sort / filter | ✅ | ✅ |  |
| Multi-select | ✅ | ✅ |  |
| Delete a file from the app | ✅ | ✅ |  |
| Watch history | ✅ | ✅ |  |
| Likes / bookmarks | ✅ | ✅ |  |
| Dislike | ✅ | ❌ |  |
| Private vault (passcode) | ✅ | ✅ |  |
| Vault import / export | ✅ | ✅ |  |
| Storage-usage stats | ✅ | ❌ |  |

## 6. In-app browser

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Browser with URL bar | ✅ | ✅ |  |
| Multi-tab UI | ✅ | ✅ |  |
| Incognito toggle | ✅ | ✅ |  |
| Desktop-site toggle | ✅ | ✅ |  |
| Media-sniff download prompt | ✅ | ✅ | Co: `MediaSniffer` |
| "Download this page's video" CTA | ✅ | ✅ |  |
| Web-game browser mode | ✅ | ✅ | Co: Games hub → browser |
| Browser history / bookmarks | ✅ | ❌ |  |
| Browser anti-detection | ✅ | ❌ | Excluded by design |
| In-app browser | ✅ | ✅ |  |
| Multi-tab | ✅ | ✅ |  |
| Incognito | ✅ | ✅ |  |
| Media sniffing | ✅ | ✅ |  |
| Web-game browsing | ✅ | ✅ |  |
| History / bookmarks | ✅ | ❌ |  |

## 7. Discovery & engagement

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Recommendation feed | ✅ | ✅ | Co: InnerTube-backed |
| Feed grid layout | ✅ | ✅ | Co: 2-column grid |
| Section headers | ✅ | ✅ | Co: "Recommended" |
| Home pill search bar | ✅ | ✅ |  |
| Feed ad slots | ✅ | ➖ | VM `home_feed_page_ad` |
| Adult-content hiding in feed | ✅ | ➖ | No such content in Cobalt |
| Feed refresh / rotate | ✅ | ✅ | Co: query-pool rotation |
| User-tunable feed topics | ❌ | ✅ | Co: feed query pool (Cobalt-only) |
| Home recommendation feed | ✅ | ✅ |  |
| Trending / discovery | ✅ | ✅ | Co: `FEtrending` |
| In-app search with results | ✅ | ✅ | Co: Explore → Search |
| Search-result paging | ✅ | ✅ | Co: continuation tokens |
| Channel pages | ✅ | ✅ |  |
| Shorts feed | ✅ | ✅ | Co: 4 merged sources |
| Like / dislike | ✅ | ⚠️ | Co: like only |
| Comments | ✅ | ✅ |  |
| Recommendation engine ("because you watched") | ✅ | ✅ | Co: on-device, opt-in |
| Adult-content filter | ✅ | ➖ |  |

## 8. Music & social

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Music player screen | ✅ | ✅ |  |
| Now-playing with artwork | ✅ | ✅ |  |
| Lyrics view | ✅ | ✅ | Co: `LrcParser` |
| Lyrics search (online) | ✅ | ❌ | VM `LrcSearchActivity` |
| Playlists | ✅ | ✅ | Co: Room playlists |
| Albums / singers browse | ✅ | ❌ |  |
| Music discovery feed | ✅ | ✅ | Co: Explore → Music |
| Equalizer | ✅ | ✅ |  |
| Sleep timer | ✅ | ❌ |  |
| Offline / Bluetooth share | ✅ | ❌ |  |
| Music player | ✅ | ✅ |  |
| Lyrics | ✅ | ✅ | Co: `LrcParser` |
| Online lyrics search | ✅ | ❌ |  |
| YouTube Music | ✅ | ✅ |  |
| WhatsApp status saver | ✅ | ✅ | Co: `status/*` |
| Social share sheet | ✅ | ✅ |  |
| Share-link shorteners | ✅ | ➖ | VM marketing hosts |

## 9. Settings & theming

### 9a. Settings

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Theme picker | ✅ | ✅ |  |
| Language picker | ✅ | ✅ |  |
| Region / content-locale picker | ✅ | ✅ |  |
| Download-path selector | ✅ | ✅ | Co: text field |
| Default quality/format | ✅ | ✅ |  |
| Clipboard trigger toggle | ✅ | ✅ |  |
| Audio-only mode | ✅ | ✅ |  |
| Notification toggles | ✅ | ✅ |  |
| Recommendation opt-in toggle | ❌ | ✅ | Co-only ("recommend videos for me") |
| Widget settings | ✅ | ❌ |  |
| Adult-content hide | ✅ | ➖ |  |
| Safe mode | ✅ | ➖ |  |
| Invidious instance pool | ❌ | ✅ | Cobalt-only |
| Shorts query pool | ❌ | ✅ | Cobalt-only |
| Home-feed query pool | ❌ | ✅ | Cobalt-only |
| Cobalt API-key field | ❌ | ✅ | Cobalt-only |
| Clear history action | ✅ | ✅ |  |
| Battery-optimisation shortcut | ✅ | ✅ |  |
| Check-for-update action | ✅ | ✅ | Co: `UpdateChecker` |

### 9b. Theming & visual design

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Light theme | ✅ | ✅ |  |
| Dark / night theme | ✅ | ✅ |  |
| Follow-system theme | ✅ | ✅ | Co: `DYNAMIC` mode |
| Material-You dynamic colour | ❌ | ✅ | Cobalt-only (Android 12+) |
| Brand-blue accent palette | ✅ | ✅ | Co: VidMate-derived palette |
| Accent/gold highlights | ✅ | ✅ |  |
| Custom animated widgets | ✅ | ⚠️ | Co: progress ring only |
| Rounded feed tiles | ✅ | ✅ |  |
| Server-managed skins | ✅ | ❌ | VM `SkinManager` |

### 9c. Notifications & widgets

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Download-progress notification | ✅ | ✅ |  |
| Download-complete notification | ✅ | ✅ |  |
| Music-player notification | ✅ | ✅ | Co: MediaSession notification |
| Recommendation notification | ✅ | ✅ | Co: "because you watched X" |
| Rich notification layouts | ✅ (22) | ⚠️ | Co: standard layouts |
| Notification action buttons | ✅ | ✅ | Co: Open / retry actions |
| Separate notification channels | ✅ | ✅ | Co: downloads + recommendations |
| Home-screen app widgets | ✅ (3) | ✅ (1) | Co: downloads widget |
| Widget deep-link into app | ✅ | ✅ |  |
| Download notifications | ✅ | ✅ |  |
| Music notifications | ✅ | ✅ |  |
| Recommendation notifications | ✅ | ✅ | Co: local, on-device |
| Separate channels | ✅ | ✅ |  |
| Server-originated push (FCM) | ✅ | ❌ | Needs Firebase project |
| Keep-alive / resident notice | ✅ | ➖ |  |
| App widgets | ✅ (3) | ✅ (1) |  |

## 10. Monetisation & growth layer

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| In-app app store (9Apps) | ✅ | ➖ |  |
| Game centre / web games | ✅ | ⚠️ | Co: games portal hub only |
| Earn-cash / wallet UX | ✅ | ➖ |  |
| Daily tasks / coins | ✅ | ➖ |  |
| Cleaner / speed-booster | ✅ | ➖ |  |
| Interstitial / banner / native ads | ✅ | ➖ |  |
| Exit-popup ad | ✅ | ➖ | VM `exit_popup_ad` |
| Ad-free experience | ❌ | ✅ | Cobalt-only |

## 11. Platform & technical

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Plugin architecture | ✅ | ❌ | VM Tencent Shadow |
| Dynamic code loading / hot-fix | ✅ | ❌ | Excluded by design |
| Native codec libraries | ✅ | ✅ | VM codec · Co FFmpeg |
| Server-driven remote config | ✅ | ✅ | Co: `remote-config.json` |
| Self-update (in-app APK install) | ✅ | ✅ | Co: `UpdateChecker` + `ApkInstaller` |
| Crash handler / auto-restart | ✅ | ✅ | Co: `CrashHandler` |
| Multi-process architecture | ✅ | ❌ | Co: single process |
| AIDL service interfaces | ✅ | ❌ |  |
| Signed store release | ❌ | ❌ | Both sideload-only |
| Unit-test suite | ❓ | ✅ | Co: 111 JVM tests |
| Open-source license | ❌ | ✅ | Co ships `LICENSE` |
| 64-bit ABI support | ✅ | ✅ |  |

## 12. Security & privacy posture

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| `MANAGE_EXTERNAL_STORAGE` | ✅ | ❌ | Co: scoped storage |
| `SYSTEM_ALERT_WINDOW` | ✅ | ❌ |  |
| `REQUEST_INSTALL_PACKAGES` | ✅ | ✅ | Co: for self-update only |
| `GET_TASKS` / `KILL_BACKGROUND_PROCESSES` | ✅ | ❌ |  |
| Notification-listener access | ✅ | ❌ |  |
| Cleartext traffic allowed | ✅ | ⚠️ | Co ships network-security config |
| Runtime package-name spoofing | ✅ | ❌ |  |
| Weak XOR string obfuscation | ✅ | ❌ |  |
| First-party API keys in plaintext | ✅ | ❌ |  |
| Ad / tracking SDKs | ✅ (6) | ❌ |  |
| Process daemon / keep-alive | ✅ | ❌ |  |
| Scoped storage (MediaStore / SAF) | ⚠️ | ✅ |  |
| Dynamic code loading | ✅ | ❌ |  |
| Packer / anti-tamper stack | ✅ | ❌ |  |

---

## Scorecard

Counted programmatically from the tables above. The ➖ rows (section 10, the monetisation layer) are excluded — that is a deliberate non-goal, not a gap. Section 12 marks describe "does this behaviour exist", so a VidMate ✅ there is not a point in VidMate's favour.

| Area | VidMate ✅ | Cobalt ✅ | Cobalt ⚠️ | Cobalt ❌ |
|---|:--:|:--:|:--:|:--:|
| 1. App shell, navigation & UI | 74 | 50 | 6 | 8 |
| 2. Download engine | 41 | 35 | 2 | 3 |
| 3. Site support & extraction | 22 | 24 | 0 | 3 |
| 4. Media playback | 26 | 19 | 4 | 2 |
| 5. Library, local media & vault | 28 | 23 | 2 | 3 |
| 6. In-app browser | 15 | 12 | 0 | 3 |
| 7. Discovery & engagement | 17 | 14 | 1 | 0 |
| 8. Music & social | 17 | 11 | 0 | 5 |
| 9. Settings & theming | 38 | 36 | 2 | 3 |
| 10. Monetisation & growth layer ¹ | 7 | 1 | 1 | 0 |
| 11. Platform & technical | 9 | 7 | 0 | 5 |
| 12. Security & privacy posture ¹ | 14 | 2 | 1 | 11 |
| **Total** | **287** | **231** | **17** | **35** |

¹ Excluded from the total.

