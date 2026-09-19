package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxrave.domain.extension.now
import com.maxrave.simpmusic.ui.theme.BottomChrome
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.end_of_page_credit
import simpmusic.composeapp.generated.resources.version_format

// Room for the credit line: 20dp above it, two lines of bodySmall, and a little air under it. It is
// reserved even with [EndOfPage.withoutCredit] so the last row never sits flush on the mini player.
private val CreditSpace = 80.dp

/**
 * The trailing block every scrolling page ends with, so its last row can be scrolled clear of the
 * floating bottom chrome. It used to be a flat 280dp — a guess that happened to cover a mini player,
 * a bottom bar and a three-button navigation bar with something to spare. It is now derived:
 * [CreditSpace] + [BottomChrome.StackHeight] (mini player 56 + gap 12 + bar 64 + bar padding 8) +
 * the device's own `navigationBars` inset, so gesture and three-button devices each get their
 * real system bar instead of a fixed allowance. At the defaults that is 80 + 140 + nav bar
 * (≈ 268dp with a 48dp three-button bar, ≈ 244dp with a 24dp gesture bar).
 *
 * Callers that already feed the Scaffold's `innerPadding.bottom` into their list (Home, Library,
 * Settings) get this on top of it, exactly as they got the 280 before; the ones that never apply
 * `innerPadding` (Album, Playlist, Artist, Search, ...) rely on this block alone.
 */
@Composable
fun EndOfPage(withoutCredit: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(CreditSpace),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!withoutCredit) {
                Text(
                    // One resource holding the whole line, placeholders included, so a translator can
                    // reorder year/name/version without touching Kotlin. The "©" is also a "©": typed "@".
                    stringResource(
                        Res.string.end_of_page_credit,
                        now().year.toString(),
                        stringResource(Res.string.app_name),
                        stringResource(Res.string.version_format, VersionManager.getVersionName()),
                    ),
                    style = typo().bodySmall,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(
                                top = 20.dp,
                            ).alpha(0.8f),
                )
            }
        }
        Spacer(Modifier.height(BottomChrome.StackHeight))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
