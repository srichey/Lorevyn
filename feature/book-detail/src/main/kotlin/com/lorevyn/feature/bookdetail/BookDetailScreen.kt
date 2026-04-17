package com.lorevyn.feature.bookdetail

// feature/book-detail/src/main/kotlin/com/lorevyn/feature/bookdetail/BookDetailScreen.kt
// Phase 4 — SCR-09 Book Detail. Per D-04 Screen Specifications v1.0.
// Phase 8 fix: Start Reading / Resume Reading buttons wired to onStatusChanged()
// Phase 9 pre: Secondary status row + DNF confirm bottom sheet
// Phase 9 post: Delete book (TD-41) — Remove from library with confirm sheet
// Phase 10: Reading stats section — duration, pace, fastest/slowest badge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lorevyn.core.ui.LocalLorevynColors
import com.lorevyn.core.domain.model.BookFormat
import com.lorevyn.core.domain.model.Genre
import com.lorevyn.core.domain.model.ReadStatus
import com.lorevyn.core.ui.components.RatingBar
import com.lorevyn.feature.bookdetail.R
import kotlinx.coroutines.flow.collect

// ── Imports added for dark mode token system ──────────────────────────────────
// All color usage now reads from LocalLorevynColors.current
// Legacy private val constants removed — Decision #181

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun BookDetailScreen(
    onNavigateBack: () -> Unit,
    isPremium: Boolean,
    onNavigateToReading: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToLog: (bookId: Long) -> Unit = {},
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val colors = LocalLorevynColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val distinctSeriesNames by viewModel.distinctSeriesNames.collectAsStateWithLifecycle()

    var showDnfSheet    by remember { mutableStateOf(false) }
    var showDeleteSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is BookDetailNavEvent.NavigateBack      -> onNavigateBack()
                is BookDetailNavEvent.NavigateToReading -> onNavigateToReading()
                is BookDetailNavEvent.NavigateToLibrary -> onNavigateToLibrary()
            }
        }
    }

    // TD-111 Phase B: series "next book" offer snackbar
    val snackbarAddLabel = stringResource(R.string.book_detail_next_in_series_action)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is BookDetailUiEvent.OfferNextInSeries -> {
                    val message = "${event.seriesName} #${event.nextPosition}?"
                    val result = snackbarHostState.showSnackbar(
                        message        = message,
                        actionLabel    = snackbarAddLabel,
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onAddNextInSeries(event.seriesName, event.nextPosition)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost   = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    containerColor  = colors.primaryGreen,
                    contentColor    = Color.White,
                    actionColor     = Color(0xFFE8D4AA)
                )
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is BookDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.sage)
                }
            }

            is BookDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = stringResource(R.string.book_detail_not_found),
                        color    = colors.textSecondary,
                        fontSize = 15.sp
                    )
                }
            }

            is BookDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = state.message,
                        color    = colors.textSecondary,
                        fontSize = 15.sp
                    )
                }
            }

            is BookDetailUiState.Ready -> {
                BookDetailContent(
                    book              = state.book,
                    isPremium         = isPremium,
                    onBack            = { viewModel.onBackPressed() },
                    onRatingChanged   = viewModel::onRatingChanged,
                    onNotesChanged    = viewModel::onNotesChanged,
                    onFavoriteToggled = viewModel::onFavoriteToggled,
                    onLogProgress     = { onNavigateToLog(state.book.bookId) },
                    onStatusChanged   = viewModel::onStatusChanged,
                    onMarkFinished    = viewModel::onMarkFinished,
                    onShowDnfSheet    = { showDnfSheet = true },
                    onPauseReading    = viewModel::onPauseReading,
                    onShowDeleteSheet = { showDeleteSheet = true },
                    onPageCountChanged  = viewModel::onPageCountChanged,
                    onGenreChanged      = viewModel::onGenreChanged,
                    onFormatChanged     = viewModel::onFormatChanged,
                    onReadAgain         = viewModel::onReadAgain,
                    distinctSeriesNames = distinctSeriesNames,
                    onSeriesChanged     = viewModel::onSeriesChanged,
                    onClearSeries       = viewModel::onClearSeries,
                    modifier            = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // ── DNF Confirm sheet ─────────────────────────────────────────────────────
    if (showDnfSheet) {
        DnfConfirmSheet(
            onConfirm = { note ->
                showDnfSheet = false
                viewModel.onMarkDnf(note)
            },
            onDismiss = { showDnfSheet = false }
        )
    }

    // ── Delete Confirm sheet ──────────────────────────────────────────────────
    if (showDeleteSheet) {
        DeleteConfirmSheet(
            onConfirm = {
                showDeleteSheet = false
                viewModel.onDeleteBook()
            },
            onDismiss = { showDeleteSheet = false }
        )
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun BookDetailContent(
    book: BookDetailUi,
    isPremium: Boolean,
    onBack: () -> Unit,
    onRatingChanged: (Float) -> Unit,
    onNotesChanged: (String) -> Unit,
    onFavoriteToggled: () -> Unit,
    onLogProgress: () -> Unit,
    onStatusChanged: (ReadStatus) -> Unit,
    onMarkFinished: () -> Unit,
    onShowDnfSheet: () -> Unit,
    onPauseReading: () -> Unit,
    onShowDeleteSheet: () -> Unit,
    onPageCountChanged: (Int) -> Unit,
    onGenreChanged: (String) -> Unit,
    onFormatChanged: (BookFormat) -> Unit,
    onReadAgain: () -> Unit,
    // TD-111: Series entry
    distinctSeriesNames: List<String>,
    onSeriesChanged: (String?, Double?) -> Unit,
    onClearSeries: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLorevynColors.current
    var showFavoriteGate by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        BookDetailHeader(
            isFavorite        = book.isFavorite,
            isPremium         = isPremium,
            onBack            = onBack,
            onFavoriteToggled = {
                if (isPremium) onFavoriteToggled()
                else showFavoriteGate = true
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── Cover + format badge ───────────────────────────────────────────
            Box(contentAlignment = Alignment.BottomStart) {
                AsyncImage(
                    model              = book.coverUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .size(width = 120.dp, height = 180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.textSecondary.copy(alpha = 0.12f))
                )
                // Format badge — EBOOK and AUDIOBOOK only. Physical is default, no badge needed.
                when (book.format) {
                    BookFormat.EBOOK -> FormatBadge(
                        label = stringResource(R.string.book_detail_format_ebook_badge),
                        color = colors.sage,
                    )
                    BookFormat.AUDIOBOOK -> FormatBadge(
                        label = stringResource(R.string.book_detail_format_audiobook_badge),
                        color = colors.terracotta,
                    )
                    else -> Unit
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Title ──────────────────────────────────────────────────────────
            Text(
                text       = book.title,
                fontSize   = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center,
                lineHeight = 28.sp,
                maxLines   = 3,
                overflow   = TextOverflow.Ellipsis
            )

            // ── Subtitle ───────────────────────────────────────────────────────
            book.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text      = subtitle,
                    fontSize  = 14.sp,
                    color     = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )
            }

            // ── Authors ────────────────────────────────────────────────────────
            if (book.authors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text      = book.authors.joinToString(", "),
                    fontSize  = 15.sp,
                    fontStyle = FontStyle.Italic,
                    color     = colors.sage,
                    textAlign = TextAlign.Center,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )
            }

            // ── Genre · Format · Year ──────────────────────────────────────────
            val meta = listOfNotNull(
                book.genre,
                book.format.name.lowercase().replaceFirstChar { it.uppercase() },
                book.publishYear?.toString()
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text      = meta,
                    fontSize  = 12.sp,
                    color     = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Status pill ────────────────────────────────────────────────────
            StatusPill(status = book.readStatus)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Progress bar (READING / RE_READING only) ───────────────────────
            if ((book.readStatus == ReadStatus.READING ||
                 book.readStatus == ReadStatus.RE_READING) &&
                book.pageCount != null) {
                ReadingProgressBar(
                    currentPage     = book.currentPage,
                    pageCount       = book.pageCount,
                    progressPercent = book.progressPercent
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Page count input (READING, no page count set) ──────────────────
            if ((book.readStatus == ReadStatus.READING ||
                 book.readStatus == ReadStatus.RE_READING) &&
                book.pageCount == null) {
                Spacer(modifier = Modifier.height(8.dp))
                PageCountSection(onPageCountChanged = onPageCountChanged)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Primary CTA ────────────────────────────────────────────────────
            PrimaryCta(
                status          = book.readStatus,
                onLogProgress   = onLogProgress,
                onStatusChanged = onStatusChanged
            )

            // ── Secondary status row (READING only) ────────────────────────────
            if (book.readStatus == ReadStatus.READING) {
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryStatusRow(
                    onMarkFinished = onMarkFinished,
                    onShowDnfSheet = onShowDnfSheet,
                    onPauseReading = onPauseReading
                )
            }

            // ── Read Again (READ or DNF only) ──────────────────────────────────
            // Increments times_read, sets status back to READING, navigates to Reading tab.
            if (book.readStatus == ReadStatus.READ || book.readStatus == ReadStatus.DNF) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.sage.copy(alpha = 0.12f))
                        .clickable { onReadAgain() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = stringResource(R.string.book_detail_read_again),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.primaryGreen,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Genre editor ───────────────────────────────────────────────────
            GenreSection(
                currentGenre   = book.genre,
                onGenreChanged = onGenreChanged
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Format selector ────────────────────────────────────────────────
            FormatSection(
                currentFormat   = book.format,
                onFormatChanged = onFormatChanged,
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Series editor (TD-111) ─────────────────────────────────────────
            SeriesSection(
                currentSeriesName     = book.seriesName,
                currentSeriesPosition = book.seriesPosition,
                seriesTotal           = book.seriesTotal,
                distinctSeriesNames   = distinctSeriesNames,
                onSeriesChanged       = onSeriesChanged,
                onClearSeries         = onClearSeries
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Rating ─────────────────────────────────────────────────────────
            RatingSection(
                rating          = book.rating,
                onRatingChanged = onRatingChanged
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Notes ──────────────────────────────────────────────────────────
            NotesSection(
                notes          = book.notes,
                onNotesChanged = onNotesChanged
            )

            // ── DNF note display (DNF status only) ─────────────────────────────
            if (book.readStatus == ReadStatus.DNF && !book.dnfNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
                Spacer(modifier = Modifier.height(20.dp))
                DnfNoteSection(dnfNote = book.dnfNote)
            }

            // ── Reading stats (READ books with valid dates) — Phase 10 ─────────
            if (book.readStatus == ReadStatus.READ &&
                (book.readingDays != null || book.isFastestRead || book.isSlowestRead)) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
                Spacer(modifier = Modifier.height(20.dp))
                ReadingStatsSection(book = book)
            }

            // ── Description ────────────────────────────────────────────────────
            if (!book.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.20f))
                Spacer(modifier = Modifier.height(20.dp))
                DescriptionSection(description = book.description)
            }

            // ── Remove from library ────────────────────────────────────────────
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text           = "Remove from library",
                fontSize       = 13.sp,
                color          = colors.terracotta.copy(alpha = 0.80f),
                textDecoration = TextDecoration.Underline,
                textAlign      = TextAlign.Center,
                modifier       = Modifier
                    .fillMaxWidth()
                    .clickable { onShowDeleteSheet() }
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Premium gate sheet for favorite toggle (free users only) ─────────────
    if (showFavoriteGate) {
        FavoriteGateSheet(onDismiss = { showFavoriteGate = false })
    }
}

// ── Phase 10: Reading stats section ───────────────────────────────────────────

@Composable
private fun ReadingStatsSection(book: BookDetailUi) {
    val colors = LocalLorevynColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text          = stringResource(R.string.book_detail_reading_stats_title),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Duration + pace row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            book.readingDays?.let { days ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text      = stringResource(R.string.book_detail_read_in_days, days),
                            fontSize  = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color     = colors.textPrimary
                        )
                        Text(
                            text     = stringResource(R.string.book_detail_duration_label),
                            fontSize = 11.sp,
                            color    = colors.textSecondary
                        )
                    }
                }
            }

            book.pacePerDay?.let { pace ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text       = stringResource(R.string.book_detail_pace_per_day, pace),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = colors.textPrimary
                        )
                        Text(
                            text     = stringResource(R.string.book_detail_pace_label),
                            fontSize = 11.sp,
                            color    = colors.textSecondary
                        )
                    }
                }
            }
        }

        // Personal record badges
        if (book.isFastestRead || book.isSlowestRead) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (book.isFastestRead) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.terracotta.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = stringResource(R.string.book_detail_fastest_badge),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = colors.terracotta
                        )
                    }
                }
                if (book.isSlowestRead) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.sage.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = stringResource(R.string.book_detail_slowest_badge),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = colors.sage
                        )
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun BookDetailHeader(
    isFavorite: Boolean,
    isPremium: Boolean,
    onBack: () -> Unit,
    onFavoriteToggled: () -> Unit
) {
    val colors = LocalLorevynColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E0E0E))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint               = Color.White
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onFavoriteToggled) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite
                              else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.book_detail_favorite_remove
                    else R.string.book_detail_favorite_add
                ),
                tint = when {
                    isFavorite -> colors.terracotta
                    isPremium  -> Color.White.copy(alpha = 0.70f)
                    else       -> Color.White.copy(alpha = 0.40f)
                }
            )
        }
    }
}

