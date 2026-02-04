//
//  PhotoHTTPServer.swift
//  AirShare
//
//  Minimal HTTP server to list and serve photo/video assets over LAN.
//

import Foundation
import Network
import Photos
import UIKit
import SystemConfiguration
import MobileCoreServices
import UniformTypeIdentifiers
import Darwin

final class PhotoHTTPServer {
    static let shared = PhotoHTTPServer()

    private var listener: NWListener?
    private var port: UInt16 = 8080
    private let queue = DispatchQueue(label: "PhotoHTTPServer.queue")

    private init() {}

    // Start server and return the URL (http://<ip>:<port>)
    func start(port: UInt16 = 8080) throws -> URL {
        if listener != nil { stop() }
        self.port = port

        let params = NWParameters.tcp
        let listener = try NWListener(using: params, on: NWEndpoint.Port(rawValue: port)!)
        self.listener = listener

        listener.newConnectionHandler = { [weak self] connection in
            self?.handle(connection: connection)
        }
        listener.stateUpdateHandler = { state in
            // You could log state changes if needed
            // print("Listener state: \(state)")
        }
        listener.start(queue: queue)

        guard let ip = Self.currentWiFiIPAddress() else {
            return URL(string: "http://127.0.0.1:\(port)")!
        }
        return URL(string: "http://\(ip):\(port)")!
    }

    func stop() {
        listener?.cancel()
        listener = nil
    }

    // MARK: - Connection Handling
    private func handle(connection: NWConnection) {
        connection.stateUpdateHandler = { newState in
            switch newState {
            case .ready:
                self.receive(on: connection)
            case .failed, .cancelled:
                connection.cancel()
            default:
                break
            }
        }
        connection.start(queue: queue)
    }

