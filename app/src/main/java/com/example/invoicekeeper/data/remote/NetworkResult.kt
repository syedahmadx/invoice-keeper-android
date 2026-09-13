package com.example.invoicekeeper.data.remote

/**
 * Every repository call returns one of these. Callers never see an exception, and every error the
 * UI needs to phrase differently has its own type.
 */
sealed interface NetworkResult<out T> {

    data class Success<T>(val value: T) : NetworkResult<T>

    sealed interface Failure : NetworkResult<Nothing> {
        /** Human-readable text safe to show in the UI. */
        val message: String

        /** Whether offering a Retry button makes sense for this failure. */
        val retryable: Boolean
    }

    data object NoConnection : Failure {
        override val message = "No internet connection. Check your network and try again."
        override val retryable = true
    }

    data object Timeout : Failure {
        override val message =
            "The request timed out. Large or dense documents can take a while; try again."
        override val retryable = true
    }

    data class Unauthorized(val detail: String? = null) : Failure {
        override val message =
            detail ?: "The server rejected the request. Check that an AI key is configured on the backend."
        override val retryable = false
    }

    data class RateLimited(val detail: String? = null) : Failure {
        override val message = detail ?: "Rate limited by the AI provider. Wait a moment and try again."
        override val retryable = true
    }

    data class ServerError(val code: Int, val detail: String? = null) : Failure {
        override val message = detail ?: "The server returned an error (HTTP $code)."
        override val retryable = code >= 500
    }

    data class InvalidRequest(val detail: String? = null) : Failure {
        override val message = detail ?: "The server could not read that request."
        override val retryable = false
    }

    data class Unknown(val detail: String? = null) : Failure {
        override val message = detail ?: "Something went wrong. Please try again."
        override val retryable = true
    }
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(value))
    is NetworkResult.Failure -> this
}
