package com.lorevyn.core.network.di

import com.lorevyn.core.network.BuildConfig
import com.lorevyn.core.network.api.GoogleBooksApi
import com.lorevyn.core.network.api.GoogleBooksApiImpl
import com.lorevyn.core.network.api.NytBooksApi
import com.lorevyn.core.network.api.NytBooksApiImpl
import com.lorevyn.core.network.api.OpenLibraryApi
import com.lorevyn.core.network.api.OpenLibraryApiImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// NetworkModule
// Provides HttpClient, GoogleBooksApi, OpenLibraryApi, NytBooksApi as singletons.
// API keys sourced from BuildConfig — read from local.properties at build time.
// Never hardcoded. Never committed to VCS.
//
// NYT API key setup (one-time, developer):
//   1. Sign up at developer.nytimes.com → Create App → enable Books API
//   2. Add to local.properties:  NYT_BOOKS_API_KEY=your_key_here
//   3. In core/network/build.gradle.kts defaultConfig block add:
//      buildConfigField("String","NYT_BOOKS_API_KEY",
//        "\"${properties["NYT_BOOKS_API_KEY"] ?: ""}\"")
//   The Best Sellers row degrades gracefully to empty if key is missing.
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true  // API may add new fields — never crash on unknown keys
        isLenient = true          // Tolerate minor malformed values
        coerceInputValues = true  // Unexpected null uses field default
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Timber.tag("Ktor").d(message)
                }
            }
            // Log headers in debug only — nothing logged in release builds
            level = if (BuildConfig.DEBUG) LogLevel.HEADERS else LogLevel.NONE
        }
    }

    @Provides
    @Singleton
    @Named("google_books_api_key")
    fun provideGoogleBooksApiKey(): String = BuildConfig.GOOGLE_BOOKS_API_KEY

    @Provides
    @Singleton
    @Named("nyt_books_api_key")
    fun provideNytApiKey(): String = BuildConfig.NYT_BOOKS_API_KEY

    @Provides
    @Singleton
    fun provideGoogleBooksApi(
        httpClient: HttpClient,
        @Named("google_books_api_key") apiKey: String
    ): GoogleBooksApi = GoogleBooksApiImpl(httpClient, apiKey)

    @Provides
    @Singleton
    fun provideOpenLibraryApi(
        httpClient: HttpClient
    ): OpenLibraryApi = OpenLibraryApiImpl(httpClient)

    @Provides
    @Singleton
    fun provideNytBooksApi(
        httpClient: HttpClient,
        @Named("nyt_books_api_key") apiKey: String
    ): NytBooksApi = NytBooksApiImpl(httpClient, apiKey)
}
