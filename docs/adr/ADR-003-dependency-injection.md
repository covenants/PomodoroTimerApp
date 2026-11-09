# ADR-003 — Manual dependency injection instead of Hilt

- Status: Accepted for v1
- Date: 2026-11-09

## Context

The engineering brief proposes Hilt. That is a reasonable long-term choice for a multi-module app.
This build is a single-module vertical slice that must compile reproducibly in the available
environment and keep the timer engine pure and testable.

## Decision

Use a single `AppContainer` owned by `PomodoroApp` and construct dependencies explicitly:

- `SystemTimeSource`
- `AppDatabase` / DAOs
- `SettingsStore`
- `AlarmScheduler`
- `TimerNotifier`
- `TimerRepository`

`MainViewModel.Factory` receives the container and injects the repository, settings store and clock.
There are no mutable global singletons and no Activity references inside the repository.

## Consequences

- The dependency graph is visible in one file and easy to audit.
- Compilation avoids annotation-processor interactions between Hilt and Room/KSP.
- If the app grows to multiple feature modules, introduce Hilt (or another DI framework) in a
  dedicated change, keeping the same constructor contracts. This ADR records the deliberate
  deviation rather than claiming Hilt is present.
