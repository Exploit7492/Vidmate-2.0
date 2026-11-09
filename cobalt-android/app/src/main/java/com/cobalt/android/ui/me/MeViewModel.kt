package com.cobalt.android.ui.me

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.cobalt.android.db.HistoryRepository
import com.cobalt.android.db.LikedRepository
import com.cobalt.android.download.DownloadRepository

/**
 * Backs the Me tab. VidMate's "Me" screen is a profile + settings hub; this
 * exposes the few counters the header shows (downloads / watched / liked)
 * and nothing else — the tab's rows are pure navigation.
 */
class MeViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadRepository = DownloadRepository(application)
    private val historyRepository = HistoryRepository(application)
    private val likedRepository = LikedRepository(application)

    val downloadCount: LiveData<Int> = downloadRepository.allDownloads.map { it.size }
    val historyCount: LiveData<Int> = historyRepository.allHistory.map { it.size }
    val likedCount: LiveData<Int> = likedRepository.allLiked.map { it.size }
}
