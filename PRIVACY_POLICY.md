# Privacy Policy — GemmA (Gemma Agent)

**Effective Date:** November 24, 2026

GemmA ("Gemma Agent") is designed as a **100% local, on-device AI agent**. Your privacy is fundamental to how the app works.

## 1. Local Processing Only
- **AI Inference:** All text and image prompts are processed locally on your device using **Gemma 4** via **Android AICore** (`com.google.mlkit:genai-prompt`). Nothing you type, say, or attach is sent to any developer-operated server or cloud LLM.
- **Voice & Audio:** Speech recognition and text-to-speech use Android's system services (`SpeechRecognizer` and `TextToSpeech`). GemmA does not record, upload, or store raw audio files.
- **Screen Context:** When invoked as your Default Digital Assistant, `AssistStructure` text and screenshots are held only in memory for your active prompt and are never transmitted off-device.

## 2. Local Storage
- **Recent Chats & Memory:** Conversations (`recent_chats.json`) and saved memories (`gemma_memory.json`) are stored solely in your device's private app storage (`filesDir`). You can delete individual chats, clear all chats, or delete memories at any time inside the app.

## 3. Permissions & Device Actions
- GemmA only uses Android permissions (`RECORD_AUDIO`, `SET_ALARM`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`) to perform actions you explicitly request (voice input, wake word, alarms/timers, opening apps, settings, flashlight, volume, etc.).
- GemmA contains **no analytics, no ads, and no tracking SDKs**.

## 4. Optional Update Check
- If you check for app updates, GemmA performs a read-only `GET` request to the public GitHub Releases endpoint (`api.github.com`) containing only the app's version number in the `User-Agent` header. No personal or conversation data is ever included.
