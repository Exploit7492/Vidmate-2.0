# VidMate — Reverse-Engineering Report

**Target:** `com.nemo.vidmate` (VidMate) version **6.0204** (versionCode 60204)
**Method:** static analysis only (REA + JADX + Apktool). The app was never executed.
**Tooling:** REA 6.8.0 (MCP/CLI) → JADX headless 0.7.1 bridge; JADX CLI 1.5.3; Apktool 2.9.3; OpenJDK 21.
**Date:** 2026-11-09

> All findings below are **static observations** from the shipped artifact. They describe what the
> code declares and appears to do, not verified runtime behaviour. Limitations are listed at the end.

---

## 1. Artifact and provenance

| Property | Value |
|---|---|
| Package | `com.nemo.vidmate` |
| Version | 6.0204 (code 60204) |
| minSdk / targetSdk / compileSdk | 21 / 35 / 35 |
| APK size | 34,761,850 bytes |
| APK SHA-256 | `9bc099d538f1da2560637630ac1db984bfc05678029358a0431d7d21d299d200` |
| APK MD5 | `bbfa9ec3e6835165928c3ce18cb85a6d` (matches Aptoide "kndes" store listing) |
| Signer (store metadata) | `CN=nemo`, SHA-1 `60:F7:C9:6D:AF:7F:99:26:32:55:39:61:2E:7D:9E:2D:14:F4:AA:01` |
| Build | AGP 8.7.3, git revision `698d2b4c5196b237b258f5bf7b09a9f5437c74ad` |

The vendor's own site (`vidmateapp.com`) now returns a *"service has been discontinued"* page; the APK
was obtained from the Aptoide mirror and its MD5 was verified against the store listing.

### 1.1 The APK is a two-layer application

The downloaded APK is **not** the whole app. It is a thin **Tencent Shadow plugin host** that
downloads/loads the real application as an encrypted plugin at runtime.

| Layer | DEX | Classes | Role |
|---|---|---|---|
| **Host** | `classes.dex` (2.66 MB) | 2,502 | Loader, splash, update, plugin manager |
| **Plugin** (`assets/4D6F6E204A`) | 5 DEX + 1 hot-fix DEX | 21,218 | The actual VidMate app |

**Plugin decryption** (`assets/config.json`):

```json
{"plugins":[{"partKey":["vidmate","remote","core"],"pluginVersion":"60204",
  "apkName":"4D6F6E204A","hash":"5AE1CCB123B13253D80DFDA2EE517D9B",
  "abi":"arm-v78a","encryptType":"xor","encryptParams":"1207"}]}
```

`assets/4D6F6E204A` (34.58 MB) is a ZIP/APK XOR-encrypted with the **single-byte key `0xB7`**
(the `"1207"` param). After decryption it is a valid APK → `plugin.apk`,
SHA-256 `7d6196a70bbb7e904f0eb441b6b053281f80148ab9d27a4985b106241214fedb`
(4,901 entries, 5 DEX, 21,218 classes). *This is trivial obfuscation, not encryption.*

### 1.2 Host native libraries

`libboost_multidex.so`, `libabichecker.so`, `libcrashlytics*.so` — multi-DEX loader, ABI check,
Crashlytics NDK.

### 1.3 Plugin native libraries (`lib/arm64-v8a`, `armeabi-v7a`)

`libvidmatecodec.so` (3.0 MB — the media converter/merger), `libmmkv.so` (Tencent MMKV key-value
store), `libapkpatch.so` / `libResourcePatchLibrary.so` (hot-fix patching), `libun7z.so`,
`libtt_ugen_layout.so` (Pangle ad layout engine), `libapminsighta/b.so`, `libpglarmor.so` /
`libse-engine.so` / `libnms.so` (packer/security), `libtiny-image.so`,
`libtobEmbedPagEncrypt.so`.

---

## 2. Architecture

### 2.1 Tencent Shadow plugin framework (host)

`MyApplication` (`com.nemo.vidmate.MyApplication`) is the host `Application`. Key facts:

- Detects its process by name and sets `sProcessType`:
  `:vidmate`/main = 0, `:remote` = 1, `:core` = 2, `:SparkProcess` = 3, `…internel` = 4.