// ── Status pill ───────────────────────────────────────────────────────────────

@Composable
private fun StatusPill(status: ReadStatus) {
    val colors = LocalLorevynColors.current
    val (label, bg, textColor) = when (status) {
        ReadStatus.READING      -> Triple("Reading",        colors.sage,  Color.White)
        ReadStatus.READ         -> Triple("Finished",       colors.primaryGreen,  Color.White)
        ReadStatus.WANT_TO_READ -> Triple("Want to Read",   Color.Transparent, colors.primaryGreen)
        ReadStatus.DNF          -> Triple("Did Not Finish", Color.Transparent, colors.terracotta)
        ReadStatus.RE_READING   -> Triple("Re-reading",     colors.sage,  Color.White)
    }
    val isOutlined = status == ReadStatus.WANT_TO_READ || status == ReadStatus.DNF

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isOutlined) Color.Transparent else bg)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = textColor
        )
    }
}

// ── Progress bar ──────────────────────────────────────────────────────────────

@Composable
private fun ReadingProgressBar(
    currentPage: Long,
    pageCount: Long,
    progressPercent: Float
) {
    val colors = LocalLorevynColors.current
    val animatedProgress by animateFloatAsState(
        targetValue   = progressPercent,
        animationSpec = tween(durationMillis = 400),
        label         = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text     = "Page $currentPage of $pageCount",
                fontSize = 13.sp,
                color    = colors.textSecondary
            )
            Text(
                text       = "${(progressPercent * 100).toInt()}%",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.primaryGreen
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress   = { animatedProgress },
            modifier   = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color      = colors.sage,
            trackColor = colors.sage.copy(alpha = 0.15f),
            strokeCap  = StrokeCap.Round
        )
    }
}

