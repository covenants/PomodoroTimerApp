# Privacy policy (draft)

**App:** Orbit Timer (working title) — package `com.covenants.pomodorotimer`
**Effective date:** 9 November 2026
**Status:** Draft for Play Console review. Replace the contact placeholder before publishing.

## Summary

Orbit Timer is an offline-first timer. It does not require an account, does not contain advertising
or analytics SDKs, and does not send your focus history, task titles or preferences to a server.

## Data the app stores on your device

- Timer sessions and stage outcomes (mode, stage, timestamps, active duration, optional task link).
- Tasks you create (title, tag, estimate, completion state).
- Preferences (theme, routine durations, sound/haptics toggles, last used mode).
- Timer state needed to restore a running session.

This data is stored in the app's private Room database and DataStore files. Android backup may copy
it according to the device's backup settings and the manifests in this repository.

## Data the app does not collect

- No name, email, phone number or contact list.
- No location.
- No advertising identifier.
- No usage analytics or crash telemetry.
- No task text, focus history or audio data is transmitted anywhere.
- The app declares no `INTERNET` permission, so the core app has no network access.

## Permissions

- **Notifications** — to show the running timer and completion alert. Optional.
- **Exact alarms** — to request a timely stage-end alert. Optional; the app degrades honestly.
- **Run at boot** — to reconcile and reschedule a session after a reboot. Optional.
- **Vibration** — for the optional completion haptic.

Denying any permission does not prevent the timer from counting down.

## Export and deletion

- **Export:** you can export your local session history as a CSV through Android's system file picker.
  The app does not upload the file; you choose the destination.
- **Delete:** Preferences → Your data → Delete all history removes completed and stopped sessions and
  session events. The active session, tasks and preferences are retained. Uninstalling the app removes
  all app-private data.

## Children

The app is a general productivity tool and is not directed at children. It does not knowingly collect
personal information from anyone.

## Changes

If a future version adds cloud sync, billing verification or analytics, this policy and the Play
Data Safety declaration must be updated before that version ships.

## Contact

Replace this line with a monitored support email before publishing:
`support@example.com`
