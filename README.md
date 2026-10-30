# Minimalist Launcher

A minimalist Android home screen for the Unihertz Jelly Star: near-black background, restrained cyan accent,
a large clock with a thin battery arc, and four big controls (Call, Message, Navigate, Tools).

It is a normal Home app (launcher). It keeps the stock operating system and the stock launcher in place.
No root, no firmware changes.

## What it is and is not

It changes the **home screen, app drawer and its own screens** (notification center, built-in tools). It does **not**
replace the lock screen, the notification shade or system settings, and it does **not** remove Android's or the
manufacturer's telemetry. An optional live wallpaper gives the lock screen the same look.

### Notification center (opt-in)
Tap the bell on Home; the list slides down from the top. It stays off until you switch on **notification access** for Minimalist Launcher in
Android's own settings; nothing is read before that. Notifications are held in memory only, never saved, and there is
no internet permission, so they cannot be sent anywhere. You can dismiss one, clear all, or tap to open it. Ongoing items
(players, USB) are shown dimmed and are not counted. On a sideloaded install Android greys the switch out as a
*restricted setting*: open App info, tap the three dots, choose **Allow restricted settings**, then try again.

### Built-in tools
The **Tools** tile opens Calculator, Stopwatch and Notes (the **Notes** shortcut opens Notes directly). They work offline
and store data in the app's private storage only. Notes has two tabs: text notes, and **Voice** memos (record, play, pause,
delete with a confirm) and a big RECORD/STOP button at the bottom of the screen, with live glowing sound waves while recording. Voice memos use the microphone only while you are on that tab with the screen on: leaving the
screen or the launcher stops and saves the recording. They are AAC files in private storage, not backed up, and not
transcribed (that would need the internet). The first RECORD asks Android for microphone permission; if you say no, nothing
is recorded. Hold a tile and choose an app to use another app instead.
The calculator works left to right like a desk calculator (2 + 3 × 4 = 20). The stopwatch keeps running when you leave
it. There is no timer or alarm: a reliable one would need alarm permissions this app does not request.

### Live wallpaper
Setup > Wallpaper > **Set live wallpaper** opens Android's own confirmation. It is one still frame in the launcher's
look and battery colour, redrawn only when it becomes visible or the battery tone changes. It does not animate.

## Features

- Battery tone: the whole UI (accent, borders, panels, text tints, glow) follows the battery level. Your chosen accent is
  used above 40%; yellow at 40% or below; red at 15% or below. It fades between tones (instant when motion is reduced).
  Thresholds are `CRITICAL_AT_PERCENT` / `MEDIUM_AT_PERCENT` in `Battery.kt`.
- Home: clock + date inside a precision dial: battery arc with 5% graduations, a glowing leading edge and a soft inner light
  ("+" while charging). Tiles have chamfered corners and thin-line glyphs.
- Charging animation (only while charging below 100%, screen on, motion enabled): soft neon energy beams (bright head, fading
  tail, additive bloom) flow along the charged arc, two sonar rings radiate from the leading edge, a wave of light runs across the unlit graduations toward
  100%, and the glow behind the clock breathes. Draws about 30 times a second while it runs; nothing runs otherwise.
- A short boot-in (dial sweep, graduations lighting, tiles fading up) plays on first launch and after you have been
  away for more than 10 minutes. It is skipped when motion is reduced.
- Favorites page: swipe left (or tap the page dots) for six more tiles. Tap an empty tile to choose an app; hold a tile to
  change or clear it. Each app's icon is redrawn automatically as a single-colour glyph in the current theme colour (it uses
  the app's own monochrome icon layer on Android 13+, otherwise its foreground shape), and follows the battery tone.
  Home or Back returns to the main page.
- First run: once the launcher is your Home app, a one-time hint explains the two gestures (swipe left, hold a tile). It goes
  away with **Got it** or when you open the Favorites page. Notices like this (and the **Set as Home app** banner) float over the
  screen: the dial and tiles never resize or move when one appears or goes away. A tile with no app yet (Tools) says **Choose app**.
- Legibility: component edges and inactive indicators are solved per tone to stay at least 3:1 against the background
  (cyan, yellow and red alike), text never goes below 13 sp, and the Home screen scales text up to 1.5x (lists and Setup follow
  the full system size).
- Four tiles open apps you choose. Unset Call/Message/Navigate use the system default
  (Call opens the dialer with no number, so it never places a call). Tools has no default and asks you to choose.
  Long-press a tile to change its app.
