package com.finly.app.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String = "savings", // "savings", "no_spend", "budget_limit"
    val title: String,
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val startDate: String = "",
    val endDate: String = "",
    val icon: String = "",
    val duration: Int = 0,
    val categories: String = ""
)
