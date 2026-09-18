package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.PlatformCastButton
import com.maxrave.simpmusic.expect.ui.isPlatformCastAvailable
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.heartBurst
import com.maxrave.simpmusic.ui.component.rememberHeartBurstState
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.AddCircleOutline
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.FastForward
import com.maxrave.simpmusic.ui.icon.FastRewind
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Star
import com.maxrave.simpmusic.ui.icon.StarBorder
import com.maxrave.simpmusic.ui.icon.VolumeDown
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cast
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.more
import simpmusic.composeapp.generated.resources.next
import simpmusic.composeapp.generated.resources.no_lyrics_for_track
import simpmusic.composeapp.generated.resources.pause
import simpmusic.composeapp.generated.resources.play
import simpmusic.composeapp.generated.resources.previous
import simpmusic.composeapp.generated.resources.queue
import simpmusic.composeapp.generated.resources.repeat_all
import simpmusic.composeapp.generated.resources.repeat_off
import simpmusic.composeapp.generated.resources.repeat_one
import simpmusic.composeapp.generated.resources.seek_bar
import simpmusic.composeapp.generated.resources.shuffle
import simpmusic.composeapp.generated.resources.unfavorite
import simpmusic.composeapp.generated.resources.volume
import simpmusic.composeapp.generated.resources.youtube_liked_music
import kotlin.math.roundToLong

/** Which body the dock is currently showing. Held by the top-level Apple Music composable. */
internal enum class AppleMusicView { MAIN, LYRICS, QUEUE }

/**
 * The page gradient's colour at [fraction] of the screen height: the seed barely darkened at the
 * top, ~32%-darkened by mid-page (48%), ~78%-darkened at the bottom. Used to BUILD the gradient
 * and to land the artwork fade on the gradient's own colour at the exact Y where it ends.
 */
internal fun appleMusicGradientColorAt(
    seedColor: Color,
    fraction: Float,
): Color {
    // Every stop is the SEED darkened, never a fixed near-black: the old bottom stop was a
    // hardcoded warm black, so on the artwork view — where only the lower half of the gradient
    // is visible under the artwork — the page read as plain black instead of tinted.
    val top = lerp(seedColor, Color.Black, 0.05f)
    val mid = lerp(seedColor, Color.Black, 0.32f)
    val bottom = lerp(seedColor, Color.Black, 0.78f)
    return if (fraction <= 0.48f) {
        lerp(top, mid, (fraction / 0.48f).coerceIn(0f, 1f))
    } else {
        lerp(mid, bottom, ((fraction - 0.48f) / 0.52f).coerceIn(0f, 1f))
    }
}

internal val AppleMusicTextSecondary = Color.White.copy(alpha = 0.72f)
internal val AppleMusicPillInactive = Color.White.copy(alpha = 0.24f)
internal val AppleMusicTrackInactive = Color.White.copy(alpha = 0.26f)
internal val AppleMusicTrackActive = Color.White.copy(alpha = 0.92f)

@Immutable
internal data class AppleMusicTypography(
    val mainTitle: TextStyle,
    val mainArtist: TextStyle,
    val compactTitle: TextStyle,
    val compactArtist: TextStyle,
    val queueSectionHeader: TextStyle,
    val queueSectionSubtitle: TextStyle,
    val times: TextStyle,
    val badge: TextStyle,
    val footer: TextStyle,
    val idleLyric: TextStyle,
    val idleTranslated: TextStyle,
)

/**
 * Maps every Apple Music text slot 1:1 onto the [typo] roles the OTHER styles use for the same
 * element — no custom sizes, no custom scaling (owner's rule: this style types like the rest of
 * the app). Precedents: M3E's track title/artist row (titleMedium/bodyMedium), LyricsView's
 * in-player line (headlineMedium), SongFullWidthItems rows (titleSmall/bodySmall), the queue
 * sheet's section headers (titleMedium) and M3E's canvas overlay lines (bodyMedium white/yellow).
 */
