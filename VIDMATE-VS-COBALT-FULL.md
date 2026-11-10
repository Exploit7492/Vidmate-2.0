# VidMate vs Cobalt Android — complete UI/UX + feature comparison

The exhaustive, checkmark-by-checkmark comparison: **UI/UX first, then every
feature, capability and technical trait**, VidMate (`com.nemo.vidmate` 6.0204)
against Cobalt Android as it stands today.

- **VidMate** is the reference, taken from `VIDMATE-REVERSE-ENGINEERING.md`
  (static decompile: host 2,502 classes + plugin 21,218 classes, 159 plugin
  activities, 83 languages, 6 ad networks, 39 ad placements).
- **Cobalt** marks are verified against the current source tree
  (`cobalt-android/`, 111 JVM tests green).

### Legend

| Mark | Meaning |
|---|---|
| ✅ | Present / fully supported |
| ⚠️ | Partial, limited, or a reduced form |
| ❌ | Absent |
| ➖ | Not applicable by design (chiefly the monetisation layer) |
| ❓ | Not confirmed by the static VidMate evidence |

Both sides are read **statically** — VidMate from the RE report, Cobalt from
source. No runtime behaviour was observed for either.

---

# PART A — UI / UX

## A1. Entry points, launch & first-run

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A1.1 | Launcher activity | ✅ | ✅ | VM two launchers (host + plugin) · Co `SplashActivity` |
| A1.2 | Branded splash screen | ✅ | ✅ | Co: 600 ms branded splash (`SplashActivity`) |
| A1.3 | Splash advertisement | ✅ | ➖ | VM `SplashView` / `SplashAdConfig` |
| A1.4 | Splash progress/countdown | ✅ | ⚠️ | VM `CountDownProgressBar` · Co fixed short delay |
| A1.5 | First-run onboarding / coach screens | ✅ | ✅ | Co: multi-page `OnboardingActivity` |
| A1.6 | Onboarding gated to first launch only | ✅ | ✅ | Co: `onboardingDone` pref |
| A1.7 | Share-sheet entry (`ACTION_SEND`) | ✅ | ✅ | Co: `text/plain` intent filter |
| A1.8 | Open-with entry (`ACTION_VIEW`) | ✅ | ❌ | VM aliases `$NewPlayerActivity`/`$HTML` |
| A1.9 | Music-player launch alias | ✅ | ❌ | VM `$MusicPlayerActivity` |
| A1.10 | Long-press app shortcuts | ✅ | ✅ | Co: "Paste & Download", "Open Queue" |
| A1.11 | Deep-link / URL-scheme entry | ✅ | ❌ | VM `$JUMP` / `$INVOKE` |
| A1.12 | Clipboard link auto-detect | ✅ | ✅ | Co: `ClipboardHelper` + trigger toggle |
| A1.13 | Network-offline screen | ✅ | ⚠️ | VM `NetworkDisconnectView` · Co inline status text |

## A2. Navigation shell

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A2.1 | Bottom navigation bar | ✅ | ✅ | VM 4–5 tabs · Co 5 tabs |
| A2.2 | Tab set server-configurable | ✅ | ✅ | Co: `remote-config.json` `tabs` |
| A2.3 | Default tab layout | ✅ | ✅ | VM Home·Downloader·[GC]·[YTM]·Me · Co Home·Explore·Library·Shorts·Me |
| A2.4 | "Earn cash" alternate layout | ✅ | ➖ | VM `main_earn_cash` variant |
| A2.5 | Per-tab back-stack (nested nav) | ✅ | ✅ | Co: Navigation component |
| A2.6 | Floating action / download button | ✅ | ✅ | Co: queue FAB |
| A2.7 | Download-count badge | ✅ | ✅ | Co: badge on queue button |
| A2.8 | Pull-to-refresh on feeds | ✅ | ⚠️ | Co: reload action, not a swipe gesture |
| A2.9 | Settings reachable from shell | ✅ | ✅ | Co: Me → Settings |
| A2.10 | Persistent mini-player bar | ✅ | ✅ | Co: music mini-player |
| A2.11 | Bottom-bar visibility driven by config | ✅ | ✅ | Co: `applyRemoteTabs()` |

