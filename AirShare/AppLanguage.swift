//
//  AppLanguage.swift
//  AirShare
//
//  App-wide language options, auto-detection, and localized strings.
//

import Foundation
import Photos
import Combine

enum AppLanguage: String {
    case zhHant = "zh-Hant"
    case english = "en"
    case japanese = "ja"

    var htmlLangCode: String {
        rawValue
    }
}

enum AppLanguageOption: String, CaseIterable, Identifiable {
    case auto
    case zhHant
    case english
    case japanese

    var id: String { rawValue }

    func resolvedLanguage(locale: Locale = .autoupdatingCurrent) -> AppLanguage {
        switch self {
        case .auto:
            return Self.detectLanguage(from: locale)
        case .zhHant:
            return .zhHant
        case .english:
            return .english
        case .japanese:
            return .japanese
        }
    }

    private static func detectLanguage(from locale: Locale) -> AppLanguage {
        let id = locale.identifier.lowercased()
        if id.hasPrefix("ja") {
            return .japanese
        }
        if id.hasPrefix("zh-hant") || id.hasPrefix("zh_tw") || id.hasPrefix("zh-hk") || id.hasPrefix("zh_mo") || id.hasPrefix("zh") {
            return .zhHant
        }
        if id.hasPrefix("en") {
            return .english
        }
        return .english
    }
}

final class AppLanguageManager: ObservableObject {
    static let shared = AppLanguageManager()

    @Published private(set) var selectedOption: AppLanguageOption
    @Published private(set) var effectiveLanguage: AppLanguage

    private static let storageKey = "AppLanguageOption"
    private let stateQueue = DispatchQueue(label: "AppLanguageManager.state")
    private var currentLanguageState: AppLanguage
    private var localeObserver: NSObjectProtocol?

    private init() {
        let saved = UserDefaults.standard.string(forKey: Self.storageKey)
        let option = AppLanguageOption(rawValue: saved ?? "") ?? .auto
        let resolved = option.resolvedLanguage()
        selectedOption = option
        effectiveLanguage = resolved
        currentLanguageState = resolved

        localeObserver = NotificationCenter.default.addObserver(
            forName: NSLocale.currentLocaleDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.onSystemLocaleChanged()
        }
    }

    deinit {
        if let observer = localeObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    func setLanguageOption(_ option: AppLanguageOption) {
        guard option != selectedOption else { return }
        selectedOption = option
        UserDefaults.standard.set(option.rawValue, forKey: Self.storageKey)
        updateEffectiveLanguage()
    }

    func currentLanguage() -> AppLanguage {
        stateQueue.sync { currentLanguageState }
    }

    func refreshForCurrentSystemLanguageIfAuto() {
        guard selectedOption == .auto else { return }
        updateEffectiveLanguage()
    }

    private func onSystemLocaleChanged() {
        guard selectedOption == .auto else { return }
        updateEffectiveLanguage()
    }

    private func updateEffectiveLanguage() {
        let resolved = selectedOption.resolvedLanguage()
        effectiveLanguage = resolved
        stateQueue.sync {
            currentLanguageState = resolved
        }
    }
}

struct AppStrings {
    let language: AppLanguage

    var appName: String { "AirShare" }

    var languageLabel: String {
        switch language {
        case .zhHant: return "語言"
        case .english: return "Language"
        case .japanese: return "言語"
        }
    }

    func languageOptionLabel(_ option: AppLanguageOption) -> String {
        switch option {
        case .auto:
            switch language {
            case .zhHant: return "自動（跟隨系統）"
            case .english: return "Auto (System)"
            case .japanese: return "自動（システム）"
            }
        case .zhHant:
            return "繁體中文"
        case .english:
            return "English"
        case .japanese:
            return "日本語"
        }
    }

    var photoPermissionNeeded: String {
        switch language {
        case .zhHant: return "需要相簿權限以提供照片與影片下載。"
        case .english: return "Photo access is required to provide photo and video downloads."
        case .japanese: return "写真と動画をダウンロードできるようにするには、写真へのアクセスが必要です。"
        }
    }

    var requestingPermissionButton: String {
        switch language {
        case .zhHant: return "請求中…"
        case .english: return "Requesting..."
        case .japanese: return "リクエスト中..."
        }
    }

    var requestPermissionButton: String {
        switch language {
        case .zhHant: return "請求相簿權限"
        case .english: return "Request Photo Access"
        case .japanese: return "写真アクセスを許可"
        }
    }

    var stopServerButton: String {
        switch language {
        case .zhHant: return "停止伺服器"
        case .english: return "Stop Server"
        case .japanese: return "サーバー停止"
        }
    }

    var startServerButton: String {
        switch language {
        case .zhHant: return "啟動伺服器"
        case .english: return "Start Server"
        case .japanese: return "サーバー起動"
        }
    }

    var serverStarted: String {
        switch language {
        case .zhHant: return "伺服器已啟動："
        case .english: return "Server started:"
        case .japanese: return "サーバー起動済み:"
        }
    }

    var serverStopped: String {
        switch language {
        case .zhHant: return "伺服器未啟動"
        case .english: return "Server is not running"
        case .japanese: return "サーバーは停止中です"
        }
    }

    var keepScreenOnHint: String {
        switch language {
        case .zhHant: return "請保持手機螢幕不要鎖定，並確保手機與電腦在同一個 Wi-Fi 內。"
        case .english: return "Keep this phone awake, and make sure your phone and computer are on the same Wi-Fi network."
        case .japanese: return "スマートフォンの画面をロックせず、スマートフォンとPCが同じWi-Fiに接続されていることを確認してください。"
        }
    }

