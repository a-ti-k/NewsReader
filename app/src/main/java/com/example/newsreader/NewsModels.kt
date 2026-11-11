package com.example.newsreader

data class NewsSource(
    val name: String,
    val url: String,
    val categories: List<String>,
    val region: String
)

data class NewsArticle(
    val title: String,
    val link: String,
    val source: String,
    val published: String,
    val excerpt: String
)

enum class LoadState { LOADING, SUCCESS, EMPTY, ERROR }
