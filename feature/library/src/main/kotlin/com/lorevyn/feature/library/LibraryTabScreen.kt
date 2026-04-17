package com.lorevyn.feature.library

// feature/library/src/main/kotlin/com/lorevyn/feature/library/LibraryTabScreen.kt
// Phase 5 — SCR-07 Library hub. Two sections: Want to Read + Finished.
// v2 — TD-62: Owned filter chip added. Filters both WantToRead and Finished lists.
// Per D-04 Screen Specifications v1.0.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lorevyn.core.ui.LocalLorevynColors
import com.lorevyn.core.domain.model.Genre
import com.lorevyn.feature.library.R
import kotlinx.coroutines.flow.collectLatest

// ── Design tokens ─────────────────────────────────────────────────────────────
// Legacy private color constants removed — colors now read from LocalLorevynColors.current

// ── Genre display name mapping ───────────────────────────────────────────────
// Maps Genre enum names to readable display labels.
// Filters out raw API strings that don't match our enum (e.g. "Corn-free diet").

private val VALID_GENRE_NAMES: Set<String> = Genre.entries.map { it.name }.toSet()

private fun genreDisplayName(genre: String): String = when (genre) {
    val colors = LocalLorevynColors.current
    "FICTION"            -> "Fiction"
    "NON_FICTION"        -> "Non-fiction"
    "MYSTERY"            -> "Mystery"
    "THRILLER"           -> "Thriller"
    "ROMANCE"            -> "Romance"
    "SCIENCE_FICTION"    -> "Sci-fi"
    "FANTASY"            -> "Fantasy"
    "HISTORICAL_FICTION" -> "Historical"
    "BIOGRAPHY"          -> "Biography"
    "SELF_HELP"          -> "Self-help"
    "GRAPHIC_NOVEL"      -> "Graphic novels"
    "POETRY"             -> "Poetry"
    "CHILDREN"           -> "Children"
    "YOUNG_ADULT"        -> "Young Adult"
    "OTHER"              -> "Other"
    else                 -> genre
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun LibraryTabScreen(
    onNavigateToBookDetail: (bookId: Long) -> Unit = {},
    onNavigateToWantToRead: () -> Unit = {},
    onNavigateToFinished: () -> Unit = {},
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToSeriesTracker: () -> Unit = {},
    onNavigateToSeriesPaywall: () -> Unit = {},
    onNavigateToManageShelves: () -> Unit = {},
    isPremium: Boolean = false,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val colors = LocalLorevynColors.current
    val wantToRead by viewModel.wantToReadBooks.collectAsStateWithLifecycle()
    val finished by viewModel.finishedBooks.collectAsStateWithLifecycle()
    val seriesSummary by viewModel.seriesSummary.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var ownedOnly by remember { mutableStateOf(false) }
    val genres by viewModel.genres.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                is LibraryNavEvent.OpenBookDetail     -> onNavigateToBookDetail(event.bookId)
                is LibraryNavEvent.OpenWantToRead     -> onNavigateToWantToRead()
                is LibraryNavEvent.OpenFinished       -> onNavigateToFinished()
                is LibraryNavEvent.OpenAddBook        -> onNavigateToAddBook()
                is LibraryNavEvent.OpenSeriesTracker  -> onNavigateToSeriesTracker()
                is LibraryNavEvent.OpenPaywallSeries  -> onNavigateToSeriesPaywall()
            }
        }
    }

    val filteredWantToRead = wantToRead
        .let { list ->
            if (ownedOnly) list.filter { it.isOwned } else list
        }
        .let { list ->
            if (selectedGenre != null)
                list.filter { it.genre?.equals(selectedGenre, ignoreCase = true) == true }
            else list
        }
        .let { list ->
            if (searchQuery.length >= 2)
                list.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.author.contains(searchQuery, ignoreCase = true) ||
                    (it.genre?.contains(searchQuery, ignoreCase = true) == true)
                }
            else list
        }

    val filteredFinished = finished
        .let { list ->
            if (ownedOnly) list.filter { it.isOwned } else list
        }
        .let { list ->
            if (selectedGenre != null)
                list.filter { it.genre?.equals(selectedGenre, ignoreCase = true) == true }
            else list
        }
        .let { list ->
            if (searchQuery.length >= 2)
                list.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.author.contains(searchQuery, ignoreCase = true) ||
                    (it.genre?.contains(searchQuery, ignoreCase = true) == true)
                }
            else list
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Search bar ────────────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.library_search_hint),
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.sage,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primaryGreen
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Filter chips — Owned always first, then genre chips ───────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Owned chip — always visible regardless of genre list state
                FilterChip(
                    selected = ownedOnly,
                    onClick = { ownedOnly = !ownedOnly },
                    label = {
                        Text(
                            text = stringResource(R.string.library_filter_owned),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryGreen,
                        selectedLabelColor = Color.White,
                        containerColor = colors.surface,
                        labelColor = colors.textSecondary
                    )
                )

                // Genre chips — only valid Genre enum values shown (raw API strings filtered out)
                genres.filter { it in VALID_GENRE_NAMES }.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = {
                            selectedGenre = if (selectedGenre == genre) null else genre
                        },
                        label = {
                            Text(
                                text = genreDisplayName(genre),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryGreen,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surface,
                            labelColor = colors.textSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Shelves entry row — SCR-07d ───────────────────────────────────────
        // Compact chip-style row. Keeps vertical footprint minimal (Scott: reduce
        // scrolling where possible). Always visible so free users discover
        // custom shelves before hitting a gate.
        item {
            ShelvesEntryRow(onTap = onNavigateToManageShelves)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Want to Read section ──────────────────────────────────────────────
        item {
            SectionHeader(
                title = stringResource(R.string.library_section_want_to_read),
                count = wantToRead.size,
                onSeeAll = { viewModel.onSeeAllWantToRead() }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            if (filteredWantToRead.isEmpty()) {
                LibraryEmptyState(
                    message = stringResource(R.string.library_empty_want_to_read)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(filteredWantToRead.take(10), key = { it.bookId }) { book ->
                        WantToReadCoverCard(
                            book = book,
                            onClick = { viewModel.onBookTapped(book.bookId) }
                        )
                    }
                }
                if (wantToRead.size > 10) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.library_see_all) + " ›",
                        fontSize = 13.sp,
                        color = colors.terracotta,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onSeeAllWantToRead() }
                            .padding(vertical = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Series Tracker entry card — TD-111 ────────────────────────────────
        // Premium-gated. Free users see aspirational teaser → paywall.
        // Hidden entirely when the user has zero multi-book series tagged
        // (no useful content yet — surfacing it early would just clutter).
        if (seriesSummary.distinctSeriesCount > 0 || !isPremium) {
            item {
                SeriesTrackerEntryCard(
                    summary            = seriesSummary,
                    isPremium          = isPremium,
                    onPremiumTap       = { viewModel.onSeriesTrackerTapped() },
                    onLockedTap        = { viewModel.onSeriesTrackerPaywallTapped() },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Finished section ──────────────────────────────────────────────────
        item {
            SectionHeader(
                title = stringResource(R.string.library_section_finished),
                count = finished.size,
                onSeeAll = { viewModel.onSeeAllFinished() }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (filteredFinished.isEmpty()) {
            item {
                LibraryEmptyState(
                    message = stringResource(R.string.library_empty_finished)
                )
            }
        } else {
            items(filteredFinished.take(5), key = { it.bookId }) { book ->
                FinishedBookRow(
                    book = book,
                    onClick = { viewModel.onBookTapped(book.bookId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (finished.size > 5) {
                item {
                    Text(
                        text = stringResource(R.string.library_see_all) + " ›",
                        fontSize = 13.sp,
                        color = colors.terracotta,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onSeeAllFinished() }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    onSeeAll: () -> Unit
) {
    val colors = LocalLorevynColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.clickable { onSeeAll() }
        ) {
            Text(
                text = stringResource(R.string.library_see_all),
                fontSize = 13.sp,
                color = colors.terracotta,
                fontWeight = FontWeight.Medium
            )
            Text(text = "›", fontSize = 15.sp, color = colors.terracotta, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Want to Read cover card ───────────────────────────────────────────────────

@Composable
private fun WantToReadCoverCard(
    book: LibraryBookUi,
    onClick: () -> Unit
) {
    val colors = LocalLorevynColors.current
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 116.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.textSecondary.copy(alpha = 0.12f))
            )
            FormatBadge(format = book.format)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = book.title,
            fontSize = 10.sp,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ── Finished book row ─────────────────────────────────────────────────────────

@Composable
private fun FinishedBookRow(
    book: LibraryBookUi,
    onClick: () -> Unit
) {
    val colors = LocalLorevynColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover with optional DNF badge + format badge
        Box {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 44.dp, height = 64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.textSecondary.copy(alpha = 0.12f))
            )
            FormatBadge(format = book.format)
            // DNF badge — terracotta, bottom-left of cover
            if (book.isDnf) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(topEnd = 4.dp))
                        .background(colors.terracotta)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = stringResource(R.string.library_dnf_badge),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (book.author.isNotBlank()) {
                Text(
                    text = book.author,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = colors.sage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            book.dateFinishedFormatted?.let { date ->
                Text(
                    text = date,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Star rating + tappable affordance chevron
        Spacer(modifier = Modifier.width(8.dp))
        book.rating?.let { rating ->
            StarRatingDisplay(rating = rating)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = "›", fontSize = 18.sp, color = colors.terracotta)
    }
}

// ── Star rating display (read-only) ──────────────────────────────────────────

@Composable
private fun StarRatingDisplay(rating: Float) {
    val colors = LocalLorevynColors.current
    Row {
        repeat(5) { index ->
            val starValue = index + 1
            Text(
                text = if (rating >= starValue) "★" else if (rating >= starValue - 0.5f) "½" else "☆",
                fontSize = 13.sp,
                color = if (rating >= starValue - 0.5f) colors.terracotta else colors.textSecondary
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun LibraryEmptyState(message: String) {
    val colors = LocalLorevynColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BookmarkBorder,
            contentDescription = null,
            tint = colors.sage.copy(alpha = 0.35f),
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = message,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ── Format badge — TD-128 ─────────────────────────────────────────────────────
// Decision #187: E-Book = sage pill (top-right). Audio = terracotta pill (top-right).
// BOOK = no badge. Must be called inside a Box {} scope.

// ── Series Tracker entry card — TD-111 ───────────────────────────────────────
// One row, compact. Premium → tappable card with current top-series snapshot.
// Free → aspirational card with upgrade CTA. No scroll noise.

@Composable
private fun SeriesTrackerEntryCard(
    summary: com.lorevyn.feature.library.LibrarySeriesSummary,
    isPremium: Boolean,
    onPremiumTap: () -> Unit,
    onLockedTap: () -> Unit,
) {
    val colors = LocalLorevynColors.current
    val subtitle: String = when {
        !isPremium && summary.distinctSeriesCount == 0 ->
            stringResource(R.string.library_series_entry_locked_empty)
        !isPremium ->
            stringResource(
                R.string.library_series_entry_locked_with_data,
                summary.distinctSeriesCount
            )
        summary.topSeriesName != null && summary.topSeriesTotal != null ->
            stringResource(
                R.string.library_series_entry_with_fraction,
                summary.topSeriesName,
                summary.topSeriesReadCount,
                summary.topSeriesTotal,
            )
        summary.topSeriesName != null ->
            stringResource(
                R.string.library_series_entry_of_owned,
                summary.topSeriesName,
                summary.topSeriesReadCount,
                summary.topSeriesOwnedCount,
            )
        else ->
            stringResource(R.string.library_series_entry_empty_premium)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable { if (isPremium) onPremiumTap() else onLockedTap() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Terracotta crown when locked (Decision #165), sage bookmark when unlocked
        Text(
            text = if (isPremium) "\uD83D\uDCDA" else "\u265B",
            fontSize = 22.sp,
            color = if (isPremium) colors.sage else colors.terracotta,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.library_series_entry_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = colors.terracotta,
        )
    }
}

// ── Shelves entry row — SCR-07d ───────────────────────────────────────────────
// Single compact row (~44dp). Low-footprint entry to Manage Shelves.
@Composable
private fun ShelvesEntryRow(onTap: () -> Unit) {
    val colors = LocalLorevynColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CollectionsBookmark,
            contentDescription = null,
            tint = colors.sage,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.library_shelves_entry_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(text = "›", fontSize = 18.sp, color = colors.terracotta)
    }
}

@Composable
private fun BoxScope.FormatBadge(format: String) {
    val colors = LocalLorevynColors.current
    val (label, bgColor) = when (format.uppercase()) {
        "E_BOOK" -> "E-Book" to colors.sage
        "AUDIO"  -> "Audio"  to colors.terracotta
        else     -> return
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .clip(RoundedCornerShape(bottomStart = 4.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