@Composable
internal fun rememberAppleMusicTypography(): AppleMusicTypography {
    val t = typo()
    return AppleMusicTypography(
        mainTitle = t.titleMedium,
        mainArtist = t.bodyMedium,
        // The compact header — Lyrics, Queue, and the row under a running canvas — carries the
        // SIZES FullscreenLyricsSheet uses for the same job (LyricsView.kt: labelSmall over
        // bodySmall), while mainTitle/mainArtist stay 18/13 because they head the controller
        // layout. Borrow the size from labelSmall but keep the titleMedium ROLE: typo() bakes a
        // color into every role — title* carry titleColor, body*/label* carry bodyColor — so
        // switching the title to labelSmall outright would also switch it to the subtitle's grey.
        // The artist was already a body role, so bodySmall changes its size and nothing else.
        compactTitle = t.titleMedium.copy(fontSize = t.labelSmall.fontSize),
        compactArtist = t.bodySmall,
        queueSectionHeader = t.titleMedium,
        queueSectionSubtitle = t.bodySmall,
        times = t.bodyMedium,
        badge = t.bodySmall,
        footer = t.bodySmall,
        idleLyric = t.bodyMedium.copy(color = Color.White),
        // The accent, not yellow: the only fixed colour this artwork-tinted page carries is the
        // fork's own, and a second one has nothing to belong to.
        idleTranslated = t.bodyMedium.copy(color = seed),
    )
}

/**
 * True alpha fade at the top/bottom edges of a scrolling region (DstIn mask): content dissolves
 * into whatever is behind it — the missing "scrim" on the lyrics list's hard-clipped edges —
 * without painting a color and without touching the wrapped component.
 */
