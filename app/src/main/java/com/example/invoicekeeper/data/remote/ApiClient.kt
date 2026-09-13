package com.example.invoicekeeper.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single OkHttp/Retrofit stack the app uses.
 *
 * Notably absent: any logging interceptor. Request bodies here contain base64 document contents
 * and supplier data, and none of that belongs in logcat.
 */
object ApiClient {

    const val DEFAULT_BASE_URL = "https://invoice-keeper-web.vercel.app"

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    private val contentType = "application/json".toMediaType()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(CallTimeout.SHORT.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(TimeoutInterceptor())
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            // Every call supplies an absolute @Url, so this is only here to satisfy Retrofit.
            .baseUrl("$DEFAULT_BASE_URL/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}

/**
 * Applies the per-call read timeout carried on the request tag, so extraction gets 60s while the
 * lighter endpoints stay at 30s without needing a second OkHttp client.
 */
class TimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val timeout = request.tag(CallTimeout::class.java) ?: CallTimeout.SHORT
        return chain
            .withReadTimeout(timeout.readTimeoutSeconds, TimeUnit.SECONDS)
            .proceed(request)
    }
}
