package com.retailone.pos.ui.Activity.DashboardActivity

import com.retailone.pos.utils.NumberFormatter
import com.retailone.pos.BaseActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.adapter.SalesDetailsAdapter
import com.retailone.pos.databinding.ActivitySalesPaymentDetailsBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.Dispatch.DispatchRequest
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleitemRequest
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsReq
import com.retailone.pos.utils.DateTimeFormatting
import com.retailone.pos.viewmodels.DashboardViewodel.SalesPaymentViewmodel

class SalesPaymentDetailsActivity : BaseActivity() {

    lateinit var  binding: ActivitySalesPaymentDetailsBinding
    lateinit var viewmodel: SalesPaymentViewmodel
    lateinit var salesDetailsAdapter: SalesDetailsAdapter
    lateinit var  localizationData: LocalizationData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         binding = ActivitySalesPaymentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewmodel = ViewModelProvider(this)[SalesPaymentViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()



        enableBackButton()
        setToolbarImage()

        prepareRecycleview()

        val saleid = intent?.getStringExtra("sale_id")



        saleid?.let {
            viewmodel.callSalesDetailsApi(SalesDetailsReq(it),this)

        }

        viewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        viewmodel.salesdetails_liveData.observe(this){

            if(it.status==1){
                val salesdata = it.data[0]

                val formattedPrice = NumberFormatter().formatPrice(salesdata.grand_total.toString(),localizationData)

                val pt = salesdata.payment_type ?: ""
                val localizedPaymentType = when {
                    pt.equals("Cash", true) -> getString(R.string.cashup_cash)
                    pt.equals("M-Money", true) -> getString(R.string.cashup_mmoney)
                    else -> pt
                }

                binding.apply {
                    orderId.text = getString(R.string.sales_id_label) + ": " + salesdata?.invoice_id?.toString()
                    date.text = getString(R.string.sales_date_label) + ": " + DateTimeFormatting.formatGlobalTime(salesdata.created_at, localizationData.timezone)
                    val spotPercent = salesdata.spot_discount_percentage?.toDoubleOrNull() ?: 0.0
                    val spotAmount = salesdata.spot_discount_amount?.toDoubleOrNull() ?: 0.0
                    if (spotPercent > 0.0 || spotAmount > 0.0) {
                        val discountPercent = if (spotPercent % 1.0 == 0.0) spotPercent.toInt().toString() else spotPercent.toString()
                        spotDiscountPercentText.isVisible = true
                        spotDiscountAmountText.isVisible = true
                        spotDiscountPercentText.text = getString(R.string.sales_spot_discount_percent_format, discountPercent)
                        spotDiscountAmountText.text = getString(R.string.sales_spot_discount_amount_label) + " " + NumberFormatter().formatPrice(salesdata.spot_discount_amount.toString(), localizationData)
                    } else {
                        spotDiscountPercentText.isVisible = false
                        spotDiscountAmountText.isVisible = false
                    }
                    grandtotal.text = getString(R.string.sales_grand_total_label) + ": $formattedPrice"
                    paymenttype.text = getString(R.string.sales_payment_type_label) + ": " + localizedPaymentType
                    storename.text = getString(R.string.sales_store_name_label) + ": " + (salesdata.store_details.store_name ?: "")
                  //  vat.text = "(+) Tax @"+(salesdata.tax?:"")+"%"+":   "+"ZWL"+(salesdata.tax_amount?:"")
                    //customername.text = "Customer name: "+(salesdata.customer.customer_name?:"")
                    customername.text = getString(R.string.sales_customer_name_label) + ": " + (salesdata.customer?.customer_name ?: "N/A")
                    customervat.text = getString(R.string.sales_customer_iva_label) + ": " + (salesdata.vat_no ?: "N/A")
// 👇 Hide cancel button if grand_total is negative
                  // binding.btnConfirmcancel.isVisible = salesdata.grand_total >= 0
                    binding.btnConfirmcancel.isVisible =
                        salesdata.grand_total >= 0 && salesdata.total_refunded_amount <= 0.0


                }

                salesDetailsAdapter = SalesDetailsAdapter(this,salesdata)

                binding.itemsRcv.adapter = salesDetailsAdapter


                binding.btnConfirmcancel.setOnClickListener {
                    //var invoiceId = binding.orderId.text.toString().trim()
                    val invoiceIdRaw = binding.orderId.text.toString().trim()
                    val invoiceId = invoiceIdRaw.replace("ID:", "").trim()  // ✅ clean prefix


                    if (invoiceId.isEmpty()) {
                        Toast.makeText(this, getString(R.string.sales_invoice_id_required), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val request = CancelSaleitemRequest(
                        invoiceID = invoiceId

                    )
                    Log.d("CancelSale", "Calling API with invoiceId: $invoiceId")

                    viewmodel.callCancelSaleAPI(request, this)
                }

            }


        }



    }


    private fun showMessage(msg: String) {
        Toast.makeText(this@SalesPaymentDetailsActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun prepareRecycleview() {

        binding.itemsRcv.apply {
            layoutManager = LinearLayoutManager(this@SalesPaymentDetailsActivity,
                RecyclerView.VERTICAL,false)

        }
    }



    private fun setToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter() // Add center crop
            .placeholder(R.drawable.mlogo) // Add a placeholder drawable
            .error(R.drawable.mlogo) // Add an error drawable (if needed)
            .into(binding.image)
    }
    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.sales_payment)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