- Initialises Shadow: `HostBridgeHub.setHostHandler(...)`, `ManagerFactoryImpl().buildManager()`,
  `ShadowApplication.instance`.
- **Resource merging:** in the main process `getResources()` returns a `MixResources(pluginResources, hostResources)`.
- **Package/PM spoofing:** `getPackageName()` and `getPackageManager()` are overridden. During
  WebView creation the app substitutes a *random or configured third-party browser package name*
  (`C0.w` interceptor, `shadow` prefs key `pkgName4WebView`) so the WebView reports a different
  host app.
- Runs a `WorkManager` job (`ShadowWorker`) every 15 min to warm/keep-alive the plugin.
- Crash handling wrapper (`installCockroach`) that rewrites "black screen" / plugin `ClassNotFoundException`
  crashes and restarts `WelcomeActivity`.

Host UI is only a **splash + progress bar**:
- `host/WelcomeActivity` and `host/MainActivity` both `setContentView(R.layout.host_welcome)` and
  drive a `ProgressBar`/`TextView` via `C0477x` while the plugin loads.
- `MainActivity.w()` **pre-inflates 22 notification `RemoteViews` layouts** (download notifications,
  music player notifications, resident notices, ad notifications) before the plugin is ready.
- When the plugin is loaded, Shadow transparently re-launches the plugin's real `WelcomeActivity`/`MainActivity`.

### 2.2 Shadow plugin services (host)

`com.nemo.vidmate.shadow.service.*` extend Shadow's `PluginProcessService`:
`vidmate` (main, `ppsOpt.isMainProcess = true`), `remote` (foreground notification channel `vdmInitChannel`),
`core` (`doOdex = true`, `useHostResource = true`), `internel` (forwards to
`com.nemo.vidmate.ad.inmobi.InmobiService`), plus `fcmForPlugin` for FCM. These keep the plugin alive
in separate processes.

### 2.3 Hot-fix (in-app code update)

- `com.nemo.vidmate.repo.ut.dex.*` (`BaseHotFix`, `BaseDexHotFixProxy`) define a pluggable
  interface the app loads dynamically; `UtParser` (video-info parser) is one such hot-fixable unit.
- `com.nemo.hotfix.base.*` (59 classes) is the hot-fix runtime.
- `com.nemo.vidmate.update.*` + `UpdateControlActivity` handle APK updates; `libapkpatch.so` +
  `libResourcePatchLibrary.so` implement resource/APK patching.
- A **second DEX is carried inside the plugin**: `assets/ht_resource/app_dex_10000020.dex` (1.17 MB),
  loaded on demand — another dynamic-code channel.

### 2.4 The extractor subsystem (separate DEX + jar)

- `com.heflash.feature.extractor.*` — 907 classes, **803 in `…extractor.api` and heavily obfuscated**
  (`A`…`Z`, `a`…`zz`). This is the download-site **extractor engine**, wired to the host through the
  `ServiceClient` / `HostContext` interface (`ServiceClientImpl.callService(...)`).
- `assets/ht_resource/ds_10590005.jar` (849 KB) — a second extractor payload.
- Host-side interface package `com.vanced.extractor.host.*` (the interface the extractor implements).
- The engine is JS-driven: `UtParser.callJsService(...)` / `setBaseUrlOfJsServiceManifest(...)` run
  extraction logic in a WebView, and the host packages a **WebView "JS hook"** layer
  (`JsHookPkg`, `WebInterceptWrapper`, `ConfigDefaultDataCenter.getDefaultWebInterceptClassList()`,
  `getJSHookPkg()`). This is the mechanism that fetches the media URL after the page loads.

### 2.5 Processes

The plugin declares the classic multi-process layout (`:remote` download process, `:core`, `internel`)
plus `com.daemon.service.DaemonService` (process daemon / keep-alive). `MyApplication.sProcessType`
and `IDownloadInf` (`com.nemo.vidmate.download.service.IDownloadInf`) implement an AIDL download
service so downloads survive UI death.

---

## 3. UI / UX

### 3.1 Entry points and navigation

Manifest (host) declares two `MAIN` launchers and many `VIEW`/`SEND` aliases:

