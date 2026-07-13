package com.yenyuyyy.airshare.server

interface WebStrings {
    val htmlLanguage: String
        get() = "en"
    val appName: String
    val previousPage: String
    val nextPage: String
    val downloadHint: String
    val videoBadge: String
    val emptyTitle: String
    val emptyMessage: String
    val unauthorizedTitle: String
    val unauthorizedMessage: String

    fun pageInfo(page: Int, pageCount: Int): String
}
