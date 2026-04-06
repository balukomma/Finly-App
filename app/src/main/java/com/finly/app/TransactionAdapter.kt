package com.finly.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.finly.app.data.Transaction
import com.finly.app.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var items: List<Any> = emptyList(),
    private val onTransactionClick: ((Transaction) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    fun updateData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            TransactionViewHolder(binding, onTransactionClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.tvHeader.text = items[position] as String
        } else if (holder is TransactionViewHolder) {
            holder.bind(items[position] as Transaction)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val tvHeader: android.widget.TextView = view.findViewById(R.id.tv_header)
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onClick: ((Transaction) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        fun bind(transaction: Transaction) {
            binding.tvTransTitle.text = transaction.title
            binding.tvTransCategory.text = transaction.category
            binding.tvTransTime.text = transaction.time
            binding.tvTransIcon.text = transaction.emoji

            if (transaction.type == "income") {
                binding.tvTransAmount.text = "+ ₹ ${transaction.amount.toInt()}"
                binding.tvTransAmount.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.primary_green)
                )
            } else {
                binding.tvTransAmount.text = "₹ ${transaction.amount.toInt()}"
                binding.tvTransAmount.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.expense_red)
                )
            }
            
            binding.root.setOnClickListener {
                onClick?.invoke(transaction)
            }
        }
    }
}
