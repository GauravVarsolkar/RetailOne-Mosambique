package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel

data class ReturnedItem(
    val created_at: String,
    val distribution_pack_id: Int,
    val distribution_pack_name: String,
    val id: Int,
    val product_id: Int,
    val product_name: String,
    val quantity: Double,

    val retail_price: Double,
    val return_quantity: Int,
    val sales_id: String,
    val status: Int,

    val total_amount: Double,

    val total_returned_amount: Double,
    val updated_at: String,

    val whole_sale_price: Double
)