// ── Primary CTA ───────────────────────────────────────────────────────────────

@Composable
private fun PrimaryCta(
    status: ReadStatus,
    onLogProgress: () -> Unit,
    onStatusChanged: (ReadStatus) -> Unit
) {
    val colors = LocalLorevynColors.current
    val (label, containerColor, onClick) = when (status) {
        ReadStatus.READING -> Triple(
            "Log progress",
            colors.terracotta,
            onLogProgress
        )
        ReadStatus.READ -> Triple(
            "Create share card",
            colors.primaryGreen,
            ({} as () -> Unit)
        )
        ReadStatus.WANT_TO_READ -> Triple(
            "Start reading",
            colors.sage,
            ({ onStatusChanged(ReadStatus.READING) } as () -> Unit)
        )
        ReadStatus.DNF -> Triple(
            "Resume reading",
            colors.sage,
            ({ onStatusChanged(ReadStatus.READING) } as () -> Unit)
        )
        ReadStatus.RE_READING -> Triple(
            "Log progress",
            colors.terracotta,
            onLogProgress
        )
    }

    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(26.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = Color.White
        )
    ) {
        Text(
            text       = label,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Secondary status row (READING only) ──────────────────────────────────────

@Composable
private fun SecondaryStatusRow(
    onMarkFinished: () -> Unit,
    onShowDnfSheet: () -> Unit,
    onPauseReading: () -> Unit
) {
    val colors = LocalLorevynColors.current
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onMarkFinished, modifier = Modifier.weight(1f)) {
            Text(
                text      = "Finished",
                fontSize  = 12.sp,
                fontWeight = FontWeight.Medium,
                color     = colors.primaryGreen,
                textAlign = TextAlign.Center,
                maxLines  = 1
            )
        }
        Text(text = "·", fontSize = 14.sp, color = colors.textSecondary)
        TextButton(onClick = onShowDnfSheet, modifier = Modifier.weight(1.4f)) {
            Text(
                text      = "Did not finish",
                fontSize  = 12.sp,
                fontWeight = FontWeight.Medium,
                color     = colors.terracotta,
                textAlign = TextAlign.Center,
                maxLines  = 1
            )
        }
        Text(text = "·", fontSize = 14.sp, color = colors.textSecondary)
        TextButton(onClick = onPauseReading, modifier = Modifier.weight(1f)) {
            Text(
                text      = "Pause",
                fontSize  = 12.sp,
                fontWeight = FontWeight.Medium,
                color     = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines  = 1
            )
        }
    }
}

