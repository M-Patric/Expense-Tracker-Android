package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Expense::class],
    version = 1,
    exportSchema = true
)
abstract class ExpenseDatabase : RoomDatabase() {

    /**
     * Gives the application access to ExpenseDao.
     */
    abstract fun expenseDao(): ExpenseDao

    companion object {

        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        /**
         * Returns the single Room database instance used
         * throughout the application.
         */
        fun getDatabase(context: Context): ExpenseDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker_database"
                )
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}