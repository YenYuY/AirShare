//
//  ContentView.swift
//  AirShare
//
//  Created by Yen on 2026/2/3.
//

import SwiftUI
import Photos
import Network

struct ContentView: View {
    @State private var isAuthorized = false
    @State private var serverRunning = false
    @State private var serverURL: URL?
    @State private var authMessage: String = ""
    @State private var errorMessage: String?
    @State private var requestingAuth = false
    @State private var showWiFiAlert = false
    @State private var networkMonitor: NWPathMonitor? = nil
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("AirShare")
                    .font(.largeTitle).bold()

                Group {
                    if !isAuthorized {
                        Text(authMessage.isEmpty ? "需要相簿權限以提供照片與影片下載。" : authMessage)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                        Button(requestingAuth ? "請求中…" : "請求相簿權限") {
                            requestPhotoAuthorization()
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(requestingAuth)
                    } else {
                        Button(serverRunning ? "停止伺服器" : "啟動伺服器") {
                            toggleServer()
                        }
                        .buttonStyle(.borderedProminent)

                        if let url = serverURL {
                            VStack(spacing: 6) {
                                Text("伺服器已啟動：")
                                Text(url.absoluteString)
                                    .font(.system(.body, design: .monospaced))
                                    .textSelection(.enabled)
                            }
                        } else {
                            Text("伺服器未啟動")
                                .foregroundStyle(.secondary)
                        }

                        Text("請保持手機螢幕不要鎖定，並確保手機與電腦在同一個 Wi‑Fi 內。")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }

                if let error = errorMessage {
                    Text(error)
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                }

                Spacer()
            }
            .padding()
            .alert("請連接 Wi‑Fi", isPresented: $showWiFiAlert) {
                Button("好", role: .cancel) { showWiFiAlert = false }
            } message: {
                Text("目前未連接 Wi‑Fi，請連接 Wi‑Fi 後再啟動伺服器。")
            }
            .navigationTitle("AirShare")
            .onAppear {
                // Ensure server is stopped every time the view appears
                PhotoHTTPServer.shared.stop()
                serverRunning = false
                serverURL = nil
                UIApplication.shared.isIdleTimerDisabled = false
                startNetworkMonitoring()
            }
            .task {
                await checkAuthorization()
            }
            .onChange(of: scenePhase) { oldPhase, newPhase in
                if newPhase == .active {
                    // Ensure server is stopped when app returns to foreground
                    PhotoHTTPServer.shared.stop()
                    serverRunning = false
                    serverURL = nil
                    UIApplication.shared.isIdleTimerDisabled = false
                }
            }
            .onDisappear {
                // 保險：離開畫面時關閉伺服器
                if serverRunning { PhotoHTTPServer.shared.stop() }
                UIApplication.shared.isIdleTimerDisabled = false
                stopNetworkMonitoring()
            }
        }
    }

    // MARK: - Authorization
    func checkAuthorization() async {
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        await MainActor.run {
            isAuthorized = (status == .authorized || status == .limited)
            switch status {
            case .notDetermined:
                authMessage = "請求相簿權限以繼續。"
            case .denied, .restricted:
                authMessage = "未授權相簿存取。請前往設定 > 隱私權 > 照片 開啟權限。"
            case .limited:
                authMessage = "目前為有限相簿存取，可在設定中調整。"
            case .authorized:
                authMessage = ""
            @unknown default:
                authMessage = "未知的權限狀態。"
            }
        }
    }

    func requestPhotoAuthorization() {
        requestingAuth = true
        PHPhotoLibrary.requestAuthorization(for: .readWrite) { _ in
            Task { @MainActor in
                requestingAuth = false
                await checkAuthorization()
            }
        }
    }

    // MARK: - Server Control
    func toggleServer() {
        if serverRunning {
            PhotoHTTPServer.shared.stop()
            serverRunning = false
            serverURL = nil
            UIApplication.shared.isIdleTimerDisabled = false
        } else {
            do {
                let url = try PhotoHTTPServer.shared.start(port: 8080)
                serverRunning = true
                serverURL = url
                UIApplication.shared.isIdleTimerDisabled = true
            } catch {
                errorMessage = "無法啟動伺服器：\(error.localizedDescription)"
            }
        }
    }

    func startNetworkMonitoring() {
        let monitor = NWPathMonitor()
        networkMonitor = monitor
        monitor.pathUpdateHandler = { path in
            // Wi‑Fi is considered satisfied + usesInterfaceType(.wifi)
            let onWiFi = path.status == .satisfied && path.usesInterfaceType(.wifi)
            if !onWiFi {
                Task { @MainActor in
                    showWiFiAlert = true
                }
            }
        }
        let queue = DispatchQueue(label: "NetworkMonitor")
        monitor.start(queue: queue)
    }

    func stopNetworkMonitoring() {
        networkMonitor?.cancel()
        networkMonitor = nil
    }
}

#Preview {
    ContentView()
}

