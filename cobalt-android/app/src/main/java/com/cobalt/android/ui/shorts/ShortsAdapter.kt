package com.cobalt.android.ui.shorts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cobalt.android.databinding.ItemShortVideoBinding
import com.cobalt.android.shorts.model.ShortItem

/**
 * One page per [ShortItem] inside the vertical `ViewPager2`. Video playback
 * itself is NOT owned per-item — a single shared `ExoPlayer` (owned by
 * `ShortsFragment`) is attached to whichever ViewHolder's `PlayerView` is
 * currently the active page, matching the standard TikTok/Shorts-style feed
 * pattern of one live player at a time rather than one per row.
 *
 * Phase 24: the per-item chrome (avatar, username, like/save/share, caption)
 * is a Compose surface ([ShortsChrome]) hosted in each ViewHolder's
 * `chromeCompose`, layered over the `PlayerView`. The player attachment in
 * `ShortsFragment.playAt()` is unchanged — it still finds this ViewHolder and
 * sets `binding.playerView.player`.
 */
class ShortsAdapter(
    private val onLikeClick: (ShortItem) -> Unit,
    private val onSaveClick: (ShortItem) -> Unit,
    private val onShareClick: (ShortItem) -> Unit
) : ListAdapter<ShortItem, ShortsAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemShortVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShortVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // ComposeView inside a RecyclerView item must dispose with the
        // item's view lifecycle, or a recycled row leaks its composition.
        binding.chromeCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.chromeCompose.setContent {
            ShortsChrome(
                item = item,
                onLikeClick = { onLikeClick(item) },
                onSaveClick = { onSaveClick(item) },
                onShareClick = { onShareClick(item) }
            )
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShortItem>() {
            override fun areItemsTheSame(oldItem: ShortItem, newItem: ShortItem) =
                oldItem.videoId == newItem.videoId

            override fun areContentsTheSame(oldItem: ShortItem, newItem: ShortItem) =
                oldItem == newItem
        }
    }
}
