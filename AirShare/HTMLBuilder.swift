//
//  HTMLBuilder.swift
//  AirShare
//
//  Generates minimal HTML for listing assets with thumbnails and download links.
//

import Foundation
import Photos

struct HTMLBuilder {
    static func indexHTML(for assets: [AssetInfo], page: Int, pageCount: Int) -> String {
        let items = assets.map { asset -> String in
            let id = escape(asset.id)
            let name = escape(asset.filename)
            let thumb = "/thumbnail/\(id)"
            let file = "/asset/\(id)"
            let badge = (asset.mediaType == .video) ? "<span class=\"badge\">VIDEO</span>" : ""
            return """
            <a class="card" href="\(file)" download>
              <div class="thumb">
                <img src="\(thumb)" alt="\(name)"/>
                \(badge)
              </div>
              <div class="meta">\(name)</div>
            </a>
            """
        }.joined(separator: "\n")

        let pager: String = {
            let prevPage = max(1, page - 1)
            let nextPage = min(pageCount, page + 1)
            let prevDisabled = page <= 1
            let nextDisabled = page >= pageCount
            let prevHTML = prevDisabled ? "<span class=\"btn disabled\">上一頁</span>" : "<a class=\"btn\" href=\"/?page=\(prevPage)\">上一頁</a>"
            let nextHTML = nextDisabled ? "<span class=\"btn disabled\">下一頁</span>" : "<a class=\"btn\" href=\"/?page=\(nextPage)\">下一頁</a>"
            return """
            <div class=\"pager\">
              \(prevHTML)
              <span class=\"pageinfo\">第 \(page) / \(pageCount) 頁</span>
              \(nextHTML)
            </div>
            """
        }()

        return """
        <!doctype html>
        <html>
        <head>
          <meta charset=\"utf-8\" />
          <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
          <title>AirShare</title>
          <style>
            :root { color-scheme: light dark; }
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 16px; }
            h1 { font-size: 22px; margin: 0 0 12px; }
            .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
            .card { display: block; text-decoration: none; color: inherit; border: 1px solid rgba(0,0,0,0.15); border-radius: 10px; overflow: hidden; background: rgba(255,255,255,0.04); }
            .thumb { position: relative; aspect-ratio: 1 / 1; background: #eee; display: grid; place-items: center; }
            .thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
            .meta { padding: 8px; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
            .badge { position: absolute; right: 6px; bottom: 6px; background: rgba(0,0,0,0.6); color: #fff; font-size: 10px; padding: 2px 6px; border-radius: 999px; }
            footer { margin-top: 16px; font-size: 12px; opacity: 0.7; }
            .pager { margin-top: 16px; display: flex; gap: 12px; align-items: center; }
            .btn { padding: 6px 10px; border: 1px solid rgba(0,0,0,0.2); border-radius: 8px; text-decoration: none; color: inherit; }
            .btn.disabled { opacity: 0.4; border-style: dashed; }
            .pageinfo { font-size: 12px; opacity: 0.8; }
          </style>
        </head>
        <body>
          <h1>AirShare</h1>
          <div class=\"grid\">
            \(items)
          </div>
          \(pager)
          <footer>Tap a thumbnail to download the original file.</footer>
        </body>
        </html>
        """
    }

    private static func escape(_ s: String) -> String {
        var out = s
        out = out.replacingOccurrences(of: "&", with: "&amp;")
        out = out.replacingOccurrences(of: "\"", with: "&quot;")
        out = out.replacingOccurrences(of: "'", with: "&#39;")
        out = out.replacingOccurrences(of: "<", with: "&lt;")
        out = out.replacingOccurrences(of: ">", with: "&gt;")
        return out
    }
}

