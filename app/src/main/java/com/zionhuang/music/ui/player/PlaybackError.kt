package com.zionhuang.music.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import com.zionhuang.music.R

@Composable
fun PlaybackError(error: PlaybackException, retry: () -> Unit, openWebPlayer: (() -> Unit)? = null) {
    var showDetails by remember(error) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.native_playback_failed), style = MaterialTheme.typography.bodyMedium)
        openWebPlayer?.let { open ->
            Button(onClick = open) { Text(stringResource(R.string.web_playback_open)) }
        }
        Row {
            TextButton(onClick = retry) { Text(stringResource(R.string.playback_retry)) }
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(stringResource(R.string.playback_details))
            }
        }
        if (showDetails) {
            Text(
                text = error.cause?.cause?.message ?: stringResource(R.string.error_unknown),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()),
            )
        }
    }
}
