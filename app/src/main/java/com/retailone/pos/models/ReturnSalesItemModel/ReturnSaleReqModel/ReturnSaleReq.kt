package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel

data class ReturnSaleReq(
    val reason_id: Int,
    val returned_items: List<ReturnedItem>,
    val sales_id: Int,
    val store_id: Int,
    val store_manager_id: Int,
    val return_date_time: String,
)
