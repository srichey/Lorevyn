package com.lorevyn.feature.reading

// feature/reading/src/main/kotlin/com/lorevyn/feature/reading/ReadingTabScreen.kt
// v8 — April 13, 2026
// W-04: Heatmap reduced from 5-week (35 cells) to 4-week (28 cells).
//       startCol calc updated (-34 → -27). inRange updated (0..34 → 0..27). Rows 0..4 → 0..3.
// W-12: AnnualGoalSection now accepts GoalProgress? and shows real "X of Y books" count.
//       AnnualGoalSection() call updated to pass goalProgress from ViewModel.
// TD-110: OnThisDayCard placeholder added. Decision #180: always visible to all users.
//         Free users see locked teaser with crown. Premium content built in Group 5.
// TD-110b: onNavigateToPaywall wired. Tap on "Unlock with Premium ›" navigates to paywall.

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lorevyn.core.domain.model.Book
import com.lorevyn.core.domain.model.GoalProgress
import com.lorevyn.core.ui.LocalLorevynColors
import java.util.Calendar

// ── Greeting helper ───────────────────────────────────────────────────────────
@Composable
private fun greeting(displayName: String?): String {
    val defaultName = stringResource(R.string.reading_greeting_default_name)
    val name = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: defaultName
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> stringResource(R.string.reading_greeting_morning, name)
        in 12..17 -> stringResource(R.string.reading_greeting_afternoon, name)
        else      -> stringResource(R.string.reading_greeting_evening, name)
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ReadingTabScreen(
    viewModel: ReadingTabViewModel,
    onNavigateToBookDetail: (Long) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToPaywall: (String) -> Unit = {},
) {
    val uiState        by viewModel.uiState.collectAsState()
    val streakDays     by viewModel.streakDays.collectAsState()
    val heatmapCells   by viewModel.heatmapCells.collectAsState()
    val companionState by viewModel.companionState.collectAsState()
    val companionType  by viewModel.companionType.collectAsState()
    val displayName    by viewModel.displayName.collectAsState()
    val goalProgress   by viewModel.goalProgress.collectAsState()

    var showStreakDetail by remember { mutableStateOf(false) }
    val streakVisible   by viewModel.streakVisible.collectAsState()

    if (showStreakDetail) {
        StreakDetailBottomSheet(
            viewModel           = viewModel,
            onDismiss           = { showStreakDetail = false },
            onNavigateToPaywall = { showStreakDetail = false }
        )
    }

    val colors = LocalLorevynColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (val state = uiState) {
            is ReadingTabUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.sage
                )
            }

            is ReadingTabUiState.Empty -> {
                ReadingEmptyState(
                    onAddBook = onNavigateToAddBook,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ReadingTabUiState.HasBooks -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = greeting(displayName),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )

                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (streakVisible) {
                            StreakChip(
                                streakDays = streakDays,
                                onClick    = { showStreakDetail = true }
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        CompanionDisplay(
                            companionType  = companionType,
                            companionState = companionState,
                            size           = 68.dp,
                            modifier       = Modifier.wrapContentSize()
                        )
                    }

                    if (state.books.size == 1) {
                        BookHeroCard(
                            book    = state.books.first(),
                            onClick = { onNavigateToBookDetail(state.books.first().id) }
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { state.books.size })

                        HorizontalPager(
                            state    = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            BookHeroCard(
                                book     = state.books[page],
                                onClick  = { onNavigateToBookDetail(state.books[page].id) },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(state.books.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .then(
                                            if (isSelected)
                                                Modifier
                                                    .width(20.dp)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(colors.terracotta)
                                            else
                                                Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.sage.copy(alpha = 0.25f))
                                        )
                                )
                            }
                        }
                    }

                    ReadingActivitySection(heatmapCells = heatmapCells)
                    AnnualGoalSection(goalProgress = goalProgress)
                    OnThisDayCard(onNavigateToPaywall = onNavigateToPaywall)
                }
            }

            is ReadingTabUiState.Error -> {
                ReadingEmptyState(
                    onAddBook = onNavigateToAddBook,
                    modifier  = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// ── Streak chip ───────────────────────────────────────────────────────────────

@Composable
private fun StreakChip(streakDays: Int, onClick: () -> Unit = {}) {
    val colors = LocalLorevynColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.terracotta.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = "🔥", fontSize = 12.sp)
            Text(
                text = if (streakDays > 0)
                           stringResource(R.string.reading_streak_active, streakDays)
                       else
                           stringResource(R.string.reading_streak_start),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = colors.terracotta
            )
            Text(
                text       = "›",
                fontSize   = 13.sp,
                color      = colors.terracotta,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun BookHeroCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors      = LocalLorevynColors.current
    val pageCount   = book.pageCount
    val currentPage = book.currentPage
    val hasProgress = pageCount != null && pageCount > 0
    val progress    = if (hasProgress) (currentPage.toFloat() / pageCount!!.toFloat()).coerceIn(0f, 1f) else 0f
    val percent     = (progress * 100).toInt()
    val authorText  = book.authors.joinToString(", ") { it.name }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .height(122.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.sage.copy(alpha = 0.08f))
            ) {
                if (book.coverUrl != null) {
                    AsyncImage(
                        model              = book.coverUrl,
                        contentDescription = book.title,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint               = colors.sage.copy(alpha = 0.35f),
                        modifier           = Modifier.size(28.dp).align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier              = Modifier.weight(1f),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = book.title,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.textPrimary,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 21.sp
                )
                if (authorText.isNotBlank()) {
                    Text(
                        text      = authorText,
                        fontSize  = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color     = colors.sage,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                var animTarget by remember { mutableFloatStateOf(0f) }
                LaunchedEffect(progress) { animTarget = progress }
                val animatedProgress by animateFloatAsState(
                    targetValue   = animTarget,
                    animationSpec = tween(durationMillis = 400),
                    label         = "progress"
                )
                LinearProgressIndicator(
                    progress    = { animatedProgress },
                    modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color       = colors.sage,
                    trackColor  = colors.sage.copy(alpha = 0.12f),
                    strokeCap   = StrokeCap.Round,
                )
                if (hasProgress) {
                    Text(
                        text     = stringResource(R.string.reading_progress_text, percent, currentPage, pageCount!!),
                        fontSize = 11.sp,
                        color    = colors.textSecondary.copy(alpha = 0.55f)
                    )
                } else if (currentPage > 0) {
                    // Page count missing from API — show pages logged as fallback
                    Text(
                        text     = stringResource(R.string.reading_pages_logged, currentPage),
                        fontSize = 11.sp,
                        color    = colors.textSecondary.copy(alpha = 0.55f)
                    )
                }
            }
            // Tappable affordance — terracotta chevron signals "tap to see book detail"
            Text(
                text       = "›",
                fontSize   = 20.sp,
                color      = colors.terracotta,
                fontWeight = FontWeight.Normal,
                modifier   = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

// ── Reading Activity — rolling 4-week daily grid (W-04) ───────────────────────
// 28 cells: index 0 = 27 days ago, index 27 = today.
// Grid is Mon–Sun aligned so each cell lands on the correct weekday column.
// Decision #32 updated April 5: 28-cell (4 complete weeks) replaces 35-cell (5 weeks).

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
private fun ReadingActivitySection(heatmapCells: List<Int>) {
    val colors = LocalLorevynColors.current
    // Find which Mon–Sun column (0=Mon … 6=Sun) day index 0 falls on.
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -27)
    // Calendar.DAY_OF_WEEK: 1=Sun … 7=Sat → convert to 0=Mon … 6=Sun
    val startCol = (cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
    val maxPages = heatmapCells.maxOrNull()?.takeIf { it > 0 } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Section label row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text          = stringResource(R.string.reading_activity_label),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = colors.textSecondary,
                letterSpacing = 0.8.sp
            )
            Text(
                text     = stringResource(R.string.reading_activity_period),
                fontSize = 10.sp,
                color    = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DAY_LABELS.forEach { label ->
                Text(
                    text       = label,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color      = colors.textSecondary,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4 rows × 7 columns — Mon–Sun aligned, 28 cells total
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (row in 0..3) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        val dayIndex = row * 7 + col - startCol
                        val inRange  = dayIndex in 0..27
                        val pages    = if (inRange) heatmapCells[dayIndex] else 0
                        val cellColor = when {
                            !inRange                 -> Color.Transparent
                            pages == 0               -> colors.sage.copy(alpha = 0.08f)
                            pages < maxPages * 0.33f -> colors.sage.copy(alpha = 0.30f)
                            pages < maxPages * 0.66f -> colors.sage.copy(alpha = 0.60f)
                            else                     -> colors.sage.copy(alpha = 0.92f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cellColor)
                        )
                    }
                }
            }
        }
    }
}

// ── Annual Goal row (W-12) ────────────────────────────────────────────────────
// goalProgress: null when no annual BOOKS goal has been set (new users).
// When non-null: shows real progress fraction + "X of Y books" count label.

@Composable
private fun AnnualGoalSection(goalProgress: GoalProgress?) {
    val colors = LocalLorevynColors.current
    val fraction = goalProgress?.let { gp ->
        if (gp.goal.target > 0) (gp.current.toFloat() / gp.goal.target.toFloat()).coerceIn(0f, 1f)
        else 0f
    } ?: 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text          = stringResource(R.string.annual_goal_label),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = colors.textSecondary,
                letterSpacing = 0.8.sp
            )
            if (goalProgress != null) {
                Text(
                    text     = stringResource(
                        R.string.annual_goal_count_label,
                        goalProgress.current,
                        goalProgress.goal.target
                    ),
                    fontSize  = 10.sp,
                    color     = colors.textSecondary
                )
            } else {
                Text(
                    text      = stringResource(R.string.reading_goal_journey_hint),
                    fontSize  = 10.sp,
                    color     = colors.textSecondary,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.5.dp)),
            color      = colors.sage,
            trackColor = colors.sage.copy(alpha = 0.12f),
            strokeCap  = StrokeCap.Round,
        )
    }
}

