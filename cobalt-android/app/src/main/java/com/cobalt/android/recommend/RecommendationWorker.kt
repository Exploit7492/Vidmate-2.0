package com.cobalt.android.recommend

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cobalt.android.util.SettingsRepository
import java.util.concurrent.TimeUnit

/**
 * Periodic "because you watched X" recommender (WorkManager, ~daily).
 *
 * The point of this worker over the old FCM push is that it needs no server and
 * no Firebase project: the taste profile is built and the recommendations are
 * resolved entirely on-device from the user's own likes/history, so there is
 * nothing to register and no push credential to provision. See
 * [RecommendationRepository].
 */
class RecommendationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext)
        if (!settings.recommendationsEnabled) return Result.success()
        return runCatching {
            RecommendationRepository(applicationContext).notifyIfAny()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val UNIQUE_NAME = "cobalt_recommendations"

        /** (Re)schedules the daily job; call whenever the setting toggles. */
        fun schedule(ctx: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<RecommendationWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(2, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
