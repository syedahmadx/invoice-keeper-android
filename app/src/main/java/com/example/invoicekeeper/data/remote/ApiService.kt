package com.example.invoicekeeper.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Tag
import retrofit2.http.Url

/**
 * The backend proxy. This app holds no AI credentials; every model call goes through these
 * endpoints, which attach the keys server-side.
 *
 * URLs are passed with @Url so the base URL can be changed at runtime from Settings without
 * rebuilding the Retrofit instance.
 */
interface ApiService {

    @POST
    suspend fun extract(
        @Url url: String,
        @Tag timeout: CallTimeout,
        @Body body: ExtractRequest,
    ): Response<ExtractResponse>

    @POST
    suspend fun review(
        @Url url: String,
        @Tag timeout: CallTimeout,
        @Body body: ReviewRequest,
    ): Response<ReviewResponse>

    @POST
    suspend fun send(
        @Url url: String,
        @Tag timeout: CallTimeout,
        @Body body: SendRequest,
    ): Response<SendResponse>

    @GET
    suspend fun config(
        @Url url: String,
        @Tag timeout: CallTimeout,
    ): Response<ConfigResponse>
}

/**
 * Per-call read timeout, applied by TimeoutInterceptor. Extraction runs a vision model over a
 * whole document and needs far longer than the other three endpoints.
 */
enum class CallTimeout(val readTimeoutSeconds: Int) {
    /** 60s, for /api/extract. */
    LONG(60),

    /** 30s, for /api/review, /api/send and /api/config. */
    SHORT(30),
}
