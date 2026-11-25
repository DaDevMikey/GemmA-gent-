# Changelog

## v1.1.7
- Full Markdown rendering in Release Notes & chat (`#`/`##`/`###` headings, bullet lists, inline code, bold)
- Added **Micro-Agent Router** (intent splitting) and **Self-Healing Retry Loop** in `AgentRunner`
- Added **Ephemeral Clipboard & Screen Memory** (7-day rolling local store)
- Added **HardwareGuard** for thermal and battery-aware execution
- Added **Exynos 2500+** (`S5E9955`, `EXYNOS 2600`) to the SoC allowlist

## v1.1.6
- Fixed dual launcher icon by removing `LAUNCHER` intent-filter from `LaunchOverlayActivity` and setting `singleTask` on `MainActivity`
- Added Snapdragon 8 Elite+ & Tensor G5+ SoC allowlist check in Setup with explicit "Bypass & Continue Anyway" option for incompatible devices
- Added required Privacy Policy agreement on the first Setup screen
- Added per-letter Twidget-style `StaggeredBlurText` animation on Setup welcome
- Added version filter chips to Release Notes (rendered in Markdown)
- Added live drag-progress blur and smooth scrolling across all `ModalBottomSheet` menus
- Added `BackHandler` in active chats to start a new chat
- Added "Support Project (PayPal)" button in Settings and credited `thatjoshguy67` for Twidget/One UI widget inspiration

## v1.1.5 (2026-11-25)
### Added & Fixed
- **Fixed Chat-to-Home Shared Sparkle Transition**: Preserved exiting turn bounds during `AnimatedContent` transition so the sparkle icon smoothly glides back to the center on New Chat.
- **Twidget-Style `BlurIntoText` & Full Setup Redesign**: Added blur-into-sharp text reveals across onboarding & home greetings, with a clean Twidget-inspired `"Welcome to Kairo"` + pill `"Get started"` welcome page.
- **Official Samsung One UI Widget Hook**: Added `theming-meta` (`meta_998_sesl_app`) and `com.sec.android.appwidget.widgetinfo` (`widget_supported_cell_size_info.xml`) to Home and Cover-Screen widgets.
- **Fixed Main App Microphone**: Reset `SpeechRecognizer` cleanly on errors, pause WakeWordService during mic taps, surface voice error toasts, and fall back to `RecognizerIntent` when needed.
- **Live Drag-Progress Sheet Blur**: Bottom sheets (including `Kairo • Powered by Gemma 4`) now reduce background blur smoothly in real time as you drag them down.
- **Settings Hub & Dangerous Tool Confirmation**: Moved Memory & Connections into Settings (accessible from the bottom-right drawer gear icon next to `Kairo • by DaManMikey`), updated Model Selector to reflect active Cloud AI, and added a 2nd confirmation dialog before dangerous tool calls (`call_person`, `send_whatsapp`, `send_email`, `run_shell_command`, etc.).

## v1.1.4 (2026-11-25)
### Added & Improved
- **One UI-Inspired Intro & Home Welcome Animations**: Added smooth staggered rise + fade-in animations (`CubicBezierEasing(0.117, 0.126, 0.312, 1.0)`), extended the final Setup Welcome screen to 3.4s, and refined the shared-element sparkle transition when returning to New Chat.
- **Cleaner Input Bar (Star Icon)**: Replaced `+` with a Star (`AutoAwesome`) icon that toggles both Modes (`Default`, `Roast`, `TL;DR`, `Dev Mode`, `Grammar / Reply`) and Quick Suggestions.
- **Kairo Pronunciation (`/kaj/roo/`) & Wake-Word**: Updated `WakeWordService` to recognize `/kaj/roo/` phonetic variants (`cairo`, `kyro`, `kai roo`, etc.) and documented pronunciation in Setup & Credits.
- **In-App-Only `web_search` & Empty-Response Fallbacks**: `web_search` never leaves the app to open a browser, and `AgentRunner` synthesizes clean fallbacks if the model returns empty output.
- **Memory Deduplication & Pinned Drawer Footer**: Normalized memory matching prevents duplicate facts, and `Kairo • by DaManMikey` stays pinned at the bottom of the drawer.
- **Expanded Health Connect Insights**: Added Active Calories, Hydration, Floors Climbed, and Resting Heart Rate (`main` edition).
- **Disable Cloud AI Label & GitHub Pages Site (`docs/`)**: Renamed the Cloud AI off-button to **"Disable Cloud AI"** and added a full GitHub Pages website (`docs/index.html`, `docs/privacy.html`, `docs/terms.html`, screenshots).

