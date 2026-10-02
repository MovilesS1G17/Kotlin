package com.centralia.app.data.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.centralia.app.BuildConfig
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ApiException(val status: Int, message: String) : Exception(message)

class SecureTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences("centralia.secure.session", Context.MODE_PRIVATE)
    private val keyAlias = "centralia.access.token"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }

    fun save(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val value = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        preferences.edit().putString("token", Base64.encodeToString(value, Base64.NO_WRAP)).apply()
    }

    fun read(): String? {
        val encoded = preferences.getString("token", null) ?: return null
        return try {
            val value = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, value.copyOfRange(0, 12)))
            String(cipher.doFinal(value.copyOfRange(12, value.size)), Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() { preferences.edit().remove("token").apply() }
}

class CentraliaApi(val tokens: SecureTokenStore, private val onSessionExpired: () -> Unit = {}) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .build()

    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null,
                        authenticated: Boolean = true): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(BuildConfig.CENTRALIA_API_BASE_URL.trimEnd('/') + path)
            .header("Accept", "application/json")
        if (authenticated) {
            val token = tokens.read() ?: throw ApiException(401, "Your session has expired. Please log in again.")
            builder.header("Authorization", "Bearer $token")
        }
        val jsonType = "application/json; charset=utf-8".toMediaType()
        val requestBody = body?.toString()?.toRequestBody(jsonType)
            ?: if (method in setOf("POST", "PUT", "PATCH")) "".toRequestBody(jsonType) else null
        builder.method(method, requestBody)
        try {
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401 && authenticated) {
                    tokens.clear()
                    onSessionExpired()
                }
                if (!response.isSuccessful) {
                    val detail = runCatching {
                        val value = JSONObject(text).opt("detail")
                        when (value) {
                            is String -> value
                            is org.json.JSONArray -> value.optJSONObject(0)?.optString("message")
                            else -> null
                        }
                    }.getOrNull()
                    throw ApiException(response.code, when (response.code) {
                        401 -> "Your session has expired. Please log in again."
                        else -> detail ?: "Centralia could not complete this request."
                    })
                }
                text
            }
        } catch (error: ApiException) {
            throw error
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw ApiException(0, "We couldn't reach Centralia. Check your connection and try again.")
        }
    }

    /**
     * Like [request], but takes query params and extra headers, and returns
     * the response [Headers] too (so callers can read `X-Total-Count`).
     * Kept separate so every existing caller of [request] stays untouched.
     */
    suspend fun requestWithHeaders(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        authenticated: Boolean = true
    ): Pair<String, Headers> = withContext(Dispatchers.IO) {
        val urlBuilder = (BuildConfig.CENTRALIA_API_BASE_URL.trimEnd('/') + path).toHttpUrl().newBuilder()
        queryParams.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
        val builder = Request.Builder().url(urlBuilder.build()).header("Accept", "application/json")
        headers.forEach { (key, value) -> builder.header(key, value) }
        if (authenticated) {
            val token = tokens.read() ?: throw ApiException(401, "Your session has expired. Please log in again.")
            builder.header("Authorization", "Bearer $token")
        }
        try {
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401 && authenticated) {
                    tokens.clear()
                    onSessionExpired()
                }
                if (!response.isSuccessful) {
                    val detail = runCatching { JSONObject(text).optString("detail").ifEmpty { null } }.getOrNull()
                    throw ApiException(response.code, when (response.code) {
                        401 -> "Your session has expired. Please log in again."
                        else -> detail ?: "Centralia could not complete this request."
                    })
                }
                text to response.headers
            }
        } catch (error: ApiException) {
            throw error
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw ApiException(0, "We couldn't reach Centralia. Check your connection and try again.")
        }
    }
}

fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else optString(key).ifEmpty { null }
