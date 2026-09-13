package com.example.invoicekeeper.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Runs a Retrofit call on the IO dispatcher and folds every outcome, including thrown exceptions,
 * into a [NetworkResult]. Nothing above this line ever sees an HTTP status code or an IOException.
 */
suspend fun <T : Any> safeApiCall(block: suspend () -> Response<T>): NetworkResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Unknown("The server returned an empty response.")
                }
            } else {
                val detail = response.errorBody()?.let { errorBody ->
                    runCatching {
                        val raw = errorBody.string()
                        ApiClient.json.decodeFromString(ApiErrorBody.serializer(), raw).text
                            ?: raw.takeIf { it.isNotBlank() && it.length < 300 }
                    }.getOrNull()
                }
                when (response.code()) {
                    400, 413, 415, 422 -> NetworkResult.InvalidRequest(detail)
                    401, 403 -> NetworkResult.Unauthorized(detail)
                    429 -> NetworkResult.RateLimited(detail)
                    504, 408 -> NetworkResult.Timeout
                    else -> NetworkResult.ServerError(response.code(), detail)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            NetworkResult.Timeout
        } catch (e: UnknownHostException) {
            NetworkResult.NoConnection
        } catch (e: SSLException) {
            NetworkResult.Unknown("Secure connection failed. Check the base URL in Settings.")
        } catch (e: IOException) {
            NetworkResult.NoConnection
        } catch (e: IllegalArgumentException) {
            // Retrofit throws this for a malformed @Url, which means a bad base URL in Settings.
            NetworkResult.InvalidRequest("That base URL is not valid. Check it in Settings.")
        } catch (e: Exception) {
            NetworkResult.Unknown(e.message?.takeIf { it.isNotBlank() })
        }
    }
