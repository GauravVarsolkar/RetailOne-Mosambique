package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel

data class Data(
    val buyers_tpin: String,
    val customer_mob_no: String?,
    val customer_name: String?,
    val ej_activation_date: String,
    val ej_no: String,
    val grand_total: String,
    val internal_data: String,
    val receipt_no: String,
    val receipt_sign: String,
    val returned_date: String,
    val returned_invoice_id: String,
    val returned_items: List<ReturnedItem>,
    val sdc_id: String,
    val store: Store,
    val tax: String,
    val tax_amount: String,
    val tax_ex: String,

    val total: Double,
    val tpin_no: String,
    val vat_no: String
)
