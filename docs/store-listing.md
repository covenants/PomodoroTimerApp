# Google Play listing copy (draft)

> “Orbit” is a working title. Run a Play Store search, trademark check and domain/handle review
> before publishing. Do not use the name as cleared brand without that review.

## App title

`Orbit Timer: Pomodoro & Focus` (29 characters)

## Short description (≤ 80 characters)

`Focus sessions, quick countdowns and calm progress. Offline and private.`

## Full description

```
One tap to start. Your time stays yours.

Orbit Timer is a calm, offline-first focus timer with two first-class modes:

FOCUS / POMODORO
• 25 / 5 by default, with 50 / 10 and 5-minute presets
• Configurable focus, short-break and long-break lengths
• Long break after your chosen number of focus sessions
• Optional auto-start for breaks and the next focus
• “Break ready” hand-off — no surprise timers, no guilt

QUICK TIMER & STOPWATCH
• Any countdown from one second to 12 hours
• 5, 10, 15, 30, 45 and 60-minute quick starts
• Direct HH:MM:SS editing and +5 minutes while running
• A stopwatch that keeps counting while the app is closed

BUILT TO BE TRUSTWORTHY
• Counts down from a saved deadline, not a ticking counter
• Restores correctly after screen-off, backgrounding, process death and reboot
• Native countdown notification with pause, skip and stop actions
• Honest handling when Android denies notification or exact-alarm access
• No account, no ads during focus, no required internet

GENTLE PROGRESS
• Local session history and today/week insights
• Daily consistency heatmap and streak
• An original constellation that grows with completed sessions — no dying plants or punishment
• Tasks with tags and estimates, optionally linked to a focus session

MAKE IT YOURS
• Midnight Orbit, Warm Paper and Quiet Aurora themes
• Reduced motion, high contrast, scalable text and TalkBack-friendly controls
• Local CSV export through Android's file picker
• Delete your history at any time

CORE TIMERS ARE FREE
Focus, countdown, stopwatch, alerts, local history and accessibility settings are free.
Orbit Plus is an optional future upgrade for extended reports, sound packs and extra themes.
The price shown in this build is a labelled sample, not a live offer.

PRIVATE BY DEFAULT
Your tasks, sessions and preferences stay on your device. No analytics SDK. No advertising ID.
No network permission. No account.

Orbit Timer is a working title; the final brand will be confirmed before release.
```

## Store assets

- App icon: `docs/play/icon-512.png` (512 × 512) and `docs/play/icon-1024.png`
- Feature graphic: `docs/play/feature-graphic-1024x500.png`
- Phone screenshots: `docs/play/screenshots/01-home.png` … `08-themes.png` (1080 × 1920)

See `docs/play/README.md` for the full asset pack.

## Screenshot order

1. **Make time count.** — `docs/play/screenshots/01-home.png`
2. **Stay in your orbit.** — `docs/play/screenshots/02-active-focus.png`
3. **Any countdown.** — `docs/play/screenshots/03-quick-timer.png`
4. **Breaks that wait.** — `docs/play/screenshots/04-break-ready.png`
5. **One thing at a time.** — `docs/play/screenshots/05-tasks.png`
6. **Progress you can see.** — `docs/play/screenshots/06-insights.png`
7. **Your orbit, your pace.** — `docs/play/screenshots/07-constellations.png`
8. **Make it yours.** — `docs/play/screenshots/08-themes.png`

Show only features that actually exist in the shipped build. Replace the Plus screenshot if the
sample offer is not live. The current pack does not show Plus or billing.

## Keywords to research

`pomodoro timer`, `focus timer`, `study timer`, `countdown timer`, `stopwatch`, `productivity timer`,
`offline timer`, `ADHD focus timer` (only if the final positioning supports it honestly).

## Data safety answers (for this build)

- Does the app collect or share required user data types? **No.**
- Is data encrypted in transit? **N/A — no data is transmitted.**
- Can users request deletion? **Yes — in-app “Delete all history”; uninstall removes all app data.**
- Data types stored on device only: app activity (focus sessions), app info and performance (none),
  personal info (none), contacts (none), location (none), audio (none).
- Advertising or tracking SDKs: **none.**
- Account creation: **none.**

Re-answer these in Play Console against the exact release build. A future billing or analytics SDK
requires a new declaration.

## Permissions declaration notes

- `POST_NOTIFICATIONS` — core active-timer and completion alerts.
- `SCHEDULE_EXACT_ALARM` — user-granted precise timer on Android 12/12L; `maxSdkVersion=32`.
- `USE_EXACT_ALARM` — auto-granted precise alarms on Android 13+ for a core timer app. The exact-alarm
  declaration is **mandatory** in Play Console.
- `USE_FULL_SCREEN_INTENT` — optional opt-in completion alert over the lock screen; not used by default.
  The full-screen-intent declaration is **mandatory** in Play Console.
- `RECEIVE_BOOT_COMPLETED` — restore/reschedule after reboot.
- `VIBRATE` — optional haptic feedback.
- No foreground service, accessibility service, package visibility or broad storage permission.
