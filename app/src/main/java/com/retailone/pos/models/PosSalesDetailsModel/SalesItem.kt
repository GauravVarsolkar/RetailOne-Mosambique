package com.retailone.pos.models.PosSalesDetailsModel

data class SalesItem(
    val distribution_pack: DistributionPack?,
    val distribution_pack_id: String?,
    val product_id: String?,
    val product_name: String?,
    //val quantity: Double,
    val quantity: String?,
    val retail_price: String?,
   // val total_amount: String,
    val total_amount: Double?,
    val whole_sale_price: String?,

    val uom: String?,
    val batchno: String?
)
