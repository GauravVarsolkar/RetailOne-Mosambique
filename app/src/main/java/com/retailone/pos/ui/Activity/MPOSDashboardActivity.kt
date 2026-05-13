package com.retailone.pos.ui.Activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.retailone.pos.BaseActivity          // ← ADDED
import com.retailone.pos.LocaleHelper          // ← ADDED
import com.retailone.pos.R
import com.retailone.pos.adapter.PastRequDetailsAdapter
import com.retailone.pos.databinding.ActivityMposdashboardBinding
import com.retailone.pos.databinding.CustomerDetailsBottomsheetBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.InventoryStockHelper
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.localstorage.SharedPreference.TimeoutHelper
import com.retailone.pos.models.GetCustomerModel.getCustomerReq
import com.retailone.pos.ui.Activity.DashboardActivity.*
import com.retailone.pos.utils.CrashHandler
import com.retailone.pos.viewmodels.DashboardViewodel.HomeDashboardViewmodel
import com.retailone.pos.viewmodels.MPOSLoginViewmodel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MPOSDashboardActivity : BaseActivity() {  // ← CHANGED

    lateinit var binding: ActivityMposdashboardBinding
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    lateinit var drawer: DrawerLayout
    lateinit var loginSession: LoginSession
    lateinit var sharedPrefHelper: SharedPrefHelper
    lateinit var inventoryStockHelper: InventoryStockHelper
    lateinit var viewmodel: HomeDashboardViewmodel
    lateinit var loginViewmodel: MPOSLoginViewmodel
    lateinit var localizationHelper: LocalizationHelper
    lateinit var organisationDetailsHelper: OrganisationDetailsHelper

    var storemanager_id = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMposdashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        drawer = binding.drawer
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawer, R.string.nav_open, R.string.nav_close)
        drawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_menu)

        val headerView = binding.navView.getHeaderView(0)
        val headerImageView = headerView.findViewById<ImageView>(R.id.header_imageView)

        loginSession = LoginSession.getInstance(this)
        sharedPrefHelper = SharedPrefHelper(this)
        inventoryStockHelper = InventoryStockHelper(this)
        localizationHelper = LocalizationHelper(this)
        organisationDetailsHelper = OrganisationDetailsHelper(this)

        viewmodel = ViewModelProvider(this)[HomeDashboardViewmodel::class.java]
        loginViewmodel = ViewModelProvider(this)[MPOSLoginViewmodel::class.java]

        val crashHandler = CrashHandler(this)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)

        lifecycleScope.launch {
            val isLoggedIn = loginSession.getLoginStatus().first()
            val token = loginSession.getToken().first()
            val storeid = loginSession.getStoreID().first().toInt()
            storemanager_id = loginSession.getStoreManagerID().first().toString()

            val timeouthelper = TimeoutHelper(this@MPOSDashboardActivity)
            if (!timeouthelper.isSessionValid()) {
                mposLogout()
            }

            viewmodel.callLocalizationApi(storeid, this@MPOSDashboardActivity)
            viewmodel.callOrganizationDetailsApi(storeid, this@MPOSDashboardActivity)
        }

        viewmodel.localization_liveData.observe(this) {
            localizationHelper.saveLocalizationData(it.data)
        }

        viewmodel.organization_liveData.observe(this) {
            organisationDetailsHelper.saveOrganisationData(it.data)
        }

        loginViewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.about_us -> {
                    Toast.makeText(this, getString(R.string.about_us), Toast.LENGTH_SHORT).show()
                }
                R.id.contact_us -> {
                    Toast.makeText(this, getString(R.string.contact_us), Toast.LENGTH_SHORT).show()
                }
                R.id.change_language -> {
                    showLanguageDialog()
                }
                R.id.log_data -> {
                    startActivity(Intent(this, CrashLogsActivity::class.java))
                }
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }

        binding.poscard.setOnClickListener {
            lifecycleScope.launch {
                val cashupTime = loginSession.getCashupDateTime().first()
                if (isCashupOutdated(cashupTime)) {
                    showCashupPopup(cashupTime)
                } else {
                    customerBottomSheet()
                }
            }
        }

        binding.returncard.setOnClickListener {
            startActivity(Intent(this, ReturnSaleActivity::class.java))
        }

        binding.goodsRWcard.setOnClickListener {
            startActivity(Intent(this, proceedToDispatchActivity::class.java))
        }

        binding.stockcard.setOnClickListener {
            sharedPrefHelper.clearStockList()
            startActivity(Intent(this, StockRequisitionActivity::class.java))
        }

        binding.materialrcvCard.setOnClickListener {
            startActivity(Intent(this, MaterialRecivingItemsActivity::class.java))
        }

        binding.pdtInventoryCard.setOnClickListener {
            startActivity(Intent(this, ProductInventoryActivity::class.java))
        }

        binding.cashupcard.setOnClickListener {
            lifecycleScope.launch {
                val cashupTime = loginSession.getCashupDateTime().first()
                val intent = Intent(this@MPOSDashboardActivity, CashUpActivity::class.java)
                if (isCashupOutdated(cashupTime)) {
                    intent.putExtra("CASHUP_DATE_TIME", cashupTime)
                }
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }

        binding.salesPaymentCard.setOnClickListener {
            startActivity(Intent(this, SalesAndPaymentActivity::class.java))
        }

        binding.expensecard.setOnClickListener {
            lifecycleScope.launch {
                val cashupTime = loginSession.getCashupDateTime().first()
                val intent = Intent(this@MPOSDashboardActivity, ExpenseRegisterActivity::class.java)
                intent.putExtra("CASHUP_DATE_TIME", cashupTime)
                startActivity(intent)
            }
        }

        binding.profileCard.setOnClickListener {
            startActivity(Intent(this, ProfileAttendanceActivity::class.java))
        }

        binding.logout.setOnClickListener {
            showLogoutDialog()
        }

        val organisation_data = organisationDetailsHelper.getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(binding.toolImage)

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.logo)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(headerImageView)
    }

    // ← ADDED: Language selector dialog
    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.english), getString(R.string.portuguese))
        val codes = arrayOf("en", "pt")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setItems(languages) { _, which ->
                LocaleHelper.setLocale(this, codes[which])
                recreate()
            }
            .setCancelable(true)
            .show()
    }

    private fun isCashupOutdated(cashupDateTime: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
            val cleanDate = cashupDateTime.trim().replace("\"", "")
            val cashupDate = formatter.parse(cleanDate) ?: return false
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            cashupDate.before(calendar.time)
        } catch (e: Exception) {
            false
        }
    }

    private fun showCashupPopup(cashupDateTime: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cashup, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            val intent = Intent(this, CashUpDetailsActivity::class.java)
            intent.putExtra("CASHUP_DATE_TIME", cashupDateTime)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            dialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun customerBottomSheet() {
        val d_binding = CustomerDetailsBottomsheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)

        viewmodel.loadingLiveData.removeObservers(this)
        viewmodel.loadingLiveData.observe(this) {
            d_binding.progress.isVisible = it.isProgress
        }

        viewmodel.get_customer_liveData.removeObservers(this)
        viewmodel.get_customer_liveData.observe(this) {
            if (it.status == 1) {
                val intent = Intent(this, PointOfSaleActivity::class.java)
                intent.putExtra("c_id", it.data.id)
                intent.putExtra("c_mobile", it.data.mobile_no ?: "")
                intent.putExtra("c_name", it.data.customer_name ?: "")
                intent.putExtra("c_tpin", it.data.tin_tpin_no ?: "")
                intent.putExtra("c_address", it.data.address ?: "")
                intent.putExtra("c_iva", it.data.vat_reg_no ?: "")
                startActivity(intent)
                if (dialog.isShowing) dialog.dismiss()
            } else {
                showMessage(it.message)
            }
        }

        d_binding.saveBtn.setOnClickListener {
            val cust_mobile_tpin = d_binding.mobileInput.text.toString()
            if (cust_mobile_tpin.isEmpty() || cust_mobile_tpin.length < 9) {
                showMessage(getString(R.string.error_customer_mobile))  // ← CHANGED
            } else if (d_binding.toggle.checkedRadioButtonId == R.id.mobile_btn) {
                viewmodel.callGetCustomerDetailsApi(getCustomerReq(mobile_no = cust_mobile_tpin, tin_tpin_no = ""), this)
            } else {
                viewmodel.callGetCustomerDetailsApi(getCustomerReq(mobile_no = "", tin_tpin_no = cust_mobile_tpin), this)
            }
        }

        d_binding.skipBtn.setOnClickListener {
            if (dialog.isShowing) dialog.dismiss()
            val intent = Intent(this, PointOfSaleActivity::class.java)
            intent.putExtra("c_id", 0)
            intent.putExtra("c_mobile", "")
            intent.putExtra("c_name", "")
            intent.putExtra("c_tpin", "")
            intent.putExtra("c_iva", "")
            intent.putExtra("c_address", "")
            startActivity(intent)
        }

        dialog.show()
    }

    private fun mposLogout() {
        val device_id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        if (device_id.isEmpty()) {
            showMessage(getString(R.string.error_device_id))        // ← CHANGED
        } else if (storemanager_id.isEmpty()) {
            showMessage(getString(R.string.error_store_manager))    // ← CHANGED
        } else {
            loginViewmodel.callLogoutApi(this, storemanager_id, device_id)
        }

        loginViewmodel.logoutLiveData.observe(this) {
            CoroutineScope(Dispatchers.IO).launch {
                loginSession.clearLoginSession()
                val intent = Intent(this@MPOSDashboardActivity, MPOSLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.logout_dialog_layout)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)

        val cancel = dialog.findViewById<MaterialButton>(R.id.prefer_cancel)
        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val logoutImg = dialog.findViewById<ImageView>(R.id.dialog_logo)

        logoutMsg.text = getString(R.string.logout_confirm_msg)     // ← CHANGED
        logoutMsg.textSize = 16F
        logoutImg.setImageResource(R.drawable.svg_off)
        logoutImg.scaleType = ImageView.ScaleType.FIT_CENTER

        confirm.setOnClickListener {
            dialog.dismiss()
            mposLogout()
        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) true
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
