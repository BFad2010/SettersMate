package com.corp.bookmate.settermate.di

import com.corp.bookmate.settermate.data.DatabaseDriverFactory
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
}