    private func receive(on connection: NWConnection) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 64 * 1024) { [weak self] data, _, isComplete, error in
            guard let self = self else { return }
            if let data = data, !data.isEmpty {
                let request = String(decoding: data, as: UTF8.self)
                self.handleRequest(raw: request, on: connection)
            } else if isComplete || error != nil {
                connection.cancel()
            } else {
                self.receive(on: connection)
            }
        }
    }

    private func send(_ data: Data, on connection: NWConnection, completion: @escaping () -> Void) {
        connection.send(content: data, completion: .contentProcessed { _ in
            completion()
        })
    }

    // MARK: - Request Routing
    private func handleRequest(raw: String, on connection: NWConnection) {
        guard let firstLine = raw.components(separatedBy: "\r\n").first else {
            let data = httpResponse(status: 400, headers: [:], body: Data("Bad Request".utf8))
            send(data, on: connection) { connection.cancel() }
            return
        }
        let parts = firstLine.split(separator: " ")
        guard parts.count >= 2 else {
            let data = httpResponse(status: 400, headers: [:], body: Data("Bad Request".utf8))
            send(data, on: connection) { connection.cancel() }
            return
        }
        let method = String(parts[0])
        let fullPath = String(parts[1])
        let components = fullPath.split(separator: "?", maxSplits: 1).map(String.init)
        let path = components.first ?? "/"
        let query = components.count > 1 ? components[1] : ""

        // Extract headers (for Range support)
        let headerLines = raw.components(separatedBy: "\r\n").dropFirst()
        var headers: [String: String] = [:]
        for line in headerLines {
            if line.isEmpty { break }
            if let idx = line.firstIndex(of: ":") {
                let key = String(line[..<idx]).trimmingCharacters(in: .whitespaces)
                let value = String(line[line.index(after: idx)...]).trimmingCharacters(in: .whitespaces)
                headers[key.lowercased()] = value
            }
        }

        guard method == "GET" else {
            let data = httpResponse(status: 405, headers: ["Allow": "GET"], body: Data("Method Not Allowed".utf8))
            send(data, on: connection) { connection.cancel() }
            return
        }

        // Authorization guard
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        let authorized = (status == .authorized || status == .limited)
        if !authorized {
            let html = """
            <!doctype html><html><head><meta charset=\"utf-8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/><title>AirShare - Authorization Required</title><style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;padding:24px}</style></head><body><h2>需要相簿授權</h2><p>請在 iPhone 上開啟 AirShare 並允許「照片」權限後再重新整理此頁面。</p></body></html>
            """
            let body = Data(html.utf8)
            let data = httpResponse(status: 403, headers: [
                "Content-Type": "text/html; charset=utf-8",
                "Content-Length": String(body.count)
            ], body: body)
            send(data, on: connection) { connection.cancel() }
            return
        }

        if path == "/" {
            let pageParam = parseQuery(query)["page"].flatMap { Int($0) } ?? 1
            let data = serveIndex(page: max(1, pageParam))
            send(data, on: connection) { connection.cancel() }
            return
        } else if path.hasPrefix("/thumbnail/") {
            let id = String(path.dropFirst("/thumbnail/".count))
            let data = serveThumbnail(id: id)
            send(data, on: connection) { connection.cancel() }
            return
        } else if path.hasPrefix("/asset/") {
            let id = String(path.dropFirst("/asset/".count))
            streamAsset(id: id, requestHeaders: headers, on: connection)
            return
        } else {
            let data = httpResponse(status: 404, headers: [:], body: Data("Not Found".utf8))
            send(data, on: connection) { connection.cancel() }
            return
        }
    }

    // MARK: - Handlers
    private func serveIndex(page: Int, pageSize: Int = 20) -> Data {
        let allAssets = PhotoLibraryService.shared.fetchAllAssets()
        let total = allAssets.count
        let pageCount = max(1, Int(ceil(Double(total) / Double(pageSize))))
        let current = min(max(1, page), pageCount)
        let start = (current - 1) * pageSize
        let end = min(start + pageSize, total)
        let pageSlice = (start < end) ? Array(allAssets[start..<end]) : []
        let infos = pageSlice.map { PhotoLibraryService.shared.assetInfo(for: $0) }
        let html = HTMLBuilder.indexHTML(for: infos, page: current, pageCount: pageCount)
        let body = Data(html.utf8)
        return httpResponse(status: 200, headers: [
            "Content-Type": "text/html; charset=utf-8",
            "Content-Length": String(body.count)
        ], body: body)
    }

    private func serveThumbnail(id: String) -> Data {
        guard let asset = PhotoLibraryService.shared.findAsset(by: id) else {
            return httpResponse(status: 404, headers: [:], body: Data("Asset Not Found".utf8))
        }
        do {
            let data = try awaitThumbnail(asset: asset)
            return httpResponse(status: 200, headers: [
                "Content-Type": "image/jpeg",
                "Content-Length": String(data.count)
            ], body: data)
        } catch {
            return httpResponse(status: 500, headers: [:], body: Data("Thumbnail Error".utf8))
        }
    }

    private func streamAsset(id: String, requestHeaders: [String: String], on connection: NWConnection) {
        guard let asset = PhotoLibraryService.shared.findAsset(by: id) else {
            let data = httpResponse(status: 404, headers: [:], body: Data("Asset Not Found".utf8))
            send(data, on: connection) { connection.cancel() }
            return
        }

        // Prepare metadata
        let res = PHAssetResource.assetResources(for: asset).first
        let filename = res?.originalFilename ?? "file"
        let uti = res?.uniformTypeIdentifier
        let mime = mimeType(fromUTI: uti) ?? "application/octet-stream"

        // Range parsing (single range only)
        let rangeHeader = requestHeaders["range"] // e.g., bytes=0- or bytes=1000-2000
        var startByte: Int64? = nil
        var endByte: Int64? = nil
        if let rangeHeader, rangeHeader.lowercased().hasPrefix("bytes=") {
            let spec = rangeHeader.dropFirst("bytes=".count)
            let parts = spec.split(separator: ",").first.map(String.init) // single range
            if let parts = parts {
                let se = parts.split(separator: "-", maxSplits: 1).map(String.init)
                if se.count == 2 {
                    if let s = Int64(se[0]) { startByte = s }
                    if let e = Int64(se[1]) { endByte = e }
                }
            }
        }

        // We don't know total length up-front without exporting; stream with chunked transfer if no range, else send 206 without Content-Length.
        // Build headers
        var header = "HTTP/1.1 200 OK\r\n"
        if startByte != nil || endByte != nil { header = "HTTP/1.1 206 Partial Content\r\n" }
        header += "Content-Type: \(mime)\r\n"
        // RFC 5987 filename*
        let encodedName = filename.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? filename
        header += "Content-Disposition: attachment; filename=\"\(filename)\"; filename*=UTF-8''\(encodedName)\r\n"
        header += "Accept-Ranges: bytes\r\n"
        header += "Transfer-Encoding: chunked\r\n"
        header += "Connection: close\r\n\r\n"

        // Send headers first
        connection.send(content: Data(header.utf8), completion: .contentProcessed { _ in })

        // Stream data as chunks using PHAssetResourceManager
        let options = PHAssetResourceRequestOptions()
        options.isNetworkAccessAllowed = true

        // For simplicity, we ignore startByte/endByte and stream full content; browsers can still download progressively.
        PHAssetResourceManager.default().requestData(for: res!, options: options) { chunk in
            // write each chunk as hex-length, data, CRLF (chunked transfer)
            let sizeLine = String(format: "%X\r\n", chunk.count)
            var data = Data(sizeLine.utf8)
            data.append(chunk)
            data.append(contentsOf: [13,10]) // CRLF
            self.send(data, on: connection) { }
        } completionHandler: { error in
            // final zero-length chunk
            let endChunk = Data("0\r\n\r\n".utf8)
            self.send(endChunk, on: connection) {
                connection.cancel()
            }
        }
    }

    // MARK: - Helpers
    private func httpResponse(status: Int, headers: [String: String], body: Data) -> Data {
        var response = "HTTP/1.1 \(status) \(statusText(status))\r\n"
        for (k, v) in headers { response += "\(k): \(v)\r\n" }
        response += "Connection: close\r\n\r\n"
        var data = Data(response.utf8)
        data.append(body)
        return data
    }

    private func statusText(_ code: Int) -> String {
        switch code {
        case 200: return "OK"
        case 400: return "Bad Request"
        case 404: return "Not Found"
        case 405: return "Method Not Allowed"
        case 500: return "Internal Server Error"
        default: return "Status"
        }
    }

    private func awaitThumbnail(asset: PHAsset) throws -> Data {
        let semaphore = DispatchSemaphore(value: 0)
        var result: Result<Data, Error> = .failure(NSError(domain: "PhotoHTTPServer", code: -1))
        Task {
            do {
                let data = try await PhotoLibraryService.shared.requestThumbnailData(for: asset)
                result = .success(data)
            } catch {
                result = .failure(error)
            }
            semaphore.signal()
        }
        semaphore.wait()
        return try result.get()
    }

    private func awaitOriginal(asset: PHAsset) throws -> (data: Data, filename: String, uti: String?) {
        let semaphore = DispatchSemaphore(value: 0)
        var result: Result<(Data, String, String?), Error> = .failure(NSError(domain: "PhotoHTTPServer", code: -1))
        Task {
            do {
                let r = try await PhotoLibraryService.shared.requestOriginalData(for: asset)
                result = .success((r.data, r.filename, r.uti))
            } catch {
                result = .failure(error)
            }
            semaphore.signal()
        }
        semaphore.wait()
        let tuple = try result.get()
        return (tuple.0, tuple.1, tuple.2)
    }

    private static func currentWiFiIPAddress() -> String? {
        // Simple getifaddrs-based resolver
        var address: String?
        var ifaddr: UnsafeMutablePointer<ifaddrs>? = nil
        if getifaddrs(&ifaddr) == 0 {
            var ptr = ifaddr
            while ptr != nil {
                let interface = ptr!.pointee
                let addrFamily = interface.ifa_addr.pointee.sa_family
                if addrFamily == UInt8(AF_INET) { // IPv4 only for simplicity
                    let name = String(cString: interface.ifa_name)
                    if name == "en0" { // Wi-Fi
                        var addr = interface.ifa_addr.pointee
                        var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                        getnameinfo(&addr, socklen_t(interface.ifa_addr.pointee.sa_len), &hostname, socklen_t(hostname.count), nil, socklen_t(0), NI_NUMERICHOST)
                        address = String(cString: hostname)
                        break
                    }
                }
                ptr = interface.ifa_next
            }
            freeifaddrs(ifaddr)
        }
        return address
    }

    private func mimeType(fromUTI uti: String?) -> String? {
        guard let uti = uti else { return nil }
        if #available(iOS 14.0, *) {
            if let utType = UTType(uti) {
                return utType.preferredMIMEType
            }
        }
        // Fallbacks
        if uti.contains("jpeg") || uti.contains("jpg") { return "image/jpeg" }
        if uti.contains("png") { return "image/png" }
        if uti.contains("heic") { return "image/heic" }
        if uti.contains("quicktime-movie") || uti.contains("mov") { return "video/quicktime" }
        if uti.contains("mp4") { return "video/mp4" }
        return nil
    }

    private func parseQuery(_ query: String) -> [String: String] {
        var dict: [String: String] = [:]
        for pair in query.split(separator: "&") {
            let parts = pair.split(separator: "=", maxSplits: 1).map(String.init)
            guard parts.count == 2 else { continue }
            let key = parts[0].removingPercentEncoding ?? parts[0]
            let value = parts[1].removingPercentEncoding ?? parts[1]
            dict[key] = value
        }
        return dict
    }
}