## A3. Screen inventory (count of distinct user-facing screens)

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A3.1 | Home / discovery | ✅ | ✅ | |
| A3.2 | Search screen | ✅ | ✅ | Co: Explore → Search |
| A3.3 | Trending / discovery | ✅ | ✅ | Co: Explore → Trending |
| A3.4 | Video detail screen | ✅ | ✅ | Co: resolve/picker flow |
| A3.5 | Full-screen player | ✅ | ✅ | Co: `VideoPlayerDialogFragment` |
| A3.6 | Downloads / files screen | ✅ | ✅ | Co: Library → Downloads |
| A3.7 | Download queue sheet | ✅ | ✅ | Co: `DownloadQueueSheet` |
| A3.8 | In-app browser | ✅ | ✅ | Co: `BrowserFragment` |
| A3.9 | Local media library | ✅ | ✅ | Co: `MediaLibraryFragment` |
| A3.10 | Private vault | ✅ | ✅ | Co: `VaultFragment` |
| A3.11 | WhatsApp status saver | ✅ | ✅ | Co: `StatusFragment` |
| A3.12 | Music screen | ✅ | ✅ | Co: `MusicFragment` |
| A3.13 | YouTube Music | ✅ | ✅ | Co: Explore → Music |
| A3.14 | Channel pages | ✅ | ✅ | Co: `ChannelFragment` |
| A3.15 | Comments | ✅ | ✅ | Co: comments on Home items |
| A3.16 | Games / web-games hub | ✅ | ✅ | Co: `GamesFragment` |
| A3.17 | App store (9Apps) | ✅ | ➖ | |
| A3.18 | Wallet / earn-cash | ✅ | ➖ | |
| A3.19 | Cleaner / speed-booster | ✅ | ➖ | |
| A3.20 | Daily-tasks / rewards | ✅ | ➖ | |
| A3.21 | Settings (multiple sub-screens) | ✅ | ✅ | Co: one Compose settings screen |
| A3.22 | History / bookmarks | ✅ | ✅ | Co: Library → History |
| A3.23 | Language picker screen | ✅ | ✅ | Co: in Settings |
| A3.24 | Region/location picker screen | ✅ | ✅ | Co: in Settings |
| A3.25 | Download-path picker screen | ✅ | ✅ | Co: text field in Settings |
| A3.26 | Push-settings screen | ✅ | ✅ | Co: notification toggles |
| A3.27 | Widget-settings screen | ✅ | ❌ | |
| A3.28 | Safe-mode screen | ✅ | ➖ | |
| A3.29 | Login / account screen | ✅ | ➖ | Co has no account by design |
| A3.30 | Update / "safe update" screen | ✅ | ⚠️ | Co: in-app update check, no dedicated screen |

## A4. Home & discovery UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A4.1 | Recommendation feed | ✅ | ✅ | Co: InnerTube-backed |
| A4.2 | Feed grid layout | ✅ | ✅ | Co: 2-column grid |
| A4.3 | Section headers | ✅ | ✅ | Co: "Recommended" |
| A4.4 | Home pill search bar | ✅ | ✅ | |
| A4.5 | Feed ad slots | ✅ | ➖ | VM `home_feed_page_ad` |
| A4.6 | Adult-content hiding in feed | ✅ | ➖ | No such content in Cobalt |
| A4.7 | Feed refresh / rotate | ✅ | ✅ | Co: query-pool rotation |
| A4.8 | User-tunable feed topics | ❌ | ✅ | Co: feed query pool (Cobalt-only) |

## A5. Download UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A5.1 | Paste-a-link box | ✅ | ✅ | |
| A5.2 | Link validity feedback | ✅ | ✅ | Co: "doesn't look like a valid link" |
| A5.3 | Format/quality picker sheet | ✅ | ✅ | |
| A5.4 | Resolution labels ("1080p" etc.) | ✅ | ✅ | Co: FFmpeg profile labels |
| A5.5 | Audio-only option | ✅ | ✅ | Co: audio format + audio-only mode |
| A5.6 | Default quality preference | ✅ | ✅ | |
| A5.7 | Download progress row (bytes/%) | ✅ | ✅ | |
| A5.8 | Download speed display (MB/s) | ✅ | ✅ | Co: `speedBps` → "x.x MB/s" |
| A5.9 | Multi-select downloads | ✅ | ✅ | |
| A5.10 | Playlist / batch download dialog | ✅ | ✅ | Co: real playlist expansion + line batch |
| A5.11 | Cancel in-progress download | ✅ | ✅ | Co: `DownloadCanceller` |
| A5.12 | Retry a failed download | ✅ | ✅ | Co: retry + auto-retry |
| A5.13 | Open a finished download | ✅ | ✅ | |
| A5.14 | Delete a finished download | ✅ | ✅ | |
| A5.15 | Clear download history | ✅ | ✅ | |
| A5.16 | Download-in-progress notification | ✅ | ✅ | |
| A5.17 | Download-complete notification | ✅ | ✅ | |
| A5.18 | Pre-download interstitial ad | ✅ | ➖ | VM `task_add_inters_ad` |

