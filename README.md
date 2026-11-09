# Orbit Timer — Pomodoro & Everyday Timer

A native Android focus timer built from the supplied product package in `docs/reference/`.
Kotlin + Jetpack Compose + Material 3 + Room + DataStore, offline-first, no account required.

**Status:** debug build verified on an Android 16 / API 36 emulator. 13 timer-engine unit tests pass. Lint passes with no errors.

<p align="center">
  <img src="docs/screenshots/01-home.png" width="23%" alt="Home / Focus" />
  <img src="docs/screenshots/02-active-focus.png" width="23%" alt="Active focus" />
  <img src="docs/screenshots/03-quick-timer.png" width="23%" alt="Quick timer" />
  <img src="docs/screenshots/05-tasks.png" width="23%" alt="Tasks" />
</p>
<p align="center">
  <img src="docs/screenshots/06-insights.png" width="23%" alt="Insights" />
  <img src="docs/screenshots/08-constellations.png" width="23%" alt="Milestones" />
  <img src="docs/screenshots/09-routine.png" width="23%" alt="Routine" />
  <img src="docs/screenshots/07-settings.png" width="23%" alt="Preferences" />
</p>

## What is implemented

### Timing

- **Focus / Pomodoro:** configurable focus, short break and long break durations; long break after N work stages; optional auto-start for breaks and next focus; “Break ready” hand-off instead of silently starting a break.
- **Quick Timer:** arbitrary countdown with HH:MM:SS editing, 5/10/15/30/45/60 minute presets, a saved 8-minute stretching preset, and +5 minutes while running.
- **Stopwatch:** counts up, pauses, resumes and resets; elapsed time is derived from monotonic anchors, not a decrementing counter.
- **One active session at a time**, enforced through a serialized repository and a single-room-row design.
- **Restart safe:** deadlines persist in Room. Same-boot timing uses `SystemClock.elapsedRealtime()`; after reboot it reconstructs conservatively from the persisted wall deadline. Boot identity comes from `/proc/sys/kernel/random/boot_id` (with `BOOT_COUNT` and an offset fallback), so a manual clock change is not mistaken for a reboot.
- **Idempotent completion:** the completion event ID is `${sessionId}:${stageIndex}:COMPLETED`, so a replayed alarm or rapid callback cannot create a second history row or reward.
- **Foreground watchdog:** if an inexact OS alarm is late while the app is visible, a once-per-second check dispatches the same idempotent completion command instead of leaving the UI at `00:00`.
- **Background notification:** ongoing notification with a native countdown chronometer and Pause/Resume, Skip and Stop actions that dispatch through the same command processor as the UI.
- **Completion alert:** high-priority notification, a sound chosen by the user, and a short vibration; all work even if notification permission is denied because sound/haptics are played directly by the receiver.
- **Exact/inexact alarms:** exact alarms are used when Android grants access; otherwise `setAndAllowWhileIdle` is used and the UI honestly reports that alerts may be delayed. No foreground service is used.
- **Boot & package-replace recovery:** `BootReceiver` reconciles and reschedules the upcoming stage.

### Product surfaces

- Home with Focus / Timer / Stopwatch modes and last-used-mode memory.
- Active session, paused state, countdown completion, “Break ready” and “Focus ready” states.
- Tasks with tag, estimate, completion toggle and optional task linkage to a focus session.
- Insights: week/month/year ranges, focus minutes, previous-period comparison, daily bars, 28-day heatmap, streak, completed sessions and most-focused window.
- Constellations: original, non-punitive milestone visual driven by completed sessions.
- Routine editor: durations, long-break cadence, auto-start options and haptics.
- Preferences: three themes (Midnight Orbit, Warm Paper, Quiet Aurora), reduced motion, high contrast, notification/sound/chime/haptic settings, exact-alarm status, CSV export through Android SAF, and history deletion with confirmation.
- Optional Plus screen. The price is explicitly labelled **Illustrative**; no fake checkout exists.

## Screens

| Screen | File |
| --- | --- |
| Home / Focus | `docs/screenshots/01-home.png` |
| Active focus | `docs/screenshots/02-active-focus.png` |
| Paused | `docs/screenshots/02d-paused.png` |
| Background resume | `docs/screenshots/02c-background-resume.png` |
| Break ready | `docs/screenshots/02b-break-ready.png` |
| Quick Timer | `docs/screenshots/03-quick-timer.png` |
| Countdown running | `docs/screenshots/04-active-countdown.png` |
| Countdown complete | `docs/screenshots/04-active-countdown-complete.png` |
| Tasks | `docs/screenshots/05-tasks.png` |
| Insights | `docs/screenshots/06-insights.png` |
| Preferences | `docs/screenshots/07-settings.png` |
| Constellations | `docs/screenshots/08-constellations.png` |
| Routine | `docs/screenshots/09-routine.png` |
| Orbit Plus | `docs/screenshots/10-plus.png` |

All screenshots were captured from the running debug APK, not from the HTML reference package.

