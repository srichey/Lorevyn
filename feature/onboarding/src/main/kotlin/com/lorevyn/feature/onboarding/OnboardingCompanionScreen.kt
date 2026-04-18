package com.lorevyn.feature.onboarding

// W-05 (onboarding picker): CompanionCardItem now loads real PNG assets.
//        Material icon placeholders removed.
// W-08: Selected companion card gets radial terracotta glow behind it.
//       isSelected passed from pager current page to each card.

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lorevyn.core.navigation.AddBookMode
import com.lorevyn.core.navigation.CompanionPickerMode
import com.lorevyn.core.ui.R as UiR
import com.lorevyn.feature.onboarding.R
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────

private val Cream       = Color(0xFFF6F1E9)
private val CardBg      = Color(0xFFFDFCFA)
private val TextPrimary = Color(0xFF1E1E1E)
private val TextSecond  = Color(0xFF1E1E1E).copy(alpha = 0.55f)
private val PrimaryDark = Color(0xFF2D5A3D)
private val Terracotta  = Color(0xFFB5673E)

// Dot indicator constants — Decision #41 LOCKED
private val DOT_ACTIVE_WIDTH   = 20.dp
private val DOT_ACTIVE_HEIGHT  = 6.dp
private val DOT_INACTIVE_SIZE  = 6.dp
private val DOT_RADIUS         = 3.dp
private val DOT_ACTIVE_COLOR   = Terracotta
private val DOT_INACTIVE_COLOR = Color(0xFFC8C4BC)

// ─────────────────────────────────────────────────────────────────────────────
// Companion data — PNG assets wired (W-05)
// ─────────────────────────────────────────────────────────────────────────────

private data class CompanionCard(
    val type:    CompanionTypeLocal,
    val nameRes: Int,
    val descRes: Int,
)

private val COMPANIONS = listOf(
    CompanionCard(CompanionTypeLocal.OWL,    R.string.onboarding_companion_owl_name,    R.string.onboarding_companion_owl_desc),
    CompanionCard(CompanionTypeLocal.CAT,    R.string.onboarding_companion_cat_name,    R.string.onboarding_companion_cat_desc),
    CompanionCard(CompanionTypeLocal.DOG,    R.string.onboarding_companion_dog_name,    R.string.onboarding_companion_dog_desc),
    CompanionCard(CompanionTypeLocal.RABBIT, R.string.onboarding_companion_rabbit_name, R.string.onboarding_companion_rabbit_desc),
)

// Maps CompanionTypeLocal → idle PNG resource (always idle state in picker)
private fun idleDrawableFor(type: CompanionTypeLocal): Int = when (type) {
    CompanionTypeLocal.OWL    -> UiR.drawable.companion_owl_idle
    CompanionTypeLocal.CAT    -> UiR.drawable.companion_cat_idle
    CompanionTypeLocal.DOG    -> UiR.drawable.companion_dog_idle
    CompanionTypeLocal.RABBIT -> UiR.drawable.companion_rabbit_idle
}

// ─────────────────────────────────────────────────────────────────────────────
// SCR-05 — Companion picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingCompanionScreen(
    onNavigateToAddBook:   (AddBookMode) -> Unit = {},
    onNavigateBack:        () -> Unit = {},
    mode:                  CompanionPickerMode = CompanionPickerMode.ONBOARDING,
    initialCompanionIndex: Int = 0,
    viewModel:             OnboardingViewModel = hiltViewModel()
) {
    val safeInitial      = initialCompanionIndex.coerceIn(0, COMPANIONS.lastIndex)
    val pagerState       = rememberPagerState(
        initialPage = safeInitial,
        pageCount   = { COMPANIONS.size }
    )
    val currentCompanion = COMPANIONS[pagerState.currentPage]
    val scope            = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Back arrow (SETTINGS mode only — P-04) ────────────────────────────
        if (mode == CompanionPickerMode.SETTINGS) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint               = PrimaryDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text = stringResource(
                if (mode == CompanionPickerMode.SETTINGS)
                    R.string.onboarding_companion_settings_title
                else
                    R.string.onboarding_companion_title
            ),
            fontSize   = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            modifier   = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Carousel ──────────────────────────────────────────────────────────
        HorizontalPager(
            state          = pagerState,
            modifier       = Modifier.fillMaxWidth(),
            pageSpacing    = 16.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp
            )
        ) { page ->
            CompanionCardItem(
                companion  = COMPANIONS[page],
                isSelected = page == pagerState.currentPage
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Dot indicators — Decision #41 LOCKED ──────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(COMPANIONS.size) { index ->
                val isActive  = index == pagerState.currentPage
                val dotWidth  = animateDpAsState(
                    targetValue = if (isActive) DOT_ACTIVE_WIDTH else DOT_INACTIVE_SIZE,
                    label       = "dot_width_$index"
                )
                val dotHeight = animateDpAsState(
                    targetValue = if (isActive) DOT_ACTIVE_HEIGHT else DOT_INACTIVE_SIZE,
                    label       = "dot_height_$index"
                )
                Box(
                    modifier = Modifier
                        .width(dotWidth.value)
                        .height(dotHeight.value)
                        .clip(RoundedCornerShape(DOT_RADIUS))
                        .background(if (isActive) DOT_ACTIVE_COLOR else DOT_INACTIVE_COLOR)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Companion name + description ──────────────────────────────────────
        Text(
            text       = stringResource(currentCompanion.nameRes),
            fontSize   = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color      = PrimaryDark,
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text      = stringResource(currentCompanion.descRes),
            fontSize  = 13.sp,
            fontStyle = FontStyle.Italic,
            color     = TextSecond,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── CTA button ────────────────────────────────────────────────────────
        Button(
            onClick = {
                if (mode == CompanionPickerMode.SETTINGS) {
                    scope.launch {
                        viewModel.saveCompanionForSettings(currentCompanion.type)
                        onNavigateBack()
                    }
                } else {
                    viewModel.onCompanionSelectedNoNav(currentCompanion.type) {
                        onNavigateToAddBook(AddBookMode.FIRST_RUN)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape  = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
        ) {
            Text(
                text = stringResource(
                    if (mode == CompanionPickerMode.SETTINGS)
                        R.string.onboarding_companion_settings_cta
                    else
                        R.string.onboarding_companion_cta
                ),
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Companion card item
//
// W-05: Loads real PNG from drawable-nodpi/ (idle state — always in picker).
// W-08: isSelected=true adds radial terracotta glow behind the companion image.
//       Selection feels like a meaningful choice, not just navigation.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompanionCardItem(
    companion:  CompanionCard,
    isSelected: Boolean = false,
) {
    Box(
        modifier         = Modifier
            .width(280.dp)
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg),
        contentAlignment = Alignment.Center
    ) {
        // W-08 — radial glow behind selected companion (Decision #140)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Terracotta.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Companion PNG — idle state, scales to fill the circle
        Box(
            modifier         = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected)
                        Terracotta.copy(alpha = 0.08f)
                    else
                        Color(0xFF4A7C59).copy(alpha = 0.06f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter            = painterResource(id = idleDrawableFor(companion.type)),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.size(160.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF6F1E9)
@Composable
private fun OnboardingCompanionPreview() {
    OnboardingCompanionScreen(onNavigateToAddBook = {})
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F1E9)
@Composable
private fun OnboardingCompanionSettingsPreview() {
    OnboardingCompanionScreen(
        mode                  = CompanionPickerMode.SETTINGS,
        onNavigateBack        = {},
        initialCompanionIndex = 1
    )
}