## A6. Player UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A6.1 | Inline player controls | ✅ | ✅ | Both Media3/ExoPlayer |
| A6.2 | Full-screen / landscape | ✅ | ✅ | |
| A6.3 | Picture-in-picture | ✅ | ✅ | Co: `enterPictureInPictureMode` |
| A6.4 | Floating pop-up window | ✅ | ❌ | VM `SYSTEM_ALERT_WINDOW` layer |
| A6.5 | Playback-speed menu | ✅ | ✅ | |
| A6.6 | Quality switch in player | ✅ | ✅ | |
| A6.7 | Subtitle toggle | ✅ | ✅ | |
| A6.8 | Equalizer panel | ✅ | ✅ | Co: `EqualizerController` |
| A6.9 | Pre-roll ad | ✅ | ➖ | VM `pre_rolls_ad` |
| A6.10 | Gesture controls (seek/brightness) | ✅ | ⚠️ | Co: basic seek bar |
| A6.11 | Next/prev in a Shorts feed | ✅ | ✅ | Co: preloads next Short |
| A6.12 | Resume playback position | ✅ | ⚠️ | Co: per-session, not persisted |

## A7. Library & local-media UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A7.1 | Downloads list with thumbnails | ✅ | ✅ | |
| A7.2 | Local video library grid | ✅ | ✅ | Co: MediaStore scan |
| A7.3 | Local music library | ✅ | ✅ | |
| A7.4 | Search within local media | ✅ | ✅ | Co: `LocalMediaQuery` |
| A7.5 | Sort / filter chips | ✅ | ✅ | Co: sort + kind filter |
| A7.6 | Multi-select + batch delete | ✅ | ✅ | |
| A7.7 | File detail / info | ✅ | ⚠️ | Co: row metadata only |
| A7.8 | Watch history timeline | ✅ | ✅ | |
| A7.9 | Liked / bookmarks list | ✅ | ✅ | |
| A7.10 | Storage-usage display | ✅ | ❌ | |

## A8. In-app browser UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A8.1 | Browser with URL bar | ✅ | ✅ | |
| A8.2 | Multi-tab UI | ✅ | ✅ | |
| A8.3 | Incognito toggle | ✅ | ✅ | |
| A8.4 | Desktop-site toggle | ✅ | ✅ | |
| A8.5 | Media-sniff download prompt | ✅ | ✅ | Co: `MediaSniffer` |
| A8.6 | "Download this page's video" CTA | ✅ | ✅ | |
| A8.7 | Web-game browser mode | ✅ | ✅ | Co: Games hub → browser |
| A8.8 | Browser history / bookmarks | ✅ | ❌ | |
| A8.9 | Browser anti-detection | ✅ | ❌ | Excluded by design |

## A9. Vault UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A9.1 | Passcode gate | ✅ | ✅ | Co: vault passcode screen |
| A9.2 | Create-passcode first-run flow | ✅ | ✅ | |
| A9.3 | Import into vault | ✅ | ✅ | Co: SAF picker |
| A9.4 | Export from vault | ✅ | ✅ | |
| A9.5 | Hidden from system gallery | ✅ | ✅ | Co: app-private storage |
| A9.6 | "Hide/unhide" confirmation UX | ✅ | ⚠️ | Co: simpler confirm |

## A10. Music UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A10.1 | Music player screen | ✅ | ✅ | |
| A10.2 | Now-playing with artwork | ✅ | ✅ | |
| A10.3 | Lyrics view | ✅ | ✅ | Co: `LrcParser` |
| A10.4 | Lyrics search (online) | ✅ | ❌ | VM `LrcSearchActivity` |
| A10.5 | Playlists | ✅ | ✅ | Co: Room playlists |
| A10.6 | Albums / singers browse | ✅ | ❌ | |
| A10.7 | Music discovery feed | ✅ | ✅ | Co: Explore → Music |
| A10.8 | Equalizer | ✅ | ✅ | |
| A10.9 | Sleep timer | ✅ | ❌ | |
| A10.10 | Offline / Bluetooth share | ✅ | ❌ | |

