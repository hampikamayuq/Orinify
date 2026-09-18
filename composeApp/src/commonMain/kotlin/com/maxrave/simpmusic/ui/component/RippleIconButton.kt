package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.pause
import simpmusic.composeapp.generated.resources.play

@Composable
fun RippleIconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    tint: Color = Color.White,
    // The icon is the button's ONLY content, so with no description TalkBack has nothing to read
    // and every one of these announces as a bare "Button" — the four on the Home top bar
    // (notifications, history, listen together, settings) were indistinguishable by ear.
    // Defaulted to null so the ~45 existing call sites keep compiling; each one names its own
    // action as it is revisited, and a decorative icon beside its own label legitimately stays null.
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector,
            contentDescription,
            tint = tint,
            modifier = if (fillMaxSize) Modifier.fillMaxSize().padding(4.dp) else Modifier,
        )
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    // Same reason as above, and this one changes meaning with its state: the label is what the tap
    // will DO ("Play" / "Pause"), which an icon swap cannot announce. Null resolves to that pair
    // here, so a caller that passes nothing still gets a state-aware name instead of a bare "Button".
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    RippleIconButton(
        if (!isPlaying) {
            SimpIcons.PlayArrow
        } else {
            SimpIcons.Pause
        },
        modifier = modifier,
        tint = tint,
        contentDescription = contentDescription
            ?: stringResource(if (isPlaying) Res.string.pause else Res.string.play),
        onClick = onClick,
    )
}
