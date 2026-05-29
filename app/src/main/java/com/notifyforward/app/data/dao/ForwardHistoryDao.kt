package com.notifyforward.app.data.dao

import androidx.room.*
import com.notifyforward.app.data.entity.ForwardHistory
import com.notifyforward.app.data.entity.ForwardStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardHistoryDao {

    /** 观察最近 200 条记录，UI 层实时更新 */
    @Query("SELECT * FROM forward_history ORDER BY forwardTime DESC LIMIT 200")
    fun observeRecent(): Flow<List<ForwardHistory>>

    /** 今日成功转发数量 */
    @Query(
        """SELECT COUNT(*) FROM forward_history
           WHERE status = '${ForwardStatus.SUCCESS}'
           AND forwardTime >= :dayStartMs"""
    )
    suspend fun countTodaySuccess(dayStartMs: Long): Int

    /** 最后一次成功转发时间 */
    @Query(
        """SELECT forwardTime FROM forward_history
           WHERE status = '${ForwardStatus.SUCCESS}'
           ORDER BY forwardTime DESC LIMIT 1"""
    )
    suspend fun lastSuccessTime(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ForwardHistory): Long

    /** 保留最新 N 条，删除旧数据，避免无限增长 */
    @Query(
        """DELETE FROM forward_history WHERE id NOT IN (
               SELECT id FROM forward_history ORDER BY forwardTime DESC LIMIT :keepCount
           )"""
    )
    suspend fun trimOldRecords(keepCount: Int = 500)

    @Query("DELETE FROM forward_history")
    suspend fun clearAll()
}