- `com.nemo.vidmate.host.WelcomeActivity` / `…MainActivity` (host launchers).
- Plugin: `com.nemo.vidmate.WelcomeActivity` (splash), `com.nemo.vidmate.MainActivity` (home).
- Activity aliases on `MainActivity` form the share/open surface:
  `$vidmate` (SEND), `$download` (SEND), `$NewPlayerActivity` (VIEW/SEND/SENDTO),
  `$MusicPlayerActivity`, `$JUMP`, `$INVOKE`, `$HTML`, `$WEBACTIVITY`, `$WEBACTIVITY`.

**Bottom navigation tabs** are dynamic (`ConfigDefaultDataCenter.getMainTabList()`):

- Default: **Home (`main_home`) · Downloader (`main_downloader`) · [Game Center `main_gc`] ·
  [YouTube Music `main_ytmusic`] · Me (`main_me`)**.
- An alternate **"earn cash"** layout replaces the downloader tab: `main_home · main_earn_cash ·
  main_ytmusic · main_me`.
- Tab set is remote-configurable (Flow-driven `MainTabProviderImpl`), so the shell can be re-skinned
  server-side.

The nav shell is `ui/nav/NavActivity` → `setContentView(R.layout.a0x)` (a `FrameLayout`) hosting a
`nav_fragment`.

### 3.2 Screen inventory (selected, from the 159 plugin activities)

| Area | Activities |
|---|---|
| Home / discovery | `MainActivity`, `ui.recommend.RecommendActivity`, `ui.recommend.RecommendFragment`, `ui.home.channel.HomeChannelActivity` |
| Search | `ui.search.SearchActivity`, `media.local.search.LocalMediaSearchActivity`, image/status search |
| Video | `ui.video.VideoDetailActivity`, `media.player.activity.NewPlayerActivity`, `media.player.online.OnlinePlayerActivity` |
| Music | `recommend.music.*` (Music/Album/Singer/Filter/Discover), `player.music.lyrics.LrcSearchActivity`, `com.nemo.music.impl.*` |
| TV / movies | `recommend.tvshow.*` (detail/filter/online), `recommend.fullmovie.*` |
| Local media | `media.local.localvideo.*`, `media.local.localmusic.*` |
| Private vault | `media.local.privatevideo.PrivateVideoActivity`, `PrivateVideoVerifyActivity`, `PrivateVideoImportActivity`, `PrivateVideoExportActivity`, `PrivateLeadUnHideActivity` |
| WhatsApp saver | `ui.whatsapp.AppFilesSaverActivity`, `…preview.PreviewActivity`, `…saver.HowitWorkActivity` |
| Files / downloads | `ui.download.MyFilesActivity`, `download.service.DownloadService`, `browser.getvideo.downloading_status.DownloadingStatusActivity` |
| Browser | `browser.control.activity.Browser{Standard,SingleTop,SingleTask,SingleInstance}Activity`, `GameBrowserActivity`, `BrowserSimpleActivity` |
| App store / games | `appstore.*`, `ui.nineapp.*`, `com.nemo.game_center.impl.*`, `com.nemo.clean.page.*`, `com.nemo.shorts.impl.*`, `com.nemo.daily_task.impl.*` |
| Settings | `ui.settings.SettingsActivity` / `SettingsContentActivity` / `SettingsOtherActivity`, `path.DownloadPathSelectorActivity`, `LocationSelectActivity`, `SelectLanguageActivity`, `SettingPushActivity`, `SettingAppWidgetActivity`, `GuideAllowDrawOverlayActivity` |
| Account | `account.LoginActivity`, `ui.youtube.me.YoutubeSigninActivity`, `ui.youtube.recaptcha.ReCaptchaActivity` |
| YouTube integration | `ui.youtube.subscription.*`, `ui.youtube.history.YtbHistoryActivity`, `ui.youtube.watchlater.YtbWatchLaterActivity`, `ui.youtube.me.YoutubeAccountInfoActivity` |
| Misc | `ui.safe.SafeModeActivity`, `ui.ad.WarningAdActivity`, `ui.library.LibraryActivity`, `favhis.BookmarkActivity`, `ClipboardDialogActivity`, `SkinManager` |

