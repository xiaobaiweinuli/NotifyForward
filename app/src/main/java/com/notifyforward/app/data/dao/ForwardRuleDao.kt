package com.notifyforward.app.data.dao

import androidx.room.*
import com.notifyforward.app.data.entity.ForwardRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardRuleDao {

    @Query("SELECT * FROM forward_rules ORDER BY priority ASC, createdAt ASC")
    fun observeAll(): Flow<List<ForwardRule>>

    @Query("SELECT * FROM forward_rules WHERE enabled = 1 ORDER BY priority ASC, createdAt ASC")
    suspend fun getEnabledRules(): List<ForwardRule>

    @Query("SELECT * FROM forward_rules ORDER BY priority ASC, createdAt ASC")
    suspend fun getAll(): List<ForwardRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ForwardRule): Long

    @Update
    suspend fun update(rule: ForwardRule)

    @Delete
    suspend fun delete(rule: ForwardRule)

    @Query("DELETE FROM forward_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE forward_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE forward_rules SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: Int)
}
