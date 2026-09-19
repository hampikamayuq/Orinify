package com.maxrave.simpmusic.ui.screen.home.wrapped

import androidx.compose.runtime.Composable
import com.maxrave.simpmusic.viewModel.WrappedListeningBand
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.am
import simpmusic.composeapp.generated.resources.pm
import kotlin.math.roundToInt

/**
 * Formatting for the reel, kept out of the cards.
 *
 * Compose Resources understands `%1$s` and `%1$d` and nothing else — no width, no flags, no `%%` —
 * so every separator, unit symbol and rounding decision has to be made in Kotlin and handed to the
 * string as an already-finished piece. A string resource here only ever joins finished pieces,
 * which is also what spares translators from format specifiers.
 *
 * Anything the Analytics screen already formats is reused from
 * [com.maxrave.simpmusic.ui.screen.home.analytics.AnalyticsFormat] rather than reimplemented — a
 * duration printed two ways in one app is a bug the user can see. That now includes the grouped
 * count (`formatCount`): Wrapped carried its own copy, and the two disagreed on four-digit numbers.
 */

/** A fraction 0..1 as whole percent — "41%". Rounded, never truncated: 0.999 is 100%, not 99%. */
fun formatPercent(fraction: Float): String = "${(fraction.coerceIn(0f, 1f) * 100).roundToInt()}%"

/**
 * The hour a card prints beside its meridiem — "9" next to "PM".
 *
 * Split rather than returned whole because card 06 sets the two at wildly different sizes. On a
 * 24-hour locale the meridiem comes back empty and the number carries the whole label.
 */
fun hourNumber(
    hour: Int,
    use24Hour: Boolean,
): String =
    when {
        use24Hour -> hour.toString()
        hour % 12 == 0 -> "12"
        else -> (hour % 12).toString()
    }

/**
 * Empty on a 24-hour locale, where a meridiem would be wrong rather than merely redundant.
 *
 * Composable because the two markers are resources: "AM"/"PM" is not universal even among the
 * twelve-hour locales this card checks for.
 */
@Composable
fun hourMeridiem(
    hour: Int,
    use24Hour: Boolean,
): String =
    when {
        use24Hour -> ""
        hour < 12 -> stringResource(Res.string.am)
        else -> stringResource(Res.string.pm)
    }

/** "9 PM", or "21" where a meridiem does not belong. Used inside sentences, not as a hero figure. */
@Composable
fun hourLabel(
    hour: Int,
    use24Hour: Boolean,
): String =
    hourMeridiem(hour, use24Hour)
        .let { meridiem -> if (meridiem.isEmpty()) hourNumber(hour, true) else "${hourNumber(hour, false)} $meridiem" }

/** The two ends of a band, as card 06's caption names them. */
@Composable
fun bandBounds(
    band: WrappedListeningBand,
    use24Hour: Boolean,
): Pair<String, String> = hourLabel(band.startHour, use24Hour) to hourLabel(band.endHour % 24, use24Hour)

/** Seconds to whole minutes — the figure card 02 is built around. */
fun wholeMinutes(seconds: Long): Long = seconds / 60

/** Seconds to whole days, for card 02's "12 whole days". Floored: claiming a day that did not finish would be a lie. */
fun wholeDays(seconds: Long): Long = seconds / 86_400