## v1.1.3 (2026-11-25)
### Added & Fixed
- **Fixed Chat Output Letter Clipping**: Moved rounded clipping onto the shine/stardust overlay layer so letters never get cut off at corners.
- **Fixed Swipe-Up to Open Full App**: Overlay swipe-up now reliably brings `MainActivity` to the foreground before closing the overlay.
- **In-App Web Search Results**: `web_search` now fetches live summaries/snippets in-app via DuckDuckGo & Wikipedia so Kairo answers directly inside chat.
- **Optional Cloud AI (Double Confirmation + Always-Off Switch)**: Added `CLOUD_AI` backend with a 2-step privacy warning confirmation, API key/endpoint config, and a persistent 1-tap **"Cloud AI • Tap to Off"** pill in the top bar.
- **MCP (Model Context Protocol) Support**: Added `McpClientManager` (`tools/list`, `tools/call`) and MCP server configuration + testing in Settings.
- **Scrollable Menus Everywhere**: Made the entire navigation drawer and all bottom sheets scrollable for foldable and compact displays.
- **Google Play `.lite` Package**: `google-play` branch now uses `com.kairo.agent.lite` (`applicationId` + `FileProvider`).

## v1.1.2 (2026-11-25)
### Added
- **Local Python Script Runner (`run_python_script`) & Exact Time (`get_current_time`)**: Kairo can now run local Python scripts (`math`, `datetime.now()`, variables, lists, `print(...)`) using `python3` when available on-device or its built-in sandboxed Python-subset evaluator, and confirm exact local time, date, weekday, and timezone.

## v1.1.1 (2026-11-25)
### Added & Improved
- **Star/Dust Typing Particles & Response Shine Sweep**: Added `StarDustParticles` drifting while Kairo streams and a diagonal `ResponseShineSweep` light effect when a response finishes.
- **Floating Overlay Blur & Edge-to-Edge Fullscreen**: Added Android 12+ background blur (`blurBehindRadius`) behind the overlay plus smooth bezel-to-bezel fullscreen expansion with animated glowing border.
- **In-App Live Weather & New Tools**: `get_weather` now fetches live temperature, humidity, wind, and conditions in-app via Open-Meteo (with GPS or city lookup), alongside `calculate_or_convert`, `get_storage_and_memory`, `translate_text`, and `set_ringer_mode`.
- **Expanded Health Connect Insights**: Added Sleep, Distance, Total Calories Burned, Average Heart Rate, and Exercise Sessions (in the GitHub full edition).
- **Prominent Permission Disclosures**: Added clear disclosure popups before every permission request in both Setup and Connections.

## v1.1.0 (2026-11-25)
### Added & Fixed
- **Isolated Floating Overlay Task**: Separated `LaunchOverlayActivity` and `AssistantOverlayActivity` into their own `com.kairo.agent.overlay` `taskAffinity` (`singleInstance`, `excludeFromRecents`) so launching the overlay opens only the floating popup without bringing the full Kairo app behind it.
- **Location Permission & `get_current_location`**: Added optional foreground location support (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) so Kairo can answer local weather, nearby places, and current city/address queries 100% on-device.
- **Animated On-Device Privacy & Prominent Disclosures**: Added an animated `OnDevicePrivacyBanner` in Setup plus prominent in-app disclosure dialogs before requesting Microphone, Location, or Contacts.
- **Rebranded Repository Links**: Updated update checker, in-app source links, and documentation to `https://github.com/DaDevMikey/Kairo`.
- **Google Play Branch (`google-play`)**: Added a dedicated Play-Store-compliant branch with `ACTION_DIAL` (no `CALL_PHONE`), no Health Connect, no `WRITE_SETTINGS`, and no shell/root power mode.

## v1.0.9 (2026-11-24)
### Added & Improved
- **Home Screen & Cover-Screen Widgets**: Added `Kairo Quick Ask` (4×1 resizable home widget with Ask, Voice, and Chat actions) and `Kairo Cover Screen` (compact 2×1 foldable cover-screen & lock-screen widget with direct Voice/Ask overlay launch).
- **Split-Screen Multitasking & More App Connections**: Added `open_split_screen` (launch one or two apps side-by-side), `media_control` (play/pause/next/previous/stop), `open_spotify`, `send_whatsapp`, `open_maps_search`, and `search_play_store`.
- **Smooth Synced Menu Blur**: Background blur now animates out smoothly in sync when bottom sheets or menus close instead of snapping off afterward.
- **Detailed Setup Permission Info**: Added clear explanations to every permission and connection card in Onboarding Setup so users know exactly what each capability enables.