## A11. Settings UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A11.1 | Theme picker | ✅ | ✅ | |
| A11.2 | Language picker | ✅ | ✅ | |
| A11.3 | Region / content-locale picker | ✅ | ✅ | |
| A11.4 | Download-path selector | ✅ | ✅ | Co: text field |
| A11.5 | Default quality/format | ✅ | ✅ | |
| A11.6 | Clipboard trigger toggle | ✅ | ✅ | |
| A11.7 | Audio-only mode | ✅ | ✅ | |
| A11.8 | Notification toggles | ✅ | ✅ | |
| A11.9 | Recommendation opt-in toggle | ❌ | ✅ | Co-only ("recommend videos for me") |
| A11.10 | Widget settings | ✅ | ❌ | |
| A11.11 | Adult-content hide | ✅ | ➖ | |
| A11.12 | Safe mode | ✅ | ➖ | |
| A11.13 | Invidious instance pool | ❌ | ✅ | Cobalt-only |
| A11.14 | Shorts query pool | ❌ | ✅ | Cobalt-only |
| A11.15 | Home-feed query pool | ❌ | ✅ | Cobalt-only |
| A11.16 | Cobalt API-key field | ❌ | ✅ | Cobalt-only |
| A11.17 | Clear history action | ✅ | ✅ | |
| A11.18 | Battery-optimisation shortcut | ✅ | ✅ | |
| A11.19 | Check-for-update action | ✅ | ✅ | Co: `UpdateChecker` |

## A12. Theming & visual design

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A12.1 | Light theme | ✅ | ✅ | |
| A12.2 | Dark / night theme | ✅ | ✅ | |
| A12.3 | Follow-system theme | ✅ | ✅ | Co: `DYNAMIC` mode |
| A12.4 | Material-You dynamic colour | ❌ | ✅ | Cobalt-only (Android 12+) |
| A12.5 | Brand-blue accent palette | ✅ | ✅ | Co: VidMate-derived palette |
| A12.6 | Accent/gold highlights | ✅ | ✅ | |
| A12.7 | Custom animated widgets | ✅ | ⚠️ | Co: progress ring only |
| A12.8 | Rounded feed tiles | ✅ | ✅ | |
| A12.9 | Server-managed skins | ✅ | ❌ | VM `SkinManager` |

## A13. Localisation

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A13.1 | In-app language picker | ✅ | ✅ | Co: 15 options |
| A13.2 | In-app region picker | ✅ | ✅ | Co: `RegionHelper` |
| A13.3 | Translated string resources | ✅ (33) | ❌ (1) | Co ships one `values/strings.xml` |
| A13.4 | Selectable UI languages | ✅ (83) | ⚠️ (15) | Co: picker switches locale, strings mostly English |
| A13.5 | Selectable regions | ✅ (107) | ⚠️ | Co: content-region bias |
| A13.6 | RTL layout support | ✅ | ✅ | Co: `supportsRtl` |
| A13.7 | Language-of-content rule (max 2) | ✅ | ❌ | |

## A14. Onboarding, permissions & guidance

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A14.1 | First-run walkthrough | ✅ | ✅ | |
| A14.2 | Notification-permission explainer | ✅ | ✅ | Co: request + rationale |
| A14.3 | Overlay-permission guide | ✅ | ➖ | VM floating window |
| A14.4 | Storage-permission guide | ✅ | ➖ | Co uses scoped storage |
| A14.5 | Battery-optimisation prompt | ✅ | ✅ | Co: exempt-battery flow |
| A14.6 | Install-packages prompt (self-update) | ✅ | ✅ | Co: `REQUEST_INSTALL_PACKAGES` |
| A14.7 | VPN/geo-failure guidance | ✅ | ✅ | Co: `ResolveFailureAdvisor` |
| A14.8 | Empty-state guidance | ✅ | ✅ | |

## A15. Notifications & widgets UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A15.1 | Download-progress notification | ✅ | ✅ | |
| A15.2 | Download-complete notification | ✅ | ✅ | |
| A15.3 | Music-player notification | ✅ | ✅ | Co: MediaSession notification |
| A15.4 | Recommendation notification | ✅ | ✅ | Co: "because you watched X" |
| A15.5 | Rich notification layouts | ✅ (22) | ⚠️ | Co: standard layouts |
| A15.6 | Notification action buttons | ✅ | ✅ | Co: Open / retry actions |
| A15.7 | Separate notification channels | ✅ | ✅ | Co: downloads + recommendations |
| A15.8 | Home-screen app widgets | ✅ (3) | ✅ (1) | Co: downloads widget |
| A15.9 | Widget deep-link into app | ✅ | ✅ | |

## A16. Sharing UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A16.1 | Share sheet out | ✅ | ✅ | |
| A16.2 | Receive shares from other apps | ✅ | ✅ | |
| A16.3 | Share to specific social apps | ✅ | ✅ | Co: system chooser |
| A16.4 | Offline / Bluetooth share | ✅ | ❌ | |
| A16.5 | QR / proximity share | ✅ | ❌ | |

