plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.cobalt.android"
    compileSdk = 35

    defaultConfig {
        // "VidMate 2.0" rebrand: a distinct id so it installs side-by-side
        // with the original com.nemo.vidmate and with Cobalt itself.
        applicationId = "com.vidmate2.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug { isDebuggable = true }
        release {
            isMinifyEnabled = false
            // proguardFiles not needed while minification is disabled
        }
    }

    // Per-ABI APKs (+ a universal one). The bundled native codecs and the
    // torrent library are ~250 MB across all four ABIs, so a device only ever
    // needs its own slice; this is what Play's ABI splits do anyway.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        // Phase 22: Compose migration foundation. viewBinding stays on
        // (deliberately not a switch) — every existing Fragment/XML screen
        // keeps working exactly as-is; Compose screens land one at a time
        // via ComposeView embedded in a Fragment's existing onCreateView,
        // the standard incremental-adoption path, not a big-bang rewrite.
        // See ARCHITECTURE.md Phase 22 for the reasoning and the rest of
        // the migration plan.
        compose = true
        // D5: generate the IDownloadService Stub/Stub.Proxy from the .aidl.
        aidl = true
    }
    composeOptions {
        // Kotlin is on 1.9.24 (bumped from 1.9.23 by this same phase,
        // specifically to land on a version compose-compiler has a
        // confirmed-compatible release for — see the version note below).
        // This project is pre-Kotlin-2.0, so Compose uses the classic
        // kotlinCompilerExtensionVersion mechanism, NOT the newer
        // org.jetbrains.kotlin.plugin.compose Gradle plugin (that plugin
        // requires Kotlin 2.0+; using it here would silently target the
        // wrong Kotlin entirely). 1.5.14 is confirmed, not guessed: read
        // directly off developer.android.com/jetpack/androidx/releases/
        // compose-compiler, which states "This compiler release is
        // targeting Kotlin 1.9.24" for exactly this version. Given this
        // project's own ffmpeg-kit history of shipped-then-broken guessed
        // versions, every version in this block was checked against a
        // primary source before being pinned — see ARCHITECTURE.md Phase
        // 22 for the full verification trail (Compose BOM 2024.06.00 and
        // Coil 2.6.0 below were checked the same way, both era-matched to
        // this same Kotlin 1.9.24 / May–June 2024 release window).
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")

    // --- QR share (VidMate's "share by QR code") ---
    // ZXing core generates the bitmap; no Play-services barcode dependency
    // (scanning is not a feature — only generating a code to hand to another
    // device's camera).
    implementation("com.google.zxing:core:3.5.3")

    // --- Phase 2 (Shorts feed) additions ---
    // ViewPager2 backs the vertical swipeable Shorts feed. This was already
    // used in fragment_shorts.xml before this phase but the dependency itself
    // was missing, so the module did not actually compile.
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    // Media3/ExoPlayer plays the real per-item video streams (progressive,
    // HLS and DASH — Innertube/Invidious return a mix of these) in the
    // TikTok/Shorts-style feed. VideoView (previously in item_short_video.xml)
    // cannot handle HLS/DASH so it's replaced by androidx.media3.ui.PlayerView.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    // MediaSession routes headset/Bluetooth transport buttons and the system
    // media notification to the player (Phase C6).
    implementation("androidx.media3:media3-session:1.4.1")
    // NewPipeExtractor is one of the three merged Shorts sources (the other
    // two — direct Innertube calls and Invidious public instances — use
    // OkHttp directly and need no extra dependency). NewPipeExtractor needs a
    // JSON parser (nanojson) and a JS engine for YouTube's cipher (Rhino) as
    // transitive dependencies, both pulled automatically from its POM.
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.6")

    // --- Gap-closure D2 (BitTorrent / magnet downloads) ---
    // libtorrent4j: the maintained JNI binding to libtorrent. The core jar
    // carries the Java API; the four `-android-*` jars each carry one ABI's
    // native `libtorrent4j.so`. All are plain JARs (no AAR), so AGP merges the
    // per-ABI `lib/<abi>/*.so` entries into the APK the same way it does for
    // any jniLibs. Verified on Maven Central (2.1.0-35) before pinning.
    implementation("org.libtorrent4j:libtorrent4j:2.1.0-35")
    implementation("org.libtorrent4j:libtorrent4j-android-arm64:2.1.0-35")
    implementation("org.libtorrent4j:libtorrent4j-android-arm:2.1.0-35")
    implementation("org.libtorrent4j:libtorrent4j-android-x86:2.1.0-35")
    implementation("org.libtorrent4j:libtorrent4j-android-x86_64:2.1.0-35")

    // --- Phase 15 (FFmpeg-based dynamic quality/format transcoding) ---
    // `full-gpl` (not `full` or `min-gpl`) is the only prebuilt ffmpeg-kit
    // variant that bundles x264/x265 (H.264/H.265 encode — needed for
    // TranscodeProfile.ALL_VIDEO's MP4 tiers) alongside lame/opus/vorbis/
    // flac (all already free of GPL entanglement on their own, bundled
    // here anyway since full-gpl is a superset). Picking a smaller variant
    // would silently drop the H.264 video tiers, which are the ones most
    // players actually support.
    //
    // Dependency history (two real breaks, two different causes — see
    // FfmpegTranscoder's DEPENDENCY_NOTE KDoc for the full incident
    // writeup before changing this line):
    // 1. `com.arthenica:ffmpeg-kit-full-gpl:6.0-2` (the original
    //    coordinate) is DEAD — arthenica retired FFmpegKit and Maven
    //    Central removed all `com.arthenica:*` binaries on 2025-04-01.
    // 2. `com.antonkarpenko:ffmpeg-kit-full-gpl` (the first replacement)
    //    resolved fine but broke the build a different way — CI failed
    //    with `Unresolved reference: arthenica` on every FFmpegKit class.
    //    Root cause: that coordinate is a Flutter plugin build
    //    (github.com/sk3llo/ffmpeg-kit-flutter), not a plain Android
    //    library — it doesn't expose the public Java API this app calls,
    //    regardless of a same-named class appearing in one of its crash
    //    logs (see DEPENDENCY_NOTE for why that "verification" didn't
    //    actually verify the right thing).
    //
    // Current: `io.github.jamaismagic.ffmpeg:ffmpeg-kit-lts-full-gpl-16kb`
    // — JamaisMagic/ffmpeg-kit-16KB, a genuine from-source fork of
    // arthenica/ffmpeg-kit (not a Flutter wrapper), rebuilt for Android's
    // 16KB page-size requirement. Its android/README.md is the unmodified
    // original arthenica Android docs — confirmed real drop-in, zero
    // import changes needed. Version `6.1.4` confirmed directly off this
    // exact artifact's own mvnrepository.com page (its only published
    // version) — an earlier guess of `6.1.7`, inferred from a sibling
    // artifact in the same group instead, was wrong; see
    // DEPENDENCY_NOTE's "VERSION NOTE — CORRECTED" for the full story and
    // what to do if a future version bump is needed.
    implementation("io.github.jamaismagic.ffmpeg:ffmpeg-kit-lts-full-gpl-16kb:6.1.4")
    // fallback serving leftover artifacts from a previous, different
    // dependency declaration, not evidence this pin is actually valid.
    // Always verify the exact version against Maven Central's own
    // index, not against what a CI log's task list appears to reach.

    // --- Phase 22 (Compose migration foundation) ---
    // BOM pinned to 2024.06.00, NOT the latest available — deliberately
    // era-matched to the Kotlin 1.9.24 / compose-compiler 1.5.14 pin
    // above (BOM 2024.06.00 published June 12 2024, right after Kotlin
    // 1.9.24's May 2024 release). A newer BOM pulls Compose runtime
    // artifacts built against newer Compose-compiler feature sets than
    // 1.5.14 actually supports — confirmed off mvnrepository.com's own
    // BOM release date, not assumed from "latest is always fine."
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    // Lets Compose read this project's existing LiveData ViewModels directly
    // via observeAsState() — the mechanism Phase 23+ Compose screens use to
    // reuse the already-tested ViewModels instead of rewriting each one
    // before its screen can migrate.
    implementation("androidx.compose.runtime:runtime-livedata")
    // Lets a ComposeView embedded in a classic Fragment call
    // dispose-on-viewtree-lifecycle-destroyed correctly, and lets
    // Composables collect existing LiveData/StateFlow from this
    // project's existing ViewModels directly (observeAsState /
    // collectAsStateWithLifecycle) instead of needing every ViewModel
    // rewritten before a single screen can move to Compose.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    // Real remote thumbnail/avatar loading — this project had NO image-
    // loading library at all before this phase (confirmed by grepping
    // this file for "Coil"/"Glide" before adding this: zero hits), which
    // is part of why Home never had a real YouTube-style thumbnail feed
    // — there was no way to load one efficiently. Coil 2.x (not 3.x/
    // coil3, which relocated groupId to io.coil-kt.coil3 for a
    // Compose-Multiplatform-first architecture this Android-only project
    // doesn't need) chosen for the same era-matching reason as the BOM
    // above — 2.6.0 published Feb 2024, squarely compatible with this
    // Kotlin/Compose pin, and coil-compose already depends on OkHttp by
    // default, which this project already uses everywhere else.
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.fragment:fragment-testing:1.6.2")
    // Real org.json implementation for JVM unit tests: Android's bundled
    // org.json stubs throw "not mocked" off-device, and the feed parser
    // tests exercise real JSON parsing (not mocks — see the project's
    // no-mocks-in-tests rule). This dependency is test-only; the app itself
    // still uses the platform's org.json at runtime.
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
