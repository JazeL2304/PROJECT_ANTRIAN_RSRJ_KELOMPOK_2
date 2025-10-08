package com.example.project_antrian_rsrj_kelompok_2.api

import com.example.project_antrian_rsrj_kelompok_2.model.NewsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
interface NewsApiService {
    @GET("api/1/latest")
    fun getHealthNews(
        @Query("apikey") apiKey: String,
        @Query("q") query: String = "health"
    ): Call<NewsResponse>
}
