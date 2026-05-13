package com.retailone.pos.adapter

import java.util.Locale

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.ItemStockProductCardBinding
import com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks.ReturnBatchItem

import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnItem
import com.retailone.pos.models.GoodsToWarehouseModel.Stocklist.DistributionPack
import com.retailone.pos.models.GoodsToWarehouseModel.Stocklist.Product
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData

// Adapter with expanded card logic


class StockListAdapter(private val products: List<Product>,
                       private val reasonNames: List<String>
) :
    RecyclerView.Adapter<StockListAdapter.StockViewHolder>() {

    private val batchAdapters = mutableListOf<BatchListAdapter>()

    inner class StockViewHolder(val binding: ItemStockProductCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockProductCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockViewHolder(binding)
    }
    // 👇 Same rule the child uses to display tvAvailableQty
    private fun availableQtyFor(item: ReturnBatchItem): Int {
        val isGoodFromReturnedMap = !item.isEditable && item.condition.equals("good", ignoreCase = true)
        return when {
            isGoodFromReturnedMap -> item.returnedQty
            item.condition.equals("good", ignoreCase = true) -> item.stockqqty
            else -> item.returnedQty
        }
    }
  override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
      val product = products[position]
      val context = holder.itemView.context

      with(holder.binding) {
          tvProductName.text = product.product_name
          tvProductCode.text = "${product.product_id}"


          val totalQty = product.distribution_pack_data.sumOf { pack ->
              val stock = pack.stock_quatity ?: 0
              val returned = pack.returned_items?.values?.sum() ?: 0
              val goodreturned = pack.good_returned_items?.values?.sum() ?: 0
              stock + returned + goodreturned
             // stock + returned

          }
          val nonExpiredStockQty = product.distribution_pack_data
              .filterNot { isExpired(it.expiry_date) } // filter out expired
              .sumOf { (it.stock_quatity ?: 0) +  (it.good_returned_items?.values?.sum() ?: 0)  }


          tvstorestockQty.text = context.getString(R.string.units_suffix, nonExpiredStockQty.toString())
          // 🔁 Flatten to the same list the child binds
          val flattenedBatchLists = flattenReturnItems(context, product.product_id, product.distribution_pack_data)

          // ✅ New: compute total available using the SAME rule as the child’s tvAvailableQty
          val overallAvailableQty = flattenedBatchLists.sumOf { availableQtyFor(it) }
         // tvStockQty.text = "$overallAvailableQty units"
           tvStockQty.text = context.getString(R.string.units_suffix, totalQty.toString())
          //tvstorestockQty.text = "${product.distribution_pack_data.sumOf { it.stock_quatity ?: 0 }} units"
        //  val firstStockQty = product.distribution_pack_data.firstOrNull()?.stock_quatity ?: 0
         // tvStockQty.text = "${product.distribution_pack_data.sumOf { it.stock_quatity }} units"
         // tvstorestockQty.text = "${product.distribution_pack_data.} units"
          //tvstorestockQty.text = "$firstStockQty units"
          tvRequestedQty.text = context.getString(R.string.units_suffix, product.previous_requested_quantity.toString())
          //tvStockQty.text = "$totalQty units"


         /* val isExpanded = llExpandable.visibility == View.VISIBLE
          ivExpand.setImageDrawable(
              ContextCompat.getDrawable(context,
                  if (isExpanded) com.retailone.pos.R.drawable.up else com.retailone.pos.R.drawable.down
              )
          )*/
          val isStoreStockZero = product.distribution_pack_data.sumOf { it.stock_quatity ?: 0 } == 0
         // val isTotalStockZero = totalQty == 0
          val isTotalStockZero = totalQty == 0
          val isRequestedQtyZero = product.previous_requested_quantity == 0

// 🔒 Hide expandable layout if everything is zero
          if (isStoreStockZero && isTotalStockZero && isRequestedQtyZero) {
              llExpandable.visibility = View.GONE
              ivExpand.visibility = View.GONE
          } else {
              llExpandable.visibility = View.GONE  // Default collapsed
              ivExpand.visibility = View.VISIBLE

              val isExpanded = llExpandable.visibility == View.VISIBLE
              ivExpand.setImageDrawable(
                  ContextCompat.getDrawable(context,
                      if (isExpanded) R.drawable.up else R.drawable.down
                  )
              )

              ivExpand.setOnClickListener {
                  val expanded = llExpandable.visibility == View.VISIBLE
                  llExpandable.visibility = if (expanded) View.GONE else View.VISIBLE

                  val iconRes = if (expanded) R.drawable.down else R.drawable.up
                  ivExpand.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
              }
          }


          ivExpand.setOnClickListener {
              val expanded = llExpandable.visibility == View.VISIBLE
              llExpandable.visibility = if (expanded) View.GONE else View.VISIBLE

              val iconRes = if (expanded) com.retailone.pos.R.drawable.down else com.retailone.pos.R.drawable.up
              ivExpand.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
          }


          // ✅ Flatten returned_items and pass to child adapter
          val flattenedBatchList = flattenReturnItems(context, product.product_id, product.distribution_pack_data)
         // val batchAdapter = BatchListAdapter(flattenedBatchList)
          val batchAdapter = BatchListAdapter(flattenedBatchList, reasonNames)

          rvBatchList.layoutManager = LinearLayoutManager(context)
          rvBatchList.adapter = batchAdapter

          batchAdapters.add(batchAdapter)
      }
  }

    override fun getItemCount(): Int = products.size

    fun getSelectedItems(): List<StockReturnItem> {
        return batchAdapters.flatMap { it.getSelectedItems() }
    }
    private fun flattenReturnItems(context: Context, productId: Int, batches: List<DistributionPack>): List<ReturnBatchItem> {
        val result = mutableListOf<ReturnBatchItem>()

        for (batch in batches) {
            val totalStockQty = batch.stock_quatity ?: 0

            // ✅ If stock is available, add a new card for "Good"/"Expired" depending on expiry date
            if (totalStockQty > 0) {
                  val condition = if (isExpired(batch.expiry_date)) context.getString(R.string.status_expired) else context.getString(R.string.status_store_stock)
                result.add(
                    ReturnBatchItem(
                        productId = productId,
                        stockqqty = totalStockQty,
                        batchNo = batch.batch_no ?: "",
                        condition = condition,
                        returnedQty = totalStockQty,
                        isEditable = true,
                        expiry_date = batch.expiry_date
                    )
                )
            }

            // ✅ Add returned_items as-is
            val returnedMap = batch.returned_items ?: emptyMap()
            for ((condition, qty) in returnedMap) {
                if(qty > 0){
                    result.add(
                        ReturnBatchItem(
                            productId = productId,
                            stockqqty = totalStockQty,
                            batchNo = batch.batch_no ?: "",
                            condition = condition,
                            returnedQty = qty,
                            isEditable = true,
                            expiry_date = batch.expiry_date
                        )
                    )
                }

            }

            // ✅ Add good_returned_items as non-editable
            val goodReturnedMap = batch.good_returned_items ?: emptyMap()
            for ((condition, qty) in goodReturnedMap) {
                if (qty > 0 /*&& condition != "Good"*/) {
                    result.add(
                        ReturnBatchItem(
                            productId = productId,
                            stockqqty = totalStockQty,
                            batchNo = batch.batch_no ?: "",
                            condition = condition,
                            returnedQty = qty,
                            isEditable = true,
                            expiry_date = batch.expiry_date,
                            fromGoodReturnedMap = true
                        )
                    )
                }
            }
        }

        return result
    }

    /*private fun flattenReturnItems(productId: Int, batches: List<DistributionPack>): List<ReturnBatchItem> {
        val result = mutableListOf<ReturnBatchItem>()

        for (batch in batches) {
            val totalStockQty = batch.stock_quatity ?: 0

            // Show stock only if stock is > 0
            if (totalStockQty > 0) {
                result.add(
                    ReturnBatchItem(

                        productId = productId,
                        stockqqty = totalStockQty,
                        batchNo = batch.batch_no ?: "",
                        condition = "Good",
                        returnedQty = totalStockQty,
                        isEditable = true,
                        expiry_date = batch.expiry_date,

                    )
                )
            }

            // Handle regular returned_items
            val returnedMap = batch.returned_items ?: emptyMap()
            for ((condition, qty) in returnedMap) {
                result.add(
                    ReturnBatchItem(
                        productId = productId,
                        stockqqty = totalStockQty,
                        batchNo = batch.batch_no ?: "",
                        condition = condition,
                        returnedQty = qty,
                        isEditable = true,
                        expiry_date = batch.expiry_date,
                    )
                )
            }

            // Handle good_returned_items as non-editable
            val goodReturnedMap = batch.good_returned_items ?: emptyMap()
            for ((condition, qty) in goodReturnedMap) {
                result.add(
                    ReturnBatchItem(
                        productId = productId,
                        stockqqty = totalStockQty,
                        batchNo = batch.batch_no ?: "",
                        condition = condition,
                        returnedQty = qty,
                        isEditable = false ,
                        expiry_date = batch.expiry_date,// non-editable
                    )
                )
            }
        }

        return result
    }
  */  private fun isExpired(expiryDateStr: String?): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = sdf.parse(expiryDateStr ?: return false)
            val today = java.util.Calendar.getInstance().time
            expiryDate.before(today)
        } catch (e: Exception) {
            false
        }
    }



}

