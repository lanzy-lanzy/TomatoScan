package com.ml.tomatoscan.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ml.tomatoscan.data.database.converters.DateConverter
import com.ml.tomatoscan.data.database.converters.StringListConverter
import com.ml.tomatoscan.data.database.dao.AnalysisDao
import com.ml.tomatoscan.data.database.entities.AnalysisEntity

@Database(
    entities = [AnalysisEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class, StringListConverter::class)
abstract class TomatoScanDatabase : RoomDatabase() {
    
    abstract fun analysisDao(): AnalysisDao
    
    companion object {
        @Volatile
        private var INSTANCE: TomatoScanDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future migrations can be added here
            }
        }
        
        fun getDatabase(context: Context): TomatoScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TomatoScanDatabase::class.java,
                    "tomato_scan_database"
                )
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
