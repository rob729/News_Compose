package com.example.jetpacknews.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewsService {

    private val BASE_URL = "https://newsapi.org/v2/"

    fun initalizeRetrofit(): NewsApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(NewsApi::class.java)
    }
}