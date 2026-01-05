package com.fintrace.app.presentation.common

/**
 * Standard wrapper for screen UI state.
 * All ViewModels should follow this pattern for consistency.
 *
 * Usage:
 * ```
 * data class MyScreenState(
 *     val items: List<Item> = emptyList(),
 *     val selectedItem: Item? = null
 * )
 *
 * class MyViewModel : ViewModel() {
 *     private val _uiState = MutableStateFlow(MyScreenState().toLoadingState())
 *     val uiState: StateFlow<UiStateWrapper<MyScreenState>> = _uiState.asStateFlow()
 *
 *     init {
 *         viewModelScope.launch {
 *             try {
 *                 val items = repository.getItems()
 *                 _uiState.value = MyScreenState(items = items).toSuccessState()
 *             } catch (e: Exception) {
 *                 _uiState.value = MyScreenState().toErrorState(UiError.Exception(e))
 *             }
 *         }
 *     }
 * }
 * ```
 */
data class UiStateWrapper<T>(
    val data: T,
    val isLoading: Boolean = true, // Default to true - loading until proven otherwise
    val error: UiError? = null
) {
    /**
     * True if there's no error and not loading.
     */
    val isSuccess: Boolean
        get() = !isLoading && error == null

    /**
     * True if there's an error.
     */
    val hasError: Boolean
        get() = error != null
}

/**
 * Types of UI errors.
 */
sealed class UiError {
    /**
     * A simple string message.
     */
    data class Message(val message: String) : UiError()

    /**
     * A string resource ID for localized messages.
     */
    data class Resource(val resId: Int, val formatArgs: List<Any> = emptyList()) : UiError()

    /**
     * An exception with optional message override.
     */
    data class Exception(
        val throwable: Throwable,
        val messageOverride: String? = null
    ) : UiError() {
        val displayMessage: String
            get() = messageOverride ?: throwable.message ?: "An error occurred"
    }
}

/**
 * Extension to create loading state.
 */
fun <T> T.toLoadingState() = UiStateWrapper(
    data = this,
    isLoading = true,
    error = null
)

/**
 * Extension to create success state.
 */
fun <T> T.toSuccessState() = UiStateWrapper(
    data = this,
    isLoading = false,
    error = null
)

/**
 * Extension to create error state.
 */
fun <T> T.toErrorState(error: UiError) = UiStateWrapper(
    data = this,
    isLoading = false,
    error = error
)

/**
 * Extension to create error state from string message.
 */
fun <T> T.toErrorState(message: String) = UiStateWrapper(
    data = this,
    isLoading = false,
    error = UiError.Message(message)
)

/**
 * Extension to create error state from exception.
 */
fun <T> T.toErrorState(exception: Throwable) = UiStateWrapper(
    data = this,
    isLoading = false,
    error = UiError.Exception(exception)
)
