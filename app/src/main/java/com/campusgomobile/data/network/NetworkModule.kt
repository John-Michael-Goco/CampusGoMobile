package com.campusgomobile.data.network

import com.campusgomobile.BuildConfig
import com.campusgomobile.data.api.AchievementsApi
import com.campusgomobile.data.api.ActivityApi
import com.campusgomobile.data.api.AuthApi
import com.campusgomobile.data.api.LeaderboardApi
import com.campusgomobile.data.api.TransactionsApi
import com.campusgomobile.data.api.InventoryApi
import com.campusgomobile.data.api.FcmApi
import com.campusgomobile.data.api.PointsTransferApi
import com.campusgomobile.data.api.ParticipantsApi
import com.campusgomobile.data.api.QuestsApi
import com.campusgomobile.data.api.StoreApi
import com.campusgomobile.data.api.UserApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // API_BASE_URL: set in app/build.gradle.kts (buildConfigField).
    // Emulator = "http://10.0.2.2:8000/". Physical phone = "http://YOUR_PC_IP:8000/" (e.g. http://192.168.1.100:8000/).
    private val BASE_URL: String get() = BuildConfig.API_BASE_URL

    val baseUrl: String get() = BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val apiHeadersInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiHeadersInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    fun createAuthenticatedClient(token: String): OkHttpClient {
        return okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun createUserApi(client: OkHttpClient): UserApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApi::class.java)

    fun createStoreApi(client: OkHttpClient): StoreApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StoreApi::class.java)

    fun createAchievementsApi(client: OkHttpClient): AchievementsApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AchievementsApi::class.java)

    fun createActivityApi(client: OkHttpClient): ActivityApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ActivityApi::class.java)

    fun createTransactionsApi(client: OkHttpClient): TransactionsApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TransactionsApi::class.java)

    fun createLeaderboardApi(client: OkHttpClient): LeaderboardApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LeaderboardApi::class.java)

    fun createInventoryApi(client: OkHttpClient): InventoryApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InventoryApi::class.java)

    fun createQuestsApi(client: OkHttpClient): QuestsApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuestsApi::class.java)

    fun createParticipantsApi(client: OkHttpClient): ParticipantsApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ParticipantsApi::class.java)

    fun createPointsTransferApi(client: OkHttpClient): PointsTransferApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PointsTransferApi::class.java)

    fun createFcmApi(client: OkHttpClient): FcmApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FcmApi::class.java)
}