// ── DNF Confirm sheet ─────────────────────────────────────────────────────────

@Composable
private fun DnfConfirmSheet(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalLorevynColors.current
    var noteText by remember { mutableStateOf("") }

    Box(
        modifier         = Modifier.fillMaxSize().navigationBarsPadding().imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable { onDismiss() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.background)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary.copy(alpha = 0.40f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text       = "Did not finish",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text       = "No judgment — some books aren't for us right now. This one moves to your library with a DNF badge.",
                    fontSize   = 13.sp,
                    color      = colors.textSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value         = noteText,
                    onValueChange = { noteText = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            text      = "Why did you stop? (optional)",
                            color     = colors.textSecondary,
                            fontSize  = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    minLines = 1,
                    maxLines = 3,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor      = colors.terracotta,
                        unfocusedBorderColor    = Color.Transparent,
                        focusedTextColor        = colors.textPrimary,
                        unfocusedTextColor      = colors.textPrimary,
                        cursorColor             = colors.terracotta
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.12f))
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(text = "Cancel", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                }
                Button(
                    onClick  = { onConfirm(noteText) },
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape    = RoundedCornerShape(24.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = colors.terracotta, contentColor = Color.White)
                ) {
                    Text(text = "Mark as DNF", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Delete confirm sheet ──────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalLorevynColors.current
    Box(
        modifier         = Modifier.fillMaxSize().navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable { onDismiss() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.background)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary.copy(alpha = 0.40f))
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(text = "Remove from library", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text       = "This book and all your notes, ratings, and reading history for it will be permanently deleted. This cannot be undone.",
                    fontSize   = 14.sp,
                    color      = colors.textSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.12f))
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(text = "Keep it", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.primaryGreen)
                }
                Button(
                    onClick  = onConfirm,
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape    = RoundedCornerShape(24.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = colors.terracotta.copy(alpha = 0.80f), contentColor = Color.White)
                ) {
                    Text(text = "Yes, remove it", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



// ── Genre editor ──────────────────────────────────────────────────────────────
// Tappable genre row — expands to full chip picker on tap.
// Lets user correct garbage API genre data with one clean selection.

private val GENRE_OPTIONS = listOf(
    Genre.FICTION          to "Fiction",
    Genre.NON_FICTION      to "Non-fiction",
    Genre.MYSTERY          to "Mystery",
    Genre.THRILLER         to "Thriller",
    Genre.ROMANCE          to "Romance",
    Genre.SCIENCE_FICTION  to "Sci-fi",
    Genre.FANTASY          to "Fantasy",
    Genre.HISTORICAL_FICTION to "Historical",
    Genre.BIOGRAPHY        to "Biography",
    Genre.SELF_HELP        to "Self-help",
    Genre.GRAPHIC_NOVEL    to "Graphic novels",
    Genre.POETRY           to "Poetry",
    Genre.CHILDREN         to "Children",
    Genre.YOUNG_ADULT      to "Young Adult",
    Genre.OTHER            to "Other"
)

private fun displayGenre(raw: String?): String {
    if (raw == null) return "Not set"
    return GENRE_OPTIONS.firstOrNull { it.first.name == raw }?.second ?: raw
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreSection(
    currentGenre:   String?,
    onGenreChanged: (String) -> Unit
) {
    val colors = LocalLorevynColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text          = "GENRE",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = colors.textSecondary,
                letterSpacing = 0.12.sp
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = displayGenre(currentGenre),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = if (currentGenre != null) colors.primaryGreen else colors.textSecondary
                )
                Text(
                    text      = "›",
                    fontSize  = 14.sp,
                    color     = colors.terracotta,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                GENRE_OPTIONS.forEach { (genre, label) ->
                    val isSelected = currentGenre == genre.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) colors.primaryGreen
                                else colors.primaryGreen.copy(alpha = 0.08f)
                            )
                            .clickable {
                                onGenreChanged(genre.name)
                                expanded = false
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = label,
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (isSelected) Color.White else colors.primaryGreen
                        )
                    }
                }
            }
        }
    }
}

