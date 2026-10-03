package com.centralia.app.data.remote

import android.content.Context
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response


class ApiException(
    val statusCode: Int,
    val code: String,
    val detail: String,

    val retryAfter: Int? = null,

    val email: String? = null
) : Exception(detail) {
    companion object {
        fun unreachable() = ApiException(
            0,
            "unreachable",
            "Centralia can’t reach the server. Check your connection and try again."
        )

        fun unexpectedResponse() = ApiException(
            0,
            "unexpected_response",
            "Centralia received an unexpected response. Please try again."
        )
    }
}


class TokenStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("centralia_session", Context.MODE_PRIVATE)

    var token: String?
        get() = preferences.getString(KEY_TOKEN, null)
        set(value) {
            preferences.edit().apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }.apply()
        }

    private companion object {
        const val KEY_TOKEN = "accessToken"
    }
}


class ApiClient(
    baseUrl: String,
    val tokenStore: TokenStore,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    private val baseUrl: HttpUrl = baseUrl.trimEnd('/').toHttpUrl()


    var onUnauthorized: (() -> Unit)? = null

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    enum class Method { GET, POST, PUT, PATCH, DELETE }

    /** envia una peticion y deserializa el cuerpo con [responseSerializer]. */
    suspend fun <T> send(
        method: Method,
        path: String,
        responseSerializer: KSerializer<T>,
        body: JsonElement? = null,
        query: Map<String, String> = emptyMap(),
        authenticated: Boolean = true
    ): T {
        val text = perform(method, path, body, query, authenticated)
        return try {
            json.decodeFromString(responseSerializer, text)
        } catch (error: Exception) {
            throw ApiException.unexpectedResponse()
        }
    }


    suspend fun execute(
        method: Method,
        path: String,
        body: JsonElement? = null,
        authenticated: Boolean = true
    ) {
        perform(method, path, body, emptyMap(), authenticated)
    }


    fun absoluteUrl(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else baseUrl.toString().trimEnd('/') + "/" + path.trimStart('/')


    fun <T> encode(serializer: KSerializer<T>, value: T): JsonElement =
        json.encodeToJsonElement(serializer, value)

    private suspend fun perform(
        method: Method,
        path: String,
        body: JsonElement?,
        query: Map<String, String>,
        authenticated: Boolean
    ): String {
        val url = baseUrl.newBuilder()
            .addPathSegments(path.trimStart('/'))
            .apply { query.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()

        val requestBody: RequestBody? = body?.toString()?.toRequestBody(JSON_MEDIA_TYPE)
            ?: if (method == Method.POST || method == Method.PUT || method == Method.PATCH) {
                "".toRequestBody(JSON_MEDIA_TYPE)
            } else {
                null
            }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            // Lets the backend tell the Android and iPhone sessions apart.
            .header("X-Client-Platform", "android")
            .method(method.name, requestBody)
            .apply {
                val token = tokenStore.token
                if (authenticated && token != null) header("Authorization", "Bearer $token")
            }
            .build()

        val (statusCode, text) = try {
            withContext(Dispatchers.IO) {
                httpClient.newCall(request).await().use { response ->
                    response.code to (response.body?.string() ?: "")
                }
            }
        } catch (error: IOException) {
            throw ApiException.unreachable()
        }

        if (statusCode !in 200..299) {
            val payload = runCatching { json.decodeFromString(ErrorPayload.serializer(), text) }.getOrNull()
            if (statusCode == 401 && authenticated && tokenStore.token != null) {
                tokenStore.token = null
                onUnauthorized?.invoke()
            }
            throw ApiException(
                statusCode = statusCode,
                code = payload?.code ?: "http_$statusCode",
                detail = payload?.detail ?: "Something went wrong. Please try again.",
                retryAfter = payload?.retryAfter,
                email = payload?.email
            )
        }

        return text
    }

    @Serializable
    private data class ErrorPayload(
        val code: String? = null,
        val detail: String? = null,
        val retryAfter: Int? = null,
        val email: String? = null
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}


private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
    })
}
