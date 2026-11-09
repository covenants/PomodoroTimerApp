package com.covenants.pomodorotimer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timer_session LIMIT 1")
    fun observeActive(): Flow<TimerSessionEntity?>

    @Query("SELECT * FROM timer_session LIMIT 1")
    suspend fun getActive(): TimerSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: TimerSessionEntity)

    @Query("DELETE FROM timer_session")
    suspend fun clearActive()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: SessionHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: SessionEventEntity): Long

    @Query("SELECT * FROM session_history ORDER BY endedWallMs DESC")
    fun observeHistory(): Flow<List<SessionHistoryEntity>>

    @Query("SELECT * FROM session_history ORDER BY endedWallMs DESC")
    suspend fun getAllHistory(): List<SessionHistoryEntity>

    @Query("SELECT * FROM session_history WHERE endedWallMs >= :fromMs AND endedWallMs < :toMs ORDER BY endedWallMs DESC")
    suspend fun historyBetween(fromMs: Long, toMs: Long): List<SessionHistoryEntity>

    @Query("DELETE FROM session_history")
    suspend fun deleteAllHistory()

    @Query("DELETE FROM session_event")
    suspend fun deleteAllEvents()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM focus_task WHERE archivedAtMs IS NULL ORDER BY done ASC, createdAtMs DESC")
    fun observeTasks(): Flow<List<FocusTaskEntity>>

    @Query("SELECT * FROM focus_task WHERE id = :id LIMIT 1")
    suspend fun getTask(id: String): FocusTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: FocusTaskEntity)

    @Query("DELETE FROM focus_task WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE focus_task SET completedSessions = completedSessions + 1 WHERE id = :id")
    suspend fun incrementCompletedSessions(id: String)
}
