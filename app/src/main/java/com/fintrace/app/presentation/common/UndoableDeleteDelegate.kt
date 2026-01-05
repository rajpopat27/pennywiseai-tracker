package com.fintrace.app.presentation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Delegate for handling delete-with-undo pattern.
 *
 * This delegate manages the soft-delete and undo workflow:
 * 1. User deletes an item -> item is soft-deleted and stored for undo
 * 2. Undo snackbar shown for [undoTimeoutMs] milliseconds
 * 3. If user clicks undo -> item is restored
 * 4. If timeout expires -> deletion becomes permanent
 *
 * Usage:
 * ```
 * class MyViewModel : ViewModel() {
 *     private val deleteDelegate = UndoableDeleteDelegate<MyItem>(
 *         scope = viewModelScope,
 *         onDelete = { item -> repository.delete(item) },
 *         onRestore = { item -> repository.insert(item) }
 *     )
 *
 *     val deletedItem = deleteDelegate.deletedItem
 *
 *     fun delete(item: MyItem) = deleteDelegate.delete(item)
 *     fun undoDelete() = deleteDelegate.undo()
 *     fun clearDeletedItem() = deleteDelegate.clear()
 * }
 * ```
 */
class UndoableDeleteDelegate<T>(
    private val scope: CoroutineScope,
    private val onDelete: suspend (T) -> Unit,
    private val onRestore: suspend (T) -> Unit,
    private val undoTimeoutMs: Long = 5000L,
    private val onTimeout: (() -> Unit)? = null
) {
    private val _deletedItem = MutableStateFlow<T?>(null)

    /**
     * The currently deleted item that can be undone, or null if none.
     */
    val deletedItem: StateFlow<T?> = _deletedItem.asStateFlow()

    private var timeoutJob: Job? = null
    private var isPermanentlyDeleted = false

    /**
     * Mark item for deletion. Shows undo option for [undoTimeoutMs].
     * If not undone, deletion becomes permanent.
     *
     * @param item The item to delete
     */
    fun delete(item: T) {
        // Cancel any previous timeout
        timeoutJob?.cancel()
        isPermanentlyDeleted = false

        // Store item for potential undo
        _deletedItem.value = item

        // Perform soft delete immediately
        scope.launch {
            try {
                onDelete(item)
            } catch (e: Exception) {
                // Restore on failure
                _deletedItem.value = null
                throw e
            }
        }

        // Start timeout for permanent deletion
        timeoutJob = scope.launch {
            delay(undoTimeoutMs)
            if (_deletedItem.value == item) {
                isPermanentlyDeleted = true
                _deletedItem.value = null
                onTimeout?.invoke()
            }
        }
    }

    /**
     * Undo the last deletion if still within timeout.
     *
     * @return true if undo was successful, false if too late or nothing to undo
     */
    fun undo(): Boolean {
        timeoutJob?.cancel()

        val item = _deletedItem.value
        if (item == null || isPermanentlyDeleted) {
            return false
        }

        _deletedItem.value = null

        scope.launch {
            onRestore(item)
        }

        return true
    }

    /**
     * Clear the deleted item without restoring.
     * Call this when user dismisses the undo snackbar.
     */
    fun clear() {
        timeoutJob?.cancel()
        isPermanentlyDeleted = true
        _deletedItem.value = null
    }

    /**
     * Check if there's a pending deletion that can be undone.
     */
    fun hasPendingUndo(): Boolean = _deletedItem.value != null && !isPermanentlyDeleted
}