### 3.3 Widgets, notifications, theming

- Custom widgets: `ui.download.DownLoadAnimationView`, `TaskAddDownloadProgressView`,
  `CustomizePagerslidingTab`, `widgets.CountDownProgressBar`, `ui.nav.DragNoScrollGridView`,
  `whatsapp.widget.PhotoView`, `wheelview`.
- **Notifications are a first-class UX surface:** 22 pre-inflated `RemoteViews` layouts (download
  progress, download finish, music player big/small, resident notices, ad notifications). Strings
  confirm rich-notification configuration (`notification_slot_*`, compact slots, thumbnail aspect
  toggle, "at most three actions").
- App widgets: three providers — `HostAppWidgetFunctionProvider`, `HostAppWidgetVideosProvider`,
  `HostAppWidgetMusicProvider` (function shortcut / videos / music), plus `SettingAppWidgetActivity`.
- **Theme:** `@style/HostAppTheme` (host) / `@style/b` (plugin). Colours are obfuscated but readable:
  brand blue `#ff0099ff`, accent red `#ffea3f40`, gold `#ffffc400`, dark surfaces `#ff212121`,
  `#ff343434`, `#ff282828`. Night resources exist (`values-night`, `values-night-v29`).
- **Splash:** `WelcomeActivity` with a `CountDownProgressBar` and optional **splash ad**
  (`SplashView`, `SplashAdConfig`, `SplashPlatformConfig`, `BrandAdActivity`).

### 3.4 Localisation (a headline UX feature)

- `assets/language.json` — **83 selectable UI languages** (incl. en-IN, en-GB, es-419, pt-BR, ar, hi,
  bn, ta, te, my, etc.).
- `assets/country.json` — **107 selectable regions/locales** (locale + ISO country).
- `values/` ships **33 `strings.xml`** locale folders (ar, as, bn, de, es, fa, fr, gu, hi, hy, in, it,
  kn, ml, mr, ms, my, or, pa, pt, ru, ta, te, th, tr, ur, vi, zh-rCN, …) — ~100–185 KB each.
- In-app language/location pickers: `ChooseLanguageActivity`, `SelectLanguageActivity`,
  `LocationSelectActivity`, `manager.globalization.*` (`AppLanguage`, `Internationalization`,
  `LocaleUtils`). Strings show a **max 2 languages** rule for the language-of-content feature.

### 3.5 Onboarding & permissions UX

- `GuideAllowDrawOverlayActivity` + `ViewDrawOverlay` request the **"draw over other apps"**
  permission (used for the floating play window / clipboard bubble).
- `ui.download.TaskAddDownloadProgressView`, `ui.download.DownLoadAnimationView` give download
  feedback; `ui.home.NetworkDisconnectView` handles offline.
- Notification permission is explicitly requested and explained (`a__`, `a_7`, `a_a` strings:
  "VidMate needs Notification Permission to remind you of file download progress").

---

## 4. Capabilities and features

### 4.1 Video/audio download engine (core product)

- **Download model:** `com.nemo.vidmate.download.VideoTask` (Parcelable/Serializable) with a
  `DownloadingPartList` of `MTDownloadPart` segments. Each part serialises as
  `curPos[start-end+type+url+path]` — i.e. **multi-segment HTTP range downloads** with per-part
  URL/path overrides, retry counter, speeds, and separate `mVideoSize`/`mAudioSize` plus a
  `mCombinState` (merge step). This is a DASH/segmented downloader.
- **States:** `Analyse → Download → Merge/Combine → Install` (`VideoTask.aaa`/`aaaa`/`aaab`
  state objects; `downloadMethod`).
- **Merge/convert:** `com.nemo.vidmate.VidmateCodec` (native `libvidmatecodec.so`, plus
  `libvidmatecodec3.so`, `applibs/vidmatecodec`) converts video→audio and muxes audio+video
  (`CombinTask`). Strings confirm MP3/1080p/480p/720p conversion and "Media Converter.so" first-run.
- **Quality selection:** per-video `@itag`/`@quality`/`@resolution`; UI "Other resolutions",
  "Select resolution", "Download Music Quality", "Online Video Quality"; 2K/4K handling with
  device-capability fallbacks.