    var wifiAlertTitle: String {
        switch language {
        case .zhHant: return "請連接 Wi-Fi"
        case .english: return "Please Connect to Wi-Fi"
        case .japanese: return "Wi-Fi に接続してください"
        }
    }

    var okButton: String {
        switch language {
        case .zhHant: return "好"
        case .english: return "OK"
        case .japanese: return "OK"
        }
    }

    var wifiAlertMessage: String {
        switch language {
        case .zhHant: return "目前未連接 Wi-Fi，請連接 Wi-Fi 後再啟動伺服器。"
        case .english: return "Wi-Fi is not connected. Please connect to Wi-Fi before starting the server."
        case .japanese: return "現在 Wi-Fi に接続されていません。サーバーを起動する前に Wi-Fi に接続してください。"
        }
    }

    func authorizationMessage(for status: PHAuthorizationStatus) -> String {
        switch status {
        case .notDetermined:
            switch language {
            case .zhHant: return "請求相簿權限以繼續。"
            case .english: return "Please grant photo access to continue."
            case .japanese: return "続行するには写真へのアクセスを許可してください。"
            }
        case .denied, .restricted:
            switch language {
            case .zhHant: return "未授權相簿存取。請前往設定 > 隱私權 > 照片 開啟權限。"
            case .english: return "Photo access is not granted. Go to Settings > Privacy > Photos to enable access."
            case .japanese: return "写真へのアクセスが許可されていません。設定 > プライバシー > 写真 で許可してください。"
            }
        case .limited:
            switch language {
            case .zhHant: return "目前為有限相簿存取，可在設定中調整。"
            case .english: return "Limited photo access is currently enabled. You can change it in Settings."
            case .japanese: return "現在、写真へのアクセスは限定的です。設定で変更できます。"
            }
        case .authorized:
            return ""
        @unknown default:
            switch language {
            case .zhHant: return "未知的權限狀態。"
            case .english: return "Unknown authorization status."
            case .japanese: return "不明な権限状態です。"
            }
        }
    }

    func serverStartError(_ detail: String) -> String {
        switch language {
        case .zhHant: return "無法啟動伺服器：\(detail)"
        case .english: return "Unable to start server: \(detail)"
        case .japanese: return "サーバーを起動できません: \(detail)"
        }
    }

    var webPrevPage: String {
        switch language {
        case .zhHant: return "上一頁"
        case .english: return "Previous"
        case .japanese: return "前へ"
        }
    }

    var webNextPage: String {
        switch language {
        case .zhHant: return "下一頁"
        case .english: return "Next"
        case .japanese: return "次へ"
        }
    }

    func webPageInfo(page: Int, pageCount: Int) -> String {
        switch language {
        case .zhHant: return "第 \(page) / \(pageCount) 頁"
        case .english: return "Page \(page) / \(pageCount)"
        case .japanese: return "\(page) / \(pageCount) ページ"
        }
    }

    var webFooter: String {
        switch language {
        case .zhHant: return "點擊縮圖下載原始檔案。"
        case .english: return "Tap a thumbnail to download the original file."
        case .japanese: return "サムネイルをタップすると元ファイルをダウンロードできます。"
        }
    }

    var webConnectedLabel: String {
        switch language {
        case .zhHant: return "已連接至 iPhone"
        case .english: return "Connected to iPhone"
        case .japanese: return "iPhone に接続中"
        }
    }

    func webItemCount(_ count: Int) -> String {
        switch language {
        case .zhHant: return "本頁 (count) 個項目"
        case .english: return "\(count) items on this page"
        case .japanese: return "このページに \(count) 件"
        }
    }

    var webDownloadLabel: String {
        switch language {
        case .zhHant: return "下載原始檔"
        case .english: return "Download original"
        case .japanese: return "オリジナルをダウンロード"
        }
    }

    var webEmptyTitle: String {
        switch language {
        case .zhHant: return "沒有可顯示的照片"
        case .english: return "No photos to show"
        case .japanese: return "表示できる写真がありません"
        }
    }

    var webEmptyMessage: String {
        switch language {
        case .zhHant: return "請確認 AirShare 的相簿權限，或稍後再重新整理。"
        case .english: return "Check AirShare's photo access, or refresh this page later."
        case .japanese: return "AirShare の写真アクセスを確認するか、後でもう一度読み込んでください。"
        }
    }

    var webVideoBadge: String {
        switch language {
        case .zhHant: return "影片"
        case .english: return "VIDEO"
        case .japanese: return "動画"
        }
    }

    var webAuthorizationTitle: String {
        switch language {
        case .zhHant: return "AirShare - 需要授權"
        case .english: return "AirShare - Authorization Required"
        case .japanese: return "AirShare - 権限が必要です"
        }
    }

    var webAuthorizationHeading: String {
        switch language {
        case .zhHant: return "需要相簿授權"
        case .english: return "Photo Permission Required"
        case .japanese: return "写真の許可が必要です"
        }
    }

    var webAuthorizationMessage: String {
        switch language {
        case .zhHant: return "請在 iPhone 上開啟 AirShare 並允許「照片」權限後再重新整理此頁面。"
        case .english: return "Open AirShare on your iPhone and allow Photos access, then refresh this page."
        case .japanese: return "iPhone で AirShare を開き、「写真」へのアクセスを許可してからこのページを再読み込みしてください。"
        }
    }
}
