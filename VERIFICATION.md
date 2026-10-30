# Verification record

Date: 2026-10-27. Two environments: host/emulator checks (first sections) and a connected
**Unihertz Jelly Star** (EEA build V01.00.04, Android 16 / API 36, 480x854 at 220 dpi, security patch 2026-07-05).
Note: the emulator was set to 320 dpi, which is not the phone's density.

## On the Jelly Star (real device)
The APK on the phone was byte-identical to the built debug APK (matching SHA-256) and had been installed and set as
the default Home app by the owner before testing; I did not change the Home role.
| Check | Result |
|---|---|
| Launcher holds the Home role; stock launcher (`com.android.launcher3`) still installed | Confirmed |
| Home key returns to the launcher from Dialer, Messages, Maps and Settings | Pass |
| Call opens Google Dialer; telecom shows no call | Pass |
| Message opens Google Messages; Navigate opens Google Maps | Pass |
| Unset Tools opens the chooser | Pass |
| Camera shortcut opens the MediaTek camera; Settings shortcut opens Settings | Pass |
| Torch toggle lights the real LED (camera service logged on/off), button state follows, left off | Pass |
| Swipe-up drawer, Setup screen and Apps list render correctly at 220 dpi and font scale 1.2 | Pass |
| Idle: 2 frames in 10 s with launcher visible (Showcase off); drawer open/close: 114 frames, 2.6% janky, p50 10 ms, p90 16 ms | Pass |
| Logcat for launcher errors, crashes, ANRs | None |

Observed, not a defect: the bottom UTILITIES/APPS buttons sit close to the bottom screen edge.

## UI refresh (Space Grotesk, precision dial, chamfered tiles, glyphs, boot-in)
| Check | Result |
|---|---|
| Debug build, 4 unit tests, lint (0 errors) | Pass |
| Emulator at 480x854/220 dpi: Home, drawer, 2.0x font | Pass |
| Boot-in sweep plays on cold start (captured at 10x slow motion on the emulator) | Pass |
| Update installed in place on the Jelly Star; Home role and settings kept | Pass |
| Jelly Star: new Home renders correctly at 78% charging; Call tile opens the dialer and no call was placed; Home key returns; drawer opens | Pass |
| Jelly Star: idle 0 frames in 10 s; no crashes or ANRs in logcat | Pass |
| "Back after more than 10 minutes" replay of the boot-in | Not tested (logic only; needs a real 10 minute gap) |
| Jelly Star at font 2.0x and with Reduce motion after this change | Not tested on the phone |

## Impeccable critique follow-up (legibility, discoverability, consistency)
Critique run 2026-10-27: 30/40 (Good). Fixed: borders and page dots at >=3:1 in every tone, 13 sp text floor, 48 dp page dots with
names, drawn charging bolt (the "+" is gone), single spoken battery description, first-run hint, "Choose app" on the unset Tools tile,
quiet row options and themed icons in the app list, grouped quick-launch drawer with glyphs, clearer names (Quick / Quick launch /
Settings / Launcher setup), sibling labels shrink together, tile glyphs decided once per grid. Left as is by choice: the charging
animation (issue 5) and the fully recoloured red UI.
| Check | Result |
|---|---|
| Build, 9 unit tests (adds palette contrast: edge >= 3:1, text >= 7:1, muted >= 4.5:1 in 7 tones), lint (0 errors) | Pass |
| Accessibility tree (emulator): dial reads "Battery 80 percent, charging" once; dots read "Page 1 of 2" / "Page 2 of 2"; dots 48 dp tall | Pass |
| Emulator: Home with hint, red tone, drawer, app list, Favorites, 2.0x text on Home and drawer | Pass after fixes (inconsistent glyphs, clipped drawer label, legacy-icon backdrop squares were found and fixed) |
| Jelly Star: Home with hint, drawer, app list with real icons (Audible, Bitwarden, Brave, Calculator, Camera, ChatGPT, Clock), Favorites page; no crash; 0 idle frames | Pass |
| Jelly Star: first-run hint flag reset after testing so the hint still shows once | Done |
| Torch state description for TalkBack | In code; not observable via uiautomator, not tested with TalkBack |
| TalkBack itself, Reduce motion, 2.0x text and yellow/red tones on the Jelly Star, 3-button navigation | Not tested |
| Critique snapshot | Left open: issue 5 (charging animation) intentionally unchanged |

## Floating notices (the tip and the Set-as-Home banner no longer shift the layout)
Emulator: Home looks identical in position and size with the tip showing and dismissed; the Set-as-Home banner also overlays without
moving anything. Jelly Star: the tip floats over the dial with the layout unchanged, no crash. Not tested: the exit animation on the
phone, and any notice style beyond these two (the launcher shows no notifications).

