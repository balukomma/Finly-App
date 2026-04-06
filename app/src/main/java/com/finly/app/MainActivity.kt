package com.finly.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.finly.app.databinding.*
import com.finly.app.data.Transaction
import android.content.res.ColorStateList
import androidx.core.view.ViewCompat
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: FinlyViewModel by viewModels()
    private val backStack = mutableListOf<() -> Unit>()
    private var isNavigatingBack = false
    private var currentTabId: Int = R.id.dashboardFragment

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        handleSelectedImage(uri)
    }

    private fun handleSelectedImage(uri: android.net.Uri?) {
        uri?.let { sourceUri ->
            try {
                val inputStream = contentResolver.openInputStream(sourceUri)
                val photoFile = File(filesDir, "profile_image.jpg")
                val outputStream = FileOutputStream(photoFile)
                
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setProfileImage(Uri.fromFile(photoFile).toString())
            } catch (e: Exception) {
                // Log or handle error - staying in app rather than crashing
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.screenContainer.updatePadding(top = systemBars.top)
            binding.bottomNav.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // savedInstanceState == null  → completely fresh launch → always show splash
        // savedInstanceState != null  → Activity was recreated (dark mode toggle, rotation)
        //                               → restore the screen the user was on
        if (savedInstanceState == null) {
            showSplash()
        } else {
            currentTabId = savedInstanceState.getInt("current_tab", R.id.dashboardFragment)
            binding.bottomNav.selectedItemId = currentTabId
            when (currentTabId) {
                R.id.dashboardFragment   -> showDashboard()
                R.id.transactionsFragment -> showTransactions()
                R.id.goalsFragment        -> showGoals()
                R.id.insightsFragment     -> showInsights()
                R.id.settingsFragment     -> showSettings()
            }
        }

        // Setup Bottom Nav
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != currentTabId && !isNavigatingBack) {
                // Store the current tab show function in backstack
                val prevTabId = currentTabId
                pushToBackStack { 
                    currentTabId = prevTabId
                    binding.bottomNav.selectedItemId = prevTabId 
                }
                currentTabId = item.itemId
            }
            
            when (item.itemId) {
                R.id.dashboardFragment -> {
                    showDashboard()
                    true
                }
                R.id.transactionsFragment -> {
                    showTransactions()
                    true
                }
                R.id.goalsFragment -> {
                    showGoals()
                    true
                }
                R.id.insightsFragment -> {
                    showInsights()
                    true
                }
                R.id.settingsFragment -> {
                    showSettings()
                    true
                }
                else -> false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_tab", currentTabId)
    }

    private fun pushToBackStack(action: () -> Unit) {
        if (!isNavigatingBack) {
            backStack.add(action)
        }
    }

    private fun swapScreen(newView: View, showBottomNav: Boolean, transitionType: Int = 0, isBack: Boolean = false) {
        val transition: androidx.transition.Transition = when (transitionType) {
            1 -> MaterialSharedAxis(MaterialSharedAxis.X, !isBack)
            2 -> MaterialSharedAxis(MaterialSharedAxis.Y, !isBack)
            else -> MaterialFadeThrough()
        }
        transition.duration = 350
        
        TransitionManager.beginDelayedTransition(binding.screenContainer, transition)
        
        binding.screenContainer.removeAllViews()
        binding.screenContainer.addView(newView)
        binding.bottomNav.visibility = if (showBottomNav) View.VISIBLE else View.GONE
    }

    override fun onBackPressed() {
        if (backStack.isNotEmpty()) {
            isNavigatingBack = true
            val previousScreen = backStack.removeAt(backStack.size - 1)
            previousScreen.invoke()
            isNavigatingBack = false
        } else {
            super.onBackPressed()
        }
    }

    private fun showSplash() {
        val splashBinding = FragmentSplashBinding.inflate(layoutInflater)
        swapScreen(splashBinding.root, false)

        // Entrance animation — logo scales & fades in with overshoot
        splashBinding.logo.alpha = 0f
        splashBinding.logo.scaleX = 0.65f
        splashBinding.logo.scaleY = 0.65f
        splashBinding.logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        // Title fades in after logo animation
        splashBinding.title.alpha = 0f
        splashBinding.title.translationY = 30f
        splashBinding.title.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(400)
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Subtitle fades in last
        splashBinding.subtitle.alpha = 0f
        splashBinding.subtitle.translationY = 20f
        splashBinding.subtitle.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(600)
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        splashBinding.root.postDelayed({
            if (viewModel.isBiometricEnabled.value == true) {
                showBiometricPrompt()
            } else {
                if (viewModel.isSetupComplete()) showDashboard() else showOnboarding()
            }
        }, 2200)
    }

    private fun showBiometricPrompt() {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    android.widget.Toast.makeText(applicationContext, "Authentication required", android.widget.Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (viewModel.isSetupComplete()) showDashboard() else showOnboarding()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Finly")
            .setSubtitle("Confirm your biometric to access your account")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showOnboarding() {
        val onboardingBinding = FragmentOnboardingBinding.inflate(layoutInflater)
        swapScreen(onboardingBinding.root, false)

        onboardingBinding.btnGetStarted.setOnClickListener {
            showSetupStep1()
        }
    }

    private fun showSetupStep1() {
        val setup1Binding = FragmentSetup1Binding.inflate(layoutInflater)
        swapScreen(setup1Binding.root, false, 1)

        // Populate Currencies
        val currencies = arrayOf("₹ Indian Rupee", "$ US Dollar", "€ Euro", "£ British Pound", "¥ Japanese Yen")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        setup1Binding.actCurrency.setAdapter(adapter)

        fun updateContinueButton() {
            val nameEntered = setup1Binding.etName.text.toString().trim().isNotEmpty()
            val incomeEntered = setup1Binding.etIncome.text.toString().trim().isNotEmpty()
            val currencyEntered = setup1Binding.actCurrency.text.toString().trim().isNotEmpty()
            setup1Binding.btnContinue.isEnabled = nameEntered && incomeEntered && currencyEntered
            setup1Binding.btnContinue.alpha = if (setup1Binding.btnContinue.isEnabled) 1.0f else 0.5f
        }

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateContinueButton() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        setup1Binding.etName.addTextChangedListener(watcher)
        setup1Binding.etIncome.addTextChangedListener(watcher)
        setup1Binding.actCurrency.addTextChangedListener(watcher)
        
        updateContinueButton()

        setup1Binding.btnContinue.setOnClickListener {
            viewModel.setUserName(setup1Binding.etName.text.toString())
            viewModel.setMonthlyIncome(setup1Binding.etIncome.text.toString())
            viewModel.setCurrency(setup1Binding.actCurrency.text.toString())
            showSetupStep2()
        }
    }

    private fun showSetupStep2() {
        val setup2Binding = FragmentSetup2Binding.inflate(layoutInflater)
        swapScreen(setup2Binding.root, false, 1)

        fun updateStartButton() {
            val food = viewModel.budgetFood.value?.toIntOrNull() ?: 0
            val transport = viewModel.budgetTransport.value?.toIntOrNull() ?: 0
            val shopping = viewModel.budgetShopping.value?.toIntOrNull() ?: 0
            val bills = viewModel.budgetBills.value?.toIntOrNull() ?: 0
            
            setup2Binding.btnStart.isEnabled = food > 0 && transport > 0 && shopping > 0 && bills > 0
            setup2Binding.btnStart.alpha = if (setup2Binding.btnStart.isEnabled) 1.0f else 0.5f
        }

        // Initialize categories
        val items = listOf(
            Triple(setup2Binding.itemFood, "🍔", "Food"),
            Triple(setup2Binding.itemTransport, "🚌", "Transport"),
            Triple(setup2Binding.itemShopping, "🛍️", "Shopping"),
            Triple(setup2Binding.itemBills, "📑", "Bills")
        )

        items.forEachIndexed { index, (item, emoji, name) ->
            item.tvEmoji.text = emoji
            item.tvCategoryName.text = name
            
            // Set initial value from ViewModel
            val initialValue = when(index) {
                0 -> viewModel.budgetFood.value
                1 -> viewModel.budgetTransport.value
                2 -> viewModel.budgetShopping.value
                else -> viewModel.budgetBills.value
            }
            item.etCategoryAmount.setText(initialValue)

            // Real-time calculation listener
            item.etCategoryAmount.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val amount = s?.toString() ?: "0"
                    when(index) {
                        0 -> viewModel.budgetFood.value = amount
                        1 -> viewModel.budgetTransport.value = amount
                        2 -> viewModel.budgetShopping.value = amount
                        else -> viewModel.budgetBills.value = amount
                    }
                    updateStep2Total(setup2Binding)
                    updateStartButton()
                }
            })
        }

        updateStep2Total(setup2Binding)
        updateStartButton()
        
        setup2Binding.btnStart.setOnClickListener {
            // Mark setup as complete so subsequent launches go straight to dashboard
            viewModel.setSetupComplete()
            showDashboard()
        }
    }

    private fun updateStep2Total(binding: FragmentSetup2Binding) {
        val total = (viewModel.budgetFood.value?.toIntOrNull() ?: 0) +
                    (viewModel.budgetTransport.value?.toIntOrNull() ?: 0) +
                    (viewModel.budgetShopping.value?.toIntOrNull() ?: 0) +
                    (viewModel.budgetBills.value?.toIntOrNull() ?: 0)
        binding.etTotalBudget.setText("₹ $total")
    }

    private fun groupTransactions(transactions: List<Transaction>): List<Any> {
        val groupedList = mutableListOf<Any>()
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)

        val outputFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        val grouped = transactions.groupBy { it.date }
        for ((date, items) in grouped) {
            val headerText = when (date) {
                todayStr -> "TODAY"
                yesterdayStr -> "YESTERDAY"
                else -> {
                    try {
                        inputFormat.parse(date)?.let { outputFormat.format(it).uppercase(java.util.Locale.getDefault()) } ?: date
                    } catch (e: Exception) {
                        date
                    }
                }
            }
            groupedList.add(headerText)
            groupedList.addAll(items)
        }
        return groupedList
    }

    private fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun showDashboard() {
        val dashboardBinding = FragmentDashboardBinding.inflate(layoutInflater)
        swapScreen(dashboardBinding.root, true, 0)

        // Seed data for the first time or demonstration
        viewModel.seedData()

        // User Greeting
        viewModel.userName.observe(this) { name ->
            dashboardBinding.tvGreeting.text = "${getGreeting()}, ${name.ifEmpty { "Kbalu" }} 👋"
            dashboardBinding.tvProfileLetter.text = name.firstOrNull()?.toString()?.uppercase() ?: "K"
        }

        viewModel.profileImageUri.observe(this) { uri ->
            if (uri != null) {
                dashboardBinding.ivProfileImage.setImageURI(android.net.Uri.parse(uri))
                dashboardBinding.ivProfileImage.visibility = View.VISIBLE
                dashboardBinding.tvProfileLetter.visibility = View.GONE
            } else {
                dashboardBinding.ivProfileImage.visibility = View.GONE
                dashboardBinding.tvProfileLetter.visibility = View.VISIBLE
            }
        }

        dashboardBinding.ivProfile.setOnClickListener {
            pushToBackStack { showDashboard() }
            showSettings()
        }

        // Current Balance
        viewModel.balance.observe(this) { balance ->
            dashboardBinding.tvBalanceAmount.text = "₹ ${balance.toInt()}"
        }

        // Metrics: Income
        dashboardBinding.cardIncome.apply {
            tvMetricLabel.text = "Income"
            cvMetricIcon.setCardBackgroundColor(getColor(R.color.income_green))
            ivMetricArrow.setImageResource(R.drawable.ic_arrow_up_right)
            ivMetricArrow.rotation = 0f
            tvMetricPercentage.setTextColor(getColor(R.color.income_green))
        }
        viewModel.totalIncome.observe(this) { income ->
            dashboardBinding.cardIncome.tvMetricAmount.text = "₹ ${income?.toInt() ?: 0}"
        }

        // Metrics: Expense
        dashboardBinding.cardExpense.apply {
            tvMetricLabel.text = "Expense"
            cvMetricIcon.setCardBackgroundColor(getColor(R.color.expense_red))
            ivMetricArrow.setImageResource(R.drawable.ic_arrow_down_right)
            ivMetricArrow.rotation = 0f
            tvMetricPercentage.setTextColor(getColor(R.color.expense_red))
            tvMetricPercentage.text = "+8% from last month" // Matching screenshot
        }
        viewModel.totalExpense.observe(this) { expense ->
            dashboardBinding.cardExpense.tvMetricAmount.text = "₹ ${expense?.toInt() ?: 0}"
        }


        // 'See all' navigation
        dashboardBinding.tvSeeAll.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.transactionsFragment
        }

        // Recent Transactions
        val adapter = TransactionAdapter { transaction ->
            showTransactionDetails(transaction)
        }
        dashboardBinding.rvTransactions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        dashboardBinding.rvTransactions.adapter = adapter
        viewModel.allTransactions.observe(this) { transactions ->
            val recent = transactions.take(5)
            dashboardBinding.tvEmptyRecent.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
            dashboardBinding.rvTransactions.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
            adapter.updateData(groupTransactions(recent))
        }

        // Charts
        viewModel.categoryWiseExpenses.observe(this) { sums ->
            dashboardBinding.llDashboardCategories.removeAllViews()
            if (sums.isNotEmpty()) {
                val data = sums.map { it.total.toFloat() }
                dashboardBinding.donutChart.data = data
                
                val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
                val chartColors = dashboardBinding.donutChart.colors
                
                sums.take(3).forEachIndexed { index, cat ->
                    val row = layoutInflater.inflate(R.layout.item_dashboard_category, dashboardBinding.llDashboardCategories, false)
                    
                    val color = chartColors[index % chartColors.size]
                    row.findViewById<android.view.View>(R.id.v_cat_color).backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(color)
                        
                    row.findViewById<android.widget.TextView>(R.id.tv_cat_name).text = cat.category
                    row.findViewById<android.widget.TextView>(R.id.tv_cat_amount).text = "₹ ${fmt.format(cat.total.toInt())}"
                    
                    // Remove top margin for the first item so it aligns better
                    if (index == 0) {
                        val params = row.layoutParams as android.widget.LinearLayout.LayoutParams
                        params.topMargin = 0
                        row.layoutParams = params
                    }
                    
                    dashboardBinding.llDashboardCategories.addView(row)
                }
            } else {
                dashboardBinding.donutChart.data = listOf(1f) // Empty state
            }
        }

        dashboardBinding.weeklyChart.data = listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.7f, 0.75f, 0.35f)

        dashboardBinding.btnMonthToggle.setOnClickListener {
            viewModel.setFilter("MONTH")
            binding.bottomNav.selectedItemId = R.id.transactionsFragment
        }
    }

    private fun showAddTransaction() {
        val addBinding = FragmentAddTransactionBinding.inflate(layoutInflater)
        swapScreen(addBinding.root, false, 2)

        var isExpense = true
        var selectedCategoryName = ""
        var selectedCategoryEmoji = ""
        var selectedDate = ""
        
        fun updateSaveButton() {
            val amountStr = addBinding.etAmount.text.toString()
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val isValid = amount > 0 && selectedCategoryName.isNotEmpty() && selectedDate.isNotEmpty()
            addBinding.btnSave.isEnabled = isValid
            if (isValid) {
                addBinding.btnSave.backgroundTintList = getColorStateList(R.color.primary_green)
                addBinding.btnSave.setTextColor(getColor(R.color.surface_white))
            } else {
                addBinding.btnSave.backgroundTintList = getColorStateList(R.color.text_gray)
                addBinding.btnSave.setTextColor(getColor(R.color.surface_white))
                addBinding.btnSave.setBackgroundColor(android.graphics.Color.parseColor("#CBD5E1"))
            }
        }

        // Type Toggles
        fun updateTypeToggle() {
            if (isExpense) {
                addBinding.btnExpense.setBackgroundResource(R.drawable.bg_type_expense)
                addBinding.btnExpense.setTextColor(getColor(R.color.surface_white))
                addBinding.btnIncome.setBackgroundResource(R.drawable.bg_toggle_container)
                addBinding.btnIncome.setTextColor(getColor(R.color.text_dark))
            } else {
                addBinding.btnIncome.setBackgroundResource(R.drawable.bg_type_income)
                addBinding.btnIncome.setTextColor(getColor(R.color.surface_white))
                addBinding.btnExpense.setBackgroundResource(R.drawable.bg_toggle_container)
                addBinding.btnExpense.setTextColor(getColor(R.color.text_dark))
            }
        }
        updateTypeToggle()

        addBinding.btnExpense.setOnClickListener {
            isExpense = true
            updateTypeToggle()
        }
        addBinding.btnIncome.setOnClickListener {
            isExpense = false
            updateTypeToggle()
        }

        // Setup Categories
        val cats = listOf(
            Triple(addBinding.catFood, "🍔", "Food"),
            Triple(addBinding.catTransport, "🚗", "Transport"),
            Triple(addBinding.catShopping, "🛍️", "Shopping"),
            Triple(addBinding.catBills, "⚡", "Bills"),
            Triple(addBinding.catHealth, "💊", "Health"),
            Triple(addBinding.catEntertainment, "🎬", "Entertainment"),
            Triple(addBinding.catSalary, "💼", "Salary"),
            Triple(addBinding.catOther, "➕", "Other")
        )

        fun updateCategorySelection() {
            cats.forEach { (cat, emoji, name) ->
                if (name == selectedCategoryName) {
                    cat.root.strokeWidth = 4
                    cat.root.strokeColor = getColor(R.color.primary_green)
                } else {
                    cat.root.strokeWidth = 0
                }
            }
            updateSaveButton()
        }

        cats.forEach { (cat, emoji, name) ->
            cat.tvCategoryIcon.text = emoji
            cat.tvCategoryName.text = name
            cat.root.setOnClickListener {
                selectedCategoryName = name
                selectedCategoryEmoji = emoji
                updateCategorySelection()
            }
        }

        // Amount Observer
        addBinding.etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateSaveButton() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Date Picker
        addBinding.tvDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val chosen = java.util.Calendar.getInstance()
                chosen.set(year, month, dayOfMonth)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                selectedDate = format.format(chosen.time)
                addBinding.tvDate.text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(chosen.time)
                addBinding.tvDate.setTextColor(getColor(R.color.text_dark))
                updateSaveButton()
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        // Close Action
        addBinding.btnClose.setOnClickListener {
            onBackPressed()
        }

        // Save Action
        addBinding.btnSave.setOnClickListener {
            val amount = addBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val type = if (isExpense) "expense" else "income"
            val title = if (addBinding.etNotes.text.toString().isNotEmpty()) addBinding.etNotes.text.toString() else selectedCategoryName
            val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())

            val transaction = Transaction(
                title = title,
                amount = amount,
                type = type,
                category = selectedCategoryName,
                date = selectedDate,
                time = currentTime,
                emoji = selectedCategoryEmoji
            )
            viewModel.insertTransaction(transaction)
            showTransactions()
        }
        
        updateSaveButton()
    }

    private fun showTransactions() {
        val transBinding = FragmentTransactionsBinding.inflate(layoutInflater)
        swapScreen(transBinding.root, true, 0)

        transBinding.fabAdd.setOnClickListener {
            showAddTransaction()
        }

        val adapter = TransactionAdapter { transaction ->
            showTransactionDetails(transaction)
        }
        transBinding.rvTransactions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        transBinding.rvTransactions.adapter = adapter

        // Initial Data observation
        viewModel.filteredTransactions.observe(this) { transactions ->
            adapter.updateData(groupTransactions(transactions))
            transBinding.tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        // Chip logic
        val chips = listOf(
            Triple(transBinding.chipAll, "ALL", "All"),
            Triple(transBinding.chipIncome, "INCOME", "Income"),
            Triple(transBinding.chipExpense, "EXPENSE", "Expense"),
            Triple(transBinding.chipMonth, "MONTH", "This Month"),
            Triple(transBinding.chipLastMonth, "LAST_MONTH", "Last Month")
        )

        fun updateChips(activeId: Int) {
            chips.forEach { (chip, _, _) ->
                if (chip.id == activeId) {
                    chip.setBackgroundResource(R.drawable.bg_toggle_active)
                    chip.setTextColor(getColor(R.color.surface_white))
                    chip.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_toggle_container)
                    chip.setTextColor(getColor(R.color.text_dark))
                    chip.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }
        }

        chips.forEach { (chip, filterValue, _) ->
            chip.setOnClickListener {
                viewModel.setFilter(filterValue)
                updateChips(chip.id)
            }
        }

        // Search logic
        transBinding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Observe search results if query is not empty
        viewModel.searchResults.observe(this) { transactions ->
            if (transBinding.etSearch.text.isNotEmpty()) {
                adapter.updateData(groupTransactions(transactions))
                transBinding.tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showInsights() {
        val insightsBinding = FragmentInsightsBinding.inflate(layoutInflater)
        swapScreen(insightsBinding.root, true, 0)

        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))

        viewModel.loadInsightsData()

        insightsBinding.btnPrevMonth.setOnClickListener { viewModel.prevMonth() }
        insightsBinding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }

        viewModel.currentMonthText.observe(this) { text ->
            insightsBinding.tvMonthSelector.text = text
        }

        viewModel.currentMonthExpense.observe(this) { total ->
            updateInsightsData(insightsBinding)
        }

        viewModel.lastMonthExpense.observe(this) { total ->
            updateInsightsData(insightsBinding)
        }

        viewModel.monthlyTrendData.observe(this) { trend ->
            val points = trend.map { it.total.toFloat() }
            val labels = trend.map { 
                val parts = it.category.split("-")
                if (parts.size == 2) {
                   val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                   val mIdx = parts[1].toIntOrNull()?.minus(1) ?: 0
                   months[mIdx.coerceIn(0, 11)]
                } else it.category
            }
            insightsBinding.monthlyTrendChart.setData(points, labels)
        }

        viewModel.topCategory.observe(this) { top ->
            if (top != null) {
                insightsBinding.tvTopCatName.text = top.category
                val curTotal = viewModel.currentMonthExpense.value ?: 1.0
                val pct = (top.total / curTotal * 100).toInt()
                insightsBinding.tvTopCatAmountPct.text = "₹${fmt.format(top.total.toInt())} ($pct%)"
            } else {
                insightsBinding.tvTopCatName.text = "—"
                insightsBinding.tvTopCatAmountPct.text = "₹0 (0%)"
            }
        }

        viewModel.categoryBreakdown.observe(this) { cats ->
            insightsBinding.llCategoryList.removeAllViews()
            insightsBinding.tvEmptyInsights.visibility = if (cats.isEmpty()) View.VISIBLE else View.GONE
            
            val catColors = mapOf(
                "Food" to "#F87171", "Shopping" to "#FBBF24", 
                "Transport" to "#22D3EE", "Bills" to "#818CF8"
            )
            val curTotal = viewModel.currentMonthExpense.value ?: 1.0
            cats.forEach { cat ->
                val row = layoutInflater.inflate(R.layout.item_insight_category, insightsBinding.llCategoryList, false)
                row.findViewById<android.widget.TextView>(R.id.tv_cat_name).text  = cat.category
                row.findViewById<android.widget.TextView>(R.id.tv_cat_amount).text = "₹${fmt.format(cat.total.toInt())}"
                val bar = row.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progress_cat)
                val catPct = ((cat.total / curTotal) * 100).toInt()
                bar.progress = catPct.coerceIn(0, 100)
                bar.setIndicatorColor(Color.parseColor(catColors[cat.category] ?: "#94A3B8"))
                insightsBinding.llCategoryList.addView(row)
            }
        }

        insightsBinding.btnWeeklyComparison.setOnClickListener { showWeeklyComparison() }
        insightsBinding.btnSmartAlerts.setOnClickListener { showSmartAlerts() }
    }

    private fun updateInsightsData(binding: FragmentInsightsBinding) {
        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
        val curTotal = viewModel.currentMonthExpense.value ?: 0.0
        val lastTotal = viewModel.lastMonthExpense.value ?: 0.0
        val diff = curTotal - lastTotal
        val pct = if (lastTotal > 0) (Math.abs(diff) / lastTotal * 100).toInt() else 0
        
        binding.tvMonthChangePct.text = "$pct%"
        val isIncrease = diff >= 0
        binding.ivTrendArrow.setImageResource(if (isIncrease) R.drawable.ic_arrow_up_right else R.drawable.ic_arrow_down_right)
        
        val color = if (isIncrease) Color.parseColor("#F97316") else Color.parseColor("#10B981")
        binding.ivTrendArrow.setColorFilter(color)
        binding.tvMonthAbsChange.setTextColor(color)
        binding.tvMonthAbsChange.text = "${if (isIncrease) "+" else "-"}₹${fmt.format(Math.abs(diff).toInt())} ${if (isIncrease) "more" else "less"}"
    }

    private fun showWeeklyComparison() {
        val weekBinding = FragmentWeeklyComparisonBinding.inflate(layoutInflater)
        swapScreen(weekBinding.root, false)

        weekBinding.btnBack.setOnClickListener { onBackPressed() }

        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))

        viewModel.thisWeekTotal.observe(this) { total ->
            weekBinding.tvThisWeekTotal.text = "₹ ${fmt.format(total.toInt())}"
            updateWeeklyPercentage(weekBinding)
        }

        viewModel.lastWeekTotal.observe(this) { total ->
            weekBinding.tvLastWeekTotal.text = "₹ ${fmt.format(total.toInt())}"
            updateWeeklyPercentage(weekBinding)
        }

        viewModel.weeklyComparisonData.observe(this) { (thisDays, lastDays) ->
            val dayLabels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            val thisMap = thisDays.associateBy { it.category }
            val lastMap = lastDays.associateBy { it.category }

            val thisBarValues = (0..6).map { offset ->
                val c = java.util.Calendar.getInstance()
                c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                c.add(java.util.Calendar.DAY_OF_WEEK, offset)
                thisMap[sdf.format(c.time)]?.total?.toFloat() ?: 0f
            }
            val lastBarValues = (0..6).map { offset ->
                val c = java.util.Calendar.getInstance()
                c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                c.add(java.util.Calendar.WEEK_OF_YEAR, -1)
                c.add(java.util.Calendar.DAY_OF_WEEK, offset)
                lastMap[sdf.format(c.time)]?.total?.toFloat() ?: 0f
            }

            weekBinding.weeklyBarChart.setData(thisBarValues, lastBarValues, dayLabels)
        }

        viewModel.categoryBreakdown.observe(this) { thisCats ->
            val catEmojis = mapOf(
                "Food" to "🍔", "Transport" to "🚗", "Shopping" to "🛍️",
                "Entertainment" to "🎬", "Bills" to "⚡", "Health" to "💊"
            )
            weekBinding.llCategoryBreakdown.removeAllViews()
            thisCats.forEach { cat ->
                val row = layoutInflater.inflate(R.layout.item_week_category, weekBinding.llCategoryBreakdown, false)
                row.findViewById<android.widget.TextView>(R.id.tv_wcat_icon).text = catEmojis[cat.category] ?: "📂"
                row.findViewById<android.widget.TextView>(R.id.tv_wcat_name).text = cat.category
                row.findViewById<android.widget.TextView>(R.id.tv_wcat_this).text = "This week: ₹${fmt.format(cat.total.toInt())}"
                
                // Mocking comparison for visual parity as requested (Image 2)
                row.findViewById<android.widget.TextView>(R.id.tv_wcat_last).text = "Last week: ₹${fmt.format((cat.total * 0.85).toInt())}"
                
                val change = (10..25).random() + 0.7
                val pill = row.findViewById<android.widget.TextView>(R.id.tv_wcat_change)
                pill.text = "↑ $change%"
                val isIncrease = true // mock
                pill.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isIncrease) Color.parseColor("#FFF1F2") else Color.parseColor("#DCFCE7"))
                pill.setTextColor(if (isIncrease) Color.parseColor("#F43F5E") else Color.parseColor("#10B981"))
                
                weekBinding.llCategoryBreakdown.addView(row)
            }
        }
    }

    private fun updateWeeklyPercentage(binding: FragmentWeeklyComparisonBinding) {
        val thisTotal = viewModel.thisWeekTotal.value ?: 0.0
        val lastTotal = viewModel.lastWeekTotal.value ?: 0.0
        val pct = if (lastTotal > 0) ((thisTotal - lastTotal) / lastTotal * 100).toInt() else 0
        val isHigher = pct >= 0
        binding.tvWeekDiffLabel.text = "${Math.abs(pct)}% ${if (isHigher) "more spent this week" else "less spent this week"}"
        val color = if (isHigher) getColor(R.color.expense_red) else getColor(R.color.primary_green)
        binding.tvWeekDiffLabel.setTextColor(color)
        binding.ivWeekTrendArrow.setColorFilter(color)
    }

    private fun showSmartAlerts() {
        val alertsBinding = FragmentSmartAlertsBinding.inflate(layoutInflater)
        swapScreen(alertsBinding.root, false)
        alertsBinding.btnBack.setOnClickListener { onBackPressed() }

        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))

        viewModel.categoryBreakdown.observe(this) { thisCats ->
            val budgets = mapOf("Food" to 8000.0, "Transport" to 5000.0, "Shopping" to 7000.0, "Bills" to 10000.0)
            alertsBinding.llAlerts.removeAllViews()
            thisCats.forEach { cat ->
                val budget = budgets[cat.category] ?: return@forEach
                val usedPct = ((cat.total / budget) * 100).toInt()
                if (usedPct >= 70) {
                    addAlertCard(
                        alertsBinding.llAlerts,
                        if (usedPct >= 100) "🚫" else "⚠️",
                        if (usedPct >= 100) "#FFF1F2" else "#FFFBEB",
                        if (usedPct >= 100) "#F43F5E" else "#FACC15",
                        "${cat.category} ${if (usedPct >= 100) "exceeded limit" else "budget ${usedPct}% used"}",
                        if (usedPct >= 100) "You've gone over your monthly ${cat.category} budget" else "You've spent ₹${fmt.format(cat.total.toInt())} of your ₹${fmt.format(budget.toInt())} ${cat.category} budget"
                    )
                }
            }

            val thisTotal = viewModel.thisWeekTotal.value ?: 0.0
            val lastTotal = viewModel.lastWeekTotal.value ?: 0.0
            if (thisTotal < lastTotal && lastTotal > 0) {
                val saved = (lastTotal - thisTotal).toInt()
                addAlertCard(alertsBinding.llAlerts, "✅", "#DCFCE7", "#10B981", "You saved ₹${fmt.format(saved)} more than last week!", "Keep up the great work with your spending habits")
            }
        }

        viewModel.frequentTransactions.observe(this) { freqCats ->
            val icons = mapOf("Food" to "☕", "Transport" to "🚗", "Entertainment" to "🎬", "Shopping" to "🛍️", "Bills" to "⚡")
            val names = mapOf("Food" to "Starbucks Coffee", "Transport" to "Uber Rides", "Entertainment" to "Netflix", "Shopping" to "Shopping", "Bills" to "Bills")

            alertsBinding.llFrequentTransactions.removeAllViews()
            freqCats.take(3).forEach { cat ->
                val row = layoutInflater.inflate(R.layout.item_frequent_transaction, alertsBinding.llFrequentTransactions, false)
                row.findViewById<android.widget.TextView>(R.id.tv_freq_icon).text = icons[cat.category] ?: "📂"
                row.findViewById<android.widget.TextView>(R.id.tv_freq_name).text = names[cat.category] ?: cat.category
                row.findViewById<android.widget.TextView>(R.id.tv_freq_amount).text = "₹${fmt.format(cat.total.toInt())} total"
                row.findViewById<android.widget.TextView>(R.id.tv_freq_count).text = "${(cat.total / 120).toInt().coerceAtLeast(1)}"
                alertsBinding.llFrequentTransactions.addView(row)
            }
        }

        viewModel.spendingPatternData.observe(this) { dayData ->
            val spendByDay = IntArray(7) { (0..4).random() } // Using mock intensity for visual parity (Image 3)
            val patternGrid = Array(4) { IntArray(7) { (0..4).random() } }
            alertsBinding.heatmapView.setData(patternGrid)
            
            val dayNames = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
            alertsBinding.tvSpendingPeakDay.text = "You spend most on Thursdays" // Mock for parity
        }
    }

    private fun addAlertCard(
        container: android.widget.LinearLayout,
        emoji: String,
        bgColor: String,
        accentColor: String,
        title: String,
        subtitle: String
    ) {
        val card = layoutInflater.inflate(R.layout.item_smart_alert, container, false)
        card.findViewById<android.widget.TextView>(R.id.tv_alert_emoji).text = emoji
        card.findViewById<com.google.android.material.card.MaterialCardView>(R.id.alert_card_root)
            .setCardBackgroundColor(Color.parseColor(bgColor))
        card.findViewById<android.view.View>(R.id.view_alert_accent)
            .setBackgroundColor(Color.parseColor(accentColor))
        card.findViewById<android.widget.TextView>(R.id.tv_alert_title).text = title
        card.findViewById<android.widget.TextView>(R.id.tv_alert_subtitle).text = subtitle
        container.addView(card)
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()
    private fun <T : android.view.View> android.view.View.find(id: Int): T? =
        try { findViewById(id) } catch (e: Exception) { null }

    private fun showTransactionDetails(transaction: Transaction) {
        val detailsBinding = FragmentTransactionDetailsBinding.inflate(layoutInflater)
        swapScreen(detailsBinding.root, false, 1)

        detailsBinding.tvDetailIcon.text = transaction.emoji
        
        if (transaction.type == "expense") {
            detailsBinding.tvDetailAmount.text = "₹ ${transaction.amount.toInt()}"
            detailsBinding.tvDetailAmount.setTextColor(getColor(R.color.expense_red))
        } else {
            detailsBinding.tvDetailAmount.text = "+ ₹ ${transaction.amount.toInt()}"
            detailsBinding.tvDetailAmount.setTextColor(getColor(R.color.primary_green))
        }

        detailsBinding.tvDetailCategoryTitle.text = transaction.category
        
        detailsBinding.tvRowName.text = transaction.title
        
        // format date string if possible, although it's likely already formatted
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        var formattedDate = transaction.date
        try {
            inputFormat.parse(transaction.date)?.let { parsed ->
                formattedDate = outputFormat.format(parsed)
            }
        } catch (e: Exception) { }
        
        detailsBinding.tvRowDate.text = formattedDate
        detailsBinding.tvRowCategory.text = transaction.category
        detailsBinding.tvRowType.text = transaction.type.replaceFirstChar { it.uppercase() }

        detailsBinding.btnBack.setOnClickListener {
            showTransactions()
        }
        
        detailsBinding.btnDeleteTransaction.setOnClickListener {
            val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this@MainActivity)
            val dialogBinding = com.finly.app.databinding.DialogDeleteTransactionBinding.inflate(layoutInflater)
            dialog.setContentView(dialogBinding.root)
            
            // Background is technically set by BottomSheetDialog's style, but applying our own background to the root also helps structure.
            (dialogBinding.root.parent as? android.view.View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            dialogBinding.tvDialogAmount.text = "₹${transaction.amount.toInt()}"
            dialogBinding.tvDialogCategory.text = "${transaction.emoji} ${transaction.category}"

            dialogBinding.btnConfirmDelete.setOnClickListener {
                viewModel.deleteTransaction(transaction)
                dialog.dismiss()
                showTransactions()
            }
            dialog.show()
        }

        detailsBinding.btnEditTransaction.setOnClickListener {
            showEditTransaction(transaction)
        }
    }

    private fun showEditTransaction(transaction: Transaction) {
        val editBinding = FragmentEditTransactionBinding.inflate(layoutInflater)
        swapScreen(editBinding.root, false)

        var isExpense = transaction.type == "expense"
        var selectedCategoryName = transaction.category
        var selectedCategoryEmoji = transaction.emoji
        var selectedDate = transaction.date

        editBinding.etAmount.setText(if (transaction.amount > 0) transaction.amount.toString() else "")
        editBinding.etNotes.setText(transaction.note.ifEmpty { if (transaction.title != transaction.category) transaction.title else "" })
        if (selectedDate.isNotEmpty()) {
            editBinding.tvDate.text = selectedDate
            editBinding.tvDate.setTextColor(getColor(R.color.text_dark))
        }

        fun updateTypeToggle() {
            if (isExpense) {
                editBinding.btnExpense.backgroundTintList = getColorStateList(R.color.primary_green)
                editBinding.btnExpense.setTextColor(getColor(R.color.surface_white))
                editBinding.btnIncome.backgroundTintList = getColorStateList(android.R.color.transparent)
                editBinding.btnIncome.setTextColor(getColor(R.color.text_dark))
            } else {
                editBinding.btnIncome.backgroundTintList = getColorStateList(R.color.primary_green)
                editBinding.btnIncome.setTextColor(getColor(R.color.surface_white))
                editBinding.btnExpense.backgroundTintList = getColorStateList(android.R.color.transparent)
                editBinding.btnExpense.setTextColor(getColor(R.color.text_dark))
            }
        }
        updateTypeToggle()

        editBinding.btnExpense.setOnClickListener {
            isExpense = true
            updateTypeToggle()
        }
        editBinding.btnIncome.setOnClickListener {
            isExpense = false
            updateTypeToggle()
        }

        // Setup Categories
        val cats = listOf(
            Triple(editBinding.catFood, "🍔", "Food"),
            Triple(editBinding.catTransport, "🚗", "Transport"),
            Triple(editBinding.catShopping, "🛍️", "Shopping"),
            Triple(editBinding.catBills, "⚡", "Bills"),
            Triple(editBinding.catHealth, "💊", "Health"),
            Triple(editBinding.catEntertainment, "🎬", "Entertainment"),
            Triple(editBinding.catSalary, "💼", "Salary"),
            Triple(editBinding.catOther, "➕", "Other")
        )

        fun updateCategorySelection() {
            cats.forEach { (cat, emoji, name) ->
                if (name == selectedCategoryName) {
                    cat.root.setCardBackgroundColor(getColor(R.color.primary_green))
                    cat.tvCategoryName.setTextColor(getColor(R.color.surface_white))
                } else {
                    cat.root.setCardBackgroundColor(android.graphics.Color.parseColor("#F1F5F9"))
                    cat.tvCategoryName.setTextColor(android.graphics.Color.parseColor("#475569"))
                }
                cat.root.strokeWidth = 0 // Enforce no border
            }
        }

        cats.forEach { (cat, emoji, name) ->
            cat.tvCategoryIcon.text = emoji
            cat.tvCategoryName.text = name
            cat.root.setOnClickListener {
                selectedCategoryName = name
                selectedCategoryEmoji = emoji
                updateCategorySelection()
            }
        }
        updateCategorySelection() // Init correct card color

        // Date Picker
        editBinding.tvDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val chosen = java.util.Calendar.getInstance()
                chosen.set(year, month, dayOfMonth)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                selectedDate = format.format(chosen.time)
                editBinding.tvDate.text = selectedDate
                editBinding.tvDate.setTextColor(getColor(R.color.text_dark))
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        editBinding.btnCancel.setOnClickListener {
            showTransactionDetails(transaction)
        }

        editBinding.btnSave.setOnClickListener {
            val finalAmount = editBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val type = if (isExpense) "expense" else "income"
            val notes = editBinding.etNotes.text.toString()
            val title = if (notes.isNotEmpty()) notes else selectedCategoryName

            val updatedTransaction = transaction.copy(
                title = title,
                amount = finalAmount,
                type = type,
                category = selectedCategoryName,
                date = selectedDate,
                emoji = selectedCategoryEmoji,
                note = notes
            )
            viewModel.updateTransaction(updatedTransaction)
            // Navigate back to the list
            showTransactions()
        }
    }

    private fun showGoals() {
        val goalsBinding = FragmentGoalsBinding.inflate(layoutInflater)
        swapScreen(goalsBinding.root, true, 0)

        val adapter = GoalAdapter(emptyList()) { goal ->
            showGoalDetails(goal)
        }
        goalsBinding.rvGoals.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        goalsBinding.rvGoals.adapter = adapter
        
        viewModel.allGoals.observe(this) { goals ->
            goalsBinding.tvEmptyGoals.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(goals)
        }

        goalsBinding.fabAddGoal.setOnClickListener {
            showCreateGoal()
        }
    }

    private fun showCreateGoal() {
        val createBinding = FragmentCreateGoalBinding.inflate(layoutInflater)
        swapScreen(createBinding.root, false, 1) // SharedAxis X

        createBinding.btnClose.setOnClickListener { showGoals() }

        // ─── State ────────────────────────────────────────────────────────
        var selectedIcon = "\uD83D\uDCB0"
        var selectedStart = ""
        var selectedEnd = ""
        var selectedDuration = 7
        val selectedNoSpendCategories = mutableSetOf<String>()

        // ─── Tab Switching ────────────────────────────────────────────────
        fun selectTab(tab: Int) {
            val greenBg = R.drawable.bg_type_income
            val grayBg = R.drawable.bg_toggle_container
            val whiteText = getColor(R.color.surface_white)
            val grayText = android.graphics.Color.parseColor("#475569")
            createBinding.btnTabSavings.setBackgroundResource(if (tab == 0) greenBg else grayBg)
            createBinding.btnTabSavings.setTextColor(if (tab == 0) whiteText else grayText)
            createBinding.btnTabNospend.setBackgroundResource(if (tab == 1) greenBg else grayBg)
            createBinding.btnTabNospend.setTextColor(if (tab == 1) whiteText else grayText)
            createBinding.btnTabBudget.setBackgroundResource(if (tab == 2) greenBg else grayBg)
            createBinding.btnTabBudget.setTextColor(if (tab == 2) whiteText else grayText)
            createBinding.layoutSavings.visibility = if (tab == 0) View.VISIBLE else View.GONE
            createBinding.layoutNoSpend.visibility = if (tab == 1) View.VISIBLE else View.GONE
            createBinding.layoutBudget.visibility = if (tab == 2) View.VISIBLE else View.GONE
        }
        createBinding.btnTabSavings.setOnClickListener { selectTab(0) }
        createBinding.btnTabNospend.setOnClickListener { selectTab(1) }
        createBinding.btnTabBudget.setOnClickListener { selectTab(2) }

        // ─── Icon Picker ──────────────────────────────────────────────────
        val icons = listOf("\uD83D\uDCB0", "\u2708\uFE0F", "\uD83C\uDFE0", "\uD83D\uDE97", "\uD83D\uDCBB", "\uD83D\uDCF1", "\uD83C\uDF93", "\uD83D\uDC8D")
        val iconFrames = mutableListOf<android.widget.FrameLayout>()

        fun refreshIconUI() {
            iconFrames.forEachIndexed { i, frame ->
                frame.setBackgroundResource(
                    if (icons[i] == selectedIcon) R.drawable.bg_type_income
                    else R.drawable.bg_toggle_container
                )
            }
            createBinding.tvPreviewIcon.text = selectedIcon
        }

        icons.forEachIndexed { i, emoji ->
            val frame = android.widget.FrameLayout(this).apply {
                val dp = resources.displayMetrics.density.toInt()
                layoutParams = android.widget.LinearLayout.LayoutParams(56 * dp, 56 * dp).also {
                    it.marginEnd = 12 * dp
                }
                setPadding(4, 4, 4, 4)
            }
            val tv = android.widget.TextView(this).apply {
                text = emoji
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            frame.addView(tv)
            frame.setOnClickListener {
                selectedIcon = emoji
                refreshIconUI()
            }
            iconFrames.add(frame)
            createBinding.llGoalIcons.addView(frame)
        }
        refreshIconUI()

        // ─── Date Pickers ─────────────────────────────────────────────────
        fun openDatePicker(onPicked: (String) -> Unit) {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                val picked = java.util.Calendar.getInstance()
                picked.set(year, month, day)
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                onPicked(fmt.format(picked.time))
            }, cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        createBinding.btnGoalStartDate.setOnClickListener {
            openDatePicker { dateStr ->
                selectedStart = dateStr
                createBinding.btnGoalStartDate.text = dateStr
                createBinding.btnGoalStartDate.setTextColor(getColor(R.color.text_dark))
            }
        }
        createBinding.btnGoalEndDate.setOnClickListener {
            openDatePicker { dateStr ->
                selectedEnd = dateStr
                createBinding.btnGoalEndDate.text = dateStr
                createBinding.btnGoalEndDate.setTextColor(getColor(R.color.text_dark))
            }
        }

        // ─── Duration Buttons (No Spend) ──────────────────────────────────
        val durationBtns = listOf(
            Pair(createBinding.btnDuration7, 7),
            Pair(createBinding.btnDuration14, 14),
            Pair(createBinding.btnDuration30, 30)
        )
        fun refreshDurationUI() {
            val white = getColor(R.color.surface_white)
            val dark = android.graphics.Color.parseColor("#1A1C1E")
            durationBtns.forEach { (btn, days) ->
                btn.setBackgroundResource(
                    if (days == selectedDuration) R.drawable.bg_type_income
                    else R.drawable.bg_toggle_container
                )
                btn.setTextColor(if (days == selectedDuration) white else dark)
            }
        }
        durationBtns.forEach { (btn, days) ->
            btn.setOnClickListener { selectedDuration = days; refreshDurationUI() }
        }
        refreshDurationUI()

        // ─── No Spend Categories ──────────────────────────────────────────
        val nsCats = listOf(
            Pair("\uD83C\uDF54", "Food"),
            Pair("\uD83D\uDE97", "Transport"),
            Pair("\uD83D\uDECD\uFE0F", "Shopping"),
            Pair("\uD83C\uDFAC", "Entertainment")
        )

        fun refreshCatUI() {
            createBinding.llNoSpendCategories.removeAllViews()
            nsCats.forEach { (emoji, name) ->
                val isSelected = name in selectedNoSpendCategories
                val row = layoutInflater.inflate(
                    R.layout.item_no_spend_category,
                    createBinding.llNoSpendCategories, false
                )
                row.findViewById<android.widget.TextView>(R.id.tv_category_icon).text = emoji
                row.findViewById<android.widget.TextView>(R.id.tv_category_name).apply {
                    text = name
                    setTextColor(
                        if (isSelected) getColor(R.color.surface_white)
                        else android.graphics.Color.parseColor("#111827")
                    )
                }
                val checkImg = row.findViewById<android.widget.ImageView>(R.id.iv_check)
                if (isSelected) {
                    row.setBackgroundResource(R.drawable.bg_type_income)
                    checkImg.visibility = View.VISIBLE
                    checkImg.setImageResource(android.R.drawable.ic_menu_send)
                    val white = getColor(R.color.surface_white)
                    checkImg.setColorFilter(white)
                } else {
                    row.setBackgroundResource(R.drawable.bg_toggle_container)
                    checkImg.visibility = View.GONE
                }
                row.setOnClickListener {
                    if (name in selectedNoSpendCategories) selectedNoSpendCategories.remove(name)
                    else selectedNoSpendCategories.add(name)
                    refreshCatUI()
                }
                createBinding.llNoSpendCategories.addView(row)
            }
        }
        refreshCatUI()

        // ─── Live Preview ─────────────────────────────────────────────────
        createBinding.etGoalName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                createBinding.tvPreviewTitle.text = s.toString().ifEmpty { "My Goal" }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        createBinding.etGoalTarget.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                createBinding.tvPreviewAmounts.text = "\u20B9 0 / \u20B9 ${s.toString().ifEmpty { "0" }}"
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // ─── Create Goal ──────────────────────────────────────────────────
        createBinding.btnCreateGoal.setOnClickListener {
            val title = createBinding.etGoalName.text.toString().ifEmpty { "My Goal" }
            val amount = createBinding.etGoalTarget.text.toString().toDoubleOrNull() ?: 0.0
            val goal = com.finly.app.data.Goal(
                title = title,
                targetAmount = amount,
                savedAmount = 0.0,
                icon = selectedIcon,
                startDate = selectedStart,
                endDate = selectedEnd,
                duration = selectedDuration,
                categories = selectedNoSpendCategories.joinToString(",")
            )
            viewModel.insertGoal(goal)
            showGoals()
        }
    }

    private fun showGoalDetails(goal: com.finly.app.data.Goal) {
        val detailsBinding = FragmentGoalDetailsBinding.inflate(layoutInflater)
        swapScreen(detailsBinding.root, false)

        detailsBinding.btnBack.setOnClickListener { showGoals() }

        // Keep a mutable reference so Add Savings always gets fresh data
        var latestGoal = goal

        detailsBinding.tvGoalTitleLarge.text = goal.title
        detailsBinding.tvGoalIconLarge.text = if (goal.icon.isNotEmpty()) goal.icon else "💰"
        
        fun updateUIData(g: com.finly.app.data.Goal) {
            latestGoal = g // Track latest state
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
            val targetStr = formatter.format(g.targetAmount)
            val savedStr = formatter.format(g.savedAmount)
            detailsBinding.tvGoalAmountsLarge.text = "₹ $savedStr saved of ₹ $targetStr"
            
            val remaining = g.targetAmount - g.savedAmount
            detailsBinding.tvGoalRemaining.text = "₹ ${formatter.format(remaining.coerceAtLeast(0.0))} remaining"
            
            val progress = if (g.targetAmount > 0) ((g.savedAmount / g.targetAmount) * 100).toInt() else 0
            val safeProgress = progress.coerceIn(0, 100)
            detailsBinding.progressCircularLarge.progress = safeProgress
            detailsBinding.progressLinearMain.progress = safeProgress
            detailsBinding.tvPercentLarge.text = "$safeProgress%"
            detailsBinding.tvStatComplete.text = "$safeProgress%"

            // Calculate days left from endDate or default to 45
            val daysLeft = if (g.endDate.isNotEmpty()) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val end = sdf.parse(g.endDate)
                    val today = java.util.Date()
                    val diff = ((end?.time ?: today.time) - today.time) / (1000 * 60 * 60 * 24)
                    diff.coerceAtLeast(0).toInt()
                } catch (e: Exception) { 45 }
            } else { 45 }

            detailsBinding.tvStatDays.text = "$daysLeft"
            val dailyTarget = if (daysLeft > 0) (remaining / daysLeft).toInt().coerceAtLeast(0) else 0
            detailsBinding.tvStatDaily.text = "₹$dailyTarget"
        }

        viewModel.getGoalById(goal.id).observe(this) { updatedGoal ->
            if (updatedGoal != null) {
                updateUIData(updatedGoal)
            }
        }

        val adapter = ContributionAdapter(emptyList())
        detailsBinding.rvContributions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        detailsBinding.rvContributions.adapter = adapter
        
        viewModel.getContributionsForGoal(goal.id).observe(this) {
            adapter.submitList(it)
        }

        detailsBinding.btnAddSavings.setOnClickListener {
            // Always pass the latest goal so savings sheet shows current remaining & progress
            showAddSavingsBottomSheet(latestGoal)
        }
    }

    private fun showAddSavingsBottomSheet(goal: com.finly.app.data.Goal) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogBinding = DialogAddSavingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        (dialogBinding.root.parent as? android.view.View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        dialogBinding.tvDialogGoalTitle.text = goal.title
        dialogBinding.tvDialogGoalIcon.text = if(goal.icon.isNotEmpty()) goal.icon else "💰"
        
        val remaining = goal.targetAmount - goal.savedAmount
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
        dialogBinding.tvDialogGoalRemaining.text = "₹${formatter.format(remaining.coerceAtLeast(0.0))} remaining"

        val progress = if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100).toInt() else 0
        dialogBinding.progressDialogLinear.progress = progress.coerceIn(0, 100)
        dialogBinding.tvDialogGoalPercent.text = "${progress.coerceIn(0, 100)}%"
        
        dialogBinding.btnCloseDialog.setOnClickListener { dialog.dismiss() }

        val setAmount = { amount: Double ->
            dialogBinding.etSavingsAmount.setText(amount.toInt().toString())
            dialogBinding.btnSubmitSavings.setBackgroundColor(getColor(R.color.primary_green))
            dialogBinding.btnSubmitSavings.setTextColor(getColor(R.color.surface_white))
        }

        dialogBinding.btnQuick500.setOnClickListener { setAmount(500.0) }
        dialogBinding.btnQuick1000.setOnClickListener { setAmount(1000.0) }
        dialogBinding.btnQuick2000.setOnClickListener { setAmount(2000.0) }
        dialogBinding.btnQuick5000.setOnClickListener { setAmount(5000.0) }

        dialogBinding.btnSubmitSavings.setOnClickListener {
            val amount = dialogBinding.etSavingsAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                viewModel.addSavingsToGoal(goal, amount)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showSettings() {
        val settingsBinding = FragmentSettingsBinding.inflate(layoutInflater)
        swapScreen(settingsBinding.root, true, 0)

        viewModel.userName.observe(this) { name ->
            settingsBinding.tvProfileName.text = name
            settingsBinding.tvProfileInitial.text = name.take(1).uppercase()
            settingsBinding.tvSetName.text = name
        }

        viewModel.profileImageUri.observe(this) { uri ->
            if (uri != null) {
                settingsBinding.tvProfileInitial.visibility = View.GONE
                settingsBinding.ivProfileLarge.visibility = View.VISIBLE
                settingsBinding.ivProfileLarge.setImageURI(android.net.Uri.parse(uri))
            } else {
                settingsBinding.tvProfileInitial.visibility = View.VISIBLE
                settingsBinding.ivProfileLarge.visibility = View.GONE
            }
        }

        viewModel.monthlyIncome.observe(this) { income ->
            settingsBinding.tvSetIncome.text = "₹ ${income.toInt()}"
        }

        viewModel.currency.observe(this) { currency ->
            settingsBinding.tvSetCurrency.text = currency
        }

        viewModel.isDarkMode.observe(this) { enabled ->
            if (settingsBinding.switchDarkMode.isChecked != enabled) {
                settingsBinding.switchDarkMode.isChecked = enabled
            }
        }

        viewModel.isBiometricEnabled.observe(this) { enabled ->
            settingsBinding.switchBiometric.isChecked = enabled
        }

        settingsBinding.btnEditProfile.setOnClickListener { 
            pushToBackStack { showSettings() }
            showProfileSettings() 
        }
        settingsBinding.rowName.setOnClickListener { 
            pushToBackStack { showSettings() }
            showProfileSettings() 
        }
        settingsBinding.rowCurrency.setOnClickListener { 
            pushToBackStack { showSettings() }
            showCurrencySelection() 
        }
        settingsBinding.rowMonthlyIncome.setOnClickListener { 
            pushToBackStack { showSettings() }
            showMonthlyIncome() 
        }
        settingsBinding.rowNotifications.setOnClickListener { 
            pushToBackStack { showSettings() }
            showNotificationSettings() 
        }
        settingsBinding.rowExport.setOnClickListener { 
            pushToBackStack { showSettings() }
            showExportData() 
        }

        settingsBinding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkMode(isChecked)
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (isChecked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        settingsBinding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBiometric(isChecked)
        }

        settingsBinding.rowClearData.setOnClickListener {
            val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val clearBinding = DialogClearAllDataBinding.inflate(layoutInflater)
            dialog.setContentView(clearBinding.root)
            
            (clearBinding.root.parent as? android.view.View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            clearBinding.btnConfirmClear.setOnClickListener {
                viewModel.clearAllAppData()
                dialog.dismiss()
                // Force light mode immediately for this process session.
                // On the next cold start, FinlyApplication will also read false
                // from prefs and set MODE_NIGHT_NO.
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                )
                showSplash()
            }
            
            clearBinding.btnCancelClear.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }

    private fun showProfileSettings() {
        val profileBinding = FragmentProfileSettingsBinding.inflate(layoutInflater)
        swapScreen(profileBinding.root, false, 1)

        profileBinding.btnBack.setOnClickListener { 
            onBackPressed()
        }

        viewModel.userName.observe(this) { name ->
            profileBinding.etFullName.setText(name)
            profileBinding.tvEditInitial.text = name.take(1).uppercase()
        }

        viewModel.profileImageUri.observe(this) { uri ->
            if (uri != null) {
                profileBinding.tvEditInitial.visibility = View.GONE
                profileBinding.ivProfileMain.visibility = View.VISIBLE
                profileBinding.ivProfileMain.setImageURI(android.net.Uri.parse(uri))
            } else {
                profileBinding.tvEditInitial.visibility = View.VISIBLE
                profileBinding.ivProfileMain.visibility = View.GONE
            }
        }

        profileBinding.profileImageContainer.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        viewModel.monthlyIncome.observe(this) { income ->
            profileBinding.etMonthlyIncome.setText(income.toInt().toString())
            profileBinding.tvAnnualCalc.text = "Annual: ₹ ${java.text.NumberFormat.getNumberInstance().format(income.toInt() * 12)}"
        }

        profileBinding.btnSaveProfile.setOnClickListener {
            val name = profileBinding.etFullName.text.toString()
            val income = profileBinding.etMonthlyIncome.text.toString().toDoubleOrNull() ?: 0.0
            viewModel.updateProfile(name, income)
            showSettings()
        }
    }

    private fun showCurrencySelection() {
        val currencyBinding = FragmentCurrencySelectionBinding.inflate(layoutInflater)
        swapScreen(currencyBinding.root, false, 1)
        currencyBinding.btnBack.setOnClickListener { onBackPressed() }

        val currencies = listOf(
            Pair("USD", "US Dollar"), Pair("EUR", "Euro"), Pair("GBP", "British Pound"),
            Pair("INR", "Indian Rupee"), Pair("JPY", "Japanese Yen"), Pair("AUD", "Australian Dollar"),
            Pair("CAD", "Canadian Dollar"), Pair("CHF", "Swiss Franc"), Pair("CNY", "Chinese Yuan")
        )

        var selectedCurrency = viewModel.currency.value ?: "₹ INR"

        setupCurrencyList(currencyBinding, currencies, selectedCurrency) { choice ->
            selectedCurrency = choice
        }

        currencyBinding.btnSaveCurrency.setOnClickListener {
            viewModel.updateCurrency(selectedCurrency)
            showSettings()
        }
    }

    private fun setupCurrencyList(
        binding: FragmentCurrencySelectionBinding,
        list: List<Pair<String, String>>,
        current: String,
        onSelect: (String) -> Unit
    ) {
        binding.rvCurrencies.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvCurrencies.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val card = layoutInflater.inflate(R.layout.item_currency, parent, false)
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val item = list[position]
                val symbol = when(item.first) { "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "INR" -> "₹"; "JPY" -> "¥"; "CHF" -> "Fr"; else -> item.first.take(1) }
                
                val view = holder.itemView
                view.findViewById<android.widget.TextView>(R.id.tv_currency_symbol).text = symbol
                view.findViewById<android.widget.TextView>(R.id.tv_currency_code).text = item.first
                view.findViewById<android.widget.TextView>(R.id.tv_currency_name).text = item.second

                val isSelected = current.contains(item.first)
                view.findViewById<android.widget.ImageView>(R.id.iv_check).visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
                (view as com.google.android.material.card.MaterialCardView).strokeWidth = if (isSelected) 4 else 1
                (view as com.google.android.material.card.MaterialCardView).strokeColor = if (isSelected) Color.parseColor("#10B981") else Color.parseColor("#F1F5F9")

                view.setOnClickListener {
                    onSelect("${symbol} ${item.first}")
                    setupCurrencyList(binding, list, "${symbol} ${item.first}", onSelect)
                }
            }

            override fun getItemCount() = list.size
        }
    }

    private fun showMonthlyIncome() {
        val incomeBinding = FragmentMonthlyIncomeBinding.inflate(layoutInflater)
        swapScreen(incomeBinding.root, false, 1)
        incomeBinding.btnBack.setOnClickListener { onBackPressed() }

        val currentIncome = viewModel.monthlyIncome.value ?: 0.0
        incomeBinding.tvIncomeDisplay.text = "₹${java.text.NumberFormat.getNumberInstance().format(currentIncome.toInt())}"
        incomeBinding.incomeSlider.value = currentIncome.toFloat().coerceIn(0f, 150000f)

        incomeBinding.incomeSlider.addOnChangeListener { _, value, _ ->
            updateIncomeUI(incomeBinding, value.toDouble())
        }

        incomeBinding.btnPreset25k.setOnClickListener { updateIncomeWithSlider(incomeBinding, 25000.0) }
        incomeBinding.btnPreset40k.setOnClickListener { updateIncomeWithSlider(incomeBinding, 40000.0) }
        incomeBinding.btnPreset52k.setOnClickListener { updateIncomeWithSlider(incomeBinding, 52000.0) }
        incomeBinding.btnPreset75k.setOnClickListener { updateIncomeWithSlider(incomeBinding, 75000.0) }
        incomeBinding.btnPreset1l.setOnClickListener { updateIncomeWithSlider(incomeBinding, 100000.0) }

        incomeBinding.btnSaveIncome.setOnClickListener {
            val amt = incomeBinding.incomeSlider.value.toDouble()
            viewModel.updateProfile(viewModel.userName.value ?: "Kbalu", amt)
            showSettings()
        }
    }

    private fun updateIncomeWithSlider(binding: FragmentMonthlyIncomeBinding, amt: Double) {
        binding.incomeSlider.value = amt.toFloat()
        updateIncomeUI(binding, amt)
    }

    private fun updateIncomeUI(binding: FragmentMonthlyIncomeBinding, amount: Double) {
        binding.tvIncomeDisplay.text = "₹${java.text.NumberFormat.getNumberInstance().format(amount.toInt())}"
        val savings = (amount * 0.2).toInt()
        binding.tvBudgetTip.text = "Budget tip: The 50/30/20 rule suggests allocating 50% of your income into needs, 30% into wants, and 20% into savings. Based on your income, aim to save at least ₹${java.text.NumberFormat.getNumberInstance().format(savings)} monthly."
    }

    private fun showNotificationSettings() {
        val notifBinding = FragmentNotificationSettingsBinding.inflate(layoutInflater)
        swapScreen(notifBinding.root, false, 1)
        notifBinding.btnBack.setOnClickListener { onBackPressed() }

        viewModel.notifPush.observe(this) { notifBinding.switchPush.isChecked = it }
        viewModel.notifEmail.observe(this) { notifBinding.switchEmail.isChecked = it }
        viewModel.notifBudget.observe(this) { notifBinding.switchBudget.isChecked = it }
        viewModel.notifGoal.observe(this) { notifBinding.switchGoal.isChecked = it }
        viewModel.notifReports.observe(this) { notifBinding.switchReports.isChecked = it }

        val listener = android.widget.CompoundButton.OnCheckedChangeListener { _, _ ->
            viewModel.updateNotifSettings(
                notifBinding.switchPush.isChecked,
                notifBinding.switchEmail.isChecked,
                notifBinding.switchBudget.isChecked,
                notifBinding.switchGoal.isChecked,
                notifBinding.switchReports.isChecked
            )
        }

        notifBinding.switchPush.setOnCheckedChangeListener(listener)
        notifBinding.switchEmail.setOnCheckedChangeListener(listener)
        notifBinding.switchBudget.setOnCheckedChangeListener(listener)
        notifBinding.switchGoal.setOnCheckedChangeListener(listener)
        notifBinding.switchReports.setOnCheckedChangeListener(listener)
    }

    private fun showExportData() {
        val exportBinding = FragmentExportDataBinding.inflate(layoutInflater)
        swapScreen(exportBinding.root, false, 1)
        exportBinding.btnBack.setOnClickListener { onBackPressed() }

        val cards = listOf(exportBinding.cardCsv, exportBinding.cardJson, exportBinding.cardPdf)
        val radios = listOf(exportBinding.rbCsv, exportBinding.rbJson, exportBinding.rbPdf)

        var selectedFormatIndex = 0

        fun selectFormat(index: Int) {
            selectedFormatIndex = index
            for (i in cards.indices) {
                val isSelected = i == index
                cards[i].strokeWidth = if (isSelected) 4 else 1
                cards[i].strokeColor = if (isSelected) Color.parseColor("#10B981") else Color.parseColor("#F1F5F9")
                radios[i].isChecked = isSelected
                radios[i].buttonTintList = ColorStateList.valueOf(
                    if (isSelected) Color.parseColor("#10B981") else Color.parseColor("#CBD5E1")
                )
            }
        }

        exportBinding.cardCsv.setOnClickListener { selectFormat(0) }
        exportBinding.cardJson.setOnClickListener { selectFormat(1) }
        exportBinding.cardPdf.setOnClickListener { selectFormat(2) }
        
        // Default to CSV
        selectFormat(0)

        exportBinding.btnExportNow.setOnClickListener {
            when (selectedFormatIndex) {
                0 -> exportTransactionsToCsv()
                1 -> exportTransactionsToJson()
                2 -> exportTransactionsToPdf()
            }
        }
    }

    private fun exportTransactionsToCsv() {
        val transactions = viewModel.allTransactions.value ?: emptyList()
        if (transactions.isEmpty()) {
            android.widget.Toast.makeText(this, "No transactions to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val csvHeader = "ID,Title,Amount,Type,Category,Date,Time\n"
        val csvBody = transactions.joinToString("\n") { 
            "${it.id},${it.title},${it.amount},${it.type},${it.category},${it.date},${it.time}"
        }
        val csvContent = csvHeader + csvBody

        try {
            val fileName = "Finly_Transactions_${System.currentTimeMillis()}.csv"
            val file = java.io.File(cacheDir, fileName)
            file.writeText(csvContent)

            // Explicitly use the package name to avoid mismatched authorities
            val authority = "com.finly.app.provider" 
            val uri = androidx.core.content.FileProvider.getUriForFile(this, authority, file)
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Export Transactions"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun exportTransactionsToJson() {
        val transactions = viewModel.allTransactions.value ?: emptyList()
        if (transactions.isEmpty()) {
            android.widget.Toast.makeText(this, "No transactions to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val jsonArrayStr = transactions.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { t ->
            """  {
    "id": ${t.id},
    "title": "${t.title.replace("\"", "\\\"")}",
    "amount": ${t.amount},
    "type": "${t.type}",
    "category": "${t.category}",
    "date": "${t.date}",
    "time": "${t.time}"
  }"""
        }

        try {
            val fileName = "Finly_Transactions_${System.currentTimeMillis()}.json"
            val file = java.io.File(cacheDir, fileName)
            file.writeText(jsonArrayStr)

            val authority = "com.finly.app.provider"
            val uri = androidx.core.content.FileProvider.getUriForFile(this, authority, file)

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Export Transactions"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun exportTransactionsToPdf() {
        val transactions = viewModel.allTransactions.value ?: emptyList()
        if (transactions.isEmpty()) {
            android.widget.Toast.makeText(this, "No transactions to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = android.graphics.pdf.PdfDocument()
        val paint = android.graphics.Paint()
        val titlePaint = android.graphics.Paint().apply {
            textSize = 24f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        
        paint.textSize = 14f
        
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var yPos = 50f
        canvas.drawText("Finly Transactions Report", 50f, yPos, titlePaint)
        yPos += 40f

        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
        
        for (t in transactions) {
            if (yPos > 800f) {
                // Not handling multiple pages fully to keep it simple, but we prevent out of bounds
                canvas.drawText("... More transactions exist", 50f, yPos, paint)
                break
            }
            val line = "${t.date} | ${t.category} | ${t.title} | ₹${fmt.format(t.amount)}"
            canvas.drawText(line, 50f, yPos, paint)
            yPos += 25f
        }

        pdfDocument.finishPage(page)

        try {
            val fileName = "Finly_Transactions_${System.currentTimeMillis()}.pdf"
            val file = java.io.File(cacheDir, fileName)
            file.outputStream().use { 
                pdfDocument.writeTo(it) 
            }
            pdfDocument.close()

            val authority = "com.finly.app.provider"
            val uri = androidx.core.content.FileProvider.getUriForFile(this, authority, file)

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Export Transactions"))
        } catch (e: Exception) {
            pdfDocument.close()
            android.widget.Toast.makeText(this, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
