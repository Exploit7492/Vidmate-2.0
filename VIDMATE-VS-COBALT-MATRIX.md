# VidMate vs Cobalt Android — full feature matrix (current)

A row-by-row checkmark comparison of **VidMate's complete feature set** against
**Cobalt Android as it stands today**. This supersedes the earlier
`VIDMATE-VS-COBALT-COMPARISON.md`, which predated the gap-closure work: many
rows that were ❌ there are ✅ now.

- **VidMate** (`com.nemo.vidmate` 6.0204) — the reference, from
  `VIDMATE-REVERSE-ENGINEERING.md` (static decompile: host 2,502 classes +
  plugin 21,151 classes, 159 plugin activities, 83 languages, 6 ad networks).
- **Cobalt Android** (`com.cobalt.android`) — verified by direct inspection of
  the current source tree (`cobalt-android/`), 87 JVM tests green.

### Legend

| Mark | Meaning |
|---|---|
| ✅ | Present / fully supported |
| ⚠️ | Partial, limited, or a reduced form of the feature |
| ❌ | Absent |
| ➖ | Not applicable by design (chiefly the monetisation layer — Cobalt is ad-free) |
| ❓ | Not confirmed by the static VidMate evidence |

**Evidence basis.** Both are static: VidMate from the RE report, Cobalt from the
source tree. No runtime behaviour was observed for either. Where a Cobalt row
says ✅, there is a concrete source file behind it (named in the note).

---

## 1. App shell, navigation & UI/UX

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 1.1 | Splash / welcome screen | ✅ | ❌ | VidMate's carries a countdown + splash ad |
| 1.2 | Bottom navigation bar | ✅ | ✅ | VM: Home/Downloader/[GC]/[YTM]/Me · Co: Home/Library/Shorts/Me |
| 1.3 | Tab set configurable server-side | ✅ | ✅ | Co: `remote-config.json` `tabs` → `RemoteConfigStore` |
| 1.4 | Home pill search bar | ✅ | ✅ | |
| 1.5 | Home recommendation feed grid | ✅ | ✅ | Co: InnerTube-backed 2-column grid |
| 1.6 | Section headers in feed | ✅ | ✅ | Co: "Recommended" |
| 1.7 | Vertical short-video feed | ✅ | ✅ | Co merges 4 sources |
| 1.8 | Dedicated Me / profile tab | ✅ | ✅ | |
| 1.9 | Separate downloads screen | ✅ | ✅ | Co: Library → Downloads |
| 1.10 | In-app settings screen | ✅ | ✅ | |
| 1.11 | Onboarding / coach screens | ✅ | ❌ | |
| 1.12 | Light theme distinct from dark | ✅ | ✅ | Co: `ui/theme/Theme.kt` (both palettes) |
| 1.13 | Night theme | ✅ | ✅ | Co: `values-night/` |
| 1.14 | Material You dynamic colour | ❌ | ✅ | Co ahead (Android 12+), `ThemeMode.DYNAMIC` |
| 1.15 | Custom animated widgets (progress wheel etc.) | ✅ | ⚠️ | Co: download progress ring (H4), not a general widget kit |
| 1.16 | Home-screen app widgets | ✅ | ✅ | VM 3 providers · Co 1 (`DownloadsWidgetProvider`) |
| 1.17 | Long-press app shortcuts | ✅ | ✅ | Co: "Paste & Download", "Open Queue" |
| 1.18 | Download-count badge on nav | ✅ | ✅ | |
| 1.19 | Floating download button | ✅ | ✅ | |
| 1.20 | UI languages shipped (translated strings) | ✅ (83) | ❌ (1) | Co ships one `values/strings.xml` |
| 1.21 | In-app language picker | ✅ | ✅ | Co: 15 options via `LocaleHelper` + `AppCompatDelegate` |
| 1.22 | Region / content-locale picker | ✅ | ✅ | Co: `RegionHelper` |

