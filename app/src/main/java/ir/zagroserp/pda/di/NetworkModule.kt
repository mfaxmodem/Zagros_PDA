package ir.zagroserp.pda.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.zagroserp.pda.BuildConfig
import ir.zagroserp.pda.data.remote.api.TokenRefreshApi
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.interceptor.AuthInterceptor
import ir.zagroserp.pda.data.remote.interceptor.BaseUrlInterceptor
import ir.zagroserp.pda.data.remote.interceptor.TokenAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L
    private const val CACHE_SIZE_BYTES = 20L * 1024 * 1024

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http_cache"), CACHE_SIZE_BYTES)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * Bare client: no auth, no authenticator. Used for `/auth/refresh` and the server probe.
     * The base-URL interceptor is present so refresh calls follow the configured host; the probe
     * opts out per-request via `X-Skip-Base-Url`.
     */
    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(4, 5, TimeUnit.MINUTES))
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(logging)
            .build()

    /** Authenticated client used by the whole application. */
    @Provides
    @Singleton
    @AuthClient
    fun provideAuthClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
        cache: Cache,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .cache(cache)
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .authenticator(authenticator)
        .build()

    /**
     * Long-lived client for Server-Sent Events. `readTimeout(0)` is mandatory so the
     * connection stays open for the lifetime of the stream.
     */
    @Provides
    @Singleton
    @BaseClient
    fun provideSseClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @AuthClient client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @RefreshRetrofit
    fun provideRefreshRetrofit(
        @RefreshClient client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideVisitorApi(retrofit: Retrofit): VisitorApi = retrofit.create(VisitorApi::class.java)

    @Provides
    @Singleton
    fun provideTokenRefreshApi(@RefreshRetrofit retrofit: Retrofit): TokenRefreshApi =
        retrofit.create(TokenRefreshApi::class.java)
}
