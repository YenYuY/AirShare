//
//  CompatibleJPEGEncoder.swift
//  AirShare
//
//  HEIC/HEIF to JPEG conversion shared by the photo service and tests.
//

import Foundation
import ImageIO
import UniformTypeIdentifiers

enum CompatibleJPEGEncoder {
    enum EncodingError: LocalizedError {
        case invalidSource
        case destinationUnavailable
        case encodingFailed

        var errorDescription: String? {
            switch self {
            case .invalidSource: return "Unable to decode the source photo."
            case .destinationUnavailable: return "Unable to create a JPEG encoder."
            case .encodingFailed: return "JPEG encoding failed."
            }
        }
    }

    static func encode(_ sourceData: Data, compressionQuality: Double = 0.92) throws -> Data {
        guard let source = CGImageSourceCreateWithData(sourceData as CFData, nil),
              CGImageSourceGetCount(source) > 0 else {
            throw EncodingError.invalidSource
        }

        let output = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(
            output,
            UTType.jpeg.identifier as CFString,
            1,
            nil
        ) else {
            throw EncodingError.destinationUnavailable
        }

        var properties = (CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any]) ?? [:]
        properties[kCGImageDestinationLossyCompressionQuality] = min(max(compressionQuality, 0), 1)
        CGImageDestinationAddImageFromSource(destination, source, 0, properties as CFDictionary)

        guard CGImageDestinationFinalize(destination) else {
            throw EncodingError.encodingFailed
        }
        return output as Data
    }
}
