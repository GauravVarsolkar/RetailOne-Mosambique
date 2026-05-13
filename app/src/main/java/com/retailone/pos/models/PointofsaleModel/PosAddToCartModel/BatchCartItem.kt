package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BatchCartItem (

    val batchno: String,
    val quantity: String,
    val retail_price: String

) : Parcelable
