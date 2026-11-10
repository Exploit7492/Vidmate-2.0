# VidMate vs Cobalt — every feature in one table

Every row from `VIDMATE-VS-COBALT-FULL.md`, flattened into a single list. Legend: ✅ present · ⚠️ partial · ❌ absent · ➖ N/A by design · ❓ unconfirmed.

| # | Category | Feature / aspect | VidMate | Cobalt | Notes |
|--:|---|:--:|:--:|:--:|---|
| 1 | Entry points, launch & first-run | Launcher activity | ✅ | ✅ | VM two launchers (host + plugin) · Co `SplashActivity` |
| 2 | Entry points, launch & first-run | Branded splash screen | ✅ | ✅ | Co: 600 ms branded splash (`SplashActivity`) |
| 3 | Entry points, launch & first-run | Splash advertisement | ✅ | ➖ | VM `SplashView` / `SplashAdConfig` |
| 4 | Entry points, launch & first-run | Splash progress/countdown | ✅ | ⚠️ | VM `CountDownProgressBar` · Co fixed short delay |
| 5 | Entry points, launch & first-run | First-run onboarding / coach screens | ✅ | ✅ | Co: multi-page `OnboardingActivity` |
| 6 | Entry points, launch & first-run | Onboarding gated to first launch only | ✅ | ✅ | Co: `onboardingDone` pref |
| 7 | Entry points, launch & first-run | Share-sheet entry (`ACTION_SEND`) | ✅ | ✅ | Co: `text/plain` intent filter |
| 8 | Entry points, launch & first-run | Open-with entry (`ACTION_VIEW`) | ✅ | ❌ | VM aliases `$NewPlayerActivity`/`$HTML` |
| 9 | Entry points, launch & first-run | Music-player launch alias | ✅ | ❌ | VM `$MusicPlayerActivity` |
| 10 | Entry points, launch & first-run | Long-press app shortcuts | ✅ | ✅ | Co: "Paste & Download", "Open Queue" |
| 11 | Entry points, launch & first-run | Deep-link / URL-scheme entry | ✅ | ❌ | VM `$JUMP` / `$INVOKE` |
| 12 | Entry points, launch & first-run | Clipboard link auto-detect | ✅ | ✅ | Co: `ClipboardHelper` + trigger toggle |
| 13 | Entry points, launch & first-run | Network-offline screen | ✅ | ⚠️ | VM `NetworkDisconnectView` · Co inline status text |
| 14 | Navigation shell | Bottom navigation bar | ✅ | ✅ | VM 4–5 tabs · Co 5 tabs |
| 15 | Navigation shell | Tab set server-configurable | ✅ | ✅ | Co: `remote-config.json` `tabs` |
| 16 | Navigation shell | Default tab layout | ✅ | ✅ | VM Home·Downloader·[GC]·[YTM]·Me · Co Home·Explore·Library·Shorts·Me |
| 17 | Navigation shell | "Earn cash" alternate layout | ✅ | ➖ | VM `main_earn_cash` variant |
| 18 | Navigation shell | Per-tab back-stack (nested nav) | ✅ | ✅ | Co: Navigation component |
| 19 | Navigation shell | Floating action / download button | ✅ | ✅ | Co: queue FAB |
| 20 | Navigation shell | Download-count badge | ✅ | ✅ | Co: badge on queue button |
| 21 | Navigation shell | Pull-to-refresh on feeds | ✅ | ⚠️ | Co: reload action, not a swipe gesture |
| 22 | Navigation shell | Settings reachable from shell | ✅ | ✅ | Co: Me → Settings |
| 23 | Navigation shell | Persistent mini-player bar | ✅ | ✅ | Co: music mini-player |
| 24 | Navigation shell | Bottom-bar visibility driven by config | ✅ | ✅ | Co: `applyRemoteTabs()` |
| 25 | Screen inventory (count of distinct user-facing screens) | Home / discovery | ✅ | ✅ |  |
| 26 | Screen inventory (count of distinct user-facing screens) | Search screen | ✅ | ✅ | Co: Explore → Search |
| 27 | Screen inventory (count of distinct user-facing screens) | Trending / discovery | ✅ | ✅ | Co: Explore → Trending |
| 28 | Screen inventory (count of distinct user-facing screens) | Video detail screen | ✅ | ✅ | Co: resolve/picker flow |
| 29 | Screen inventory (count of distinct user-facing screens) | Full-screen player | ✅ | ✅ | Co: `VideoPlayerDialogFragment` |
| 30 | Screen inventory (count of distinct user-facing screens) | Downloads / files screen | ✅ | ✅ | Co: Library → Downloads |
| 31 | Screen inventory (count of distinct user-facing screens) | Download queue sheet | ✅ | ✅ | Co: `DownloadQueueSheet` |
| 32 | Screen inventory (count of distinct user-facing screens) | In-app browser | ✅ | ✅ | Co: `BrowserFragment` |
| 33 | Screen inventory (count of distinct user-facing screens) | Local media library | ✅ | ✅ | Co: `MediaLibraryFragment` |
| 34 | Screen inventory (count of distinct user-facing screens) | Private vault | ✅ | ✅ | Co: `VaultFragment` |
| 35 | Screen inventory (count of distinct user-facing screens) | WhatsApp status saver | ✅ | ✅ | Co: `StatusFragment` |
| 36 | Screen inventory (count of distinct user-facing screens) | Music screen | ✅ | ✅ | Co: `MusicFragment` |
| 37 | Screen inventory (count of distinct user-facing screens) | YouTube Music | ✅ | ✅ | Co: Explore → Music |
| 38 | Screen inventory (count of distinct user-facing screens) | Channel pages | ✅ | ✅ | Co: `ChannelFragment` |
| 39 | Screen inventory (count of distinct user-facing screens) | Comments | ✅ | ✅ | Co: comments on Home items |
| 40 | Screen inventory (count of distinct user-facing screens) | Games / web-games hub | ✅ | ✅ | Co: `GamesFragment` |
| 41 | Screen inventory (count of distinct user-facing screens) | App store (9Apps) | ✅ | ➖ |  |
| 42 | Screen inventory (count of distinct user-facing screens) | Wallet / earn-cash | ✅ | ➖ |  |
| 43 | Screen inventory (count of distinct user-facing screens) | Cleaner / speed-booster | ✅ | ➖ |  |
| 44 | Screen inventory (count of distinct user-facing screens) | Daily-tasks / rewards | ✅ | ➖ |  |
| 45 | Screen inventory (count of distinct user-facing screens) | Settings (multiple sub-screens) | ✅ | ✅ | Co: one Compose settings screen |
| 46 | Screen inventory (count of distinct user-facing screens) | History / bookmarks | ✅ | ✅ | Co: Library → History |
| 47 | Screen inventory (count of distinct user-facing screens) | Language picker screen | ✅ | ✅ | Co: in Settings |
| 48 | Screen inventory (count of distinct user-facing screens) | Region/location picker screen | ✅ | ✅ | Co: in Settings |
| 49 | Screen inventory (count of distinct user-facing screens) | Download-path picker screen | ✅ | ✅ | Co: text field in Settings |
| 50 | Screen inventory (count of distinct user-facing screens) | Push-settings screen | ✅ | ✅ | Co: notification toggles |
| 51 | Screen inventory (count of distinct user-facing screens) | Widget-settings screen | ✅ | ❌ |  |
| 52 | Screen inventory (count of distinct user-facing screens) | Safe-mode screen | ✅ | ➖ |  |
| 53 | Screen inventory (count of distinct user-facing screens) | Login / account screen | ✅ | ➖ | Co has no account by design |
| 54 | Screen inventory (count of distinct user-facing screens) | Update / "safe update" screen | ✅ | ⚠️ | Co: in-app update check, no dedicated screen |
| 55 | Home & discovery UX | Recommendation feed | ✅ | ✅ | Co: InnerTube-backed |
| 56 | Home & discovery UX | Feed grid layout | ✅ | ✅ | Co: 2-column grid |
| 57 | Home & discovery UX | Section headers | ✅ | ✅ | Co: "Recommended" |
| 58 | Home & discovery UX | Home pill search bar | ✅ | ✅ |  |
| 59 | Home & discovery UX | Feed ad slots | ✅ | ➖ | VM `home_feed_page_ad` |
| 60 | Home & discovery UX | Adult-content hiding in feed | ✅ | ➖ | No such content in Cobalt |
| 61 | Home & discovery UX | Feed refresh / rotate | ✅ | ✅ | Co: query-pool rotation |
| 62 | Home & discovery UX | User-tunable feed topics | ❌ | ✅ | Co: feed query pool (Cobalt-only) |
| 63 | Download UX | Paste-a-link box | ✅ | ✅ |  |
| 64 | Download UX | Link validity feedback | ✅ | ✅ | Co: "doesn't look like a valid link" |
| 65 | Download UX | Format/quality picker sheet | ✅ | ✅ |  |
| 66 | Download UX | Resolution labels ("1080p" etc.) | ✅ | ✅ | Co: FFmpeg profile labels |
| 67 | Download UX | Audio-only option | ✅ | ✅ | Co: audio format + audio-only mode |
| 68 | Download UX | Default quality preference | ✅ | ✅ |  |
| 69 | Download UX | Download progress row (bytes/%) | ✅ | ✅ |  |
| 70 | Download UX | Download speed display (MB/s) | ✅ | ✅ | Co: `speedBps` → "x.x MB/s" |
| 71 | Download UX | Multi-select downloads | ✅ | ✅ |  |
| 72 | Download UX | Playlist / batch download dialog | ✅ | ✅ | Co: real playlist expansion + line batch |
| 73 | Download UX | Cancel in-progress download | ✅ | ✅ | Co: `DownloadCanceller` |
| 74 | Download UX | Retry a failed download | ✅ | ✅ | Co: retry + auto-retry |
| 75 | Download UX | Open a finished download | ✅ | ✅ |  |
| 76 | Download UX | Delete a finished download | ✅ | ✅ |  |
| 77 | Download UX | Clear download history | ✅ | ✅ |  |
| 78 | Download UX | Download-in-progress notification | ✅ | ✅ |  |
| 79 | Download UX | Download-complete notification | ✅ | ✅ |  |
| 80 | Download UX | Pre-download interstitial ad | ✅ | ➖ | VM `task_add_inters_ad` |
| 81 | Player UX | Inline player controls | ✅ | ✅ | Both Media3/ExoPlayer |
| 82 | Player UX | Full-screen / landscape | ✅ | ✅ |  |
| 83 | Player UX | Picture-in-picture | ✅ | ✅ | Co: `enterPictureInPictureMode` |
| 84 | Player UX | Floating pop-up window | ✅ | ❌ | VM `SYSTEM_ALERT_WINDOW` layer |
| 85 | Player UX | Playback-speed menu | ✅ | ✅ |  |
| 86 | Player UX | Quality switch in player | ✅ | ✅ |  |
| 87 | Player UX | Subtitle toggle | ✅ | ✅ |  |
| 88 | Player UX | Equalizer panel | ✅ | ✅ | Co: `EqualizerController` |
| 89 | Player UX | Pre-roll ad | ✅ | ➖ | VM `pre_rolls_ad` |
| 90 | Player UX | Gesture controls (seek/brightness) | ✅ | ⚠️ | Co: basic seek bar |
| 91 | Player UX | Next/prev in a Shorts feed | ✅ | ✅ | Co: preloads next Short |
| 92 | Player UX | Resume playback position | ✅ | ⚠️ | Co: per-session, not persisted |
| 93 | Library & local-media UX | Downloads list with thumbnails | ✅ | ✅ |  |
| 94 | Library & local-media UX | Local video library grid | ✅ | ✅ | Co: MediaStore scan |
| 95 | Library & local-media UX | Local music library | ✅ | ✅ |  |
| 96 | Library & local-media UX | Search within local media | ✅ | ✅ | Co: `LocalMediaQuery` |
| 97 | Library & local-media UX | Sort / filter chips | ✅ | ✅ | Co: sort + kind filter |
| 98 | Library & local-media UX | Multi-select + batch delete | ✅ | ✅ |  |
| 99 | Library & local-media UX | File detail / info | ✅ | ⚠️ | Co: row metadata only |
| 100 | Library & local-media UX | Watch history timeline | ✅ | ✅ |  |
| 101 | Library & local-media UX | Liked / bookmarks list | ✅ | ✅ |  |
| 102 | Library & local-media UX | Storage-usage display | ✅ | ❌ |  |
| 103 | In-app browser UX | Browser with URL bar | ✅ | ✅ |  |
| 104 | In-app browser UX | Multi-tab UI | ✅ | ✅ |  |
| 105 | In-app browser UX | Incognito toggle | ✅ | ✅ |  |
| 106 | In-app browser UX | Desktop-site toggle | ✅ | ✅ |  |
| 107 | In-app browser UX | Media-sniff download prompt | ✅ | ✅ | Co: `MediaSniffer` |
| 108 | In-app browser UX | "Download this page's video" CTA | ✅ | ✅ |  |
| 109 | In-app browser UX | Web-game browser mode | ✅ | ✅ | Co: Games hub → browser |
| 110 | In-app browser UX | Browser history / bookmarks | ✅ | ❌ |  |
| 111 | In-app browser UX | Browser anti-detection | ✅ | ❌ | Excluded by design |
| 112 | Vault UX | Passcode gate | ✅ | ✅ | Co: vault passcode screen |
| 113 | Vault UX | Create-passcode first-run flow | ✅ | ✅ |  |
| 114 | Vault UX | Import into vault | ✅ | ✅ | Co: SAF picker |
| 115 | Vault UX | Export from vault | ✅ | ✅ |  |
| 116 | Vault UX | Hidden from system gallery | ✅ | ✅ | Co: app-private storage |
| 117 | Vault UX | "Hide/unhide" confirmation UX | ✅ | ⚠️ | Co: simpler confirm |
| 118 | Music UX | Music player screen | ✅ | ✅ |  |
| 119 | Music UX | Now-playing with artwork | ✅ | ✅ |  |
| 120 | Music UX | Lyrics view | ✅ | ✅ | Co: `LrcParser` |
| 121 | Music UX | Lyrics search (online) | ✅ | ❌ | VM `LrcSearchActivity` |
| 122 | Music UX | Playlists | ✅ | ✅ | Co: Room playlists |
| 123 | Music UX | Albums / singers browse | ✅ | ❌ |  |
| 124 | Music UX | Music discovery feed | ✅ | ✅ | Co: Explore → Music |
| 125 | Music UX | Equalizer | ✅ | ✅ |  |
| 126 | Music UX | Sleep timer | ✅ | ❌ |  |
| 127 | Music UX | Offline / Bluetooth share | ✅ | ❌ |  |
| 128 | Settings UX | Theme picker | ✅ | ✅ |  |
| 129 | Settings UX | Language picker | ✅ | ✅ |  |
| 130 | Settings UX | Region / content-locale picker | ✅ | ✅ |  |
| 131 | Settings UX | Download-path selector | ✅ | ✅ | Co: text field |
| 132 | Settings UX | Default quality/format | ✅ | ✅ |  |
| 133 | Settings UX | Clipboard trigger toggle | ✅ | ✅ |  |
| 134 | Settings UX | Audio-only mode | ✅ | ✅ |  |
| 135 | Settings UX | Notification toggles | ✅ | ✅ |  |
| 136 | Settings UX | Recommendation opt-in toggle | ❌ | ✅ | Co-only ("recommend videos for me") |
| 137 | Settings UX | Widget settings | ✅ | ❌ |  |
| 138 | Settings UX | Adult-content hide | ✅ | ➖ |  |
| 139 | Settings UX | Safe mode | ✅ | ➖ |  |
| 140 | Settings UX | Invidious instance pool | ❌ | ✅ | Cobalt-only |
| 141 | Settings UX | Shorts query pool | ❌ | ✅ | Cobalt-only |
| 142 | Settings UX | Home-feed query pool | ❌ | ✅ | Cobalt-only |
| 143 | Settings UX | Cobalt API-key field | ❌ | ✅ | Cobalt-only |
| 144 | Settings UX | Clear history action | ✅ | ✅ |  |
| 145 | Settings UX | Battery-optimisation shortcut | ✅ | ✅ |  |
| 146 | Settings UX | Check-for-update action | ✅ | ✅ | Co: `UpdateChecker` |
| 147 | Theming & visual design | Light theme | ✅ | ✅ |  |
| 148 | Theming & visual design | Dark / night theme | ✅ | ✅ |  |
| 149 | Theming & visual design | Follow-system theme | ✅ | ✅ | Co: `DYNAMIC` mode |
| 150 | Theming & visual design | Material-You dynamic colour | ❌ | ✅ | Cobalt-only (Android 12+) |
| 151 | Theming & visual design | Brand-blue accent palette | ✅ | ✅ | Co: VidMate-derived palette |
| 152 | Theming & visual design | Accent/gold highlights | ✅ | ✅ |  |
| 153 | Theming & visual design | Custom animated widgets | ✅ | ⚠️ | Co: progress ring only |
| 154 | Theming & visual design | Rounded feed tiles | ✅ | ✅ |  |
| 155 | Theming & visual design | Server-managed skins | ✅ | ❌ | VM `SkinManager` |
| 156 | Localisation | In-app language picker | ✅ | ✅ | Co: 15 options |
| 157 | Localisation | In-app region picker | ✅ | ✅ | Co: `RegionHelper` |
| 158 | Localisation | Translated string resources | ✅ (33) | ❌ (1) | Co ships one `values/strings.xml` |
| 159 | Localisation | Selectable UI languages | ✅ (83) | ⚠️ (15) | Co: picker switches locale, strings mostly English |
| 160 | Localisation | Selectable regions | ✅ (107) | ⚠️ | Co: content-region bias |
| 161 | Localisation | RTL layout support | ✅ | ✅ | Co: `supportsRtl` |
| 162 | Localisation | Language-of-content rule (max 2) | ✅ | ❌ |  |
| 163 | Onboarding, permissions & guidance | First-run walkthrough | ✅ | ✅ |  |
| 164 | Onboarding, permissions & guidance | Notification-permission explainer | ✅ | ✅ | Co: request + rationale |
| 165 | Onboarding, permissions & guidance | Overlay-permission guide | ✅ | ➖ | VM floating window |
| 166 | Onboarding, permissions & guidance | Storage-permission guide | ✅ | ➖ | Co uses scoped storage |
| 167 | Onboarding, permissions & guidance | Battery-optimisation prompt | ✅ | ✅ | Co: exempt-battery flow |
| 168 | Onboarding, permissions & guidance | Install-packages prompt (self-update) | ✅ | ✅ | Co: `REQUEST_INSTALL_PACKAGES` |
| 169 | Onboarding, permissions & guidance | VPN/geo-failure guidance | ✅ | ✅ | Co: `ResolveFailureAdvisor` |
| 170 | Onboarding, permissions & guidance | Empty-state guidance | ✅ | ✅ |  |
| 171 | Notifications & widgets UX | Download-progress notification | ✅ | ✅ |  |
| 172 | Notifications & widgets UX | Download-complete notification | ✅ | ✅ |  |
| 173 | Notifications & widgets UX | Music-player notification | ✅ | ✅ | Co: MediaSession notification |
| 174 | Notifications & widgets UX | Recommendation notification | ✅ | ✅ | Co: "because you watched X" |
| 175 | Notifications & widgets UX | Rich notification layouts | ✅ (22) | ⚠️ | Co: standard layouts |
| 176 | Notifications & widgets UX | Notification action buttons | ✅ | ✅ | Co: Open / retry actions |
| 177 | Notifications & widgets UX | Separate notification channels | ✅ | ✅ | Co: downloads + recommendations |
| 178 | Notifications & widgets UX | Home-screen app widgets | ✅ (3) | ✅ (1) | Co: downloads widget |
| 179 | Notifications & widgets UX | Widget deep-link into app | ✅ | ✅ |  |
| 180 | Sharing UX | Share sheet out | ✅ | ✅ |  |
| 181 | Sharing UX | Receive shares from other apps | ✅ | ✅ |  |
| 182 | Sharing UX | Share to specific social apps | ✅ | ✅ | Co: system chooser |
| 183 | Sharing UX | Offline / Bluetooth share | ✅ | ❌ |  |
| 184 | Sharing UX | QR / proximity share | ✅ | ❌ |  |
| 185 | Growth & monetisation UX | In-app app store (9Apps) | ✅ | ➖ |  |
| 186 | Growth & monetisation UX | Game centre / web games | ✅ | ⚠️ | Co: games portal hub only |
| 187 | Growth & monetisation UX | Earn-cash / wallet UX | ✅ | ➖ |  |
| 188 | Growth & monetisation UX | Daily tasks / coins | ✅ | ➖ |  |
| 189 | Growth & monetisation UX | Cleaner / speed-booster | ✅ | ➖ |  |
| 190 | Growth & monetisation UX | Interstitial / banner / native ads | ✅ | ➖ |  |
| 191 | Growth & monetisation UX | Exit-popup ad | ✅ | ➖ | VM `exit_popup_ad` |
| 192 | Growth & monetisation UX | Ad-free experience | ❌ | ✅ | Cobalt-only |
| 193 | Download engine | HTTP(S) download | ✅ | ✅ |  |
| 194 | Download engine | Multi-segment / range downloads | ✅ | ✅ | Co: `SegmentedDownloadEngine` |
| 195 | Download engine | Per-part URL fail-over (CDN fallback) | ✅ | ✅ | Co: per-`Segment` URL |
| 196 | Download engine | Per-part path fail-over | ✅ | ⚠️ | Co: single temp file |
| 197 | Download engine | Resume an interrupted download | ✅ | ✅ | Co: ETag-guarded |
| 198 | Download engine | DASH / adaptive segmented | ✅ | ✅ |  |
| 199 | Download engine | HLS download | ✅ | ✅ | Co: saves HLS |
| 200 | Download engine | Audio+video merge (mux) | ✅ | ✅ | VM native codec · Co FFmpeg |
| 201 | Download engine | Video → audio extraction | ✅ | ✅ |  |
| 202 | Download engine | Quality/format conversion tiers | ✅ | ✅ | Co: FFmpeg ladder |
| 203 | Download engine | BitTorrent / magnet | ✅ | ❌ | VM `download.bt.core.*` |
| 204 | Download engine | SABR (segmented adaptive) | ✅ | ❌ |  |
| 205 | Download engine | Playlist / batch download | ✅ | ✅ | Co: real playlist + line batch |
| 206 | Download engine | Parallel segment count control | ✅ | ⚠️ | Co: fixed concurrency |
| 207 | Download engine | Auto-retry on network failure | ✅ | ✅ | Co: 3 retries |
| 208 | Download engine | Storage-full handling | ✅ | ✅ |  |
| 209 | Download engine | Custom download location | ✅ | ✅ |  |
| 210 | Download engine | Foreground download service | ✅ | ✅ |  |
| 211 | Download engine | Downloads survive UI death | ✅ | ✅ | Co: WorkManager + service |
| 212 | Download engine | Persisted download state | ✅ | ✅ | VM Room+MMKV · Co Room |
| 213 | Download engine | AIDL / cross-process download service | ✅ | ❌ | Co: single-process |
| 214 | Download engine | Cancel in-progress download | ✅ | ✅ |  |
| 215 | Download engine | Cancel in-progress transcode | ✅ | ✅ | Co: FFmpegKit cancel |
| 216 | Download engine | Download speed metering | ✅ | ✅ |  |
| 217 | Site support & extraction | YouTube | ✅ | ✅ |  |
| 218 | Site support & extraction | TikTok | ✅ | ✅ |  |
| 219 | Site support & extraction | Twitter / X | ✅ | ✅ |  |
| 220 | Site support & extraction | Instagram | ✅ | ✅ |  |
| 221 | Site support & extraction | Facebook | ✅ | ✅ |  |
| 222 | Site support & extraction | Dailymotion | ✅ | ✅ |  |
| 223 | Site support & extraction | Vimeo | ✅ | ✅ |  |
| 224 | Site support & extraction | Pinterest | ✅ | ✅ |  |
| 225 | Site support & extraction | Reddit | ❓ | ✅ |  |
| 226 | Site support & extraction | SoundCloud | ❓ | ✅ |  |
| 227 | Site support & extraction | Twitch | ❓ | ✅ |  |
| 228 | Site support & extraction | Bilibili | ❓ | ✅ |  |
| 229 | Site support & extraction | Tumblr | ❓ | ✅ |  |
| 230 | Site support & extraction | Broad catch-all extractor | ✅ | ✅ | Co: any `http(s)` host offered |
| 231 | Site support & extraction | Extractor updatable without app update | ✅ | ✅ | Co: remote `supportedHosts` |
| 232 | Site support & extraction | Media sniffing from web pages | ✅ | ✅ | Co: `MediaSniffer` |
| 233 | Site support & extraction | VPN hint on failure | ✅ | ✅ | Co: `ResolveFailureAdvisor` |
| 234 | Site support & extraction | Playlist URL support | ✅ | ✅ | Co: `PlaylistRepository` |
| 235 | Site support & extraction | YouTube public search / trending | ✅ | ✅ | Co: InnerTube |
| 236 | Site support & extraction | YouTube Music discovery | ✅ | ✅ | Co: `FEmusic_home` |
| 237 | Site support & extraction | Channel pages | ✅ | ✅ |  |
| 238 | Site support & extraction | Comments | ✅ | ✅ |  |
| 239 | Site support & extraction | YouTube sign-in (account state) | ✅ | ❌ | Needs OAuth; public surface done |
| 240 | Site support & extraction | Subscriptions / watch-later / history | ✅ | ❌ | Account-only |
| 241 | Site support & extraction | Clipboard link detection | ✅ | ✅ |  |
| 242 | Site support & extraction | Share-sheet intake | ✅ | ✅ |  |
| 243 | Site support & extraction | JS/WebView-driven extraction | ✅ | ❌ | Co uses cobalt backend |
| 244 | Media playback | Inline video playback | ✅ | ✅ |  |
| 245 | Media playback | Full-screen playback | ✅ | ✅ |  |
| 246 | Media playback | HLS / DASH playback | ✅ | ✅ |  |
| 247 | Media playback | Picture-in-picture | ✅ | ✅ |  |
| 248 | Media playback | Floating overlay player | ✅ | ❌ |  |
| 249 | Media playback | Background audio | ✅ | ✅ | Co: `MusicPlaybackService` |
| 250 | Media playback | Media-button / headset control | ✅ | ✅ | Co: MediaSession |
| 251 | Media playback | Playback speed | ✅ | ✅ |  |
| 252 | Media playback | Subtitle / caption support | ✅ | ✅ |  |
| 253 | Media playback | In-player quality switch | ✅ | ✅ |  |
| 254 | Media playback | Equalizer | ✅ | ✅ |  |
| 255 | Media playback | Volume / brightness gestures | ✅ | ⚠️ |  |
| 256 | Media playback | Shorts autoplay + preload | ✅ | ✅ |  |
| 257 | Media playback | One-live-player discipline | ✅ | ✅ |  |
| 258 | Media playback | Playback-position persistence | ✅ | ⚠️ |  |
| 259 | Library, local media & vault | Local video scanner | ✅ | ✅ | Co: MediaStore |
| 260 | Library, local media & vault | Local music scanner | ✅ | ✅ |  |
| 261 | Library, local media & vault | Local media search | ✅ | ✅ |  |
| 262 | Library, local media & vault | Sort / filter | ✅ | ✅ |  |
| 263 | Library, local media & vault | Multi-select | ✅ | ✅ |  |
| 264 | Library, local media & vault | Delete a file from the app | ✅ | ✅ |  |
| 265 | Library, local media & vault | Watch history | ✅ | ✅ |  |
| 266 | Library, local media & vault | Likes / bookmarks | ✅ | ✅ |  |
| 267 | Library, local media & vault | Dislike | ✅ | ❌ |  |
| 268 | Library, local media & vault | Private vault (passcode) | ✅ | ✅ |  |
| 269 | Library, local media & vault | Vault import / export | ✅ | ✅ |  |
| 270 | Library, local media & vault | Storage-usage stats | ✅ | ❌ |  |
| 271 | In-app browser | In-app browser | ✅ | ✅ |  |
| 272 | In-app browser | Multi-tab | ✅ | ✅ |  |
| 273 | In-app browser | Incognito | ✅ | ✅ |  |
| 274 | In-app browser | Desktop-site toggle | ✅ | ✅ |  |
| 275 | In-app browser | Media sniffing | ✅ | ✅ |  |
| 276 | In-app browser | Web-game browsing | ✅ | ✅ |  |
| 277 | In-app browser | History / bookmarks | ✅ | ❌ |  |
| 278 | In-app browser | Browser anti-detection | ✅ | ❌ | Excluded by design |
| 279 | Discovery & engagement | Home recommendation feed | ✅ | ✅ |  |
| 280 | Discovery & engagement | Trending / discovery | ✅ | ✅ | Co: `FEtrending` |
| 281 | Discovery & engagement | In-app search with results | ✅ | ✅ | Co: Explore → Search |
| 282 | Discovery & engagement | Search-result paging | ✅ | ✅ | Co: continuation tokens |
| 283 | Discovery & engagement | Channel pages | ✅ | ✅ |  |
| 284 | Discovery & engagement | Shorts feed | ✅ | ✅ | Co: 4 merged sources |
| 285 | Discovery & engagement | Like / dislike | ✅ | ⚠️ | Co: like only |
| 286 | Discovery & engagement | Comments | ✅ | ✅ |  |
| 287 | Discovery & engagement | Recommendation engine ("because you watched") | ✅ | ✅ | Co: on-device, opt-in |
| 288 | Discovery & engagement | Adult-content filter | ✅ | ➖ |  |
| 289 | Music | Music player | ✅ | ✅ |  |
| 290 | Music | Albums / singers browse | ✅ | ❌ |  |
| 291 | Music | Lyrics | ✅ | ✅ | Co: `LrcParser` |
| 292 | Music | Online lyrics search | ✅ | ❌ |  |
| 293 | Music | Playlists | ✅ | ✅ |  |
| 294 | Music | Equalizer | ✅ | ✅ |  |
| 295 | Music | Sleep timer | ✅ | ❌ |  |
| 296 | Music | YouTube Music | ✅ | ✅ |  |
| 297 | Music | Offline / Bluetooth share | ✅ | ❌ |  |
| 298 | Social & sharing | WhatsApp status saver | ✅ | ✅ | Co: `status/*` |
| 299 | Social & sharing | Social share sheet | ✅ | ✅ |  |
| 300 | Social & sharing | Offline / Bluetooth share | ✅ | ❌ |  |
| 301 | Social & sharing | Share-link shorteners | ✅ | ➖ | VM marketing hosts |
| 302 | Notifications & push | Download notifications | ✅ | ✅ |  |
| 303 | Notifications & push | Music notifications | ✅ | ✅ |  |
| 304 | Notifications & push | Recommendation notifications | ✅ | ✅ | Co: local, on-device |
| 305 | Notifications & push | Separate channels | ✅ | ✅ |  |
| 306 | Notifications & push | Server-originated push (FCM) | ✅ | ❌ | Needs Firebase project |
| 307 | Notifications & push | Keep-alive / resident notice | ✅ | ➖ |  |
| 308 | Notifications & push | App widgets | ✅ (3) | ✅ (1) |  |
| 309 | Platform & technical | Plugin architecture | ✅ | ❌ | VM Tencent Shadow |
| 310 | Platform & technical | Dynamic code loading / hot-fix | ✅ | ❌ | Excluded by design |
| 311 | Platform & technical | Native codec libraries | ✅ | ✅ | VM codec · Co FFmpeg |
| 312 | Platform & technical | Server-driven remote config | ✅ | ✅ | Co: `remote-config.json` |
| 313 | Platform & technical | Self-update (in-app APK install) | ✅ | ✅ | Co: `UpdateChecker` + `ApkInstaller` |
| 314 | Platform & technical | Crash handler / auto-restart | ✅ | ✅ | Co: `CrashHandler` |
| 315 | Platform & technical | Multi-process architecture | ✅ | ❌ | Co: single process |
| 316 | Platform & technical | AIDL service interfaces | ✅ | ❌ |  |
| 317 | Platform & technical | Signed store release | ❌ | ❌ | Both sideload-only |
| 318 | Platform & technical | Unit-test suite | ❓ | ✅ | Co: 111 JVM tests |
| 319 | Platform & technical | Open-source license | ❌ | ✅ | Co ships `LICENSE` |
| 320 | Platform & technical | 64-bit ABI support | ✅ | ✅ |  |
| 321 | Security & privacy posture | `MANAGE_EXTERNAL_STORAGE` | ✅ | ❌ | Co: scoped storage |
| 322 | Security & privacy posture | `SYSTEM_ALERT_WINDOW` | ✅ | ❌ |  |
| 323 | Security & privacy posture | `REQUEST_INSTALL_PACKAGES` | ✅ | ✅ | Co: for self-update only |
| 324 | Security & privacy posture | `GET_TASKS` / `KILL_BACKGROUND_PROCESSES` | ✅ | ❌ |  |
| 325 | Security & privacy posture | Notification-listener access | ✅ | ❌ |  |
| 326 | Security & privacy posture | Cleartext traffic allowed | ✅ | ⚠️ | Co ships network-security config |
| 327 | Security & privacy posture | Runtime package-name spoofing | ✅ | ❌ |  |
| 328 | Security & privacy posture | Weak XOR string obfuscation | ✅ | ❌ |  |
| 329 | Security & privacy posture | First-party API keys in plaintext | ✅ | ❌ |  |
| 330 | Security & privacy posture | Ad / tracking SDKs | ✅ (6) | ❌ |  |
| 331 | Security & privacy posture | Process daemon / keep-alive | ✅ | ❌ |  |
| 332 | Security & privacy posture | Scoped storage (MediaStore / SAF) | ⚠️ | ✅ |  |
| 333 | Security & privacy posture | Dynamic code loading | ✅ | ❌ |  |
| 334 | Security & privacy posture | Packer / anti-tamper stack | ✅ | ❌ |  |


---

## Scorecard (from the rows above)

Monetisation (Growth) and Security-posture rows are excluded from the total: the
first is a deliberate non-goal, the second describes traits where a VidMate check
is not a point in VidMate's favour.

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

¹ Excluded from the total.

---

## Cobalt-only (things VidMate has no equivalent for)

| # | Feature | Notes |
|--:|---|---|
| 1 | Ad-free by construction | No ad SDKs, no tracking |
| 2 | Material-You dynamic colour | Android 12+ |
| 3 | 4-source merged Shorts pipeline | InnerTube / Invidious / NewPipe / PeerTube |
| 4 | 111-test JVM suite | All green |
| 5 | Open-source license | `LICENSE` present |
| 6 | Invidious instance pool | User-tunable |
| 7 | Shorts query pool | User-tunable |
| 8 | Home-feed query pool | User-tunable |
| 9 | Optional cobalt API-key field | |
| 10 | Scoped storage by default | No `MANAGE_EXTERNAL_STORAGE` |
| 11 | On-device recommender, no account | Local likes/history only |
| 12 | Extra confirmed sites | Reddit, SoundCloud, Twitch, Bilibili, Tumblr |
