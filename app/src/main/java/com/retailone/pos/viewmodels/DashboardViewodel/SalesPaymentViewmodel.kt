package com.retailone.pos.viewmodels.DashboardViewodel

import com.retailone.pos.R

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.google.gson.Gson
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsReq
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsRes
import com.retailone.pos.models.CashupModel.SendOTP.SendOtpRes
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleResponse
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleitemRequest
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceReq
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceRes
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsReq
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsRes
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListReq
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListRes
import com.retailone.pos.network.ApiClient
import com.retailone.pos.ui.Activity.DashboardActivity.SalesAndPaymentActivity
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SalesPaymentViewmodel(application: Application): AndroidViewModel(application) {

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading

    val saleslist_data = MutableLiveData<SalesListRes>()
    val saleslist_liveData: LiveData<SalesListRes>
        get() = saleslist_data

    val salesdetails_data = MutableLiveData<SalesDetailsRes>()
    val salesdetails_liveData: LiveData<SalesDetailsRes>
        get() = salesdetails_data


    val invoice_data = MutableLiveData<InvoiceRes>()
    val invoice_livedata: LiveData<InvoiceRes>
        get() = invoice_data


    fun callCancelSaleAPI(request: CancelSaleitemRequest, context: Context) {
        loading.postValue(ProgressData(isProgress = true))
        val gson = Gson()
        Log.d("SubmitRequestJSON", gson.toJson(request))

        ApiClient().getApiService(context).cancelItemAPI(request).enqueue(object : Callback<CancelSaleResponse> {
            override fun onResponse(call: Call<CancelSaleResponse>, response: Response<CancelSaleResponse>) {
                loading.postValue(ProgressData(isProgress = false))
                Log.d("SubmitResponse", response.body().toString())
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    val localizedMsg = when (res.message?.trim()) {
                        "Invoice already cancelled", "Invoice already cancelled." -> getApplication<Application>().getString(R.string.sales_invoice_already_cancelled)
                        "Invoice created cancelled", "Invoice created cancelled." -> getApplication<Application>().getString(R.string.sales_invoice_created_cancelled)
                        else -> res.message
                    }

                    if (res.status == 1) {
                        Toast.makeText(context, localizedMsg, Toast.LENGTH_LONG).show()

                        // ✅ Redirect to another screen (update with your desired target Activity)
                        val intent = Intent(context, SalesAndPaymentActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, localizedMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.sales_cancel_sale_failed), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CancelSaleResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false))
                Toast.makeText(context, context.getString(R.string.sales_something_went_wrong, t.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
            }
        })
    }



    fun callSalesListApi(salesListReq: SalesListReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getSalesListAPI(salesListReq).enqueue(object :
            Callback<SalesListRes> {
            override fun onResponse(call: Call<SalesListRes>, response: Response<SalesListRes>) {

                if(response.isSuccessful && response.body()!=null){
                    saleslist_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<SalesListRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }

    fun callSalesDetailsApi(salesDetailsReq: SalesDetailsReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getSalesDetailsAPI(salesDetailsReq).enqueue(object :
            Callback<SalesDetailsRes> {
            override fun onResponse(call: Call<SalesDetailsRes>, response: Response<SalesDetailsRes>) {

                if(response.isSuccessful && response.body()!=null){
                    salesdetails_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<SalesDetailsRes>, t: Throwable) {
                Log.d("xxx",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }

    fun callInvoiceApi(invoiceReq: InvoiceReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getInvoiceAPI(invoiceReq).enqueue(object :
            Callback<InvoiceRes> {
            override fun onResponse(call: Call<InvoiceRes>, response: Response<InvoiceRes>) {

                if(response.isSuccessful && response.body()!=null){
                    invoice_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_response) ))
                }
            }

            override fun onFailure(call: Call<InvoiceRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }



}