internal fun Modifier.appleMusicVerticalFadeEdges(
    topFade: Dp,
    bottomFade: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = topFade.toPx().coerceAtMost(size.height / 2f)
            val bottomPx = bottomFade.toPx().coerceAtMost(size.height / 2f)
            val topStop = if (size.height > 0f) topPx / size.height else 0f
            val bottomStop = if (size.height > 0f) 1f - bottomPx / size.height else 1f
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        topStop to Color.Black,
                        bottomStop to Color.Black,
                        1f to Color.Transparent,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

/**
 * Press-to-swell ("phồng to ra") like the liquid-glass buttons: the control springs up while a
 * finger is on it and settles back on release.
 *
 * Detection is a plain awaitEachGesture down/up watch, NOT GlassInteraction's drag inspector —
 * that one only starts its animation from a DRAG start, so a normal tap on these buttons never
 * animated anything. `requireUnconsumed = false` keeps it working under the wrapped `clickable`,
 * and nothing here consumes events, so clicks and slider drags still land.
 */
@Composable
internal fun Modifier.appleMusicPressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "appleMusicPressInflate",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * One glyph button with a tight circular ripple — [IconButton] sized and clipped exactly
 * like Classic/M3E's plain icon buttons (Info, PlaylistAdd, Queue, Replay5…), not a custom
 * Box+clickable and not an unconstrained [com.maxrave.simpmusic.ui.component.RippleIconButton].
 * [size] is the touch target, [iconSize] the glyph: the glyph stays 24dp while the target meets 48.
 * [isSelected] is announced only when non-null — a plain action button has no on/off to report.
 */
@Composable
internal fun AppleMusicGlyphButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = Color.White,
    isSelected: Boolean? = null,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .appleMusicPressInflate()
                .size(size)
                .clip(CircleShape)
                .semantics { if (isSelected != null) selected = isSelected },
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/**
 * The ⊕ (YouTube-liked) / ☆ (favourite, with the heart-burst) / ⋯ (more sheet) trio — shared by
 * the MAIN title row and the LYRICS/QUEUE compact header. Fires the heart-burst on the tap that
 * likes, never on state, matching every other style's like button.
 */
@Composable
internal fun AppleMusicHeaderActions(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
) {
    // Every target is 48dp with its glyph centred, so the gap between targets is zero: the
    // glyph-to-glyph spacing is already carried by the 8-13dp of padding inside each one. Callers
    // end the row 8dp from the edge instead of 20 so the ⋯ glyph lands where it always did.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isUserLoggedIn) {
            // 48dp target around a 22dp glyph: a bare 22dp clickable is under half the Material
            // minimum, on the row that gets tapped most.
            val liked = state.likeStatus
            Box(
                modifier =
                    Modifier
                        .appleMusicPressInflate()
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { actions.onAddToYouTubeLiked() }
                        .semantics { selected = liked },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = liked, label = "appleMusicYtLiked") { isLiked ->
                    Icon(
                        imageVector = if (isLiked) SimpIcons.CheckCircle else SimpIcons.AddCircleOutline,
                        contentDescription = stringResource(Res.string.youtube_liked_music),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        val likeBurst = rememberHeartBurstState()
        val isFavorite = state.controllerState.isLiked
        Box(
            modifier =
                Modifier
                    .appleMusicPressInflate()
                    .size(48.dp)
                    .heartBurst(likeBurst)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) {
                        if (!isFavorite) likeBurst.fire()
                        actions.onUIEvent(UIEvent.ToggleLike)
                    }.semantics { selected = isFavorite },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = isFavorite, label = "appleMusicFavorite") { liked ->
                Icon(
                    imageVector = if (liked) SimpIcons.Star else SimpIcons.StarBorder,
                    // The action, not the state: "Remove from favorites" is what the tap does.
                    contentDescription = stringResource(if (liked) Res.string.unfavorite else Res.string.favorite),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        AppleMusicGlyphButton(
            icon = SimpIcons.MoreVert,
            contentDescription = stringResource(Res.string.more),
            onClick = { actions.onShowMoreSheet() },
        )
    }
}

/**
 * 56dp rounded thumbnail + title/artist column + [AppleMusicHeaderActions], used where the
 * artwork isn't on screen (LYRICS and QUEUE bodies).
 */
@Composable
internal fun AppleMusicCompactHeader(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
) {
    Row(
        // end = 8: the trailing 48dp ⋯ target carries 12dp of its own, so its glyph still sits
        // 20dp from the edge, level with the thumbnail on the left.
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(state.screenData.thumbnailURL)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(state.screenData.thumbnailURL)
                    .crossfade(300)
                    .build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = null,
            modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Ellipsis, not marquee: a marquee in this narrow header scrolls constantly and
            // snapshots as garbage ("Vill Be Okay … Eve" in the first device screenshots).
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typography.compactTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.screenData.isExplicit) {
                    ExplicitBadge(modifier = Modifier.size(20.dp).padding(end = 4.dp))
                }
                Text(
                    text = state.screenData.artistName,
                    style = typography.compactArtist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Padding INSIDE the clickable, so the tap target is the line plus 10dp below
                    // (~30dp) rather than the bare glyph height; the old 2dp spacer is folded into
                    // the top padding. A full 48dp line would push the column past the 55dp
                    // thumbnail and grow the header.
                    modifier =
                        Modifier
                            .clickable { actions.onNavigateToArtist() }
                            .padding(top = 2.dp, bottom = 10.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AppleMusicHeaderActions(state = state, actions = actions)
    }
}

/** 7dp track shared by the progress bar and the volume row — same visual language, only the color and callbacks differ. */
@Composable
internal fun AppleMusicThinSlider(
    value: Float,
    activeColor: Color,
    label: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    // The slider's "phồng" is a track that thickens while touched/dragged — same spring feel as
    // the button inflate, expressed the way a bar can.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val trackHeight by animateDpAsState(
        targetValue = if (pressed || dragged) 14.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "appleMusicSliderInflate",
    )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            // No thumb and no label anywhere near it, so the bar has nothing to be read as.
            modifier = modifier.semantics { contentDescription = label },
            interactionSource = interactionSource,
            track = {
                // Hand-drawn instead of SliderDefaults.Track. M3 rounds each half of the track with
                // TWO different radii: the OUTER end with trackCornerSize (height/2 — fully round)
                // and the INNER end, where the two halves meet, with trackInsideCornerSize (2dp —
                // reads as square). At value 0 the inactive half owns the entire bar, so its square
                // inner end lands on the LEFT while its round outer end sits on the right — the
                // mismatched bar. Clipping ONE container and drawing the played portion inside it
                // keeps both ends of the bar equally round, and gives the played portion the flat
                // right edge Apple's own bar has.
                val fraction = value.coerceIn(0f, 1f)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(AppleMusicTrackInactive),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(activeColor),
                    )
                }
            },
            thumb = {
                // No thumb — the approved mock and Apple's own bars are track-only; the
                // active/inactive split marks the position (same call the MiniPlayer makes).
                Spacer(Modifier.size(0.dp))
            },
        )
    }
}