## A17. Growth & monetisation UX

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| A17.1 | In-app app store (9Apps) | ✅ | ➖ | |
| A17.2 | Game centre / web games | ✅ | ⚠️ | Co: games portal hub only |
| A17.3 | Earn-cash / wallet UX | ✅ | ➖ | |
| A17.4 | Daily tasks / coins | ✅ | ➖ | |
| A17.5 | Cleaner / speed-booster | ✅ | ➖ | |
| A17.6 | Interstitial / banner / native ads | ✅ | ➖ | |
| A17.7 | Exit-popup ad | ✅ | ➖ | VM `exit_popup_ad` |
| A17.8 | Ad-free experience | ❌ | ✅ | Cobalt-only |

---

# PART B — FEATURES & CAPABILITIES

## B1. Download engine

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B1.1 | HTTP(S) download | ✅ | ✅ | |
| B1.2 | Multi-segment / range downloads | ✅ | ✅ | Co: `SegmentedDownloadEngine` |
| B1.3 | Per-part URL fail-over (CDN fallback) | ✅ | ✅ | Co: per-`Segment` URL |
| B1.4 | Per-part path fail-over | ✅ | ⚠️ | Co: single temp file |
| B1.5 | Resume an interrupted download | ✅ | ✅ | Co: ETag-guarded |
| B1.6 | DASH / adaptive segmented | ✅ | ✅ | |
| B1.7 | HLS download | ✅ | ✅ | Co: saves HLS |
| B1.8 | Audio+video merge (mux) | ✅ | ✅ | VM native codec · Co FFmpeg |
| B1.9 | Video → audio extraction | ✅ | ✅ | |
| B1.10 | Quality/format conversion tiers | ✅ | ✅ | Co: FFmpeg ladder |
| B1.11 | BitTorrent / magnet | ✅ | ❌ | VM `download.bt.core.*` |
| B1.12 | SABR (segmented adaptive) | ✅ | ❌ | |
| B1.13 | Playlist / batch download | ✅ | ✅ | Co: real playlist + line batch |
| B1.14 | Parallel segment count control | ✅ | ⚠️ | Co: fixed concurrency |
| B1.15 | Auto-retry on network failure | ✅ | ✅ | Co: 3 retries |
| B1.16 | Storage-full handling | ✅ | ✅ | |
| B1.17 | Custom download location | ✅ | ✅ | |
| B1.18 | Foreground download service | ✅ | ✅ | |
| B1.19 | Downloads survive UI death | ✅ | ✅ | Co: WorkManager + service |
| B1.20 | Persisted download state | ✅ | ✅ | VM Room+MMKV · Co Room |
| B1.21 | AIDL / cross-process download service | ✅ | ❌ | Co: single-process |
| B1.22 | Cancel in-progress download | ✅ | ✅ | |
| B1.23 | Cancel in-progress transcode | ✅ | ✅ | Co: FFmpegKit cancel |
| B1.24 | Download speed metering | ✅ | ✅ | |

## B2. Site support & extraction

| # | Site / capability | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B2.1 | YouTube | ✅ | ✅ | |
| B2.2 | TikTok | ✅ | ✅ | |
| B2.3 | Twitter / X | ✅ | ✅ | |
| B2.4 | Instagram | ✅ | ✅ | |
| B2.5 | Facebook | ✅ | ✅ | |
| B2.6 | Dailymotion | ✅ | ✅ | |
| B2.7 | Vimeo | ✅ | ✅ | |
| B2.8 | Pinterest | ✅ | ✅ | |
| B2.9 | Reddit | ❓ | ✅ | |
| B2.10 | SoundCloud | ❓ | ✅ | |
| B2.11 | Twitch | ❓ | ✅ | |
| B2.12 | Bilibili | ❓ | ✅ | |
| B2.13 | Tumblr | ❓ | ✅ | |
| B2.14 | Broad catch-all extractor | ✅ | ✅ | Co: any `http(s)` host offered |
| B2.15 | Extractor updatable without app update | ✅ | ✅ | Co: remote `supportedHosts` |
| B2.16 | Media sniffing from web pages | ✅ | ✅ | Co: `MediaSniffer` |
| B2.17 | VPN hint on failure | ✅ | ✅ | Co: `ResolveFailureAdvisor` |
| B2.18 | Playlist URL support | ✅ | ✅ | Co: `PlaylistRepository` |
| B2.19 | YouTube public search / trending | ✅ | ✅ | Co: InnerTube |
| B2.20 | YouTube Music discovery | ✅ | ✅ | Co: `FEmusic_home` |
| B2.21 | Channel pages | ✅ | ✅ | |
| B2.22 | Comments | ✅ | ✅ | |
| B2.23 | YouTube sign-in (account state) | ✅ | ❌ | Needs OAuth; public surface done |
| B2.24 | Subscriptions / watch-later / history | ✅ | ❌ | Account-only |
| B2.25 | Clipboard link detection | ✅ | ✅ | |
| B2.26 | Share-sheet intake | ✅ | ✅ | |
| B2.27 | JS/WebView-driven extraction | ✅ | ❌ | Co uses cobalt backend |

