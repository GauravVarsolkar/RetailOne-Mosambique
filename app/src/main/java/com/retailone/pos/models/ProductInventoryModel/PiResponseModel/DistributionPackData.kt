package com.retailone.pos.models.ProductInventoryModel.PiResponseModel

/*
data class DistributionPackData(
    val id: String,
    val no_of_packs: Int,
    val stock_quatity: Double,
    val pack_description: String,
    val retail_price: String?,
    val expiry_date: String?,
    val batch_no: String?,
)*/
data class DistributionPackData(
    val id: String,
    val no_of_packs: Int,
    val stock_quatity: Double,
    val pack_description: String,
    val retail_price: String?,
    val expiry_date: String?,
    val batch_no: String?,
    val returned_items: Map<String, Int>? ,// <-- Important for Good, Expired, Defective
    val good_returned_items: Map<String, Int>? // <-- Important for Good, Expired, Defective
)
