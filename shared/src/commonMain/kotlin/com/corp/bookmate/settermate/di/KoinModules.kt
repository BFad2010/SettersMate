package com.corp.bookmate.settermate.di

import com.corp.bookmate.settermate.data.DatabaseDriverFactory
import com.corp.bookmate.settermate.data.FavoritesDatabase
import com.corp.bookmate.settermate.data.FavoritesRepo
import com.corp.bookmate.settermate.service.SettersRepo
import com.corp.bookmate.settermate.ui.LeaguesViewModel
import com.corp.bookmate.settermate.ui.YoursViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    single<HttpClient> {
        HttpClient {
            install(Logging) { level = LogLevel.INFO }
        }
    }
    singleOf(::SettersRepo)
    single { FavoritesDatabase(get<DatabaseDriverFactory>().createDriver()) }
    singleOf(::FavoritesRepo)
    viewModelOf(::LeaguesViewModel)
    viewModelOf(::YoursViewModel)
}
