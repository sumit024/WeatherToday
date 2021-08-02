package com.app_devs.weathertoday.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Build.VERSION.SDK

object Constants {
    const val APP_ID:String= "177192ec0279ce5abfaf5542194f0ca8"
    const val BASE_URL:String="https://api.openweathermap.org/data/"
    const val METRIC_UNIT="metric"
    const val PREFERENCE_NAME="WeatherPreferenceName"
    const val WEATHER_RESPONSE_DATA="weather_response_data"

    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //For newer versions of android
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            val network=connectivityManager.activeNetwork?:return false
            val activeNetwork=connectivityManager.getNetworkCapabilities(network)?:return false
            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->true
                else ->false
            }
        }
        //For older versions of android
        else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}