// ── Page count section ────────────────────────────────────────────────────────
// Shows when book is READING and page_count is null (API missed it).
// User enters total pages once — progress bar on Reading tab unlocks immediately.

@Composable
private fun PageCountSection(onPageCountChanged: (Int) -> Unit) {
    val colors = LocalLorevynColors.current
    var input by remember { mutableStateOf("") }

    val submit = {
        val count = input.trim().toIntOrNull()
        if (count != null && count > 0) onPageCountChanged(count)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text          = "TOTAL PAGES",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = { if (it.length <= 6) input = it.filter { c -> c.isDigit() } },
                modifier      = Modifier.weight(1f),
                placeholder   = {
                    Text(
                        text      = "e.g. 320",
                        color     = colors.textSecondary,
                        fontSize  = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                singleLine      = true,
                shape           = RoundedCornerShape(12.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor      = colors.sage,
                    unfocusedBorderColor    = Color.Transparent,
                    focusedTextColor        = colors.textPrimary,
                    unfocusedTextColor      = colors.textPrimary,
                    cursorColor             = colors.primaryGreen
                )
            )
            Button(
                onClick  = submit,
                enabled  = input.trim().toIntOrNull()?.let { it > 0 } ?: false,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = colors.sage,
                    contentColor   = Color.White
                ),
                modifier = Modifier.height(56.dp)
            ) {
                Text(text = "Set", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Rating section ────────────────────────────────────────────────────────────

@Composable
private fun RatingSection(
    rating: Float?,
    onRatingChanged: (Float) -> Unit
) {
    val colors = LocalLorevynColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "YOUR RATING", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.12.sp)
        Spacer(modifier = Modifier.height(10.dp))
        RatingBar(rating = rating, starSize = 32.dp, onRatingChanged = onRatingChanged)
        if (rating != null && rating > 0f) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "${"%.1f".format(rating)} / 5.0", fontSize = 12.sp, color = colors.textSecondary)
        }
    }
}

