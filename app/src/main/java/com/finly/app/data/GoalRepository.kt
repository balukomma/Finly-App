package com.finly.app.data

import androidx.lifecycle.LiveData

class GoalRepository(private val goalDao: GoalDao) {
    fun getAllGoals(): LiveData<List<Goal>> = goalDao.getAllGoals()
    
    fun getGoalById(id: Int): LiveData<Goal> = goalDao.getGoalById(id)
    
    fun getContributionsByGoalId(goalId: Int): LiveData<List<Contribution>> = goalDao.getContributionsByGoalId(goalId)

    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)

    suspend fun deleteAllGoals() = goalDao.deleteAllGoals()

    suspend fun insertContribution(contribution: Contribution) = goalDao.insertContribution(contribution)
}
