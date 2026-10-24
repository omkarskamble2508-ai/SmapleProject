package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JalSurakshaDao {

    // Actor Accounts for 4 Separate Portals
    @Query("SELECT * FROM actor_accounts ORDER BY lastLoginAt DESC")
    fun getAllActorAccounts(): Flow<List<ActorAccountEntity>>

    @Query("SELECT * FROM actor_accounts WHERE actorRole = :role AND mobileNumber = :mobile LIMIT 1")
    suspend fun findAccountByRoleAndMobile(role: String, mobile: String): ActorAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActorAccount(account: ActorAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActorAccounts(accounts: List<ActorAccountEntity>)

    @Update
    suspend fun updateActorAccount(account: ActorAccountEntity)

    @Query("SELECT * FROM villages ORDER BY CASE overallRisk WHEN 'CONTAMINATED' THEN 1 WHEN 'RISKY' THEN 2 ELSE 3 END, name ASC")
    fun getAllVillages(): Flow<List<VillageEntity>>

    @Query("SELECT COUNT(*) FROM villages")
    suspend fun getVillageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillages(villages: List<VillageEntity>)

    @Update
    suspend fun updateVillage(village: VillageEntity)

    @Query("SELECT * FROM water_sources ORDER BY lastTestedTimestamp DESC")
    fun getAllWaterSources(): Flow<List<WaterSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterSources(sources: List<WaterSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterSource(source: WaterSourceEntity)

    @Update
    suspend fun updateWaterSource(source: WaterSourceEntity)

    @Query("SELECT * FROM action_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllActionTasks(): Flow<List<ActionTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionTasks(tasks: List<ActionTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionTask(task: ActionTaskEntity)

    @Update
    suspend fun updateActionTask(task: ActionTaskEntity)

    @Query("DELETE FROM action_tasks WHERE id = :taskId")
    suspend fun deleteActionTaskById(taskId: Int)

    @Query("SELECT * FROM sample_custody ORDER BY collectionTimestamp DESC")
    fun getAllSamples(): Flow<List<SampleCustodyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<SampleCustodyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: SampleCustodyEntity)

    @Update
    suspend fun updateSample(sample: SampleCustodyEntity)

    @Query("SELECT * FROM symptom_reports ORDER BY timestamp DESC")
    fun getAllSymptomReports(): Flow<List<SymptomReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomReports(reports: List<SymptomReportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomReport(report: SymptomReportEntity)

    @Query("UPDATE water_sources SET syncStatus = :newStatus WHERE syncStatus != 'CLOUD_SYNCED'")
    suspend fun updateAllWaterSourcesSync(newStatus: String)

    @Query("UPDATE action_tasks SET syncStatus = :newStatus WHERE syncStatus != 'CLOUD_SYNCED'")
    suspend fun updateAllTasksSync(newStatus: String)

    @Query("UPDATE sample_custody SET syncStatus = :newStatus WHERE syncStatus != 'CLOUD_SYNCED'")
    suspend fun updateAllSamplesSync(newStatus: String)

    @Query("UPDATE symptom_reports SET syncStatus = :newStatus WHERE syncStatus != 'CLOUD_SYNCED'")
    suspend fun updateAllReportsSync(newStatus: String)
}
