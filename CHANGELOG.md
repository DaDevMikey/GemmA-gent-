# Changelog

## v1.0.1 (2026-11-24)
### Added
- **Copy & Share**: Copy prompts (tap/long-press or copy icon), copy responses in both the main chat and floating overlay, and share responses via the Android share sheet.
- **Speak & Regenerate**: Read any response aloud with Text-to-Speech or regenerate the latest reply.
- **GitHub Actions Release Workflow**: Automatically tests, builds, and attaches the APK whenever a new GitHub Release is published.
- **Privacy Policy & Terms of Service**: Added `PRIVACY_POLICY.md`, `TERMS_OF_SERVICE.md`, and an in-app **Privacy & Terms** viewer.
- **Per-Chat Delete**: Delete individual conversations from Recent Chats in the side drawer.
- **Animations**: Added bouncing gradient thinking dots, live typing cursor, and smooth layout transitions.

### Changed
- **100% Local by Default**: Removed automatic startup update checks so no network requests are made unless you tap Check.
- **Cleaner UI**: Removed the temporary debug overlay buttons from the drawer and model sheet.

## v1.0.0 (2026-11-24)
- Initial release of **GemmA (Gemma Agent)** powered by on-device Gemma 4 via Android AICore.
- Gemini-style dark UI, floating `ACTION_ASSIST` overlay with swipe-to-fullscreen, multi-step tool calling, multimodal image upload & screen context, persistent Memory, Recent Chats, and `"Hey GemmA"` wake word service.
