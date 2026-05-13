package com.retailone.pos.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.models.SalesData
import com.retailone.pos.ui.Activity.DashboardActivity.SearchReturnProductActivity
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.utils.NumberFormatter


class SalesListAdapter(private val context: Context, private val salesList: List<SalesData>) :
    RecyclerView.Adapter<SalesListAdapter.SalesViewHolder>() {

    inner class SalesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val invoiceId = itemView.findViewById<TextView>(R.id.tvInvoiceId)
        val saleDate = itemView.findViewById<TextView>(R.id.tvSaleDate)
        val totalAmount = itemView.findViewById<TextView>(R.id.tvGrandTotal)
        val paymentType = itemView.findViewById<TextView>(R.id.tvPaymentType)
        val returnableFlag = itemView.findViewById<TextView>(R.id.tvReturnableFlag)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sales_list, parent, false)
        return SalesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalesViewHolder, position: Int) {
        val item = salesList[position]
        holder.invoiceId.text = item.invoice_id
        holder.saleDate.text = item.sale_date_time
        val localizationData = LocalizationHelper(context).getLocalizationData()
        holder.totalAmount.text = NumberFormatter().formatPrice(item.grand_total.toString(), localizationData)
        holder.paymentType.text = item.payment_type
        // 🔥 Click to launch activity with invoice_id
        // 🔺 Show flag if total_refunded_amount == 0.0
        if (item.total_refunded_amount == 0.0) {
            holder.returnableFlag.visibility = View.VISIBLE
        } else {
            holder.returnableFlag.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            val intent = Intent(context, SearchReturnProductActivity::class.java)
            intent.putExtra("invoice_id", item.invoice_id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = salesList.size
}
