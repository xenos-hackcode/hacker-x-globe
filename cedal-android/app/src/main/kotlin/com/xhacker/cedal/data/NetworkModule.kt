package com.xhacker.cedal.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// cedal-server now runs on Cloud Run (Postgres/Cloud SQL backed), not on a
// LAN-local machine - works from any network, not just the same Wi-Fi as a
// dev PC. See cedal-server/Dockerfile and db/DatabaseFactory.kt.
private const val BASE_URL = "https://cedal-server-717899371194.us-central1.run.app/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(storage: SecureStorage, json: Json): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            // connectTimeout is short — if the phone can't even reach the
            // server (wrong Wi-Fi, server down, stale IP), there's no point
            // waiting 30s to find out. readTimeout stays generous since
            // /code/run can legitimately take several seconds (it waits on
            // the server's own Wandbox call, which has its own 20s budget).
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            // Access tokens are long-lived now, but this transparently
            // refreshes+retries on the rare case a call still 401s instead
            // of surfacing that to the UI.
            .authenticator(TokenAuthenticator(storage, json))
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}

private class TokenAuthenticator(
    private val storage: SecureStorage,
    private val json: Json,
) : Authenticator {
    // A separate, authenticator-free client so the refresh call itself can't
    // recursively trigger authenticate() again.
    private val refreshClient = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // already retried once — give up, don't loop forever
        val refreshToken = storage.refreshToken ?: return null

        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val refreshRequest = Request.Builder().url("${BASE_URL}auth/refresh").post(body).build()

        return try {
            refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) {
                    // The refresh token itself is dead (expired/already
                    // rotated/server restarted with a fresh dev secret) — no
                    // amount of retrying will fix this. Clear the session so
                    // the app cleanly routes back to sign-in next time
                    // instead of silently re-failing every request forever.
                    storage.clearSession()
                    return null
                }
                val responseBody = refreshResponse.body?.string() ?: return null
                val tokens = json.decodeFromString(AuthTokens.serializer(), responseBody)
                storage.accessToken = tokens.accessToken
                storage.refreshToken = tokens.refreshToken
                storage.userId = tokens.userId
                storage.role = tokens.role
                response.request.newBuilder().header("Authorization", "Bearer ${tokens.accessToken}").build()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
