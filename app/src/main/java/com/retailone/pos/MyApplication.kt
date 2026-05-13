package com.retailone.pos

import android.app.Application
import android.content.Context

class MyApplication : Application() {
    override fun attachBaseContext(base: Context) {
        val lang = LocaleHelper.getSavedLanguage(base)
        val context = LocaleHelper.setLocale(base, lang)
        super.attachBaseContext(context)
    }
}