- Quick-launch drawer: swipe up, or tap **QUICK**. Camera, Music, Notes, Torch, then Settings (the phone's) and All apps, with **Launcher setup** as a quiet link.
- **Apps**: full app list with search. `···` per app: App info, Hide/Show. Hiding only removes an app
  from this list; it is not disabled or uninstalled.
- App list: themed icons, search, and a quiet options control on each row (long-press also works).
- Setup: Home-app status, shortcut choices, accent colour, reduce motion, haptics, Showcase mode, clock format.
- Showcase mode (off by default): day-progress ring and seconds marker. Redraws about 15 times a second while the
  launcher is visible, and not at all when it is not.
- Motion respects the system animation scale (0 = reduced) and has its own Reduce-motion switch.

## Requirements and assumptions

- minSdk 29 (Android 10), because that is the first release with the Home role dialog.
- Tested on a Unihertz Jelly Star (EEA build V01.00.04) running **Android 16 (API 36)**, 480x854 at 220 dpi,
  security patch 2026-07-05. See `VERIFICATION.md`.
- Build: JDK 17, Android SDK platform 35 + build-tools. The first build downloads Gradle and AndroidX from
  Google/Maven (build time only; the app itself has no internet permission).

## Build and install

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

A prebuilt debug APK is also at `MinimalistLauncher-debug.apk` (debug-signed; not a store build).
Without adb: copy the APK to the phone and open it, allowing "install unknown apps" for your file manager.

Unit tests and lint: `./gradlew testDebugUnitTest lintDebug`

## Make it your Home app

1. Open **Minimalist Launcher** from the app list (it also has a normal launcher icon).
2. Tap **SET AS HOME APP**. Android shows its own dialog. Pick Minimalist Launcher and confirm. Nothing changes until you do.
3. If the dialog does not appear on your phone, use Setup > **CHANGE HOME APP IN SYSTEM SETTINGS**
   (Settings > Apps > Default apps > Home app).

## Recovery: going back to the stock launcher

Minimalist Launcher never removes or disables the original launcher. Any one of these works:

1. Minimalist Launcher > Quick > Launcher setup > **CHANGE HOME APP IN SYSTEM SETTINGS**, then choose the original.
2. Phone Settings > Apps > Default apps > Home app (wording varies by manufacturer).
3. Uninstall Minimalist Launcher (Settings > Apps). Android falls back to the remaining launcher.
4. From a computer with adb: `adb uninstall com.minimalist.launcher`.

If a tile points at an app that was removed, tapping it shows a dialog with **Choose app**, **Use system default**
or **Dismiss**.

## Permissions

Two permissions are declared. **`RECORD_AUDIO`**, for voice memos, is a runtime permission, so nothing can record until you
approve Android's prompt, and you can revoke it any time. **`VIBRATE`** is a normal permission with no prompt; it is used
only for the long strong vibration in the clock easter egg (ordinary tap feedback does not need it). There is no internet, accessibility, device admin, overlay or
`QUERY_ALL_PACKAGES` (a narrow `<queries>` entry for launcher apps is used instead).
**Notification access** is the one special access the app can use, and only if you switch it on yourself in system
settings (it is not a manifest permission, and Android shows its own warning). Without it the notification center is inert.
The merged APK also contains one AndroidX-generated, signature-level permission scoped to this app's own package
(`...DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`); other apps cannot be granted it and it shows no prompt.
Backup and device-transfer are disabled. Settings live in the app's private storage.

See `PRIVACY_CHECKLIST.md` for phone-side settings, and `VERIFICATION.md` for what was and was not tested.

## Credits

Type: Space Grotesk (c) The Space Grotesk Project Authors, SIL Open Font License 1.1 (`FONT_LICENSE_OFL.txt`), bundled in `res/font`.

## Code map

`app/src/main/java/com/minimalist/launcher/`
- `MainActivity.kt` - Home role request, Home-intent handling (`singleTask`, `onNewIntent` returns to home)
- `LauncherViewModel.kt` - screen state, app list, role state, notification-access state
- `NotificationHub.kt` - the opt-in notification listener and its in-memory store
- `PocketWallpaperService.kt` - the still live wallpaper
- `Calculator.kt`, `Notes.kt` - built-in tools' logic (calculator engine, notes store, stopwatch state; unit-tested)
- `Prefs.kt`, `Apps.kt`, `Format.kt` - settings, app discovery/launching, clock formatting (unit-tested)
- `IconMask.kt` - turns any app icon into a tintable mask (cached)
- `ui/` - Compose screens (`HomePager.kt` holds the two home pages and the themed favorite tiles): Home, Utility drawer, App list/picker, Setup; `Glyphs.kt` draws the tile icons; `Components.kt` holds the font, shapes and buttons
