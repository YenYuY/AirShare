package com.yenyuyyy.airshare.language

import android.os.LocaleList
import java.util.Locale

enum class AppLanguage {
    ENGLISH,
    TRADITIONAL_CHINESE,
    JAPANESE,
}

enum class AppLanguageOption {
    AUTO,
    TRADITIONAL_CHINESE,
    ENGLISH,
    JAPANESE,
    ;

    fun resolve(locales: LocaleList = LocaleList.getDefault()): AppLanguage = when (this) {
        TRADITIONAL_CHINESE -> AppLanguage.TRADITIONAL_CHINESE
        ENGLISH -> AppLanguage.ENGLISH
        JAPANESE -> AppLanguage.JAPANESE
        AUTO -> detectLanguage(locales)
    }

    private fun detectLanguage(locales: LocaleList): AppLanguage {
        for (index in 0 until locales.size()) {
            when (locales[index].language.lowercase(Locale.ROOT)) {
                "ja" -> return AppLanguage.JAPANESE
                "zh" -> return AppLanguage.TRADITIONAL_CHINESE
                "en" -> return AppLanguage.ENGLISH
            }
        }
        return AppLanguage.ENGLISH
    }
}

data class WebText(
    val title: String,
    val subtitle: String,
    val videoBadge: String,
    val previous: String,
    val next: String,
    val page: (Int, Int) -> String,
    val downloadHint: String,
    val emptyTitle: String,
    val emptyBody: String,
    val accessDeniedTitle: String,
    val accessDeniedBody: String,
    val sessionExpiredTitle: String,
    val sessionExpiredBody: String,
)

data class AppText(
    val appName: String,
    val tagline: String,
    val languageLabel: String,
    val languageAuto: String,
    val languageTraditionalChinese: String,
    val languageEnglish: String,
    val languageJapanese: String,
    val privacyLabel: String,
    val privacyBody: String,
    val privacyDetailsLabel: String,
    val privacyDetailsBody: String,
    val permissionTitle: String,
    val permissionBody: String,
    val grantPermission: String,
    val openSettings: String,
    val partialAccessTitle: String,
    val partialAccessBody: String,
    val changeSelection: String,
    val wifiRequiredTitle: String,
    val wifiRequiredBody: String,
    val readyTitle: String,
    val readyBody: String,
    val startSharing: String,
    val starting: String,
    val activeTitle: String,
    val activeBody: String,
    val openOnComputer: String,
    val copyAddress: String,
    val copied: String,
    val stopSharing: String,
    val encryptedLabel: String,
    val localOnlyLabel: String,
    val keepAwakeHint: String,
    val certificateHint: String,
    val fingerprintLabel: String,
    val howItWorks: String,
    val stepOne: String,
    val stepTwo: String,
    val stepThree: String,
    val wifiLost: String,
    val networkChanged: String,
    val accessLost: String,
    val localNetworkDenied: String,
    val genericError: String,
    val dismiss: String,
    val web: WebText,
) {
    fun languageOption(option: AppLanguageOption): String = when (option) {
        AppLanguageOption.AUTO -> languageAuto
        AppLanguageOption.TRADITIONAL_CHINESE -> languageTraditionalChinese
        AppLanguageOption.ENGLISH -> languageEnglish
        AppLanguageOption.JAPANESE -> languageJapanese
    }
}

fun textFor(language: AppLanguage): AppText = when (language) {
    AppLanguage.ENGLISH -> englishText
    AppLanguage.TRADITIONAL_CHINESE -> traditionalChineseText
    AppLanguage.JAPANESE -> japaneseText
}

