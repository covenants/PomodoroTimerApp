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

## Screenshot order

1. Home / Focus — `01-home.png`
2. Active focus — `02-active-focus.png`
3. Quick Timer — `03-quick-timer.png`
4. Tasks — `05-tasks.png`
5. Insights — `06-insights.png`
6. Themes / Preferences — `07-settings.png`
7. Constellations — `08-constellations.png`
8. Break ready — `02b-break-ready.png`

Show only features that actually exist in the shipped build. Replace the Plus screenshot if the
sample offer is not live.

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
- `SCHEDULE_EXACT_ALARM` — user-facing precise timer. Explain the fallback when access is denied.
- `RECEIVE_BOOT_COMPLETED` — restore/reschedule after reboot.
- `VIBRATE` — optional haptic feedback.
- No foreground service, accessibility service, package visibility or broad storage permission.
