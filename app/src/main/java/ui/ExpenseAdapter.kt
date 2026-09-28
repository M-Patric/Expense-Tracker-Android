package com.example.expensetracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.Expense
import com.example.expensetracker.databinding.ItemExpenseBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private val onEditClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExpenseViewHolder {

        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ExpenseViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {

            binding.expenseCategoryText.text = expense.category

            binding.expenseDescriptionText.text =
                expense.description

            binding.expenseAmountText.text =
                formatAmount(expense.amountMinor)

            binding.expenseDateText.text =
                formatDate(expense.dateMillis)

            binding.editExpenseButton.setOnClickListener {
                onEditClick(expense)
            }

            binding.deleteExpenseButton.setOnClickListener {
                onDeleteClick(expense)
            }
        }
    }

    private fun formatAmount(amountMinor: Long): String {

        val amount = amountMinor / 100.0

        return NumberFormat
            .getNumberInstance(Locale.getDefault())
            .apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            .format(amount)
    }

    private fun formatDate(dateMillis: Long): String {

        val formatter = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )

        return formatter.format(Date(dateMillis))
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {

        override fun areItemsTheSame(
            oldItem: Expense,
            newItem: Expense
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Expense,
            newItem: Expense
        ): Boolean {
            return oldItem == newItem
        }
    }
}