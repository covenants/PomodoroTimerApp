# Release checklist

This build is a verified debug artifact, not a production-signed release. Complete every item before
uploading to Google Play.

## 1. Brand and legal

- [ ] Clear the final app name (trademark, Play search, domain, handle).
- [ ] Replace `app_name` and store title.
- [ ] Replace the privacy-policy contact placeholder and host a public URL.
- [ ] Review the supplied reference package for any third-party marks; do not copy competitor art.
- [ ] Add an app attribution / open-source notices screen if required by dependencies.

## 2. Signing and build

- [x] Create an upload keystore (never commit it).
- [x] Add a `release` signing config fed from the local, git-ignored `keystore.properties`.
- [x] Confirm `applicationId` and version code/name for the release train.
- [x] Build `bundleRelease` and `assembleRelease`.
- [ ] Keep the mapping file if minification is enabled (R8 is enabled; `app/build/outputs/mapping/release/`).
- [x] Back up the keystore and `keystore.properties` in a secure vault.
  Local backup created at `C:\Users\mmkha\OrbitTimer-signing-backup.zip`; move it off this machine.

Local release key (do not commit):

```text
C:\Users\mmkha\.android\keystores\orbit-release.jks
```

Local password file (do not commit):

```text
D:\repos\PomodoroTimerApp\keystore.properties
```

Example signing config (already wired in `app/build.gradle.kts`):

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(properties["storeFile"])
        storePassword = properties["storePassword"]
        keyAlias = properties["keyAlias"]
        keyPassword = properties["keyPassword"]
    }
}
```

Build:

```bash
./gradlew --no-daemon bundleRelease assembleRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
apksigner verify app/build/outputs/apk/release/app-release.apk
```

## 3. Device and policy tests

- [ ] Pixel-class device on the current target API.
- [ ] Samsung One UI device.
- [ ] Xiaomi/Redmi or another aggressive battery manager.
- [ ] API 26/27 minimum-SDK device or emulator.
- [ ] Fresh install → start focus → screen off → wait → notification.
- [ ] Deny notifications → timer still accurate → honest UI.
- [ ] Deny exact alarms → inexact fallback → honest UI.
- [ ] Reboot during a running session → reconcile.
- [ ] Rotate/recreate Activity repeatedly.
- [ ] Tap a notification Pause/Resume action after the process has been killed.
- [ ] Change the wall clock while an alarm is pending → completion remains on time.
- [ ] Full-screen alert on a locked device with the Android 14+ grant.
- [ ] TalkBack, 200% font, high contrast, reduced motion.
- [ ] Doze / app standby.
- [ ] Delete history and verify it is gone.
- [ ] Export CSV through SAF and open the file.

## 4. Play Console

- [ ] App content questionnaire (ads: no; news: no; health: no).
- [ ] Data Safety form matches `docs/privacy-policy.md`.
- [ ] **Exact-alarm declaration is mandatory** because `USE_EXACT_ALARM` is declared. Explain that
      precise stage-end alerts are the core function of the app and that the app falls back when access
      is unavailable.
- [ ] **Full-screen-intent declaration is mandatory** because `USE_FULL_SCREEN_INTENT` is declared.
      Explain that it is an opt-in, user-visible completion alert for a timer; it is off by default.
- [ ] Target API level meets the current Play requirement.
- [ ] Content rating questionnaire.
- [ ] Store listing text, screenshots, feature graphic and icon.
- [ ] Internal testing → closed testing → staged production.
- [ ] Pre-launch report reviewed; fix crashes/ANRs.
- [ ] Billing: do not enable Plus until Google Play Billing is implemented and tested.

## 5. Release artifacts

- [x] Signed AAB: `app/build/outputs/bundle/release/app-release.aab`.
- [x] Signed APK for smoke testing: `app/build/outputs/apk/release/app-release.apk`.
- [x] Play icon, feature graphic and eight 1080 × 1920 screenshots in `docs/play/`.
- [ ] Versioned changelog.
- [ ] Screenshots re-captured from the final release build.
- [ ] Privacy policy URL.
- [ ] Support email.
- [ ] Rollback plan and staged rollout percentage.

## 6. Post-launch

- [ ] Monitor crash-free sessions and ANR rate.
- [ ] Review notification-opt-out and session-abandonment signals.
- [ ] Do not add analytics or cloud sync without updating the privacy policy and Data Safety form.
