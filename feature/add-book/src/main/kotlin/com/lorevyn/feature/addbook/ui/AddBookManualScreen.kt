package com.lorevyn.feature.addbook.ui

// feature/add-book/src/main/kotlin/com/lorevyn/feature/addbook/ui/AddBookManualScreen.kt
// SCR-12 — Manual Book Entry
// Shown when search returns no results or user arrives from scanner empty-handed.
// Fields: Title (required), Author, ISBN (pre-filled from scanner), Page count, Genre, Status.
// Uses LorevynColors tokens — dark mode ready per Decision #181.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorevyn.core.domain.model.Genre
import com.lorevyn.core.domain.model.ReadStatus
import com.lorevyn.core.ui.LocalLorevynColors
import com.lorevyn.feature.addbook.R
import kotlinx.coroutines.flow.collectLatest

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AddBookManualScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookDetail: (bookId: Long) -> Unit,
    onNavigateToReading: () -> Unit,
    viewModel: AddBookManualViewModel = hiltViewModel()
) {
    val colors = LocalLorevynColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val author by viewModel.author.collectAsStateWithLifecycle()
    val isbn by viewModel.isbn.collectAsStateWithLifecycle()
    val pageCount by viewModel.pageCount.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val titleError by viewModel.titleError.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                is ManualEntryNavEvent.NavigateBack            -> onNavigateBack()
                is ManualEntryNavEvent.NavigateToBookDetail    -> onNavigateToBookDetail(event.bookId)
                is ManualEntryNavEvent.NavigateToReading       -> onNavigateToReading()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primaryGreen)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.onBackTapped() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(R.string.manual_entry_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Form ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Title — required
            FormField(
                label = stringResource(R.string.manual_entry_label_title),
                value = title,
                onValueChange = viewModel::onTitleChange,
                placeholder = stringResource(R.string.manual_entry_hint_title),
                isError = titleError,
                errorMessage = if (titleError) stringResource(R.string.manual_entry_title_required) else null,
                capitalization = KeyboardCapitalization.Words,
                colors = colors
            )

            // Author — optional, comma-separated for multiple
            FormField(
                label = stringResource(R.string.manual_entry_label_author),
                value = author,
                onValueChange = viewModel::onAuthorChange,
                placeholder = stringResource(R.string.manual_entry_hint_author),
                capitalization = KeyboardCapitalization.Words,
                colors = colors
            )

            // ISBN — optional, numeric only, max 13 digits
            FormField(
                label = stringResource(R.string.manual_entry_label_isbn),
                value = isbn,
                onValueChange = viewModel::onIsbnChange,
                placeholder = stringResource(R.string.manual_entry_hint_isbn),
                keyboardType = KeyboardType.Number,
                colors = colors
            )

            // Page count — optional
            FormField(
                label = stringResource(R.string.manual_entry_label_page_count),
                value = pageCount,
                onValueChange = viewModel::onPageCountChange,
                placeholder = stringResource(R.string.manual_entry_hint_page_count),
                keyboardType = KeyboardType.Number,
                colors = colors
            )

            // Genre picker — 15-chip grid per Decision #149
            GenrePicker(
                selectedGenre = selectedGenre,
                onGenreSelected = viewModel::onGenreSelected,
                colors = colors
            )

            // Status chips
            SectionLabel(
                text = stringResource(R.string.manual_entry_label_status),
                colors = colors
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(ReadStatus.READING, ReadStatus.WANT_TO_READ).forEach { status ->
                    val label = stringResource(
                        when (status) {
                            ReadStatus.READING     -> R.string.add_book_shelf_reading
                            else                   -> R.string.add_book_shelf_want_to_read
                        }
                    )
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = if (selectedStatus == status) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryGreen,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = colors.surface,
                            labelColor = colors.textSecondary
                        )
                    )
                }
            }

            // Error message
            if (uiState is ManualEntryUiState.Error) {
                Text(
                    text = (uiState as ManualEntryUiState.Error).message,
                    fontSize = 13.sp,
                    color = colors.terracotta
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Save button ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { viewModel.onSave() },
                enabled = uiState !is ManualEntryUiState.Saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.terracotta,
                    contentColor = Color.White,
                    disabledContainerColor = colors.terracotta.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                if (uiState is ManualEntryUiState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.manual_entry_save_cta),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Genre picker ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenrePicker(
    selectedGenre: Genre?,
    onGenreSelected: (Genre?) -> Unit,
    colors: com.lorevyn.core.ui.LorevynColors
) {
    SectionLabel(
        text = stringResource(R.string.manual_entry_label_genre),
        colors = colors
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Genre.entries.forEach { genre ->
            val isSelected = selectedGenre == genre
            FilterChip(
                selected = isSelected,
                onClick = {
                    // Tap again to deselect
                    onGenreSelected(if (isSelected) null else genre)
                },
                label = {
                    Text(
                        text = genre.displayName(),
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.sage,
                    selectedLabelColor = Color.White,
                    containerColor = colors.surface,
                    labelColor = colors.textSecondary
                )
            )
        }
    }
}

// ── Form field ────────────────────────────────────────────────────────────────

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    colors: com.lorevyn.core.ui.LorevynColors
) {
    Column(modifier = modifier) {
        SectionLabel(text = label, colors = colors)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = colors.textSecondary.copy(alpha = 0.5f)
                )
            },
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.sage,
                unfocusedBorderColor = colors.divider,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                errorBorderColor = colors.terracotta,
                errorTextColor = colors.textPrimary
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = colors.terracotta,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    colors: com.lorevyn.core.ui.LorevynColors
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary,
        letterSpacing = 0.5.sp
    )
}

// ── Genre display names ───────────────────────────────────────────────────────

private fun Genre.displayName(): String = when (this) {
    Genre.FICTION           -> "Fiction"
    Genre.NON_FICTION       -> "Non-Fiction"
    Genre.MYSTERY           -> "Mystery"
    Genre.THRILLER          -> "Thriller"
    Genre.ROMANCE           -> "Romance"
    Genre.SCIENCE_FICTION   -> "Sci-Fi"
    Genre.FANTASY           -> "Fantasy"
    Genre.HISTORICAL_FICTION -> "Historical"
    Genre.BIOGRAPHY         -> "Biography"
    Genre.SELF_HELP         -> "Self-Help"
    Genre.GRAPHIC_NOVEL     -> "Graphic Novel"
    Genre.POETRY            -> "Poetry"
    Genre.CHILDREN          -> "Children"
    Genre.YOUNG_ADULT       -> "Young Adult"
    Genre.OTHER             -> "Other"
}
