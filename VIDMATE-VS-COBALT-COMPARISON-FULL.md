# VidMate vs Cobalt Android — full feature & capability comparison

Legend: ✅ present · ❌ absent · ⚠️ partial/gap · ➖ intentionally out of scope (monetisation) · ❓ not confirmed by the static evidence.

Evidence: VidMate `VIDMATE-REVERSE-ENGINEERING.md` (static decompile — host 2,502 classes + plugin 21,218 classes, 159 activities). Cobalt: current source tree in `cobalt-android/`. Both static; no runtime observed.

## 1. App shell, navigation & UI
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Launcher activity | ✅ | ✅ |
| Branded splash screen | ✅ | ✅ |
| Splash advertisement | ✅ | ➖ |
| Splash progress/countdown | ✅ | ⚠️ |
| First-run onboarding / coach screens | ✅ | ✅ |
| Onboarding gated to first launch only | ✅ | ✅ |
| Share-sheet entry (`ACTION_SEND`) | ✅ | ✅ |
| Open-with entry (`ACTION_VIEW`) | ✅ | ❌ |
| Music-player launch alias | ✅ | ❌ |
| Long-press app shortcuts | ✅ | ✅ |
| Deep-link / URL-scheme entry | ✅ | ❌ |
| Clipboard link auto-detect | ✅ | ✅ |
| Network-offline screen | ✅ | ⚠️ |
| Bottom navigation bar | ✅ | ✅ |
| Tab set server-configurable | ✅ | ✅ |
| Default tab layout | ✅ | ✅ |
| "Earn cash" alternate layout | ✅ | ➖ |
| Per-tab back-stack (nested nav) | ✅ | ✅ |
| Floating action / download button | ✅ | ✅ |
| Download-count badge | ✅ | ✅ |
| Pull-to-refresh on feeds | ✅ | ⚠️ |
| Settings reachable from shell | ✅ | ✅ |
| Persistent mini-player bar | ✅ | ✅ |
| Bottom-bar visibility driven by config | ✅ | ✅ |
| Home / discovery | ✅ | ✅ |
| Search screen | ✅ | ✅ |
| Trending / discovery | ✅ | ✅ |
| Video detail screen | ✅ | ✅ |
| Full-screen player | ✅ | ✅ |
| Downloads / files screen | ✅ | ✅ |
| Download queue sheet | ✅ | ✅ |
| In-app browser | ✅ | ✅ |
| Local media library | ✅ | ✅ |
| Private vault | ✅ | ✅ |
| WhatsApp status saver | ✅ | ✅ |
| Music screen | ✅ | ✅ |
| YouTube Music | ✅ | ✅ |
| Channel pages | ✅ | ✅ |
| Comments | ✅ | ✅ |
| Games / web-games hub | ✅ | ✅ |
| App store (9Apps) | ✅ | ➖ |
| Wallet / earn-cash | ✅ | ➖ |
| Cleaner / speed-booster | ✅ | ➖ |
| Daily-tasks / rewards | ✅ | ➖ |
| Settings (multiple sub-screens) | ✅ | ✅ |
| History / bookmarks | ✅ | ✅ |
| Language picker screen | ✅ | ✅ |
| Region/location picker screen | ✅ | ✅ |
| Download-path picker screen | ✅ | ✅ |
| Push-settings screen | ✅ | ✅ |
| Widget-settings screen | ✅ | ❌ |
| Safe-mode screen | ✅ | ➖ |
| Login / account screen | ✅ | ➖ |
| Update / "safe update" screen | ✅ | ⚠️ |
| In-app language picker | ✅ | ✅ |
| In-app region picker | ✅ | ✅ |
| Translated string resources | ✅ (33) | ❌ (1) |
| Selectable UI languages | ✅ (83) | ⚠️ (15) |
| Selectable regions | ✅ (107) | ⚠️ |
| RTL layout support | ✅ | ✅ |
| Language-of-content rule (max 2) | ✅ | ❌ |
| First-run walkthrough | ✅ | ✅ |
| Notification-permission explainer | ✅ | ✅ |
| Overlay-permission guide | ✅ | ➖ |
| Storage-permission guide | ✅ | ➖ |
| Battery-optimisation prompt | ✅ | ✅ |
| Install-packages prompt (self-update) | ✅ | ✅ |
| VPN/geo-failure guidance | ✅ | ✅ |
| Empty-state guidance | ✅ | ✅ |
| Share sheet out | ✅ | ✅ |
| Receive shares from other apps | ✅ | ✅ |
| Share to specific social apps | ✅ | ✅ |
| Offline / Bluetooth share | ✅ | ❌ |
| QR / proximity share | ✅ | ❌ |

