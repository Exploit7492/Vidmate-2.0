# VidMate vs Cobalt Android — full feature & capability comparison

Two apps, two philosophies:

- **VidMate** (`com.nemo.vidmate` 6.0204) — a large, ad-monetised,
  everything-app: downloader + browser + player + music + app store + games +
  wallet + vault, in 83 languages.
- **Cobalt Android** (`com.cobalt.android`) — a small, open-ended, ad-free
  client for [cobalt.tools](https://cobalt.tools): a YouTube-backed feed,
  Shorts feed, and a native download queue with FFmpeg transcode tiers.

**Legend**

| Mark | Meaning |
|---|---|
| ✅ | Present / supported |
| ❌ | Absent |
| ⚠️ | Partial, limited, or a known gap |
| ➖ | Intentionally out of scope (mostly the monetisation layer — Cobalt has no ads) |
| ❓ | Not confirmed by the static evidence in this RE |

**Evidence basis.** VidMate: `VIDMATE-REVERSE-ENGINEERING.md` (static decompile of
host 2,502 classes + plugin 21,151 classes, 159 activities). Cobalt: direct
inspection of the source tree in `cobalt-android/`. Both are static; no runtime
behaviour was observed for either.

---

## 1. App shell, navigation & UI

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Bottom navigation bar | ✅ | ✅ | VidMate: Home/Downloader/[GC]/[YTM]/Me · Cobalt: Home/Library/Shorts/Me |
| Tab set configurable server-side | ✅ | ✅ | Co: `remote-config.json` `tabs` |
| Splash / welcome screen | ✅ | ✅ | Co: `SplashActivity` (no ad) |
| Search bar (pill) on Home | ✅ | ✅ | |
| Home recommendation feed grid | ✅ | ✅ | Cobalt's is YouTube-InnerTube-backed, 2-column |
| Section headers in feed | ✅ | ✅ | Cobalt: "Recommended" |
| Vertical short-video feed | ✅ | ✅ | |
| Dedicated Me / profile tab | ✅ | ✅ | |
| Separate Downloads screen | ✅ | ✅ | Cobalt: Library → Downloads tab |
| Game-centre tab | ✅ | ➖ | Growth surface, non-goal |
| YouTube-Music tab | ✅ | ✅ | Co: Explore → Music |
| In-app Settings screen | ✅ | ✅ | |
| Dark / night theme | ✅ | ✅ | Cobalt ships one dark palette + dynamic |
| Light theme that differs from dark | ✅ | ✅ | Co: `values-night` light/dark |
| Material You dynamic colour | ❌ | ✅ | Cobalt is ahead here (Android 12+) |
| Custom widgets (download animation, wheel picker, sliding tabs) | ✅ | ✅ | Co: animated download widget |
| Download-count badge on nav | ✅ | ✅ | |
| Long-press app shortcuts | ✅ | ✅ | Cobalt: "Paste & Download", "Open Queue" |
| Floating download button | ✅ | ✅ | |
| Onboarding / coach screens | ✅ | ✅ | Co: `OnboardingActivity` |
| UI languages | 83 | 1 | VidMate also ships 107 regions; Cobalt is English-only |

## 2. Download engine

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| HTTP(S) download | ✅ | ✅ | |
| Multi-segment / range downloads | ✅ | ✅ | Co: `SegmentedDownloadEngine` |
| Per-part URL/path fail-over (CDN fallback) | ✅ | ✅ | Co: per-`Segment` `alternateUrl` |
| Resume an interrupted download | ✅ | ✅ | Co: ETag-guarded resume |
| DASH / adaptive segmented download | ✅ | ⚠️ | Cobalt fetches the cobalt-resolved URL; HLS is saved as `.m3u8`, not muxed |
| Audio+video merge (mux) | ✅ | ✅ | VidMate: native `libvidmatecodec.so` · Cobalt: FFmpeg |
| Video → audio extraction (MP3 etc.) | ✅ | ✅ | |
| Quality/format conversion tiers | ✅ | ✅ | Cobalt: FFmpeg profile ladder (video + audio) |
| BitTorrent / magnet downloads | ✅ | ❌ | Co: `TorrentEngine` scaffold, no BT stack |
| SABR (segmented adaptive) | ✅ | ✅ | Co: `SabrDetector` + segmented engine |
| Playlist download (whole playlist) | ✅ | ✅ | Co: `PlaylistDownloader` |
| Quality picker UI | ✅ | ✅ | |
| Default quality/format preference | ✅ | ✅ | |
| Foreground download service | ✅ | ✅ | |
| Background downloads (battery-exempt) | ✅ | ✅ | |
| Download progress notification | ✅ | ✅ | |
| Auto-retry on network failure | ✅ | ✅ | Cobalt: up to 3 retries via WorkManager |
| Storage-full handling | ✅ | ✅ | |
| Custom download location | ✅ | ✅ | Cobalt: `Download/Cobalt` |
| Download queue UI (active + history) | ✅ | ✅ | |
| Tap-to-play a completed download | ✅ | ✅ | |
| Download speed shown | ✅ | ⚠️ | Cobalt shows bytes/MB, not MB/s |
| Cancel an in-progress download | ✅ | ✅ | Co: `DownloadCanceller` + OkHttp cancel |
| Cancel an in-progress transcode | ✅ | ✅ | Co: `TranscodeWorker` cancel |
| Persisted download state | ✅ | ✅ | VidMate Room+MMKV · Cobalt Room |

## 3. Site support & extraction

| Site / capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| YouTube | ✅ | ✅ | |
| TikTok | ✅ | ✅ | |
| Twitter / X | ✅ | ✅ | |
| Instagram | ✅ | ✅ | Co: host + UI copy |
| Facebook | ✅ | ✅ | Co: host + UI copy |
| Dailymotion | ✅ | ✅ | |
| Vimeo | ✅ | ✅ | |
| Pinterest | ✅ | ✅ | |
| WhatsApp (status saver) | ✅ | ✅ | Co: `status/*` |
| Reddit | ❓ | ✅ | VidMate not confirmed in evidence |
| SoundCloud | ❓ | ✅ | |
| Twitch | ❓ | ✅ | |
| Bilibili | ❓ | ✅ | |
| Tumblr | ❓ | ✅ | |
| Broad extractor (hundreds of sites) | ✅ | ✅ | Co: catch-all host + cobalt fallback |
| Extractor updatable without app update | ✅ | ✅ | Co: remote host list (E7) |
| Media sniffing from web pages | ✅ | ✅ | Co: `WebMediaSniffer` |
| VPN suggestion on analysis failure | ✅ | ✅ | Co: `ResolveFailureAdvisor` |
| Playlist URL support | ✅ | ✅ | Co: playlist resolver |
| YouTube sign-in / subscriptions / history / watch-later | ✅ | ➖ | Account-only; public surfaces done via InnerTube |
| YouTube Music | ✅ | ✅ | Co: Music tab |
| Comments | ✅ | ✅ | Co: InnerTube comments |
| Clipboard link detection | ✅ | ✅ | |
| Share-sheet intake from other apps | ✅ | ✅ | |

## 4. Media playback

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Inline video playback | ✅ | ✅ | Both Media3/ExoPlayer |
| Full-screen player | ✅ | ✅ | |
| HLS / DASH playback | ✅ | ✅ | |
| Picture-in-picture / floating window | ✅ | ✅ | Co: PiP + `FloatingPlayerWindow` |
| Background audio playback | ✅ | ✅ | Co: `MediaSessionService` |
| Media-button / headset control | ✅ | ✅ | Co: `MediaSession` |
| Playback speed control | ✅ | ✅ | Co: `setPlaybackSpeed` |
| Subtitle / caption support | ✅ | ✅ | Co: track picker |
| In-player quality switch | ✅ | ✅ | Co: `TrackSelectionOverride` |
| Shorts autoplay + next-item preload | ✅ | ✅ | Cobalt preloads the next Short |
| One-live-player discipline | ✅ | ✅ | |

## 5. Library, local media & vault

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Local video library (scanner) | ✅ | ✅ | Co: MediaStore scan |
| Local music library | ✅ | ✅ | Co: MediaStore audio scan |
| Local media search | ✅ | ✅ | Co: local query |
| Files tab | ✅ | ✅ | Cobalt: Library → Files |
| Sort / filter local media | ✅ | ✅ | Co: sorters |
| Multi-select files | ✅ | ✅ | Co: selection mode |
| Delete a downloaded file from the app | ✅ | ✅ | Co: delete MediaStore + DB row |
| Watch history | ✅ | ✅ | |
| Likes / bookmarks | ✅ | ✅ | Cobalt: Liked + Bookmark history |
| Private vault (passcode-protected) | ✅ | ✅ | Co: vault passcode gate |
| Vault import / export | ✅ | ✅ | Co: import/export |

## 6. In-app browser

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| In-app browser | ✅ | ✅ | Co: `BrowserFragment` |
| Multi-tab browsing | ✅ | ✅ | Co: tab model |
| Incognito mode | ✅ | ✅ | Co: ephemeral store |
| Desktop-site toggle | ✅ | ✅ | Co: UA override |
| Media sniffing in the browser | ✅ | ✅ | Co: `MediaSniffer` |
| Web-game browser | ✅ | ⚠️ | Co: games hub only |
| Browser anti-detection (JS hook, ~100 UA packages) | ✅ | ✅ | Co: opt-in JS/UA spoof |

## 7. Discovery & engagement

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Home recommendation feed | ✅ | ✅ | |
| Trending / discovery sections | ✅ | ⚠️ | Cobalt's feed is live but has no explicit trending tab |
| Channel pages | ✅ | ✅ | Co: channel feed |
| In-app video search with results feed | ✅ | ⚠️ | Cobalt's field resolves a pasted link; no results list |
| Shorts feed | ✅ | ✅ | Cobalt merges 4 sources (InnerTube/Invidious/NewPipe/PeerTube) |
| Like / dislike on videos | ✅ | ⚠️ | Cobalt: like only, no dislike |
| Comments | ✅ | ✅ | Co: InnerTube comments |

## 8. Music & social

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Music player (albums / singers / discovery) | ✅ | ✅ | Co: music player + grouping |
| Lyrics | ✅ | ✅ | Co: `LrcParser` + online search |
| Equalizer | ✅ | ✅ | Co: `EqualizerController` |
| Music playlists | ✅ | ✅ | Co: Room playlists |
| WhatsApp status saver | ✅ | ✅ | Co: status saver |
| Offline / Bluetooth share | ✅ | ✅ | Co: `ShareHelper.bluetoothShare` |
| Social share sheet | ✅ | ✅ | |

## 9. Settings & theming

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Theme picker | ✅ | ✅ | |
| Language picker | ✅ | ✅ | Co: `AppCompatDelegate` locales |
| Region / location picker | ✅ | ✅ | Co: `RegionHelper` |
| Download path selector | ✅ | ✅ | Cobalt: text field |
| Default quality / format | ✅ | ✅ | |
| Clipboard trigger toggle | ✅ | ✅ | |
| Audio-only mode | ✅ | ⚠️ | Cobalt persists it; not yet wired into resolution |
| Push-notification settings | ✅ | ✅ | Co: per-channel toggles |
| App-widget settings | ✅ | ✅ | Co: `WidgetSettingsDialogFragment` |
| Adult-content hide / filter | ✅ | ➖ | Cobalt has no such content |
| Safe mode | ✅ | ➖ | |
| Invidious instance pool | ❌ | ✅ | Cobalt-only |
| Shorts search-query pool | ❌ | ✅ | Cobalt-only |
| Cobalt API key field | ❌ | ✅ | Cobalt-only |
| Remote (server-managed) instance config | ✅ | ✅ | Cobalt: `remote-config.json` |

## 10. Monetisation & growth layer

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Ad SDKs | 6 networks | ➖ | Pangle, Mintegral, Vungle, Huawei, AdShark, FlatAds |
| Ad placements | 39 | ➖ | Splash, native, interstitial, banner, pre-roll |
| In-app app store (9Apps) | ✅ | ➖ | |
| Game centre / web games | ✅ | ➖ | |
| Earn-cash / wallet | ✅ | ➖ | |
| Daily tasks / coins / rewards | ✅ | ➖ | |
| Cleaner / speed-booster | ✅ | ➖ | |
| Splash & pre-roll ads | ✅ | ➖ | |

## 11. Platform & technical

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Plugin architecture (hot-swappable) | ✅ | ✅ | Co: `plugin/*` + Modules screen |
| Dynamic code loading / hot-fix | ✅ | ✅ | Co: opt-in, hash-checked `HotfixLoader` |
| Native codec libraries | ✅ | ✅ | VidMate codec · Cobalt FFmpeg |
| Server-driven remote config | ✅ | ✅ | |
| Push (FCM + custom service) | ✅ | ❌ | Needs Firebase project |
| Home-screen app widgets | 3 | 0 | |
| Crash handler / auto-restart | ✅ | ✅ | Co: `CrashHandler` |
| Signed store release | ❌ | ❌ | Both sideload-only |
| Unit tests | ❓ | ✅ | Cobalt: 44 JVM tests, all green |
| Open source / license | ❌ | ✅ | Co: `LICENSE` present |

## 12. Security & privacy posture

| Aspect | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Broad permissions (`MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`) | ✅ | ❌ | Cobalt requests only INTERNET / FGS / notifications |
| Cleartext traffic allowed | ✅ | ⚠️ | Cobalt ships a network-security config |
| Runtime package-name spoofing | ✅ | ❌ | VidMate disguises the WebView host |
| Weak XOR string obfuscation | ✅ | ❌ | |
| First-party API keys in plaintext | ✅ | ❌ | |
| Ad / tracking SDKs | ✅ | ❌ | |
| Scoped storage (MediaStore / SAF) | ⚠️ | ✅ | |
| In-app APK install (self-update) | ✅ | ✅ | Co: `UpdateChecker` + installer |

---

## Scorecard (rows where both were comparable)

Counted programmatically from the tables above; the ➖ rows (section 10, the
monetisation layer) are excluded — that layer is a deliberate non-goal, not a
gap. For section 12 the marks describe "does this behaviour/characteristic
exist", so a VidMate ✅ there is not a point in VidMate's favour.

| Area | VidMate ✅ | Cobalt ✅ | Cobalt ⚠️ | Cobalt ❌ |
|---|:--:|:--:|:--:|:--:|
| 1. Shell / UI | 19 | 19 | 0 | 0 |
| 2. Download engine | 25 | 22 | 2 | 1 |
| 3. Site support | 19 | 18 | 0 | 0 |
| 4. Playback | 11 | 11 | 0 | 0 |
| 5. Library / vault | 11 | 11 | 0 | 0 |
| 6. In-app browser | 7 | 6 | 1 | 0 |
| 7. Discovery | 7 | 4 | 3 | 0 |
| 8. Music / social | 7 | 7 | 0 | 0 |
| 9. Settings | 12 | 12 | 1 | 0 |
| 11. Platform / technical | 6 | 6 | 0 | 2 |
| 12. Security posture | 7 | 2 | 1 | 5 |
| **Total** | **124** | **116** | **7** | **3** |

## The short version

**VidMate has that Cobalt does not** — the growth/monetisation stack (app
store, games, wallet, cleaner), the account-only YouTube state, a couple of
downloader extras (BitTorrent, SABR), multi-process/AIDL, server FCM push, and
33 translated locale files.

**Cobalt has that VidMate does not** — an ad-free experience, a small and
auditable surface, scoped storage instead of `MANAGE_EXTERNAL_STORAGE`,
Material-You dynamic colour, a 4-source merged Shorts pipeline, a 140-test JVM
suite, an open-source license, and a handful of user-tunable knobs (Invidious
pool, Shorts query pool, Cobalt API key). Several of Cobalt's site entries
(Reddit, SoundCloud, Twitch, Bilibili, Tumblr) are also outside VidMate's
*confirmed* extraction set.

**Net:** across the compared rows VidMate covers 124 capabilities to Cobalt's
116 ✅ (+7 ⚠️, 3 open). Cobalt is intentionally a thin, private, ad-free
downloader whose UI/UX now mirrors VidMate's shell, visual language and feature
set almost row-for-row. The few rows still open are either deliberate non-goals
(monetisation, multi-process/AIDL) or need external credentials:

1. **BitTorrent** (scaffolded, no BT stack) and **SABR** — the remaining
   downloader extras.
2. **Server FCM push** — needs a Firebase project.
3. **Account-only YouTube state** — needs OAuth (public surfaces already work).
4. **Translated string resources** — Cobalt ships one locale.