## Favorites page (swipe left, six tiles, auto-themed icons)
| Check | Result |
|---|---|
| Build, 7 unit tests, lint (0 errors; one lint false positive on `produceState` avoided by using `LaunchedEffect`) | Pass |
| Emulator: swipe left reaches page 2; page dots show and tapping the second dot switches page; empty tiles show "+ ADD" | Pass |
| Emulator: icons for Chrome, Maps, Photos, Calendar, Clock and Drive redrawn as cyan glyphs; recolour to red live at low battery | Pass |
| Emulator: tile tap launches the app; Home key returns to the main page; Back from page 2 returns to the main page | Pass |
| Emulator: long-press opens the chooser with "Clear this slot"; picking Drive through the UI stores the full component name and returns to page 2 | Pass |
| Jelly Star: both pages render; picked Calculator and Brave (icons redrawn correctly); Calculator tile launched; Home returned to launcher; both favorites cleared again, nothing left stored | Pass |
| Jelly Star: favorites with more icon types (legacy full-bleed icons), the six-tile page full, Reduce motion, 2.0x font | Not tested |
| Known limit: a legacy (non-adaptive) icon with a full-bleed square background is approximated by contrast and may look rougher than adaptive icons | Not evaluated on a real example |

## Battery tone (UI colour follows the battery level)
| Check | Result |
|---|---|
| Build, 7 unit tests (4 clock format + 3 battery tone thresholds), lint (0 errors; one `remember` misuse caught by lint and fixed) | Pass |
| Emulator: 70-75% normal cyan, 30% yellow, 10% and 9% red; borders, panels, glyphs, glow, text tints, drawer and Setup all recolour; text stays legible in red | Pass |
| Emulator: tone changes live while the launcher is open (red to cyan at 75%) | Pass |
| Jelly Star at 95%: normal tone renders, no crash | Pass |
| Jelly Star in yellow and red | Not tested (needs the battery to fall, or a temporary fake level) |
| Initial battery state seeded from the current reading, so no flash of the wrong tone on launch | Implemented; not specifically screenshot-verified |

## Charging animation (third version: neon gradient beams, sonar rings, tick wave, breathing glow)
Beams replaced the earlier hard-edged dashes. Emulator (70% on simulated AC): renders, no crash. Jelly Star at 94% charging:
renders, 11 ms median / 12 ms 90th percentile frame time, 0% janky, no crashes. The gating and frame-count rows below were
measured on the earlier versions; the gating code is unchanged.

### Earlier version notes
| Check | Result |
|---|---|
| Build, 4 unit tests, lint (0 errors) | Pass |
| Emulator, 62% on simulated AC: effect renders, no crash | Pass |
| Emulator frames per 10 s: charging visible 290; launcher behind another app 0; at 100% 0; system animations off 0; on battery 0 (measured with the first, comet version; the gating code is unchanged) | Pass |
| Jelly Star at 92% charging: effect renders; 12 ms median/90th percentile frame time, 0% janky | Pass |
| Jelly Star frame count while animating: about 580 per 10 s, roughly twice the emulator's 290 for the same 33 ms ticker. Cause not identified | Open |
| Jelly Star: gating off the animation (screen off, behind another app, at 100%, Reduce motion) | Not tested on the phone |

## Build / static (host machine)
| Check | Result |
|---|---|
| `./gradlew assembleDebug` | Passed; APK is 18 MB, debug-signed |
| Unit tests (`FormatTest`, 4 tests: 12/24 h, system setting, time zone) | 4 passed |
| Lint (`lintDebug`) | 0 errors. Remaining notes: newer library versions exist; portrait is locked; `allowBackup` advisory |
| Permissions in built APK (`aapt2 dump permissions`) | Only the app-scoped AndroidX signature permission; no internet or other permissions |
| Debug leftovers (`Log.`, `println`, `TODO`) | None found |

