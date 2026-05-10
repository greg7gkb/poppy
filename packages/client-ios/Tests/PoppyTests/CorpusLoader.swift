// Load the shared @poppy/conformance corpus from the monorepo into Swift test
// code. Per the Phase 2 plan, the corpus directory is the contract — we don't
// import a Swift port of `loadCases()`, we re-implement it here in ~30 LOC.
//
// Path resolution: the corpus lives at <repo>/packages/conformance/cases/.
// The test file is at <repo>/packages/client-ios/Tests/PoppyTests/CorpusLoader.swift.
// We compute the corpus root by walking up from `#file`. This works under
// `swift test` regardless of the build directory.

import Foundation

struct CorpusCase {
    let slug: String
    let folder: URL
    let documentJSON: Data
    let descriptionMarkdown: String
}

struct ValidCase {
    let base: CorpusCase
    let snapshotIOS: String?
}

struct InvalidCase {
    let base: CorpusCase
    let expectedKeyword: String
    let expectedPath: String?
}

enum CorpusLoader {
    /// Returns all 15 valid cases from packages/conformance/cases/valid/, sorted by slug.
    static func loadValid() throws -> [ValidCase] {
        let root = corpusRoot().appendingPathComponent("valid", isDirectory: true)
        return try loadFolder(root).map { base in
            let snapshotURL = base.folder.appendingPathComponent("snapshot.ios.txt")
            let snapshot = (try? String(contentsOf: snapshotURL, encoding: .utf8))
            return ValidCase(base: base, snapshotIOS: snapshot)
        }
    }

    /// Returns all 7 invalid cases from packages/conformance/cases/invalid/, sorted by slug.
    static func loadInvalid() throws -> [InvalidCase] {
        let root = corpusRoot().appendingPathComponent("invalid", isDirectory: true)
        return try loadFolder(root).map { base in
            let expectedURL = base.folder.appendingPathComponent("expected-error.json")
            let data = try Data(contentsOf: expectedURL)
            let parsed = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
            let keyword = parsed["keyword"] as? String ?? ""
            let path = parsed["path"] as? String
            return InvalidCase(base: base, expectedKeyword: keyword, expectedPath: path)
        }
    }

    static func corpusRoot() -> URL {
        // <repo>/packages/conformance/cases/
        return repoRoot()
            .appendingPathComponent("packages", isDirectory: true)
            .appendingPathComponent("conformance", isDirectory: true)
            .appendingPathComponent("cases", isDirectory: true)
    }

    static func repoRoot() -> URL {
        // CorpusLoader.swift is at <repo>/packages/client-ios/Tests/PoppyTests/.
        // Drop four trailing path components to land on the repo root.
        let here = URL(fileURLWithPath: #filePath)
        return here
            .deletingLastPathComponent() // PoppyTests/
            .deletingLastPathComponent() // Tests/
            .deletingLastPathComponent() // client-ios/
            .deletingLastPathComponent() // packages/
            .deletingLastPathComponent() // <repo>/
    }

    private static func loadFolder(_ folder: URL) throws -> [CorpusCase] {
        let fm = FileManager.default
        let entries = try fm.contentsOfDirectory(
            at: folder,
            includingPropertiesForKeys: [.isDirectoryKey],
            options: [.skipsHiddenFiles]
        )
        var cases: [CorpusCase] = []
        for entry in entries {
            var isDir: ObjCBool = false
            guard fm.fileExists(atPath: entry.path, isDirectory: &isDir), isDir.boolValue else {
                continue
            }
            let docURL = entry.appendingPathComponent("document.json")
            let descURL = entry.appendingPathComponent("description.md")
            let docData = try Data(contentsOf: docURL)
            let desc = (try? String(contentsOf: descURL, encoding: .utf8)) ?? ""
            cases.append(
                CorpusCase(
                    slug: entry.lastPathComponent,
                    folder: entry,
                    documentJSON: docData,
                    descriptionMarkdown: desc
                )
            )
        }
        cases.sort { $0.slug < $1.slug }
        return cases
    }
}
