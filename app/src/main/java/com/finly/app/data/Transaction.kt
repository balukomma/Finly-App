package com.finly.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "income" or "expense"
    val category: String,
    val date: String, // Date string (e.g. "Mar 31, 2026")
    val time: String, // Time string (e.g. "05:30 AM")
    val note: String = "",
    val emoji: String = ""
)
