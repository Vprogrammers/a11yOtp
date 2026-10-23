import XCTest

/// Drives the SwiftUI sample to check the Kotlin UIKit OTP components on a device or simulator.
/// VoiceOver speech, one-time-code autofill from Messages and the system paste prompt are manual checks.
final class OtpSampleUITests: XCTestCase {
    private let timeout: TimeInterval = 5
    private var app = XCUIApplication()

    /// Plain text field, or a secure one while masked.
    private var field: XCUIElement {
        let plain = app.textFields["Verification code"]
        return plain.exists ? plain : app.secureTextFields["Verification code"]
    }
    private var verifyButton: XCUIElement { app.buttons["Verify"] }
    private var verifiedLabel: XCUIElement { app.staticTexts["Code verified"] }

    override func setUpWithError() throws {
        continueAfterFailure = false
        launch(manualVerify: false)
    }

    // MARK: - Accessibility audit

    /// Apple's accessibility audit (contrast, Dynamic Type, clipped text, hit regions, element
    /// descriptions, traits). With the keyboard and Done key showing, every element above the keyboard
    /// counts, Done included; then the whole screen in the error state with the keyboard dismissed.
    func testAccessibilityAuditPasses() throws {
        try auditKeyboardAndErrorStates()
    }

    func testAccessibilityAuditPassesInDarkMode() throws {
        XCUIDevice.shared.appearance = .dark
        defer { XCUIDevice.shared.appearance = .light }
        relaunch(manualVerify: false)
        try auditKeyboardAndErrorStates()
    }

    func testAccessibilityAuditPassesAtTheLargestAccessibilityTextSize() throws {
        app.terminate()
        let largest = ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"]
        launch(manualVerify: false, extraArguments: largest)
        try auditKeyboardAndErrorStates()
    }

    private func auditKeyboardAndErrorStates() throws {
        let done = app.buttons["Done"]
        XCTAssertTrue(done.waitForExistence(timeout: timeout))
        // The Done row starts one key gap (6 pt) above the key.
        try auditIgnoringContentBehindKeyboard(keyboardTop: done.frame.minY - 6)
        type("000000")
        XCTAssertTrue(app.staticTexts["Incorrect code. Try again."].waitForExistence(timeout: timeout))
        dismissKeyboard()
        XCTAssertTrue(waitForKeyboardToHide())
        try app.performAccessibilityAudit()
    }

    /// App content scrolled under the keyboard and the Done row is partly covered, which the audit
    /// reports as failed contrast or as text without an element (the covered area belongs to the
    /// keyboard, so it finds no element there; such issues carry no element or frame). Only those two
    /// audit types are ignored, and only for content reaching under the keyboard; the same content is
    /// audited without exceptions once the keyboard is dismissed.
    private func auditIgnoringContentBehindKeyboard(keyboardTop: CGFloat) throws {
        try app.performAccessibilityAudit { issue in
            guard issue.auditType == .contrast || issue.auditType == .elementDetection else { return false }
            guard let element = issue.element else { return issue.auditType == .elementDetection }
            return element.label != "Done" && element.frame.maxY > keyboardTop
        }
    }

    // MARK: - Focus and structure