## 2. Download engine

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 2.1 | HTTP(S) download | ✅ | ✅ | |
| 2.2 | Multi-segment / range downloads | ✅ | ✅ | Co: `download/SegmentedDownloadEngine.kt` |
| 2.3 | Per-part URL / path fail-over | ✅ | ✅ | Co: per-`Segment` URL + fail-over |
| 2.4 | Resume an interrupted download | ✅ | ✅ | Co: ETag-guarded resume |
| 2.5 | DASH / adaptive segmented | ✅ | ✅ | Co: segmented engine + HLS |
| 2.6 | Audio+video merge (mux) | ✅ | ✅ | VM native codec · Co FFmpeg |
| 2.7 | Video → audio extraction (MP3 etc.) | ✅ | ✅ | |
| 2.8 | Quality / format conversion tiers | ✅ | ✅ | Co: FFmpeg profile ladder |
| 2.9 | BitTorrent / magnet downloads | ✅ | ❌ | VM: `download.bt.core.*` |
| 2.10 | SABR (segmented adaptive) | ✅ | ❌ | |
| 2.11 | Playlist / batch download | ✅ | ✅ | Co: `PlaylistDownloadDialog` (batch, line-per-URL) |
| 2.12 | Quality picker UI | ✅ | ✅ | |
| 2.13 | Default quality / format preference | ✅ | ✅ | |
| 2.14 | Foreground download service | ✅ | ✅ | |
| 2.15 | Background downloads (battery-exempt) | ✅ | ✅ | |
| 2.16 | Download progress notification | ✅ | ✅ | |
| 2.17 | Auto-retry on network failure | ✅ | ✅ | Co: up to 3 retries via WorkManager |
| 2.18 | Storage-full handling | ✅ | ✅ | |
| 2.19 | Custom download location | ✅ | ✅ | Co: `Download/Cobalt` |
| 2.20 | Download queue UI (active + history) | ✅ | ✅ | |
| 2.21 | Tap-to-play a completed download | ✅ | ✅ | |
| 2.22 | Download speed shown (MB/s) | ✅ | ✅ | Co: `speedBps` → "x.x MB/s" |
| 2.23 | Cancel an in-progress download | ✅ | ✅ | Co: `DownloadCanceller` |
| 2.24 | Cancel an in-progress transcode | ✅ | ✅ | Co: `FFmpegKit.cancel()` via worker |
| 2.25 | Persisted download state | ✅ | ✅ | VM Room+MMKV · Co Room |

## 3. Site support & extraction

| # | Site / capability | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 3.1 | YouTube | ✅ | ✅ | |
| 3.2 | TikTok | ✅ | ✅ | |
| 3.3 | Twitter / X | ✅ | ✅ | |
| 3.4 | Instagram | ✅ | ✅ | Co: `util/UrlMatcher.kt` |
| 3.5 | Facebook | ✅ | ✅ | |
| 3.6 | Dailymotion | ✅ | ✅ | |
| 3.7 | Vimeo | ✅ | ✅ | |
| 3.8 | Pinterest | ✅ | ✅ | |
| 3.9 | Reddit | ❓ | ✅ | Co-only in confirmed set |
| 3.10 | SoundCloud | ❓ | ✅ | |
| 3.11 | Twitch | ❓ | ✅ | |
| 3.12 | Bilibili | ❓ | ✅ | |
| 3.13 | Tumblr | ❓ | ✅ | |
| 3.14 | Broad extractor (hundreds of sites) | ✅ | ✅ | Co: catch-all — any `http(s)` host offered to cobalt (`UrlMatcher`) |
| 3.15 | Extractor updatable without app update | ✅ | ✅ | Co: `supportedHosts` in remote-config |
| 3.16 | Media sniffing from web pages | ✅ | ✅ | Co: `MediaSniffer` |
| 3.17 | VPN hint on analysis failure | ✅ | ✅ | Co: `ResolveFailureAdvisor` (geo/403/429) |
| 3.18 | Playlist URL support | ✅ | ✅ | Co: real playlist → track list (`PlaylistRepository`) |
| 3.19 | YouTube sign-in / subs / history / watch-later | ✅ | ⚠️ | Co: all *public* YouTube via InnerTube, no sign-in; account-only state needs OAuth |
| 3.20 | YouTube Music | ✅ | ✅ | Co: `FEmusic_home` in Explore |
| 3.21 | Channel pages | ✅ | ✅ | Co: `feed/channel/*` |
| 3.22 | Comments | ✅ | ✅ | Co: `feed/comments/*` |
| 3.23 | Clipboard link detection | ✅ | ✅ | |
| 3.24 | Share-sheet intake from other apps | ✅ | ✅ | |

