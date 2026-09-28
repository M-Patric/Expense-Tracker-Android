package com.example.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    /**
     * Observable stream containing all expenses.
     */
    val allExpenses: Flow<List<Expense>> =
        repository.allExpenses

    /**
     * Observable stream containing total spending.
     */
    val totalSpending: Flow<Long> =
        repository.totalSpending

    /**
     * Indicates that an expense is currently
     * being inserted or updated.
     */
    private val _isSavingExpense =
        MutableStateFlow(false)

    val isSavingExpense =
        _isSavingExpense.asStateFlow()

    /**
     * Indicates that an expense is currently
     * being deleted.
     */
    private val _isDeletingExpense =
        MutableStateFlow(false)

    val isDeletingExpense =
        _isDeletingExpense.asStateFlow()

    /**
     * One-time events sent to the UI.
     */
    private val _expenseEvents =
        MutableSharedFlow<ExpenseEvent>()

    val expenseEvents =
        _expenseEvents.asSharedFlow()

    /**
     * Add a new expense or update an existing one.
     *
     * id == 0 → INSERT
     * id != 0 → UPDATE
     */
    fun saveExpense(expense: Expense) {

        if (_isSavingExpense.value) {
            return
        }

        viewModelScope.launch {

            _isSavingExpense.value = true

            try {

                if (expense.id == 0L) {

                    repository.insert(expense)

                    _expenseEvents.emit(
                        ExpenseEvent.Added
                    )

                } else {

                    val updatedRows =
                        repository.update(expense)

                    if (updatedRows == 1) {

                        _expenseEvents.emit(
                            ExpenseEvent.Updated
                        )

                    } else {

                        _expenseEvents.emit(
                            ExpenseEvent.UpdateError
                        )
                    }
                }

            } catch (e: Exception) {

                if (expense.id == 0L) {

                    _expenseEvents.emit(
                        ExpenseEvent.InsertError
                    )

                } else {

                    _expenseEvents.emit(
                        ExpenseEvent.UpdateError
                    )
                }

            } finally {

                _isSavingExpense.value = false
            }
        }
    }

    /**
     * Delete an existing expense.
     */
    fun deleteExpense(expense: Expense) {

        if (_isDeletingExpense.value) {
            return
        }

        viewModelScope.launch {

            _isDeletingExpense.value = true

            try {

                val deletedRows =
                    repository.delete(expense)

                if (deletedRows == 1) {

                    _expenseEvents.emit(
                        ExpenseEvent.Deleted(
                            expense
                        )
                    )

                } else {

                    _expenseEvents.emit(
                        ExpenseEvent.DeleteError
                    )
                }

            } catch (e: Exception) {

                _expenseEvents.emit(
                    ExpenseEvent.DeleteError
                )

            } finally {

                _isDeletingExpense.value = false
            }
        }
    }

    /**
     * UI events.
     */
    sealed class ExpenseEvent {

        object Added : ExpenseEvent()

        object Updated : ExpenseEvent()

        object InsertError : ExpenseEvent()

        object UpdateError : ExpenseEvent()

        data class Deleted(
            val expense: Expense
        ) : ExpenseEvent()

        object DeleteError : ExpenseEvent()
    }
}