# ADR-002 — Alarm and foreground-service policy

- Status: Accepted
- Date: 2026-11-09

## Context

Android does not guarantee a notification at the exact end of a timer. Doze, OEM battery managers,
the Android 12+ exact-alarm access gate and the Android 13+ notification permission all affect
delivery. The brief requires honest behavior when access is denied and forbids an unconditional
foreground service.

## Decision

1. Schedule **one** `AlarmManager` alert for the next stage end. Do not poll in the background.
2. Use `setExactAndAllowWhileIdle` only when `canScheduleExactAlarms()` returns true (or pre-API 31).
   Otherwise use `setAndAllowWhileIdle` and state that alerts may be delayed.
3. Never use a persistent foreground service for timing. The app uses an ongoing notification
   without a service, plus a cheap in-process watchdog while the app is alive.
4. Channels:
   - `active_timer` — low importance, silent, no badge.
   - `session_alerts` — high importance, no channel sound; sound/vibration are played by the app so
     user toggles remain truthful and work when notifications are denied.
5. Request `POST_NOTIFICATIONS` contextually on the first timer start, not at launch.
6. `SCHEDULE_EXACT_ALARM` is declared with a precise-timer justification. `USE_EXACT_ALARM` is not
   used. The user can open the system special-access screen from Settings or the in-session warning.
7. `BootReceiver` reconciles state and reschedules after `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.
8. Permission denial never crashes or traps the user. The countdown remains correct and the UI
   explains the alert limitation.
9. No `AccessibilityService`, `QUERY_ALL_PACKAGES`, usage-stats, contacts, location, broad storage or
   app-blocking permissions in this release.

## Consequences

- Completion may be late on aggressive OEM builds. The UI does not promise otherwise.
- Force-stopping the app blocks receivers until the user opens it again.
- There is no fake “guaranteed alarm” claim in store copy.