## 2. Download engine
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Paste-a-link box | ✅ | ✅ |
| Link validity feedback | ✅ | ✅ |
| Format/quality picker sheet | ✅ | ✅ |
| Resolution labels ("1080p" etc.) | ✅ | ✅ |
| Audio-only option | ✅ | ✅ |
| Default quality preference | ✅ | ✅ |
| Download progress row (bytes/%) | ✅ | ✅ |
| Download speed display (MB/s) | ✅ | ✅ |
| Multi-select downloads | ✅ | ✅ |
| Playlist / batch download dialog | ✅ | ✅ |
| Cancel in-progress download | ✅ | ✅ |
| Retry a failed download | ✅ | ✅ |
| Open a finished download | ✅ | ✅ |
| Delete a finished download | ✅ | ✅ |
| Clear download history | ✅ | ✅ |
| Download-in-progress notification | ✅ | ✅ |
| Download-complete notification | ✅ | ✅ |
| Pre-download interstitial ad | ✅ | ➖ |
| HTTP(S) download | ✅ | ✅ |
| Multi-segment / range downloads | ✅ | ✅ |
| Per-part URL fail-over (CDN fallback) | ✅ | ✅ |
| Per-part path fail-over | ✅ | ⚠️ |
| Resume an interrupted download | ✅ | ✅ |
| DASH / adaptive segmented | ✅ | ✅ |
| HLS download | ✅ | ✅ |
| Audio+video merge (mux) | ✅ | ✅ |
| Video → audio extraction | ✅ | ✅ |
| Quality/format conversion tiers | ✅ | ✅ |
| BitTorrent / magnet | ✅ | ❌ |
| SABR (segmented adaptive) | ✅ | ❌ |
| Playlist / batch download | ✅ | ✅ |
| Parallel segment count control | ✅ | ⚠️ |
| Auto-retry on network failure | ✅ | ✅ |
| Storage-full handling | ✅ | ✅ |
| Custom download location | ✅ | ✅ |
| Foreground download service | ✅ | ✅ |
| Downloads survive UI death | ✅ | ✅ |
| Persisted download state | ✅ | ✅ |
| AIDL / cross-process download service | ✅ | ❌ |
| Cancel in-progress transcode | ✅ | ✅ |
| Download speed metering | ✅ | ✅ |

## 3. Site support & extraction
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| YouTube | ✅ | ✅ |
| TikTok | ✅ | ✅ |
| Twitter / X | ✅ | ✅ |
| Instagram | ✅ | ✅ |
| Facebook | ✅ | ✅ |
| Dailymotion | ✅ | ✅ |
| Vimeo | ✅ | ✅ |
| Pinterest | ✅ | ✅ |
| Reddit | ❓ | ✅ |
| SoundCloud | ❓ | ✅ |
| Twitch | ❓ | ✅ |
| Bilibili | ❓ | ✅ |
| Tumblr | ❓ | ✅ |
| Broad catch-all extractor | ✅ | ✅ |
| Extractor updatable without app update | ✅ | ✅ |
| Media sniffing from web pages | ✅ | ✅ |
| VPN hint on failure | ✅ | ✅ |
| Playlist URL support | ✅ | ✅ |
| YouTube public search / trending | ✅ | ✅ |
| YouTube Music discovery | ✅ | ✅ |
| Channel pages | ✅ | ✅ |
| Comments | ✅ | ✅ |
| YouTube sign-in (account state) | ✅ | ❌ |
| Subscriptions / watch-later / history | ✅ | ❌ |
| Clipboard link detection | ✅ | ✅ |
| Share-sheet intake | ✅ | ✅ |
| JS/WebView-driven extraction | ✅ | ❌ |

