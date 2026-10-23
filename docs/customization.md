# Customization

Change the look through `style`, `mask` and `alignment`. Accessibility never depends on the look: the field is one labelled text input whatever you draw, and the cells are always hidden from screen readers.

## Field style

Create a style with `OtpFieldDefaults.style(...)` and pass it to the field (`style =` on Android, `field.style =` on iOS).

| | Android (`dp`, `sp`) | iOS (points; grows with Dynamic Type) |
|---|---|---|
| Text | `textStyle` (default monospace, 24 sp, medium) | `font` (default monospaced 28 pt, semibold), scaled with `fontTextStyle` (default `.title1`) |
| Cell size | `cellWidth`, `cellHeight`: minimums, raised to at least 48 dp | `minCellWidth`, `minCellHeight` (default 48 × 56): raised to at least 44 pt |
| Cell shape | `cellShape` (default rounded 8 dp) | `cornerRadius` (default 8; capped at half the cell size, so a large value with equal width and height gives round cells) |
| Spacing | `cellSpacing` (8 dp) | `cellSpacing` (8) |
| Borders | `borderWidth` (1 dp), `focusedBorderWidth` (2 dp) | `borderWidth` (1), `focusedBorderWidth` (2) |
| Grouping | `groupSize` (null = none) | `groupSize` (0 = none) |
| Placeholder | `placeholder: Char?` in empty cells | `placeholder: String?` in empty cells |
| Error text | `errorTextStyle` (14 sp) | `errorTextStyle` (default `.callout`) |
| Focused cell lift | `liftFocusedCell` (default on) | `withFocusLift(false)` turns it off |
| Liquid Glass | – | `withGlass(true)`: iOS 26 glass backdrop behind the cell row |
| Colors | `colors` (null = follow the system theme) | `colors` (dynamic system colors) |

iOS also has copy helpers: `withGroupSize`, `withCornerRadius`, `withMinCellSize`, `withGlass` and `withFocusLift`.

Rules the styles enforce:

- **Cells never get smaller than the minimum touch target.** Cells grow with the font scale or Dynamic Type; sizes are minimums.
- **`focusedBorderWidth` must be larger than `borderWidth`.** Focus and errors are shown by border width too, never by color alone.
- **Grouping is decorative.** `groupSize = 3` draws `123 – 456` without changing the code. Separators are drawn only when the length splits into equal groups, so 3 groups a 6-digit code but leaves 4- and 8-digit codes ungrouped.

On iOS, create a style once (for example `static let`) and reuse it, rather than creating one per SwiftUI update.

## Colors and dark mode

**Android:**

```kotlin
OtpFieldDefaults.style()                                   // follows the system light or dark theme
OtpFieldDefaults.style(colors = OtpFieldDefaults.colors()) // always light
OtpFieldDefaults.style(colors = OtpFieldDefaults.darkColors())
OtpFieldDefaults.style(colors = if (appIsDark) OtpFieldDefaults.darkColors() else OtpFieldDefaults.colors())
```

A style without `colors` follows the system theme, also when you change other style values. If your app has its own theme switch, pick the colors yourself as in the last line. `OtpButtonDefaults` has the same `colors()`, `darkColors()` and `systemColors()`.

**iOS:** the default colors are dynamic system colors (`labelColor`, `systemBackgroundColor`, …), so dark mode and Increase Contrast work automatically. If you pass your own `UIColor`s, use dynamic colors to keep that behavior.

Both default palettes meet WCAG contrast: 4.5:1 for text, 3:1 for cell borders and focus. In dark mode the focused cell has a white border and a white shadow, because a blue border would be too close to the normal border and a black shadow would not show.

**Your own colors are checked.** The library compares your colors with the WCAG minimums and logs a warning that names the failing pair and its ratio, but it still uses your colors, so brand colors keep working. The warning goes to Logcat (tag `A11yOtp`) on Android and to the Xcode console on iOS. Translucent colors are not checked, because their contrast depends on what is behind them.

`OtpContrast.ratio(foreground, background)` and `OtpContrast.warning(...)` are available for checking your own color pairs (colors as `0xRRGGBB`).

## Masks

```kotlin
mask = OtpMask.None                 // default: digits are shown and read by screen readers
mask = OtpMask.Character('•')       // each digit drawn as the character
mask = OtpMask.FilledCell           // entered cells are filled, no glyph
```

In Swift: `OtpMaskNone.shared`, `OtpMaskCharacter(char: 0x2022)` and `OtpMaskFilledCell.shared`.

Any mask other than `None` does more than change the look:

- the field is treated as a password field;
- screen readers announce only progress, never the digits;
- copying is blocked;
- on Android, the keyboard becomes `NumberPassword`.

So what is hidden on screen is also hidden from assistive technology. Leave codes visible unless you have a reason to hide them: visible digits let users check what they typed.

## Alignment

`alignment = OtpAlignment.START`, `CENTER` (default) or `END` positions the cells and the error text. `START` and `END` follow the layout direction, so they mirror in right-to-left layouts; the digits inside the row always read left to right. Pass the same alignment to the buttons to keep them lined up.

## Button style

`OtpButtonDefaults.style(...)`:

| Android | iOS |
|---|---|
| `textStyle`, `cornerRadius` / `shape`, `minHeight` (48 dp), `contentPadding`, `borderWidth`, `colors` | `font`, `fontTextStyle` (`.headline`), `cornerRadius`, `minHeight` (48), `horizontalPadding`, `verticalPadding`, `borderWidth`, `colors`, `withGlass(true)` (iOS 26 glass capsule while enabled) |

The visible text is always the accessible name. On the resend button, `countdownDescription` should start with the label (`Resend code in 25 s`), so Voice Control users can say the label they see.

## The iOS Done key

The number pad has no Return key, so the field adds a Done key above the keyboard, titled `doneButtonTitle` (required, localized by you). It is shaped and placed like the number pad's **3** key.

- **Dismissing:** Done dismisses the keyboard, and so do the hardware Return and Escape keys and VoiceOver's two-finger scrub.
- **Color:** the fill is your tint color, darkened if needed so the white title reaches 4.5:1 contrast (7:1 with Increase Contrast). A translucent tint is used at full opacity.
- **Layout:** the row that holds the key is transparent, so the code suggestion stays tappable. It adds the key height plus 12 pt (about 60 pt) above the keyboard; account for it in layouts that avoid the keyboard.
- **Placement:** the size and position are measured from the iOS 26 iPhone number pad. They respect the safe area in landscape, and on iPad the key keeps the iPhone width.

Android needs no extra key: the keyboard's own Done key closes it.

## Haptics

The field vibrates once when it enters the error state. Turn it off with `hapticFeedbackOnError = false` (both platforms). A rejection does not vibrate.
