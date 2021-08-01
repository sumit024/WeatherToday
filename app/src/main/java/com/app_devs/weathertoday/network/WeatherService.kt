package com.app_devs.weathertoday.network

import com.app_devs.weathertoday.models.WeatherResponse
import retrofit.Call
import retrofit.http.GET
import retrofit.http.Query

interface WeatherService {
    @GET("2.5/weather")
    fun getWeather(
                   @Query("lat") lat:Double,
                   @Query("lon") lon:Double,
                   @Query("appid") appid:String?,
                   @Query("units") units:String
    ):Call<WeatherResponse>

}