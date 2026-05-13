package com.retailone.pos.adapter

import com.retailone.pos.utils.NumberFormatter
import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.SalesDetailsItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvItem
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsData
import com.retailone.pos.network.Constants
import com.retailone.pos.utils.FunUtils

class SalesDetailsAdapter(
    val context: Context,
    val salesdetails: SalesDetailsData)
    : RecyclerView.Adapter<SalesDetailsAdapter.SalesDetailsViewHolder>() {


    private val localizationData = LocalizationHelper(context).getLocalizationData()



    class SalesDetailsViewHolder(val binding: SalesDetailsItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesDetailsViewHolder {
        return SalesDetailsViewHolder(
            SalesDetailsItemLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: SalesDetailsViewHolder, position: Int) {

        val item = salesdetails.sales_items[position]

        val formattedPrice = NumberFormatter().formatPrice(item?.total_amount.toString(),localizationData)


        holder.binding.apply {
           date.text = item?.product?.product_name
            //name.text = item?.distribution_pack?.product_description
            name.text = item?.distribution_pack_name
            category.text = context.getString(R.string.sales_details_quantity_prefix,
                item?.total_quantity?.let { FunUtils.DtoString(it) } ?: "0")
            tax.text = context.getString(R.string.sales_details_tax_prefix, salesdetails.tax ?: "")
            taxamount.text = context.getString(R.string.sales_details_tax_amount_prefix, item?.tax_amount ?: "")


            price.text = formattedPrice
        }


    }



    override fun getItemCount(): Int {
        return salesdetails.sales_items.size
    }


}
