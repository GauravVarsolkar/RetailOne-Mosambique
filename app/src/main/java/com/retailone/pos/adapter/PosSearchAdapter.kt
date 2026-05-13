package com.retailone.pos.adapter

import com.retailone.pos.utils.NumberFormatter
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.PosSearchItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.models.CommonModel.StockRequsition.SearchResData
import com.retailone.pos.network.Constants
import com.retailone.pos.utils.BatchUtils
import com.retailone.pos.utils.FunUtils

class PosSearchAdapter(
    private val stockSearchRes: List<StoreProData>,
    val context: Context,
    val onItemClick: (StoreProData) -> Unit
) : RecyclerView.Adapter<PosSearchAdapter.StockSearchViewHolder>() {

    /* private var matrcvd = MaterialReceived()

     fun setMatRcvdData(matrcvd: MaterialReceived) {
         this.matrcvd = matrcvd
         notifyDataSetChanged()
     }*/

    private val sharedPrefHelper = SharedPrefHelper(context)
    val localizationData = LocalizationHelper(context).getLocalizationData()



    class StockSearchViewHolder(val binding: PosSearchItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            PosSearchItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {

        // val type = matrcvd.materiallist[position].type


        val productitem = stockSearchRes[position]

        Log.d("`cd`",stockSearchRes.toString())

        val total_stock = BatchUtils.getTotalPosQuantity(productitem.batch)

        if (total_stock > 0) {
            holder.binding.cartLayout.visibility = View.VISIBLE
        } else {
            holder.binding.cartLayout.visibility = View.GONE
            val layoutParams = holder.binding.cartLayout.layoutParams
            layoutParams.height = 0
            holder.binding.cartLayout.layoutParams = layoutParams
        }
        // visibility seting of cart item



        val isLooseOil = FunUtils.isLooseOil(productitem.category_id,productitem.pack_product_description)


        holder.binding.itemName.text = productitem.product_name
        holder.binding.itemDesc.text = productitem.pack_product_description+" ("+productitem.uom+") "

        val formattedPrice = NumberFormatter().formatPrice(productitem.retail_price?:"-",localizationData)
        holder.binding.itemPrice.text = formattedPrice


       /* holder.binding.addcart.setOnClickListener {
            onItemClick(productitem)
        }*/

        //if (productitem.stock_quantity > 0) {
        if (BatchUtils.getTotalPosQuantity(productitem.batch) > 0) {
            holder.binding.addlayout.isVisible = true

           // holder.binding.itemUnit.text = "${productitem.stock_quantity} Units"
          //  holder.binding.itemUnit.text = "${if(isLooseOil) FunUtils.DtoDouble(productitem.stock_quantity) else FunUtils.DtoInt(productitem.stock_quantity)} ${if (isLooseOil) "Liters" else "Units"}"
            holder.binding.itemUnit.text = "${FunUtils.DtoString(BatchUtils.getTotalPosQuantity(productitem.batch))} ${if (isLooseOil) "Liters" else "Units"}"

            holder.binding.itemUnit.setTextColor(Color.parseColor("#008000"))

            holder.binding.addcart.setOnClickListener {
                onItemClick(productitem)
            }

        } else {
            holder.binding.quantityContainer.isVisible = false
            holder.binding.itemUnit.text = "Out Of Stock"
            holder.binding.itemUnit.setTextColor(Color.parseColor("#FF0000"))
        }


        /*  if(productitem.stock_quantity > 0){
              holder.binding.quantityContainer.isVisible = true
              holder.binding.addlayout.isVisible = true

              holder.binding.itemUnit.text = "${productitem.stock_quantity} Units"
              holder.binding.itemUnit.setTextColor(Color.parseColor("#008000"))

              holder.binding.addcart.setOnClickListener {
                  sharedPrefHelper.saveSearchItem(productitem)
                  //initial quantity ad
                  sharedPrefHelper.updateQuantity(productitem.product_id,productitem.distribution_pack_id,"1")

                  holder.binding.addlayout.isVisible = false
                  holder.binding.plusMinusLayout.isVisible = true

                  holder.binding.cartProductQuantity.text = "1"

              }

          }else{
              holder.binding.quantityContainer.isVisible = false
              holder.binding.itemUnit.text = "Out Of Stock"
              holder.binding.itemUnit.setTextColor(Color.parseColor("#FF0000"))
          }

          cartControl(holder.binding,productitem)*/


        /* holder.binding.productimg.setOnClickListener {
             // Handle item click
             // Add the clicked item to the shared preferences list

            // val updatedList = sharedPrefHelper.getSearchResultsList().toMutableList()
            // updatedList.add(productitem)
            // sharedPrefHelper.saveSearchResultsList(updatedList)

             //Toast.makeText(holder.itemView.context,"dfgh",Toast.LENGTH_SHORT).show()

             // You may also perform other actions related to item click

             sharedPrefHelper.saveSearchItem(productitem)
         }*/





        Glide.with(context)
            .load(Constants.IMAGE_URL + productitem.product_photo)
            .centerCrop() // Add center crop
            .placeholder(R.drawable.temp) // Add a placeholder drawable
            .error(R.drawable.temp) // Add an error drawable (if needed)
            .into(holder.binding.productimg)


    }

    private fun cartControl(binding: PosSearchItemLayoutBinding, productitem: SearchResData) {

        binding.cartPlusImg.setOnClickListener {
            var value = binding.cartProductQuantity.text.toString().toInt()

            if (value < productitem.stock_quantity.toInt()) {
                var newquantity = ++value
                binding.cartProductQuantity.text = (newquantity).toString()
                sharedPrefHelper.updateQuantity(
                    productitem.product_id,
                    productitem.distribution_pack_id,
                    newquantity.toString()
                )
            } else {
                Toast.makeText(context, "Can't Add more item,Limit exceed", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.cartMinusImg.setOnClickListener {
            var value = binding.cartProductQuantity.text.toString().toInt()

            if (value > 1) {
                var newquantity = --value

                binding.cartProductQuantity.text = (newquantity).toString()
                sharedPrefHelper.updateQuantity(
                    productitem.product_id,
                    productitem.distribution_pack_id, (newquantity).toString()
                )
            } else if (value == 1) {
                binding.addlayout.isVisible = true
                binding.plusMinusLayout.isVisible = false
                sharedPrefHelper.removeItem(
                    productitem.product_id,
                    productitem.distribution_pack_id
                )

            }
        }


    }


    override fun getItemCount(): Int {
        return stockSearchRes.size
    }


    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: PosSearchAdapter.StockSearchViewHolder) {
        super.onViewRecycled(holder)
        //holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
    }
}