- **BitTorrent:** `download.bt.core.{Torrent, TorrentMetaInfo, BencodeFileItem}`,
  `TorrentStateParcel` — torrent downloads are supported.
- **SABR/streaming:** `download.sabr.*`, `vplayer.sabr.*` (segmented/adaptive playback download).
- **Foreground service** with `FOREGROUND_SERVICE_DATA_SYNC` / `_MEDIA_PLAYBACK` / `_SPECIAL_USE`.

### 4.2 Site extraction

- Extractor engine (obfuscated, hot-updatable, JS/WebView-driven) + `VideoGetInfoClient` /
  `PlayListGetInfoClient` in `browser.getvideo`.
- Referenced platforms (from strings + code): **YouTube (171 refs), Facebook (19), Instagram (8),
  TikTok (7), Twitter/X (8), Dailymotion (4), Vimeo (2), WhatsApp**; the UI claims
  "download everything from Instagram, Youtube, Whatsapp and other platforms".
- YouTube integration is deep: `youtubei.googleapis.com`, `www.youtube.com/watch?v=`, sign-in
  (`YoutubeSigninActivity`, reCAPTCHA), subscriptions, watch-later, history, **YouTube Music**
  (`main_ytmusic` tab, `com.nemo.music.impl.*`), channel pages, comments
  (`repo.ut.model.Comments`), likes/dislikes, playlists.
- **Playlist/status support:** `PlaylistBean`, `YtbPlayListBean`, `PLayListDetailActivity`,
  `PlayListSelectDownloadActivity`.
- **VPN hint:** strings "Using VPN to switch ip may analyze successfully, do you want to try?" and
  "Try VPN" — the app suggests a VPN when a site fails to analyse.

### 4.3 In-app browser

`com.nemo.vidmate.browser.*`: `WebViewE`, `PageBrowserJS`, `BrowserCommand`, four launch modes
(standard/singleTop/singleTask/singleInstance), tab management ("New tab", "Close all Tabs",
"Up to N tabs"), **Incognito** mode, desktop-site toggle, and **sniffing/download of media found in
pages** (`getvideo` package). A `GameBrowserActivity` exists for web games.

### 4.4 Media library (local)

- Local video/music scanners and caches (`media.local.common.model.{VideoInfo,MusicInfo,…}`,
  `*ScanCache`), sorters, search (`LocalMediaSearchActivity`), detail and multi-select activities.
- `component_impl.history.LocalVideoHistoryDatabase` and `media.local.*` Room databases.

### 4.5 Private vault / "hide"

- Move videos & photos into a passcode-protected **Private Space** (`PrivateVideoActivity`,
  4-digit passcode per strings), with import/export (`PrivateVideoImport/ExportActivity`),
  verify (`PrivateVideoVerifyActivity`), and a "PRIVATE SPACE" confirmation. Related DBs:
  `manager.ds_control.db.DsStatusDatabase`, `processes.alive_check.ProcessAliveDatabase`.

### 4.6 WhatsApp status saver

`ui.whatsapp.*` — save WhatsApp/WhatsApp-Business status images & videos
(`AppFilesSaverActivity`, `StatusSavedFragment`, `PreviewActivity`, `HowitWorkActivity`);
manifest `<queries>` explicitly lists `com.whatsapp`, `com.facebook.katana`, `com.facebook.orca`,
`com.instagram.android`, `com.twitter.android`.

### 4.7 Music

`recommend.music.*` (albums, singers, filters, discovery), local music player
(`com.nemo.local_music.impl.*`), lyrics search, equalizer, offline share, playlists, and the
**YouTube Music** tab. Media3/ExoPlayer and AppLovin's ExoPlayer fork are bundled.

### 4.8 Player

`media.player.activity.NewPlayerActivity`, `OnlinePlayerActivity`, and
`vplayer.watch.*` (`MainPlayer`, `MainVideoPlayerContainer`, `PopupVideoPlayerContainer`,
`ExpandableSurfaceView`). Supports **picture-in-picture / floating popup**
(`HostFloatingActivity`, `DiversionFloatLayerActivity`, `SYSTEM_ALERT_WINDOW`), media-button
control (`MainPlayer` handles `android.intent.action.MEDIA_BUTTON`), pre-roll ads
(`FrontAdExoPlayerWrapper`, `PreRollsAdVideoCompleteView`), and a large custom surface view.

