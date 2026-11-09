# Changelog

## 1.0.0 — release candidate (2026-11-09)

### Timing and alarms

- Added `USE_EXACT_ALARM` for auto-granted precise alarms on Android 13+, keeping
  `SCHEDULE_EXACT_ALARM` for Android 12/12L.
- Re-reconciles a running timer when the app returns from Android settings after exact-alarm access is
  granted.
- Switched the alarm from `RTC_WAKEUP` to `ELAPSED_REALTIME_WAKEUP`, so wall-clock or network time
  changes cannot move a pending completion.
- Added a foreground-only watchdog; it runs only while a timer is running and the app is visible.
- Notification updates now happen on state changes only; the native chronometer handles the per-second
  display.

### Notifications and sound

- Completion sound now lives on high-importance channels using `AudioAttributes.USAGE_ALARM`; the bell
  channel uses the system alarm sound.
- Added a direct-sound fallback only when notifications are denied, avoiding double sound.
- Added an opt-in full-screen alert with the Android 14+ grant and a Settings link.
- Notification Pause/Resume now reads the active session from Room inside the command lock, so it works
  after Android kills the process. Verified by killing the app, tapping Pause in the shade, and seeing
  `00:37 remaining` paused.

### Data and copy

- Database version 2 with an explicit `1 → 2` migration adding `activeStartedWallMs`.
- Running stopwatch now reconstructs the current run from its wall anchor after a reboot.
- Fixed “1 sessions” and “1 completed focus sessions” pluralization.
- Enabled R8 minification and resource shrinking for release.

### Icon and store assets

- New adaptive launcher icon: midnight gradient background, orbit ring, mint progress arc, central
  planet and four-point star, plus a monochrome themed-icon layer and legacy density PNGs.
- Play icon (512/1024), feature graphic (1024 × 500) and eight framed 1080 × 1920 screenshots in
  `docs/play/`.
- Signed AAB and APK produced with a local upload key; the key is not in git.

### Still not implemented

Google Play Billing, ambient audio packs, home-screen widget, cloud sync, app blocking, lap list,
onboarding, translations, and physical-device OEM testing.
