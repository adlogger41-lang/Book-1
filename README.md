# The Old Book (Android Edition)

A beautifully aged, offline-first personal exercise notebook app designed and optimized for Android tablets, specifically tailored for the Amazon Fire HD 10 (11th generation, 2021 release, Model KFTRWI, Fire OS 7 / Android 9 API 28+).

---

## Features

- **Antique Aesthetic**: Dark brown wood backdrop, worn cracked leather cover, subtle antique gold borders, cream aged paper with realistic fiber texture, classic exercise-book blue horizontal lines, and faded red vertical margins.
- **Realistic Page Turns**: Interactive 3D paper curl animations for page transitions with swipe gesture support.
- **Seamless Ruled Writing**: Tap anywhere on a rule to begin writing. Natural text wrapping aligned with ruled lines, hardware keyboard support (Tab, Enter, Ctrl+Z, Ctrl+Y), and reliable caret positioning.
- **Dedicated Drawing Tools**:
  - Pen (freehand with smooth strokes)
  - Straight Line
  - Circle / Ellipse
  - Triangle
  - Quadrilateral
  - Drawing Eraser (erases strokes without affecting text or paper)
  - Color and stroke width selectors
  - Multi-level Undo & Redo spanning both text and completed drawing actions
- **Resolution-Independent Coordinates**: Drawing coordinates are normalized proportionally, keeping all strokes and shapes perfectly aligned to paper lines across screen orientations, resize events, and full-screen transitions.
- **Page Management**:
  - Previous / Next navigation
  - Jump to existing page
  - Insert page (automatically shifts subsequent page numbers and maintains bookmark integrity)
  - Duplicate page (copies both typed text and drawing layers)
  - Clear page
  - Bookmark pages with a dedicated bookmarks drawer
- **Paper Customization**:
  - Ruled exercise lines, graph paper, dotted grid, or blank parchment
  - Customizable leather cover color, paper tint, line color, margin color, and line spacing
- **Distraction-Free Full-Screen Mode**: Immersive mode hiding system navigation/status bars and toolbars with safe exit controls and Android system Back button handling.
- **Offline & Private**: 100% offline. No telemetry, no external accounts, no cloud dependencies.
- **Tri-Layer Data Safety**:
  1. Primary `localStorage` inside the app container
  2. Fallback recovery storage in cache
  3. Mirroring to app-private native storage (`the_old_book_state.json`) via `AndroidBridge` to guard against cache purges.
- **Export & Backup (Android Storage Access Framework)**:
  - **Export Text**: Exports all pages formatted as plain text (`.txt`).
  - **Backup Book**: Full JSON backup (`the-old-book-backup.json`) including text, drawings, bookmarks, and settings.
  - **Restore Backup**: Safely restores previous JSON backups with schema validation and pre-restore snapshots.

---

## Amazon Fire HD 10 Compatibility & Target Specs

- **Device**: Amazon Fire HD 10 (11th Gen, 2021)
- **Model Identifier**: KFTRWI
- **Display**: 10.1 inches, 1920 × 1200 pixels (16:10 aspect ratio)
- **Minimum Android API**: API 28 (Android 9.0 / Fire OS 7)
- **Hardware Keyboard**: Fully supported via connected Bluetooth or keyboard cases.

---

## How to Build the APK

### Building Locally via Gradle

Prerequisites:
- JDK 17 or higher
- Android SDK with Platform 36 and Build-Tools installed

Run:
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Automated GitHub Actions Build

The repository contains a GitHub Actions workflow: `.github/workflows/build-android.yml`.

### How to download the APK artifact from GitHub Actions:
1. Push this project to your GitHub repository or open the **Actions** tab in GitHub.
2. Select the latest run of the **Build Android APK** workflow.
3. Scroll down to the **Artifacts** section at the bottom of the workflow summary page.
4. Download the `the-old-book-debug-apk` zip archive.
5. Extract `app-debug.apk` from the zip archive.

---

## Installation on Amazon Fire HD 10 (Sideloading)

1. On your Fire HD 10 tablet, navigate to:
   **Settings > Security & Privacy > Apps from Unknown Sources**.
2. Select your browser or file manager and enable **Allow from this source**.
3. Transfer `app-debug.apk` to your tablet via USB cable, micro-SD card, or download it via local Wi-Fi.
4. Open the APK with the tablet's Files / Docs app and tap **Install**.
5. Launch **The Old Book** from your home screen.

---

## Backup and Restore Instructions

### Exporting Text
1. Open the Settings panel (⚙ icon in the header or in the left sidebar).
2. Tap **Export text**.
3. Choose a destination folder in your tablet storage to save `the-old-book.txt`.

### Creating a Full Backup
1. In the Settings panel, tap **Backup book**.
2. Save `the-old-book-backup.json` to your internal storage, Documents folder, or SD card.

### Restoring a Backup
1. In the Settings panel, tap **Restore backup**.
2. Select your `the-old-book-backup.json` file.
3. The app validates the file structure and restores all pages, drawings, bookmarks, and appearance preferences.

---

## Key Bugs Fixed in Reference Implementation

1. **Drawing Coordinate Distortion**: In the original code, drawing coordinates were stored as raw pixel values. When rotating the tablet, changing window sizes, or entering full screen, drawings misaligned with paper lines. We implemented normalized coordinate math (`nx`, `ny`), keeping drawings perfectly registered.
2. **Bookmark Drift on Page Insertion**: Inserting a page shifted subsequent pages, but the original code failed to shift bookmarks, corrupting bookmarks that pointed to later pages. The insertion algorithm now shifts bookmark indexes correspondingly.
3. **Caret Jumping & Whitespace Runaway**: When tapping empty lines, runaway spaces could wrap lines unnaturally. Caret calculation is now clamped to line width boundaries.
4. **Android File Chooser Support**: Android's `WebView` does not handle file downloads or `<input type="file">` by default without a `WebChromeClient` implementation. We integrated Android Storage Access Framework (SAF) via `AndroidBridge` and `onShowFileChooser`.
5. **System Immersive Bar Transitions**: Entering full-screen now coordinates with `WindowInsetsControllerCompat` to hide status and navigation bars without leaving black bars.
6. **Back Navigation Trapping**: Pressing the Android hardware/system Back button now gracefully exits full-screen mode or closes open dialogs rather than accidentally exiting the app.
