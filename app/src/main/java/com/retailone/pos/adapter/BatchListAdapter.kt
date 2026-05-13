package com.retailone.pos.adapter

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.ItemBatchRowBinding
import com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks.ReturnBatchItem
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BatchListAdapter(
    private val returnItems: List<ReturnBatchItem>,
    private val reasonNames: List<String>
) : RecyclerView.Adapter<BatchListAdapter.BatchViewHolder>() {

    private val selectedItems = mutableListOf<StockReturnItem>()

    inner class BatchViewHolder(val binding: ItemBatchRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
        val binding = ItemBatchRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BatchViewHolder(binding)
    }


    override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
        val item = returnItems[position]
        val fromGoodReturned = item.fromGoodReturnedMap == true
        val context = holder.itemView.context
        holder.binding.tvStatus.isSelected = true


        with(holder.binding) {

            val isGoodFromReturnedMap = /*!item.isEditable && */item.condition.equals("Good", ignoreCase = true)

            tvAvailableQty.text = if (isGoodFromReturnedMap) {
                item.returnedQty.toString()  // Show returned qty if from good_returned_items
            } else if (item.condition.equals("Good", ignoreCase = true)) {
                     item.returnedQty.toString()    // Otherwise, show full stock
                 } else {
                item.returnedQty.toString()  // For others like Damaged, Expired
            }


            tvStock.text = item.stockqqty.toString()
            tvBatchNos.text = item.batchNo
            tvStatus.text = when(item.condition.lowercase()) {
                "good" -> context.getString(R.string.status_good)
                "expired" -> context.getString(R.string.status_expired)
                "defective" -> context.getString(R.string.status_defective)
                "damaged" -> context.getString(R.string.status_damaged)
                "store stock" -> context.getString(R.string.status_store_stock)
                else -> item.condition
            }
            //  etEnterQty.setText("0")
            etRemarks.setText("")
            llExtraFields.visibility = View.GONE

            // Set dynamic background
            val statusColor = when (item.condition.lowercase()) {
                "good" -> ContextCompat.getColor(context, R.color.green)
                "expired" -> ContextCompat.getColor(context, R.color.red)
                "defective", "damaged" -> ContextCompat.getColor(context, R.color.orange)
                else -> ContextCompat.getColor(context, R.color.grey)
            }

            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setColor(statusColor)
            }

            tvStatus.background = bgDrawable
            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white))

            val reasonAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, reasonNames)
            reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spReason.adapter = reasonAdapter
             // 🔒 Disable fields if not editable
            if (!item.isEditable ) {
                //  etEnterQty.visibility = View.GONE
            /*    etEnterQty.isEnabled = false
                etEnterQty.isFocusable = false
                etEnterQty.isClickable = false
                etEnterQty.alpha = 0.5f
                spReason.visibility = View.GONE
                etRemarks.visibility = View.GONE
                llExtraFields.visibility = View.GONE*/
            }
            else {

                // ✅ 1. Handle quantity change
                etEnterQty.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        val enteredQty = etEnterQty.text.toString().toIntOrNull() ?: 0
                        val availableQty = item.returnedQty
                       /* val availableQty = if (item.condition.equals("Good", ignoreCase = true))
                            item.stockqqty else item.returnedQty*/
                        if (enteredQty > availableQty) {
                            etEnterQty.setText("")
                            etEnterQty.setSelection(etEnterQty.text.length)
                            llExtraFields.visibility = View.GONE
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.error_more_than_available, availableQty),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return
                        }




                        /* val isGood = item.condition.equals("good", true)
                         llExtraFields.visibility = if (enteredQty > 0 && isGood) View.VISIBLE else View.GONE
*/
                        // ===== Visibility rules =====
                        val isStoreStock = item.condition.equals("Store Stock", true)
                        if (fromGoodReturned || isStoreStock) {
                            // Only Remarks for good_returned_map items, after qty > 0
                            if (enteredQty > 0) {
                                llExtraFields.visibility = View.VISIBLE
                                spReason.visibility = View.GONE
                                etRemarks.visibility = View.VISIBLE
                                etRemarks.isEnabled = true
                                etRemarks.alpha = 1.0f
                            } else {
                                llExtraFields.visibility = View.GONE
                                etRemarks.setText("")
                            }
                        } else {
                            val isGood = item.condition.equals("good", true)
                            llExtraFields.visibility = if (enteredQty > 0 && isGood) View.VISIBLE else View.GONE
                            // Original behavior:
                            // show Reason (and maybe Remarks depending on selected reason)
                            /* val showExtras = enteredQty > 0 && isGood
                             llExtraFields.visibility = if (showExtras) View.VISIBLE else View.GONE
                             spReason.visibility = if (showExtras) View.VISIBLE else View.GONE

                             if (!showExtras) {
                                 etRemarks.visibility = View.GONE
                                 etRemarks.setText("")
                             }*/

                        }
                        //second
                        /*  val isGood = item.condition.equals("good", true)
                          val showExtras = enteredQty > 0 && (isGood || isGoodFromReturnedMap)
                          llExtraFields.visibility = if (showExtras) View.VISIBLE else View.GONE
                        */  saveOrUpdateSelectedItem(item, enteredQty, etRemarks.text.toString(), spReason.selectedItem?.toString() ?: "")
                    }
                })

                // ✅ 2. Handle remarks change
                etRemarks.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        val qty = etEnterQty.text.toString().toIntOrNull() ?: 0
                        saveOrUpdateSelectedItem(item, qty, s.toString(), spReason.selectedItem?.toString() ?: "")
                    }
                })

                spReason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                        val selectedReason = parent.getItemAtPosition(pos)?.toString()?.trim() ?: ""
                        val enteredQty = etEnterQty.text.toString().toIntOrNull() ?: 0
                        /*   val isGood = item.condition.equals("good", ignoreCase = true)
                                                   val shouldShowRemarks = enteredQty > 0 &&
                                                           (isGood || isGoodFromReturnedMap) &&
                                                           selectedReason.equals("Good", ignoreCase = true)*/
                        /*  val shouldShowRemarks = enteredQty > 0 &&
                                  item.condition.equals("good", ignoreCase = true) &&
                                  selectedReason.equals("Good", ignoreCase = true)



                          etRemarks.visibility = if (shouldShowRemarks) View.VISIBLE else View.GONE
                          etRemarks.isEnabled = shouldShowRemarks
                          etRemarks.alpha = if (shouldShowRemarks) 1.0f else 0.5f

                          if (!shouldShowRemarks) {
                              etRemarks.setText("")
                          }*/
                        val isGood = item.condition.equals("good", ignoreCase = true)
                        val shouldShowRemarks = enteredQty > 0 && (isGood)

                        etRemarks.visibility = if (shouldShowRemarks) View.VISIBLE else View.GONE
                        etRemarks.isEnabled = shouldShowRemarks
                        etRemarks.alpha = if (shouldShowRemarks) 1.0f else 0.5f
                        if (!shouldShowRemarks) etRemarks.setText("")


                        saveOrUpdateSelectedItem(item, enteredQty, etRemarks.text.toString(), selectedReason)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }


        }



    }
    override fun getItemCount(): Int = returnItems.size

    fun getSelectedItems(): List<StockReturnItem> = selectedItems
    private fun isExpired(expiryDateStr: String?): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = sdf.parse(expiryDateStr ?: return false)
            val today = Calendar.getInstance().time
            expiryDate.before(today)
        } catch (e: Exception) {
            false
        }
    }
    private fun saveOrUpdateSelectedItem(item: ReturnBatchItem, qty: Int, remarks: String, reason: String) {
        val isGood = item.condition.equals("good", true)
        val isStoreStock = item.condition.equals("Store Stock", true)
        val fromGoodReturned = item.fromGoodReturnedMap == true
        val allowRemarks = isGood || fromGoodReturned ||isStoreStock
        selectedItems.removeAll {
            it.batch_no == item.batchNo &&
                    it.product_id == item.productId &&
                    it.condition.equals(item.condition, true)
        }

        if (qty > 0) {
            // ✅ treat "Store Stock", "StoreStock", "store_stock" the same
            /* val isStoreStock = item.condition
                 ?.replace("_", "")
                 ?.replace(" ", "")
                 ?.equals("good", ignoreCase = true) == true

             val conditionToSend = if (isStoreStock) "Storestock" else item.condition.lowercase()
             Log.d("condition",conditionToSend)

            */ selectedItems.add(
                StockReturnItem(
                    product_id = item.productId,
                    batch_no = item.batchNo,
                    quantity = qty,
                    condition = item.condition.lowercase(),
                    //  condition = conditionToSend,

                    remarks = if (allowRemarks) remarks else "",
                    fromGoodReturnedMap = fromGoodReturned

                )
            )
        }
    }

}

