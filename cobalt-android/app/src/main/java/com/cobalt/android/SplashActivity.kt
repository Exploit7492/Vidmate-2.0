package com.cobalt.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.ui.common.TypewriterText
import com.cobalt.android.ui.theme.CobaltTheme
import com.cobalt.android.util.SettingsRepository
import kotlinx.coroutines.delay

/**
 * Branded launch screen. The name types itself out (VidMate 2.0), then the
 * app routes to [OnboardingActivity] on first run or [MainActivity]
 * afterwards. The hold is derived from the typewriter's own duration plus a
 * short beat so the finished name is readable, rather than a fixed guess.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = getString(R.string.app_name)
        setContentView(
            ComposeView(this).apply {
                setContent { SplashScreen(name, onDone = ::route) }
            }
        )
    }

    private fun route() {
        val firstRun = !SettingsRepository(this).onboardingDone
        val next = if (firstRun) OnboardingActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    @Composable
    private fun SplashScreen(name: String, onDone: () -> Unit) {
        CobaltTheme {
            val perChar = 110L
            LaunchedEffect(Unit) {
                delay(name.length * perChar + HOLD_MS)
                onDone()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.vidmate_logo),
                    contentDescription = name,
                    modifier = Modifier.size(112.dp)
                )
                Spacer(Modifier.height(16.dp))
                TypewriterText(
                    text = name,
                    msPerChar = perChar,
                    color = MaterialTheme.colorScheme.primary,
                    caretColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.splash_tagline),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }

    private companion object {
        const val HOLD_MS = 450L
    }
}
