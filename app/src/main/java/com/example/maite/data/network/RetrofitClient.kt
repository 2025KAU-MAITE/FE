package com.example.maite.data.network

import android.content.Context
import com.example.maite.AuthInterceptor
import com.example.maite.PreferencesUtil
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://3.39.205.32:8080/"
    private var retrofit: Retrofit? = null
    
    fun getRetrofit(context: Context): Retrofit {
        if (retrofit == null) {
            synchronized(this) {
                if (retrofit == null) {
                    val preferencesUtil = PreferencesUtil(context.applicationContext)
                    val authInterceptor = AuthInterceptor(preferencesUtil)
                    
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    
                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .addInterceptor(authInterceptor)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()
                    
                    retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
        }
        return retrofit!!
    }
}
