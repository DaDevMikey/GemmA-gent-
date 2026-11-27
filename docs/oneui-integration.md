# One UI widgets and SESL integration

## Widget wallpaper blur

Both Quick Ask and Cover Screen widgets declare Samsung's `colorful` style and
their actual size class (`wideSmall` for the 4×1 widget, `small` for the 2×1 widget).
Their outer `FrameLayout` uses `@android:id/background`. The content retains its
own IDs and all existing ask, voice and chat PendingIntents.

On devices advertising `com.samsung.feature.samsung_experience_mobile`, updates
set the outer background to a dark `ColorDrawable` with alpha 153/255. This is
the tint One UI Home reads when rendering wallpaper blur behind the widget.
The content layer has no background to obscure that blur. Other devices retain
the existing rounded dark drawable. Widget option changes reapply the background
and click actions.

The launcher owns the effect. This does not blur text or icons, capture wallpaper
in Kairo, use hidden APIs, or require SESL dependencies. It is independent of
whether the One UI theme plugin is enabled. The reference implementation targets
One UI 7+; the Samsung feature check identifies a candidate device, not host blur
capability. Samsung third-party launchers and cover/keyguard hosts may display
only the translucent tint. The existing keyguard category is retained; it does
not establish Samsung ServiceBox integration.

