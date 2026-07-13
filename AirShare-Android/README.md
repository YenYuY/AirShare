# AirShare for Android

AirShare 是獨立的原生 Android 專案。它會在同一個 Wi-Fi 內建立暫時的 HTTP 服務，讓電腦瀏覽器下載手機允許的照片與影片，不需要帳號、雲端或桌面程式。

## 技術基準

- Kotlin、Jetpack Compose、Material 3
- Material 3 Adaptive Window Size Classes；支援平板、分割畫面與桌面自由視窗即時重排
- `minSdk 29`、`compileSdk 36`、`targetSdk 35`
- Application ID：`com.yenyuyyy.airshare.android`
- Gradle 9.4.1、Android Gradle Plugin 9.2.1、JDK 17+
- 繁體中文、English、日本語

> Application ID 第一次上架後不可更換。建立 Play Console App 前，請再次確認這個 ID。

## 在 Android Studio 開啟

1. 選擇 **File → Open**。
2. 開啟此 `AirShare-Android` 資料夾，而不是外層 iOS 專案。
3. 等待 Gradle Sync 完成。
4. 使用 Android 10（API 29）以上實機測試；相簿權限與區域網路流程不適合只用 Preview 驗證。

Android Studio 的 **Split / Design** 預覽中可直接查看 `AirSharePreviews.kt` 提供的手機、平板直向、平板橫向與桌面視窗尺寸。

## Adaptive UI

- Compact（寬度小於 600dp）：手機優先的單欄流程。
- Medium（600–839dp）：加寬的單欄，適合平板直向與較寬的分割視窗。
- Expanded（840–1199dp）：說明支援欄與主要操作欄並排，適合平板橫向。
- Large（1200dp 以上）：保留雙欄，並將 QR Code 與工作階段操作並排，適合桌面模式與大平板。
- 高度小於 480dp：不論裝置類型都退回緊湊單欄，避免手機橫向或矮桌面視窗擁擠。

所有判斷都依 App 當下可用視窗，而不是裝置型號；旋轉、分割畫面、摺疊與桌面視窗縮放時會即時重排。

## 驗證指令

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest
./gradlew lintDebug assembleDebug
./gradlew bundleRelease
```

輸出位置：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release AAB：`app/build/outputs/bundle/release/app-release.aab`

## 建立 Google Play upload key

建議使用 Android Studio 的 **Build → Generate Signed App Bundle or APK → Android App Bundle** 建立 upload key，並將它備份到安全位置。也可以：

1. 將 `keystore.properties.example` 複製為 `keystore.properties`。
2. 把 upload keystore 放在 `play/private/`，或修改 `storeFile` 指向安全位置。
3. 填入強密碼與 alias。
4. 再執行 `./gradlew bundleRelease`。
5. 用 `jarsigner -verify app/build/outputs/bundle/release/app-release.aab` 確認簽章。

`keystore.properties`、`*.jks` 與 `play/private/` 已排除於 Git。請勿提交金鑰或密碼。

## 安全模型

- 每次分享產生新的 192-bit URL-safe token，網址與 QR Code 都帶有這個暫時 token。
- 伺服器只綁定目前的 Wi-Fi IP，停止分享後 token 立即失效。
- 使用 HTTP 避免自簽憑證警告；區網內容不加密，因此只應在可信任的私人 Wi-Fi 使用。
- App 進入背景、Wi-Fi 中斷或相簿權限撤銷時會停止分享。
- 伺服器限制 header 大小、讀取期限與同時連線數，並正確處理 Range 下載。

任何取得完整暫時網址、且能連上相同區網的人都可能使用分享工作階段。只將 QR Code 或網址提供給預期的接收者，完成後立即停止分享。

## Google Play

上架前依序完成：

- [Release checklist](docs/PLAY_RELEASE_CHECKLIST.md)
- [Privacy policy draft](docs/PRIVACY_POLICY.md)
- [Data Safety worksheet](docs/DATA_SAFETY.md)
- [Photo/video permission declaration](docs/MEDIA_PERMISSION_DECLARATION.md)
- [Build verification](docs/BUILD_VERIFICATION.md)
- `play/listing/` 內的三語商店文案

Play 的政策與 target API 會更新；正式送審當天請重新查核官方規定。
