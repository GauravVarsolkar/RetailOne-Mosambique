package com.retailone.pos.adapter
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Locale
import com.retailone.pos.utils.NumberFormatter
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.MaterialreceivedItemLayoutBinding
import com.retailone.pos.databinding.ReturnItemBatchLayoutBinding
import com.retailone.pos.databinding.ReturnItemLayoutBinding
import com.retailone.pos.interfaces.OnReturnQuantityChangeListener
import com.retailone.pos.localstorage.SharedPreference.LocalReturnCartHelper
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvItem
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnCalculation
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnedItem
import com.retailone.pos.models.ReturnSalesItemModel.SalesItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.OrderItem
import com.retailone.pos.utils.FunUtils

class ReturnSalesItemBatchAdapter(
    private val returnitems: List<ReturnItemData>,
    val context: Context,
    val returnbatchlist : List<BatchReturnItem>,
    val readonlyMode: Boolean = false,
    val onBatchQuantityChange: (List<BatchReturnItem>) -> Unit

    //  val onBatchQuantityChange: (List<BatchReturnItem>) -> Unit
) : RecyclerView.Adapter<ReturnSalesItemBatchAdapter.StockSearchViewHolder>() {



    private var subtotal = 0.0
    private var taxTotal = 0.0
    private var grandTotal = 0.0
    private val sharedPrefHelper = SharedPrefHelper(context)
    val localizationData = LocalizationHelper(context).getLocalizationData()

    private val filteredList = returnbatchlist.filter { it.quantity > 0.0 }

    // Global list to maintain received quantities
    private val matReceivedList = mutableListOf<BatchReturnItem>()
    init {
        matReceivedList.clear()
    }

   /* init {
        val filteredList = returnbatchlist.filter { it.quantity > 0.0 }
        matReceivedList.addAll(filteredList.map {
            BatchReturnItem(
                it.batch,
                it.quantity,
                it.retail_price,
                it.subtotal,
                it.sales_item_id,
                it.return_quantity,
                it.return_reason,
                it.batch_return_quantity,
                it.batch_refund_amount,

                )
        })

            }*/



    class StockSearchViewHolder(val binding: ReturnItemBatchLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            ReturnItemBatchLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }



    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {
        val retunitemitem = filteredList[position]

        val formattedPrice = NumberFormatter().formatPrice(
            retunitemitem.retail_price.toString() ?: "-", localizationData
        )
        val taxDisplay = formatTaxForDisplay(returnitems.get(0).tax)
       // val taxDisplay = formatPercent(returnitems.get(0).tax)
        holder.binding.apply {

            if (readonlyMode) {
                // Disable quantity
                // input
                quantityLayouts.isVisible = true
                batchNames.text = retunitemitem.batch
                batchPrices.text = formattedPrice
                saleQuantitys.text = retunitemitem.quantity.toString()


                batchNames.isVisible = true
                batchPrices.isVisible = true
                saleQuantitys.isVisible = true

                returnQuantitys.isEnabled = false
                returnQuantitys.isFocusable = false
                returnQuantitys.setText(retunitemitem.return_quantity.toString())
                returnReasonLayout.isVisible = true
                returnReason.isVisible = true
                returnReason.isSelected = true
                returnReason.isEnabled = false
                returnReason.isFocusable = false
                paymentcards.isVisible = true
                calculateTotalss(holder,retunitemitem)
               // returnReason.setText(retunitemitem.return_reason.toString())
                returnReason.setText(retunitemitem.return_reason ?: "Not Given")



            } else {
                quantityLayout.isVisible = true
                batchName.isVisible = true
                batchPrice.isVisible = true
                saleQuantity.isVisible = true
                paymentcard.isVisible = true
                batchName.text = retunitemitem.batch
                batchPrice.text = formattedPrice
                saleQuantity.text = retunitemitem.quantity.toString()

                // Editable Mode
                // tvReturnReason.isVisible = false

                returnReasonLayout.isVisible = false
                returnReason.isVisible = false
                returnQuantity.setText("") // Clear field
                returnQuantity.isEnabled = true
                returnQuantity.isFocusable = true


                holder.binding.taxfield.setText("(+) " + context.getString(R.string.pos_tax) + " @"+taxDisplay)

                returnQuantity.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val input = s.toString().trim()
                        val enteredValue = input.toIntOrNull()

                        if (input.isEmpty() || enteredValue == null || enteredValue == 0) {
                            // ✅ Force remove from list
                            updateReceivedQuantity(retunitemitem, 0)

                            // Reset this item
                            retunitemitem.batch_return_quantity = 0
                            retunitemitem.batch_refund_amount = 0.0

                            // Reset UI
                            holder.binding.subtotals.text = "RWF0.00"
                            holder.binding.taxAmounts.text = "RWF0.00"
                            holder.binding.alltotalAmounts.text = "RWF0.00"

                            if (input.isNotEmpty()) {
                                Toast.makeText(context, "Quantity cannot be 0", Toast.LENGTH_SHORT).show()
                                returnQuantity.text = null
                            }

                            return
                        }else {

                            if (enteredValue > retunitemitem.quantity.toInt()) {
                                Toast.makeText(
                                    context,
                                    "Entered quantity exceeds the quantity purchased.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                returnQuantity.text = null
                                return
                            }

                            // ✅ Valid input — update both model and list
                            retunitemitem.batch_return_quantity = enteredValue
                            retunitemitem.batch_refund_amount =
                                enteredValue * retunitemitem.retail_price
                            updateReceivedQuantity(retunitemitem, enteredValue)
                            calculateTotals(holder, retunitemitem)
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {

                    }
                })
                updateBtn.setOnClickListener {
                    // Check if item already saved in local cart
                    val savedItemList = LocalReturnCartHelper.getCartItems(context)
                    var isSaved = savedItemList.any { it.id == retunitemitem.sales_item_id }

// Initial setup
                    updateBtn.setImageResource(if (isSaved) R.drawable.delete else R.drawable.pencil)
                    returnQuantity.isEnabled = !isSaved
                    returnQuantity.isFocusable = !isSaved
                    returnQuantity.isFocusableInTouchMode = !isSaved

// Save or Delete functionality
                    updateBtn.setOnClickListener {
                        val currentList = LocalReturnCartHelper.getCartItems(context).toMutableList()
                        val exists = currentList.any { it.id == retunitemitem.sales_item_id }

                        if (exists) {
                            // 🔴 DELETE MODE
                            currentList.removeAll { it.id == retunitemitem.sales_item_id }
                            LocalReturnCartHelper.saveList(context, currentList) // Custom method needed
                            Toast.makeText(context, "Item removed from cart", Toast.LENGTH_SHORT).show()

                            // UI changes
                            returnQuantity.isEnabled = true
                            returnQuantity.isFocusable = true
                            returnQuantity.isFocusableInTouchMode = true
                            updateBtn.setImageResource(R.drawable.pencil)
                        } else {
                            // ✅ SAVE MODE
                            val input = returnQuantity.text.toString().trim()
                            val returnQty = input.toIntOrNull()

                            if (returnQty != null && returnQty > 0) {
                                val item = ReturnedItem(
                                    id = retunitemitem.sales_item_id,
                                    return_quantity = returnQty
                                )
                                LocalReturnCartHelper.saveSingleItem(context, item)
                                Toast.makeText(context, context.getString(R.string.pos_saved_to_cart), Toast.LENGTH_SHORT).show()

                                // UI changes
                                returnQuantity.isEnabled = false
                                returnQuantity.isFocusable = false
                                updateBtn.setImageResource(R.drawable.delete)
                            } else {
                                Toast.makeText(context, "Enter valid return quantity", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    /* val input = returnQuantity.text.toString().trim()
                     val returnQty = input.toIntOrNull()

                     if (returnQty != null && returnQty > 0) {
                         val item = ReturnedItem(
                             id = retunitemitem.sales_item_id,
                             return_quantity = returnQty
                         )
                         LocalReturnCartHelper.saveSingleItem(context, item)
                         Toast.makeText(context, context.getString(R.string.pos_saved_to_cart), Toast.LENGTH_SHORT).show()
                     } else {
                         Toast.makeText(context, "Enter valid return quantity", Toast.LENGTH_SHORT).show()
                     }*/
                }



            }
        }
    }



    override fun getItemCount(): Int = filteredList.size
    //alredy retun only show
    private fun calculateTotalss(holder: StockSearchViewHolder, item: BatchReturnItem) {
        val taxPercent = parseTaxPercent(returnitems.firstOrNull()?.tax)
        val spotDiscountPercent = returnitems.firstOrNull()?.spot_discount_percentage?.toDouble() ?: 0.0

        // ✅ use return_quantity, fall back to batch_return_quantity if 0
        val qty = if (item.return_quantity > 0) item.return_quantity else item.batch_return_quantity

        if (qty > 0) {
            val itemTotal = qty * item.retail_price
            val spotDiscountAmount = itemTotal * spotDiscountPercent / 100
            val discountedBase = itemTotal - spotDiscountAmount
            val taxAmount = (discountedBase * taxPercent / 100.0)
            val grandTotal = itemTotal + taxAmount

            holder.binding.subtotalss.text =
                NumberFormatter().formatPrice(String.format(Locale.US, "%.2f", itemTotal), localizationData)
            holder.binding.taxAmountss.text =
                NumberFormatter().formatPrice(String.format(Locale.US, "%.2f", taxAmount), localizationData)
            holder.binding.alltotalAmountss.text =
                NumberFormatter().formatPrice(String.format(Locale.US, "%.2f", grandTotal), localizationData)
        } else {
            holder.binding.subtotalss.text = "RWF0.00"
            holder.binding.taxAmountss.text = "RWF0.00"
            holder.binding.alltotalAmountss.text = "RWF0.00"
        }

        holder.binding.taxfields.text = "(+) Tax @${formatPercent(taxPercent)}%"
    }

    //not return edit
    private fun calculateTotals(holder: StockSearchViewHolder, item: BatchReturnItem) {
        val taxPercent = parseTaxPercent(returnitems.firstOrNull()?.tax)
        val spotDiscountPercent = returnitems.firstOrNull()?.spot_discount_percentage?.toDouble() ?: 0.0

        val qty = item.batch_return_quantity
        if (qty > 0) {
            val itemTotal = qty * item.retail_price
            val spotDiscountAmount = itemTotal * spotDiscountPercent / 100
            val discountedBase = itemTotal - spotDiscountAmount
            val taxAmount = (discountedBase * taxPercent / 100.0)
            val grandTotal = itemTotal + taxAmount

            holder.binding.subtotals.text =
                NumberFormatter().formatPrice(String.format(Locale.US, "%.2f", itemTotal), localizationData)
            holder.binding.taxAmounts.text =
                NumberFormatter().formatPrice(String.format(Locale.US, "%.2f", taxAmount), localizationData)
            holder.binding.alltotalAmounts.text =
                NumberFormatter().formatPrice(String.format(Locale.US, "%.2f", grandTotal), localizationData)
        } else {
            holder.binding.subtotals.text = "RWF0.00"
            holder.binding.taxAmounts.text = "RWF0.00"
            holder.binding.alltotalAmounts.text = "RWF0.00"
        }

        holder.binding.taxfield.text = "(+) Tax @${formatPercent(taxPercent)}%"
    }


 /*  private fun calculateTotalss(holder: StockSearchViewHolder, item: BatchReturnItem) {
       val taxPercent = returnitems.firstOrNull()?.tax?.toDoubleOrNull() ?: 0.0

       val qty = item.return_quantity
       Log.d("Quantity",qty.toString())

       if (qty > 0) {
           val itemTotal = qty * item.retail_price
           val taxAmount = itemTotal * taxPercent / 100
           val grandTotal = itemTotal + taxAmount

           holder.binding.subtotalss.text = NumberFormatter().formatPrice(String.format("%.2f", itemTotal), localizationData)
           holder.binding.taxAmountss.text = NumberFormatter().formatPrice(String.format("%.2f", taxAmount), localizationData)
           holder.binding.alltotalAmountss.text = NumberFormatter().formatPrice(String.format("%.2f", grandTotal), localizationData)
       }
       else {
           // Reset all values if qty is zero
           holder.binding.subtotalss.text = "RWF0.00"
           holder.binding.taxAmountss.text = "RWF0.00"
           holder.binding.alltotalAmountss.text = "RWF0.00"
       }

       holder.binding.taxfields.text = "(+) " + context.getString(R.string.pos_tax) + " @${taxPercent.toInt()}%"
   }

    private fun calculateTotals(holder: StockSearchViewHolder, item: BatchReturnItem) {
      val taxPercent = returnitems.firstOrNull()?.tax?.toDoubleOrNull() ?: 0.0

       val qty = item.batch_return_quantity

       if (qty > 0) {
           val itemTotal = qty * item.retail_price
           val taxAmount = itemTotal * taxPercent / 100
           val grandTotal = itemTotal + taxAmount

           holder.binding.subtotals.text = NumberFormatter().formatPrice(String.format("%.2f", itemTotal), localizationData)
           holder.binding.taxAmounts.text = NumberFormatter().formatPrice(String.format("%.2f", taxAmount), localizationData)
           holder.binding.alltotalAmounts.text = NumberFormatter().formatPrice(String.format("%.2f", grandTotal), localizationData)
       }
       else {
           // Reset all values if qty is zero
           holder.binding.subtotals.text = "RWF0.00"
           holder.binding.taxAmounts.text = "RWF0.00"
           holder.binding.alltotalAmounts.text = "RWF0.00"
       }

       holder.binding.taxfield.text = "(+) " + context.getString(R.string.pos_tax) + " @${taxPercent.toInt()}%"
   }*/






    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: ReturnSalesItemBatchAdapter.StockSearchViewHolder) {
        super.onViewRecycled(holder)
        //holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
    }



/*

  private fun updateReceivedQuantity(item: BatchReturnItem, value: Int) {
      val batchKey = item.batch?.trim()?.lowercase() ?: ""
      val salesItemId = item.sales_item_id

      val index = matReceivedList.indexOfFirst {
          it.batch?.trim()?.lowercase() == batchKey && it.sales_item_id == salesItemId
      }

      Log.d("ReturnAdapter", "Updating item: ${item.batch} (qty: $value), FoundIndex: $index")

      if (value > 0) {
          if (index != -1) {
              matReceivedList[index].batch_return_quantity = value
              matReceivedList[index].batch_refund_amount = value * item.retail_price
          } else {
              matReceivedList.add(
                  item.copy(
                      batch_return_quantity = value,
                      batch_refund_amount = value * item.retail_price
                  )
              )
          }
      } else {
          if (index != -1) {
              Log.d("ReturnAdapter", "Removed ${matReceivedList[index].batch} from matReceivedList")
              matReceivedList.removeAt(index)
          } else {
              Log.d("ReturnAdapter", "Item to remove not found in matReceivedList")
          }
      }

      val validItems = matReceivedList.filter { it.batch_return_quantity > 0 }

      Log.d("ReturnAdapter", "Filtered validItems: ${validItems.map { it.batch to it.batch_return_quantity }}")
      Log.d("ReturnAdapter", "matReceivedList after update: ${matReceivedList.map { it.batch to it.batch_return_quantity }}")

      onBatchQuantityChange(validItems)
  }*/


    private fun updateReceivedQuantity(item: BatchReturnItem, value: Int) {
        val batchKey = item.batch?.trim()?.lowercase() ?: ""
        val salesItemId = item.sales_item_id

        // Always remove the old entry
        matReceivedList.removeAll {
            it.batch?.trim()?.lowercase() == batchKey && it.sales_item_id == salesItemId
        }

        if (value > 0) {
            // Only add if value > 0
            matReceivedList.add(
                item.copy(
                    batch_return_quantity = value,
                    batch_refund_amount = value * item.retail_price
                )
            )
        }

        // Final filter again to make sure list is clean
       /* val validItems = matReceivedList.filter { it.batch_return_quantity > 0 }

        Log.d("ReturnAdapter", "Filtered validItems: ${validItems.map { it.batch to it.batch_return_quantity }}")
        Log.d("ReturnAdapter", "matReceivedList after update: ${matReceivedList.map { it.batch to it.batch_return_quantity }}")

        onBatchQuantityChange(validItems)*/
        // Final filter again to make sure list is clean
        val validItems = matReceivedList.filter { it.batch_return_quantity > 0 }

        Log.d("ReturnAdapter", "Filtered validItems: ${validItems.map { it.batch to it.batch_return_quantity }}")
        Log.d("ReturnAdapter", "matReceivedList after update: ${matReceivedList.map { it.batch to it.batch_return_quantity }}")

        if (validItems.isEmpty()) {
            Toast.makeText(context, "No items selected for return", Toast.LENGTH_SHORT).show()
            return // ⛔ Don't pass empty list to backend
        }

        onBatchQuantityChange(validItems) // ✅ Only pass valid items with quantity > 0

    }


    fun getValidReturnBatches(): List<BatchReturnItem> {
        return matReceivedList.filter { it.batch_return_quantity > 0 }
    }





    // Converts "16.5", "16,5", "16.50%", or "165" -> "16.5"


    // Display as percent: "18", "165", "16.5%", "0.18" -> "18%", "16.5%", "16.5%", "18%"
    private fun formatTaxForDisplay(raw: Any?): String {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return "0%"

        // keep digits and one decimal sep; accept comma or dot
        val s1 = s0.replace(Regex("[^0-9.,]"), "").replace(',', '.')
        if (s1.isEmpty() || s1 == ".") return "0%"

        fun BigDecimal.pretty(): String {
            val x = this.stripTrailingZeros()
            val plain = x.toPlainString()
            return if (plain.endsWith(".0")) plain.dropLast(2) else plain
        }

        return try {
            val value: BigDecimal =
                if (s1.contains('.')) {
                    // decimal given; treat <=1 as fraction (0.18 -> 18)
                    val d = BigDecimal(s1)
                    if (d <= BigDecimal.ONE) d.multiply(BigDecimal(100)) else d
                } else {
                    // integer; decide if it needs de-scaling
                    val n = s1.toLong()
                    when {
                        n <= 100 -> BigDecimal(n)                             // already a %
                        n <= 1000 -> BigDecimal(n).divide(BigDecimal.TEN)     // 165 -> 16.5
                        else -> BigDecimal(n).divide(BigDecimal(100))         // 1800 -> 18
                    }
                }

            value.pretty() + "%"
        } catch (_: Exception) {
            // fallback: raw digits + %
            s1.trimEnd('.') + "%"
        }
    }



    // Parse a percent like "16.5", "16,5", "16", "16.50%", or legacy "165" -> 16.5
    private fun parseTaxPercent(raw: Any?): Double {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return 0.0

        // keep digits + one decimal separator, accept comma/dot; strip % and spaces
        val cleaned = s0.replace(Regex("[^0-9.,-]"), "").replace(',', '.')
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "-") return 0.0

        val value = cleaned.toDoubleOrNull() ?: return 0.0

        // Heuristic for legacy values like "165" (meant 16.5). If no decimal in original
        // string and value >= 100, first try ÷10; if still >100, fall back to ÷100.
        if (!s0.contains('.') && !s0.contains(',') && value >= 100) {
            val by10 = value / 10.0
            return if (by10 <= 100) by10 else value / 100.0
        }
        return value
    }

    private fun formatPercent(p: Double): String {
        val df = DecimalFormat("#.##")
        return df.format(p)
    }



}

