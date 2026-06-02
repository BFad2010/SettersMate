package com.corp.bookmate.settermate.service

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface SettersApi {

    @FormUrlEncoded
    @POST("Leagues/Schedules")
    suspend fun getLeagueInfo(
        @Field("leagueday") leagueDay: Int,
        @Field("lid") leagueId: Int,
    ): Response<String>

    @GET("Leagues/GetLeaguesByDay")
    suspend fun getLeaguesByDay(
        @Query("dayid") dayId: Int
    ): Response<String>

    @GET
    suspend fun downloadSchedule(
        @Url url: String
    ): Response<ResponseBody>
}