## Architecture

```
app/src/main/java/com/covenants/pomodorotimer/
  core/model/        Domain enums and immutable models
  core/time/         TimeSource (monotonic, wall, zone, boot identity) + fake clock
  core/domain/       Pure TimerEngine transitions and TimeFormat helpers
  data/db/           Room entities, DAOs, versioned database
  data/              DataStore settings, TimerRepository command processor
  platform/          AlarmScheduler, receivers, notification, sound/haptics
  ui/                ViewModel, Compose navigation, theme, components, screens
```

- **Single activity, unidirectional flow.** `MainViewModel` combines Room, DataStore and a display tick into `StateFlow<AppUiState>`.
- **The reducer is pure.** `TimerEngine` has no Android dependency and is covered by JVM tests with `FakeTimeSource`.
- **All commands are serialized** through `TimerRepository` with a `Mutex`; state is committed to Room before alarms or notifications are touched.
- **Room is authoritative.** Settings/routine live in DataStore. History is immutable and local.
- **Manual constructor injection** through `AppContainer`. See `docs/adr/ADR-003-dependency-injection.md` for why Hilt was not introduced in this build.
- **No backend, no Firebase, no network permission.** Core behavior is fully offline.

## Build & install

Prerequisites: JDK 17+ (the Android Studio JBR works), Android SDK with platform 36 and build-tools 36.

```bash
# Windows (PowerShell)
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat --no-daemon assembleDebug

# macOS / Linux
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew --no-daemon assembleDebug
```

`local.properties` is intentionally git-ignored. Create it with your own SDK path:

```properties
sdk.dir=/path/to/Android/Sdk
```

Install and launch:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.covenants.pomodorotimer/.MainActivity
```

Run tests and lint:

```bash
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon lintDebug
```

Release signing is not configured. See `docs/release-checklist.md` before producing a Play bundle.

## Permissions

| Permission | Why | If denied |
| --- | --- | --- |
| `POST_NOTIFICATIONS` | Active timer status and completion alerts | Timer still runs; UI explains alerts are off; sound/haptics still attempt to play |
| `SCHEDULE_EXACT_ALARM` | Timely stage-end alert | Falls back to inexact allow-while-idle alarm; UI warns that alerts may be delayed |
| `RECEIVE_BOOT_COMPLETED` | Reconcile and reschedule after reboot | Timer still restores from persisted state when the app is opened |
| `VIBRATE` | Optional completion haptic | No vibration; all other behavior unchanged |

No `AccessibilityService`, `QUERY_ALL_PACKAGES`, `UsageStats`, location, contacts, broad storage or foreground-service permissions are used. CSV export uses the Android Storage Access Framework.

## Verified evidence

| Check | Result |
| --- | --- |
| `testDebugUnitTest` | **PASS**, 13 tests |
| `lintDebug` | **PASS**, 0 errors |
| `assembleDebug` | **PASS** |
| Install on API 36 emulator | **PASS** |
| 5-second countdown completion | **PASS** — one history row, one completion notification |
| 1-minute focus completion | **PASS** — “Break ready”, one completed work stage |
| Background screen-off timing | **PASS** — accurate remaining after 12 seconds off-screen; native chronometer notification |
| Pause while backgrounded | **PASS** — notification shows “Paused · 00:29 remaining” |
| Populated insights | **PASS** — 1m, 1 session, 1-day streak |
| Physical-device OEM matrix | **NOT RUN** — needs Pixel/Samsung/Xiaomi-class hardware |
| Notification-denied path | **NOT RUN** as a device test |
| TalkBack / 200% font | **NOT RUN** as a device test |

See `docs/test-matrix.md` for the full T-001–T-016 matrix and honest status.

## Known limitations

- Exact completion alerts are ultimately controlled by Android/OEM battery policy. The app never claims a notification is guaranteed when permission is denied.
- Force-stopping the app from Android settings blocks alarms until the app is opened again; the session is reconstructed on next launch.
- No Google Play Billing implementation. The Plus screen is a truthful placeholder.
- No ambient audio packs, home-screen widget, cloud sync, app blocking, lap list, or onboarding flow yet.
- Single-module build for clarity; package boundaries are prepared for later feature modules.
- Only the default `en` strings are present; UI copy is ready for localization but not translated.

## Documentation

- `docs/adr/ADR-001-timer-timebase.md`
- `docs/adr/ADR-002-alarm-policy.md`
- `docs/adr/ADR-003-dependency-injection.md`
- `docs/test-matrix.md`
- `docs/privacy-policy.md`
- `docs/store-listing.md`
- `docs/release-checklist.md`
- `docs/reference/` — the supplied research, PRD, UI gallery, engineering spec, agent brief and contact sheet

## Attribution

The implementation is original. The supplied HTML package is an internal planning artifact dated 9 November 2026. Competitor names and any third-party marks belong to their respective owners. “Orbit” is a working title and has not been trademark-cleared. Review the name before publishing.
