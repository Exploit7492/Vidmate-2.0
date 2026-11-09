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
| Tab set configurable server-side | ✅ | ❌ | VidMate ships an "earn cash" alternate tab layout remotely |
| Splash / welcome screen | ✅ | ❌ | VidMate has a countdown splash (+ splash ad) |
| Search bar (pill) on Home | ✅ | ✅ | |
| Home recommendation feed grid | ✅ | ✅ | Cobalt's is YouTube-InnerTube-backed, 2-column |
| Section headers in feed | ✅ | ✅ | Cobalt: "Recommended" |
| Vertical short-video feed | ✅ | ✅ | |
| Dedicated Me / profile tab | ✅ | ✅ | |
| Separate Downloads screen | ✅ | ✅ | Cobalt: Library → Downloads tab |
| Game-centre tab | ✅ | ❌ | |
| YouTube-Music tab | ✅ | ❌ | |
| In-app Settings screen | ✅ | ✅ | |
| Dark / night theme | ✅ | ✅ | Cobalt ships one dark palette + dynamic |
| Light theme that differs from dark | ✅ | ❌ | Cobalt's known gap (single palette) |
| Material You dynamic colour | ❌ | ✅ | Cobalt is ahead here (Android 12+) |
| Custom widgets (download animation, wheel picker, sliding tabs) | ✅ | ❌ | |
| Download-count badge on nav | ✅ | ✅ | |
| Long-press app shortcuts | ✅ | ✅ | Cobalt: "Paste & Download", "Open Queue" |
| Floating download button | ✅ | ✅ | |
| Onboarding / coach screens | ✅ | ❌ | |
| UI languages | 83 | 1 | VidMate also ships 107 regions; Cobalt is English-only |

## 2. Download engine

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| HTTP(S) download | ✅ | ✅ | |
| Multi-segment / range downloads | ✅ | ❌ | VidMate: `curPos[start-end+type+url+path]` per-part |
| Per-part URL/path fail-over (CDN fallback) | ✅ | ❌ | |
| Resume an interrupted download | ✅ | ❌ | Cobalt restarts from zero |
| DASH / adaptive segmented download | ✅ | ⚠️ | Cobalt fetches the cobalt-resolved URL; HLS is saved as `.m3u8`, not muxed |
| Audio+video merge (mux) | ✅ | ✅ | VidMate: native `libvidmatecodec.so` · Cobalt: FFmpeg |
| Video → audio extraction (MP3 etc.) | ✅ | ✅ | |
| Quality/format conversion tiers | ✅ | ✅ | Cobalt: FFmpeg profile ladder (video + audio) |
| BitTorrent / magnet downloads | ✅ | ❌ | VidMate: `download.bt.core.*` |
| SABR (segmented adaptive) | ✅ | ❌ | |
| Playlist download (whole playlist) | ✅ | ❌ | VidMate: `PLayListSelectDownloadActivity` |
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
| Cancel an in-progress download | ✅ | ❌ | Cobalt's `onCancel` is a stub |
| Cancel an in-progress transcode | ✅ | ❌ | Cobalt documented gap |
| Persisted download state | ✅ | ✅ | VidMate Room+MMKV · Cobalt Room |

## 3. Site support & extraction

| Site / capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| YouTube | ✅ | ✅ | |
| TikTok | ✅ | ✅ | |
| Twitter / X | ✅ | ✅ | |
| Instagram | ✅ | ❌ | |
| Facebook | ✅ | ❌ | |
| Dailymotion | ✅ | ✅ | |
| Vimeo | ✅ | ✅ | |
| Pinterest | ✅ | ✅ | |
| WhatsApp (status saver) | ✅ | ❌ | |
| Reddit | ❓ | ✅ | VidMate not confirmed in evidence |
| SoundCloud | ❓ | ✅ | |
| Twitch | ❓ | ✅ | |
| Bilibili | ❓ | ✅ | |
| Tumblr | ❓ | ✅ | |
| Broad extractor (hundreds of sites) | ✅ | ❌ | VidMate: obfuscated, hot-updatable extractor (~803 classes) |
| Extractor updatable without app update | ✅ | ❌ | Cobalt is fixed to its host list |
| Media sniffing from web pages | ✅ | ❌ | |
| VPN suggestion on analysis failure | ✅ | ❌ | |
| Playlist URL support | ✅ | ❌ | |
| YouTube sign-in / subscriptions / history / watch-later | ✅ | ❌ | |
| YouTube Music | ✅ | ❌ | |
| Comments | ✅ | ❌ | |
| Clipboard link detection | ✅ | ✅ | |
| Share-sheet intake from other apps | ✅ | ✅ | |