## 4. Media playback
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Inline player controls | ✅ | ✅ |
| Full-screen / landscape | ✅ | ✅ |
| Picture-in-picture | ✅ | ✅ |
| Floating pop-up window | ✅ | ❌ |
| Playback-speed menu | ✅ | ✅ |
| Quality switch in player | ✅ | ✅ |
| Subtitle toggle | ✅ | ✅ |
| Equalizer panel | ✅ | ✅ |
| Pre-roll ad | ✅ | ➖ |
| Gesture controls (seek/brightness) | ✅ | ⚠️ |
| Next/prev in a Shorts feed | ✅ | ✅ |
| Resume playback position | ✅ | ⚠️ |
| Inline video playback | ✅ | ✅ |
| Full-screen playback | ✅ | ✅ |
| HLS / DASH playback | ✅ | ✅ |
| Floating overlay player | ✅ | ❌ |
| Background audio | ✅ | ✅ |
| Media-button / headset control | ✅ | ✅ |
| Playback speed | ✅ | ✅ |
| Subtitle / caption support | ✅ | ✅ |
| In-player quality switch | ✅ | ✅ |
| Equalizer | ✅ | ✅ |
| Volume / brightness gestures | ✅ | ⚠️ |
| Shorts autoplay + preload | ✅ | ✅ |
| One-live-player discipline | ✅ | ✅ |
| Playback-position persistence | ✅ | ⚠️ |

## 5. Library, local media & vault
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Downloads list with thumbnails | ✅ | ✅ |
| Local video library grid | ✅ | ✅ |
| Local music library | ✅ | ✅ |
| Search within local media | ✅ | ✅ |
| Sort / filter chips | ✅ | ✅ |
| Multi-select + batch delete | ✅ | ✅ |
| File detail / info | ✅ | ⚠️ |
| Watch history timeline | ✅ | ✅ |
| Liked / bookmarks list | ✅ | ✅ |
| Storage-usage display | ✅ | ❌ |
| Passcode gate | ✅ | ✅ |
| Create-passcode first-run flow | ✅ | ✅ |
| Import into vault | ✅ | ✅ |
| Export from vault | ✅ | ✅ |
| Hidden from system gallery | ✅ | ✅ |
| "Hide/unhide" confirmation UX | ✅ | ⚠️ |
| Local video scanner | ✅ | ✅ |
| Local music scanner | ✅ | ✅ |
| Local media search | ✅ | ✅ |
| Sort / filter | ✅ | ✅ |
| Multi-select | ✅ | ✅ |
| Delete a file from the app | ✅ | ✅ |
| Watch history | ✅ | ✅ |
| Likes / bookmarks | ✅ | ✅ |
| Dislike | ✅ | ❌ |
| Private vault (passcode) | ✅ | ✅ |
| Vault import / export | ✅ | ✅ |
| Storage-usage stats | ✅ | ❌ |

## 6. In-app browser
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Browser with URL bar | ✅ | ✅ |
| Multi-tab UI | ✅ | ✅ |
| Incognito toggle | ✅ | ✅ |
| Desktop-site toggle | ✅ | ✅ |
| Media-sniff download prompt | ✅ | ✅ |
| "Download this page's video" CTA | ✅ | ✅ |
| Web-game browser mode | ✅ | ✅ |
| Browser history / bookmarks | ✅ | ❌ |
| Browser anti-detection | ✅ | ❌ |
| In-app browser | ✅ | ✅ |
| Multi-tab | ✅ | ✅ |
| Incognito | ✅ | ✅ |
| Media sniffing | ✅ | ✅ |
| Web-game browsing | ✅ | ✅ |
| History / bookmarks | ✅ | ❌ |

