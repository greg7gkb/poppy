// Render an Image component using SwiftUI's AsyncImage.
//
// Per the plan, no third-party image library — AsyncImage is sufficient for
// v0.1. The host's isUrlAllowed callback controls whether the URL is loaded;
// rejected URLs render a blank placeholder and report the rejection through
// host.onError(_:).
//
// Behavioral invariants tested in BehaviorTests.swift:
//
//   - alt is exposed as the SwiftUI accessibilityLabel
//   - declared width/height are honored as logical pt; absent dimensions
//     leave the image at its container/intrinsic size
//   - fit token resolves to ContentMode.fill (cover) / .fit (contain) / nil
//     plus aspectRatio(contentMode:); fill = .fill, contain = .fit, fill =
//     no aspectRatio (the image is stretched freely)

import SwiftUI

struct PoppyImage: View {
    let image: Image
    let host: PoppyHost

    var body: some View {
        let isAllowed = host.isUrlAllowed(image.url, context: .image)

        Group {
            if isAllowed, let url = URL(string: image.url) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        SwiftUI.Color.clear
                    case .success(let img):
                        applyFit(img)
                    case .failure:
                        SwiftUI.Color.clear
                    @unknown default:
                        SwiftUI.Color.clear
                    }
                }
            } else {
                SwiftUI.Color.clear
                    .onAppear {
                        if !isAllowed {
                            host.onError(PoppyImageError.disallowedURL(image.url))
                        }
                    }
            }
        }
        .frame(
            width: image.width.map { CGFloat($0) },
            height: image.height.map { CGFloat($0) }
        )
        .accessibilityLabel(image.alt)
        .modifier(PoppyImageIdentifier(id: image.id))
    }

    @ViewBuilder
    private func applyFit(_ img: SwiftUI.Image) -> some View {
        switch image.fit ?? .contain {
        case .contain:
            img.resizable().aspectRatio(contentMode: .fit)
        case .cover:
            img.resizable().aspectRatio(contentMode: .fill).clipped()
        case .fill:
            // CSS `object-fit: fill` stretches to the box without preserving
            // aspect ratio — closest SwiftUI equivalent is `.resizable()`
            // without `.aspectRatio()`.
            img.resizable()
        }
    }
}

public enum PoppyImageError: Error, Equatable, Sendable {
    case disallowedURL(String)
}

private struct PoppyImageIdentifier: ViewModifier {
    let id: String?
    func body(content: Content) -> some View {
        if let id = id {
            content.accessibilityIdentifier(id)
        } else {
            content
        }
    }
}
