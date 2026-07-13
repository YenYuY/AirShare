# AirShare

[繁體中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [紹介サイト](https://yenyuy.github.io/AirShare/)

AirShareは、同じWi-Fiネットワーク上のスマートフォンとパソコンの間で、写真や動画を直接転送するネイティブアプリです。アカウント、ケーブル、クラウドへのアップロード、パソコン用ソフトは必要ありません。スマートフォンで共有を開始し、表示されたアドレスをパソコンのブラウザで開くだけで、オリジナルファイルを閲覧・ダウンロードできます。

![AirShareの概要](AppStore/Marketing/website-hero-1920x1080.jpg)

## 特長

- iOSとAndroidに対応
- 写真や動画をローカルネットワーク内だけで転送
- パソコンへのインストール不要。ブラウザから利用可能
- オリジナルの写真・動画をダウンロード
- QRコードとローカルアドレスですばやく接続
- 繁体字中国語、英語、日本語に対応
- アカウント登録不要、外部クラウドストレージも不要

## 使い方

1. AirShareに写真と動画へのアクセスを許可します。
2. スマートフォンとパソコンを同じWi-Fiに接続します。
3. AirShareで共有を開始します。
4. QRコードを読み取るか、表示されたアドレスをパソコンのブラウザに入力します。
5. 必要な写真や動画を選んでダウンロードします。

> 転送中はAirShareを開いたままにし、スマートフォンがスリープしないようにしてください。信頼できるローカルネットワークでのみ共有を開始してください。

## スクリーンショット

| 共有前 | 共有中 |
| --- | --- |
| <img src="AppStore/Raw/airshare-home-iphone17pro.png" alt="AirShareのホーム画面" width="320"> | <img src="AppStore/Raw/airshare-ready-iphone17pro.png" alt="AirShareの共有画面" width="320"> |

## リポジトリ構成

- `AirShare/`：SwiftUI製iOSアプリ
- `AirShare-Android/`：Kotlin／Jetpack Compose製Androidアプリ
- `AppStore/`：ストア文面、元スクリーンショット、マーケティング素材
- `index.html`：GitHub Pagesの紹介サイト

## 開発要件

### iOS

- Xcode
- iOS 16.0以降

Xcodeで`AirShare.xcodeproj`を開き、署名チームと対象デバイスを選択して実行します。

### Android

- Android StudioまたはJDK 17
- Android SDK 36
- Android 10（API 29）以降

```bash
cd AirShare-Android
./gradlew assembleDebug
```

## プライバシー

AirShareの基本的な転送処理は、ユーザー自身のローカルネットワーク内で完結します。メディアをAirShareや外部のクラウドサービスへアップロードする必要はありません。プラットフォーム固有のプライバシーおよび公開資料は[`AirShare-Android/docs/`](AirShare-Android/docs/)を参照してください。
