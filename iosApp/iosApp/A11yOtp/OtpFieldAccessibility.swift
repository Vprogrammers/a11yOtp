import Foundation

/// Host-provided, localized strings. There are deliberately no English defaults.
struct OtpFieldAccessibility {
    /// Accessible name of the whole field. It should match any visible label for Voice Control.
    let label: String
    /// Shown below the cells and announced once when the field enters the error state.
    let errorMessage: String
    /// Value text for progress, called with (entered, total), for example "3 of 6 digits entered".
    let progressDescription: (Int, Int) -> String
    /// Title of the keyboard toolbar button that dismisses the number pad, which has no Return key.
    let doneButtonTitle: String

    init(
        label: String,
        errorMessage: String,
        progressDescription: @escaping (Int, Int) -> String,
        doneButtonTitle: String
    ) {
        precondition(!label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, "label must not be blank")
        precondition(
            !errorMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            "errorMessage must not be blank"
        )
        precondition(
            !doneButtonTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            "doneButtonTitle must not be blank"
        )
        self.label = label
        self.errorMessage = errorMessage
        self.progressDescription = progressDescription
        self.doneButtonTitle = doneButtonTitle
    }
}