    func testFieldIsFocusedAndKeyboardShownOnLaunch() {
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: timeout))
        XCTAssertTrue(hasKeyboardFocus(field))
    }

    func testFieldIsOneAccessibilityElementWithLabelAndProgress() {
        XCTAssertEqual(app.textFields.matching(identifier: "Verification code").count, 1)
        XCTAssertEqual(field.label, "Verification code")
        XCTAssertEqual(valueText, "0 of 6 digits entered")
        type("123")
        XCTAssertEqual(valueText, "1 2 3, 3 of 6 digits entered")
        // XCUITest also lists non-accessible views (the cells), so it cannot prove the cells are hidden
        // from VoiceOver; that stays a manual check. It does prove nothing else in the field is interactive.
        XCTAssertEqual(app.textFields.count + app.secureTextFields.count, 1)
    }

    func testDoneButtonDismissesTheKeyboard() {
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: timeout))
        let done = app.buttons["Done"]
        XCTAssertTrue(done.waitForExistence(timeout: timeout))
        done.tap()
        XCTAssertTrue(waitForKeyboardToHide())
    }

    // MARK: - Key-by-key input

    /// Keys typed in quick succession arrive one per run-loop turn and must stay separate digits; only
    /// one-time-code autofill (which iOS sends with its own signature) is applied as one input.
    /// XCUITest cannot send a real hardware Escape on iOS, so Escape stays a manual check.
    func testQuicklyTypedKeysAreNeitherMergedNorRejected() throws {
        try skipUnlessSimulator()
        for key in ["4", "8", "2"] { field.typeKey(key, modifierFlags: []) }
        XCTAssertEqual(valueText, "4 8 2, 3 of 6 digits entered")
    }

    private func skipUnlessSimulator() throws {
        #if !targetEnvironment(simulator)
        throw XCTSkip("typeKey needs the simulator or a connected hardware keyboard.")
        #endif
    }

    // MARK: - Typing and deleting

    func testTypingUpdatesProgressAndBackspaceRemovesDigit() {
        type("12")
        XCTAssertEqual(valueText, "1 2, 2 of 6 digits entered")
        app.keys["Delete"].tap()
        XCTAssertEqual(valueText, "1, 1 of 6 digits entered")
    }

    func testVerifyIsEnabledOnlyForCompleteCode() {
        relaunch(manualVerify: true)
        XCTAssertTrue(verifyButton.exists)
        XCTAssertFalse(verifyButton.isEnabled)
        type("12345")
        XCTAssertFalse(verifyButton.isEnabled)
        type("6")
        XCTAssertTrue(verifyButton.isEnabled)
        XCTAssertFalse(verifiedLabel.exists, "Completion must not verify in manual mode")
        verifyButton.tap()
        XCTAssertTrue(verifiedLabel.waitForExistence(timeout: timeout))
        XCTAssertEqual(app.staticTexts.matching(identifier: "Code verified").count, 1)
        XCTAssertFalse(verifyButton.isEnabled)
    }

    func testDeletingADigitDisablesVerifyAgain() {
        relaunch(manualVerify: true)
        type("123456")
        XCTAssertTrue(verifyButton.isEnabled)
        app.keys["Delete"].tap()
        XCTAssertFalse(verifyButton.isEnabled)
    }

    // MARK: - Completion

    func testCompletionShowsCodeVerifiedExactlyOnce() {
        type("123456")
        XCTAssertTrue(verifiedLabel.waitForExistence(timeout: timeout))
        // Give a duplicate completion time to show up before counting.
        Thread.sleep(forTimeInterval: 1)
        XCTAssertEqual(app.staticTexts.matching(identifier: "Code verified").count, 1)
    }

    func testResendAfterCompletionClearsCodeAndVerifiedState() {
        type("123456")
        XCTAssertTrue(verifiedLabel.waitForExistence(timeout: timeout))
        // The field is disabled once verified; resending clears the code and the result.
        resendWhenAvailable()
        XCTAssertFalse(verifiedLabel.exists)
        XCTAssertEqual(valueText, "0 of 6 digits entered")
    }

    // MARK: - Error state

    func testRejectedCodeExposesErrorAndTypingClearsIt() {
        type("000000")
        let error = "Incorrect code. Try again."
        XCTAssertTrue(valueText.contains(error), "Value was: \(valueText)")
        XCTAssertTrue(valueText.contains("6 of 6 digits entered"))
        XCTAssertFalse(verifiedLabel.exists)
        // Without color alone: the visible error text is on screen too.
        XCTAssertTrue(app.staticTexts[error].exists)

        app.keys["Delete"].tap()
        XCTAssertFalse(valueText.contains(error), "Value was: \(valueText)")
        XCTAssertEqual(valueText, "0 0 0 0 0, 5 of 6 digits entered")
        XCTAssertFalse(app.staticTexts[error].exists)
    }

    // MARK: - Masking

    func testMaskedModeExposesProgressWithoutDigits() {
        dismissKeyboard()
        app.buttons["Character"].tap()
        field.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: timeout))
        type("123")
        XCTAssertEqual(valueText, "3 of 6 digits entered")
        XCTAssertFalse(valueText.contains("1"), "Masked value leaks digits: \(valueText)")
        XCTAssertFalse(valueText.contains("2"), "Masked value leaks digits: \(valueText)")
        for digit in ["1", "2", "3"] {
            XCTAssertFalse(app.staticTexts[digit].exists)
        }
    }

    // MARK: - Resend

    func testResendCountsDownThenEnablesAndClearsCode() {
        let waiting = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Resend code in'")).firstMatch
        XCTAssertTrue(waiting.waitForExistence(timeout: timeout))
        XCTAssertFalse(waiting.isEnabled)
        type("12")

        let resend = app.buttons["Resend code"]
        XCTAssertTrue(resend.waitForExistence(timeout: 15), "Countdown did not finish")
        XCTAssertTrue(resend.isEnabled)
        resend.tap()
        XCTAssertEqual(valueText, "0 of 6 digits entered")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Resend code in'")).firstMatch
            .waitForExistence(timeout: timeout), "Countdown did not restart")
    }

    // MARK: - Helpers

    private func launch(manualVerify: Bool, extraArguments: [String] = []) {
        app = XCUIApplication()
        app.launchArguments += extraArguments
        // A short countdown keeps the resend test fast; other tests never wait for it.
        app.launchArguments += ["-resendSeconds", "8"]
        if manualVerify { app.launchArguments += ["-manualVerify", "YES"] }
        app.launch()
        XCTAssertTrue(field.waitForExistence(timeout: timeout), "The OTP field is missing")
    }

    private func relaunch(manualVerify: Bool) {
        app.terminate()
        launch(manualVerify: manualVerify)
    }

    private var valueText: String { (field.value as? String) ?? "" }

    private func type(_ digits: String) {
        for digit in digits {
            let key = app.keys[String(digit)]
            XCTAssertTrue(key.waitForExistence(timeout: timeout), "Key \(digit) is missing")
            key.tap()
        }
    }

    private func hasKeyboardFocus(_ element: XCUIElement) -> Bool {
        (element.value(forKey: "hasKeyboardFocus") as? Bool) ?? false
    }

    private func waitForKeyboardToHide() -> Bool {
        let gone = NSPredicate(format: "exists == false")
        let expectation = XCTNSPredicateExpectation(predicate: gone, object: app.keyboards.firstMatch)
        return XCTWaiter().wait(for: [expectation], timeout: timeout) == .completed
    }

    private func dismissKeyboard() {
        let done = app.buttons["Done"]
        if done.waitForExistence(timeout: timeout) { done.tap() }
    }

    private func resendWhenAvailable() {
        let resend = app.buttons["Resend code"]
        XCTAssertTrue(resend.waitForExistence(timeout: 15), "Countdown did not finish")
        resend.tap()
    }
}
