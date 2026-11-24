# Changelog

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
