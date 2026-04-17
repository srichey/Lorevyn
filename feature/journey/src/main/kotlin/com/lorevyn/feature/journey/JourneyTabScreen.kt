package com.lorevyn.feature.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lorevyn.core.domain.model.BookFormat
import com.lorevyn.core.domain.model.CompanionType
import com.lorevyn.core.domain.model.GoalProgress
import com.lorevyn.core.domain.model.MoodTag
import com.lorevyn.core.domain.model.MonthlyProgress
import com.lorevyn.core.domain.model.ReadingStats
import com.lorevyn.core.ui.PremiumGate
import com.lorevyn.feature.journey.R
import java.text.DateFormatSymbols
import com.lorevyn.core.ui.LocalLorevynColors

// ── Design tokens ─────────────────────────────────────────────────────────────
// Phase B will migrate these to LorevynColors tokens. Keep hardcoded until then.
// Legacy private color constants removed — colors now read from LocalLorevynColors.current

// ─────────────────────────────────────────────────────────────────────────────
// JourneyTabScreen — SCR-08
// Structure: 5 named sections (Decision #162)
// Section 1: Streak & Goal   — free, habit zone
// Section 2: This Year       — free, basic stats
// Section 3: Challenges      — free (1 active) / premium (all)
// Section 4: Reading Intelligence — premium, always visible, crown badge
// Section 5: Your Reading Story   — free, Annual Wrap-Up entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun JourneyTabScreen(
    isPremium: Boolean = false,
    onAnnualWrapUpTapped: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToReadingSpine: () -> Unit = {},
    viewModel: JourneyViewModel = hiltViewModel(),
) {
    val colors = LocalLorevynColors.current
    val uiState      by viewModel.uiState.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()
    val companionType by viewModel.companionType.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (val state = uiState) {
            is JourneyUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = colors.sage,
                )
            }

            is JourneyUiState.Error -> {
                Column(
                    modifier            = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text       = stringResource(R.string.journey_error_title),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textPrimary,
                        textAlign  = TextAlign.Center,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.sage)
                            .clickable { viewModel.retry() }
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text       = stringResource(R.string.journey_retry),
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                        )
                    }
                }
            }

            is JourneyUiState.Ready -> {
                JourneyContent(
                    state                    = state,
                    goalProgress             = goalProgress,
                    companionType            = companionType,
                    isPremium                = isPremium,
                    onFingerprintTapped      = viewModel::onFingerprintTapped,
                    onUpgradeTapped          = viewModel::onUpgradeTapped,
                    onAnnualWrapUpTapped     = onAnnualWrapUpTapped,
                    onNavigateToFavorites    = onNavigateToFavorites,
                    onNavigateToReadingSpine = onNavigateToReadingSpine,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main scrollable content — 5-section structure
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JourneyContent(
    state: JourneyUiState.Ready,
    goalProgress: GoalProgress?,
    companionType: CompanionType,
    isPremium: Boolean,
    onFingerprintTapped: () -> Unit,
    onUpgradeTapped: () -> Unit,
    onAnnualWrapUpTapped: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToReadingSpine: () -> Unit,
) {
    val colors = LocalLorevynColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Section 1: Streak & Goal ──────────────────────────────────────────
        // Habit zone — drives daily opens. Always free.

        SectionHeader(label = stringResource(R.string.journey_section_streak_goal))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label    = stringResource(R.string.journey_best_streak),
                value    = stringResource(R.string.journey_days_value, state.stats.longestStreak),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = stringResource(R.string.journey_reading_hours),
                value    = formatHours(state.stats.readingMinutes),
                modifier = Modifier.weight(1f),
            )
        }

        goalProgress?.let { GoalProgressCard(goalProgress = it) }

        // ── Section 2: This Year ──────────────────────────────────────────────
        // Current-year basic stats. Always free.

        SectionHeader(label = stringResource(R.string.journey_section_this_year))

        Text(
            text       = stringResource(R.string.journey_year_header, state.year),
            fontSize   = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color      = colors.textPrimary,
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label    = stringResource(R.string.journey_books_finished),
                value    = state.stats.booksFinished.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = stringResource(R.string.journey_pages_read),
                value    = formatPages(state.stats.pagesRead),
                modifier = Modifier.weight(1f),
            )
        }

        state.stats.averageRating?.let { avg ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label    = stringResource(R.string.journey_avg_rating_label),
                    value    = stringResource(R.string.journey_rating_value, avg),
                    modifier = Modifier.weight(1f),
                )
                state.stats.topFormat?.let { fmt ->
                    StatCard(
                        label    = stringResource(R.string.journey_top_format),
                        value    = formatLabel(fmt),
                        modifier = Modifier.weight(1f),
                    )
                } ?: Spacer(modifier = Modifier.weight(1f))
            }
        }

        HeatmapCard(cells = state.heatmapCells)

        if (state.stats.booksFinished > 0) {
            MonthlyChartCard(
                monthlyProgress  = state.stats.monthlyProgress,
                bestReadingMonth = state.bestReadingMonth,
            )
        }

        if (state.topAuthors.isNotEmpty()) {
            TopAuthorsCard(authors = state.topAuthors)
        }

        if (state.ratingDistribution.isNotEmpty()) {
            RatingDistributionCard(distribution = state.ratingDistribution)
        }

        if (state.fastestReadTitle != null || state.slowestReadTitle != null) {
            ReadingRecordsCard(
                fastestReadTitle = state.fastestReadTitle,
                fastestReadDays  = state.fastestReadDays,
                slowestReadTitle = state.slowestReadTitle,
                slowestReadDays  = state.slowestReadDays,
            )
        }

        FingerprintCard(
            available     = state.fingerprintAvailable,
            booksFinished = state.stats.booksFinished,
            onTap         = onFingerprintTapped,
        )

        // ── Section 3: Challenges ─────────────────────────────────────────────
        // Free: 1 active monthly challenge visible.
        // Premium: all challenge types active simultaneously.
        // Full challenge system wired in Group 6. Placeholder for now.

        SectionHeader(label = stringResource(R.string.journey_section_challenges))

        ChallengesPlaceholderCard()

        // ── Section 4: Reading Intelligence ──────────────────────────────────
        // Always visible. Always aspirational. Never hidden. (Decision #163)
        // Crown badge on header — not padlock, not grey. (Decision #165)
        // Each card visible to all users. Locked content blurred + crown + CTA.

        ReadingIntelligenceSectionHeader()

        if (state.stats.genreDistribution.isNotEmpty()) {
            PremiumGate(
                isPremium         = isPremium,
                featureTitle      = stringResource(R.string.journey_genre_breakdown_title),
                lockedDescription = stringResource(R.string.journey_genre_locked_desc),
                onUpgradeClick    = onUpgradeTapped,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text          = stringResource(R.string.journey_genre_breakdown_title).uppercase(),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = colors.textSecondary,
                        letterSpacing = 0.8.sp,
                    )
                    GenreBreakdownContent(distribution = state.stats.genreDistribution)
                }
            }
        }

        if (state.stats.moodDistribution.isNotEmpty()) {
            PremiumGate(
                isPremium         = isPremium,
                featureTitle      = stringResource(R.string.journey_mood_distribution_title),
                lockedDescription = stringResource(R.string.journey_mood_locked_desc),
                onUpgradeClick    = onUpgradeTapped,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text          = stringResource(R.string.journey_mood_distribution_title).uppercase(),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = colors.textSecondary,
                        letterSpacing = 0.8.sp,
                    )
                    MoodDistributionContent(distribution = state.stats.moodDistribution)
                }
            }
        }

        state.stats.yearOverYear?.let { yoy ->
            PremiumGate(
                isPremium         = isPremium,
                featureTitle      = stringResource(R.string.journey_yoy_title),
                lockedDescription = stringResource(R.string.journey_yoy_locked_desc),
                onUpgradeClick    = onUpgradeTapped,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text          = stringResource(R.string.journey_yoy_title).uppercase(),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = colors.textSecondary,
                        letterSpacing = 0.8.sp,
                    )
                    YearOverYearContent(
                        yoy              = yoy,
                        year             = state.year,
                        currentYearBooks = state.stats.booksFinished,
                    )
                }
            }
        }

      PercentileBenchmarkCard(
            booksThisYear   = state.stats.booksFinished,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        PersonalRecordsInsightCard(
            longestStreak    = state.stats.longestStreak,
            fastestReadTitle = state.fastestReadTitle,
            fastestReadDays  = state.fastestReadDays,
            bestReadingMonth = state.bestReadingMonth,
            monthlyProgress  = state.stats.monthlyProgress,
            isPremium        = isPremium,
            onUpgradeTapped  = onUpgradeTapped,
        )

        // TD-98 — Streak Intelligence
        StreakIntelligenceCard(
            sessionDayOfWeekCounts = state.sessionDayOfWeekCounts,
            isPremium              = isPremium,
            onUpgradeTapped        = onUpgradeTapped,
        )

        // TD-99 — Annual Goal Coaching
        goalProgress?.let { gp ->
            AnnualGoalCoachingCard(
                booksFinished   = state.stats.booksFinished,
                goalTarget      = gp.goal.target,
                isPremium       = isPremium,
                onUpgradeTapped = onUpgradeTapped,
            )
        }

        // TD-100 — Pace Fingerprint by Genre
        PaceFingerprintCard(
            paceByGenre     = state.paceByGenre,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        // TD-101 — Reading Mood Cycles
        ReadingMoodCyclesCard(
            moodCycles      = state.moodCycles,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        // TD-102 — Reading DNA Timeline
        ReadingDnaTimelineCard(
            dnaTimeline     = state.dnaTimeline,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        // TD-96 — DNF Reframe Intelligence
        DnfReframeCard(
            dnfCount        = state.dnfCount,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        // TD-97 — Re-Read Insights
        ReReadInsightCard(
            reReadBooks     = state.reReadBooks,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        // TD-93 — Reader Personality Type (capstone of Section 4)
        ReaderPersonalityCard(
            booksFinished   = state.stats.booksFinished,
            dnfCount        = state.dnfCount,
            reReadCount     = state.reReadBooks.size,
            longestStreak   = state.stats.longestStreak,
            monthlyProgress = state.stats.monthlyProgress,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        // TD-103/104 — Companion Memory (emotional coda of Section 4)
        CompanionMemoryCard(
            companionType   = companionType,
            booksFinished   = state.stats.booksFinished,
            dnfCount        = state.dnfCount,
            reReadCount     = state.reReadBooks.size,
            longestStreak   = state.stats.longestStreak,
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
        )

        // TD-106 — Time-of-Day Reading Profile
        state.timeOfDayProfile?.let { profile ->
            TimeOfDayProfileCard(
                profile   = profile,
                isPremium = isPremium,
            )
        }

        // TD-107 — Books That Found Me entry card (premium collection)
        BooksFoundMeEntryCard(
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
            onTap           = onNavigateToFavorites,
        )

        // TD-109 — Reading Spine entry card (premium visual timeline)
        ReadingSpineEntryCard(
            isPremium       = isPremium,
            onUpgradeTapped = onUpgradeTapped,
            onTap           = onNavigateToReadingSpine,
        )

        // ── Section 5: Your Reading Story ─────────────────────────────────────
        // Annual Wrap-Up entry card. Always free. Always visible.

        SectionHeader(label = stringResource(R.string.journey_section_your_story))

        AnnualWrapUpEntryCard(
            year  = state.year,
            onTap = onAnnualWrapUpTapped,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header — plain (Sections 1, 2, 3, 5)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(label: String) {
    val colors = LocalLorevynColors.current
    Text(
        text          = label.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = colors.textSecondary,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(top = 8.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reading Intelligence section header — crown badge in terracotta (Decision #165)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReadingIntelligenceSectionHeader() {
    val colors = LocalLorevynColors.current
    Row(
        modifier              = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text          = stringResource(R.string.journey_section_intelligence).uppercase(),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 1.sp,
        )
        // Crown in terracotta — premium aspiration, not a padlock (Decision #165)
        Text(
            text     = "\u265B",
            fontSize = 16.sp,
            color    = colors.terracotta,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Challenges placeholder — Group 6 will replace with full system
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChallengesPlaceholderCard() {
    val colors = LocalLorevynColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text          = stringResource(R.string.journey_challenges_card_title).uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.8.sp,
        )
        Text(
            text       = stringResource(R.string.journey_challenges_card_body),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = colors.textPrimary,
        )
        Text(
            text     = stringResource(R.string.journey_challenges_card_sub),
            fontSize = 13.sp,
            color    = colors.textSecondary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Annual Wrap-Up entry card — Section 5, always free (Decision #66)
// Terracotta arrow per Decision #127 (tappable non-button = right arrow in colors.terracotta)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnnualWrapUpEntryCard(
    year: Int,
    onTap: () -> Unit,
) {
    val colors = LocalLorevynColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.primaryGreen)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text       = stringResource(R.string.journey_annual_wrapup_title, year),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
                Text(
                    text    = stringResource(R.string.journey_annual_wrapup_subtitle),
                    fontSize = 13.sp,
                    color   = Color.White.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Terracotta arrow — tappable non-button affordance (Decision #127)
            Text(
                text     = "\u2192",
                fontSize = 20.sp,
                color    = colors.terracotta,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Books That Found Me entry card — Section 4, premium (Decision #172)
// Terracotta arrow per Decision #127
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BooksFoundMeEntryCard(
    isPremium: Boolean,
    onUpgradeTapped: () -> Unit,
    onTap: () -> Unit,
) {
    val colors = LocalLorevynColors.current
    PremiumGate(
        isPremium         = isPremium,
        featureTitle      = stringResource(R.string.journey_books_found_me_title),
        lockedDescription = stringResource(R.string.journey_books_found_me_locked),
        onUpgradeClick    = onUpgradeTapped,
        modifier          = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text       = stringResource(R.string.journey_books_found_me_title),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textPrimary,
                    )
                    Text(
                        text     = stringResource(R.string.journey_books_found_me_subtitle),
                        fontSize = 13.sp,
                        color    = colors.textSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text     = "\u2192",
                    fontSize = 20.sp,
                    color    = colors.terracotta,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TD-109 — Reading Spine entry card (Decision #179)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReadingSpineEntryCard(
    isPremium: Boolean,
    onUpgradeTapped: () -> Unit,
    onTap: () -> Unit,
) {
    val colors = LocalLorevynColors.current
    PremiumGate(
        isPremium         = isPremium,
        featureTitle      = stringResource(R.string.journey_spine_entry_title),
        lockedDescription = stringResource(R.string.journey_spine_locked),
        onUpgradeClick    = onUpgradeTapped,
        modifier          = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text       = stringResource(R.string.journey_spine_entry_title),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textPrimary,
                    )
                    Text(
                        text     = stringResource(R.string.journey_spine_entry_subtitle),
                        fontSize = 13.sp,
                        color    = colors.textSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text     = "\u2192",
                    fontSize = 20.sp,
                    color    = colors.terracotta,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLorevynColors.current
    Column(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement    = Arrangement.spacedBy(4.dp),
        horizontalAlignment    = Alignment.CenterHorizontally,
    ) {
        Text(
            text          = label.uppercase(),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.8.sp,
            maxLines      = 1,
            overflow      = TextOverflow.Ellipsis,
            textAlign     = TextAlign.Center,
        )
        Text(
            text       = value,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = colors.textPrimary,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Goal progress card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GoalProgressCard(goalProgress: GoalProgress) {
    val colors = LocalLorevynColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text          = stringResource(R.string.journey_goal_label).uppercase(),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = colors.textSecondary,
                letterSpacing = 0.8.sp,
            )
            Text(
                text       = "${goalProgress.current} / ${goalProgress.goal.target}",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary,
            )
        }
        LinearProgressIndicator(
            progress   = {
                (goalProgress.current.toFloat() / goalProgress.goal.target.toFloat())
                    .coerceIn(0f, 1f)
            },
            modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color      = colors.sage,
            trackColor = colors.sageTrack,
            strokeCap  = StrokeCap.Round,
        )
        if (goalProgress.isComplete) {
            Text(
                text       = stringResource(R.string.journey_goal_complete),
                fontSize   = 13.sp,
                color      = colors.sage,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Heatmap — 52 weekly cells
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeatmapCard(cells: List<Int>) {
    val colors = LocalLorevynColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text          = stringResource(R.string.journey_reading_activity).uppercase(),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = colors.textSecondary,
                letterSpacing = 0.8.sp,
            )
            Text(
                text     = stringResource(R.string.journey_last_12_months),
                fontSize = 10.sp,
                color    = colors.textSecondary,
            )
        }

        val maxPages = cells.maxOrNull()?.takeIf { it > 0 } ?: 1
        val rows = listOf(
            cells.slice(0..12),
            cells.slice(13..25),
            cells.slice(26..38),
            cells.slice(39..51),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { rowCells ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rowCells.forEach { pages ->
                        val cellColor = when {
                            pages == 0               -> colors.sage.copy(alpha = 0.08f)
                            pages < maxPages * 0.33f -> colors.sage.copy(alpha = 0.25f)
                            pages < maxPages * 0.66f -> colors.sage.copy(alpha = 0.55f)
                            else                     -> colors.sage.copy(alpha = 0.90f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cellColor)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monthly bar chart — 12 months, books finished per month
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyChartCard(
    monthlyProgress: List<MonthlyProgress>,
    bestReadingMonth: Int?,
) {
    val colors = LocalLorevynColors.current
    val monthLabels = DateFormatSymbols.getInstance().shortMonths.take(12).map { it.take(1) }
    val maxBooks = monthlyProgress.maxOfOrNull { it.booksFinished }.takeIf { it != null && it > 0 } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text          = stringResource(R.string.journey_monthly_chart_title).uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.8.sp,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            drawMonthlyBars(
                monthlyProgress = monthlyProgress,
                maxBooks        = maxBooks,
                bestMonth       = bestReadingMonth,
                barColor        = colors.sage,
                bestBarColor    = colors.terracotta,
                trackColor      = colors.sage.copy(alpha = 0.08f),
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            monthLabels.forEachIndexed { index, label ->
                val month = index + 1
                Text(
                    text       = label,
                    fontSize   = 9.sp,
                    color      = if (month == bestReadingMonth) colors.terracotta else colors.textSecondary,
                    fontWeight = if (month == bestReadingMonth) FontWeight.Bold else FontWeight.Normal,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.weight(1f),
                )
            }
        }

        bestReadingMonth?.let { bestMonth ->
            val bestData = monthlyProgress.getOrNull(bestMonth - 1)
            if (bestData != null && bestData.booksFinished > 0) {
                val monthName = DateFormatSymbols.getInstance().months.getOrElse(bestMonth - 1) { "" }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.terracotta.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.journey_best_month_callout,
                            monthName,
                            bestData.booksFinished,
                        ),
                        fontSize   = 13.sp,
                        color      = colors.terracotta,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMonthlyBars(
    monthlyProgress: List<MonthlyProgress>,
    maxBooks: Int,
    bestMonth: Int?,
    barColor: Color,
    bestBarColor: Color,
    trackColor: Color,
) {
    val barCount = monthlyProgress.size
    if (barCount == 0) return

    val totalWidth  = size.width
    val totalHeight = size.height
    val barWidth    = (totalWidth / barCount) * 0.55f
    val gap         = (totalWidth / barCount) * 0.45f

    monthlyProgress.forEachIndexed { index, data ->
        val month = data.month
        val x     = index * (barWidth + gap) + gap / 2

        drawRect(
            color   = trackColor,
            topLeft = Offset(x, 0f),
            size    = Size(barWidth, totalHeight),
        )

        val barHeight = (data.booksFinished.toFloat() / maxBooks) * totalHeight
        if (barHeight > 0f) {
            val color = if (month == bestMonth) bestBarColor else barColor
            drawRect(
                color   = color,
                topLeft = Offset(x, totalHeight - barHeight),
                size    = Size(barWidth, barHeight),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top authors (free)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopAuthorsCard(authors: List<Pair<String, Int>>) {
    val colors = LocalLorevynColors.current
    val maxCount = authors.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text          = stringResource(R.string.journey_top_authors_title).uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.8.sp,
        )

        authors.forEachIndexed { index, (name, count) ->
            AuthorRow(
                name     = name,
                count    = count,
                maxCount = maxCount,
                isTop    = index == 0,
            )
        }
    }
}

@Composable
private fun AuthorRow(
    name: String,
    count: Int,
    maxCount: Int,
    isTop: Boolean,
) {
    val colors = LocalLorevynColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = name,
                fontSize   = 14.sp,
                color      = if (isTop) colors.textPrimary else colors.textSecondary,
                fontWeight = if (isTop) FontWeight.SemiBold else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text       = stringResource(R.string.journey_books_count, count),
                fontSize   = 12.sp,
                color      = colors.textSecondary,
                fontWeight = FontWeight.Medium,
            )
        }
        LinearProgressIndicator(
            progress   = { count.toFloat() / maxCount },
            modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color      = if (isTop) colors.terracotta else colors.sage,
            trackColor = colors.sageTrack,
            strokeCap  = StrokeCap.Round,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rating distribution — 0.5-star histogram (free)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RatingDistributionCard(distribution: Map<Float, Int>) {
    val colors = LocalLorevynColors.current
    val buckets  = listOf(5.0f, 4.5f, 4.0f, 3.5f, 3.0f, 2.5f, 2.0f, 1.5f, 1.0f)
    val maxCount = distribution.values.maxOrNull()?.takeIf { it > 0 } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text          = stringResource(R.string.journey_rating_distribution_title).uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.8.sp,
        )

        buckets.forEach { bucket ->
            val count = distribution[bucket] ?: 0
            if (count > 0 || bucket >= 3.0f) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text      = "%.1f".format(bucket),
                        fontSize  = 11.sp,
                        color     = colors.textSecondary,
                        modifier  = Modifier.width(28.dp),
                        textAlign = TextAlign.End,
                    )
                    LinearProgressIndicator(
                        progress   = { count.toFloat() / maxCount },
                        modifier   = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color      = when {
                            bucket >= 4.5f -> colors.terracotta
                            bucket >= 3.5f -> colors.sage
                            else           -> colors.textSecondary
                        },
                        trackColor = colors.sageTrack,
                        strokeCap  = StrokeCap.Round,
                    )
                    Text(
                        text      = if (count > 0) count.toString() else "",
                        fontSize  = 11.sp,
                        color     = colors.textSecondary,
                        modifier  = Modifier.width(20.dp),
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reading records — fastest + slowest (free)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReadingRecordsCard(
    fastestReadTitle: String?,
    fastestReadDays: Int?,
    slowestReadTitle: String?,
    slowestReadDays: Int?,
) {
    val colors = LocalLorevynColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text          = stringResource(R.string.journey_reading_records_title).uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.8.sp,
        )

        fastestReadTitle?.let { title ->
            RecordRow(
                label = stringResource(R.string.journey_fastest_read),
                title = title,
                days  = fastestReadDays,
                color = colors.terracotta,
            )
        }

        if (fastestReadTitle != null && slowestReadTitle != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.textSecondary.copy(alpha = 0.15f))
            )
        }

        slowestReadTitle?.let { title ->
            RecordRow(
                label = stringResource(R.string.journey_slowest_read),
                title = title,
                days  = slowestReadDays,
                color = colors.sage,
            )
        }
    }
}

@Composable
private fun RecordRow(
    label: String,
    title: String,
    days: Int?,
    color: Color,
) {
    val colors = LocalLorevynColors.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.10f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text       = label,
                fontSize   = 10.sp,
                color      = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                fontSize   = 13.sp,
                color      = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
            days?.let {
                Text(
                    text     = stringResource(R.string.journey_days_label, it),
                    fontSize = 11.sp,
                    color    = colors.textSecondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Genre breakdown content (shown inside PremiumGate previewContent)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GenreBreakdownContent(distribution: Map<String, Int>) {
    val colors = LocalLorevynColors.current
    val sorted    = distribution.entries.sortedByDescending { it.value }
    val top5      = sorted.take(5)
    val maxCount  = top5.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1
    val remainder = sorted.size - 5

    top5.forEach { (genre, count) ->
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text     = genre.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                color    = colors.textSecondary,
                modifier = Modifier.width(90.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress   = { count.toFloat() / maxCount },
                modifier   = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color      = colors.sage,
                trackColor = colors.sageTrack,
                strokeCap  = StrokeCap.Round,
            )
            Text(
                text      = count.toString(),
                fontSize  = 11.sp,
                color     = colors.textSecondary,
                modifier  = Modifier.width(20.dp),
                textAlign = TextAlign.End,
            )
        }
    }

    if (remainder > 0) {
        Text(
            text     = stringResource(R.string.journey_genre_more, remainder),
            fontSize = 11.sp,
            color    = colors.textSecondary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mood distribution content (shown inside PremiumGate previewContent)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoodDistributionContent(distribution: Map<MoodTag, Int>) {
    val colors = LocalLorevynColors.current
    val top5     = distribution.entries.sortedByDescending { it.value }.take(5)
    val maxCount = top5.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1

    top5.forEach { (mood, count) ->
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text     = mood.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                color    = colors.textSecondary,
                modifier = Modifier.width(90.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress   = { count.toFloat() / maxCount },
                modifier   = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color      = colors.terracotta,
                trackColor = colors.terracotta.copy(alpha = 0.10f),
                strokeCap  = StrokeCap.Round,
            )
            Text(
                text      = count.toString(),
                fontSize  = 11.sp,
                color     = colors.textSecondary,
                modifier  = Modifier.width(20.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Year-over-year content (shown inside PremiumGate previewContent)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun YearOverYearContent(
    yoy: com.lorevyn.core.domain.model.YearOverYearStats,
    year: Int,
    currentYearBooks: Int,
) {
    val colors = LocalLorevynColors.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = (year - 1).toString(), fontSize = 11.sp, color = colors.textSecondary)
            Text(
                text       = yoy.previousYear.toString(),
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.textSecondary,
            )
            Text(
                text     = stringResource(R.string.journey_books_count, yoy.previousYear),
                fontSize = 11.sp,
                color    = colors.textSecondary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primaryGreen)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = year.toString(), fontSize = 11.sp, color = Color.White.copy(alpha = 0.70f))
            Text(
                text       = currentYearBooks.toString(),
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
            Text(
                text     = stringResource(R.string.journey_books_count, currentYearBooks),
                fontSize = 11.sp,
                color    = Color.White.copy(alpha = 0.70f),
            )
        }
    }

    val deltaText = when {
        yoy.delta > 0 -> stringResource(R.string.journey_yoy_more, yoy.delta)
        yoy.delta < 0 -> stringResource(R.string.journey_yoy_fewer, -yoy.delta)
        else          -> stringResource(R.string.journey_yoy_same)
    }
    val deltaColor = when {
        yoy.delta > 0 -> colors.sage
        yoy.delta < 0 -> colors.terracotta
        else          -> colors.textSecondary
    }
    Text(
        text       = deltaText,
        fontSize   = 13.sp,
        color      = deltaColor,
        fontWeight = FontWeight.Medium,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reading Fingerprint entry card (free)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FingerprintCard(
    available: Boolean,
    booksFinished: Int,
    onTap: () -> Unit,
) {
    val colors = LocalLorevynColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(if (available) colors.primaryGreen else colors.surface)
            .then(if (available) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        if (available) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text       = stringResource(R.string.journey_fingerprint_title),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
                Text(
                    text    = stringResource(R.string.journey_fingerprint_subtitle),
                    fontSize = 13.sp,
                    color   = Color.White.copy(alpha = 0.75f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = stringResource(R.string.journey_fingerprint_cta),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.sage,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text       = stringResource(R.string.journey_fingerprint_title),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.textPrimary,
                )
                val booksNeeded = 5 - booksFinished
                Text(
                    text    = stringResource(R.string.journey_fingerprint_locked, booksNeeded),
                    fontSize = 13.sp,
                    color   = colors.textSecondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatPages(pages: Int): String = when {
    pages >= 1000 -> "${pages / 1000}.${(pages % 1000) / 100}k"
    else          -> pages.toString()
}

private fun formatHours(minutes: Int): String {
    val hours = minutes / 60
    return if (hours < 1) "<1h" else "${hours}h"
}

@Composable
private fun formatLabel(format: BookFormat): String = when (format) {
    val colors = LocalLorevynColors.current
    BookFormat.PHYSICAL  -> stringResource(R.string.journey_format_physical)
    BookFormat.EBOOK     -> stringResource(R.string.journey_format_ebook)
    BookFormat.AUDIOBOOK -> stringResource(R.string.journey_format_audiobook)
}