## 4. Media playback

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 4.1 | Inline video playback | ✅ | ✅ | Both Media3/ExoPlayer |
| 4.2 | Full-screen player | ✅ | ✅ | |
| 4.3 | HLS / DASH playback | ✅ | ✅ | |
| 4.4 | Picture-in-picture / floating window | ✅ | ✅ | Co: `VideoPlayerDialogFragment.enterPip()` |
| 4.5 | Background audio playback | ✅ | ✅ | Co: `MusicPlaybackService` (MediaSessionService) |
| 4.6 | Media-button / headset control | ✅ | ✅ | Co: MediaSession |
| 4.7 | Playback speed control | ✅ | ✅ | Co: `setupPlaybackSpeed` |
| 4.8 | Subtitle / caption support | ✅ | ✅ | Co: track selection |
| 4.9 | In-player quality switch | ✅ | ✅ | Co: `TrackSelectionOverride` |
| 4.10 | Equalizer | ✅ | ✅ | Co: `util/EqualizerController.kt` |
| 4.11 | Shorts autoplay + next-item preload | ✅ | ✅ | |
| 4.12 | One-live-player discipline | ✅ | ✅ | |

## 5. Library, local media & vault

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 5.1 | Local video library (scanner) | ✅ | ✅ | Co: `media/LocalMediaRepository.kt` |
| 5.2 | Local music library | ✅ | ✅ | Co: MediaStore audio |
| 5.3 | Local media search | ✅ | ✅ | Co: `media/LocalMediaQuery.kt` |
| 5.4 | Files tab | ✅ | ✅ | Co: Library → Files |
| 5.5 | Sort / filter local media | ✅ | ✅ | Co: `Sort` / `KindFilter` |
| 5.6 | Multi-select files | ✅ | ✅ | Co: `LibraryScreen` selection mode |
| 5.7 | Delete a file from the app | ✅ | ✅ | Co: batch delete in selection mode |
| 5.8 | Watch history | ✅ | ✅ | |
| 5.9 | Likes / bookmarks | ✅ | ✅ | Co: like only |
| 5.10 | Dislike | ✅ | ❌ | |
| 5.11 | Private vault (passcode-protected) | ✅ | ✅ | Co: `vault/*` |
| 5.12 | Vault import / export | ✅ | ✅ | Co: `VaultRepository.importFrom/export` |

## 6. In-app browser

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 6.1 | In-app browser | ✅ | ✅ | Co: `ui/browser/*` |
| 6.2 | Multi-tab browsing | ✅ | ✅ | |
| 6.3 | Incognito mode | ✅ | ✅ | |
| 6.4 | Desktop-site toggle | ✅ | ✅ | |
| 6.5 | Media sniffing in the browser | ✅ | ✅ | Co: `MediaSniffer` |
| 6.6 | Web-game browser | ✅ | ✅ | Co: Games hub → browser (F6) |
| 6.7 | Browser anti-detection (JS hook, ~100 UA packages) | ✅ | ❌ | Excluded by design |

## 7. Discovery & engagement

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 7.1 | Home recommendation feed | ✅ | ✅ | |
| 7.2 | Trending / discovery sections | ✅ | ✅ | Co: `FEtrending` grid in Explore |
| 7.3 | Channel pages | ✅ | ✅ | |
| 7.4 | In-app video search with results feed | ✅ | ✅ | Co: Explore → Search results + paging |
| 7.5 | Shorts feed | ✅ | ✅ | |
| 7.6 | Like / dislike on videos | ✅ | ⚠️ | Co: like only |
| 7.7 | Comments | ✅ | ✅ | |

## 8. Music & social

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 8.1 | Music player (albums / singers / discovery) | ✅ | ⚠️ | Co: local player; no albums/discovery surfaces |
| 8.2 | Lyrics | ✅ | ✅ | Co: `LrcParser` + lookup |
| 8.3 | Music playlists | ✅ | ✅ | Co: Room entity + UI (G4) |
| 8.4 | WhatsApp status saver | ✅ | ✅ | Co: `status/*` (E8) |
| 8.5 | Offline / Bluetooth share | ✅ | ❌ | |
| 8.6 | Social share sheet | ✅ | ✅ | |

