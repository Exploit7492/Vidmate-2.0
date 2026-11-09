package com.cobalt.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cobalt.android.databinding.ActivityOnboardingBinding
import com.cobalt.android.util.SettingsRepository

/**
 * First-run onboarding (Phase H2): a short, skippable coach sequence that
 * explains the three things a new user needs — paste a link, pick a format,
 * find downloads — before handing off to [MainActivity].
 *
 * Shown once; `SettingsRepository.firstLaunchDone` gates it. MainActivity keeps
 * its own first-launch permission/battery prompts, so this is purely the
 * "what is this app" intro, not a second permission surface.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private data class Page(val title: String, val body: String)

    private val pages = listOf(
        Page(
            "Download anything",
            "Copy a link from any app and paste it here, or share it straight to " +
                "Cobalt. YouTube, TikTok, Instagram, Facebook and hundreds more are " +
                "supported."
        ),
        Page(
            "Pick your format",
            "Choose video or audio, and the quality you want. Big files download in " +
                "parallel parts and resume if your connection drops."
        ),
        Page(
            "Find it later",
            "Everything you save lands in Library. Keep private clips in the " +
                "passcode-locked Vault, or play your music in the Music tab."
        )
    )

    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        render()
        binding.btnNext.setOnClickListener {
            if (index < pages.lastIndex) {
                index++
                render()
            } else {
                finishOnboarding()
            }
        }
        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun render() {
        val page = pages[index]
        binding.tvTitle.text = page.title
        binding.tvBody.text = page.body
        binding.tvStep.text = "${index + 1} / ${pages.size}"
        binding.btnNext.text = if (index == pages.lastIndex) "Start" else getString(R.string.onboarding_next)
    }

    private fun finishOnboarding() {
        SettingsRepository(this).firstLaunchDone = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