## 7. Discovery & engagement
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Recommendation feed | ✅ | ✅ |
| Feed grid layout | ✅ | ✅ |
| Section headers | ✅ | ✅ |
| Home pill search bar | ✅ | ✅ |
| Feed ad slots | ✅ | ➖ |
| Adult-content hiding in feed | ✅ | ➖ |
| Feed refresh / rotate | ✅ | ✅ |
| User-tunable feed topics | ❌ | ✅ |
| Home recommendation feed | ✅ | ✅ |
| Trending / discovery | ✅ | ✅ |
| In-app search with results | ✅ | ✅ |
| Search-result paging | ✅ | ✅ |
| Channel pages | ✅ | ✅ |
| Shorts feed | ✅ | ✅ |
| Like / dislike | ✅ | ⚠️ |
| Comments | ✅ | ✅ |
| Recommendation engine ("because you watched") | ✅ | ✅ |
| Adult-content filter | ✅ | ➖ |

## 8. Music & social
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Music player screen | ✅ | ✅ |
| Now-playing with artwork | ✅ | ✅ |
| Lyrics view | ✅ | ✅ |
| Lyrics search (online) | ✅ | ❌ |
| Playlists | ✅ | ✅ |
| Albums / singers browse | ✅ | ❌ |
| Music discovery feed | ✅ | ✅ |
| Equalizer | ✅ | ✅ |
| Sleep timer | ✅ | ❌ |
| Offline / Bluetooth share | ✅ | ❌ |
| Music player | ✅ | ✅ |
| Lyrics | ✅ | ✅ |
| Online lyrics search | ✅ | ❌ |
| YouTube Music | ✅ | ✅ |
| WhatsApp status saver | ✅ | ✅ |
| Social share sheet | ✅ | ✅ |
| Share-link shorteners | ✅ | ➖ |

## 9. Settings & theming
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Theme picker | ✅ | ✅ |
| Language picker | ✅ | ✅ |
| Region / content-locale picker | ✅ | ✅ |
| Download-path selector | ✅ | ✅ |
| Default quality/format | ✅ | ✅ |
| Clipboard trigger toggle | ✅ | ✅ |
| Audio-only mode | ✅ | ✅ |
| Notification toggles | ✅ | ✅ |
| Recommendation opt-in toggle | ❌ | ✅ |
| Widget settings | ✅ | ❌ |
| Adult-content hide | ✅ | ➖ |
| Safe mode | ✅ | ➖ |
| Invidious instance pool | ❌ | ✅ |
| Shorts query pool | ❌ | ✅ |
| Home-feed query pool | ❌ | ✅ |
| Cobalt API-key field | ❌ | ✅ |
| Clear history action | ✅ | ✅ |
| Battery-optimisation shortcut | ✅ | ✅ |
| Check-for-update action | ✅ | ✅ |
| Light theme | ✅ | ✅ |
| Dark / night theme | ✅ | ✅ |
| Follow-system theme | ✅ | ✅ |
| Material-You dynamic colour | ❌ | ✅ |
| Brand-blue accent palette | ✅ | ✅ |
| Accent/gold highlights | ✅ | ✅ |
| Custom animated widgets | ✅ | ⚠️ |
| Rounded feed tiles | ✅ | ✅ |
| Server-managed skins | ✅ | ❌ |
| Download-progress notification | ✅ | ✅ |
| Download-complete notification | ✅ | ✅ |
| Music-player notification | ✅ | ✅ |
| Recommendation notification | ✅ | ✅ |
| Rich notification layouts | ✅ (22) | ⚠️ |
| Notification action buttons | ✅ | ✅ |
| Separate notification channels | ✅ | ✅ |
| Home-screen app widgets | ✅ (3) | ✅ (1) |
| Widget deep-link into app | ✅ | ✅ |
| Download notifications | ✅ | ✅ |
| Music notifications | ✅ | ✅ |
| Recommendation notifications | ✅ | ✅ |
| Separate channels | ✅ | ✅ |
| Server-originated push (FCM) | ✅ | ❌ |
| Keep-alive / resident notice | ✅ | ➖ |
| App widgets | ✅ (3) | ✅ (1) |

