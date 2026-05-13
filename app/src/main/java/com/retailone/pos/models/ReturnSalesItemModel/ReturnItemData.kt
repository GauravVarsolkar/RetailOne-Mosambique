package com.retailone.pos.models.ReturnSalesItemModel

data class ReturnItemData(
    val amount_tendered: Int,
    val created_at: String,
    val customer: Customer,
    val discount_amount: String,
    val grand_total: String,
    val id: Int,
    val invoice_id: String,
    val payment_type: String,
    val salesItems: List<SalesItem>,
    //val sales_items: List<SalesItem>,
    val status: Int,
    val store_details: StoreDetails,
    val store_id: Int,
    val store_manager_details: StoreManagerDetails,
    val store_manager_id: Int,
    val total_refunded_amount:Double,

    val sub_total: Double,
    val spot_discount_amount: String,
    val spot_discount_percentage: Float,

    val subtotal_after_discount: Double,

    val tax: String,
    val tax_amount: String,
    val updated_at: String
)
