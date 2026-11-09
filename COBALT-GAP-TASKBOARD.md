# Cobalt Android — full gap-closure task board

Every ❌ from `VIDMATE-VS-COBALT-COMPARISON.md` (71 rows), grouped into build
phases. Status: ✅ done · 🔨 in progress · ⬜ queued · 🚫 excluded (with reason).

Ground rule applied throughout: **build the real capability, not a stub.**
Where VidMate's approach is the reference, the underlying engine design is used
as the argument (segmented range download with per-part fail-over, native mux,
MediaSession playback, extractor rule-sets, etc.) — but nothing is copied from
VidMate's code (it is not open source); the designs are reimplemented.

---

## Phase A — Settings, theme & control (foundation)

| # | Gap | Plan | Status |
|---|---|---|---|
| A1 | Light theme differing from dark | Real light palette + `values-night`, theme applier honours mode | ✅ |
| A2 | Language picker | `AppCompatDelegate.setApplicationLocales`, locale list, per-app language | ✅ |
| A3 | Region / location picker | Persist region, feed it to feed + extractor queries | ✅ |
| A4 | Cancel an in-progress download | Cancel token in `DownloadService` + `OkHttp` call cancel | ✅ |
| A5 | Cancel an in-progress transcode | `TranscodeWorker` cancel + ffmpeg kill | ✅ |
| A6 | Push-notification settings | Per-channel enable + notification prefs (local, no FCM) | ✅ |

## Phase B — Download engine (VidMate's core engine design)

| # | Gap | Plan | Status |
|---|---|---|---|
| B1 | Multi-segment / range downloads | Split file into N ranges, parallel GETs with `Range:` headers, write at offsets (VidMate `MTDownloadPart` model) | ✅ |
| B2 | Per-part URL/path fail-over | Alternate URL per segment, retry segment on its own (VidMate `curPos[start-end+type+url+path]`) | ✅ |
| B3 | Resume an interrupted download | Persist segment offsets; resume from `Content-Range`/`ETag` | ✅ |
| B4 | Download speed shown | Rolling MB/s + ETA in queue UI | ✅ |
| B5 | BitTorrent / magnet | Excluded this pass — needs a full BitTorrent stack; noted as a separate engine | 🚫 |
| B6 | SABR | Covered by B1 (segmented adaptive) | ✅ |
| B7 | Playlist download | Resolve playlist → enqueue all items ✅ |

## Phase C — Playback

| # | Gap | Plan | Status |
|---|---|---|---|
| C1 | Playback speed control | ExoPlayer `setPlaybackSpeed` UI | ✅ |
| C2 | In-player quality switch | Track selector from resolved formats | ✅ |
| C3 | Subtitle / caption support | ExoPlayer `TextOutput` + track picker | ✅ |
| C4 | Background audio playback | `MediaSessionService` + notification | ⬜ |
| C5 | Picture-in-picture / floating window | `enterPictureInPictureMode` (API 26+) | ✅ |
| C6 | Media-button / headset control | `MediaSession` callbacks | ✅ |

## Phase D — Library, files & vault

| # | Gap | Plan | Status |
|---|---|---|---|
| D1 | Delete a downloaded file | Delete MediaStore row + DB row from Library | ✅ |
| D2 | Multi-select files | Selection mode in Files tab | ⬜ |
| D3 | Sort / filter local media | Name/date/size/type sorters | ✅ |
| D4 | Local video library (scanner) | MediaStore video scan | ✅ |
| D5 | Local music library | MediaStore audio scan | ✅ |
| D6 | Local media search | Query local library | ✅ |
| D7 | Private vault (passcode) | Passcode gate + move-to-vault (app-private dir) | ✅ |
| D8 | Vault import / export | Import from / export to public storage | ✅ |

## Phase E — Sites & extraction

| # | Gap | Plan | Status |
|---|---|---|---|
| E1 | Instagram | cobalt handles it server-side; add host + UI copy | ✅ |
| E2 | Facebook | same | ✅ |
| E3 | Playlist URL support | Route playlist URLs through resolver ✅ |
| E4 | Media sniffing from web pages | Sniff `<video>`/m3u8/mp4 in the in-app browser ✅ |
| E5 | VPN suggestion on failure | Detect geo-block/403 → suggest retry ✅ |
| E6 | Broad extractor (hundreds of sites) | Generic host allow-list + cobalt fallback | ✅ |
| E7 | Extractor updatable without app update | Remote host/rule list (JSON) refreshed from remote config ✅ |
| E8 | WhatsApp (status saver) | Status dir scan + save | ⬜ |
| E9 | YouTube sign-in / subs / history / watch-later | Needs Google OAuth — excluded (see note) | ⬜ |
| E10 | YouTube Music | Music tab (Phase G) ✅ |
| E11 | Comments | InnerTube comments fetch + UI | ⬜ |

