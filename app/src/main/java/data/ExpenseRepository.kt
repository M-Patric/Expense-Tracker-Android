package com.example.expensetracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {

    /**
     * Continuously observes all expenses from the database.
     *
     * The newest expenses are emitted first because that
     * ordering is already defined inside the DAO.
     */
    val allExpenses: Flow<List<Expense>> =
        expenseDao.getAllExpenses()

    /**
     * Continuously observes the total amount spent.
     */
    val totalSpending: Flow<Long> =
        expenseDao.getTotalSpending()

    /**
     * Adds a new expense to the database.
     */
    suspend fun insert(expense: Expense): Long {
        return expenseDao.insert(expense)
    }

    /**
     * Updates an existing expense.
     */
    suspend fun update(expense: Expense): Int {
        return expenseDao.update(expense)
    }

    /**
     * Deletes an existing expense.
     */
    suspend fun delete(expense: Expense): Int {
        return expenseDao.delete(expense)
    }
}