// ── Notes section ─────────────────────────────────────────────────────────────

@Composable
private fun NotesSection(notes: String?, onNotesChanged: (String) -> Unit) {
    val colors = LocalLorevynColors.current
    var localNotes by remember(notes) { mutableStateOf(notes ?: "") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "YOUR NOTES", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.12.sp)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value         = localNotes,
            onValueChange = { newVal -> localNotes = newVal; onNotesChanged(newVal) },
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = {
                Text(text = "Your private notes about this book...", color = colors.textSecondary, fontSize = 14.sp, fontStyle = FontStyle.Italic)
            },
            minLines = 3,
            maxLines = 8,
            shape    = RoundedCornerShape(12.dp),
            colors   = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor      = colors.sage,
                unfocusedBorderColor    = Color.Transparent,
                focusedTextColor        = colors.textPrimary,
                unfocusedTextColor      = colors.textPrimary,
                cursorColor             = colors.primaryGreen
            )
        )
    }
}

// ── DNF note display ──────────────────────────────────────────────────────────

@Composable
private fun DnfNoteSection(dnfNote: String) {
    val colors = LocalLorevynColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "WHY YOU STOPPED", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.terracotta.copy(alpha = 0.06f))
                .padding(14.dp)
        ) {
            Text(text = dnfNote, fontSize = 14.sp, color = colors.textSecondary, fontStyle = FontStyle.Italic)
        }
    }
}

