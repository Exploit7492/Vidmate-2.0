package com.cobalt.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cobalt.android.R

/**
 * The per-item download affordance: a bell that opens the quality-selection
 * flow for one piece of content. Used on Home tiles, search results and the
 * Shorts rail, so "get this" looks and behaves the same everywhere. Shows a
 * spinner in place of the bell while that item's link is being resolved.
 */
@Composable
fun DownloadBellIcon(
    isResolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .cobaltGlass(shape = CircleShape)
            .clickable(enabled = !isResolving, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = "download",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(0.dp).size(20.dp)
            )
        }
    }
}