## B3. Media playback

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B3.1 | Inline video playback | ✅ | ✅ | |
| B3.2 | Full-screen playback | ✅ | ✅ | |
| B3.3 | HLS / DASH playback | ✅ | ✅ | |
| B3.4 | Picture-in-picture | ✅ | ✅ | |
| B3.5 | Floating overlay player | ✅ | ❌ | |
| B3.6 | Background audio | ✅ | ✅ | Co: `MusicPlaybackService` |
| B3.7 | Media-button / headset control | ✅ | ✅ | Co: MediaSession |
| B3.8 | Playback speed | ✅ | ✅ | |
| B3.9 | Subtitle / caption support | ✅ | ✅ | |
| B3.10 | In-player quality switch | ✅ | ✅ | |
| B3.11 | Equalizer | ✅ | ✅ | |
| B3.12 | Volume / brightness gestures | ✅ | ⚠️ | |
| B3.13 | Shorts autoplay + preload | ✅ | ✅ | |
| B3.14 | One-live-player discipline | ✅ | ✅ | |
| B3.15 | Playback-position persistence | ✅ | ⚠️ | |

## B4. Library, local media & vault

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B4.1 | Local video scanner | ✅ | ✅ | Co: MediaStore |
| B4.2 | Local music scanner | ✅ | ✅ | |
| B4.3 | Local media search | ✅ | ✅ | |
| B4.4 | Sort / filter | ✅ | ✅ | |
| B4.5 | Multi-select | ✅ | ✅ | |
| B4.6 | Delete a file from the app | ✅ | ✅ | |
| B4.7 | Watch history | ✅ | ✅ | |
| B4.8 | Likes / bookmarks | ✅ | ✅ | |
| B4.9 | Dislike | ✅ | ❌ | |
| B4.10 | Private vault (passcode) | ✅ | ✅ | |
| B4.11 | Vault import / export | ✅ | ✅ | |
| B4.12 | Storage-usage stats | ✅ | ❌ | |

## B5. In-app browser

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B5.1 | In-app browser | ✅ | ✅ | |
| B5.2 | Multi-tab | ✅ | ✅ | |
| B5.3 | Incognito | ✅ | ✅ | |
| B5.4 | Desktop-site toggle | ✅ | ✅ | |
| B5.5 | Media sniffing | ✅ | ✅ | |
| B5.6 | Web-game browsing | ✅ | ✅ | |
| B5.7 | History / bookmarks | ✅ | ❌ | |
| B5.8 | Browser anti-detection | ✅ | ❌ | Excluded by design |

## B6. Discovery & engagement

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B6.1 | Home recommendation feed | ✅ | ✅ | |
| B6.2 | Trending / discovery | ✅ | ✅ | Co: `FEtrending` |
| B6.3 | In-app search with results | ✅ | ✅ | Co: Explore → Search |
| B6.4 | Search-result paging | ✅ | ✅ | Co: continuation tokens |
| B6.5 | Channel pages | ✅ | ✅ | |
| B6.6 | Shorts feed | ✅ | ✅ | Co: 4 merged sources |
| B6.7 | Like / dislike | ✅ | ⚠️ | Co: like only |
| B6.8 | Comments | ✅ | ✅ | |
| B6.9 | Recommendation engine ("because you watched") | ✅ | ✅ | Co: on-device, opt-in |
| B6.10 | Adult-content filter | ✅ | ➖ | |

## B7. Music

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B7.1 | Music player | ✅ | ✅ | |
| B7.2 | Albums / singers browse | ✅ | ❌ | |
| B7.3 | Lyrics | ✅ | ✅ | Co: `LrcParser` |
| B7.4 | Online lyrics search | ✅ | ❌ | |
| B7.5 | Playlists | ✅ | ✅ | |
| B7.6 | Equalizer | ✅ | ✅ | |
| B7.7 | Sleep timer | ✅ | ❌ | |
| B7.8 | YouTube Music | ✅ | ✅ | |
| B7.9 | Offline / Bluetooth share | ✅ | ❌ | |