// ── Description ───────────────────────────────────────────────────────────────

@Composable
private fun DescriptionSection(description: String) {
    val colors = LocalLorevynColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "ABOUT THIS BOOK", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text     = description,
            fontSize = 14.sp,
            color    = colors.textSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = if (expanded) "Show less" else "Read more",
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = colors.sage,
            modifier   = Modifier.clickable { expanded = !expanded }
        )
    }
}

// ── Format selector section ────────────────────────────────────────────────────
// 3-chip row: Physical / E-Book / Audiobook.
// Saves immediately on tap — same pattern as genre editor.

@Composable
private fun FormatSection(
    currentFormat: BookFormat,
    onFormatChanged: (BookFormat) -> Unit,
) {
    val colors = LocalLorevynColors.current
    val formats = listOf(
        BookFormat.PHYSICAL  to R.string.book_detail_format_physical,
        BookFormat.EBOOK     to R.string.book_detail_format_ebook,
        BookFormat.AUDIOBOOK to R.string.book_detail_format_audiobook,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text          = stringResource(R.string.book_detail_format_label),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.12.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            formats.forEach { (format, labelResId) ->
                val isSelected = format == currentFormat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primaryGreen else colors.surface)
                        .clickable { if (!isSelected) onFormatChanged(format) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = stringResource(labelResId),
                        fontSize   = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isSelected) Color.White else colors.textSecondary,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ── Format badge — overlaid on cover image ────────────────────────────────────
// Shown for EBOOK (sage) and AUDIOBOOK (terracotta) only.
// Follows Decision #37 badge pattern — bottom-left of cover.

@Composable
private fun FormatBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            letterSpacing = 0.4.sp,
        )
    }
}

// ── FavoriteGateSheet — shown to free users who tap the heart ─────────────────

@Composable
private fun FavoriteGateSheet(onDismiss: () -> Unit) {
    val colors = LocalLorevynColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.surface)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .clickable(onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Favorite,
                contentDescription = null,
                tint               = colors.terracotta,
                modifier           = Modifier.size(32.dp)
            )
            Text(
                text       = stringResource(R.string.book_detail_premium_favorite_gate_title),
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center,
            )
            Text(
                text       = stringResource(R.string.book_detail_premium_favorite_gate_body),
                fontSize   = 14.sp,
                color      = colors.textSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(26.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = colors.terracotta,
                    contentColor   = Color.White
                )
            ) {
                Text(
                    text       = stringResource(R.string.premium_cta_try_free),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    text     = stringResource(R.string.premium_cta_not_now),
                    fontSize = 14.sp,
                    color    = colors.textSecondary
                )
            }
        }
    }
}

// ── TD-111: Series entry section ──────────────────────────────────────────────
// Tappable "Series" row — opens an edit dialog with autocomplete from the
// user's own previously-entered series names. Zero external API dependency,
// so every suggestion is trustworthy. Position is optional decimal.

