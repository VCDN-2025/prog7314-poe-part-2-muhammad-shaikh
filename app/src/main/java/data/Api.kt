package za.co.studysync.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Api {
    private var token: String? = null

    private val log = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(log)
        .addInterceptor { chain ->
            val b = chain.request().newBuilder()
            token?.let { b.addHeader("Authorization", "Bearer $it") }
            chain.proceed(b.build())
        }
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())   // <-- important for Kotlin data classes
        .build()

    // Emulator -> PC localhost
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5041/") // change if using phone/LAN IP or deployed URL
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val svc: ApiService = retrofit.create(ApiService::class.java)

    fun setToken(t: String) { token = t }
    fun clearToken() { token = null }
}
