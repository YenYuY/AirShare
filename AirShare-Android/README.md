# AirShare for Android

AirShare 是獨立的原生 Android 專案。它會在同一個 Wi-Fi 內建立暫時的 HTTPS 服務，讓電腦瀏覽器下載手機允許的照片與影片，不需要帳號、雲端或桌面程式。

## 技術基準

- Kotlin、Jetpack Compose、Material 3
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

- 每次分享產生新的 P-256 自簽 TLS 憑證與 192-bit URL-safe token。
- 憑證 SAN 綁定目前的 Wi-Fi IP；網址與 QR Code 帶有暫時 token。
- 只允許 TLS 1.2／1.3，明文 HTTP 已停用。
- App 進入背景、Wi-Fi 中斷或相簿權限撤銷時會停止分享。
- 伺服器限制 header 大小、讀取期限與同時連線數，並正確處理 Range 下載。

因憑證是在手機上臨時建立，桌面瀏覽器會顯示本機自簽憑證警告。使用者應核對 App 顯示的 SHA-256 憑證指紋後再繼續。

## Google Play

上架前依序完成：

- [Release checklist](docs/PLAY_RELEASE_CHECKLIST.md)
- [Privacy policy draft](docs/PRIVACY_POLICY.md)
- [Data Safety worksheet](docs/DATA_SAFETY.md)
- [Photo/video permission declaration](docs/MEDIA_PERMISSION_DECLARATION.md)
- [Build verification](docs/BUILD_VERIFICATION.md)
- `play/listing/` 內的三語商店文案

Play 的政策與 target API 會更新；正式送審當天請重新查核官方規定。