## B8. Social & sharing

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B8.1 | WhatsApp status saver | ✅ | ✅ | Co: `status/*` |
| B8.2 | Social share sheet | ✅ | ✅ | |
| B8.3 | Offline / Bluetooth share | ✅ | ❌ | |
| B8.4 | Share-link shorteners | ✅ | ➖ | VM marketing hosts |

## B9. Notifications & push

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B9.1 | Download notifications | ✅ | ✅ | |
| B9.2 | Music notifications | ✅ | ✅ | |
| B9.3 | Recommendation notifications | ✅ | ✅ | Co: local, on-device |
| B9.4 | Separate channels | ✅ | ✅ | |
| B9.5 | Server-originated push (FCM) | ✅ | ❌ | Needs Firebase project |
| B9.6 | Keep-alive / resident notice | ✅ | ➖ | |
| B9.7 | App widgets | ✅ (3) | ✅ (1) | |

## B10. Platform & technical

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B10.1 | Plugin architecture | ✅ | ❌ | VM Tencent Shadow |
| B10.2 | Dynamic code loading / hot-fix | ✅ | ❌ | Excluded by design |
| B10.3 | Native codec libraries | ✅ | ✅ | VM codec · Co FFmpeg |
| B10.4 | Server-driven remote config | ✅ | ✅ | Co: `remote-config.json` |
| B10.5 | Self-update (in-app APK install) | ✅ | ✅ | Co: `UpdateChecker` + `ApkInstaller` |
| B10.6 | Crash handler / auto-restart | ✅ | ✅ | Co: `CrashHandler` |
| B10.7 | Multi-process architecture | ✅ | ❌ | Co: single process |
| B10.8 | AIDL service interfaces | ✅ | ❌ | |
| B10.9 | Signed store release | ❌ | ❌ | Both sideload-only |
| B10.10 | Unit-test suite | ❓ | ✅ | Co: 111 JVM tests |
| B10.11 | Open-source license | ❌ | ✅ | Co ships `LICENSE` |
| B10.12 | 64-bit ABI support | ✅ | ✅ | |

## B11. Security & privacy posture

> For this section the mark means "does this behaviour/characteristic exist", so
> a VidMate ✅ here is **not** a point in VidMate's favour.

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| B11.1 | `MANAGE_EXTERNAL_STORAGE` | ✅ | ❌ | Co: scoped storage |
| B11.2 | `SYSTEM_ALERT_WINDOW` | ✅ | ❌ | |
| B11.3 | `REQUEST_INSTALL_PACKAGES` | ✅ | ✅ | Co: for self-update only |
| B11.4 | `GET_TASKS` / `KILL_BACKGROUND_PROCESSES` | ✅ | ❌ | |
| B11.5 | Notification-listener access | ✅ | ❌ | |
| B11.6 | Cleartext traffic allowed | ✅ | ⚠️ | Co ships network-security config |
| B11.7 | Runtime package-name spoofing | ✅ | ❌ | |
| B11.8 | Weak XOR string obfuscation | ✅ | ❌ | |
| B11.9 | First-party API keys in plaintext | ✅ | ❌ | |
| B11.10 | Ad / tracking SDKs | ✅ (6) | ❌ | |
| B11.11 | Process daemon / keep-alive | ✅ | ❌ | |
| B11.12 | Scoped storage (MediaStore / SAF) | ⚠️ | ✅ | |
| B11.13 | Dynamic code loading | ✅ | ❌ | |
| B11.14 | Packer / anti-tamper stack | ✅ | ❌ | |

---

## Cobalt has that VidMate does not

| # | Feature | Notes |
|---|---|---|
| C1 | Ad-free by construction | No ad SDKs, no tracking |
| C2 | Material-You dynamic colour | Android 12+ |
| C3 | 4-source merged Shorts pipeline | InnerTube / Invidious / NewPipe / PeerTube |
| C4 | 111-test JVM suite | All green |
| C5 | Open-source license | `LICENSE` present |
| C6 | Invidious instance pool | User-tunable |
| C7 | Shorts query pool | User-tunable |
| C8 | Home-feed query pool | User-tunable |
| C9 | Optional cobalt API-key field | |
| C10 | Scoped storage by default | No `MANAGE_EXTERNAL_STORAGE` |
| C11 | On-device recommender, no account | Local likes/history only |
| C12 | Extra confirmed sites | Reddit, SoundCloud, Twitch, Bilibili, Tumblr |

---

## Scorecard

