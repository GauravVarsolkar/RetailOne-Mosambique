package com.retailone.pos.models.ReturnSalesItemModel

data class BatchReturnItem (

    val batch: String,
    val quantity: Double,
    val retail_price: Double,
    val subtotal: Double,
    val sales_item_id: Int,
    var return_quantity: Int,
    val return_reason: String?,
    var batch_return_quantity: Int = 0, //manually added extra
    var batch_refund_amount: Double = 0.0,//manually added extra
    var remark: String? = null

)
