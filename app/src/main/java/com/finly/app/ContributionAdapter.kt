package com.finly.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.finly.app.data.Contribution
import com.finly.app.databinding.ItemContributionBinding

class ContributionAdapter(
    private var contributions: List<Contribution>
) : RecyclerView.Adapter<ContributionAdapter.ContributionViewHolder>() {

    inner class ContributionViewHolder(private val binding: ItemContributionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contribution: Contribution) {
            binding.tvContributionDate.text = contribution.date
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
            val amountStr = formatter.format(contribution.amount)
            binding.tvContributionAmount.text = "+₹ $amountStr"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributionViewHolder {
        val binding = ItemContributionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContributionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContributionViewHolder, position: Int) {
        holder.bind(contributions[position])
    }

    override fun getItemCount(): Int = contributions.size

    fun submitList(newList: List<Contribution>) {
        contributions = newList
        notifyDataSetChanged()
    }
}
