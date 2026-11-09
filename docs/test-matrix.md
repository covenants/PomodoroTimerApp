# Test matrix and executed evidence

Date: 2026-11-09. Environment: Windows 11, JDK 21 (Android Studio JBR), Gradle 8.14.3,
Android SDK 36, emulator API 36 (1080 × 2400, 420 dpi). Debug and signed release builds.

Abbreviations: **PASS** = executed and observed; **UNIT** = proven by a JVM unit test;
**NOT RUN** = required but not executed in this environment; **N/A** = feature intentionally absent.

| ID | Case | Status | Evidence / notes |
| --- | --- | --- | --- |
| T-001 | Start 25m, advance monotonic 3m, read | **PASS / UNIT** | `TimerEngineTest.start then advance three minutes leaves exactly 22 minutes` → `22:00` |
| T-002 | Pause at 22m, advance 1h, read | **PASS / UNIT** | `pause freezes remaining through an hour of idle time` |
| T-003 | Resume then advance to due | **PASS / UNIT** | `resume creates a new deadline and does not count paused time as focus`; device 5s countdown and 1m focus completed |
| T-004 | Deliver duplicate completion callbacks | **PASS / UNIT** | `completing twice is idempotent`; repository deduplicates via `session_event` primary key |
| T-005 | Rotate / recreate Activity 5× | **NOT RUN** | No explicit rotation script; state is Room-backed and ViewModel-scoped |
| T-005b | Process death / force-stop mid-session | **PASS** | Force-stopped a 30 s countdown, relaunched: remaining restored correctly and the live screen opened automatically |
| T-006 | Change wall clock ±2h during same boot | **PASS / UNIT** | `same boot wall clock changes do not affect the countdown`; boot identity is `boot_id`/`BOOT_COUNT` |
| T-007 | Reboot during active countdown | **PASS / UNIT** | `reboot falls back to the wall deadline and never goes negative`; no physical reboot test |
| T-008 | Lock device / power saving | **PARTIAL** | Device screen-off/backgrounded for 12 s; remaining was accurate and notification showed a native chronometer. Doze and OEM battery managers not exercised |
| T-009 | Deny notifications / exact alarms | **PARTIAL** | Exact-alarm denial path observed on the emulator (“Precise alerts unavailable” + inexact fallback). Notification permission denial not exercised as a device test |
| T-010 | Focus 4 cycles incl. long break | **PARTIAL** | Device: 1-minute work stage completed and produced “Break ready”. Unit: 4th completion selects a long break; skipped work does not increment the cycle |
| T-011 | Stopwatch pause and resume | **PASS / UNIT** | `stopwatch pause excludes idle time` |
| T-012 | Midnight / DST / timezone bucketing | **NOT RUN** | History stores UTC epoch millis and buckets with the current zone at presentation; no DST fixture |
| T-013 | Rapid start/pause/resume tapping | **NOT RUN** | Commands are `Mutex`-serialized; no stress script executed |
| T-014 | TalkBack / 200% font | **NOT RUN** | Semantic labels and 48dp targets are implemented, but no TalkBack/accessibility sweep was executed |
| T-015 | Upgrade with Room migration | **PASS** | Installed debug v1 with one completed session; upgraded to v2; session and settings were preserved and `activeStartedWallMs` was added |
| T-016 | Purchase cancel / offline / restore | **N/A** | Billing intentionally not implemented; the Plus screen is a labelled placeholder |
| T-017 | Exact-alarm access on fresh install | **PASS** | `USE_EXACT_ALARM` + `SCHEDULE_EXACT_ALARM`; Settings showed “Allowed” on API 36 |
| T-018 | Notification action after process death | **PASS** | Started a 2-minute countdown, pressed Home, ran `am kill`, expanded the shade and tapped Pause. The restarted process read the session from Room and paused at `00:37 remaining` |
| T-019 | Wall-clock change while alarm pending | **NOT RUN** | Alarm now uses `ELAPSED_REALTIME_WAKEUP`; no rooted wall-clock mutation test executed |
| T-020 | Completion channel sound | **PASS** | `session_alerts_bell` reports `mSound=content://settings/system/alarm_alert` and `usage=USAGE_ALARM`; gentle/chime use the same alarm stream |
| T-021 | Full-screen alert over lock screen | **NOT RUN** | Opt-in setting and `USE_FULL_SCREEN_INTENT` wired; requires a locked, granted Android 14+ device |
| T-022 | Signed release build | **PASS** | `bundleRelease` + `assembleRelease`; `jarsigner -verify` says “jar verified”; `apksigner` verifies APK Signature Scheme v2 |
| T-023 | Singular/plural copy | **PASS** | Home shows `1m · 1 session`; Insights shows `1 completed focus session` |
| T-024 | R8/minified release smoke test | **PASS** | Installed 1.69 MB release APK; launched, completed a countdown, posted the notification, no R8 crash |

## Executed commands and results

```text
.\gradlew.bat --no-daemon clean assembleDebug      BUILD SUCCESSFUL
.\gradlew.bat --no-daemon testDebugUnitTest         BUILD SUCCESSFUL — 15 tests
.\gradlew.bat --no-daemon lintDebug                 BUILD SUCCESSFUL — 0 errors
.\gradlew.bat --no-daemon assembleDebug             BUILD SUCCESSFUL
.\gradlew.bat --no-daemon bundleRelease assembleRelease   BUILD SUCCESSFUL
jarsigner -verify app-release.aab                   jar verified.
apksigner verify app-release.apk                    Verified using v2 scheme
adb install app/build/outputs/apk/release/app-release.apk   Success
adb shell am start -n com.covenants.pomodorotimer/.MainActivity   Displayed
```

## Device observations

- 5-second countdown: produced exactly one completion notification (`Countdown finished`) and one
  completion screen. A second callback would be a no-op by design.
- 1-minute focus: produced `Break ready` and a 5-minute short break; insights then showed `1m`,
  `1 completed`, and a 1-day streak.
- Background: notification title `Focus · Deep Work`, `showChronometer=true`,
  `chronometerCountDown=true`, text `Finishes at 1:03 PM`.
- Paused background: `showChronometer=false`, text `Paused · 00:29 remaining`.
- Process death: force-stopped a 30 s countdown, relaunched, and the live screen returned with `00:17`
  remaining before completion.
- Foreground watchdog: the same countdown reached `00:00`, showed “Time's up.”, and wrote a completion
  history row while the app was visible.
- Release build: minified APK 1.69 MB, AAB 3.76 MB; both signatures verified.
- Exact alarms: Settings showed “Allowed” after installing the release candidate with
  `USE_EXACT_ALARM`; a running timer is rescheduled on resume after returning from settings.
- Notification channels: `session_alerts_bell` uses the system `alarm_alert` URI on
  `AudioAttributes.USAGE_ALARM`; the active-timer channel is silent.
- Notification action: after `am kill`, tapping Pause in the shade restarted the process and paused the
  timer at `00:37 remaining` instead of being ignored.
- Migration: upgrading the installed debug build from v1 to v2 preserved the completed session.
- Copy: `1m · 1 session` and `1 completed focus session` render correctly.
- No crash or ANR appeared in `adb logcat` during the exercised flows.

## Still required before a public release

Physical-device tests on at least Pixel, Samsung and a Xiaomi/Redmi-class OEM build; notification
denial; exact-alarm denial from a clean install; reboot; Activity rotation; TalkBack and 200% font;
Doze; wall-clock mutation while an alarm is pending; full-screen alert on a locked device; and a real
Google Play Billing sandbox test if Plus ships.