/** Elapsed time, the codec badge (hidden when unknown), and the remaining time as "-m:ss". */
@Composable
internal fun AppleMusicTimesRow(
    state: NowPlayingContentState,
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
) {
    // `total` stays -1 until the player reports a duration, and SimpleMediaState.Ready carries
    // ONLY the duration — never a position. After a queue restore nothing is playing, and the
    // position poll only runs while isPlaying, so no later event arrives to correct it. Deriving
    // elapsed from total therefore zeroed BOTH numbers at once, which is why a restored queue read
    // 00:00 / -00:00: the played time was never actually unknown, TimeLine.current held it.
    val knownTotal = state.timelineState.total.takeIf { it > 0L }
    val elapsedMs =
        if (knownTotal != null) {
            // Still derived from the slider while the duration IS known, so this number tracks the
            // finger while scrubbing instead of waiting for the player to report the seek back.
            (knownTotal * (state.sliderValue / 100f)).roundToLong()
        } else {
            state.timelineState.current.coerceAtLeast(0L)
        }
    // Clamp BEFORE formatDuration — it renders any negative as "NA:NA", and the remaining time
    // must never show that at the end of a track whose length IS known. An UNKNOWN length is
    // exactly what that string is for, so null deliberately takes the negative path below.
    val remainingMs = knownTotal?.let { (it - elapsedMs).coerceAtLeast(0L) }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatDuration(elapsedMs),
            style = typography.times,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Left,
        )
        // ONE slot holding two alternatives — and both of them stay composed, swapping on alpha
        // rather than on presence. AnimatedVisibility removes its content from the LAYOUT, so this
        // Box took the height of whichever state was up: the codec pill is a 15dp icon wrapped in
        // 4dp of vertical padding, "Crossfading" is bare text at the times style, and with NEITHER
        // showing (no codec, not crossfading) the Box collapsed to nothing at all. Every swap
        // therefore resized this row and shoved the whole transport below it up or down. Holding
        // both means the slot is always as tall as the tallest one, at any type scale, with no
        // measured constant to keep in sync. The cross-fade looks identical — alpha is what
        // fadeIn/fadeOut animated anyway.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            // Sweep head for the "Crossfading" shimmer, 0..1. Runs UNCONDITIONALLY — put behind the
            // crossfade check it would restart from zero every time the label appears, which is the
            // same reason the other two styles declare it outside their own visibility gate.
            val sweepTransition = rememberInfiniteTransition(label = "appleMusicCrossfadeSweep")
            val crossfadeSweep by sweepTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(3200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "appleMusicSweepHead",
            )
            val codec = state.audioCodecLabel
            val crossfadeLabelAlpha by animateFloatAsState(
                targetValue = if (state.timelineState.isCrossfading) 1f else 0f,
                label = "appleMusicCrossfadeLabelAlpha",
            )
            val codecBadgeAlpha by animateFloatAsState(
                targetValue = if (!state.timelineState.isCrossfading && codec != null) 1f else 0f,
                label = "appleMusicCodecBadgeAlpha",
            )
            Box(modifier = Modifier.alpha(crossfadeLabelAlpha)) {
                // Identical treatment to Classic and M3 Expressive: a highlight swept through the
                // glyphs with a text brush — no overlay, no clipping.
                val shimmerSpan = 140f
                val shimmerHead = crossfadeSweep * (shimmerSpan * 3f) - shimmerSpan
                val labelColor = typography.times.color
                Text(
                    text = stringResource(Res.string.crossfading),
                    style =
                        typography.times.copy(
                            brush =
                                Brush.horizontalGradient(
                                    0f to labelColor.copy(alpha = 0.45f),
                                    // The sweep head is PURE white, not the resting label colour — that
                                    // colour is an adaptive grey, and a grey gleam reads as no gleam.
                                    0.5f to Color.White,
                                    1f to labelColor.copy(alpha = 0.45f),
                                    startX = shimmerHead,
                                    endX = shimmerHead + shimmerSpan,
                                    tileMode = TileMode.Clamp,
                                ),
                        ),
                    textAlign = TextAlign.Center,
                )
            }
            // A PILL, like the mock's badge (and Apple's "Lossless"): translucent rounded
            // background, not bare text floating between the two times. It is the TALLER of the two
            // states, so it is what the slot's height ends up being — see the note above.
            Box(modifier = Modifier.alpha(codecBadgeAlpha)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = SimpIcons.GraphicEq,
                        // Decorative: the codec text beside it is the content.
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // orEmpty(), not codec!!: the null check drives the alpha above rather than
                    // guarding this branch, so there is nothing here for the compiler to
                    // smart-cast. It also renders while alpha is 0, which is the point.
                    Text(text = codec.orEmpty(), style = typography.badge.copy(color = Color.White.copy(alpha = 0.9f)))
                }
            }
        }
        Text(
            // No leading "-" when the length is unknown: "-NA:NA" reads as a negative amount of
            // nothing. formatDuration's own out-of-range string is the app's established way to
            // say "no value here".
            text = remainingMs?.let { "-" + formatDuration(it) } ?: formatDuration(-1L),
            style = typography.times,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Right,
        )
    }
}

