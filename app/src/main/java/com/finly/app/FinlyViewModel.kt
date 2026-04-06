package com.finly.app

import android.app.Application
import androidx.lifecycle.*
import com.finly.app.data.*
import kotlinx.coroutines.launch

class FinlyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database.transactionDao())
    private val goalRepository = GoalRepository(database.goalDao())
    private val settingsRepository = SettingsRepository(application)

    // Session-level flag: survives config changes (dark mode toggle) but not process death.
    // Ensures splash only shows once per app session, not on every Activity recreation.
    var splashShown: Boolean = false

    // Checks if the user has actually completed the setup wizard (Setup1 + Setup2).
    // Returns false on fresh installs and after "Clear All Data".
    fun isSetupComplete(): Boolean = settingsRepository.isSetupComplete()

    // Called from MainActivity after user presses "Start" in Setup Step 2.
    fun setSetupComplete() = settingsRepository.setSetupComplete(true)

    // Goal Data
    val allGoals: LiveData<List<Goal>> = goalRepository.getAllGoals()

    // Transaction Data
    val allTransactions: LiveData<List<Transaction>> = repository.getAllTransactions()
    val totalIncome: LiveData<Double?> = repository.totalIncome
    val totalExpense: LiveData<Double?> = repository.totalExpense
    val categoryWiseExpenses: LiveData<List<CategorySum>> = repository.categoryWiseExpenses

    // Filtering & Searching
    private val _filter = MutableLiveData<String>("ALL")
    private val _searchQuery = MutableLiveData<String>("")

    val filteredTransactions: LiveData<List<Transaction>> = _filter.switchMap { filter ->
        when (filter) {
            "INCOME" -> repository.getTransactionsByType("income")
            "EXPENSE" -> repository.getTransactionsByType("expense")
            "MONTH" -> {
                val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
                repository.getTransactionsByMonth(currentMonth)
            }
            "LAST_MONTH" -> {
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.MONTH, -1)
                val lastMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(calendar.time)
                repository.getTransactionsByMonth(lastMonth)
            }
            else -> repository.getAllTransactions()
        }
    }

    val searchResults: LiveData<List<Transaction>> = _searchQuery.switchMap { query ->
        if (query.isEmpty()) repository.getAllTransactions()
        else repository.searchTransactions(query)
    }

    fun setFilter(filter: String) { _filter.value = filter }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // Calculated Balance
    val balance: LiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(totalIncome) { income ->
            value = (income ?: 0.0) - (totalExpense.value ?: 0.0)
        }
        addSource(totalExpense) { expense ->
            value = (totalIncome.value ?: 0.0) - (expense ?: 0.0)
        }
    }

    // Persistent Settings
    private val _userName = MutableLiveData<String>(settingsRepository.getUserName())
    val userName: LiveData<String> = _userName
    
    private val _monthlyIncome = MutableLiveData<Double>(settingsRepository.getMonthlyIncome())
    val monthlyIncome: LiveData<Double> = _monthlyIncome
    
    private val _currency = MutableLiveData<String>(settingsRepository.getCurrency())
    val currency: LiveData<String> = _currency
    
    private val _isDarkMode = MutableLiveData<Boolean>(settingsRepository.isDarkModeEnabled())
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    private val _isNotifEnabled = MutableLiveData<Boolean>(settingsRepository.isNotificationsEnabled())
    val isNotifEnabled: LiveData<Boolean> = _isNotifEnabled

    private val _isBiometricEnabled = MutableLiveData<Boolean>(settingsRepository.isBiometricEnabled())
    val isBiometricEnabled: LiveData<Boolean> = _isBiometricEnabled

    private val _profileImageUri = MutableLiveData<String?>(settingsRepository.getProfileImageUri())
    val profileImageUri: LiveData<String?> = _profileImageUri

    fun setProfileImage(uri: String) {
        settingsRepository.saveProfileImageUri(uri)
        _profileImageUri.value = uri
    }

    // Detailed Notifications
    val notifPush = MutableLiveData<Boolean>(settingsRepository.getNotifPush())
    val notifEmail = MutableLiveData<Boolean>(settingsRepository.getNotifEmail())
    val notifBudget = MutableLiveData<Boolean>(settingsRepository.getNotifBudget())
    val notifGoal = MutableLiveData<Boolean>(settingsRepository.getNotifGoal())
    val notifReports = MutableLiveData<Boolean>(settingsRepository.getNotifReports())

    fun updateProfile(name: String, income: Double) {
        settingsRepository.saveUserName(name)
        settingsRepository.saveMonthlyIncome(income)
        _userName.value = name
        _monthlyIncome.value = income
    }

    fun updateCurrency(newCurrency: String) {
        settingsRepository.saveCurrency(newCurrency)
        _currency.value = newCurrency
    }

    fun setDarkMode(enabled: Boolean) {
        settingsRepository.setDarkModeEnabled(enabled)
        _isDarkMode.value = enabled
    }

    fun setNotifications(enabled: Boolean) {
        settingsRepository.setNotificationsEnabled(enabled)
        _isNotifEnabled.value = enabled
    }

    fun setBiometric(enabled: Boolean) {
        settingsRepository.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    fun updateNotifSettings(push: Boolean, email: Boolean, budget: Boolean, goal: Boolean, reports: Boolean) {
        settingsRepository.setNotifPush(push)
        settingsRepository.setNotifEmail(email)
        settingsRepository.setNotifBudget(budget)
        settingsRepository.setNotifGoal(goal)
        settingsRepository.setNotifReports(reports)
        notifPush.value = push
        notifEmail.value = email
        notifBudget.value = budget
        notifGoal.value = goal
        notifReports.value = reports
    }

    fun clearAllAppData() {
        viewModelScope.launch {
            repository.deleteAll()
            goalRepository.deleteAllGoals()
            settingsRepository.clearAll()
            // Reset setup so onboarding shows again on next launch
            settingsRepository.setSetupComplete(false)
            settingsRepository.setDarkModeEnabled(false)
            // Reset LiveData triggers
            _userName.postValue("")
            _monthlyIncome.postValue(0.0)
            _currency.postValue("₹ INR")
            _isDarkMode.postValue(false)
        }
    }

    val budgetFood = MutableLiveData<String>("8000")
    val budgetTransport = MutableLiveData<String>("5000")
    val budgetShopping = MutableLiveData<String>("7000")
    val budgetBills = MutableLiveData<String>("10000")

    // Compatibility Setters
    fun setUserName(name: String) { updateProfile(name, _monthlyIncome.value ?: 0.0) }
    fun setMonthlyIncome(income: String) { updateProfile(_userName.value ?: "Kbalu", income.toDoubleOrNull() ?: 0.0) }
    fun setCurrency(currencyStr: String) { updateCurrency(currencyStr) }
    
    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (settingsRepository.hasSampleData() && transaction.note != "SAMPLE_DATA") {
                repository.deleteSampleTransactions()
                settingsRepository.setSampleData(false)
            }
            repository.insert(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    // Goals Handling
    fun insertGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }

    fun getGoalById(id: Int): LiveData<Goal> {
        return goalRepository.getGoalById(id)
    }

    fun getContributionsForGoal(id: Int): LiveData<List<Contribution>> {
        return goalRepository.getContributionsByGoalId(id)
    }

    fun addSavingsToGoal(goal: Goal, amount: Double) {
        viewModelScope.launch {
            val contribution = Contribution(
                goalId = goal.id,
                amount = amount,
                date = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            )
            val updatedGoal = goal.copy(savedAmount = goal.savedAmount + amount)
            goalRepository.insertContribution(contribution)
            goalRepository.updateGoal(updatedGoal)
        }
    }

    private var isSeeded = false

    fun seedData() {
        if (!settingsRepository.isFirstRun()) return
        settingsRepository.setFirstRun(false)
        settingsRepository.setSampleData(true)
        viewModelScope.launch {
            // Wipe existing duplicates to clean up from the bug
            repository.deleteAll()
            goalRepository.deleteAllGoals()
            
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            
            val initialIncome = settingsRepository.getMonthlyIncome()
            
            // Add User's Actual Income
            repository.insert(Transaction(title = "Monthly Income", amount = if (initialIncome > 0) initialIncome else 50000.0, type = "income", category = "Salary", date = currentDate, time = currentTime, emoji = "💼", note = "USER_INCOME"))
            
            // Add Sample Expenses
            repository.insert(Transaction(title = "Netflix Subscription (Sample)", amount = 199.0, type = "expense", category = "Entertainment", date = currentDate, time = "05:30 AM", emoji = "🎬", note = "SAMPLE_DATA"))
            repository.insert(Transaction(title = "Coffee (Sample)", amount = 120.0, type = "expense", category = "Food", date = currentDate, time = "08:15 AM", emoji = "☕", note = "SAMPLE_DATA"))
            repository.insert(Transaction(title = "Uber Ride (Sample)", amount = 320.0, type = "expense", category = "Transport", date = currentDate, time = "06:45 PM", emoji = "🚗", note = "SAMPLE_DATA"))
            repository.insert(Transaction(title = "Shopping (Sample)", amount = 850.0, type = "expense", category = "Shopping", date = currentDate, time = "11:30 AM", emoji = "🛍️", note = "SAMPLE_DATA"))

            // Seed Goals
            insertGoal(Goal(title = "Emergency Fund", targetAmount = 20000.0, savedAmount = 13600.0, icon = "💰"))
            insertGoal(Goal(title = "Vacation Trip", targetAmount = 50000.0, savedAmount = 25000.0, icon = "✈️"))
            insertGoal(Goal(title = "New Laptop", targetAmount = 80000.0, savedAmount = 35000.0, icon = "💻"))
        }
    }

    val totalBudget: Int
        get() {
            val f = budgetFood.value?.toIntOrNull() ?: 0
            val t = budgetTransport.value?.toIntOrNull() ?: 0
            val s = budgetShopping.value?.toIntOrNull() ?: 0
            val b = budgetBills.value?.toIntOrNull() ?: 0
            return f + t + s + b
        }

    // ─── Insights Data ─────────────────────────────────────────────────────────

    private val _monthlyTrendData = MutableLiveData<List<CategorySum>>()
    val monthlyTrendData: LiveData<List<CategorySum>> = _monthlyTrendData

    private val _categoryBreakdown = MutableLiveData<List<CategorySum>>()
    val categoryBreakdown: LiveData<List<CategorySum>> = _categoryBreakdown

    private val _topCategory = MutableLiveData<CategorySum?>()
    val topCategory: LiveData<CategorySum?> = _topCategory

    private val _currentMonthExpense = MutableLiveData<Double>()
    val currentMonthExpense: LiveData<Double> = _currentMonthExpense

    private val _lastMonthExpense = MutableLiveData<Double>()
    val lastMonthExpense: LiveData<Double> = _lastMonthExpense

    private val _weeklyComparisonData = MutableLiveData<Pair<List<CategorySum>, List<CategorySum>>>()
    val weeklyComparisonData: LiveData<Pair<List<CategorySum>, List<CategorySum>>> = _weeklyComparisonData

    private val _thisWeekTotal = MutableLiveData<Double>()
    val thisWeekTotal: LiveData<Double> = _thisWeekTotal

    private val _lastWeekTotal = MutableLiveData<Double>()
    val lastWeekTotal: LiveData<Double> = _lastWeekTotal

    private val _frequentTransactions = MutableLiveData<List<CategorySum>>()
    val frequentTransactions: LiveData<List<CategorySum>> = _frequentTransactions

    private val _spendingPatternData = MutableLiveData<List<CategorySum>>()
    val spendingPatternData: LiveData<List<CategorySum>> = _spendingPatternData

    private val _currentMonthOffset = MutableLiveData<Int>(0)
    val currentMonthOffset: LiveData<Int> = _currentMonthOffset
    
    private val _currentMonthText = MutableLiveData<String>()
    val currentMonthText: LiveData<String> = _currentMonthText

    fun nextMonth() {
        _currentMonthOffset.value = (_currentMonthOffset.value ?: 0) + 1
        loadInsightsData()
    }

    fun prevMonth() {
        _currentMonthOffset.value = (_currentMonthOffset.value ?: 0) - 1
        loadInsightsData()
    }

    fun loadInsightsData() {
        viewModelScope.launch {
            val offset = _currentMonthOffset.value ?: 0
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, offset)
            
            _currentMonthText.value = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)

            // Current Month
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val curStart = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            val curEnd = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            
            // Last Month
            val calLast = java.util.Calendar.getInstance()
            calLast.add(java.util.Calendar.MONTH, offset - 1)
            calLast.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val lastStart = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calLast.time)
            calLast.set(java.util.Calendar.DAY_OF_MONTH, calLast.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            val lastEnd = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calLast.time)

            // Week Bounds
            fun weekBounds(weekOffset: Int): Pair<String, String> {
                val c = java.util.Calendar.getInstance()
                c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                c.add(java.util.Calendar.WEEK_OF_YEAR, weekOffset)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val start = sdf.format(c.time)
                c.add(java.util.Calendar.DAY_OF_WEEK, 6)
                val end = sdf.format(c.time)
                return Pair(start, end)
            }
            val (thisWkStart, thisWkEnd) = weekBounds(0)
            val (lastWkStart, lastWkEnd) = weekBounds(-1)

            // 28 days for pattern
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val end28 = sdf.format(java.util.Date())
            val pcal = java.util.Calendar.getInstance()
            pcal.add(java.util.Calendar.DAY_OF_YEAR, -28)
            val start28 = sdf.format(pcal.time)

            // Run database queries
            val curTotal = repository.getCurrentMonthExpense(curStart, curEnd) ?: 0.0
            val lastTotal = repository.getLastMonthExpense(lastStart, lastEnd) ?: 0.0
            val curCats = repository.getCategoryBreakdown(curStart, curEnd)
            val trend = repository.getMonthlyTrend()
            
            val thisWkTotalVal = repository.getCurrentMonthExpense(thisWkStart, thisWkEnd) ?: 0.0 // Reuse metric
            val lastWkTotalVal = repository.getLastMonthExpense(lastWkStart, lastWkEnd) ?: 0.0
            val thisWkDaily = repository.getWeeklyComparison(thisWkStart, thisWkEnd)
            val lastWkDaily = repository.getWeeklyComparison(lastWkStart, lastWkEnd)
            
            val freqCats = repository.getFrequentTransactions()
            val patternData = repository.getSpendingPattern(start28, end28)
            
            // Post Values
            _currentMonthExpense.postValue(curTotal)
            _lastMonthExpense.postValue(lastTotal)
            _categoryBreakdown.postValue(curCats)
            _topCategory.postValue(curCats.firstOrNull())
            _monthlyTrendData.postValue(trend)
            _thisWeekTotal.postValue(thisWkTotalVal)
            _lastWeekTotal.postValue(lastWkTotalVal)
            _weeklyComparisonData.postValue(Pair(thisWkDaily, lastWkDaily))
            _frequentTransactions.postValue(freqCats)
            _spendingPatternData.postValue(patternData)
        }
    }
}
