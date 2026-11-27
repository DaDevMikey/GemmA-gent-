# Kairo Chinese Language Pack Plugin (`com.kairo.plugin.zh`)

Reference example of a **standalone, non-launchable Kairo Language Pack APK**.

- Exposes `ChineseLangPluginService` with `<action android:name="com.kairo.agent.PLUGIN" />`
- Sets `kairo.plugin.type = "language"` and `kairo.plugin.lang_code = "zh"`
- Appears in **Kairo → Settings → Plugins & Extensions** with an instant **"Use Now"** prompt to switch both UI & AI responses to Simplified Chinese (`中文`).

See [PLUGINS.md](../PLUGINS.md) for the full Kairo Plugin specification.
