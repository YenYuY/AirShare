//
//  HTMLBuilder.swift
//  AirShare
//
//  Browser gallery for downloading local photo-library assets.
//

import Foundation
import Photos

struct HTMLBuilder {
    static func indexHTML(for assets: [AssetInfo], page: Int, pageCount: Int, language: AppLanguage) -> String {
        let strings = AppStrings(language: language)
        let items = assets.map { asset -> String in
            let id = escape(asset.id)
            let name = escape(asset.downloadFilename)
            let thumb = "/thumbnail/\(id)"
            let file = "/asset/\(id)"
            let typeBadge = asset.mediaType == .video
                ? "<span class=\"media-badge\">\(videoIcon)\(escape(strings.webVideoBadge))</span>"
                : ""
            let duration = asset.mediaType == .video && asset.duration > 0
                ? "<span class=\"duration\">\(formattedDuration(asset.duration))</span>"
                : ""
            return """
            <a class="asset" href="\(file)" download aria-label="\(escape(strings.webDownloadLabel)): \(name)">
              <span class="thumbnail">
                <img src="\(thumb)" alt="\(name)" loading="lazy" decoding="async" />
                <span class="asset-overlay" aria-hidden="true">
                  <span class="download-action">\(downloadIcon)\(escape(strings.webDownloadLabel))</span>
                </span>
                \(typeBadge)
                \(duration)
              </span>
              <span class="asset-name" title="\(name)">\(name)</span>
            </a>
            """
        }.joined(separator: "\n")

        let gallery: String
        if assets.isEmpty {
            gallery = """
            <section class="empty-state" aria-labelledby="empty-title">
              <span class="empty-icon" aria-hidden="true">\(photosIcon)</span>
              <h2 id="empty-title">\(escape(strings.webEmptyTitle))</h2>
              <p>\(escape(strings.webEmptyMessage))</p>
            </section>
            """
        } else {
            gallery = "<main class=\"gallery\" aria-label=\"\(escape(strings.appName))\">\(items)</main>"
        }

        return """
        <!doctype html>
        <html lang="\(language.htmlLangCode)">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
          <meta name="theme-color" content="#f4f7fb" media="(prefers-color-scheme: light)" />
          <meta name="theme-color" content="#0d1118" media="(prefers-color-scheme: dark)" />
          <title>\(escape(strings.appName))</title>
          <style>\(sharedStyles)</style>
        </head>
        <body>
          <header class="topbar">
            <div class="topbar-inner">
              <a class="brand" href="/" aria-label="\(escape(strings.appName))">
                <span class="brand-mark" aria-hidden="true">\(photosIcon)</span>
                <span>\(escape(strings.appName))</span>
              </a>
              <div class="connection">
                <span class="status-dot" aria-hidden="true"></span>
                <span>\(escape(strings.webConnectedLabel))</span>
              </div>
            </div>
          </header>

          <div class="page-shell">
            <section class="intro" aria-labelledby="gallery-title">
              <div>
                <h1 id="gallery-title">\(escape(strings.appName))</h1>
                <p>\(escape(strings.webFooter))</p>
              </div>
              <span class="item-count">\(escape(strings.webItemCount(assets.count)))</span>
            </section>

            \(gallery)
            \(pagerHTML(page: page, pageCount: pageCount, strings: strings))

            <footer class="footer">
              <span class="footer-icon" aria-hidden="true">\(linkIcon)</span>
              <span>\(escape(strings.webConnectedLabel))</span>
            </footer>
          </div>
        </body>
        </html>
        """
    }