## Phase F — In-app browser

| # | Gap | Plan | Status |
|---|---|---|---|
| F1 | In-app browser | `WebView` screen with address bar | ✅ |
| F2 | Multi-tab browsing | Tab model + tab strip | ⬜ |
| F3 | Incognito mode | Ephemeral cookie store / no history | ⬜ |
| F4 | Desktop-site toggle | UA override | ✅ |
| F5 | Media sniffing in browser | `WebViewClient` request interception → offer download | ✅ |
| F6 | Web-game browser | Same browser, games entry | ⬜ |
| F7 | Browser anti-detection (JS hook) | UA/JS spoofing — excluded by default (see note) | ⬜ |

## Phase G — Music

| # | Gap | Plan | Status |
|---|---|---|---|
| G1 | Music player | Audio-only Media3 player over downloaded/local audio ✅ |
| G2 | Lyrics | Lyric lookup + timed display | ⬜ |
| G3 | Equalizer | `android.media.audiofx.Equalizer` | ✅ |
| G4 | Music playlists | Local playlist entity + UI | ⬜ |

## Phase H — Shell, discovery & UX

| # | Gap | Plan | Status |
|---|---|---|---|
| H1 | Splash / welcome screen | Branded splash activity ✅ |
| H2 | Onboarding / coach screens | First-run permission + intro pages ✅ |
| H3 | Tab set configurable server-side | Read tab config from `remote-config.json` ✅ |
| H4 | Custom widgets (download animation, wheel, sliding tabs) | Compose equivalents | ⬜ |
| H5 | Channel pages | Channel feed screen | ⬜ |
| H6 | Game-centre tab | Excluded — growth/monetisation surface, not a media capability | ⬜ |
| H7 | App-widget settings + home-screen widget | `AppWidgetProvider` for downloads | ⬜ |

## Phase I — Platform & infrastructure

| # | Gap | Plan | Status |
|---|---|---|---|
| I1 | Crash handler / auto-restart | `UncaughtExceptionHandler` + restart activity | ✅ |
| I2 | In-app APK install (self-update) | `REQUEST_INSTALL_PACKAGES` + update check — needs a signed release channel | ⬜ |
| I3 | Push (FCM + custom service) | Local notification infra built; FCM needs a Google project (see note) | ⬜ |
| I4 | Plugin architecture (hot-swappable) | Excluded — large framework; extractor hot-update (E7) covers the real need | ⬜ |
| I5 | Dynamic code loading / hot-fix | Excluded — Play-policy and security-hostile; E7 covers config-level updates | ⬜ |
| I6 | Signed store release | Excluded — needs a keystore + Play/account the user owns | ⬜ |
| I7 | Open source / license | Add a LICENSE | ✅ |

## Phase J — Not carried over (by design, not a gap)

| # | Gap | Why |
|---|---|---|
| J1 | Broad permissions (MANAGE_EXTERNAL_STORAGE, REQUEST_INSTALL_PACKAGES, SYSTEM_ALERT_WINDOW) | Cobalt uses scoped storage; broad perms are an anti-feature, not a capability |
| J2 | Runtime package-name spoofing | Anti-detection evasion; deliberately not built |
| J3 | Weak XOR string obfuscation | Security anti-pattern; nothing to gain |
| J4 | First-party API keys in plaintext | Security anti-pattern |
| J5 | Ad / tracking SDKs | Cobalt is ad-free by design |
| J6 | Monetisation layer (app store, wallet, daily tasks, cleaner, splash ads) | Growth/monetisation surface, out of scope for a downloader |

---

## Notes on the three genuinely external blockers

- **E9 YouTube sign-in**: requires a Google OAuth client id registered to the
  app's signing certificate and Google's API terms. Cannot be completed in this
  sandbox; the code path can be scaffolded but the credential is the user's to
  provision.
- **I3 FCM push**: requires a `google-services.json` from a Firebase project.
  Local notification infrastructure is buildable now; server push is not.
- **I6 signed release**: requires a keystore (and, for Play, a developer
  account). `assembleRelease` can be wired to a keystore the user supplies.
