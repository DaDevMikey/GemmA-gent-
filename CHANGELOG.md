# Changelog

## v1.0.4 (2026-11-24)
### Added
- **Selectable Text**: Prompts and responses in both the main chat and floating overlay are wrapped in `SelectionContainer` so you can long-press and copy any selected text.
- **Formatted Code Blocks**: Fenced ```` ```code blocks``` ```` now render in dark monospace code cards with a language header, horizontal scrolling, and a one-tap **Copy code** button, plus inline `` `code` `` and `**bold**` formatting.

## v1.0.3 (2026-11-24)
### Added
- **Package Rename**: Migrated Android package & application ID to `com.kairo.agent`.
- **Multi-Turn Memory & Proactive Saving**: Kairo retains the last 6 conversation turns and automatically saves important facts via `<memory>` tags.
- **Calling, Texting & Image Sending**: Added `call_person`, `text_person`, and `send_image_to_person` (via `FileProvider`) tools.
- **Setup, Compatibility & Bypass**: Added a Setup & Compatibility sheet with device checks, a **Bypass compatibility checks** toggle, and an OpenAI-compatible **Local Model Server** backend for non-AICore devices (e.g. Z Fold 8 / llama.cpp / Ollama).
- **Power Mode**: Optional Root / Shell Power Mode (`run_shell_command`) + `set_brightness`.
- **Audio-Reactive Glow & Spring Physics**: Overlay edge glow reacts live to `SpeechRecognizer` amplitude (`onRmsChanged`), with spring animations, ripple feedback, and **Cover-Screen Mini-Mode**.
- **Instant Persona Chips**: Quick-switch chips for **Default**, **Roast**, **TL;DR**, **Dev Mode**, and **Grammar / Reply** (reads clipboard).
- **On-Device Flexes**: Live `tok/s`, `TTFT`, and `RAM` nerd-stats pill, **Think Harder** (`enableThinking` re-run), **Notification Catch-Up** (`NotificationListenerService`), and an in-app **Release Notes** viewer.

## v1.0.2 (2026-11-24)
- Play-safe rebrand to **Kairo**, credits to **DaManMikey** (`damanmikey.me`), Health Connect & Contacts integration, release APK builds.

## v1.0.1 (2026-11-24)
- Copy prompts/responses, Share, Read aloud, Regenerate, GitHub Actions release workflow, Privacy Policy & Terms.

## v1.0.0 (2026-11-24)
- Initial release powered by on-device Gemma 4 via Android AICore.
