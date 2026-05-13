package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import com.retailone.pos.R
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.ProductInventoryModel.PiChildData
import com.retailone.pos.models.ProductInventoryModel.PiData
import com.retailone.pos.models.ProductInventoryModel.PiParentData
import com.retailone.pos.models.ProductInventoryModel.PiRequestModel.ProductInventoryRequest
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.ProductInventoryResponse
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchReq
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchRes
import com.retailone.pos.models.UserProfileModels.UserProfileResponse
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductInventoryViewmodel:ViewModel() {


    val pi_data = MutableLiveData<PiData>()
    val pi_LiveData : LiveData<PiData>
        get() = pi_data

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading

    val inventorydata = MutableLiveData<ProductInventoryResponse>()
    val inventoryLiveData : LiveData<ProductInventoryResponse>
        get() = inventorydata


    fun getPiData(){

        val child_list = arrayListOf<PiChildData>()
        child_list.add(PiChildData("12377","10","ZK 120","sep/2024"))
        child_list.add(PiChildData("12678","16","ZK 190","oct/2024"))
        child_list.add(PiChildData("12300","70","ZK 129","jan/2024"))

        val parent_list = arrayListOf<PiParentData>()
        parent_list.add(PiParentData("MM Gold Premium","1l","30","ml",child_list))
        parent_list.add(PiParentData("MM Gold Premium","1l","30","ml",child_list))
        parent_list.add(PiParentData("MM Gold Premium","1l","30","ml",child_list))
        parent_list.add(PiParentData("MM Gold Premium","1l","30","ml",child_list))


        val data =  PiData(true,parent_list)
        pi_data.postValue(data)


    }

    fun callProductInventoryApi( store_id: Int,context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getProductInventory(ProductInventoryRequest(store_id)).enqueue(object :
            Callback<ProductInventoryResponse> {
            override fun onResponse(call: Call<ProductInventoryResponse>, response: Response<ProductInventoryResponse>) {
                Log.d("categorySubmitResponse", response.body().toString())

                if(response.isSuccessful && response.body()!=null){
                    inventorydata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<ProductInventoryResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }

}
