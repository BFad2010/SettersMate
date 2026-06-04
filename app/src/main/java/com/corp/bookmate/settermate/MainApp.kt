package com.corp.bookmate.settermate

import android.app.Application
import com.corp.bookmate.settermate.di.androidModule
import com.corp.bookmate.settermate.di.sharedModule
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        startKoin {
            androidContext(this@MainApp)
            androidLogger()
            modules(sharedModule, androidModule)
        }
    }
}