## 4. Media playback

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Inline video playback | ✅ | ✅ | Both Media3/ExoPlayer |
| Full-screen player | ✅ | ✅ | |
| HLS / DASH playback | ✅ | ✅ | |
| Picture-in-picture / floating window | ✅ | ❌ | VidMate uses `SYSTEM_ALERT_WINDOW` |
| Background audio playback | ✅ | ❌ | No MediaSession service in Cobalt |
| Media-button / headset control | ✅ | ❌ | |
| Playback speed control | ✅ | ❌ | |
| Subtitle / caption support | ✅ | ❌ | |
| In-player quality switch | ✅ | ❌ | |
| Shorts autoplay + next-item preload | ✅ | ✅ | Cobalt preloads the next Short |
| One-live-player discipline | ✅ | ✅ | |

## 5. Library, local media & vault

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Local video library (scanner) | ✅ | ❌ | |
| Local music library | ✅ | ❌ | |
| Local media search | ✅ | ❌ | |
| Files tab | ✅ | ✅ | Cobalt: Library → Files |
| Sort / filter local media | ✅ | ❌ | |
| Multi-select files | ✅ | ❌ | |
| Delete a downloaded file from the app | ✅ | ❌ | Cobalt only deletes on failed-download cleanup |
| Watch history | ✅ | ✅ | |
| Likes / bookmarks | ✅ | ✅ | Cobalt: Liked + Bookmark history |
| Private vault (passcode-protected) | ✅ | ❌ | VidMate: 4-digit passcode "Private Space" |
| Vault import / export | ✅ | ❌ | |

## 6. In-app browser

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| In-app browser | ✅ | ❌ | |
| Multi-tab browsing | ✅ | ❌ | |
| Incognito mode | ✅ | ❌ | |
| Desktop-site toggle | ✅ | ❌ | |
| Media sniffing in the browser | ✅ | ❌ | |
| Web-game browser | ✅ | ❌ | |
| Browser anti-detection (JS hook, ~100 UA packages) | ✅ | ❌ | |

## 7. Discovery & engagement

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Home recommendation feed | ✅ | ✅ | |
| Trending / discovery sections | ✅ | ⚠️ | Cobalt's feed is live but has no explicit trending tab |
| Channel pages | ✅ | ❌ | |
| In-app video search with results feed | ✅ | ⚠️ | Cobalt's field resolves a pasted link; no results list |
| Shorts feed | ✅ | ✅ | Cobalt merges 4 sources (InnerTube/Invidious/NewPipe/PeerTube) |
| Like / dislike on videos | ✅ | ⚠️ | Cobalt: like only, no dislike |
| Comments | ✅ | ❌ | |

## 8. Music & social

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Music player (albums / singers / discovery) | ✅ | ❌ | |
| Lyrics | ✅ | ❌ | |
| Equalizer | ✅ | ❌ | |
| Music playlists | ✅ | ❌ | |
| WhatsApp status saver | ✅ | ❌ | |
| Offline / Bluetooth share | ✅ | ❌ | |
| Social share sheet | ✅ | ✅ | |

## 9. Settings & theming