private val englishText = AppText(
    appName = "AirShare",
    tagline = "Photos, straight to your computer",
    languageLabel = "Language",
    languageAuto = "Auto (system)",
    languageTraditionalChinese = "繁體中文",
    languageEnglish = "English",
    languageJapanese = "日本語",
    privacyLabel = "Private by design",
    privacyBody = "AirShare creates a temporary encrypted connection on your Wi-Fi. Your media is never uploaded to us.",
    privacyDetailsLabel = "Privacy details",
    privacyDetailsBody = "AirShare reads only the photos and videos Android allows so it can build your local browser gallery. Transfers go directly to the browser on your Wi-Fi over HTTPS. AirShare has no account, cloud storage, analytics, ads, or developer-operated server. The temporary access token expires when sharing stops. You can change media access at any time in Android Settings.",
    permissionTitle = "Choose what AirShare can share",
    permissionBody = "Photo and video access is needed so your computer can browse and download media you allow.",
    grantPermission = "Allow photo access",
    openSettings = "Open settings",
    partialAccessTitle = "Limited media access",
    partialAccessBody = "Only the media types or selected items you allowed will appear in the browser.",
    changeSelection = "Change selection",
    wifiRequiredTitle = "Connect to Wi-Fi",
    wifiRequiredBody = "Your phone and computer must be on the same trusted Wi-Fi network.",
    readyTitle = "Ready to share",
    readyBody = "Start a temporary session, then scan the QR code or enter the address on your computer.",
    startSharing = "Start secure sharing",
    starting = "Starting secure session…",
    activeTitle = "Sharing is on",
    activeBody = "Keep AirShare open while you download. Stop the session when you are finished.",
    openOnComputer = "Open this address on your computer",
    copyAddress = "Copy address",
    copied = "Address copied",
    stopSharing = "Stop sharing",
    encryptedLabel = "Encrypted",
    localOnlyLabel = "Same Wi-Fi only",
    keepAwakeHint = "The screen will stay awake while sharing is active.",
    certificateHint = "Because this secure session is created on your phone, your browser may ask you to continue past a local certificate warning.",
    fingerprintLabel = "Certificate fingerprint",
    howItWorks = "How it works",
    stepOne = "Allow access to the photos and videos you want to share.",
    stepTwo = "Start sharing while both devices use the same Wi-Fi.",
    stepThree = "Scan the QR code and download originals in your browser.",
    wifiLost = "Wi-Fi disconnected, so AirShare stopped sharing.",
    networkChanged = "The Wi-Fi connection changed, so AirShare stopped sharing. Start a new session to use the new address.",
    accessLost = "Photo access changed, so AirShare stopped sharing.",
    localNetworkDenied = "Local network access is required to share with your computer.",
    genericError = "AirShare could not start the secure session.",
    dismiss = "Dismiss",
    web = WebText(
        title = "AirShare",
        subtitle = "Download original photos and videos from this phone",
        videoBadge = "VIDEO",
        previous = "Previous",
        next = "Next",
        page = { page, total -> "Page $page of $total" },
        downloadHint = "Select a thumbnail to download the original file.",
        emptyTitle = "No media available",
        emptyBody = "Allow more photos on the phone, then refresh this page.",
        accessDeniedTitle = "Photo access is unavailable",
        accessDeniedBody = "Open AirShare on the phone and allow photo access, then refresh.",
        sessionExpiredTitle = "This session has ended",
        sessionExpiredBody = "Start a new sharing session in AirShare and use its new QR code.",
    ),
)

private val traditionalChineseText = AppText(
    appName = "AirShare",
    tagline = "照片，直接傳到電腦",
    languageLabel = "語言",
    languageAuto = "自動（跟隨系統）",
    languageTraditionalChinese = "繁體中文",
    languageEnglish = "English",
    languageJapanese = "日本語",
    privacyLabel = "隱私優先",
    privacyBody = "AirShare 只會在目前 Wi-Fi 建立暫時的加密連線，不會將你的媒體上傳給我們。",
    privacyDetailsLabel = "隱私權詳細資訊",
    privacyDetailsBody = "AirShare 只會讀取 Android 允許的照片與影片，用來建立區域網路瀏覽頁面。檔案透過 HTTPS 直接傳到同一 Wi-Fi 內的瀏覽器。AirShare 沒有帳號、雲端儲存、分析、廣告或開發者伺服器。暫時存取 token 會在停止分享後失效，你也能隨時在 Android 設定變更媒體權限。",
    permissionTitle = "選擇 AirShare 可以分享的內容",
    permissionBody = "需要照片與影片權限，電腦才能瀏覽及下載你允許的媒體。",
    grantPermission = "允許照片權限",
    openSettings = "開啟設定",
    partialAccessTitle = "有限媒體權限",
    partialAccessBody = "瀏覽器只會顯示你允許的媒體類型或已選取項目。",
    changeSelection = "變更選取項目",
    wifiRequiredTitle = "請連接 Wi-Fi",
    wifiRequiredBody = "手機與電腦必須連上同一個可信任的 Wi-Fi。",
    readyTitle = "可以開始分享",
    readyBody = "啟動暫時工作階段，再用電腦掃描 QR Code 或輸入網址。",
    startSharing = "開始安全分享",
    starting = "正在啟動安全連線…",
    activeTitle = "正在分享",
    activeBody = "下載期間請保持 AirShare 開啟，完成後請停止分享。",
    openOnComputer = "在電腦開啟這個網址",
    copyAddress = "複製網址",
    copied = "已複製網址",
    stopSharing = "停止分享",
    encryptedLabel = "加密傳輸",
    localOnlyLabel = "僅限同一 Wi-Fi",
    keepAwakeHint = "分享期間螢幕會保持開啟。",
    certificateHint = "安全憑證由你的手機臨時建立，瀏覽器可能會要求你確認本機憑證警告後繼續。",
    fingerprintLabel = "憑證指紋",
    howItWorks = "使用方式",
    stepOne = "允許 AirShare 存取你想分享的照片與影片。",
    stepTwo = "確認兩台裝置使用同一 Wi-Fi，然後開始分享。",
    stepThree = "掃描 QR Code，在瀏覽器下載原始檔。",
    wifiLost = "Wi-Fi 已中斷，AirShare 已停止分享。",
    networkChanged = "Wi-Fi 連線已變更，AirShare 已停止分享。請重新開始，以使用新的網址。",
    accessLost = "照片權限已變更，AirShare 已停止分享。",
    localNetworkDenied = "需要區域網路權限，才能與你的電腦分享。",
    genericError = "AirShare 無法啟動安全連線。",
    dismiss = "關閉",
    web = WebText(
        title = "AirShare",
        subtitle = "從這支手機下載原始照片與影片",
        videoBadge = "影片",
        previous = "上一頁",
        next = "下一頁",
        page = { page, total -> "第 $page / $total 頁" },
        downloadHint = "點選縮圖即可下載原始檔案。",
        emptyTitle = "沒有可分享的媒體",
        emptyBody = "請在手機允許更多照片，然後重新整理此頁。",
        accessDeniedTitle = "無法存取照片",
        accessDeniedBody = "請在手機開啟 AirShare 並允許照片權限，再重新整理。",
        sessionExpiredTitle = "分享工作階段已結束",
        sessionExpiredBody = "請在 AirShare 啟動新的分享工作階段，並使用新的 QR Code。",
    ),
)