    static func authorizationRequiredHTML(language: AppLanguage) -> String {
        let strings = AppStrings(language: language)
        return """
        <!doctype html>
        <html lang="\(language.htmlLangCode)">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
          <meta name="theme-color" content="#f4f7fb" media="(prefers-color-scheme: light)" />
          <meta name="theme-color" content="#0d1118" media="(prefers-color-scheme: dark)" />
          <title>\(escape(strings.webAuthorizationTitle))</title>
          <style>\(sharedStyles)</style>
        </head>
        <body class="centered-page">
          <main class="message-panel" aria-labelledby="message-title">
            <span class="message-icon" aria-hidden="true">\(photosIcon)</span>
            <p class="message-brand">\(escape(strings.appName))</p>
            <h1 id="message-title">\(escape(strings.webAuthorizationHeading))</h1>
            <p>\(escape(strings.webAuthorizationMessage))</p>
            <a class="refresh-button" href="/">
              \(refreshIcon)\(escape(refreshLabel(language)))
            </a>
          </main>
        </body>
        </html>
        """
    }

    private static func pagerHTML(page: Int, pageCount: Int, strings: AppStrings) -> String {
        guard pageCount > 1 else { return "" }
        let prevPage = max(1, page - 1)
        let nextPage = min(pageCount, page + 1)
        let prevLabel = escape(strings.webPrevPage)
        let nextLabel = escape(strings.webNextPage)
        let previous = page <= 1
            ? "<span class=\"page-button disabled\" aria-disabled=\"true\">\(chevronLeft)\(prevLabel)</span>"
            : "<a class=\"page-button\" href=\"/?page=\(prevPage)\">\(chevronLeft)\(prevLabel)</a>"
        let next = page >= pageCount
            ? "<span class=\"page-button disabled\" aria-disabled=\"true\">\(nextLabel)\(chevronRight)</span>"
            : "<a class=\"page-button\" href=\"/?page=\(nextPage)\">\(nextLabel)\(chevronRight)</a>"
        return """
        <nav class="pager" aria-label="\(escape(strings.webPageInfo(page: page, pageCount: pageCount)))">
          \(previous)
          <span class="page-info">\(escape(strings.webPageInfo(page: page, pageCount: pageCount)))</span>
          \(next)
        </nav>
        """
    }

