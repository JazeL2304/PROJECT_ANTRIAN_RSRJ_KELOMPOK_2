package com.example.project_antrian_rsrj_kelompok_2.model

data class NewsResponse(
    val results: List<NewsItem>?
)

data class NewsItem(
    val title: String?,
    val link: String?,
    val description: String?,
    val pubDate: String?
)
