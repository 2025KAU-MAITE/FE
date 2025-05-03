package com.example.maite

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MyRetrofit {

    private const val BASE_URL = "http://3.39.205.32:8080/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val timetableApi: TimetableApi by lazy {
        retrofit.create(TimetableApi::class.java)
    }
}
