package com.lorevyn.feature.addbook.ui

// feature/add-book/src/main/kotlin/com/lorevyn/feature/addbook/ui/AddBookManualViewModel.kt
// SCR-12 — Manual Book Entry ViewModel
// Handles form state + validation + save via AddBookUseCase.
// ISBN pre-filled when arriving from the scanner via SavedStateHandle.

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorevyn.core.domain.model.BookFormat
import com.lorevyn.core.domain.model.BookInsert
import com.lorevyn.core.domain.model.BookSource
import com.lorevyn.core.domain.model.Genre
import com.lorevyn.core.domain.model.ReadStatus
import com.lorevyn.core.domain.result.BookshelfResult
import com.lorevyn.core.domain.usecase.AddBookUseCase
import com.lorevyn.core.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AddBookManualViewModel"

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class ManualEntryUiState {
    object Idle : ManualEntryUiState()
    object Saving : ManualEntryUiState()
    data class Error(val message: String) : ManualEntryUiState()
    data class Saved(val bookId: Long) : ManualEntryUiState()
    data class Duplicate(val existingBookId: Long) : ManualEntryUiState()
}

// ── Nav Events ────────────────────────────────────────────────────────────────

sealed class ManualEntryNavEvent {
    object NavigateBack : ManualEntryNavEvent()
    data class NavigateToBookDetail(val bookId: Long) : ManualEntryNavEvent()
    object NavigateToReading : ManualEntryNavEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AddBookManualViewModel @Inject constructor(
    private val addBookUseCase: AddBookUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Pre-fill ISBN from scanner if present in nav args
    private val scannedIsbn: String? =
        savedStateHandle.get<String>(Destination.AddBookManual.ARG_ISBN)
            ?.takeIf { it.isNotBlank() }

    // ── Form fields ───────────────────────────────────────────────────────────

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _author = MutableStateFlow("")
    val author: StateFlow<String> = _author.asStateFlow()

    private val _isbn = MutableStateFlow(scannedIsbn ?: "")
    val isbn: StateFlow<String> = _isbn.asStateFlow()

    private val _pageCount = MutableStateFlow("")
    val pageCount: StateFlow<String> = _pageCount.asStateFlow()

    private val _selectedGenre = MutableStateFlow<Genre?>(null)
    val selectedGenre: StateFlow<Genre?> = _selectedGenre.asStateFlow()

    private val _selectedStatus = MutableStateFlow(ReadStatus.READING)
    val selectedStatus: StateFlow<ReadStatus> = _selectedStatus.asStateFlow()

    private val _titleError = MutableStateFlow(false)
    val titleError: StateFlow<Boolean> = _titleError.asStateFlow()

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<ManualEntryUiState>(ManualEntryUiState.Idle)
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<ManualEntryNavEvent>()
    val navEvents = _navEvents.asSharedFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    fun onTitleChange(value: String) {
        _title.value = value
        if (value.isNotBlank()) _titleError.value = false
    }

    fun onAuthorChange(value: String) { _author.value = value }
    fun onIsbnChange(value: String) { _isbn.value = value.filter { it.isDigit() }.take(13) }
    fun onPageCountChange(value: String) { _pageCount.value = value.filter { it.isDigit() }.take(5) }
    fun onGenreSelected(genre: Genre?) { _selectedGenre.value = genre }
    fun onStatusSelected(status: ReadStatus) { _selectedStatus.value = status }

    fun onSave() {
        val titleTrimmed = _title.value.trim()
        if (titleTrimmed.isBlank()) {
            _titleError.value = true
            return
        }

        viewModelScope.launch {
            _uiState.value = ManualEntryUiState.Saving

            val authorNames = _author.value.trim()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val insert = BookInsert(
                title       = titleTrimmed,
                isbn13      = _isbn.value.trim().takeIf { it.length == 13 },
                pageCount   = _pageCount.value.toIntOrNull()?.takeIf { it > 0 },
                genre       = _selectedGenre.value?.name,
                source      = BookSource.MANUAL,
                authorNames = authorNames,
                format      = BookFormat.PHYSICAL,
                language    = "en"
            )

            when (val result = addBookUseCase(insert, _selectedStatus.value)) {
                is BookshelfResult.Success -> {
                    _uiState.value = ManualEntryUiState.Saved(result.data)
                    _navEvents.emit(ManualEntryNavEvent.NavigateToReading)
                }
                is BookshelfResult.Failure.ValidationError -> {
                    when (result.field) {
                        "duplicate" -> {
                            val existingId = result.message.toLongOrNull()
                            if (existingId != null) {
                                _uiState.value = ManualEntryUiState.Duplicate(existingId)
                                _navEvents.emit(ManualEntryNavEvent.NavigateToBookDetail(existingId))
                            } else {
                                _uiState.value = ManualEntryUiState.Error("This book is already in your library.")
                            }
                        }
                        else -> {
                            _uiState.value = ManualEntryUiState.Error(result.message)
                        }
                    }
                }
                else -> {
                    Log.e(TAG, "Manual save failed: $result")
                    _uiState.value = ManualEntryUiState.Error("Could not save book. Please try again.")
                }
            }
        }
    }

    fun onBackTapped() {
        viewModelScope.launch { _navEvents.emit(ManualEntryNavEvent.NavigateBack) }
    }
}
