package com.retailone.pos.ui.Activity.DashboardActivity

import com.retailone.pos.utils.NumberFormatter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import com.retailone.pos.BaseActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.retailone.pos.R
import com.retailone.pos.adapter.PointofsaleItemAdapter
import com.retailone.pos.adapter.PosSearchAdapter
import com.retailone.pos.databinding.ActivityPointOfSaleBinding
import com.retailone.pos.databinding.BarcodeProductsaleBottomsheetBinding
import com.retailone.pos.databinding.BarcodeProductsaleRcvBottomsheetBinding
import com.retailone.pos.databinding.PosSearchDialogLayoutBinding
import com.retailone.pos.interfaces.OnDeleteItemClickListener
import com.retailone.pos.interfaces.OnQuantityChangeListener
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.CartProductItem
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartReq
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.BatchCartItem
import com.retailone.pos.network.Constants
import com.retailone.pos.utils.BatchUtils
import com.retailone.pos.utils.FunUtils
import com.retailone.pos.viewmodels.DashboardViewodel.PointofSaleViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PointOfSaleActivity : BaseActivity(),
    OnDeleteItemClickListener,
    OnQuantityChangeListener {

    lateinit var binding: ActivityPointOfSaleBinding
    lateinit var pos_viewmodel: PointofSaleViewmodel
    lateinit var positem_adapter: PointofsaleItemAdapter
    lateinit var incudebinding: PosSearchDialogLayoutBinding
    lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var posItemList = mutableListOf<StoreProData>()
    var storeid = 0
    var c_id = 0
    var c_name = ""
    var c_mobile = ""
    var c_tpin = ""
    var c_address = ""
    var c_iva = ""
    lateinit var localizationData: LocalizationData
    private val PERMISSION_REQUEST_CAMERA = 1
    private lateinit var scanQrResultLauncher: ActivityResultLauncher<Intent>
    var canpressback = true
    private var maxSpotDiscountLimit = 0.0
    private var appliedSpotDiscountPercent = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPointOfSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        c_id = intent.getIntExtra("c_id", 0)
        c_name = intent.getStringExtra("c_name").toString()
        c_mobile = intent.getStringExtra("c_mobile").toString()
        c_tpin = intent.getStringExtra("c_tpin").toString()
        c_address = intent.getStringExtra("c_address").toString()
        c_iva = intent.getStringExtra("c_iva").toString()
        incudebinding = binding.include
        sheetBehavior = BottomSheetBehavior.from(incudebinding.bottomSheetLayout)
        pos_viewmodel = ViewModelProvider(this)[PointofSaleViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()

        if (posItemList.isEmpty()) {
            binding.relativeLayout.isVisible = false
        }

        val loginSession = LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first().toInt()
            pos_viewmodel.callSearchStoreProductApi("", storeid, c_id, this@PointOfSaleActivity)
            
            val isSpotDiscountEnabled = loginSession.isSpotDiscountEnabled().first()
            val limitStr = loginSession.getSpotDiscountLimit().first()
            maxSpotDiscountLimit = limitStr.toDoubleOrNull() ?: 0.0

            if (isSpotDiscountEnabled) {
                binding.spotDiscountCard.isVisible = true
            }
        }
    setupSpotDiscountUI()
        pos_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        scanQrResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultData ->
            if (resultData.resultCode == RESULT_OK && resultData.data != null) {
                val result = ScanIntentResult.parseActivityResult(resultData.resultCode, resultData.data)
                if (result.contents == null) {
                    Toast.makeText(this, getString(R.string.pos_scan_cancelled), Toast.LENGTH_LONG).show()
                } else {
                    pos_viewmodel.callSearchStoreProductBarcodeApi(
                        result.contents.toString(),
                        storeid,
                        c_id,
                        this@PointOfSaleActivity
                    )
                }
            }
        }

        enableBackButton()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackWithConfirm()   // <- uses your existing collapse + confirm logic
                }
            }
        )

        preparePositemRCV()
        prepareSearchBottomSheet()
        setToolbarImage()

        binding.searchtext.setOnClickListener {
            dismissKeyboard(it)
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.addproductLayout.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.nextlayout.setOnClickListener {
            Log.d("sdf", posItemList.toString())
            if (posItemList.isNotEmpty()) {
                if (!posItemList.any { BatchUtils.getTotalPosCartQuantity(it.batch).toDouble() == 0.0 }) {
                    // proceed (unchanged)
                } else {
                    showMessage(getString(R.string.pos_qty_zero))
                }
            } else {
                showMessage(getString(R.string.pos_add_item))
            }
        }

        binding.barcodeimage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@PointOfSaleActivity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        PERMISSION_REQUEST_CAMERA
                    )
                } else {
                    startScanning()
                }
            } else startScanning()
        }

        incudebinding.barcodeimage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@PointOfSaleActivity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        PERMISSION_REQUEST_CAMERA
                    )
                } else {
                    startScanning()
                }
            } else startScanning()
        }

        pos_viewmodel.storeProSearchBarcodeLivedata.observe(this) {
            if (it.data.isNotEmpty()) {
                barcodeProductSaleBottomSheetRCV(it.data)
                if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            } else {
                showMessage(getString(R.string.pos_product_unavailable))
            }
        }
    }

    private fun barcodeProductSaleBottomSheetRCV(item: List<StoreProData>) {
        val rcv_binding = BarcodeProductsaleRcvBottomsheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(rcv_binding.root)
        dialog.setCancelable(true)

        var posSearchAdapter: PosSearchAdapter
        rcv_binding.searchstockRcv.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        if (item.isNotEmpty()) {
            posSearchAdapter = PosSearchAdapter(item, this@PointOfSaleActivity) { clickitem ->
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                dialog.dismiss()

                updateQuantity(clickitem)

                positem_adapter = PointofsaleItemAdapter(this, posItemList, this, this)
                binding.positemRcv.adapter = positem_adapter

                if (posItemList.size > 0) {
                    binding.positemRcv.isVisible = true
                    binding.addproductLayout.isVisible = false
                } else {
                    binding.positemRcv.isVisible = false
                    binding.addproductLayout.isVisible = true
                }
            }
            rcv_binding.searchstockRcv.adapter = posSearchAdapter
            rcv_binding.searchstockRcv.isVisible = true
        } else {
            rcv_binding.searchstockRcv.isVisible = false
        }

        pos_viewmodel.loadingLiveData.observe(this) {
            incudebinding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        pos_viewmodel.posAddtocartLivedata.observe(this) { addtocartres ->
            binding.nextlayout.setOnClickListener {
                Log.d("sdf", posItemList.toString())

                if (posItemList.isNotEmpty()) {
                    if (!posItemList.any { BatchUtils.getTotalPosCartQuantity(it.batch).toDouble() == 0.0 }) {
                        if (!posItemList.any { it.dispense_status.toInt() == 1 }) {
                            val total_amount = FunUtils.stringToDouble(addtocartres.grand_total)
                            Log.d("MyApp", "Total Amount : $total_amount")
                            if (total_amount > 0) {
                                val intent = Intent(this@PointOfSaleActivity, PointofSaleDetailsActivity::class.java)
                                intent.putExtra("saleitem", addtocartres)
                                intent.putExtra("c_name", c_name)
                                intent.putExtra("c_mobile", c_mobile)
                                intent.putExtra("c_tpin", c_tpin)
                                intent.putExtra("c_id", c_id)
                                intent.putExtra("total_amount", total_amount)
                                intent.putExtra("spot_discount_percent", appliedSpotDiscountPercent)
                                intent.putExtra("c_address", c_address)
                                intent.putExtra("c_iva", c_iva)
                                startActivity(intent)
                            } else {
                                showMessage(getString(R.string.pos_try_again_later))
                            }
                        } else {
                            showMessage(getString(R.string.pos_sell_dispensed))
                        }
                    } else {
                        showMessage(getString(R.string.pos_qty_zero))
                    }
                } else {
                    showMessage(getString(R.string.pos_add_item))
                }
            }

            val gson = Gson()
            val json = gson.toJson(addtocartres)
            Log.d("res", json)

            binding.apply {
                paymentcard.isVisible = true
                subtotal.text = NumberFormatter().formatPrice(addtocartres.sub_total.toString(), localizationData)
                spotDiscountPercentField.text = getString(R.string.pos_spot_discount_format, addtocartres.spot_discount_percentage)
                spotDiscountAmountValue.text = NumberFormatter().formatPrice(addtocartres.spot_discount_amount.toString(), localizationData)
                spotDiscountRow.isVisible = addtocartres.spot_discount_amount.toDoubleOrNull()?.let { it > 0.0 } ?: false
                taxfield.text = getString(R.string.pos_tax_value, addtocartres.tax.toString())
                taxAmount.text = NumberFormatter().formatPrice(addtocartres.tax_amount.toString(), localizationData)
                discountvalue.text = NumberFormatter().formatPrice(addtocartres.discount_amount.toString(), localizationData)
                alltotalAmount.text = NumberFormatter().formatPrice(addtocartres.grand_total.toString(), localizationData)
                itemno.text = getString(R.string.pos_items_count, addtocartres.data.size)
                rlPrice.text = NumberFormatter().formatPrice(addtocartres.grand_total.toString(), localizationData)

                if (addtocartres.data.size > 0) relativeLayout.isVisible = true else relativeLayout.isVisible = false
                if (addtocartres.data.size > 0) paymentcard.isVisible = true else paymentcard.isVisible = false
            }
        }

        dialog.show()
    }

    private fun barcodeProductSaleBottomSheet(productitem: StoreProData) {
        val d_binding = BarcodeProductsaleBottomsheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)
        dialog.setCancelable(true)

        d_binding.saveBtn.setOnClickListener { dialog.dismiss() }

        d_binding.itemName.text = productitem.product_name
        d_binding.itemType.text = productitem.pack_product_description

        val formattedPrice = NumberFormatter().formatPrice(productitem.retail_price ?: "-", localizationData)
        d_binding.itemPrice.text = formattedPrice

        if (productitem.stock_quantity > 0) {
            d_binding.addlayout.isVisible = true
            d_binding.itemUnit.text = getString(R.string.pos_units, productitem.stock_quantity.toString())
            d_binding.itemUnit.setTextColor(Color.parseColor("#008000"))
            d_binding.addcart.setOnClickListener {
                onBarcodeItemClick(productitem)
                dialog.dismiss()
            }

        } else {
            d_binding.quantityContainer.isVisible = false
            d_binding.itemUnit.text = getString(R.string.pos_out_of_stock)
            d_binding.itemUnit.setTextColor(Color.parseColor("#FF0000"))
        }

        Glide.with(this)
            .load(Constants.IMAGE_URL + productitem.product_photo)
            .centerCrop()
            .placeholder(R.drawable.temp)
            .error(R.drawable.temp)
            .into(d_binding.productimg)

        dialog.show()
    }

    private fun onBarcodeItemClick(clickitem: StoreProData) {
        updateQuantity(clickitem)
        positem_adapter = PointofsaleItemAdapter(this, posItemList, this, this)
        binding.positemRcv.adapter = positem_adapter

        if (posItemList.size > 0) {
            binding.positemRcv.isVisible = true
            binding.addproductLayout.isVisible = false
        } else {
            binding.positemRcv.isVisible = false
            binding.addproductLayout.isVisible = true
        }
    }

    private fun setToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(binding.image)
    }

    private fun prepareSearchBottomSheet() {
        var posSearchAdapter: PosSearchAdapter

        incudebinding.searchstockRcv.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        pos_viewmodel.storeProSearchLivedata.observe(this) {
            if (it.data.isNotEmpty()) {
                posSearchAdapter = PosSearchAdapter(it.data, this@PointOfSaleActivity) { clickitem ->
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                    updateQuantity(clickitem)

                    positem_adapter = PointofsaleItemAdapter(this, posItemList, this, this)
                    binding.positemRcv.adapter = positem_adapter

                    if (posItemList.size > 0) {
                        binding.positemRcv.isVisible = true
                        binding.addproductLayout.isVisible = false
                    } else {
                        binding.positemRcv.isVisible = false
                        binding.addproductLayout.isVisible = true
                    }
                }
                incudebinding.searchstockRcv.adapter = posSearchAdapter
                incudebinding.searchstockRcv.isVisible = true
                incudebinding.noDataFound.isVisible = false
            } else {
                incudebinding.searchstockRcv.isVisible = false
                incudebinding.noDataFound.isVisible = true
            }
        }

        pos_viewmodel.loadingLiveData.observe(this) {
            incudebinding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        pos_viewmodel.posAddtocartLivedata.observe(this) { addtocartres ->
            binding.nextlayout.setOnClickListener {
                Log.d("sdf", posItemList.toString())

                if (posItemList.isNotEmpty()) {
                    if (!posItemList.any { BatchUtils.getTotalPosCartQuantity(it.batch).toDouble() == 0.0 }) {

                        if (!posItemList.any { it.dispense_status.toInt() == 1 }) {
                            val total_amount = FunUtils.stringToDouble(addtocartres.grand_total)

                            if (total_amount > 0) {
                                val intent =
                                    Intent(this@PointOfSaleActivity, PointofSaleDetailsActivity::class.java)
                                intent.putExtra("saleitem", addtocartres)
                                intent.putExtra("c_name", c_name)
                                intent.putExtra("c_mobile", c_mobile)
                                intent.putExtra("c_tpin", c_tpin)
                                intent.putExtra("c_id", c_id)
                                intent.putExtra("total_amount", total_amount)
                                intent.putExtra("spot_discount_percent", appliedSpotDiscountPercent)
                                intent.putExtra("c_address", c_address)
                                intent.putExtra("c_iva", c_iva)
                                startActivity(intent)
                            } else {
                                showMessage(getString(R.string.pos_try_again_later))
                            }
                        } else {
                            showMessage(getString(R.string.pos_sell_dispensed))
                        }
                    } else {
                        showMessage(getString(R.string.pos_qty_zero))
                    }
                } else {
                    showMessage(getString(R.string.pos_add_item))
                }
            }

            val gson = Gson()
            val json = gson.toJson(addtocartres)
            Log.d("res", json)

            binding.apply {
                paymentcard.isVisible = true
                subtotal.text = NumberFormatter().formatPrice(addtocartres.sub_total.toString(), localizationData)
                spotDiscountPercentField.text = getString(R.string.pos_spot_discount_format, addtocartres.spot_discount_percentage)
                spotDiscountAmountValue.text = NumberFormatter().formatPrice(addtocartres.spot_discount_amount.toString(), localizationData)
                spotDiscountRow.isVisible = addtocartres.spot_discount_amount.toDoubleOrNull()?.let { it > 0.0 } ?: false
                taxfield.text = getString(R.string.pos_tax_value, addtocartres.tax.toString())
                taxAmount.text = NumberFormatter().formatPrice(addtocartres.tax_amount.toString(), localizationData)
                discountvalue.text = NumberFormatter().formatPrice(addtocartres.discount_amount.toString(), localizationData)
                alltotalAmount.text = NumberFormatter().formatPrice(addtocartres.grand_total.toString(), localizationData)
                itemno.text = getString(R.string.pos_items_count, addtocartres.data.size)
                rlPrice.text = NumberFormatter().formatPrice(addtocartres.grand_total.toString(), localizationData)

                if (addtocartres.data.size > 0) relativeLayout.isVisible = true else relativeLayout.isVisible = false
                if (addtocartres.data.size > 0) paymentcard.isVisible = true else paymentcard.isVisible = false
            }
        }

        incudebinding.searchBar.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                pos_viewmodel.callSearchStoreProductApi(
                    newText ?: "",
                    storeid,
                    customerid = c_id,
                    this@PointOfSaleActivity
                )
                return false
            }
        })
    }

    private fun preparePositemRCV() {
        binding.positemRcv.apply {
            layoutManager = LinearLayoutManager(this@PointOfSaleActivity, RecyclerView.VERTICAL, false)
        }
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.pos_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
        }
        // Toolbar back click -> same behavior as system back
        binding.toolbar.setNavigationOnClickListener { handleBackWithConfirm() }
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBackWithConfirm()
        return true
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@PointOfSaleActivity, msg, Toast.LENGTH_SHORT).show()
    }

    // ==== Dispense activity callback (unchanged, with safe notify) ====
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            canpressback = false

            val dis_quantity = data?.getDoubleExtra("dis_quantity", 0.0)
            val pro_id = data?.getIntExtra("pro_id", 0)
            val dis_id = data?.getIntExtra("dis_id", 0)

            val existingItemIndex = posItemList.indexOfFirst {
                it.product_id == pro_id && it.distribution_pack_id == dis_id
            }
            val batchX = posItemList[existingItemIndex].batch.toMutableList()
            val updatedBatchX = batchX[0].copy(batch_cart_quantity = dis_quantity ?: 0.0)
            batchX[0] = updatedBatchX

            val updatedItemx = posItemList[existingItemIndex].copy(batch = batchX, dispense_status = 2)
            posItemList[existingItemIndex] = updatedItemx

            if (binding.positemRcv.isComputingLayout) {
                binding.positemRcv.post { positem_adapter.notifyItemChanged(existingItemIndex) }
            } else {
                positem_adapter.notifyItemChanged(existingItemIndex)
            }

        } else if (requestCode == 200 && resultCode != Activity.RESULT_OK) {
            showMessage(getString(R.string.pos_details_store_error)) // Reusing store error or similar toast message key
        }
    }

    override fun onDeleteItemClicked(item: StoreProData) {
        val position = posItemList.indexOf(item)
        if (position != -1) {
            posItemList.removeAt(position)
            if (binding.positemRcv.isComputingLayout) {
                binding.positemRcv.post {
                    positem_adapter.notifyItemRemoved(position)
                    positem_adapter.notifyDataSetChanged()
                    calladdToCartAPI()
                }
            } else {
                positem_adapter.notifyItemRemoved(position)
                positem_adapter.notifyDataSetChanged()
                calladdToCartAPI()
            }
        }
    }
    private fun setupSpotDiscountUI() {

        binding.checkboxYes.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxNo.isChecked = false
                binding.spotDiscountInputLayout.isVisible = true
                binding.spotDiscountInput.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.spotDiscountInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        binding.spotDiscountInput.filters = arrayOf(object : InputFilter {
            override fun filter(
                source: CharSequence?, start: Int, end: Int,
                dest: Spanned?, dstart: Int, dend: Int
            ): CharSequence? {
                val result = dest.toString().substring(0, dstart) +
                        source +
                        dest.toString().substring(dend)
                val pattern = Regex("^\\d{0,3}(\\.\\d{0,2})?$")
                return if (pattern.matches(result)) null else ""
            }
        })

        binding.checkboxNo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxYes.isChecked = false
                binding.spotDiscountInputLayout.isVisible = false
                binding.spotDiscountInput.setText("")
                appliedSpotDiscountPercent = 0.0
                binding.spotDiscountCard.isVisible = false
            }
        }

        binding.spotDiscountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toDoubleOrNull() ?: return
                if (input > maxSpotDiscountLimit) {
                    binding.spotDiscountInput.setText("")
                    appliedSpotDiscountPercent = 0.0
                    showMessage(getString(R.string.error_spot_discount_limit, maxSpotDiscountLimit.toString()))  // ← updated
                } else {
                    appliedSpotDiscountPercent = input
                    Log.d("SpotDiscount", "Applied discount: $appliedSpotDiscountPercent%")
                    calladdToCartAPI()
                }
            }
        })
    }

    override fun onQuantityChange(position: Int, newBatchList: List<PosSaleBatch>) {
        posItemList[position].batch = newBatchList
        calladdToCartAPI()
        if (binding.positemRcv.isComputingLayout) {
            binding.positemRcv.post { positem_adapter.notifyItemChanged(position) }
        } else {
            positem_adapter.notifyItemChanged(position)
        }
    }

    private fun calladdToCartAPI() {
        val cartitemlist = mutableListOf<CartProductItem>()

        posItemList.forEach {
            val batchCartItemList = it.batch.map { posSaleBatch ->
                BatchCartItem(
                    batchno = posSaleBatch.batch_no,
                    quantity = posSaleBatch.batch_cart_quantity.toString(),
                    retail_price = posSaleBatch.price.toString()
                )
            }
            cartitemlist.add(
                CartProductItem(
                    distribution_pack_id = it.distribution_pack_id,
                    product_id = it.product_id,
                    batch = batchCartItemList
                )
            )
        }

        val cart_data = PosAddToCartReq(
            store_id = storeid,
            spot_discount_percentage = appliedSpotDiscountPercent,
            products = cartitemlist
        )
        pos_viewmodel.callAddtoCartPosApi(cart_data, this@PointOfSaleActivity)

        val gson = Gson()
        val json = gson.toJson(cart_data)
        Log.d("reqx", json)
    }

    fun updateQuantity(clickitem: StoreProData) {
        val existingItemIndex = posItemList.indexOfFirst {
            it.product_id == clickitem.product_id &&
                    it.distribution_pack_id == clickitem.distribution_pack_id
        }

        if (existingItemIndex != -1) {
            if (posItemList[existingItemIndex].dispense_status == 2) {
                showMessage(getString(R.string.pos_sell_dispensed))
                return
            }
            showMessage(getString(R.string.pos_already_in_cart))
        } else {
            if (FunUtils.isLooseOil(clickitem.category_id, clickitem.pack_product_description)) {
                clickitem.dispense_status = 1
                clickitem.cart_quantity = 1.0
                posItemList.add(clickitem)
            } else {
                clickitem.dispense_status = 0
                clickitem.cart_quantity = 1.0
                posItemList.add(clickitem)
            }
        }
    }

    private fun startScanning() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt(getString(R.string.pos_scan_prompt))
        options.setOrientationLocked(true)
        scanQrResultLauncher.launch(ScanContract().createIntent(this, options))
    }

    // ✅ Correct Kotlin signature
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, getString(R.string.forgot_enter_valid_mobile), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // ===== Back handling (system back & toolbar back share the same logic) =====
    private fun handleBackWithConfirm() {
        // 1) collapse bottom sheet first
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }
        // 2) respect existing dispense guard
        if (!canpressback) {
            showMessage(getString(R.string.pos_sell_dispensed))
            return
        }
        // 3) confirm
        showBackConfirmDialog()
    }

    private fun showBackConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pos_leave_screen))
            .setMessage(getString(R.string.pos_confirm_back))
            .setCancelable(true)
            .setNegativeButton(getString(R.string.pos_no)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.pos_yes)) { _, _ -> finish() }
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Use the unified handler
        handleBackWithConfirm()
    }

}