### 4.9 App store / games / "diversion" growth features

- **9Apps / app store:** `appstore.model.*` (`AppDetails`, `HomeData`, `NineAppResponseEntity`),
  `ui.nineapp.*`, `apk-dym.hillo.app` / `apk-dym.9appsinstall.com`, `9appsappstore.com`.
- **Game center & web games:** `com.nemo.game_center.impl.*`, `gamehub.gogofun.games`.
- **Earn cash / wallet:** `module.money_interface.IEarnCashComponent`,
  `diversion.wallet.MyWalletActivity`, "Vidmate Cash", `main_earn_cash` tab, daily tasks
  (`com.nemo.daily_task.impl.*`), coins/rewards.
- **Cleaner/booster:** `com.nemo.clean.page.file.CleanFilesActivity`, `…speedup.SpeedUpActivity`.
- **Shorts:** `com.nemo.shorts.impl.entrance.ShortsActivity`.
- **WhatsApp-style diversion landing pages:** `ui.diversion.AlHiwarLandingActivity`,
  `diversion.wallet.*`.

### 4.10 Sharing

Share links (`sharevideo1/2/3.com`, `sharemusic1.com`, `sharetv1.com`, `sharemovie1.com`),
**offline/Bluetooth share** (`manager.share.BluetoothShareActivity`), social share
(`ShareSdkConfig.xml` with Facebook app id), and "Offline share with friends without data traffic".

### 4.11 Notifications / push / widgets

FCM (`VDFirebaseMessagingService`, `fcmForPlugin`), a custom push service
(`pushmsg.VidmateService`), `WorkManager` keep-alive (`PingWorker`, `FastIntervalWork`,
`PeriodicManager`), notification-bar update, and the three app widgets.

### 4.12 Updates

`com.nemo.vidmate.update.*` + `common.update.core.*` (`AppUpdateInfo`, `CheckParams`),
`UpdateControlActivity`, "Safe Update" UI, and native APK/resource patching. `REQUEST_INSTALL_PACKAGES`
allows in-app APK installs.

---

## 5. Logic deep dives

### 5.1 String obfuscation

Sensitive literals are XOR-obfuscated with a Base64 wrapper:

```java
// aasG.aaaQ.a(String)  ->  aaet.C4075a.a(str, "android.view")
// decode: base64_decode(s)  XOR  repeating key "android.view"  ->  UTF-8
```

The key is the constant `MBridgeConstans.APPLICATION_STACK_ANDROID_VIEW = "android.view"`.
~493 distinct strings are hidden this way (log tags, field names, host names, format strings).
This is weak protection — the key ships in the APK.

### 5.2 WebView "JS hook" / browser cloaking

`JsHookPkg`, `WebInterceptWrapper`, `ConfigDefaultDataCenter.getDefaultWebInterceptClassList()`,
and the host `C0.w` interceptor implement a scheme to make the WebView present itself as a
**mainstream browser**:

- `JsHookDefaultPkgList` lists ~100 browser packages (Chrome, Firefox, Opera, UC, Brave, Tor,
  Dolphin, Edge, Ecosia, Puffin, Yandex, …) used to pick a random/configured package name.
- At WebView-construction time `MyApplication.getPackageName()`/`getPackageManager()` return the
  substituted name, so the page's JS sees a real browser instead of VidMate.
- `getDefaultWebInterceptClassList()` hooks Chromium/WebView internals
  (`org.chromium.base.BuildInfo`, `android.webkit.WebViewCore.loadUrl`,
  `com.android.webview.chromium.WebViewChromiumFactoryProviderForR`, `BrowserStartupControllerImpl`).

Purpose: get desktop/mobile media pages to serve content the app can sniff, avoiding site
detection of the downloader.

### 5.3 Download task serialisation

