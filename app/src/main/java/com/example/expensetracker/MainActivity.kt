package com.example.expensetracker

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseDatabase
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.ui.ExpenseAdapter
import com.example.expensetracker.ui.ExpenseViewModel
import com.example.expensetracker.ui.ExpenseViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var selectedDateMillis =
        MaterialDatePicker.todayInUtcMilliseconds()

    /**
     * null = adding a new expense
     *
     * non-null = editing an existing expense
     */
    private var editingExpense: Expense? = null

    companion object {
        private const val KEY_SELECTED_DATE =
            "selected_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        // --------------------------------------------------
        // EDGE-TO-EDGE / SYSTEM BAR HANDLING
        // --------------------------------------------------

        val rootView =
            findViewById<View>(R.id.main)

        val initialLeft =
            rootView.paddingLeft

        val initialTop =
            rootView.paddingTop

        val initialRight =
            rootView.paddingRight

        val initialBottom =
            rootView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )

            insets
        }

        // --------------------------------------------------
        // RESTORE SELECTED DATE
        // --------------------------------------------------

        selectedDateMillis =
            savedInstanceState?.getLong(
                KEY_SELECTED_DATE,
                selectedDateMillis
            ) ?: selectedDateMillis

        // --------------------------------------------------
        // FIND UI VIEWS
        // --------------------------------------------------

        val formScrollView =
            findViewById<NestedScrollView>(
                R.id.formScrollView
            )

        val addExpenseTitle =
            findViewById<TextView>(
                R.id.addExpenseTitle
            )

        val totalSpendingText =
            findViewById<TextView>(
                R.id.totalSpendingText
            )

        val amountInputLayout =
            findViewById<TextInputLayout>(
                R.id.amountInputLayout
            )

        val amountEditText =
            findViewById<TextInputEditText>(
                R.id.amountEditText
            )

        val descriptionInputLayout =
            findViewById<TextInputLayout>(
                R.id.descriptionInputLayout
            )

        val descriptionEditText =
            findViewById<TextInputEditText>(
                R.id.descriptionEditText
            )

        val categoryInputLayout =
            findViewById<TextInputLayout>(
                R.id.categoryInputLayout
            )

        val categoryDropdown =
            findViewById<AutoCompleteTextView>(
                R.id.categoryAutoCompleteTextView
            )

        val dateButton =
            findViewById<Button>(
                R.id.dateButton
            )

        val addExpenseButton =
            findViewById<Button>(
                R.id.addExpenseButton
            )

        val cancelEditButton =
            findViewById<Button>(
                R.id.cancelEditButton
            )

        val emptyExpensesText =
            findViewById<TextView>(
                R.id.emptyExpensesText
            )

        val emptyExpensesMessage =
            findViewById<TextView>(
                R.id.emptyExpensesMessage
            )

        val expenseRecyclerView =
            findViewById<RecyclerView>(
                R.id.expenseRecyclerView
            )

        // --------------------------------------------------
        // DATABASE
        // --------------------------------------------------

        val database =
            ExpenseDatabase.getDatabase(
                applicationContext
            )

        val repository =
            ExpenseRepository(
                database.expenseDao()
            )

        val factory =
            ExpenseViewModelFactory(
                repository
            )

        val viewModel =
            ViewModelProvider(
                this,
                factory
            )[ExpenseViewModel::class.java]

        // --------------------------------------------------
        // RECYCLERVIEW
        // --------------------------------------------------

        val expenseAdapter =
            ExpenseAdapter(

                // ------------------------------------------
                // EDIT
                // ------------------------------------------

                onEditClick = { expense ->

                    editingExpense = expense

                    addExpenseTitle.text =
                        getString(
                            R.string.edit_expense
                        )

                    addExpenseButton.text =
                        getString(
                            R.string.save_changes
                        )

                    cancelEditButton.visibility =
                        View.VISIBLE

                    val amount =
                        BigDecimal.valueOf(
                            expense.amountMinor,
                            2
                        )

                    amountEditText.setText(
                        amount.toPlainString()
                    )

                    descriptionEditText.setText(
                        expense.description
                    )

                    categoryDropdown.setText(
                        expense.category,
                        false
                    )

                    selectedDateMillis =
                        expense.dateMillis

                    dateButton.text =
                        formatDate(
                            selectedDateMillis
                        )

                    amountInputLayout.error =
                        null

                    descriptionInputLayout.error =
                        null

                    categoryInputLayout.error =
                        null

                    formScrollView.smoothScrollTo(
                        0,
                        0
                    )

                    amountEditText.requestFocus()
                },

                // ------------------------------------------
                // DELETE
                // ------------------------------------------

                onDeleteClick = { expense ->

                    MaterialAlertDialogBuilder(this)
                        .setTitle(
                            getString(
                                R.string.delete_expense
                            )
                        )
                        .setMessage(
                            buildString {

                                append(
                                    getString(
                                        R.string.delete_confirmation
                                    )
                                )

                                append("\n\n")

                                append(
                                    "${expense.category} • "
                                )

                                append(
                                    formatAmount(
                                        expense.amountMinor
                                    )
                                )

                                append("\n")

                                append(
                                    expense.description
                                )
                            }
                        )
                        .setNegativeButton(
                            getString(
                                R.string.cancel
                            ),
                            null
                        )
                        .setPositiveButton(
                            getString(
                                R.string.delete
                            )
                        ) { _, _ ->

                            viewModel.deleteExpense(
                                expense
                            )
                        }
                        .show()
                }
            )

        expenseRecyclerView.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MainActivity
                )

            adapter = expenseAdapter

            setHasFixedSize(false)
        }

        // --------------------------------------------------
        // CATEGORY DROPDOWN
        // --------------------------------------------------

        val categoryAdapter =
            ArrayAdapter.createFromResource(
                this,
                R.array.expense_categories,
                android.R.layout
                    .simple_dropdown_item_1line
            )

        categoryDropdown.apply {

            setAdapter(categoryAdapter)

            setOnClickListener {
                showDropDown()
            }

            setOnFocusChangeListener {
                    _,
                    hasFocus ->

                if (hasFocus) {
                    showDropDown()
                }
            }
        }

        // --------------------------------------------------
        // DATE PICKER
        // --------------------------------------------------

        dateButton.text =
            formatDate(
                selectedDateMillis
            )

        dateButton.setOnClickListener {

            val datePicker =
                MaterialDatePicker
                    .Builder
                    .datePicker()
                    .setTitleText(
                        getString(
                            R.string.date_label
                        )
                    )
                    .setSelection(
                        selectedDateMillis
                    )
                    .build()

            datePicker
                .addOnPositiveButtonClickListener {
                        selection ->

                    selectedDateMillis =
                        selection

                    dateButton.text =
                        formatDate(
                            selectedDateMillis
                        )
                }

            datePicker.show(
                supportFragmentManager,
                "EXPENSE_DATE_PICKER"
            )
        }

        // --------------------------------------------------
        // ADD / UPDATE EXPENSE
        // --------------------------------------------------

        addExpenseButton.setOnClickListener {

            amountInputLayout.error =
                null

            descriptionInputLayout.error =
                null

            categoryInputLayout.error =
                null

            val amountText =
                amountEditText
                    .text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val description =
                descriptionEditText
                    .text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val category =
                categoryDropdown
                    .text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            // ------------------------------------------
            // VALIDATE AMOUNT
            // ------------------------------------------

            val amountMinor =
                parseAmountToMinorUnits(
                    amountText
                )

            if (amountMinor == null) {

                amountInputLayout.error =
                    getString(
                        R.string.invalid_amount
                    )

                amountEditText.requestFocus()

                return@setOnClickListener
            }

            // ------------------------------------------
            // VALIDATE DESCRIPTION
            // ------------------------------------------

            if (description.isBlank()) {

                descriptionInputLayout.error =
                    getString(
                        R.string.description_required
                    )

                descriptionEditText.requestFocus()

                return@setOnClickListener
            }

            // ------------------------------------------
            // VALIDATE CATEGORY
            // ------------------------------------------

            val validCategories =
                resources
                    .getStringArray(
                        R.array.expense_categories
                    )
                    .toSet()

            if (category !in validCategories) {

                categoryInputLayout.error =
                    getString(
                        R.string.category_required
                    )

                categoryDropdown.requestFocus()

                return@setOnClickListener
            }

            // ------------------------------------------
            // PRESERVE ID WHEN EDITING
            // ------------------------------------------

            val currentId =
                editingExpense?.id ?: 0L

            // ------------------------------------------
            // CREATE EXPENSE OBJECT
            // ------------------------------------------

            val expense =
                Expense(
                    id = currentId,
                    amountMinor = amountMinor,
                    description = description,
                    category = category,
                    dateMillis = selectedDateMillis
                )

            viewModel.saveExpense(
                expense
            )
        }

        // --------------------------------------------------
        // CANCEL EDIT
        // --------------------------------------------------

        cancelEditButton.setOnClickListener {

            exitEditMode(
                addExpenseTitle = addExpenseTitle,
                addExpenseButton = addExpenseButton,
                cancelEditButton = cancelEditButton,
                amountEditText = amountEditText,
                descriptionEditText = descriptionEditText,
                categoryDropdown = categoryDropdown,
                amountInputLayout = amountInputLayout,
                descriptionInputLayout = descriptionInputLayout,
                categoryInputLayout = categoryInputLayout,
                dateButton = dateButton
            )
        }

        // --------------------------------------------------
        // OBSERVE DATABASE + VIEWMODEL
        // --------------------------------------------------

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // ------------------------------------------
                // EXPENSE HISTORY
                // ------------------------------------------

                launch {

                    viewModel.allExpenses
                        .collect { expenses ->

                            expenseAdapter
                                .submitList(
                                    expenses
                                )

                            val hasExpenses =
                                expenses.isNotEmpty()

                            expenseRecyclerView.visibility =
                                if (hasExpenses) {
                                    View.VISIBLE
                                } else {
                                    View.GONE
                                }

                            emptyExpensesText.visibility =
                                if (hasExpenses) {
                                    View.GONE
                                } else {
                                    View.VISIBLE
                                }

                            emptyExpensesMessage.visibility =
                                if (hasExpenses) {
                                    View.GONE
                                } else {
                                    View.VISIBLE
                                }
                        }
                }

                // ------------------------------------------
                // TOTAL SPENDING
                // ------------------------------------------

                launch {

                    viewModel.totalSpending
                        .collect { totalMinor ->

                            totalSpendingText.text =
                                formatAmount(
                                    totalMinor
                                )
                        }
                }

                // ------------------------------------------
                // ADD / UPDATE / DELETE EVENTS
                // ------------------------------------------

                launch {

                    viewModel.expenseEvents
                        .collect { event ->

                            when (event) {

                                // --------------------------
                                // ADDED
                                // --------------------------

                                ExpenseViewModel
                                    .ExpenseEvent
                                    .Added -> {

                                    exitEditMode(
                                        addExpenseTitle,
                                        addExpenseButton,
                                        cancelEditButton,
                                        amountEditText,
                                        descriptionEditText,
                                        categoryDropdown,
                                        amountInputLayout,
                                        descriptionInputLayout,
                                        categoryInputLayout,
                                        dateButton
                                    )

                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(
                                            R.string.expense_added
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                // --------------------------
                                // UPDATED
                                // --------------------------

                                ExpenseViewModel
                                    .ExpenseEvent
                                    .Updated -> {

                                    exitEditMode(
                                        addExpenseTitle,
                                        addExpenseButton,
                                        cancelEditButton,
                                        amountEditText,
                                        descriptionEditText,
                                        categoryDropdown,
                                        amountInputLayout,
                                        descriptionInputLayout,
                                        categoryInputLayout,
                                        dateButton
                                    )

                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(
                                            R.string.expense_updated
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                // --------------------------
                                // UPDATE ERROR
                                // --------------------------

                                ExpenseViewModel
                                    .ExpenseEvent
                                    .UpdateError -> {

                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(
                                            R.string.expense_update_failed
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                // --------------------------
                                // SAVE ERROR
                                // --------------------------

                                ExpenseViewModel
                                    .ExpenseEvent
                                    .InsertError -> {

                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(
                                            R.string.expense_add_failed
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                // --------------------------
                                // DELETED
                                // --------------------------

                                is ExpenseViewModel
                                .ExpenseEvent
                                .Deleted -> {

                                    /*
                                     * If the deleted expense was
                                     * currently being edited,
                                     * leave edit mode.
                                     */
                                    if (
                                        editingExpense?.id ==
                                        event.expense.id
                                    ) {

                                        exitEditMode(
                                            addExpenseTitle,
                                            addExpenseButton,
                                            cancelEditButton,
                                            amountEditText,
                                            descriptionEditText,
                                            categoryDropdown,
                                            amountInputLayout,
                                            descriptionInputLayout,
                                            categoryInputLayout,
                                            dateButton
                                        )
                                    }

                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(
                                            R.string.expense_deleted
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                // --------------------------
                                // DELETE ERROR
                                // --------------------------

                                ExpenseViewModel
                                    .ExpenseEvent
                                    .DeleteError -> {

                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(
                                            R.string.expense_delete_failed
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                }

                // ------------------------------------------
                // SAVING STATE
                // ------------------------------------------

                launch {

                    viewModel.isSavingExpense
                        .collect { isSaving ->

                            addExpenseButton
                                .isEnabled =
                                !isSaving

                            cancelEditButton
                                .isEnabled =
                                !isSaving

                            addExpenseButton.text =
                                when {

                                    isSaving -> {
                                        getString(
                                            R.string.saving_expense
                                        )
                                    }

                                    editingExpense != null -> {
                                        getString(
                                            R.string.save_changes
                                        )
                                    }

                                    else -> {
                                        getString(
                                            R.string.add_expense_button
                                        )
                                    }
                                }
                        }
                }

                // ------------------------------------------
                // DELETING STATE
                // ------------------------------------------

                launch {

                    viewModel.isDeletingExpense
                        .collect { isDeleting ->

                            expenseRecyclerView
                                .isEnabled =
                                !isDeleting
                        }
                }
            }
        }
    }

    // ------------------------------------------------------
    // EXIT EDIT MODE
    // ------------------------------------------------------

    private fun exitEditMode(
        addExpenseTitle: TextView,
        addExpenseButton: Button,
        cancelEditButton: Button,
        amountEditText: TextInputEditText,
        descriptionEditText: TextInputEditText,
        categoryDropdown: AutoCompleteTextView,
        amountInputLayout: TextInputLayout,
        descriptionInputLayout: TextInputLayout,
        categoryInputLayout: TextInputLayout,
        dateButton: Button
    ) {

        editingExpense = null

        addExpenseTitle.text =
            getString(
                R.string.add_expense
            )

        addExpenseButton.text =
            getString(
                R.string.add_expense_button
            )

        cancelEditButton.visibility =
            View.GONE

        amountEditText.text?.clear()

        descriptionEditText.text?.clear()

        categoryDropdown.setText(
            "",
            false
        )

        amountInputLayout.error =
            null

        descriptionInputLayout.error =
            null

        categoryInputLayout.error =
            null

        selectedDateMillis =
            MaterialDatePicker
                .todayInUtcMilliseconds()

        dateButton.text =
            formatDate(
                selectedDateMillis
            )
    }

    // ------------------------------------------------------
    // CONVERT AMOUNT TO MINOR CURRENCY UNITS
    // ------------------------------------------------------

    private fun parseAmountToMinorUnits(
        value: String
    ): Long? {

        if (value.isBlank()) {
            return null
        }

        return try {

            val amount =
                BigDecimal(value)

            if (amount <= BigDecimal.ZERO) {
                return null
            }

            if (amount.scale() > 2) {
                return null
            }

            amount
                .movePointRight(2)
                .longValueExact()

        } catch (e: Exception) {

            null
        }
    }

    // ------------------------------------------------------
    // FORMAT AMOUNT
    // ------------------------------------------------------

    private fun formatAmount(
        amountMinor: Long
    ): String {

        val amount =
            BigDecimal.valueOf(
                amountMinor,
                2
            )

        return NumberFormat
            .getNumberInstance(
                Locale.getDefault()
            )
            .apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            .format(amount)
    }

    // ------------------------------------------------------
    // FORMAT DATE
    // ------------------------------------------------------

    private fun formatDate(
        dateMillis: Long
    ): String {

        val formatter =
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )

        formatter.timeZone =
            TimeZone.getTimeZone("UTC")

        return formatter.format(
            Date(dateMillis)
        )
    }

    // ------------------------------------------------------
    // SAVE INSTANCE STATE
    // ------------------------------------------------------

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        outState.putLong(
            KEY_SELECTED_DATE,
            selectedDateMillis
        )

        super.onSaveInstanceState(
            outState
        )
    }
}