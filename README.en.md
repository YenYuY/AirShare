# AirShare

[繁體中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [Website](https://yenyuy.github.io/AirShare/)

AirShare is a native mobile app for transferring photos and videos directly between your phone and computer on the same Wi-Fi network. No account, cable, cloud upload, or desktop software is required. Start sharing on your phone, open the displayed address in a computer browser, then browse and download the original files.

![AirShare overview](AppStore/Marketing/website-hero-1920x1080.jpg)

## Features

- Native apps for iOS and Android
- Local-network-only photo and video transfer
- Works in a computer browser with no desktop installation
- Downloads original photos and videos
- QR code and local address for quick connection
- Traditional Chinese, English, and Japanese interfaces
- No account and no third-party cloud storage step

## How it works

1. Allow AirShare to access your photos and videos.
2. Connect your phone and computer to the same Wi-Fi network.
3. Start sharing in AirShare.
4. Scan the QR code or enter the displayed address in your computer browser.
5. Select and download the photos or videos you need.

> Keep AirShare open and your phone awake during a transfer. Only start sharing on a trusted local network.

## Screenshots

| Ready to share | Sharing |
| --- | --- |
| <img src="AppStore/Raw/airshare-home-iphone17pro.png" alt="AirShare home screen" width="320"> | <img src="AppStore/Raw/airshare-ready-iphone17pro.png" alt="AirShare sharing screen" width="320"> |

## Repository layout

- `AirShare/`: iOS app built with SwiftUI
- `AirShare-Android/`: Android app built with Kotlin and Jetpack Compose
- `AppStore/`: store copy, source screenshots, and marketing artwork
- `index.html`: GitHub Pages product website

## Development requirements

### iOS

- Xcode
- iOS 16.0 or later

Open `AirShare.xcodeproj` in Xcode, select a signing team and target device, then run the app.

### Android

- Android Studio or JDK 17
- Android SDK 36
- Android 10 (API 29) or later

```bash
cd AirShare-Android
./gradlew assembleDebug
```

## Privacy

AirShare's core transfer flow stays on the user's own local network. Media does not need to be uploaded to AirShare or a third-party cloud service. Platform-specific privacy and release documentation is available in [`AirShare-Android/docs/`](AirShare-Android/docs/).