    private static let sharedStyles = """
    :root {
      color-scheme: light dark;
      --bg: #f4f7fb;
      --surface: rgba(255, 255, 255, .82);
      --surface-solid: #ffffff;
      --ink: #172033;
      --muted: #5d687a;
      --line: rgba(25, 38, 61, .10);
      --accent: #2e7bf6;
      --accent-ink: #ffffff;
      --success: #20a66a;
      --thumb: #e6ebf2;
      --focus: rgba(46, 123, 246, .35);
      --shadow: 0 6px 18px rgba(45, 66, 99, .10);
      font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Segoe UI", sans-serif;
      font-synthesis: none;
    }

    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #0d1118;
        --surface: rgba(24, 30, 41, .82);
        --surface-solid: #171d27;
        --ink: #f3f6fc;
        --muted: #a9b3c2;
        --line: rgba(255, 255, 255, .11);
        --accent: #5c9cff;
        --accent-ink: #07101f;
        --success: #48d597;
        --thumb: #1c2430;
        --focus: rgba(92, 156, 255, .42);
        --shadow: 0 6px 18px rgba(0, 0, 0, .22);
      }
    }

    * { box-sizing: border-box; }
    html { background: var(--bg); }
    body {
      margin: 0;
      min-height: 100vh;
      background:
        radial-gradient(circle at 88% -8%, rgba(46, 123, 246, .11), transparent 30rem),
        var(--bg);
      color: var(--ink);
      -webkit-font-smoothing: antialiased;
    }
    a { color: inherit; }
    svg { display: block; width: 1em; height: 1em; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }

    .topbar {
      position: sticky;
      top: 0;
      z-index: 10;
      padding-top: env(safe-area-inset-top);
      border-bottom: 1px solid var(--line);
      background: var(--surface);
      -webkit-backdrop-filter: saturate(1.35) blur(20px);
      backdrop-filter: saturate(1.35) blur(20px);
    }
    .topbar-inner, .page-shell { width: min(1180px, calc(100% - 40px)); margin-inline: auto; }
    .topbar-inner { min-height: 68px; display: flex; align-items: center; justify-content: space-between; gap: 20px; }
    .brand { display: inline-flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 720; text-decoration: none; letter-spacing: -.015em; }
    .brand-mark { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 10px; background: var(--accent); color: var(--accent-ink); font-size: 19px; }
    .connection { display: inline-flex; align-items: center; gap: 8px; color: var(--muted); font-size: 13px; font-weight: 600; }
    .status-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 4px color-mix(in srgb, var(--success) 14%, transparent); }

    .page-shell { padding-block: 46px max(32px, env(safe-area-inset-bottom)); }
    .intro { display: flex; justify-content: space-between; align-items: end; gap: 28px; margin-bottom: 24px; }
    .intro h1 { margin: 0; font-size: clamp(30px, 5vw, 44px); line-height: 1.04; letter-spacing: -.035em; }
    .intro p { max-width: 58ch; margin: 10px 0 0; color: var(--muted); font-size: 15px; line-height: 1.55; }
    .item-count { flex: 0 0 auto; color: var(--muted); font-size: 13px; font-weight: 650; }

    .gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(172px, 1fr)); gap: 18px 14px; }
    .asset { min-width: 0; text-decoration: none; border-radius: 14px; outline: none; }
    .thumbnail { position: relative; display: block; overflow: hidden; aspect-ratio: 1 / 1; border-radius: 14px; background: var(--thumb); box-shadow: var(--shadow); }
    .thumbnail img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform 180ms cubic-bezier(.22, 1, .36, 1); }
    .asset-overlay { position: absolute; inset: 0; display: flex; align-items: end; justify-content: center; padding: 12px; opacity: 0; background: linear-gradient(transparent 42%, rgba(5, 9, 16, .62)); transition: opacity 160ms ease-out; }
    .download-action { display: inline-flex; align-items: center; gap: 7px; padding: 8px 11px; border-radius: 999px; background: rgba(255, 255, 255, .93); color: #142039; font-size: 12px; font-weight: 700; }
    .media-badge, .duration { position: absolute; top: 9px; display: inline-flex; align-items: center; gap: 5px; padding: 5px 8px; border-radius: 999px; background: rgba(8, 13, 22, .68); color: white; font-size: 10px; font-weight: 720; -webkit-backdrop-filter: blur(8px); backdrop-filter: blur(8px); }
    .media-badge { left: 9px; }
    .duration { right: 9px; font-variant-numeric: tabular-nums; }
    .asset-name { display: block; padding: 9px 3px 0; overflow: hidden; color: var(--ink); font-size: 12px; font-weight: 560; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }
    .asset:hover .thumbnail img { transform: scale(1.025); }
    .asset:hover .asset-overlay, .asset:focus-visible .asset-overlay { opacity: 1; }
    .asset:focus-visible { box-shadow: 0 0 0 4px var(--focus); }

    .pager { margin-top: 40px; display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 14px; }
    .page-button { min-height: 44px; display: inline-flex; align-items: center; justify-content: center; gap: 7px; width: fit-content; padding: 10px 15px; border-radius: 999px; background: var(--surface-solid); color: var(--ink); box-shadow: 0 2px 8px rgba(40, 58, 87, .10); text-decoration: none; font-size: 13px; font-weight: 680; }
    .page-button:last-child { justify-self: end; }
    .page-button.disabled { color: var(--muted); opacity: .45; box-shadow: none; }
    .page-button:focus-visible, .refresh-button:focus-visible { outline: none; box-shadow: 0 0 0 4px var(--focus); }
    .page-info { color: var(--muted); font-size: 12px; font-weight: 620; font-variant-numeric: tabular-nums; }

    .footer { margin-top: 48px; padding-top: 20px; border-top: 1px solid var(--line); display: flex; align-items: center; justify-content: center; gap: 7px; color: var(--muted); font-size: 12px; }
    .footer-icon { font-size: 14px; }
    .empty-state { min-height: 48vh; display: grid; place-items: center; align-content: center; text-align: center; }
    .empty-icon, .message-icon { display: grid; place-items: center; width: 72px; height: 72px; border-radius: 16px; background: color-mix(in srgb, var(--accent) 12%, var(--surface-solid)); color: var(--accent); font-size: 34px; }
    .empty-state h2 { margin: 18px 0 7px; font-size: 21px; }
    .empty-state p { max-width: 44ch; margin: 0; color: var(--muted); line-height: 1.55; }

    .centered-page { display: grid; place-items: center; padding: 28px; }
    .message-panel { width: min(480px, 100%); padding: 34px; border-radius: 16px; background: var(--surface-solid); box-shadow: var(--shadow); }
    .message-brand { margin: 20px 0 4px; color: var(--accent); font-size: 13px; font-weight: 720; }
    .message-panel h1 { margin: 0; font-size: 28px; letter-spacing: -.025em; }
    .message-panel > p:last-of-type { margin: 13px 0 24px; color: var(--muted); line-height: 1.6; }
    .refresh-button { min-height: 48px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; width: 100%; border: 0; border-radius: 999px; background: var(--accent); color: var(--accent-ink); font: inherit; font-size: 14px; font-weight: 720; text-decoration: none; cursor: pointer; }

    @media (max-width: 620px) {
      .topbar-inner, .page-shell { width: min(100% - 28px, 1180px); }
      .topbar-inner { min-height: 60px; }
      .connection span:last-child { display: none; }
      .page-shell { padding-top: 30px; }
      .intro { align-items: start; flex-direction: column; gap: 12px; }
      .gallery { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 10px; }
      .asset-overlay { display: none; }
      .pager { grid-template-columns: 1fr 1fr; }
      .page-info { grid-row: 2; grid-column: 1 / -1; justify-self: center; }
      .message-panel { padding: 26px 22px; }
    }
    @media (hover: none) { .asset-overlay { display: none; } }
    @media (prefers-reduced-motion: reduce) { *, *::before, *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; } }
    """