/** FastRewind(44dp) → Previous, Play/Pause(62dp, plain white — no container disc), FastForward(44dp) → Next. */
@Composable
internal fun AppleMusicTransportRow(
    controllerState: ControlState,
    onUIEvent: (UIEvent) -> Unit,
    modifier: Modifier = Modifier,
    // Only the fullscreen lyrics page asks for these; the Now Playing page keeps its three buttons.
    showShuffleAndRepeat: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Mock: a tight centered cluster with a 58dp gap — NOT SpaceEvenly, which spreads the
        // rewind/forward glyphs to the screen edges (first device screenshots). With shuffle and
        // repeat on the two ends the five span the row instead, the way Apple's desktop player
        // lays them out — five buttons at a 58dp gap no longer fit beside the artwork.
        horizontalArrangement =
            if (showShuffleAndRepeat) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showShuffleAndRepeat) {
            AppleMusicGlyphButton(
                icon = SimpIcons.Shuffle,
                contentDescription = stringResource(Res.string.shuffle),
                isSelected = controllerState.isShuffle,
                onClick = { onUIEvent(UIEvent.Shuffle) },
                size = 40.dp,
                tint = Color.White.copy(alpha = if (controllerState.isShuffle) 1f else 0.4f),
            )
        }
        IconButton(
            onClick = { if (controllerState.isPreviousAvailable) onUIEvent(UIEvent.Previous) },
            modifier = Modifier.appleMusicPressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = SimpIcons.FastRewind,
                contentDescription = stringResource(Res.string.previous),
                tint = Color.White.copy(alpha = if (controllerState.isPreviousAvailable) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
        // No loading state on this button. It is play/pause and nothing else: it stays pressable
        // and keeps showing the transport glyph even while the player is buffering, so the control
        // never disappears out from under a finger reaching for it.
        Box(
            modifier =
                Modifier
                    .appleMusicPressInflate()
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = controllerState.isPlaying, label = "appleMusicPlayPauseIcon") { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                    tint = Color.White,
                    modifier = Modifier.size(66.dp),
                )
            }
        }
        IconButton(
            onClick = { if (controllerState.isNextAvailable) onUIEvent(UIEvent.Next) },
            modifier = Modifier.appleMusicPressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = SimpIcons.FastForward,
                contentDescription = stringResource(Res.string.next),
                tint = Color.White.copy(alpha = if (controllerState.isNextAvailable) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
        if (showShuffleAndRepeat) {
            val repeatState = controllerState.repeatState
            AppleMusicGlyphButton(
                icon = if (repeatState is RepeatState.One) SimpIcons.RepeatOne else SimpIcons.Repeat,
                contentDescription = stringResource(repeatState.repeatLabelRes()),
                isSelected = repeatState !is RepeatState.None,
                onClick = { onUIEvent(UIEvent.Repeat) },
                size = 40.dp,
                tint = Color.White.copy(alpha = if (repeatState !is RepeatState.None) 1f else 0.4f),
            )
        }
    }
}

