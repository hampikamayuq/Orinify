package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.maxrave.simpmusic.AppLinks
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.marqueeIterations
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.based_on_simpmusic
import simpmusic.composeapp.generated.resources.copyright
import simpmusic.composeapp.generated.resources.credit_app
import simpmusic.composeapp.generated.resources.issue_tracker
import simpmusic.composeapp.generated.resources.orinify_author
import simpmusic.composeapp.generated.resources.orinify_glyph
import simpmusic.composeapp.generated.resources.source_code
import simpmusic.composeapp.generated.resources.support_original_developer
import simpmusic.composeapp.generated.resources.version_format

// The launcher's own gradient (ic_launcher_background), so the icon here is the icon on the
// home screen and not a second drawing of it.
private val IconGradient = listOf(Color(0xFF3B1E8A), Color(0xFF7C3AED))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun CreditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val hazeState = rememberHazeState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 64.dp)
                .verticalScroll(rememberScrollState())
                .hazeSource(state = hazeState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Box(
            modifier =
                Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(IconGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.orinify_glyph),
                // Decorative: the app name is the next line.
                contentDescription = null,
                modifier = Modifier.size(150.dp),
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = stringResource(Res.string.app_name),
            style = typo().titleLarge,
            fontSize = 22.sp,
            // This is the page's actual heading — the TopAppBar title below shows the same
            // string, but only this one should be where a screen reader user jumps to.
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = stringResource(Res.string.version_format, VersionManager.getVersionName()),
            style = typo().bodySmall,
            fontSize = 13.sp,
        )

        Text(
            text = stringResource(Res.string.orinify_author),
            style = typo().bodyMedium,
            textDecoration = TextDecoration.Underline,
            modifier =
                Modifier
                    .clickable { openUrl(AppLinks.REPO) }
                    .padding(8.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.credit_app),
            style = typo().bodyMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Plain 48dp text buttons: the minimum-interactive-size override that used to sit here
        // made every one of these a 20dp target.
        LinkButton(Res.string.source_code) { openUrl(AppLinks.REPO) }
        LinkButton(Res.string.issue_tracker) { openUrl(AppLinks.ISSUES) }
        // Principle 4: credit what is not ours, by name and with a way to give back.
        LinkButton(Res.string.based_on_simpmusic) { openUrl(AppLinks.UPSTREAM_REPO) }
        LinkButton(Res.string.support_original_developer) { openUrl(AppLinks.UPSTREAM_SPONSOR) }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(Res.string.copyright),
            style = typo().bodySmall,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp, vertical = 5.dp),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(200.dp))
    }
    TopAppBar(
        modifier =
            Modifier
                .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                    blurEnabled = true
                },
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                style = typo().titleMedium,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(
                            align = Alignment.CenterVertically,
                        ).basicMarquee(
                            iterations = marqueeIterations(Int.MAX_VALUE),
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    SimpIcons.ArrowBackIosNew,
                    Modifier
                        .size(48.dp),
                    true,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "Back",
                ) {
                    navController.navigateUp()
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                Color.Transparent,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
            ),
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.LinkButton(
    label: org.jetbrains.compose.resources.StringResource,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier =
            Modifier
                .align(Alignment.Start)
                .padding(horizontal = 25.dp),
    ) {
        Text(text = stringResource(label))
    }
}