## 10. Monetisation & growth layer
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| In-app app store (9Apps) | ✅ | ➖ |
| Game centre / web games | ✅ | ⚠️ |
| Earn-cash / wallet UX | ✅ | ➖ |
| Daily tasks / coins | ✅ | ➖ |
| Cleaner / speed-booster | ✅ | ➖ |
| Interstitial / banner / native ads | ✅ | ➖ |
| Exit-popup ad | ✅ | ➖ |
| Ad-free experience | ❌ | ✅ |

## 11. Platform & technical
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| Plugin architecture | ✅ | ❌ |
| Dynamic code loading / hot-fix | ✅ | ❌ |
| Native codec libraries | ✅ | ✅ |
| Server-driven remote config | ✅ | ✅ |
| Self-update (in-app APK install) | ✅ | ✅ |
| Crash handler / auto-restart | ✅ | ✅ |
| Multi-process architecture | ✅ | ❌ |
| AIDL service interfaces | ✅ | ❌ |
| Signed store release | ❌ | ❌ |
| Unit-test suite | ❓ | ✅ |
| Open-source license | ❌ | ✅ |
| 64-bit ABI support | ✅ | ✅ |

## 12. Security & privacy posture
| Capability | VidMate | Cobalt |
|---|:--:|:--:|
| `MANAGE_EXTERNAL_STORAGE` | ✅ | ❌ |
| `SYSTEM_ALERT_WINDOW` | ✅ | ❌ |
| `REQUEST_INSTALL_PACKAGES` | ✅ | ✅ |
| `GET_TASKS` / `KILL_BACKGROUND_PROCESSES` | ✅ | ❌ |
| Notification-listener access | ✅ | ❌ |
| Cleartext traffic allowed | ✅ | ⚠️ |
| Runtime package-name spoofing | ✅ | ❌ |
| Weak XOR string obfuscation | ✅ | ❌ |
| First-party API keys in plaintext | ✅ | ❌ |
| Ad / tracking SDKs | ✅ (6) | ❌ |
| Process daemon / keep-alive | ✅ | ❌ |
| Scoped storage (MediaStore / SAF) | ⚠️ | ✅ |
| Dynamic code loading | ✅ | ❌ |
| Packer / anti-tamper stack | ✅ | ❌ |

## Scorecard (compared rows only; ➖ excluded)
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

¹ Excluded from the total (monetisation is a non-goal; security marks describe "does this exist").

## The short version
- **VidMate has that Cobalt does not**: the whole growth/monetisation layer, the account-only YouTube state (subscriptions, watch-later, history), the heavy downloader extras (BitTorrent, SABR), the floating overlay player, the music extras (albums, online lyrics, sleep timer, Bluetooth share), the browser history/bookmarks, the plugin/hot-fix architecture, multi-process + AIDL, server FCM push, widget settings, and 33 translated locale files.
- **Cobalt has that VidMate does not**: ad-free by construction, Material-You dynamic colour, scoped storage instead of `MANAGE_EXTERNAL_STORAGE`, a 4-source merged Shorts pipeline, an on-device recommender that needs no account, a 111-test JVM suite, an open-source license, and user-tunable knobs (Invidious pool, Shorts query pool, feed query pool, Cobalt API key). Cobalt also confirms several sites (Reddit, SoundCloud, Twitch, Bilibili, Tumblr) outside VidMate's confirmed extraction set.
- **Net**: across the compared rows VidMate covers 287 capabilities to Cobalt's 231 ✅ (+17 partial), with 35 open. Cobalt's open rows split three ways: deliberate non-goals (monetisation, browser anti-detection, plugin framework, dynamic code loading, multi-process/AIDL); external-credential blockers (account-only YouTube state needs OAuth, server FCM push needs a Firebase project); and genuine remaining engineering (BitTorrent, SABR, floating overlay player, music albums/online-lyrics/sleep-timer/Bluetooth share, browser history/bookmarks, storage-usage stats, file detail view, translated strings, widget settings, dislike, saved playback position, parallel-segment control).
- **Honesty notes**: both apps are compared from static evidence (no runtime observed); ⚠️ marks are cases where Cobalt has the plumbing but not the finished behaviour.