private val japaneseText = AppText(
    appName = "AirShare",
    tagline = "写真を、そのままパソコンへ",
    languageLabel = "言語",
    languageAuto = "自動（システム）",
    languageTraditionalChinese = "繁體中文",
    languageEnglish = "English",
    languageJapanese = "日本語",
    privacyLabel = "プライバシーを優先",
    privacyBody = "AirShare は Wi-Fi 内に一時的な暗号化接続を作成します。メディアが当社へアップロードされることはありません。",
    privacyDetailsLabel = "プライバシーの詳細",
    privacyDetailsBody = "AirShare は Android が許可した写真と動画だけを読み取り、ローカルのブラウザギャラリーを作成します。転送は同じ Wi-Fi 上のブラウザへ HTTPS で直接行われます。アカウント、クラウド保存、分析、広告、開発者運営サーバーはありません。一時アクセストークンは共有停止時に失効し、メディア権限は Android の設定でいつでも変更できます。",
    permissionTitle = "AirShare で共有する項目を選択",
    permissionBody = "パソコンで許可した写真や動画を表示・ダウンロードするため、アクセス権が必要です。",
    grantPermission = "写真へのアクセスを許可",
    openSettings = "設定を開く",
    partialAccessTitle = "制限付きメディアアクセス",
    partialAccessBody = "許可したメディアの種類または選択した項目だけがブラウザに表示されます。",
    changeSelection = "選択を変更",
    wifiRequiredTitle = "Wi-Fi に接続してください",
    wifiRequiredBody = "スマートフォンとパソコンを同じ信頼できる Wi-Fi に接続してください。",
    readyTitle = "共有の準備ができました",
    readyBody = "一時セッションを開始し、パソコンで QR コードを読み取るかアドレスを入力します。",
    startSharing = "安全な共有を開始",
    starting = "安全なセッションを開始しています…",
    activeTitle = "共有中",
    activeBody = "ダウンロード中は AirShare を開いたままにし、完了したら共有を停止してください。",
    openOnComputer = "パソコンでこのアドレスを開く",
    copyAddress = "アドレスをコピー",
    copied = "アドレスをコピーしました",
    stopSharing = "共有を停止",
    encryptedLabel = "暗号化",
    localOnlyLabel = "同じ Wi-Fi のみ",
    keepAwakeHint = "共有中は画面がスリープしません。",
    certificateHint = "安全な証明書はスマートフォン上で一時的に作成されるため、ブラウザでローカル証明書の警告を確認して続行する場合があります。",
    fingerprintLabel = "証明書のフィンガープリント",
    howItWorks = "使い方",
    stepOne = "共有する写真と動画へのアクセスを許可します。",
    stepTwo = "両方の端末を同じ Wi-Fi に接続して共有を開始します。",
    stepThree = "QR コードを読み取り、ブラウザで元のファイルをダウンロードします。",
    wifiLost = "Wi-Fi が切断されたため、共有を停止しました。",
    networkChanged = "Wi-Fi 接続が変わったため、共有を停止しました。新しいアドレスでセッションを開始し直してください。",
    accessLost = "写真へのアクセスが変更されたため、共有を停止しました。",
    localNetworkDenied = "パソコンと共有するにはローカルネットワークへのアクセスが必要です。",
    genericError = "安全なセッションを開始できませんでした。",
    dismiss = "閉じる",
    web = WebText(
        title = "AirShare",
        subtitle = "このスマートフォンから元の写真と動画をダウンロード",
        videoBadge = "動画",
        previous = "前へ",
        next = "次へ",
        page = { page, total -> "$page / $total ページ" },
        downloadHint = "サムネイルを選択すると元のファイルをダウンロードできます。",
        emptyTitle = "共有できるメディアがありません",
        emptyBody = "スマートフォンで写真を追加許可してから、このページを更新してください。",
        accessDeniedTitle = "写真にアクセスできません",
        accessDeniedBody = "スマートフォンで AirShare を開き、写真へのアクセスを許可してから更新してください。",
        sessionExpiredTitle = "共有セッションは終了しました",
        sessionExpiredBody = "AirShare で新しい共有セッションを開始し、新しい QR コードを使用してください。",
    ),
)
