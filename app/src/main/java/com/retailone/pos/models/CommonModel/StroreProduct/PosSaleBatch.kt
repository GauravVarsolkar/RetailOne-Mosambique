package com.retailone.pos.models.CommonModel.StroreProduct


data class PosSaleBatch (
    val batch_no: String,
    val quantity: Double,
    val price: Double,
    var batch_cart_quantity: Double = 0.0,
    var batch_total_du_amount: String = "",

    var dispense_status: Int = 0, //manually added extra // 0 - packed,1- loose not dispensed 2 - loose dispensed//


)
