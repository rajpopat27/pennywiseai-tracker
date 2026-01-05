package com.fintrace.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.database.entity.MerchantAliasEntity
import com.fintrace.app.data.repository.MerchantAliasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MerchantAliasViewModel @Inject constructor(
    private val merchantAliasRepository: MerchantAliasRepository
) : ViewModel() {

    val aliases: StateFlow<List<MerchantAliasEntity>> =
        merchantAliasRepository.getAllAliases()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _editingAlias = MutableStateFlow<MerchantAliasEntity?>(null)
    val editingAlias: StateFlow<MerchantAliasEntity?> = _editingAlias.asStateFlow()

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        object AliasAdded : UiEvent()
        object AliasDeleted : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    fun showAddDialog() {
        _editingAlias.value = null
        _showAddDialog.value = true
    }

    fun showEditDialog(alias: MerchantAliasEntity) {
        _editingAlias.value = alias
        _showAddDialog.value = true
    }

    fun dismissDialog() {
        _showAddDialog.value = false
        _editingAlias.value = null
    }

    fun saveAlias(originalName: String, aliasName: String) {
        if (originalName.isBlank() || aliasName.isBlank()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Both fields are required"))
            }
            return
        }

        if (originalName.trim().equals(aliasName.trim(), ignoreCase = true)) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Alias must be different from original name"))
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                merchantAliasRepository.setAlias(originalName.trim(), aliasName.trim())
                _events.emit(UiEvent.AliasAdded)
                dismissDialog()
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("Failed to save alias: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAlias(originalName: String) {
        viewModelScope.launch {
            try {
                merchantAliasRepository.removeAlias(originalName)
                _events.emit(UiEvent.AliasDeleted)
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("Failed to delete alias: ${e.message}"))
            }
        }
    }
}
