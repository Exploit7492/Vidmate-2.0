package com.cobalt.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cobalt.android.databinding.ActivitySplashBinding
import com.cobalt.android.util.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Branded launch screen (Phase H1). Shows the brand for a brief moment while
 * the process warms up, then routes to [OnboardingActivity] on first run or
 * [MainActivity] afterwards. A splash this short is purely cosmetic — it is not
 * holding up real work — so it is a fixed, small delay rather than a fake
 * "loading" gate.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CoroutineScope(Dispatchers.Main).launch {
            delay(SPLASH_MS)
            val firstRun = !SettingsRepository(this@SplashActivity).onboardingDone
            val next = if (firstRun) OnboardingActivity::class.java else MainActivity::class.java
            startActivity(Intent(this@SplashActivity, next))
            finish()
        }
    }

    private companion object {
        const val SPLASH_MS = 600L
    }
}
