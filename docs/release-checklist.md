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

- [ ] Create an upload keystore (never commit it).
- [ ] Add a `release` signing config fed from environment variables or `~/.gradle/gradle.properties`.
- [ ] Confirm `applicationId` and version code/name for the release train.
- [ ] Build `bundleRelease` and verify the `.aab` with `bundletool`.
- [ ] Keep the mapping file if minification is enabled.

Example (do not commit secrets):

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("ORBIT_KEYSTORE") ?: "missing.jks")
        storePassword = System.getenv("ORBIT_STORE_PASSWORD")
        keyAlias = System.getenv("ORBIT_KEY_ALIAS")
        keyPassword = System.getenv("ORBIT_KEY_PASSWORD")
    }
}
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
- [ ] TalkBack, 200% font, high contrast, reduced motion.
- [ ] Doze / app standby.
- [ ] Delete history and verify it is gone.
- [ ] Export CSV through SAF and open the file.

## 4. Play Console

- [ ] App content questionnaire (ads: no; news: no; health: no).
- [ ] Data Safety form matches `docs/privacy-policy.md`.
- [ ] Permissions declaration for exact alarms, if submitted.
- [ ] Target API level meets the current Play requirement.
- [ ] Content rating questionnaire.
- [ ] Store listing text, screenshots, feature graphic and icon.
- [ ] Internal testing → closed testing → staged production.
- [ ] Pre-launch report reviewed; fix crashes/ANRs.
- [ ] Billing: do not enable Plus until Google Play Billing is implemented and tested.

## 5. Release artifacts

- [ ] Signed AAB.
- [ ] Versioned changelog.
- [ ] Screenshots captured from the release build.
- [ ] Privacy policy URL.
- [ ] Support email.
- [ ] Rollback plan and staged rollout percentage.

## 6. Post-launch

- [ ] Monitor crash-free sessions and ANR rate.
- [ ] Review notification-opt-out and session-abandonment signals.
- [ ] Do not add analytics or cloud sync without updating the privacy policy and Data Safety form.