## Emulator only: Pixel, Android 15 (API 35), arm64, resized to 480x854 at 320 dpi
| Check | Result |
|---|---|
| Role dialog from "Set as Home app" appears; stock launcher listed as current default | Pass |
| After approving, launcher holds ROLE_HOME; setup banner disappears | Pass |
| Home key from another app returns to the launcher | Pass |
| Home key while a sub-screen/picker is open returns to home screen | Pass |
| Default survives emulator reboot; Home key after reboot lands on launcher | Pass |
| Call tile opens the dialer; telecom shows no call placed | Pass |
| Unset Tools tile opens the chooser; choosing Calendar then launches Calendar | Pass |
| Missing app (fake component injected) shows the recovery dialog | Pass |
| Hide app: leaves list, "Show hidden (1)" appears, app still installed and not disabled | Pass |
| Torch toggle switches the emulator's virtual camera torch on and off | Pass |
| Drawer swipe-up and button; Settings, app list, picker reachable | Pass |
| Font scale 2.0: Home, drawer and Setup stay readable (labels shrink to fit; Setup and lists scroll). Home screen text is capped at 1.3x by design | Pass after fixes |
| Reduced motion: with system animation scale 0 the drawer appears with no slide | Observed; the "system animations are off" note text was not confirmed on screen |
| Idle rendering: 0 frames/10 s (Showcase off), about 145 frames/10 s (Showcase on), 0 frames/10 s when launcher is behind another app | Pass |
| Logcat during the session | No crashes, ANRs or app errors; only system-server housekeeping warnings |

## Not tested
- **Reboot persistence on the Jelly Star** (only confirmed on the emulator; rebooting the phone needs the owner's PIN and consent)
- Selecting the Home role from scratch on the Jelly Star (it was already selected); the role dialog on its OEM build
- 3-button vs gesture navigation behaviour; the physical programmable key
- Large font (2.0) and Reduce-motion/Showcase on the Jelly Star (only on the emulator)
- Battery drain over hours (the phone was charging; a short idle-frame count says little about power use)
- Real Notes/Music apps; the hide/show flow on the phone
- Release (minified) build; landscape is locked out by design

## Wallpaper, notification center and built-in tools

| Check | Result |
|---|---|
| Unit tests: calculator (chaining, decimals, divide by zero, editing, limits), stopwatch format, relative time. 17 tests in all | Pass (after fixing a real bug: 20 showed as 2E+1) |
| Lint: 0 errors | Pass |
| Emulator: notification screen shows the access card before access; bell hidden; after granting access with `cmd notification allow_listener`, posted notifications list, count badge shows, dismiss and Clear work, ongoing items stay dimmed with no dismiss | Pass |
| Emulator: Tools hub, Calculator (12 x 3 = 36), Stopwatch (start, laps, leave and return while running), Notes (create, auto-save, list) | Pass |
| Emulator: Setup > Set live wallpaper opens the system preview showing the wallpaper; applied to home and lock; Setup reports it active | Pass |
| Jelly Star: Home, Tools tile, Calculator, Notifications access card render correctly; the system access page opens | Pass |
| Jelly Star: Android greys out the access switch as a *restricted setting* for this sideloaded build, so the user must allow restricted settings first (the card explains and links to App info) | Observed |

Not tested: notification access actually granted on the Jelly Star (it needs the owner's own consent); tapping a notification
to open its app; the wallpaper on the Jelly Star or on a real lock screen (the emulator's power key opened the Assistant
instead of the lock screen); Notes with a large font or a long note; Stopwatch after a reboot.

## Voice memos

| Check | Result |
|---|---|
| Unit test: duration formatting (18 tests in all pass; lint 0 errors) | Pass |
| Emulator: Voice tab; REC asks Android's permission; Don't allow shows the explanation and an App info button | Pass |
| Emulator: Allow, record with live timer, STOP saves an AAC file in private storage, list shows date and length | Pass |
| Emulator: Play, Pause, position counter; second memo; delete asks to confirm and removes the file | Pass |
| Emulator: Home pressed mid-recording stops and saves it (no background recording) | Pass |
| Emulator lock screen shows the Minimalist Launcher wallpaper behind the clock | Pass (this also closes the earlier lock-screen gap for the emulator) |

Not tested: recording on the Jelly Star (the microphone permission is yours to grant); playback sound (the emulator has no
audio output here, so only position and state were checked); another app holding the microphone; a very long recording.

Live sound waves while recording: the drawing was checked with simulated levels in a throwaway build (since removed; the shipped
APK was rebuilt from the real code). With real sound it is untested: the emulator's microphone is silent (it showed the idle
baseline), and the Jelly Star recording needs the owner's microphone permission.

## Small-phone pass (bigger text and targets)

Type scale raised one step (caption 14, label 16, body 18, title 20 sp); every button is at least 56 dp tall, list rows 56-76 dp,
primary actions (Record, New note, Stopwatch controls) are 80 dp thumb-sized buttons at the bottom of their screens. Every screen was
screenshotted on the Jelly Star (Home, Favorites, drawer, Apps, Setup, Notifications access card, Tools, Calculator, Stopwatch idle and
running, Notes, Voice, note editor) and fixed where it wrapped or sat off the margins. Not re-tested after this pass: system font scale
above 1.0 on the phone, and the emulator-only screens (a granted notification list, a recording in progress).

## Unlock transition

Removed from the app. The live wallpaper remains and is still a single still frame.
