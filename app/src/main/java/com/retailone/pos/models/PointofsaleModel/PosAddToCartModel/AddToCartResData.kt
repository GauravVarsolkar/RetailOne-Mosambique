package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddToCartResData(
    val distribution_pack_id: Int,
    val distribution_pack: DistributionPackCart,
    val price_without_discount: String,
    val product_id: Int,
    val product_name: String,

    val batch: List<BatchCartItem>,



    /// val quantity: Double,
   // val quantity: Int,
   /// val retail_price: String,
    val stock_id: Int,
    val total: Double
): Parcelable
