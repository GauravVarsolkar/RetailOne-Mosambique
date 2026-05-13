package com.retailone.pos.ui.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.retailone.pos.BaseActivity          // ← ADDED
import com.retailone.pos.LocaleHelper          // ← ADDED
import com.retailone.pos.R                     // ← ADDED
import com.retailone.pos.databinding.ActivityMposloginBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.TimeoutHelper
import com.retailone.pos.models.LoginModels.LoginResponse
import com.retailone.pos.viewmodels.DashboardViewodel.ProfileAttendanceViewmodel
import com.retailone.pos.viewmodels.MPOSLoginViewmodel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MPOSLoginActivity : BaseActivity() {     // ← CHANGED
    lateinit var binding: ActivityMposloginBinding
    lateinit var loginviewmodel: MPOSLoginViewmodel
    lateinit var loginSession: LoginSession
    lateinit var profileAttendanceViewmodel: ProfileAttendanceViewmodel
    private var loginResponse: LoginResponse? = null
    private var yourCoroutineJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMposloginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginviewmodel = ViewModelProvider(this)[MPOSLoginViewmodel::class.java]
        profileAttendanceViewmodel = ViewModelProvider(this)[ProfileAttendanceViewmodel::class.java]
        loginSession = LoginSession.getInstance(this)

        // Setup language globe icon click listener
        binding.btnLanguage.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 0, 0, getString(R.string.lang_english))
            popup.menu.add(0, 1, 0, getString(R.string.lang_portuguese))
            popup.setOnMenuItemClickListener { item ->
                val lang = if (item.itemId == 0) "en" else "pt"
                LocaleHelper.setLocale(this, lang)
                recreate()
                true
            }
            popup.show()
        }

        loginviewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage)
                showMessage(it.message)
        }

        loginviewmodel.loginLiveData.observe(this) {
            if (it.status == 1) {
                loginResponse = it
                CoroutineScope(Dispatchers.IO).launch {
                    loginSession.storeLoginSession(it.data.token, false)
                    loginSession.storeCashupDateTime(it.cashup_date_time.toString())
                    Log.d("LoginSession", "Saving cashup time: ${it.cashup_date_time}")
                    val spotDiscount = it.data.spot_discount
                    val isEnabled = spotDiscount?.is_spot_discount_enabled == 1
                    val maxLimit = spotDiscount?.max_spot_discount_limit ?: "0.00"
                    loginSession.storeSpotDiscount(isEnabled, maxLimit)
                    Log.d("LoginSession", "Spot Discount Enabled: $isEnabled | Max Limit: $maxLimit")
                    profileAttendanceViewmodel.callUserProfileApi(this@MPOSLoginActivity)
                }
                showMessage(getString(R.string.fetching_store_details))  // ← CHANGED
            } else {
                showMessage(it.message)
            }
        }

        profileAttendanceViewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage)
                showMessage(it.message)
        }

        profileAttendanceViewmodel.userProfileLiveData.observe(this) {
            CoroutineScope(Dispatchers.IO).launch {
                val storeid = it.data.user_details.store_id
                val store_manager_id = it.data.user_details.id

                if (!storeid.isNullOrBlank() && loginResponse != null) {
                    val timeouthelper = TimeoutHelper(this@MPOSLoginActivity)
                    timeouthelper.saveSessionTimestamp()
                    loginSession.saveStoreID(storeid)
                    loginSession.saveStoreManagerID(store_manager_id.toString())
                    loginSession.storeLoginSession(loginResponse!!.data.token, true)
                    showMessage(getString(R.string.login_successful))   // ← CHANGED
                    navigateToHomepage()
                } else {
                    showMessage(getString(R.string.user_no_store))      // ← CHANGED
                }
            }
        }

        binding.loginBtn.setOnClickListener {
            val userid = binding.mobileedit.text.toString()
            val pin = binding.pinedit.text.toString()
            validateCredential(userid, pin)
        }

        binding.forgotpin.setOnClickListener {
            val intent = Intent(this@MPOSLoginActivity, ForgotPinActivity::class.java)
            startActivity(intent)
        }

        binding.forgotpin.paintFlags = Paint.UNDERLINE_TEXT_FLAG
    }

    // removed showLanguageDialog as it was replaced by popup menu

    private fun navigateToHomepage() {
        val intent = Intent(this@MPOSLoginActivity, MPOSDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun validateCredential(userid: String, pin: String) {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        val device_id = getMyDeviceId(this)

        if (userid.isEmpty() || userid.isBlank()) {
            showMessage(getString(R.string.error_enter_email))          // ← CHANGED
        } else if (!userid.matches(emailPattern.toRegex())) {
            showMessage(getString(R.string.error_valid_email))          // ← CHANGED
        } else if (pin.isEmpty() || pin.isBlank() || pin.trim().length != 6) {
            showMessage(getString(R.string.error_enter_pin))            // ← CHANGED
        } else {
            login(userid, pin, device_id)
        }
    }

    fun getMyDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    private fun login(userid: String, pin: String, device_id: String) {
        loginviewmodel.callLoginApi(this@MPOSLoginActivity, userid, pin, device_id)
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this@MPOSLoginActivity, activityClass)
        startActivity(intent)
        finish()
    }

    private fun showMessage(msg: String) {
        yourCoroutineJob = GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MPOSLoginActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun <T> navigateToActivity(target: Class<T>, key: String? = null, value: String? = null) {
        val intent = Intent(this, target)
        if (key != null && value != null) {
            intent.putExtra(key, value)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        yourCoroutineJob?.cancel()
        super.onDestroy()
    }
}
