package dev.oneuiproject.oneuiexample.data.stargazers.network.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.github.com/"

    /**
     * To allow more than 60 requests per hour, you will need
     * the `addHeader` line below to include a github access token (no scope).
     * https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
    */
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                //.addHeader("Authorization", "Bearer <GITHUB_API_KEY>")
                .build()
            chain.proceed(request)
        }
        .build()

    @JvmStatic
    val instance: GitHubService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubService::class.java)
    }
}