@Composable
private fun SeriesSection(
    currentSeriesName: String?,
    currentSeriesPosition: Double?,
    seriesTotal: Int?,
    distinctSeriesNames: List<String>,
    onSeriesChanged: (String?, Double?) -> Unit,
    onClearSeries: () -> Unit
) {
    val colors = LocalLorevynColors.current
    var showDialog by remember { mutableStateOf(false) }

    val summary = when {
        currentSeriesName == null -> stringResource(R.string.book_detail_series_none)
        currentSeriesPosition != null && seriesTotal != null ->
            "$currentSeriesName · ${formatPositionShort(currentSeriesPosition)}/$seriesTotal"
        currentSeriesPosition != null ->
            "$currentSeriesName · #${formatPositionShort(currentSeriesPosition)}"
        else -> currentSeriesName
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text          = stringResource(R.string.book_detail_series_label).uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.textSecondary,
            letterSpacing = 0.12.sp
        )
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text       = summary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = if (currentSeriesName != null) colors.primaryGreen else colors.textSecondary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.widthIn(max = 200.dp)
            )
            Text(
                text       = "›",
                fontSize   = 14.sp,
                color      = colors.terracotta,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showDialog) {
        SeriesEditDialog(
            initialName         = currentSeriesName,
            initialPosition     = currentSeriesPosition,
            distinctSeriesNames = distinctSeriesNames,
            onSave              = { name, pos ->
                onSeriesChanged(name, pos)
                showDialog = false
            },
            onRemove            = {
                onClearSeries()
                showDialog = false
            },
            onDismiss           = { showDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesEditDialog(
    initialName: String?,
    initialPosition: Double?,
    distinctSeriesNames: List<String>,
    onSave: (String?, Double?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalLorevynColors.current
    var nameText     by remember { mutableStateOf(initialName.orEmpty()) }
    var positionText by remember {
        mutableStateOf(initialPosition?.let { formatPositionShort(it) }.orEmpty())
    }

    // In-memory filter over user's own series names. No API, no network.
    val suggestions = remember(nameText, distinctSeriesNames) {
        val query = nameText.trim()
        if (query.isBlank()) {
            distinctSeriesNames.take(5)
        } else {
            distinctSeriesNames
                .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
                .take(5)
        }
    }

    val positionValid = positionText.isBlank() ||
        positionText.toDoubleOrNull()?.let { it > 0.0 && it < 1000.0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = colors.surface,
        title = {
            Text(
                text       = stringResource(R.string.book_detail_series_dialog_title),
                fontWeight = FontWeight.SemiBold,
                color      = colors.primaryGreen
            )
        },
        text = {
            Column {
                Text(
                    text     = stringResource(R.string.book_detail_series_dialog_body),
                    fontSize = 13.sp,
                    color    = colors.textSecondary
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value         = nameText,
                    onValueChange = { nameText = it },
                    label         = { Text(stringResource(R.string.book_detail_series_name_label)) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.sage,
                        cursorColor        = colors.sage
                    )
                )

                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        suggestions.forEach { name ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.primaryGreen.copy(alpha = 0.08f))
                                    .clickable { nameText = name }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text     = name,
                                    fontSize = 12.sp,
                                    color    = colors.primaryGreen
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value           = positionText,
                    onValueChange   = { input ->
                        // Allow digits + single decimal point, up to 5 chars (e.g. 12.5)
                        if (input.length <= 5 && input.all { c -> c.isDigit() || c == '.' } &&
                            input.count { it == '.' } <= 1
                        ) positionText = input
                    },
                    label           = { Text(stringResource(R.string.book_detail_series_position_label)) },
                    placeholder     = { Text(stringResource(R.string.book_detail_series_position_hint)) },
                    singleLine      = true,
                    isError         = !positionValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth(),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.sage,
                        cursorColor        = colors.sage
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = positionValid,
                onClick = {
                    val cleanName = nameText.trim().takeIf { it.isNotBlank() }
                    val cleanPos  = positionText.trim().toDoubleOrNull()
                        ?.takeIf { it > 0.0 && it < 1000.0 }
                    onSave(cleanName, cleanPos)
                }
            ) {
                Text(
                    text       = stringResource(R.string.book_detail_series_save),
                    color      = colors.primaryGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            Row {
                if (initialName != null) {
                    TextButton(onClick = onRemove) {
                        Text(
                            text  = stringResource(R.string.book_detail_series_remove),
                            color = colors.terracotta
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text  = stringResource(R.string.book_detail_series_cancel),
                        color = colors.textSecondary
                    )
                }
            }
        }
    )
}

// 1.0 → "1", 2.5 → "2.5". Keeps the UI tight — no trailing ".0".
private fun formatPositionShort(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else value.toString()
