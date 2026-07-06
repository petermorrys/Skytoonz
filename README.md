# Frame Animator 🎨🎬

Frame Animator is a highly polished, professional frame-by-frame vector animation studio app built with Jetpack Compose and modern Material Design 3. It provides a complete sandbox for creators to sketch, organize, and export layered vector animations with synchronized audio tracks.

---

## 🚀 Key Features

*   **Pro Drawing Canvas**: Supports multi-layer vector sketching with responsive stroke rendering, custom brush styles (Pen, Pencil, Brush, Airbrush, Eraser), and dynamic pressure-sensitivity options.
*   **Onion Skinning**: Visually guides your transitions by projecting configurable before/after frames with customizable opacity.
*   **Timeline & Layer Management**: Full multi-frame timeline navigation (play, pause, step forward, step backward) and layer depth controls (Background, Middle, Foreground) with independent visibility toggles.
*   **Audio Track Synchronization**: Record vocal snippets, sound effects, or import audio files to play back and animate precisely on cue.
*   **Export Formats**: Seamlessly export your hand-drawn masterpieces to high-fidelity **Animated GIFs**, **HD Video MP4s**, or structured **Layer Vectors (JSON)**.
*   **Cloud Synchronization**: Instantly synchronize vector coordinates and asset streams with secure cloud databases for cross-device backups.

---

## 🛠️ Recent Fixes & Changes

### 1. Custom Icon Engine (`AppIcons.kt`)
*   **The Issue**: The app previously experienced massive APK bloat and compile timeouts due to dependency on the heavy `androidx.compose.material.icons.extended` package.
*   **The Solution**: We completely replaced the external icons library with a hand-drawn, high-performance Canvas-based custom rendering system inside `AppIcons.kt`. Icons like `ADD`, `DELETE`, `PLAY_ARROW`, `PAUSE`, `SETTINGS`, and `CREATE` are rendered directly via vector mathematical operations, slashing compilation overhead and keeping the app extremely lightweight.

### 2. Runtime Integrity and Crash Capture (`MyApplication` & `CrashHandler`)
*   **The Issue**: Potential early-lifecycle application crashes during startup can be difficult to diagnose without direct developer access to device consoles or active USB debug bridges.
*   **The Solution**:
    *   **Early Intervention**: Created `MyApplication` (the root application initializer) which intercepts the system lifecycle before any activity or main thread UI gets rendered.
    *   **Early-Intercept Uncaught Exception Handler**: Built `CrashHandler.kt` to monitor all global background and UI thread crashes.
    *   **Direct Local Filesystem Log Writing**: When a crash is intercepted, the handler outputs an ultra-detailed `crash_log.txt` file containing the timestamp, app metadata, full stack trace, hardware manufacturer, device model, and Android OS version.
    *   **Dual-Storage Logging**: Saves to:
        1.  **Internal Storage**: `context.filesDir/crash_log.txt` (private and secure)
        2.  **External Android Data Directory**: `context.getExternalFilesDir(null)/crash_log.txt` (directly accessible via computer USB connection or native File Manager apps under `Android/data/com.aistudio.flipacraft.nxhkqp/files/`).

### 3. "Art Animator" UI Upgrade & Custom Canvas Engine
*   **The Upgrade**: Redesigned the main dashboard with a gorgeous, high-fidelity tabbed interface featuring **Projects** and **Clips** lists, an interactive floating navigation bar, and bento-styled cards.
*   **Top Center Floating Brush Control Bar**: Quick selection of active drawing tools (Brushes, Eraser, Lasso, Paint Bucket, and Text Overlay tools) packed in an elegant semi-transparent floating horizontal glass bar with a drag handle symbol.
*   **Center End Floating Vertical Controls**: Side-docked capsule containing quick active brush size text (px), active color swatch display with modal settings trigger, and ruler guide trigger buttons.
*   **Bento-Styled Timeline & Controls**:
    *   **Frame Timeline Aligned Above**: Smoothly scrolled horizontal LazyRow listing beautiful thumbnail frame badges displaying the frame index, active selected frame indicator, and a custom red circular notification badge showing active frames.
    *   **Frame Add with Plus View**: Quick, high-contrast dashed-border button for instant frame creation directly adjacent to the timeline.
    *   **Lower Action Bar**: Beautiful bento row carrying audio sync, undo/redo, copy/paste shortcuts, and active layers stack counts.
*   **Active Ruler Guides (Line, Circle, Box, Mirror)**:
    *   *Real-time snapping*: Automatically snaps drawing paths to lines, perfect circles, rectangles, or mirrors across a vertical axis.
    *   *Interactive Guides*: Direct drag-and-drop support for shifting the origin center of circle/box guides, and live pinching/dragging to adjust sizing.
    *   *Lock/Unlock Option*: High-contrast active bar below the canvas allowing creators to lock ruler positioning in place to focus entirely on tracing strokes.
*   **Clipboard Copy-Paste Engine**: Integrated local clipboard buffers allowing users to copy complete vector path data from any layer/frame and paste them instantly across frames to fast-track repetitive frame edits.
*   **Interactive Theme Settings**: Fully integrated settings panel enabling creators to switch between high-contrast light theme, eye-safe slate-dark theme, or follow system configuration.
*   **Custom Background Engine**: Creators can now select from 14 background canvas textures and presets on project creation, including:
    *   *Solid colors*: White, Black, Gray, Cream, Blue, Green, Pink.
    *   *Interactive textures*: Ruled school paper, grid lines, dot grids, sketch patterns, brown Kraft cardboard, and blackboard Chalk green.
    *   All background swatches feature miniature vector representations drawn live in the creation dialog to preview their look before building!
*   **Widescreen & Cinematic Canvas Presets**: Selectable aspect ratios including:
    *   *YouTube (16:9 Landscape)*: 1920 x 1080, perfect for widescreen cartoons.
    *   *Instagram (1:1 Square)*: 1080 x 1080, optimized for social media posts.
    *   *TikTok / Shorts (9:16 Vertical)*: 1080 x 1920, tailored for mobile viewers.
    *   *Standard (4:3 Retro)*: 1440 x 1080, classic traditional cartoon canvas.
*   **Preloaded "Cat Jump" Demo**: Pre-populates the workspace on launch with a gorgeous 3-frame jumping cat vector animation. Users can tap it, hit play, and inspect real vector paths immediately!

---

## 📂 Crash Log Retrieval

To inspect or download crash logs directly from a phone or emulator:

1.  **Via USB / PC File Explorer**:
    *   Connect your device and navigate to:
        `Internal Storage/Android/data/com.aistudio.flipacraft.nxhkqp/files/crash_log.txt`
2.  **Via On-Device File Manager**:
    *   Open your favorite File Manager app.
    *   Navigate to `Android -> data -> com.aistudio.flipacraft.nxhkqp -> files -> crash_log.txt`.
3.  **Via App Sandbox (Rooted / ADB)**:
    *   View internal reports at:
        `/data/data/com.aistudio.flipacraft.nxhkqp/files/crash_log.txt`

---

## 📐 Architecture & Technology Stack

*   **Jetpack Compose**: 100% declarative modern UI, tailored around Material Design 3 guidelines with elegant dark slate color palettes (`0xFF0F172A`).
*   **MVVM Architecture**: Separates the rich timeline, recording, and vector editing state in a lifecycle-aware `AnimationViewModel` to keep UI composables completely stateless and testable.
*   **Kotlin Coroutines & Flow**: Drives asynchronous onion skin compositing, frame export background tasks, and seamless real-time audio playback states.
