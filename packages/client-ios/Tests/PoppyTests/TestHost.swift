// A reusable PoppyHost stub that records every callback so behavioral tests
// can assert what actions were dispatched, what URLs were rejected, and
// which errors flowed through onError.

import Foundation
@testable import Poppy

final class TestHost: PoppyHost {
    private(set) var actions: [Action] = []
    private(set) var errors: [Error] = []
    var allowURL: (String, ImageContext) -> Bool = { _, _ in true }

    func onAction(_ action: Action) {
        actions.append(action)
    }

    func onError(_ error: Error) {
        errors.append(error)
    }

    func isUrlAllowed(_ url: String, context: ImageContext) -> Bool {
        return allowURL(url, context)
    }
}
