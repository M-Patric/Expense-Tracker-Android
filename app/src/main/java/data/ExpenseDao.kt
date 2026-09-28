package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /**
     * Add a new expense to the database.
     *
     * Returns the ID assigned to the newly inserted row.
     */
    @Insert
    suspend fun insert(expense: Expense): Long

    /**
     * Update an existing expense.
     *
     * Returns the number of rows updated.
     */
    @Update
    suspend fun update(expense: Expense): Int

    /**
     * Delete an existing expense.
     *
     * Returns the number of rows deleted.
     */
    @Delete
    suspend fun delete(expense: Expense): Int

    /**
     * Retrieve all expenses.
     *
     * Newest expenses appear first.
     *
     * Flow automatically emits a new list whenever the
     * expenses table changes.
     */
    @Query(
        """
        SELECT *
        FROM expenses
        ORDER BY date_millis DESC, id DESC
        """
    )
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Calculate total spending.
     *
     * COALESCE ensures that an empty database returns 0
     * instead of null.
     */
    @Query(
        """
        SELECT COALESCE(SUM(amount_minor), 0)
        FROM expenses
        """
    )
    fun getTotalSpending(): Flow<Long>
}