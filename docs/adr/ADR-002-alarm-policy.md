# ADR-002 — Alarm, notification and foreground-service policy

- Status: Accepted
- Date: 2026-11-09 (updated for release candidate)

## Context

Android does not guarantee a notification at the exact end of a timer. Doze, OEM battery managers,
the Android 12+ exact-alarm access gate and the Android 13+ notification permission all affect
delivery. The app must be honest when access is denied and must not use an unconditional foreground
service.

## Decision

1. Schedule **one** `AlarmManager` alert for the next stage end. Do not poll in the background.
2. Use `ELAPSED_REALTIME_WAKEUP`, not `RTC_WAKEUP`. The trigger is
   `elapsedRealtime() + remaining`, so manual or network wall-clock changes cannot move the alarm.
   Rebooting clears elapsed-realtime alarms; `BootReceiver` reconstructs the remaining time from the
   persisted wall fallback and reschedules.
3. Declare both `USE_EXACT_ALARM` and `SCHEDULE_EXACT_ALARM`:
   - `USE_EXACT_ALARM` is auto-granted on Android 13+ for apps whose core function is a timer/alarm.
   - `SCHEDULE_EXACT_ALARM` remains the user-granted path on Android 12/12L and is capped with
     `android:maxSdkVersion="32"`, matching Google's recommended combination.
   - Always check `canScheduleExactAlarms()` and fall back to `setAndAllowWhileIdle` if precise access
     is unavailable.
4. When the user returns from Android settings after granting exact-alarm access, the app re-runs
   reconciliation so a running timer is rescheduled on the exact path immediately.
5. Never use a persistent foreground service for timing. The app uses an ongoing notification
   without a service, plus a foreground-only watchdog while the app is visible.
6. Notification channels:
   - `active_timer` — low importance, silent, no badge.
   - `session_alerts_v2_silent`, `session_alerts_v2_gentle`, `session_alerts_v2_chime`,
     `session_alerts_v2_bell` — high importance. The three audible channels use bundled WAV resources
     (`gentle_chime.wav`, `chime.wav`, `bell.wav`) on `AudioAttributes.USAGE_ALARM`, so Gentle, Chime
     and Bell are genuinely different. The `v2` IDs force Android to pick up the new sounds after an
     upgrade, since channel settings are immutable once created.
   - If notifications are denied, the app plays the same bundled resource directly and still runs the
     optional haptic. It does not play both channel and fallback sound when the notification is posted.
7. Full-screen alert is an **opt-in** setting. When enabled and `NotificationManager.canUseFullScreenIntent()`
   allows it, the completion notification uses a full-screen intent that launches `MainActivity` with
   `setShowWhenLocked`/`setTurnScreenOn`. On Android 14+ the user can grant or revoke this access; the
   Settings screen links to the system page. Without the grant the high-priority notification still
   appears.
8. Request `POST_NOTIFICATIONS` contextually on the first timer start, not at launch.
9. `BootReceiver` reconciles state and reschedules after `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.
10. Permission denial never crashes or traps the user. The countdown remains correct and the UI
    explains the alert limitation.
11. No `AccessibilityService`, `QUERY_ALL_PACKAGES`, usage-stats, contacts, location, broad storage or
    app-blocking permissions in this release.

## Consequences

- Completion may still be late on aggressive OEM builds; the UI does not promise otherwise.
- Force-stopping the app blocks receivers until the user opens it again.
- Full-screen alerts are subject to Android 14+ policy and user consent.
- There is no fake “guaranteed alarm” claim in store copy.
