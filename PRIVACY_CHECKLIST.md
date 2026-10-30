# Privacy setup checklist (phone settings)

Minimalist Launcher only handles the home screen. These are separate choices in Android itself.
None of them is changed automatically; menu names vary a little by manufacturer.

## 1. App permissions
Settings > Apps (or Privacy > Permission manager). Review each app's Location, Microphone, Camera, Contacts, SMS,
Phone and "Nearby devices".
- **Revoke** what an app does not need: it loses that feature, and some apps then ask again or stop working.
- Choose **While using the app** for location where offered; background tracking stops, but navigation or
  weather widgets may update less.
- Android removes permissions from apps you do not use for a few months (auto-reset); keep it on unless an app breaks.
- Minimalist Launcher asks for none, so there is nothing to review for it.

## 2. Account syncing
Settings > Passwords & accounts (or Accounts).
- Removing an account, or turning off sync for Contacts/Calendar/Photos, stops copies going to that provider.
  It also stops backup and cross-device sync, so you need another way to keep that data.
- Keep a Google account only if you need the Play Store, mail or Maps; many apps and security updates rely on it.
- Minimalist Launcher has no accounts, no cloud sync and no backup.

### Microphone
Minimalist Launcher asks for the microphone only for Notes > Voice. Under Settings > Apps > Minimalist Launcher > Permissions you can
set it to **Ask every time** or **Don't allow**. It records only while that tab is open and the screen is on, and the
audio never leaves the phone (no internet permission). Android also shows its green microphone indicator while it records.

## 3. Lock-screen notification visibility
Settings > Notifications > Notifications on lock screen (or Display > Lock screen).
- **Hide sensitive content** keeps notifications arriving but hides message text and codes from anyone holding the phone.
- **Don't show notifications at all** is the most private, but you will not see calls or messages without unlocking.
- Per app, you can hide just messaging and banking apps instead of everything.
- Minimalist Launcher reads notifications **only if you turn on notification access** (Settings > Notifications > Device & app
  notifications, or the button in the launcher). It shows them in its own screen, in memory only, and cannot send them
  anywhere (no internet permission). Turn the access off in the same place at any time. This lock-screen setting is
  unaffected by it.

## 4. Security updates
Settings > About phone > Android version > Security update (and System update).
- Check the **security patch level date**. If it is many months old, the phone is missing fixes.
- Unihertz lists Android 13 for the Jelly Star and says further OS updates are not guaranteed. Check the maker's
  site for patch releases and install them when offered (updates need a charged battery and some free storage).
- An unpatched phone is more exposed to known vulnerabilities. Mitigations are to install apps only from the Play Store,
  keep the apps themselves updated, and avoid unknown links and sideloaded APKs (including this debug one, once you trust it).
- Sideloading Minimalist Launcher requires allowing "install unknown apps" for one app. Turn that permission off afterwards.

## What this launcher cannot change
Lock screen, notification shade, system settings, preinstalled apps, and Android/manufacturer telemetry
(Settings > Privacy > Usage & diagnostics, ads personalisation) are controlled only in system settings.
Turning those off can reduce data sharing but may limit crash reporting, bug fixes or personalised features.
