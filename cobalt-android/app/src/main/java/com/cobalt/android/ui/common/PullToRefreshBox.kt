package com.cobalt.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Pull-to-refresh wrapper for the Compose feeds. Material3 1.2.x drives this
 * from the gesture: when the user pulls far enough `state.isRefreshing` flips
 * true, we run [onRefresh], then stop the spinner. One implementation so every
 * feed (home, shorts, library) shows the same indicator and gesture.
 *
 * [refreshing] is the caller's own loading flag, used only to stop the spinner
 * early when the caller finishes (or fails) before the pull animation would.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableFeed(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(state.isRefreshing) {
            onRefresh()
            state.endRefresh()
        }
    }
    LaunchedEffect(refreshing) {
        if (!refreshing && state.isRefreshing) state.endRefresh()
    }
    Box(
        modifier
            .fillMaxSize()
            .nestedScroll(state.nestedScrollConnection)
    ) {
        content()
        PullToRefreshContainer(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
