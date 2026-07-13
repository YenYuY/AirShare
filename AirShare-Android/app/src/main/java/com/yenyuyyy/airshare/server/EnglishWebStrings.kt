package com.yenyuyyy.airshare.server

object EnglishWebStrings : WebStrings {
    override val appName = "AirShare"
    override val previousPage = "Previous"
    override val nextPage = "Next"
    override val downloadHint = "Tap a thumbnail to download the original file."
    override val videoBadge = "VIDEO"
    override val emptyTitle = "No media to share"
    override val emptyMessage = "No photos or videos are available with the current permission."
    override val unauthorizedTitle = "Share link unavailable"
    override val unauthorizedMessage = "Use the complete link or QR code shown in AirShare."

    override fun pageInfo(page: Int, pageCount: Int): String = "Page $page / $pageCount"
}
