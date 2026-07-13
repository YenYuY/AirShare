//
//  PhotoLibraryService.swift
//  AirShare
//
//  A small service to access photo library assets, list them, and export thumbnails/data.
//

import Foundation
import Photos
import UIKit

struct AssetInfo: Identifiable {
    let id: String            // PHAsset.localIdentifier
    let filename: String
    let uti: String?
    let mediaType: PHAssetMediaType
    let pixelWidth: Int
    let pixelHeight: Int
    let duration: TimeInterval

    var usesCompatibleJPEGDownload: Bool {
        guard mediaType == .image else { return false }
        let fileExtension = (filename as NSString).pathExtension.lowercased()
        let normalizedUTI = uti?.lowercased() ?? ""
        return fileExtension == "heic" ||
            fileExtension == "heif" ||
            normalizedUTI.contains("heic") ||
            normalizedUTI.contains("heif")
    }

    var downloadFilename: String {
        guard usesCompatibleJPEGDownload else { return filename }
        let base = (filename as NSString).deletingPathExtension
        return "\(base.isEmpty ? "photo" : base).jpg"
    }
}

final class PhotoLibraryService {
    static let shared = PhotoLibraryService()
    private init() {}

    // Fetch recent assets (photos and videos)
    func fetchAllAssets() -> [PHAsset] {
        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        let results = PHAsset.fetchAssets(with: options)
        var assets: [PHAsset] = []
        results.enumerateObjects { asset, _, _ in
            assets.append(asset)
        }
        return assets
    }

    func assetInfo(for asset: PHAsset) -> AssetInfo {
        let resources = PHAssetResource.assetResources(for: asset)
        let primary = resources.first
        let filename = primary?.originalFilename ?? "asset"
        let uti = primary?.uniformTypeIdentifier
        return AssetInfo(
            id: asset.localIdentifier,
            filename: filename,
            uti: uti,
            mediaType: asset.mediaType,
            pixelWidth: asset.pixelWidth,
            pixelHeight: asset.pixelHeight,
            duration: asset.duration
        )
    }

    // Thumbnail JPEG data for an asset
    func requestThumbnailData(for asset: PHAsset, targetSize: CGSize = CGSize(width: 240, height: 240)) async throws -> Data {
        return try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Data, Error>) in
            let manager = PHImageManager.default()
            let options = PHImageRequestOptions()
            options.deliveryMode = .highQualityFormat // reduce multiple callbacks
            options.resizeMode = .fast
            options.isSynchronous = false
            options.isNetworkAccessAllowed = true

            var didResume = false
            func safeResume(_ result: Result<Data, Error>) {
                guard !didResume else { return }
                didResume = true
                switch result {
                case .success(let data):
                    continuation.resume(returning: data)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }

            manager.requestImage(for: asset, targetSize: targetSize, contentMode: .aspectFill, options: options) { image, info in
                if didResume { return }

                if let error = info?[PHImageErrorKey] as? NSError {
                    safeResume(.failure(error))
                    return
                }

                if let degraded = info?[PHImageResultIsDegradedKey] as? NSNumber, degraded.boolValue == true {
                    // Ignore low-quality (degraded) callbacks; wait for the final high-quality image
                    return
                }

                guard let image = image else {
                    safeResume(.failure(NSError(domain: "PhotoLibraryService", code: -1, userInfo: [NSLocalizedDescriptionKey: "No thumbnail image"])))
                    return
                }
                guard let data = image.jpegData(compressionQuality: 0.8) else {
                    safeResume(.failure(NSError(domain: "PhotoLibraryService", code: -2, userInfo: [NSLocalizedDescriptionKey: "JPEG encoding failed"])))
                    return
                }
                safeResume(.success(data))
            }
        }
    }

    // Export original resource as Data (for simplicity; for large videos consider streaming)
    func requestOriginalData(for asset: PHAsset) async throws -> (data: Data, filename: String, uti: String?) {
        let resources = PHAssetResource.assetResources(for: asset)
        guard let resource = resources.first else {
            throw NSError(domain: "PhotoLibraryService", code: -3, userInfo: [NSLocalizedDescriptionKey: "No resource for asset"])
        }
        let filename = resource.originalFilename
        let uti = resource.uniformTypeIdentifier

        let buffer = NSMutableData()
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            let options = PHAssetResourceRequestOptions()
            options.isNetworkAccessAllowed = true
            PHAssetResourceManager.default().requestData(for: resource, options: options) { chunk in
                buffer.append(chunk)
            } completionHandler: { error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
        return (data: buffer as Data, filename: filename, uti: uti)
    }

    /// Converts a full-resolution photo to broadly supported JPEG while retaining
    /// the source image metadata where ImageIO can represent it in JPEG.
    func requestCompatibleJPEGData(for asset: PHAsset, compressionQuality: CGFloat = 0.92) async throws -> Data {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Data, Error>) in
            let options = PHImageRequestOptions()
            options.version = .current
            options.deliveryMode = .highQualityFormat
            options.isNetworkAccessAllowed = true
            options.isSynchronous = false

            PHImageManager.default().requestImageDataAndOrientation(for: asset, options: options) { data, _, _, info in
                if let error = info?[PHImageErrorKey] as? Error {
                    continuation.resume(throwing: error)
                    return
                }
                if let cancelled = info?[PHImageCancelledKey] as? NSNumber, cancelled.boolValue {
                    continuation.resume(throwing: NSError(
                        domain: "PhotoLibraryService",
                        code: -4,
                        userInfo: [NSLocalizedDescriptionKey: "JPEG conversion was cancelled"]
                    ))
                    return
                }
                guard let data else {
                    continuation.resume(throwing: NSError(
                        domain: "PhotoLibraryService",
                        code: -5,
                        userInfo: [NSLocalizedDescriptionKey: "Source photo data is unavailable"]
                    ))
                    return
                }

                do {
                    let jpeg = try CompatibleJPEGEncoder.encode(
                        data,
                        compressionQuality: Double(compressionQuality)
                    )
                    continuation.resume(returning: jpeg)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    // Utility to find asset by localIdentifier
    func findAsset(by localIdentifier: String) -> PHAsset? {
        let results = PHAsset.fetchAssets(withLocalIdentifiers: [localIdentifier], options: nil)
        return results.firstObject
    }
}
