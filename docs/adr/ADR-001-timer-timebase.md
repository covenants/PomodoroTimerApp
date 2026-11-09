# ADR-001 — Timer timebase and recovery

- Status: Accepted
- Date: 2026-11-09

## Context

A countdown is a time interval, not a counter. The brief requires correctness across screen-off,
Doze, Activity recreation, process death, reboot, timezone/DST changes and manual wall-clock changes.
Decrementing a `secondsLeft` field once per second is explicitly forbidden.

## Decision

1. A countdown is the difference between an authoritative deadline and a clock:
   - Same boot: `max(0, endElapsedRealtimeMs - SystemClock.elapsedRealtime())`.
   - After reboot or ambiguous identity: `max(0, expectedWallEndMs - System.currentTimeMillis())`.
2. On start/resume, persist `endElapsedRealtimeMs`, `expectedWallEndMs`, boot identity, stage,
   version and status in one Room transaction **before** scheduling the alarm.
3. Boot identity is `/proc/sys/kernel/random/boot_id`, then `Settings.Global` `boot_count`, then a
   coarse wall/elapsed offset. The first two are stable across manual clock changes, so T-006 does
   not misclassify a clock change as a reboot.
4. Pause computes remaining from the monotonic clock, stores it, clears the deadline and cancels the
   alarm. Resume creates a new deadline. Paused wall time is never counted as focus time.
5. Stopwatch uses an accumulated active duration plus a current monotonic start anchor. It never
   uses a deadline. The active run also stores a wall-clock anchor; after a reboot the app reconstructs
   the current run from `nowWall - activeStartedWallMs` and treats it as approximate.
6. Completion is idempotent. The event ID `${sessionId}:${stageIndex}:COMPLETED` is a primary key;
   inserting it twice is a no-op and the history write is skipped when the event already exists.
7. UI ticks are display refreshes only. A foreground-only watchdog checks due-ness once per second
   while the app is visible; the alarm receiver and app restore call the same command. The notification
   is posted only on state changes; its native chronometer handles the per-second display.
8. The database is versioned. Version 2 adds `activeStartedWallMs` with an explicit `1 → 2` migration,
   so a pre-release install survives the upgrade.

## Consequences

- Wall-clock changes on the same boot do not move a countdown.
- Reboot timing is approximate by nature; the app never shows a negative remaining value.
- A force-stopped app cannot receive alarms; the state is still reconstructed on next launch.
- Unit tests can inject a fake clock and prove all transitions deterministically.
