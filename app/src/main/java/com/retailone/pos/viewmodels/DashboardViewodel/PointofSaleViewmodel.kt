package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import com.retailone.pos.R
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.retailone.pos.models.AddNewCustomerModel.AddNewCustReq
import com.retailone.pos.models.AddNewCustomerModel.AddNewCustRes
import com.retailone.pos.models.GetCustomerModel.getCustomerReq
import com.retailone.pos.models.GetCustomerModel.getCustomerRes
import com.retailone.pos.models.PointofsaleModel.PointOfSaleItem
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartReq
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartRes
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleReq
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleRes
import com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel.SearchStoreProBarcodeReq
import com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel.SearchStoreProBarcodeRes
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProReq
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProRes
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import com.retailone.pos.network.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PointofSaleViewmodel: ViewModel() {


    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    val storeProSearchdata = MutableLiveData<SearchStoreProRes>()
    val storeProSearchLivedata : LiveData<SearchStoreProRes>
        get() = storeProSearchdata

    val storeProSearchBarcodedata = MutableLiveData<SearchStoreProBarcodeRes>()
    val storeProSearchBarcodeLivedata : LiveData<SearchStoreProBarcodeRes>
        get() =  storeProSearchBarcodedata

    val posAddtocartData = MutableLiveData<PosAddToCartRes>()
    val posAddtocartLivedata : LiveData<PosAddToCartRes>
        get() = posAddtocartData

    val posSaleData = MutableLiveData<PosSalesDetails>()
    val posSaleLivedata : LiveData<PosSalesDetails>
        get() = posSaleData


    val addNewCustData = MutableLiveData<AddNewCustRes>()
    val addNewCustLivedata : LiveData<AddNewCustRes>
        get() = addNewCustData



    // Using SingleLiveEvent instead of MutableLiveData
    private val get_customerdata = SingleLiveEvent<getCustomerRes>()

    // Exposing the LiveData for observers
    val get_customer_liveData: LiveData<getCustomerRes>
        get() = get_customerdata

    // Function to update the value
    fun updateCustomerData(customerData: getCustomerRes) {
        get_customerdata.value = customerData
    }



    fun callSearchStoreProductApi( searchname: String,storeid:Int,customerid:Int,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        Log.d("requestsearchstoreproduct", searchname+storeid+customerid)
        ApiClient().getApiService(context).searchStoreProduct(SearchStoreProReq(searchname,storeid,customerid)).enqueue(object :
            Callback<SearchStoreProRes> {
            override fun onResponse(call: Call<SearchStoreProRes>, response: Response<SearchStoreProRes>) {

                if(response.isSuccessful && response.body()!=null){
                    storeProSearchdata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<SearchStoreProRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }


    fun callSearchStoreProductBarcodeApi( searchname: String,storeid:Int,customerid:Int,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        Log.d("request", searchname+storeid+customerid)
        val requestObj = SearchStoreProBarcodeReq(
            customer_id = customerid,
            search_string = searchname,
            store_id = storeid
        )

        // ✅ Print Request JSON
        val requestJson = Gson().toJson(requestObj)
        Log.d("🔍 SearchAPIRequest", requestJson)
        println("🔍 Final Request JSON: $requestJson")
        ApiClient().getApiService(context).searchStoreProductBarcode(SearchStoreProBarcodeReq(customerid,searchname,storeid)).enqueue(object :
            Callback<SearchStoreProBarcodeRes> {
            override fun onResponse(call: Call<SearchStoreProBarcodeRes>, response: Response<SearchStoreProBarcodeRes>) {

                Log.d("xxx", Gson().toJson(response.body()))
               // Log.d("yyy", Gson().toJson(SearchStoreProBarcodeReq(customerid,searchname,storeid)))
                Log.d("APIResponse", "Status Code: ${response.code()}")
                Log.d("APIResponse", "Body: ${Gson().toJson(response.body())}")
                if(response.isSuccessful && response.body()!=null){
                    storeProSearchBarcodedata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<SearchStoreProBarcodeRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }



    fun callAddtoCartPosApi( posAddToCartReq: PosAddToCartReq,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        //vhdvdbvdbvn
        //called requst and response
        val requestJson = GsonBuilder().setPrettyPrinting().create().toJson(posAddToCartReq)
        Log.d("📝 AddToCart Request JSON", requestJson)
        println("📝 Final Request JSON:\n$requestJson")
        if (posAddToCartReq == null) {
            Log.e("🛑 RequestError", "posAddToCartReq is NULL")
            return
        }
        ApiClient().getApiService(context).addToCartPos(posAddToCartReq).enqueue(object :
            Callback<PosAddToCartRes> {
            override fun onResponse(call: Call<PosAddToCartRes>, response: Response<PosAddToCartRes>) {
                Log.d("response:new api", Gson().toJson(response.body()))
                // Log.d("yyy", Gson().toJson(SearchStoreProBarcodeReq(customerid,searchname,storeid)))
                Log.d("APIResponse new ", "Status Code: ${response.code()}")
                Log.d("APIResponse new ", "Body: ${Gson().toJson(response.body())}")

                if(response.isSuccessful && response.body()!=null){
                    posAddtocartData.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<PosAddToCartRes>, t: Throwable) {
                Log.d("xxx",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }

   /* fun callposSaleApi( posSaleReq: PosSaleReq,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        //vhdvdbvdbvn
        //called requst and response
        val requestJson = GsonBuilder().setPrettyPrinting().create().toJson(posSaleReq)
        Log.d("📝 POSNEW Request JSON", requestJson)
        println("📝 Final Request JSON:\n$requestJson")
        ApiClient().getApiService(context).posSale(posSaleReq).enqueue(object :
            Callback<PosSalesDetails> {
            override fun onResponse(call: Call<PosSalesDetails>, response: Response<PosSalesDetails>) {
                Log.d("response:pos api", Gson().toJson(response.body()))
                // Log.d("yyy", Gson().toJson(SearchStoreProBarcodeReq(customerid,searchname,storeid)))
                Log.d("APIResponse pos ", "Status Code: ${response.code()}")
                Log.d("APIResponse pos ", "Body: ${Gson().toJson(response.body())}")

                if(response.isSuccessful && response.body()!=null){
                    posSaleData.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                }
            }

            override fun onFailure(call: Call<PosSalesDetails>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
            }
        })
    }
*/
   fun callposSaleApi(posSaleReq: PosSaleReq, context: Context) {
       loading.postValue(ProgressData(isProgress = true))
       val requestJson = GsonBuilder().setPrettyPrinting().create().toJson(posSaleReq)
       Log.d("📝 POSNEW Request JSON", requestJson)

       ApiClient().getApiService(context).posSale(posSaleReq).enqueue(object : Callback<PosSalesDetails> {
           override fun onResponse(call: Call<PosSalesDetails>, response: Response<PosSalesDetails>) {
               Log.d("APIResponse pos", "Status Code: ${response.code()}")
               Log.d("APIResponse pos", "Body: ${Gson().toJson(response.body())}")

               loading.postValue(ProgressData(isProgress = false))

               if (response.isSuccessful && response.body() != null) {
                   posSaleData.postValue(response.body())
               } else {
                   // Try to parse error body if possible
                   val errorBody = response.errorBody()?.string()
                   Log.e("APIResponse pos", "Error Body: $errorBody")

                   try {
                       val errorResponse = Gson().fromJson(errorBody, PosSalesDetails::class.java)
                       posSaleData.postValue(errorResponse)
                   } catch (e: Exception) {
                       Log.e("API Parsing Error", "Could not parse error response")
                       loading.postValue(ProgressData(isProgress = false, isMessage = true, message = "Something went wrong!"))
                   }
               }
           }

           override fun onFailure(call: Call<PosSalesDetails>, t: Throwable) {
               Log.e("API Failure", "Error: ${t.localizedMessage}")
                    //if customer already exsit messege need then pls change this sentences... already invoice number validation handle in android side

               loading.postValue(ProgressData(isProgress = false, isMessage = true, message = context.getString(R.string.pos_details_invoice_used)))

           }
       })
   }


    fun callAddNewCustApi( addNewCustReq: AddNewCustReq,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        Log.d("AddNewCust", "Request: ${Gson().toJson(addNewCustReq)}")

        ApiClient().getApiService(context).addNewCustAPI(addNewCustReq).enqueue(object :
            Callback<AddNewCustRes> {
            override fun onResponse(call: Call<AddNewCustRes>, response: Response<AddNewCustRes>) {
                Log.d("AddNewCust", "Response Code: ${response.code()} | Body: ${Gson().toJson(response.body())}")

                if(response.isSuccessful && response.body()!=null){
                    addNewCustData.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    //loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))

                    //Statically Added to avoid 409 conflict error in this API
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.pos_details_customer_exists) ))
                }
            }

            override fun onFailure(call: Call<AddNewCustRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
                //Log.d("x1",t.message.toString())
               // Log.d("x1",Gson().toJson(addNewCustReq))
            }
        })
    }

    fun callGetCustomerDetailsApi(getCustomerReq: getCustomerReq, context: Context) {
        loading.postValue(ProgressData(isProgress = true))
        ApiClient().getApiService(context).getCustomerInfoAPI(getCustomerReq)
            .enqueue(object : Callback<getCustomerRes> {
                override fun onResponse(
                    call: Call<getCustomerRes>,
                    response: Response<getCustomerRes>
                ) {

                    if (response.isSuccessful && response.body() != null) {
                        //get_customerdata.postValue(response.body())
                        //loading.postValue(ProgressData(isProgress = false))
                        //updateCustomerData(response.body())
                        val customerRes = response.body() // Store the result in a local variable

                        if (customerRes != null) {
                            // Update the SingleLiveEvent with the non-null result
                            updateCustomerData(customerRes)

                            // Update loading state
                            loading.postValue(ProgressData(isProgress = false))
                        }


                    } else {
                        loading.postValue(ProgressData(isProgress = false,isMessage = true, message =context.getString(R.string.sales_fetch_failed) ))
                    }
                }

                override fun onFailure(call: Call<getCustomerRes>, t: Throwable) {
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message = context.getString(R.string.error_something_went_wrong)))
                }
            })
    }

}
