package com.retailone.pos.adapter

import com.retailone.pos.utils.NumberFormatter
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.HeaderrowlayoutBinding


import com.retailone.pos.databinding.PiChildItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.ProductInventoryModel.PiChildData
import com.retailone.pos.models.ProductInventoryModel.PiChildRow
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.DistributionPackData
import com.retailone.pos.utils.FunUtils
import kotlin.math.absoluteValue

class PiChildAdapter(
    private val context: Context,
    private val packList: List<DistributionPackData>
    // 👈 Add this
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val localizationData = LocalizationHelper(context).getLocalizationData()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    //third
    private val rows: List<PiChildRow> = packList.flatMap { pack ->
        val stockQty = pack.stock_quatity
        val header = PiChildRow.Header(pack.batch_no ?: "-", stockQty)
        val children = mutableListOf<PiChildRow.Item>()

        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalDate.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val expiryDate = try {
            java.time.LocalDate.parse(pack.expiry_date ?: "", formatter)
        } catch (e: Exception) {
            null
        }

        val isExpired = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            expiryDate != null && expiryDate.isBefore(today)
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        // 1. Add auto-tagged stock items
        if (stockQty > 0) {
            val autoStatus = if (isExpired) context.getString(R.string.status_expired) else context.getString(R.string.status_store_stock)
            children.add(
                PiChildRow.Item(
                    status = autoStatus,
                    qty = stockQty.toInt(),
                    data = pack
                )
            )
        }

        // 2. Add manual returned_items
        pack.returned_items?.forEach { (status, qty) ->
            if (!status.equals("Good", ignoreCase = true) && qty > 0) {
                children.add(
                    PiChildRow.Item(
                        status = status,
                        qty = qty,
                        data = pack
                    )
                )
            }
        }

        // 3. Add good_returned_items
        // 3. Add good_returned_items but skip "Good"
        pack.good_returned_items?.forEach { (status, qty) ->
            if (/*!status.equals("Good", ignoreCase = true) && */qty > 0) {
                children.add(
                    PiChildRow.Item(
                        status = status,
                        qty = qty,
                        data = pack
                    )
                )
            }
        }


        // Return header + child if any items exist
        if (children.isNotEmpty()) listOf(header) + children else emptyList()
    }




    // ViewHolders
    inner class HeaderViewHolder(val binding: HeaderrowlayoutBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ItemViewHolder(val binding: PiChildItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is PiChildRow.Header -> TYPE_HEADER
            is PiChildRow.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = HeaderrowlayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = PiChildItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is PiChildRow.Header -> {
                val headerHolder = holder as HeaderViewHolder
                // headerHolder.binding.txtBatchHeader.text = "Batch: ${row.batchNo} | Stock: ${row.stock.toInt()}"
                headerHolder.binding.txtBatchHeader.text = "${context.getString(R.string.batch_label)}: ${row.batchNo}"
            }
            is PiChildRow.Item -> {
                val itemHolder = holder as ItemViewHolder
                val pack = row.data

                itemHolder.binding.txtBatchName.text = pack.pack_description ?: "-"
                itemHolder.binding.txtQuantity.text = row.qty.toString()
                itemHolder.binding.txtExpiry.text = pack.expiry_date

                val price = NumberFormatter().formatPrice(pack.retail_price ?: "-", localizationData)
                itemHolder.binding.txtPrice.text = price
                // val status = row.status.lowercase().trim()
                // val badgeText = status.firstOrNull()?.uppercaseChar()?.toString() ?: "-"
                val status = row.status.trim()
                val badgeText = status
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .joinToString("") { it.first().uppercaseChar().toString() }
                itemHolder.binding.viewStatus.text = badgeText

                val badgeColorResId = when (status) {
                    context.getString(R.string.status_good) -> R.color.grad1s
                    context.getString(R.string.status_damaged), context.getString(R.string.status_defective) -> R.color.badge_orange
                    context.getString(R.string.status_expired) -> R.color.badge_red
                    context.getString(R.string.status_store_stock) -> R.color.badge_green
                    else -> {
                        val colorIndex = (status.hashCode().absoluteValue % 10) + 1
                        context.resources.getIdentifier("badge_color_$colorIndex", "color", context.packageName)
                    }
                }
                itemHolder.binding.viewStatus.backgroundTintList =
                    ContextCompat.getColorStateList(context, badgeColorResId)

                itemHolder.binding.viewStatus.setOnClickListener { view ->

                    // Inflate the custom tooltip layout
                    val popupView = LayoutInflater.from(context).inflate(R.layout.layout_status_popup, null)
                    val popupText = popupView.findViewById<TextView>(R.id.txtStatusDetail)
                    // popupText.text = "$badgeText - $status"
                    popupText.text = "$status"

                    // Create the PopupWindow
                    val popupWindow = PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true // Focusable to dismiss on outside touch
                    )

                    // Show above-right of viewStatus
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)

                    popupWindow.showAtLocation(view, Gravity.NO_GRAVITY,
                        location[0] + view.width / 2, // X: half right
                        location[1] - view.height - 20 // Y: above with padding
                    )

                    // Optional: close when clicking outside
                    popupWindow.isOutsideTouchable = true
                    popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
                }


            }
        }
    }

    override fun getItemCount(): Int = rows.size
}
