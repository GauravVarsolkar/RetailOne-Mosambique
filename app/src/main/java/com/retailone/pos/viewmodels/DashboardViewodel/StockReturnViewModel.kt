package com.retailone.pos.viewmodels.DashboardViewodel

import com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks.StockReturnResponse
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.network.ApiClient

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockReturnViewModel : ViewModel() {
    private val _stockReturns = MutableLiveData<StockReturnResponse>()
    val stockReturns: LiveData<StockReturnResponse> get() = _stockReturns

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun fetchStockReturns(context: Context) {
        _loading.postValue(true)
        ApiClient().getApiService(context).getStockReturns()
            .enqueue(object : Callback<StockReturnResponse> {
                override fun onResponse(call: Call<StockReturnResponse>, response: Response<StockReturnResponse>) {
                    _loading.postValue(false)
                    if (response.isSuccessful) _stockReturns.postValue(response.body())
                }

                override fun onFailure(call: Call<StockReturnResponse>, t: Throwable) {
                    _loading.postValue(false)
                }
            })
    }
}
