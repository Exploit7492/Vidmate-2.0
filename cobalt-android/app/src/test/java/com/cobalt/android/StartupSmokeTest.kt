package com.cobalt.android

import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Launches the real activities on the JVM so a startup crash fails the build
 * instead of only showing up on a device. These exercise the true onCreate
 * path (theme, view binding, navigation graph, Compose hosts) rather than a
 * mock of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = CobaltApplication::class)
class StartupSmokeTest {

    @Test
    fun mainActivity_starts_withoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity.findViewById<android.view.View>(R.id.navHost) != null)
                assert(activity.findViewById<android.view.View>(R.id.navBottom) != null)
                assert(activity.findViewById<android.view.View>(R.id.shortsTab) != null)
                assert(activity.findViewById<android.view.View>(R.id.shortsFlare) != null)
                // No ActionBar => no static "VidMate 2.0" title strip.
                assert(activity.supportActionBar == null) {
                    "an ActionBar is present — the static title strip was not removed"
                }
            }
        }
    }

    @Test
    fun everyBottomTab_destination_loads_withoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nav = androidx.navigation.Navigation.findNavController(
                    activity, R.id.navHost
                )
                listOf(R.id.nav_home, R.id.nav_shorts, R.id.nav_settings,
                    R.id.nav_library, R.id.nav_browser).forEach { dest ->
                    nav.navigate(dest)
                    org.robolectric.shadows.ShadowLooper.idleMainLooper()
                }
            }
        }
    }

    @Test
    fun splashActivity_starts_withoutCrash() {
        ActivityScenario.launch(SplashActivity::class.java).use { }
    }
}
