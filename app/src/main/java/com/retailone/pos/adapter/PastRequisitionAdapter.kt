package com.retailone.pos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.PastRequisitionItemLayoutBinding
import com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel.PastRequisitionData
import com.retailone.pos.utils.DateFormatter

class PastRequisitionAdapter(
    val context: Context,
    val pastRequsitionRes: List<PastRequisitionData>,
    val type: String,
    val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PastRequisitionAdapter.PastRequisitionViewHolder>() {


    class PastRequisitionViewHolder(val binding: PastRequisitionItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastRequisitionViewHolder {
        return PastRequisitionViewHolder(
            PastRequisitionItemLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: PastRequisitionViewHolder, position: Int) {

        val item = pastRequsitionRes[position]

        val datemodel = DateFormatter(item.pi_date).formatDateModel()


        holder.binding.apply {
            itemName.text = context.getString(R.string.req_id_prefix, item.order_id)
            date.text = datemodel.day.toString()
            month.text = datemodel.month.toString()
            itemQuantity.text = if (item.order_items.size == 1) {
                context.getString(R.string.item_suffix, item.order_items.size.toString())
            } else {
                context.getString(R.string.items_suffix, item.order_items.size.toString())
            }

            setStatusString(item.status, status)
        }


        holder.binding.card.setOnClickListener {
            onItemClick(item.id.toString())  // request id
        }

    }

    private fun setStatusString(status: String, textview: android.widget.TextView) {

        when (status) {
            "0" -> {
                textview.text = context.getString(R.string.status_pending)
                textview.setTextColor(android.graphics.Color.parseColor("#FF980E"))
            }

            "1" -> {
                textview.text = context.getString(R.string.status_approved)
                textview.setTextColor(android.graphics.Color.parseColor("#0496c7"))
            }

            "2" -> {
                textview.text = context.getString(R.string.status_cancelled)
                textview.setTextColor(android.graphics.Color.parseColor("#D3212C"))
            }
            "4" -> {
                textview.text = context.getString(R.string.status_dispatched)
                textview.setTextColor(android.graphics.Color.parseColor("#673AB7"))
            }

            "3" -> {
                textview.text = context.getString(R.string.status_received)
                textview.setTextColor(android.graphics.Color.parseColor("#008000"))
            }
            "5" -> {
                textview.text = context.getString(R.string.status_rejected)
                textview.setTextColor(android.graphics.Color.parseColor("#D3212C"))
            }

            else -> {
                textview.text = "."
            }
        }
    }



    override fun getItemCount(): Int {
        var size = pastRequsitionRes.size

        if (type != "all" && size > 3) {
            size = 3
        }

        return size
    }

}
