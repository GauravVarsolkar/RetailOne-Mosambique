package com.retailone.pos.viewmodels.DashboardViewodel
import android.content.Context
import com.retailone.pos.R
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemRes
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.SalesListRequest
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.SalesReturnReasonRes
import com.retailone.pos.models.SalesListResponse
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReturnSalesDetailsViewmodel : ViewModel() {

    val returnitem_data = MutableLiveData<ReturnItemRes>()
    val returnitem_liveData: LiveData<ReturnItemRes>
        get() = returnitem_data

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading

    val returnsalesubmit_data = MutableLiveData<ReturnSaleRes>()
    val returnsalesubmit_liveData: LiveData<ReturnSaleRes>
        get() = returnsalesubmit_data

    val salesreturnreason_data = MutableLiveData<SalesReturnReasonRes>()
    val salesreturnreason_liveData: LiveData<SalesReturnReasonRes>
        get() = salesreturnreason_data

    val salesListLiveData = MutableLiveData<SalesListResponse>()
    /*val salesList_liveData: LiveData<SalesReturnReasonRes>
        get() = salesListLiveData*/
   // val loading = MutableLiveData<ProgressData>()

    fun callSalesListApi(context: Context, storeId: String) {
        loading.postValue(ProgressData(isProgress = true))

        val request = SalesListRequest(store_id = storeId)

        ApiClient().getApiService(context).getSalesList(days = 7, request)
            .enqueue(object : Callback<SalesListResponse> {
                override fun onResponse(
                    call: Call<SalesListResponse>,
                    response: Response<SalesListResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        salesListLiveData.postValue(response.body())
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = context.getString(R.string.sales_fetch_failed)
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<SalesListResponse>, t: Throwable) {
                    Log.e("SalesListAPI", "Error: ${t.localizedMessage}")
                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = context.getString(R.string.sales_something_went_wrong, t.message ?: "")
                        )
                    )
                }
            })
    }


    fun callReturnSalesDetailsApi(returnItemReq: ReturnItemReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))
        Log.e("SalesListAPIRequest", "Request: ${returnItemReq}")
        ApiClient().getApiService(context).getReturnSalesItemAPI(returnItemReq).enqueue(object :
            Callback<ReturnItemRes> {
            override fun onResponse(call: Call<ReturnItemRes>, response: Response<ReturnItemRes>) {
                Log.e("SalesListAPIResponse", "Response: ${response.body()}")
                if(response.isSuccessful && response.body()!=null){
                    returnitem_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false,))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<ReturnItemRes>, t: Throwable) {
                Log.d("rty",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }


    fun callReturnSalesSubmitApi(returnSaleReq: ReturnSaleReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))


        Log.e("SalesListAPIRequestreturn", "Request: ${returnSaleReq}")
        ApiClient().getApiService(context).getReturnSalesSubmitAPI(returnSaleReq).enqueue(object :
            Callback<ReturnSaleRes> {
            override fun onResponse(call: Call<ReturnSaleRes>, response: Response<ReturnSaleRes>) {

                if(response.isSuccessful && response.body()!=null){
                    returnsalesubmit_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false,))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<ReturnSaleRes>, t: Throwable) {
                Log.d("rty",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }


    fun callSaleReturnReasonApi( context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getReturnReasonAPI().enqueue(object :
            Callback<SalesReturnReasonRes> {
            override fun onResponse(call: Call<SalesReturnReasonRes>, response: Response<SalesReturnReasonRes>) {

                if(response.isSuccessful && response.body()!=null){
                    salesreturnreason_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false,))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<SalesReturnReasonRes>, t: Throwable) {
                Log.d("rty",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }


}