`MTDownloadPart.aaae()` produces:
`"<curPos>[<start>-<end>+<type>+<url>+<path>]"` (or the short `curPos[start-end]` form), and
`MTDownloadPart.a(aabQ)` parses it back. `<type>` distinguishes a plain range part from a part
with an **alternate URL** (CDN fallback) and an **alternate file path**. This shows the engine
supports per-segment URL/path fail-over and resume — the state is persisted (Room + MMKV).

### 5.4 Ad mediation

`assets/ad_mediation_default_config.json` defines a mediation graph of **39 placements**
(splashscreen, native, interstitial, banner) each with a priority-ordered list of network
requests. Networks observed: **pangle (ByteDance), mintegral (MBridge), vungle, huawei (PPS),
adshark, flatads**, plus SDKs for applovin/MAX, inmobi, bigo, and others. Placement ids are
product-named: `splashscreen_ad`, `pre_rolls_ad`, `video_detail_page_ad`, `home_feed_page_ad`,
`download_video_ad`, `task_add_inters_normal_ad`, `nav_exit_inters_ad`, `exit_popup_ad`,
`search_feed_ad`, `search_banner_ad(320x100)`, `task_add_inters_adult_ad`, `nav_page_ad_adult`, …
Notably there are **separate "adult" placements**, and settings strings to
"Hide adult content in your feed / in search results / when viewing a channel or playlist"
plus "Hide adult websites in the app" — the config model carries an adult-content dimension.

### 5.5 Crash resilience / anti-fragility

`MyApplication.y` (`UncaughtExceptionHandler`) filters known noisy exceptions, tracks a `crashCount`
and deletes a state file after >3 crashes, and restarts `WelcomeActivity` on plugin
`ClassNotFoundException` (a plugin-reload recovery loop).

---

## 6. Network and endpoints (observed static references)

| Host | Use |
|---|---|
| `www.youtube.com`, `m.youtube.com`, `youtu.be`, `youtubei.googleapis.com` | YouTube extraction / InnerTube |
| `i.ytimg.com`, `i4.ytimg.com`, `lh3.googleusercontent.com` | Thumbnails |
| `redirector.googlevideo.com` | Media CDN |
| `img.vidmatefilm.org`, `feedback.vidmatefilm.org` | App content / feedback |
| `www.vidmateapp.com`, `vidmate.tv`, `vidmate.mobi` | Product/legal web |
| `apk-dym.hillo.app`, `apk-dym.9appsinstall.com`, `www.9appsappstore.com` | App-store installs |
| `gamehub.gogofun.games` | Web games |
| `vdm-sg.oss-ap-southeast-1.aliyuncs.com` | Aliyun OSS content |
| `static-res.playit2019.com`, `v.starhalo.mobi`, `getvdm1.com`, `sharevideo{1,2,3}.com`, `sharemusic1.com`, `sharetv1.com`, `sharemovie1.com` | Share / marketing |
| `play.google.com`, `accounts.google.com`, `googleapis` (GMS) | Google services |

`android:usesCleartextTraffic="true"` is set, and the plugin ships a network-security config
(`@xml/d`).

---

## 7. Security / privacy observations (static)

1. **Weak plugin "encryption":** single-byte XOR (`0xB7`) with the key printed in `assets/config.json`.
2. **Extensive dynamic code loading:** Shadow plugin, hot-fix DEX (`BaseDexHotFixProxy`), a
   second bundled DEX (`app_dex_10000020.dex`), and a hot-update jar — code can change after install.
