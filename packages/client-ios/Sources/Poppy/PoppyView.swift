// PoppyView: top-level SwiftUI view that renders a typed PoppyDocument.
//
// Construction is deliberately separated from validation: the host calls
// Poppy.validate(_:) first, and constructs PoppyView only from a successful
// .ok(document). This means the renderer never has to deal with malformed
// input and the view tree is statically typed end-to-end.
//
// The host (PoppyHost) receives action callbacks and image-URL allowlist
// queries. The renderer never interprets action URIs or fetches image bytes
// itself — both are delegated to the host.

import SwiftUI

// MARK: - Host protocol

/// The host's interface to Poppy. Action dispatch, error reporting, and
/// per-image URL allowlisting all flow through this protocol.
public protocol PoppyHost {
    /// Called when a Button (or future actionable component) fires. Action is
    /// forwarded verbatim — the renderer does not parse, prefix, or transform
    /// the URI.
    func onAction(_ action: Action)

    /// Receives recoverable errors (e.g. an image URL the host disallowed).
    /// Default: no-op.
    func onError(_ error: Error)

    /// Decide whether an image URL is safe to load. Default: an allowlist
    /// matching the web client's `isUrlAllowedDefault` (https only, with
    /// data: rejected).
    func isUrlAllowed(_ url: String, context: ImageContext) -> Bool
}

public extension PoppyHost {
    func onError(_ error: Error) { /* no-op */ }
    func isUrlAllowed(_ url: String, context: ImageContext) -> Bool {
        return defaultAllowImageURL(url)
    }
}

/// Mirrors @poppy/client-web's isUrlAllowedDefault: only http(s) URLs;
/// data:, javascript:, file: rejected.
public func defaultAllowImageURL(_ url: String) -> Bool {
    let lower = url.lowercased()
    if lower.hasPrefix("http://") || lower.hasPrefix("https://") {
        return true
    }
    return false
}

// MARK: - PoppyView

public struct PoppyView: View {
    public let document: PoppyDocument
    public let host: PoppyHost

    public init(document: PoppyDocument, host: PoppyHost) {
        self.document = document
        self.host = host
    }

    public var body: some View {
        ComponentView(component: document.root, host: host)
    }
}

// MARK: - Component dispatch

/// Dispatch view: renders a Component by switching on its kind. This stays
/// internal because the public surface area is just `PoppyView`; consumers
/// always start at the document root.
struct ComponentView: View {
    let component: Component
    let host: PoppyHost

    var body: some View {
        switch component {
        case .stack(let s):
            PoppyStack(stack: s, host: host)
        case .text(let t):
            PoppyText(text: t)
        case .image(let i):
            PoppyImage(image: i, host: host)
        case .button(let b):
            PoppyButton(button: b, host: host)
        }
    }
}
