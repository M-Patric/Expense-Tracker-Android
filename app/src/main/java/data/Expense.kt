package com.example.expensetracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    val description: String,

    val category: String,

    @ColumnInfo(name = "date_millis")
    val dateMillis: Long
)