## v1.0.8 (2026-11-24)
### Fixed & Improved
- **AICore Model Discovery & Selection**: Upgraded ML Kit GenAI Prompt API to `1.0.0-beta5` and added automatic discovery across **Stable** and **Preview** (**Fast** & **Full**) AICore models.
- **Preview Model Support (`[Preview, CPU]` / Developer Preview)**: `Auto (Best Available)` now automatically falls back to `Preview • Full` / `Preview • Fast` when Stable Feature 636 isn't provisioned on a device, and lets you pick any available AICore model directly from the model sheet or onboarding setup.
- **Clearer `ErrorCode 606` Diagnostics**: Shows an actionable prompt to switch AICore Model profiles or use Local HTTP if an AICore feature isn't provisioned.

## v1.0.7 (2026-11-24)
### Fixed & Improved
- **User-Only Memory**: Tightened `<memory>` rules and filtering so Kairo only remembers facts about you—never its own responses or device status outputs.
- **Final Setup Welcome Screen**: Added a final animated `"Welcome, <Name> • Let's get started"` screen with the Kairo sparkle that transitions seamlessly into chat.
- **Settings & Nerd Stats**: Added a dedicated **Settings & Nerd Stats** sheet in the drawer and a **Live Nerd Stats** toggle in the model sheet.
- **Real-Time Swipe & Response Blur**: Background blur now tracks your drawer swipe progress (`currentOffset`) in real time with zero delay, and responses animate in with a blur-to-sharp reveal.
- **More Easter Eggs & Greetings**: Added unique rotating start-page greetings, logo tap (5/10/15x) and long-press Easter eggs, a 45-second `"You still there? 👀"` idle Easter egg, plus `/ping`, `/matrix`, and `/coffee` chat commands.
- **Public & Play Store Readiness**: Updated `PRIVACY_POLICY.md` and `TERMS_OF_SERVICE.md` for `com.kairo.agent`.

## v1.0.6 (2026-11-24)
### Added
- **Shared-Element Logo Transition**: The centered Kairo sparkle now smoothly flies and morphs from the center of the screen directly to next to Kairo's response when you send your first message.
- **Reactive Multi-Orb Aurora Background & Frosted Blur**: Added `ReactiveAuroraBackground` with blurred glowing orbs that react live to voice amplitude and generation, plus animated background blur when drawers or sheets open.
- **Upgraded Setup Flow**: Frosted glass setup cards, animated step headlines, a **Personalize** step (saves your name to Memory), and example prompt chips right after completing setup.
- **Permission Fixes**: Fixed Health Connect `Granted` status detection via `HealthConnectClient.permissionController.getGrantedPermissions()`, and declared `WRITE_SETTINGS` in `AndroidManifest.xml` so System Brightness can be toggled.

## v1.0.5 (2026-11-24)
### Added
- **First-Run Onboarding Flow**: Setup & compatibility checks, backend configuration, and permissions now run cleanly before entering the app for the first time, rather than cluttering the main menus.
- **Live GitHub Release Notes & Exciting Update Card**: Release notes are fetched directly from GitHub Releases, and available updates appear in a highlighted release card with version diff and release notes.
- **Enhanced Connections & Permissions**: Live granted/missing status for Microphone, Contacts, Direct Calling, Notification Catch-Up, Health Connect, and System Brightness.
- **New Tools**: Added `get_weather`, `open_clock`, and `create_note`.
- **Easter Eggs**: Tap the center sparkle 5+ times or try `/about` / `konami` in chat.

## v1.0.4 (2026-11-24)
- Selectable text (`SelectionContainer`) and formatted ```` ```code blocks``` ```` with language headers and one-tap copy.

## v1.0.3 (2026-11-24)
- Migrated package to `com.kairo.agent`, multi-turn conversation memory, proactive `<memory>` saving, call/text/image sending, local model endpoint support, Root/Shell Power Mode, audio-reactive glow, personas, nerd stats, and Notification Catch-Up.
