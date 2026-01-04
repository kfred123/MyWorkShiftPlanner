package com.pb.myworkshiftplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Shift::class, ShiftAssignment::class, ActualWorkTime::class, WorkingHours::class], version = 5, exportSchema = false)
abstract class ShiftDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun shiftAssignmentDao(): ShiftAssignmentDao
    abstract fun actualWorkTimeDao(): ActualWorkTimeDao
    abstract fun workingHoursDao(): WorkingHoursDao

    companion object {
        @Volatile
        private var INSTANCE: ShiftDatabase? = null

        fun getDatabase(context: Context): ShiftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShiftDatabase::class.java,
                    "shift_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

