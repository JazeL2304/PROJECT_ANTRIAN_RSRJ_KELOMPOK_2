package com.example.projectantrianrsrjkelompok2.model

data class NewsResponse(
    val results: List<NewsItem>?
)

data class NewsItem(
    val title: String?,
    val link: String?,
    val description: String?,
    val pubDate: String?
)
