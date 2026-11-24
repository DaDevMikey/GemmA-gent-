# Privacy Policy — Kairo (On-Device AI Agent)

**Effective Date:** November 24, 2026

**Kairo** (`com.kairo.agent`), created by **DaManMikey** ([damanmikey.me](https://damanmikey.me/)), is designed as a **100% local, on-device AI agent**. Your privacy is fundamental to how the app works.

## 1. Local Processing Only
- **AI Inference:** All text and image prompts are processed locally on your device using **Gemma 4** via **Android AICore** (`com.google.mlkit:genai-prompt`) or your own configured local model endpoint. Nothing you type, say, or attach is sent to any developer-operated server or cloud LLM.
- **Voice & Audio:** Speech recognition and text-to-speech use Android's system services (`SpeechRecognizer` and `TextToSpeech`). Kairo does not record, upload, or store raw audio files.
- **Screen Context:** When invoked as your Default Digital Assistant, `AssistStructure` text and screenshots are held only in memory for your active prompt and are never transmitted off-device.

## 2. Local Storage
- **Recent Chats & Memory:** Conversations (`recent_chats.json`) and saved user memories (`gemma_memory.json`) are stored solely in your device's private app storage (`filesDir`). You can delete individual chats, clear all chats, or delete memories at any time inside the app.

## 3. Permissions & Device Actions
- Kairo only uses Android permissions (`RECORD_AUDIO`, `READ_CONTACTS`, `CALL_PHONE`, `SET_ALARM`, `WRITE_SETTINGS`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, Health Connect `READ_STEPS` / `READ_HEART_RATE`, and `BIND_NOTIFICATION_LISTENER_SERVICE`) to perform actions you explicitly request.
- Kairo contains **no analytics, no ads, and no tracking SDKs**.

## 4. Optional Update & Release Notes Check
- When you manually tap **Check for Updates** or **Release Notes**, Kairo performs a read-only `GET` request to the GitHub Releases API (`api.github.com`) containing only the app's version number in the `User-Agent` header. No personal or conversation data is ever included.