/** The TalkBack label for a repeat state — the state, since the glyph is the same for off and all. */
internal fun RepeatState.repeatLabelRes() =
    when (this) {
        RepeatState.None -> Res.string.repeat_off
        RepeatState.All -> Res.string.repeat_all
        RepeatState.One -> Res.string.repeat_one
    }

@Composable
internal fun AppleMusicVolumeRow(
    controller: DeviceVolumeController,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Both glyphs decorative: they are not buttons, and the slider between them carries the
        // one "Volume" label — labelling all three reads it out three times.
        Icon(
            imageVector = SimpIcons.VolumeDown,
            contentDescription = null,
            tint = AppleMusicTextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        // 24dp hit height around the 7dp track; the row's own icons stay 18.
        Box(modifier = Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
            AppleMusicThinSlider(
                value = controller.volumeFraction,
                activeColor = AppleMusicTrackActive,
                label = stringResource(Res.string.volume),
                onValueChange = { controller.setVolumeFraction(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = SimpIcons.VolumeUp,
            contentDescription = null,
            tint = AppleMusicTextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * One dock button: a 48dp target around the 40dp circle that lights behind a DARK glyph while
 * [active] (white-on-light was unreadable). The circle is the visual, the outer box the target,
 * so the dock reads exactly as it did at 40.
 */
@Composable
internal fun AppleMusicDockButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stateLabel: String? = null,
) {
    Box(
        modifier =
            modifier
                .appleMusicPressInflate()
                .size(48.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics {
                    selected = active
                    stateLabel?.let { stateDescription = it }
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (active) activeColor else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint =
                    when {
                        !enabled -> Color.White.copy(alpha = 0.4f)
                        active -> activeContentColor
                        else -> Color.White.copy(alpha = 0.85f)
                    },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Lyrics · Cast · Queue. The Cast slot renders [PlatformCastButton] itself (which hides when
 * Cast is unavailable) and takes no "active" tint of its own — same rule M3E's connected group
 * follows for its Cast slot.
 */
@Composable
internal fun AppleMusicDock(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    castState: GenericCastState,
    lyricsAvailable: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    modifier: Modifier = Modifier,
) {
    val castLabel = stringResource(Res.string.cast)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Re-tapping the active tab returns to MAIN — the dock is a toggle, not one-way nav.
        AppleMusicDockButton(
            icon = SimpIcons.Lyrics,
            contentDescription = stringResource(Res.string.lyrics),
            active = viewState == AppleMusicView.LYRICS,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            enabled = lyricsAvailable,
            // The dim needs its reason spoken: a caption under the button would change the dock's
            // height per track and shift the artwork zone with it, so it rides the semantics.
            stateLabel = if (lyricsAvailable) null else stringResource(Res.string.no_lyrics_for_track),
            onClick = {
                onSelectView(if (viewState == AppleMusicView.LYRICS) AppleMusicView.MAIN else AppleMusicView.LYRICS)
            },
        )
        if (isPlatformCastAvailable()) {
            // 48dp slot, but the target is the native MediaRouteButton inside it (22dp) — it owns
            // its own touch handling and semantics, so the box can be neither clickable nor labelled
            // without double-firing. The label rides on the box so TalkBack has a name for the slot.
            Box(
                modifier =
                    Modifier
                        .appleMusicPressInflate()
                        .size(48.dp)
                        .semantics { contentDescription = castLabel },
                contentAlignment = Alignment.Center,
            ) {
                PlatformCastButton(
                    modifier = Modifier.size(22.dp),
                    // Accent while casting: the one state on the dock that is about the fork's
                    // own session rather than the record playing.
                    tint = if (castState.isRemote) seed else Color.White,
                )
            }
        }
        AppleMusicDockButton(
            icon = SimpIcons.QueueMusic,
            contentDescription = stringResource(Res.string.queue),
            active = viewState == AppleMusicView.QUEUE,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            onClick = {
                onSelectView(if (viewState == AppleMusicView.QUEUE) AppleMusicView.MAIN else AppleMusicView.QUEUE)
            },
        )
    }
}

/**
 * Progress bar + times + transport — the playback half of [AppleMusicBottomCluster], shared with
 * the fullscreen lyrics landscape layout. Emits straight into the caller's Column; the horizontal
 * gutter belongs to that Column, as it does in the cluster.
 */
@Composable
internal fun ColumnScope.AppleMusicPlaybackControls(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    showShuffleAndRepeat: Boolean = false,
) {
    // Fixed 24dp shell: the track swells on touch, but inside a CONSTANT footprint —
    // otherwise the growing slider re-measures this whole column and the artwork above
    // it visibly jumps. It also gives the bar a real 24dp touch target instead of 7dp.
    // Was 18: the extra 6 is split 3/3 around the track, and the 3 below is taken back from
    // the times row's top padding (8 → 5) so nothing under the bar moves.
    Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
        AppleMusicThinSlider(
            value = state.sliderValue / 100f,
            activeColor = if (state.timelineState.isCrossfading) state.sliderTrackColor else AppleMusicTrackActive,
            label = stringResource(Res.string.seek_bar),
            onValueChange = { actions.onSliderChange(it * 100f) },
            onValueChangeFinished = actions.onSliderChangeFinished,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AppleMusicTimesRow(state = state, typography = typography, modifier = Modifier.padding(top = 5.dp))
    Spacer(modifier = Modifier.height(12.dp))
    AppleMusicTransportRow(
        controllerState = state.controllerState,
        onUIEvent = actions.onUIEvent,
        showShuffleAndRepeat = showShuffleAndRepeat,
    )
}

/**
 * Progress bar + times + transport + volume + dock — the fixed block every Apple Music body
 * (MAIN, LYRICS, QUEUE) renders at the bottom, identically. On Desktop only the dock renders
 * (no slider/transport/volume), matching the `Platform.Android` gate the other two styles use.
 */
@Composable
internal fun AppleMusicBottomCluster(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    deviceVolumeController: DeviceVolumeController?,
    modifier: Modifier = Modifier,
) {
    val localDensity = LocalDensity.current
    // Every spacer here is 3-4dp shorter than it was, by exactly the amount the seek bar's shell
    // (18 → 24), the volume slider's shell (18 → 24) and the dock buttons (40 → 48) grew on each
    // side, so the tracks and glyphs sit where they always did.
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 5.dp)) {
        if (getPlatform() == Platform.Android) {
            AppleMusicPlaybackControls(state = state, actions = actions, typography = typography)
            Spacer(modifier = Modifier.height(11.dp))
            deviceVolumeController?.let { controller ->
                AppleMusicVolumeRow(controller = controller)
                Spacer(modifier = Modifier.height(7.dp))
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
        AppleMusicDock(
            viewState = viewState,
            onSelectView = onSelectView,
            castState = state.castState,
            lyricsAvailable = state.screenData.lyricsData != null,
            activeColor = activePillContainer,
            activeContentColor = activePillContent,
        )
        Spacer(
            modifier =
                Modifier.height(
                    // Breathing room under the dock: the bare inset parked the icons right on the
                    // gesture bar.
                    with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() } + 8.dp,
                ),
        )
    }
}