Counted programmatically from the tables above. Section A17 and B-section rows
marked ➖ (monetisation) are excluded — that layer is a deliberate non-goal.
Section B11 is excluded from the "Cobalt gap" tally, because a VidMate ✅ there
describes a behaviour Cobalt intentionally lacks.

| Part | VidMate ✅ | Cobalt ✅ | Cobalt ⚠️ | Cobalt ❌ |
|---|:--:|:--:|:--:|:--:|
| Entry points, launch & first-run | 13 | 7 | 2 | 3 |
| Navigation shell | 11 | 9 | 1 | 0 |
| Screen inventory (count of distinct user-facing screens) | 30 | 22 | 1 | 1 |
| Home & discovery UX | 7 | 6 | 0 | 0 |
| Download UX | 18 | 17 | 0 | 0 |
| Player UX | 12 | 8 | 2 | 1 |
| Library & local-media UX | 10 | 8 | 1 | 1 |
| In-app browser UX | 9 | 7 | 0 | 2 |
| Vault UX | 6 | 5 | 1 | 0 |
| Music UX | 10 | 6 | 0 | 4 |
| Settings UX | 14 | 16 | 0 | 1 |
| Theming & visual design | 8 | 7 | 1 | 1 |
| Localisation | 7 | 3 | 2 | 2 |
| Onboarding, permissions & guidance | 8 | 6 | 0 | 0 |
| Notifications & widgets UX | 9 | 8 | 1 | 0 |
| Sharing UX | 5 | 3 | 0 | 2 |
| Growth & monetisation UX ¹ | 7 | 1 | 1 | 0 |
| Download engine | 24 | 19 | 2 | 3 |
| Site support & extraction | 22 | 24 | 0 | 3 |
| Media playback | 15 | 12 | 2 | 1 |
| Library, local media & vault | 12 | 10 | 0 | 2 |
| In-app browser | 8 | 6 | 0 | 2 |
| Discovery & engagement | 10 | 8 | 1 | 0 |
| Music | 9 | 5 | 0 | 4 |
| Social & sharing | 4 | 2 | 0 | 1 |
| Notifications & push | 7 | 5 | 0 | 1 |
| Platform & technical | 9 | 7 | 0 | 5 |
| Security & privacy posture ¹ | 14 | 2 | 1 | 11 |
| **Total (buildable rows)** | **297** | **236** | **17** | **40** |

¹ Excluded from the total: A17 is the monetisation layer (a deliberate
non-goal), and B11 describes security traits where a VidMate ✅ is not a point
in VidMate's favour.

Part A is the **UX** framing and Part B the **capability** framing of the same
app, so a few rows appear in both on purpose (browser anti-detection A8.9/B5.8,
Bluetooth share A10.10/A16.4/B8.3, sleep timer A10.9/B7.7, albums browse
A10.6/B7.2). Those are one gap described twice, not two.

---

## The short version

**VidMate is ahead where it is a growth-and-media suite:** the monetisation layer
(section A17), the account-only YouTube state (B2.23/B2.24), the heavy downloader
extras (BitTorrent B1.11, SABR B1.12), the floating overlay player (B3.5), the
music extras (albums, online lyrics, sleep timer, Bluetooth share), the
translated-language count (33 locale files, 83 selectable), the plugin/hot-fix
architecture (B10.1/B10.2), multi-process + AIDL (B10.7/B10.8), server FCM push
(B9.5), and the ~100-UA browser anti-detection (B5.8).

**Cobalt is ahead where it is a lean, private, modern downloader:** no ads or
trackers, Material You, scoped storage, a merged 4-source Shorts pipeline, an
on-device recommender that needs no account, a green 111-test suite, an
open-source license, and several user-tunable knobs VidMate has no equivalent
for.

**Net:** across the buildable rows VidMate covers **297** capabilities to
Cobalt's **236 ✅ (+17 ⚠️)**, with **40 ❌**. Cobalt's ❌ rows split three ways:

1. **Deliberate non-goals** — the whole monetisation layer, browser
   anti-detection, plugin framework, dynamic code loading, multi-process/AIDL.
2. **External-credential blockers** — account-only YouTube state (OAuth),
   server-originated FCM push (Firebase project).
3. **Genuine remaining engineering** — BitTorrent, SABR, floating overlay
   player, music albums/online-lyrics/sleep-timer/Bluetooth share, browser
   history/bookmarks, storage-usage stats, file detail view, translated strings,
   widget settings, dislike, saved playback position, parallel-segment control.

Group 3 is the honest backlog if the goal were feature parity with VidMate.