// ── On This Day card — TD-110 / Decision #180 ─────────────────────────────────
// Free placeholder is ALWAYS visible. Premium content built in Group 5.
// Card never disappears — a missing section looks like a crash to testers.

@Composable
private fun OnThisDayCard(
    onNavigateToPaywall: (String) -> Unit = {},
) {
    val colors = LocalLorevynColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Section label row with crown
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text          = stringResource(R.string.on_this_day_label),
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = colors.textSecondary,
                    letterSpacing = 0.8.sp
                )
                // Crown badge — terracotta, Decision #165
                Icon(
                    imageVector        = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint               = colors.terracotta,
                    modifier           = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Locked teaser body
            Text(
                text       = stringResource(R.string.on_this_day_teaser),
                fontSize   = 13.sp,
                fontStyle  = FontStyle.Italic,
                color      = colors.textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Upgrade nudge — tappable row, terracotta, Decision #127
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPaywall("on_this_day") },
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = stringResource(R.string.on_this_day_unlock_cta),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.terracotta
                )
                Text(
                    text       = " ›",
                    fontSize   = 14.sp,
                    color      = colors.terracotta,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun ReadingEmptyState(
    onAddBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLorevynColors.current
    Column(
        modifier              = modifier.padding(horizontal = 20.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                .background(colors.surface).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint               = colors.sage.copy(alpha = 0.40f),
                    modifier           = Modifier.size(56.dp)
                )
                Text(
                    text       = stringResource(R.string.reading_empty_title),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.primaryGreen,
                    textAlign  = TextAlign.Center
                )
                Text(
                    text      = stringResource(R.string.reading_empty_body),
                    fontSize  = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color     = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.sage)
                        .clickable(onClick = onAddBook)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text       = stringResource(R.string.reading_empty_cta),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}