    private static func formattedDuration(_ duration: TimeInterval) -> String {
        let total = max(0, Int(duration.rounded()))
        return String(format: "%d:%02d", total / 60, total % 60)
    }

    private static func refreshLabel(_ language: AppLanguage) -> String {
        switch language {
        case .zhHant: return "重新整理"
        case .english: return "Refresh page"
        case .japanese: return "再読み込み"
        }
    }

    private static func escape(_ value: String) -> String {
        value
            .replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "\"", with: "&quot;")
            .replacingOccurrences(of: "'", with: "&#39;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
    }

    private static let photosIcon = #"<svg viewBox="0 0 24 24"><rect x="3" y="6" width="15" height="14" rx="2"/><path d="m6 16 3.4-3.6a1 1 0 0 1 1.5 0L13 15l1.4-1.4a1 1 0 0 1 1.4 0L18 16"/><circle cx="13.5" cy="10.5" r="1.2"/><path d="M7 6V4h14v13h-3"/></svg>"#
    private static let downloadIcon = #"<svg viewBox="0 0 24 24"><path d="M12 3v11m0 0 4-4m-4 4-4-4"/><path d="M5 18v2h14v-2"/></svg>"#
    private static let videoIcon = #"<svg viewBox="0 0 24 24"><path d="m9 7 8 5-8 5Z"/></svg>"#
    private static let linkIcon = #"<svg viewBox="0 0 24 24"><path d="M10 13a5 5 0 0 0 7.1.1l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1"/><path d="M14 11a5 5 0 0 0-7.1-.1l-2 2A5 5 0 0 0 12 20l1.1-1.1"/></svg>"#
    private static let refreshIcon = #"<svg viewBox="0 0 24 24"><path d="M20 11a8 8 0 1 0-2.3 5.7"/><path d="M20 4v7h-7"/></svg>"#
    private static let chevronLeft = #"<svg viewBox="0 0 24 24"><path d="m15 18-6-6 6-6"/></svg>"#
    private static let chevronRight = #"<svg viewBox="0 0 24 24"><path d="m9 18 6-6-6-6"/></svg>"#
}
