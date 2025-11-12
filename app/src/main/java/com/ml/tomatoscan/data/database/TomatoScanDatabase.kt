package com.ml.tomatoscan.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ml.tomatoscan.data.database.converters.DateConverter
import com.ml.tomatoscan.data.database.converters.DiagnosticReportConverter
import com.ml.tomatoscan.data.database.converters.StringListConverter
import com.ml.tomatoscan.data.database.dao.AnalysisDao
import com.ml.tomatoscan.data.database.dao.ResultCacheDao
import com.ml.tomatoscan.data.database.entities.AnalysisEntity
import com.ml.tomatoscan.data.database.entities.ResultCacheEntity

@Database(
    entities = [AnalysisEntity::class, ResultCacheEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class, StringListConverter::class, DiagnosticReportConverter::class)
abstract class TomatoScanDatabase : RoomDatabase() {
    
    abstract fun analysisDao(): AnalysisDao
    abstract fun resultCacheDao(): ResultCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: TomatoScanDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add result_cache table for caching diagnostic results
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS result_cache (
                        imageHash TEXT PRIMARY KEY NOT NULL,
                        diagnosticReport TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        accessCount INTEGER NOT NULL DEFAULT 0,
                        lastAccessedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add diagnosticReport column to analysis_results table
                database.execSQL("""
                    ALTER TABLE analysis_results 
                    ADD COLUMN diagnosticReport TEXT DEFAULT NULL
                """.trimIndent())
            }
        }
        
        fun getDatabase(context: Context): TomatoScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TomatoScanDatabase::class.java,
                    "tomato_scan_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created for the first time
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Database opened
                    }
                })
                .fallbackToDestructiveMigration() // For development only
                .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
