package com.ml.tomatoscan.data.database.dao

import androidx.room.*
import com.ml.tomatoscan.data.database.entities.AnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    
    @Query("SELECT * FROM analysis_results ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<AnalysisEntity>>
    
    @Query("SELECT * FROM analysis_results ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAnalyses(limit: Int = 50): Flow<List<AnalysisEntity>>
    
    @Query("SELECT * FROM analysis_results WHERE id = :id")
    suspend fun getAnalysisById(id: Long): AnalysisEntity?

    @Query("SELECT * FROM analysis_results WHERE timestamp = :timestamp LIMIT 1")
    suspend fun findAnalysisByTimestamp(timestamp: java.util.Date): AnalysisEntity?
    
    @Query("SELECT * FROM analysis_results WHERE diseaseDetected LIKE :disease ORDER BY timestamp DESC")
    suspend fun getAnalysesByDisease(disease: String): List<AnalysisEntity>
    
    @Query("SELECT * FROM analysis_results WHERE severity = :severity ORDER BY timestamp DESC")
    suspend fun getAnalysesBySeverity(severity: String): List<AnalysisEntity>
    
    @Query("SELECT COUNT(*) FROM analysis_results")
    suspend fun getAnalysisCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyses(analyses: List<AnalysisEntity>)
    
    @Update
    suspend fun updateAnalysis(analysis: AnalysisEntity)
    
    @Delete
    suspend fun deleteAnalysis(analysis: AnalysisEntity)
    
    @Query("DELETE FROM analysis_results WHERE id = :id")
    suspend fun deleteAnalysisById(id: Long)
    
    @Query("DELETE FROM analysis_results")
    suspend fun deleteAllAnalyses()
    
    @Query("DELETE FROM analysis_results WHERE timestamp < :cutoffDate")
    suspend fun deleteOldAnalyses(cutoffDate: Long)
    
    // Statistics queries
    @Query("SELECT diseaseDetected, COUNT(*) as count FROM analysis_results GROUP BY diseaseDetected ORDER BY count DESC")
    suspend fun getDiseaseStatistics(): List<DiseaseStatistic>
    
    @Query("SELECT severity, COUNT(*) as count FROM analysis_results GROUP BY severity ORDER BY count DESC")
    suspend fun getSeverityStatistics(): List<SeverityStatistic>
    
    @Query("SELECT AVG(confidence) as averageConfidence FROM analysis_results")
    suspend fun getAverageConfidence(): Float?
}

data class DiseaseStatistic(
    val diseaseDetected: String,
    val count: Int
)

data class SeverityStatistic(
    val severity: String,
    val count: Int
)