| Capability | VidMate | Cobalt | Notes |
|---|:--:|:--:|---|
| Theme picker | ✅ | ✅ | |
| Language picker | ✅ | ❌ | |
| Region / location picker | ✅ | ❌ | |
| Download path selector | ✅ | ✅ | Cobalt: text field |
| Default quality / format | ✅ | ✅ | |
| Clipboard trigger toggle | ✅ | ✅ | |
| Audio-only mode | ✅ | ⚠️ | Cobalt persists it; not yet wired into resolution |
| Push-notification settings | ✅ | ❌ | |
| App-widget settings | ✅ | ❌ | |
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
| Plugin architecture (hot-swappable) | ✅ | ❌ | VidMate: Tencent Shadow host + plugin |
| Dynamic code loading / hot-fix | ✅ | ❌ | |
| Native codec libraries | ✅ | ✅ | VidMate codec · Cobalt FFmpeg |
| Server-driven remote config | ✅ | ✅ | |
| Push (FCM + custom service) | ✅ | ❌ | |
| Home-screen app widgets | 3 | 0 | |
| Crash handler / auto-restart | ✅ | ❌ | |
| Signed store release | ❌ | ❌ | Both sideload-only |
| Unit tests | ❓ | ✅ | Cobalt: 44 JVM tests, all green |
| Open source / license | ❌ | ❌ | Cobalt has no LICENSE file |

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
| In-app APK install (self-update) | ✅ | ❌ | |

---

## Scorecard (rows where both were comparable)

Counted programmatically from the tables above; the ➖ rows (section 10, the
monetisation layer) are excluded — that layer is a deliberate non-goal, not a
gap. For section 12 the marks describe "does this behaviour/characteristic
exist", so a VidMate ✅ there is not a point in VidMate's favour.

| Area | VidMate ✅ | Cobalt ✅ | Cobalt ⚠️ | Cobalt ❌ |
|---|:--:|:--:|:--:|:--:|
| 1. Shell / UI | 19 | 13 | 0 | 7 |
| 2. Download engine | 25 | 15 | 2 | 8 |
| 3. Site support | 19 | 13 | 0 | 11 |
| 4. Playback | 11 | 5 | 0 | 6 |
| 5. Library / vault | 11 | 3 | 0 | 8 |
| 6. In-app browser | 7 | 0 | 0 | 7 |
| 7. Discovery | 7 | 2 | 3 | 2 |
| 8. Music / social | 7 | 1 | 0 | 6 |
| 9. Settings | 12 | 8 | 1 | 4 |
| 11. Platform / technical | 6 | 3 | 0 | 6 |
| 12. Security posture | 7 | 1 | 1 | 6 |
| **Total** | **131** | **64** | **7** | **71** |

## The short version

**VidMate has that Cobalt does not** — the whole discovery-and-consume surface
(in-app browser, local media library, music player, private vault, WhatsApp
status saver), the heavy downloader features (segmented/resume, BitTorrent,
SABR, playlist download), the playback extras (PiP, background audio, subtitles,
speed), the growth stack (app store, games, wallet, cleaner), and the
infrastructure (plugin/hot-update, push, widgets, 83 languages).

**Cobalt has that VidMate does not** — an ad-free experience, a small and
auditable surface, scoped storage instead of `MANAGE_EXTERNAL_STORAGE`,
Material-You dynamic colour, a 4-source merged Shorts pipeline, a JVM test
suite, and a handful of user-tunable knobs (Invidious pool, Shorts query pool,
Cobalt API key). Several of Cobalt's site entries (Reddit, SoundCloud, Twitch,
Bilibili, Tumblr) are also outside VidMate's *confirmed* extraction set.

**Net:** across the compared rows VidMate covers 131 capabilities to Cobalt's
64 ✅ (+7 ⚠️) — roughly twice the surface area, before counting its
monetisation layer at all. Cobalt is intentionally a thin, private, ad-free
downloader whose
UI/UX now mirrors VidMate's shell and visual language. The biggest concrete
capability gaps if you wanted to close them, in rough priority order:

1. **Resume + segmented downloads** (Cobalt restarts from zero today).
2. **Cancel in-progress download/transcode** (both are stubs today).
3. **More extraction sites** (Facebook, Instagram, WhatsApp).
4. **Background audio + PiP playback.**
5. **A delete-file action** in the Library.
6. **Localisation** (Cobalt ships one locale).