Reference: [One UI Blur Integration](https://github.com/thatjoshguy67/blur-widget-demo/wiki/One-UI-Blur-Integration).

Device acceptance checks still needed:

- Add both widgets to One UI Home over a detailed wallpaper and verify that the
  wallpaper is blurred while labels and icons remain sharp.
- Resize, rotate, restart the launcher, and verify ask, voice and chat actions.
- Check light/dark wallpaper, launcher transparency settings and reduced-effects
  accessibility settings for legibility.
- Check the actual cover host separately; home-screen blur does not prove cover
  or lock-screen support. Check another launcher for its fallback appearance.

## Plugin-gated SESL9 controls

One UI plugin 1.1.0 declares `kairo.plugin.ui_engine=sesl9`. Kairo activates native
controls only when that installed theme is selected, its type is `theme`, and
its package is `com.kairo.plugin.oneui`. A palette or icon-pack hint cannot enable
SESL controls. Missing, disabled, legacy, language and unrelated theme plugins
use the existing Compose Material controls.

Implemented components in plugin 1.2.0 / host 1.3.2-dev-sesl9:

- Native SESL9 switches, circular spinners and determinate progress bars.
- Native SESL9 conversation search, with query synchronization, clear/submit
  behaviour and detached-listener cleanup. The stock drawer keeps its existing
  Compose search field.
- One UI Sans variable typography at real 300/400/500/600/700 weights, reusing
  Twidget's font asset. Theme roles, assistant prose, chat input and native search
  use the font; code blocks remain monospace.
- One UI shapes and neutral surface colours, 22dp cards, subtle card/capsule
  outlines, 48dp buttons, pill filter chips, rounded dialogs and modal sheets.
  Error, warning and success colours are preserved.
- A collapsing Settings header using Compose's nested-scroll app bar, card-style
  toggle rows, rounded drawer chrome and capsule navigation rows. The plugin
  drawer adds direct Settings, Memory and Connections routes using the existing
  callbacks and sheet/navigation state.
- One UI icons for all 32 current Material icon names, including assistant marks,
  input actions, navigation, setup, response-copy and overlay controls. Back icons
  retain the icon library's RTL mirroring and descriptions remain at call sites.
- Twidget's spinner animation cleanup when hidden or detached.

Native SESL widgets receive a local themed context. Larger layouts remain in
Compose: cards, buttons, chips, dialogs and the collapsing header are adaptations
using One UI typography/shape/surface tokens, not claims that Compose controls
became native SESL View classes. Kairo's back stack, callbacks, selection,
permissions and data handling stay in place. No unused tabs, slider settings or
picker screens were added solely to showcase components.

All appearance changes use the existing selected/installed One UI plugin gate.
The default branches retain their original fonts, shapes, colours and Material
controls. Observers restore that branch on disable/removal. The metadata plugin
does not execute external code inside Kairo.

## Asset provenance

Eight SVGs were imported from the user's
[OneUI-Icons repository](https://github.com/SadLinusGuy0/oneui-icons/tree/5a561c9d1d4be4b53602a440dd094df84cfdb60b)
at revision `5a561c9d1d4be4b53602a440dd094df84cfdb60b`: settings, edit, close,
voice, send, assistant, security/privacy and voice-wakeup. They were converted to
Android vectors with original paths, fill/stroke rules and centred aspect ratios.
The remaining icons use `io.github.oneuiproject:icons:1.1.0`, also used by Twidget.
Source MIT notices are bundled under `assets/licenses/`. One UI Sans is reused
from Twidget's `res/font/one_ui_sans.ttf`.

## Build and verification

The matched SESL9 revisions are reused from Twidget. Only AppCompat/Core and
their required dependencies are requested; constraints keep any transitive SESL
modules in the same family. Stock AndroidX artifacts with overlapping class
names are excluded, as required by SESL. Compose Material stays in place.

SESL9 requires compile SDK 37 and carries Java 24 bytecode. The build uses
Twidget's compatible AGP 9.4.1 / Gradle 9.8 toolchain, while target SDK 36 and
minimum SDK 26 remain unchanged. Run Gradle with JDK 27 (the source toolchain is
still JDK 17). GitHub Packages credentials come from `GH_PACKAGES_USER` /
`GH_PACKAGES_TOKEN`, with a local fallback to
`~/.config/kairo/github.properties` (or the existing Twidget credential file at
`~/.config/twidget/github.properties`). No credentials are stored in this repo.
CI must supply those two secrets with package-read access.

`PluginUiGateTest` covers capability/type/package gating. `PluginIconTest` covers
all current icon names, disabled-theme bypass and unknown-icon fallback.
`PluginAppearanceTest` exercises typography/shape/surface restoration. `PluginControlsTest`
checks real installed-plugin discovery, disabled and missing-plugin fallback,
native View creation/removal, determinate progress, and callback synchronization.
Install the updated One UI plugin on the test device before running it.

Sources checked for this assessment:

- [SESL AndroidX modules and integration](https://github.com/tribalfs/sesl-androidx)
- [SESL Material components](https://github.com/tribalfs/sesl-material-components-android)
- [One UI Design library](https://github.com/tribalfs/oneui-design)
- [Android Views in Compose](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)

The locally cached One UI Design 0.9.13+oneui8 AAR also contains the named
`dev.oneuiproject.oneui` classes. This establishes their availability in that
artifact, not runtime compatibility with Kairo or a newer SESL family.

Validation for the expanded SESL9 implementation: both debug APKs and the Android
instrumentation APK build, and all seventeen JVM tests pass. Lint still reports the
three existing errors in voice-session API gating, German string formatting and
the telephony feature declaration. Instrumentation and visual checks were not
run: the user requested installation only and will test on the phone.

## Upstream merge and landing reveal

The local branch includes upstream commits `86962d4` (six-step setup), `70595a7`
(dedicated About screen) and `97955cc` (optional startup update checks, disabled
by default). Local plugin components remain in place; About uses the same gated
icon/surface adapters.

The landing greeting now ports Twidget's `AnimatedTitleView` reveal in all
themes: seven-frame letter staggering, a 500ms fade, 1000ms horizontal settle
and blur-to-sharp rendering. The current theme supplies its font and colour.
Android 12+ uses hardware blur; earlier supported Android versions use cached
software blur masks. It respects disabled system animators, cancels detached
animations, and exposes one accessible text label. Grapheme boundaries preserve
emoji sequences; settled text is drawn using the full shaped layout.

Drawer icons use monochrome glyphs rather than tinting coloured category badge
backgrounds, and navigation rows have separation. Both widget variants swap the
assistant and microphone artwork with the theme, refresh immediately on theme
selection, and refresh after Kairo upgrades or One UI plugin removal.

All six onboarding stage headings, including the initial welcome and completion
heading, share this same reveal in every theme. Forward/back navigation creates
a fresh reveal, while the current theme supplies the original heading typography.
