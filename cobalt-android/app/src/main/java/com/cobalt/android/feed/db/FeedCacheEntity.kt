package com.cobalt.android.feed.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cobalt.android.feed.model.FeedItem
import com.cobalt.android.feed.model.FeedSourceType

@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val viewCountText: String,
    val publishedText: String,
    val durationSeconds: Long,
    val watchUrl: String,
    val source: String,
    /** epoch millis; used to evict stale entries. */
    val cachedAt: Long
)

fun FeedItem.toCacheEntity(cachedAt: Long = System.currentTimeMillis()) = FeedCacheEntity(
    videoId = videoId,
    title = title,
    channelName = channelName,
    thumbnailUrl = thumbnailUrl,
    viewCountText = viewCountText,
    publishedText = publishedText,
    durationSeconds = durationSeconds,
    watchUrl = watchUrl,
    source = source.name,
    cachedAt = cachedAt
)

fun FeedCacheEntity.toFeedItem() = FeedItem(
    videoId = videoId,
    title = title,
    channelName = channelName,
    thumbnailUrl = thumbnailUrl,
    viewCountText = viewCountText,
    publishedText = publishedText,
    durationSeconds = durationSeconds,
    watchUrl = watchUrl,
    source = FeedSourceType.valueOf(source)
)
