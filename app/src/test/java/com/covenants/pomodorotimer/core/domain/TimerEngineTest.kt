package com.covenants.pomodorotimer.core.domain

import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.core.time.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimerEngineTest {

    private fun focusSession(
        clock: FakeTimeSource,
        durationMs: Long = 25 * 60_000L,
    ) = TimerEngine.newSession(
        sessionId = "session-1",
        mode = TimerMode.FOCUS,
        stage = FocusStage.WORK,
        stageIndex = 0,
        durationMs = durationMs,
        clock = clock,
    )

    @Test
    fun `start then advance three minutes leaves exactly 22 minutes`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(3 * 60_000L)
        assertThat(TimerEngine.remainingMs(session, clock)).isEqualTo(22 * 60_000L)
        assertThat(TimeFormat.countdown(TimerEngine.remainingMs(session, clock))).isEqualTo("22:00")
    }

    @Test
    fun `pause freezes remaining through an hour of idle time`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(3 * 60_000L)
        val paused = TimerEngine.pause(session, clock)
        clock.advance(60 * 60_000L)
        assertThat(TimerEngine.remainingMs(paused, clock)).isEqualTo(22 * 60_000L)
        assertThat(paused.status).isEqualTo(TimerStatus.PAUSED)
    }

    @Test
    fun `resume creates a new deadline and does not count paused time as focus`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(3 * 60_000L)
        val paused = TimerEngine.pause(session, clock)
        clock.advance(60 * 60_000L)
        val resumed = TimerEngine.resume(paused, clock)
        clock.advance(22 * 60_000L)

        assertThat(TimerEngine.isDue(resumed, clock)).isTrue()
        val completed = TimerEngine.complete(resumed, clock)
        assertThat(completed.status).isEqualTo(TimerStatus.COMPLETED)
        assertThat(TimerEngine.activeDurationMs(completed, clock)).isEqualTo(25 * 60_000L)
    }

    @Test
    fun `completing twice is idempotent`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(25 * 60_000L)
        val once = TimerEngine.complete(session, clock)
        val twice = TimerEngine.complete(once, clock)
        assertThat(twice).isEqualTo(once)
        assertThat(twice.version).isEqualTo(once.version)
        assertThat(twice.status).isEqualTo(TimerStatus.COMPLETED)
    }

    @Test
    fun `same boot wall clock changes do not affect the countdown`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(5 * 60_000L)
        clock.changeWallClock(2 * 60 * 60_000L)
        assertThat(TimerEngine.remainingMs(session, clock)).isEqualTo(20 * 60_000L)
    }

    @Test
    fun `reboot falls back to the wall deadline and never goes negative`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(5 * 60_000L) // 20 minutes remain, wall deadline is 20 minutes away
        clock.reboot()
        assertThat(TimerEngine.remainingMs(session, clock)).isEqualTo(20 * 60_000L)
        clock.advance(30 * 60_000L)
        assertThat(TimerEngine.remainingMs(session, clock)).isEqualTo(0L)
    }

    @Test
    fun `long break is scheduled after the configured fourth work stage`() {
        val routine = RoutineConfig(longBreakEvery = 4)
        val clock = FakeTimeSource()
        val first = focusSession(clock)
        assertThat(TimerEngine.planNextStage(first.copy(completedWorkCount = 2), routine, true).stage)
            .isEqualTo(FocusStage.SHORT_BREAK)
        val plan = TimerEngine.planNextStage(first.copy(completedWorkCount = 3), routine, true)
        assertThat(plan.stage).isEqualTo(FocusStage.LONG_BREAK)
        assertThat(plan.completedWorkCount).isEqualTo(4)
    }

    @Test
    fun `skipped work does not increment the cycle`() {
        val routine = RoutineConfig(longBreakEvery = 4)
        val clock = FakeTimeSource()
        val first = focusSession(clock)
        val plan = TimerEngine.planNextStage(first.copy(completedWorkCount = 2), routine, false)
        assertThat(plan.completedWorkCount).isEqualTo(2)
        assertThat(plan.stage).isEqualTo(FocusStage.SHORT_BREAK)
    }

    @Test
    fun `break completion returns to work with configured duration`() {
        val routine = RoutineConfig(focusMinutes = 50, longBreakEvery = 4)
        val clock = FakeTimeSource()
        val breakSession = focusSession(clock).copy(
            stage = FocusStage.LONG_BREAK,
            stageIndex = 7,
            completedWorkCount = 4,
        )
        val plan = TimerEngine.planNextStage(breakSession, routine, true)
        assertThat(plan.stage).isEqualTo(FocusStage.WORK)
        assertThat(plan.durationMs).isEqualTo(50 * 60_000L)
        assertThat(plan.completedWorkCount).isEqualTo(4)
    }

    @Test
    fun `stopwatch pause excludes idle time`() {
        val clock = FakeTimeSource()
        val watch = TimerEngine.newStopwatch("watch", clock)
        clock.advance(10_000L)
        val paused = TimerEngine.pause(watch, clock)
        clock.advance(60_000L)
        assertThat(TimerEngine.stopwatchElapsedMs(paused, clock)).isEqualTo(10_000L)
        val resumed = TimerEngine.resume(paused, clock)
        clock.advance(5_000L)
        assertThat(TimerEngine.stopwatchElapsedMs(resumed, clock)).isEqualTo(15_000L)
    }

    @Test
    fun `extend adds five minutes in running and paused states`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(5 * 60_000L)
        val running = TimerEngine.extend(session, 5 * 60_000L, clock)
        assertThat(TimerEngine.remainingMs(running, clock)).isEqualTo(25 * 60_000L)

        val paused = TimerEngine.pause(running, clock)
        val extended = TimerEngine.extend(paused, 5 * 60_000L, clock)
        assertThat(TimerEngine.remainingMs(extended, clock)).isEqualTo(30 * 60_000L)
        assertThat(extended.totalDurationMs).isEqualTo(35 * 60_000L)
    }

    @Test
    fun `a fresh timer displays its full duration`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        assertThat(TimeFormat.countdown(TimerEngine.remainingMs(session, clock))).isEqualTo("25:00")
        assertThat(TimerEngine.progress(session, clock)).isEqualTo(0f)
    }

    @Test
    fun `progress reaches one at the deadline`() {
        val clock = FakeTimeSource()
        val session = focusSession(clock)
        clock.advance(25 * 60_000L)
        assertThat(TimerEngine.progress(session, clock)).isEqualTo(1f)
    }
}
