package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize

data class PosAddToCartRes(
    val `data`: List<AddToCartResData>,
    val discount_amount: Double,
    val grand_total: String,
    val message: String,
    val status: Int,
    val sub_total: Double,
    val sub_total_after_discount: Double,
    val tax: String,
    val tax_amount: String,
    val spot_discount_percentage: String,
    val spot_discount_amount: String
): Parcelable
