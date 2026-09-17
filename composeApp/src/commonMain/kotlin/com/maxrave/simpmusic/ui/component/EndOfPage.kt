package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxrave.domain.extension.now
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.end_of_page_credit
import simpmusic.composeapp.generated.resources.maxrave_dev
import simpmusic.composeapp.generated.resources.version_format

@Composable
fun EndOfPage(withoutCredit: Boolean = false) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(280.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (!withoutCredit) {
            Text(
                // One resource holding the whole line, placeholders and line break included. It was
                // assembled here out of four pieces joined with " " and "\n", which a translator can
                // only ever translate a word of — the order of year, name, version and author and
                // where the line breaks are all live in this file's Kotlin, out of their reach, and
                // several locales need a different order. The "©" is also a "©": it was typed "@".
                stringResource(
                    Res.string.end_of_page_credit,
                    now().year.toString(),
                    stringResource(Res.string.app_name),
                    stringResource(Res.string.version_format, VersionManager.getVersionName()),
                    stringResource(Res.string.maxrave_dev),
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
}
