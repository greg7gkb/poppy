Image with explicit dimensions and `cover` fit. The image must scale to fill its bounds while preserving aspect ratio (cropping as needed).

## Behavioral invariants

Each renderer's tests must verify:

- The image renders at the declared width and height per case 007.
- The `fit: "cover"` token resolves to the platform's equivalent: `object-fit: cover` on web, `ContentScale.Crop` on Compose, `.aspectRatio(contentMode: .fill).clipped()` (or the SwiftUI equivalent) on iOS.
