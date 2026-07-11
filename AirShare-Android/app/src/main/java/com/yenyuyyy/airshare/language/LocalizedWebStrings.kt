package com.yenyuyyy.airshare.language

import com.yenyuyyy.airshare.server.WebStrings

class LocalizedWebStrings(
    language: AppLanguage,
) : WebStrings {
    @Volatile
    private var currentLanguage: AppLanguage = language

    fun update(language: AppLanguage) {
        currentLanguage = language
    }

    private val text: AppText
        get() = textFor(currentLanguage)

    override val htmlLanguage: String
        get() = when (currentLanguage) {
            AppLanguage.ENGLISH -> "en"
            AppLanguage.TRADITIONAL_CHINESE -> "zh-Hant"
            AppLanguage.JAPANESE -> "ja"
        }
    override val appName: String
        get() = text.web.title
    override val previousPage: String
        get() = text.web.previous
    override val nextPage: String
        get() = text.web.next
    override val downloadHint: String
        get() = text.web.downloadHint
    override val videoBadge: String
        get() = text.web.videoBadge
    override val emptyTitle: String
        get() = text.web.emptyTitle
    override val emptyMessage: String
        get() = text.web.emptyBody
    override val unauthorizedTitle: String
        get() = text.web.sessionExpiredTitle
    override val unauthorizedMessage: String
        get() = text.web.sessionExpiredBody

    override fun pageInfo(page: Int, pageCount: Int): String = text.web.page(page, pageCount)
}
