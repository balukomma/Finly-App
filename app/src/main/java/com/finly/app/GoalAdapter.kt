package com.finly.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.finly.app.data.Goal
import com.finly.app.databinding.ItemGoalBinding

class GoalAdapter(
    private var goals: List<Goal>,
    private val onItemClick: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(private val binding: ItemGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            // Support multiple formats. 
            // If it's budget/no_spend, title layout differs slightly, but standardizing works.
            binding.tvGoalIcon.text = if (goal.icon.isNotEmpty()) goal.icon else "💰"
            binding.tvGoalTitle.text = goal.title
            
            // Format amounts
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
            val targetStr = formatter.format(goal.targetAmount)
            val savedStr = formatter.format(goal.savedAmount)
            
            binding.tvGoalAmounts.text = "₹ $savedStr / ₹ $targetStr"

            val progress = if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100).toInt() else 0
            val safeProgress = progress.coerceIn(0, 100)
            
            binding.progressCircular.progress = safeProgress
            binding.progressLinear.progress = safeProgress
            binding.tvGoalPercent.text = "$safeProgress%"
            
            binding.root.setOnClickListener { onItemClick(goal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount(): Int = goals.size

    fun submitList(newList: List<Goal>) {
        goals = newList
        notifyDataSetChanged()
    }
}
