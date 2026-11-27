# Kairo Plugin & Extension SDK Guide

Kairo supports **standalone, non-launchable Android APK plugins** discovered automatically via `PackageManager` (`com.kairo.agent.PLUGIN`).

## How It Works
Any Android APK can declare an exported `<service>` with the `com.kairo.agent.PLUGIN` intent-filter and `<meta-data>` tags — **no `MAIN` or `LAUNCHER` activity is needed**, so plugins never clutter the user's app drawer.

### 1. Language Pack Plugin (Example: [`chinese-lang-plugin/`](chinese-lang-plugin/))
```xml
<service
    android:name=".ChineseLangPluginService"
    android:exported="true"
    android:label="中文语言包 (Chinese)">
    <intent-filter>
        <action android:name="com.kairo.agent.PLUGIN" />
    </intent-filter>
    <meta-data android:name="kairo.plugin.type" android:value="language" />
    <meta-data android:name="kairo.plugin.name" android:value="中文语言包 (Chinese)" />
    <meta-data android:name="kairo.plugin.description" android:value="Adds Simplified Chinese (zh) UI and AI response language support to Kairo" />
    <meta-data android:name="kairo.plugin.lang_code" android:value="zh" />
</service>
```
When enabled in **Settings → Plugins & Extensions**, Kairo prompts the user to immediately switch both the UI and AI model language to `zh`.

### 2. Theme Plugin (Example: [`oneui-theme-plugin/`](oneui-theme-plugin/))
```xml
<meta-data android:name="kairo.plugin.type" android:value="theme" />
<meta-data android:name="kairo.plugin.name" android:value="One UI Design Theme" />
<meta-data android:name="kairo.plugin.description" android:value="Samsung One UI palette &amp; squircle icons (credits: tribalfs/oneui-design &amp; thatjoshguy67/oneui-icons)" />
<meta-data android:name="kairo.plugin.accent_color" android:value="#0381FE" />
<meta-data android:name="kairo.plugin.bg_color" android:value="#010101" />
<meta-data android:name="kairo.plugin.surface_color" android:value="#17171A" />
<meta-data android:name="kairo.plugin.icon_pack" android:value="oneui-icons" />
<meta-data android:name="kairo.plugin.corner_dp" android:value="22" />
```

One UI plugin 1.2.0 also declares `kairo.plugin.ui_engine=sesl9`. When that
installed plugin is selected, Kairo uses One UI Sans, native SESL9 switches,
progress and search, One UI icons, cards, buttons, chips, dialogs and navigation
styling. Disabling or removing it restores
the stock Material controls immediately. Other plugins do not enable SESL UI. See [One UI integration notes](docs/oneui-integration.md)
for widget blur support and the SESL component assessment.

### 3. Tool / Feature Plugin
```xml
<meta-data android:name="kairo.plugin.type" android:value="tool" />
<meta-data android:name="kairo.plugin.name" android:value="My Custom Tool" />
<meta-data android:name="kairo.plugin.description" android:value="Adds a custom capability to Kairo" />
<meta-data android:name="kairo.plugin.tool_name" android:value="my_tool" />
<meta-data android:name="kairo.plugin.tool_desc" android:value="Describe what the tool does" />
```

## Building the Example Plugins
```bash
./gradlew :oneui-theme-plugin:assembleDebug :chinese-lang-plugin:assembleDebug
```
