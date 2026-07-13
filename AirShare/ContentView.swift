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
    @StateObject private var languageManager = AppLanguageManager.shared
    @State private var isAuthorized = false
    @State private var serverRunning = false
    @State private var serverURL: URL?
    @State private var authorizationStatus: PHAuthorizationStatus = .notDetermined
    @State private var serverErrorDetail: String?
    @State private var requestingAuth = false
    @State private var showWiFiAlert = false
    @State private var networkMonitor: NWPathMonitor?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let accent = Color(red: 0.18, green: 0.48, blue: 0.98)

    private var strings: AppStrings {
        AppStrings(language: languageManager.effectiveLanguage)
    }

    private var languageSelectionBinding: Binding<AppLanguageOption> {
        Binding(
            get: { languageManager.selectedOption },
            set: { languageManager.setLanguageOption($0) }
        )
    }

    var body: some View {
        NavigationStack {
            ZStack {
                ambientBackground

                ScrollView {
                    VStack(spacing: 32) {
                        hero
                        primaryPanel
                        connectionHint
                    }
                    .frame(maxWidth: 560)
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    .padding(.bottom, 40)
                    .frame(maxWidth: .infinity)
                }
            }
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Picker(strings.languageLabel, selection: languageSelectionBinding) {
                            ForEach(AppLanguageOption.allCases) { option in
                                Text(strings.languageOptionLabel(option)).tag(option)
                            }
                        }
                    } label: {
                        Image(systemName: "globe")
                            .font(.system(size: 17, weight: .semibold))
                            .frame(width: 34, height: 34)
                    }
                    .accessibilityLabel(strings.languageLabel)
                }
            }
            .alert(strings.wifiAlertTitle, isPresented: $showWiFiAlert) {
                Button(strings.okButton, role: .cancel) { showWiFiAlert = false }
            } message: {
                Text(strings.wifiAlertMessage)
            }
            .onAppear(perform: resetAndMonitor)
            .task { await checkAuthorization() }
            .onChange(of: scenePhase) { newPhase in
                if newPhase == .active { resetServer() }
            }
            .onDisappear {
                if serverRunning { PhotoHTTPServer.shared.stop() }
                UIApplication.shared.isIdleTimerDisabled = false
                stopNetworkMonitoring()
            }
        }
        .tint(accent)
    }

    private var ambientBackground: some View {
        ZStack {
            Color(uiColor: .systemGroupedBackground)
                .ignoresSafeArea()

            Circle()
                .fill(accent.opacity(0.16))
                .frame(width: 320, height: 320)
                .blur(radius: 70)
                .offset(x: 160, y: -270)

            Circle()
                .fill(Color.cyan.opacity(0.10))
                .frame(width: 260, height: 260)
                .blur(radius: 80)
                .offset(x: -170, y: 310)
        }
        .accessibilityHidden(true)
    }

    private var hero: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(.ultraThinMaterial)
                    .frame(width: 96, height: 96)
                    .overlay(Circle().stroke(.white.opacity(0.22), lineWidth: 0.5))

                Image(systemName: serverRunning ? "antenna.radiowaves.left.and.right" : "photo.stack.fill")
                    .font(.system(size: 38, weight: .medium))
                    .symbolRenderingMode(.hierarchical)
                    .foregroundStyle(serverRunning ? Color.green : accent)
            }

            VStack(spacing: 6) {
                Text(strings.appName)
                    .font(.system(.largeTitle, design: .rounded, weight: .bold))
                Text(isAuthorized ? strings.serverStopped : strings.photoPermissionNeeded)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private var primaryPanel: some View {
        VStack(spacing: 24) {
            statusRow

            Divider().opacity(0.55)

            if isAuthorized {
                serverControls
            } else {
                permissionControls
            }

            if let detail = serverErrorDetail {
                Label(strings.serverStartError(detail), systemImage: "exclamationmark.triangle.fill")
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .transition(.opacity)
            }
        }
        .padding(24)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(.white.opacity(0.18), lineWidth: 0.5)
        }
    }

    private var statusRow: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill((serverRunning ? Color.green : Color.secondary).opacity(0.14))
                    .frame(width: 40, height: 40)
                Image(systemName: serverRunning ? "checkmark" : "power")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(serverRunning ? Color.green : Color.secondary)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(serverRunning ? strings.serverStarted : strings.serverStopped)
                    .font(.headline)
                Text(serverRunning ? "HTTP · Wi-Fi" : "Wi-Fi")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()

            Capsule()
                .fill(serverRunning ? Color.green : Color.secondary.opacity(0.35))
                .frame(width: 10, height: 10)
                .accessibilityHidden(true)
        }
        .accessibilityElement(children: .combine)
    }

    private var permissionControls: some View {
        VStack(spacing: 20) {
            let message = strings.authorizationMessage(for: authorizationStatus)
            Text(message.isEmpty ? strings.photoPermissionNeeded : message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            primaryButton(
                title: requestingAuth ? strings.requestingPermissionButton : strings.requestPermissionButton,
                icon: "photo.badge.plus",
                isDestructive: false
            ) {
                requestPhotoAuthorization()
            }
            .disabled(requestingAuth)
        }
    }

    private var serverControls: some View {
        VStack(spacing: 20) {
            if let url = serverURL {
                VStack(alignment: .leading, spacing: 8) {
                    Text(url.absoluteString)
                        .font(.system(.body, design: .monospaced, weight: .semibold))
                        .textSelection(.enabled)
                        .lineLimit(2)
                        .minimumScaleFactor(0.8)
                    Text(strings.keepScreenOnHint)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(Color.primary.opacity(0.055), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            primaryButton(
                title: serverRunning ? strings.stopServerButton : strings.startServerButton,
                icon: serverRunning ? "stop.fill" : "play.fill",
                isDestructive: serverRunning
            ) {
                withAnimation(reduceMotion ? nil : .easeOut(duration: 0.2)) {
                    toggleServer()
                }
            }
        }
    }

    private var connectionHint: some View {
        Label(strings.keepScreenOnHint, systemImage: "wifi")
            .font(.footnote)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, 8)
            .accessibilityElement(children: .combine)
    }

    private func primaryButton(
        title: String,
        icon: String,
        isDestructive: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: icon)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .frame(minHeight: 52)
                .contentShape(Rectangle())
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.capsule)
        .tint(isDestructive ? .red : accent)
    }

    // MARK: - Authorization

    func checkAuthorization() async {
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        await MainActor.run {
            authorizationStatus = status
            isAuthorized = (status == .authorized || status == .limited)
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
            resetServer()
        } else {
            do {
                let url = try PhotoHTTPServer.shared.start(port: 8080)
                serverRunning = true
                serverURL = url
                serverErrorDetail = nil
                UIApplication.shared.isIdleTimerDisabled = true
            } catch {
                serverErrorDetail = error.localizedDescription
            }
        }
    }

    private func resetAndMonitor() {
        resetServer()
        languageManager.refreshForCurrentSystemLanguageIfAuto()
        startNetworkMonitoring()
    }

    private func resetServer() {
        PhotoHTTPServer.shared.stop()
        serverRunning = false
        serverURL = nil
        serverErrorDetail = nil
        UIApplication.shared.isIdleTimerDisabled = false
        languageManager.refreshForCurrentSystemLanguageIfAuto()
    }

    func startNetworkMonitoring() {
        let monitor = NWPathMonitor()
        networkMonitor = monitor
        monitor.pathUpdateHandler = { path in
            let onWiFi = path.status == .satisfied && path.usesInterfaceType(.wifi)
            if !onWiFi {
                Task { @MainActor in showWiFiAlert = true }
            }
        }
        monitor.start(queue: DispatchQueue(label: "NetworkMonitor"))
    }

    func stopNetworkMonitoring() {
        networkMonitor?.cancel()
        networkMonitor = nil
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