3. **Broad permissions:** `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES`,
   `SYSTEM_ALERT_WINDOW`, `KILL_BACKGROUND_PROCESSES`, `GET_TASKS`/`REAL_GET_TASKS`,
   `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, launcher settings read/write, notification listeners.
4. **Package/PM spoofing at runtime** to disguise the WebView host (anti-detection).
5. **First-party identifiers in plaintext:** Facebook App id/secret (`ShareSdkConfig.xml`),
   Fabric `io.fabric.ApiKey`, and multiple ad-network app ids/keys as `<meta-data>`.
6. **Packer/obfuscation stack:** `libpglarmor.so`, `libse-engine.so`, `libnms.so` (likely a
   commercial packer/vmp) alongside the string obfuscation.
7. **Process daemon** (`com.daemon.service.DaemonService`) and keep-alive workers for persistence.
8. **Clipboard access** (`ClipboardDialogActivity`) — typical for link-detect paste UX.

*(Observations only. No claim of malicious behaviour is made; these are common patterns for
"free downloader + ad monetisation" apps.)*

---

## 8. Third-party components (from package inventory)

Media3/ExoPlayer (320+98+216 classes), AppLovin + `applovin.exoplayer2`, ByteDance Pangle
(`com.bytedance.sdk`, `com.bytedance.adsdk`, `com.bykv.vk`), Mintegral MBridge (`com.mbridge.msdk`),
Vungle, Huawei HMS/PPS Ads, `com.bidnex.sdk` (ad SDK), `com.pgl.ssdk` (packer/security),
`org.mozilla.javascript` (Rhino — JS engine for the extractor/JS bridge), Google GMS/Firebase
(analytics, messaging, crashlytics, remote config, datastore, room, work), Glide, Gson,
`me.jessyan.retrofiturlmanager`, `pub.devrel.easypermissions`, `com.github.moduth.blockcanary`,
Tencent Shadow + MMKV, ByteDance boost_multidex, Kotlin coroutines.

---

## 9. Evidence, method, and limitations

**Evidence chain (all local, reproducible):**

- REA `inspect-android-package` on host and plugin → full decoded manifests, SDKs, permissions,
  component inventories (saved: `artifacts/manifest-full.json`, `artifacts/plugin-manifest.json`).
- REA `search-android-classes` (empty query) → complete class inventories
  (`artifacts/classes.txt`, `artifacts/plugin-classes.txt`).
- JADX CLI decompilation → `decompiled/host` (2,502 files), `decompiled/plugin` (21,151 files).
- Apktool decode → `apktool/plugin` (4,901 resources; 1,334 layouts; 33 locale `strings.xml`).
- Custom obfuscation decoder (`decode_obf.py`) using key `android.view`.

**Limitations (must be read with every finding):**

- **Static only.** The APK was never executed; no runtime behaviour, network traffic, or dynamic
  class loading was observed.
- **Obfuscation.** Most app framework classes are renamed (`a`, `aa`, `aaa`, `aasG`, …). Named
  classes and strings are reliable; method-level logic in obfuscated packages is inferred from
  call sites, string literals, and known-library behaviour.
- The **extractor engine** (`com.heflash.feature.extractor.*`, 803 obfuscated classes) and the
  JS/WebView extraction flow were not fully deobfuscated; its exact per-site rules are not
  enumerated here.
- **Plugin vs host:** findings from `plugin.apk` apply to the app logic; `VidMate-6.0204.apk`
  (host) is only the loader. Signature verification was **not** performed by the tooling
  (`signature_verification: not_performed`); the signer identity comes from store metadata.
- JADX reported a small number of decompilation errors (36 files plugin, 4 host) — a handful of
  methods may be missing or approximate.
- Aptoide-sourced build: while MD5 matches the store listing, this is not an official-distributor
  verification.

---

## 10. One-paragraph summary

VidMate 6.0204 is a **Tencent Shadow plugin-host application**: the installed APK only loads a
34.6 MB plugin (`assets/4D6F6E204A`) that is trivially XOR-obfuscated (key `0xB7`) and contains the
real 21,218-class app. Its UI is a dynamic bottom-nav shell (Home / Downloader / Game Center /
YouTube Music / Me, with an "earn cash" variant) over 159 activities covering video & music
download, a tabbed WebView browser with media sniffing, a Media3/ExoPlayer-based player with
PiP and background playback, a local media library, a passcode **Private Vault**, a **WhatsApp
status saver**, YouTube account/subscription/history integration, and a heavy ad-monetisation and
growth layer (39 mediation placements across Pangle/Mintegral/Vungle/Huawei/etc., app-store,
games, cleaner, wallet). Its core download engine uses segmented range downloads with per-part
URL/path fail-over (`curPos[start-end+type+url+path]`), audio/video merge via the native
`libvidmatecodec.so`, plus torrent and SABR support. A hot-fix/dex-loading stack and a WebView
"JS-hook" that impersonates ~100 popular browsers let extraction logic change server-side and
evade site detection. It ships 83 languages and 107 regions.