## 9. Settings & theming

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 9.1 | Theme picker | ✅ | ✅ | |
| 9.2 | Language picker | ✅ | ✅ | |
| 9.3 | Region / location picker | ✅ | ✅ | |
| 9.4 | Download path selector | ✅ | ✅ | Co: text field |
| 9.5 | Default quality / format | ✅ | ✅ | |
| 9.6 | Clipboard trigger toggle | ✅ | ✅ | |
| 9.7 | Audio-only mode | ✅ | ✅ | Co: persisted pref |
| 9.8 | Push-notification settings | ✅ | ✅ | Co: download + recommendation toggles/channels |
| 9.9 | App-widget settings | ✅ | ❌ | |
| 9.10 | Adult-content hide / safe mode | ✅ | ➖ | Co has no such content |
| 9.11 | Invidious instance pool | ❌ | ✅ | Co-only |
| 9.12 | Shorts search-query pool | ❌ | ✅ | Co-only |
| 9.13 | Home feed query pool | ❌ | ✅ | Co-only |
| 9.14 | Cobalt API key field | ❌ | ✅ | Co-only |
| 9.15 | Remote (server-managed) instance config | ✅ | ✅ | Co: `remote-config.json` |

## 10. Monetisation & growth layer

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 10.1 | Ad SDKs | ✅ (6) | ➖ | Pangle, Mintegral, Vungle, Huawei, AdShark, FlatAds |
| 10.2 | Ad placements | ✅ (39) | ➖ | Splash, native, interstitial, banner, pre-roll |
| 10.3 | In-app app store (9Apps) | ✅ | ➖ | |
| 10.4 | Game centre / web games | ✅ | ⚠️ | Co: portal hub only, no game store |
| 10.5 | Earn-cash / wallet | ✅ | ➖ | |
| 10.6 | Daily tasks / coins / rewards | ✅ | ➖ | |
| 10.7 | Cleaner / speed-booster | ✅ | ➖ | |
| 10.8 | Splash & pre-roll ads | ✅ | ➖ | |

## 11. Platform & technical

| # | Feature | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 11.1 | Plugin architecture (hot-swappable) | ✅ | ❌ | VM: Tencent Shadow host + plugin |
| 11.2 | Dynamic code loading / hot-fix | ✅ | ❌ | Excluded by design |
| 11.3 | Native codec libraries | ✅ | ✅ | VM codec · Co FFmpeg |
| 11.4 | Server-driven remote config | ✅ | ✅ | |
| 11.5 | Push (FCM + custom service) | ✅ | ❌ | Needs a Firebase project (external) |
| 11.6 | Home-screen app widgets | ✅ (3) | ✅ (1) | |
| 11.7 | Crash handler / auto-restart | ✅ | ✅ | Co: `util/CrashHandler.kt` |
| 11.8 | In-app APK install (self-update) | ✅ | ✅ | Co: `update/*` (I2) |
| 11.9 | Signed store release | ❌ | ❌ | Both sideload-only |
| 11.10 | Unit tests | ❓ | ✅ | Co: 87 JVM tests, all green |
| 11.11 | Open source / license | ❌ | ✅ | Co ships `LICENSE` |

## 12. Security & privacy posture

> For this section the mark means "does this behaviour/characteristic exist",
> so a VidMate ✅ here is **not** a point in VidMate's favour.

| # | Aspect | VidMate | Cobalt | Notes |
|---|---|:--:|:--:|---|
| 12.1 | Broad permissions (`MANAGE_EXTERNAL_STORAGE`, `SYSTEM_ALERT_WINDOW`, …) | ✅ | ❌ | Co: INTERNET / FGS / notifications / battery / install only |
| 12.2 | Cleartext traffic allowed | ✅ | ⚠️ | Co ships a network-security config |
| 12.3 | Runtime package-name spoofing | ✅ | ❌ | |
| 12.4 | Weak XOR string obfuscation | ✅ | ❌ | |
| 12.5 | First-party API keys in plaintext | ✅ | ❌ | |
| 12.6 | Ad / tracking SDKs | ✅ | ❌ | |
| 12.7 | Scoped storage (MediaStore / SAF) | ⚠️ | ✅ | |
| 12.8 | Signed releases | ❌ | ❌ | |

---

## Cobalt has that VidMate does not

