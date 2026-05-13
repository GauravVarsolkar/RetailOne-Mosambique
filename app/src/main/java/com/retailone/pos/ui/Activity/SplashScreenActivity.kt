package com.retailone.pos.ui.Activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.retailone.pos.BaseActivity  // ← CHANGED
import com.retailone.pos.R
import com.retailone.pos.databinding.ActivitySplashScreenBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashScreenActivity : BaseActivity() {  // ← CHANGED
    lateinit var binding: ActivitySplashScreenBinding
    lateinit var loginSession: LoginSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginSession = LoginSession.getInstance(this)

        lifecycleScope.launch {
            delay(1000)
            val isLoggedIn = loginSession.getLoginStatus().first()
            if (isLoggedIn) {
                navigateToActivity(MPOSDashboardActivity::class.java)
            } else {
                navigateToActivity(MPOSLoginActivity::class.java)
            }
        }
    }

    private fun <T> navigateToActivity(target: Class<T>, key: String? = null, value: String? = null) {
        val intent = Intent(this, target)
        if (key != null && value != null) {
            intent.putExtra(key, value)
        }
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
    }
}