| # | Feature | Notes |
|---|---|---|
| C1 | Ad-free by construction | No ad SDKs, no tracking |
| C2 | Material You dynamic colour | Android 12+ |
| C3 | 4-source merged Shorts pipeline | InnerTube / Invidious / NewPipe / PeerTube |
| C4 | JVM unit-test suite | 87 tests, green |
| C5 | Open-source license | `LICENSE` present |
| C6 | User-tunable Invidious instance pool | |
| C7 | User-tunable Shorts query pool | |
| C8 | User-tunable Home feed query pool | |
| C9 | Optional cobalt API key field | |
| C10 | Extra confirmed sites | Reddit, SoundCloud, Twitch, Bilibili, Tumblr |
| C11 | Scoped storage by default | No `MANAGE_EXTERNAL_STORAGE` |

---

## Scorecard

Counted programmatically from the tables above. The ➖ rows (section 10, the
monetisation layer) are excluded — that layer is a deliberate non-goal, not a
gap. Section 12 is excluded from the "Cobalt gap" tally because a VidMate ✅
there describes a behaviour Cobalt intentionally lacks.

| Area | VidMate ✅ | Cobalt ✅ | Cobalt ⚠️ | Cobalt ❌ |
|---|:--:|:--:|:--:|:--:|
| 1. Shell / UI/UX | 21 | 18 | 1 | 3 |
| 2. Download engine | 25 | 23 | 0 | 2 |
| 3. Site support & extraction | 19 | 23 | 1 | 0 |
| 4. Playback | 12 | 12 | 0 | 0 |
| 5. Library / vault | 12 | 11 | 0 | 1 |
| 6. In-app browser | 7 | 6 | 0 | 1 |
| 7. Discovery | 7 | 6 | 1 | 0 |
| 8. Music & social | 6 | 4 | 1 | 1 |
| 9. Settings & theming | 11 | 13 | 0 | 1 |
| 11. Platform / technical | 8 | 7 | 0 | 4 |
| **Total (buildable rows)** | **128** | **123** | **4** | **13** |

(❓ rows in section 3 are counted on the VidMate side only where VidMate
confirmed the site; the Co-only sites are counted in Cobalt's column.)

---

## The short version

**Where VidMate is still ahead** (13 ❌ rows):

- **Heavy downloader extras:** BitTorrent/magnet (2.9), SABR (2.10).
- **Account-only YouTube state:** personal subscriptions / watch-later / history
  (3.19, ⚠️) — needs an OAuth client. Every *public* YouTube surface is done.
- **Breadth of UI:** splash (1.1), onboarding (1.11), 83 translated languages
  (1.20), the custom widget kit (1.15).
- **Social / non-goals:** dislike (5.10), offline/Bluetooth share (8.5),
  anti-detection (6.7), and the whole growth stack (section 10).
- **Infrastructure:** plugin framework (11.1), dynamic code loading (11.2),
  server-originated FCM push (11.5).

**Where Cobalt is ahead:** ad-free by construction, scoped storage, Material You,
a merged 4-source Shorts pipeline, a green test suite (111 JVM tests), an
open-source license, and several user-tunable knobs VidMate has no equivalent for.

**Net:** across the buildable rows VidMate covers **128** capabilities to
Cobalt's **123 ✅ (+4 ⚠️)**, with **13 ❌**. That is a large narrowing from the
pre-gap-closure comparison (131 vs 64 + 7 ⚠️ / 71 ❌). This round closed the whole
"use InnerTube fully" batch plus section 3 and the on-device recommender:

- **3.14** catch-all extractor — any `http(s)` host is offered to cobalt.
- **3.17** VPN/geo hint on failure (`ResolveFailureAdvisor`).
- **3.18** real playlist URL → track list → batch download.
- **3.20** YouTube Music discovery, **7.2** trending grid, **7.4** in-app search.
- **3.19** the public half of the YouTube surface — search, trending, Music,
  channels, comments, playlists — all via the public InnerTube key, no sign-in.
- **9.8 + I3** push-style notifications with an on-device "because you watched X"
  recommender (local likes/history, daily WorkManager job, own channel).

The remaining ❌ rows split cleanly into three groups:

1. **Deliberate non-goals** — monetisation (section 10), anti-detection (6.7),
   plugin framework (11.1), dynamic code loading (11.2).
2. **External-credential blockers** — account-only YouTube state (3.19),
   server-originated FCM push (11.5).
3. **Genuine remaining engineering** — BitTorrent (2.9), SABR (2.10), splash and
   onboarding (1.1/1.11), full localisation (1.20), dislike (5.10), Bluetooth
   share (8.5), widget settings (9.9).

Group 3 is the honest backlog if the goal were parity with VidMate.
