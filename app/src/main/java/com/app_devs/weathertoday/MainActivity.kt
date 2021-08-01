package com.app_devs.weathertoday

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.app_devs.weathertoday.models.WeatherResponse
import com.app_devs.weathertoday.network.WeatherService
import com.app_devs.weathertoday.utils.Constants
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import retrofit.GsonConverterFactory
import retrofit.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mFusedLocationClient:FusedLocationProviderClient
    private var mLatitude:Double=0.0
    private var mLongitude:Double=0.0

    private var mProgressDialog:Dialog?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)

        if(!isLocationEnabled())
        {
            Toast.makeText(this,"Please turn on your location",Toast.LENGTH_SHORT).show()
            val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else
        {
            Dexter.withActivity(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    .withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()) {
                                requestLocationData()
                            }

                            if (report.isAnyPermissionPermanentlyDenied) {
                                Toast.makeText(
                                        this@MainActivity,
                                        "You have denied location permission. Please allow it is mandatory.",
                                        Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                        ) {
                            showRationalDialogForPermissions()
                        }
                    }).onSameThread()
                    .check()
        }
    }
    /**
     * A function which is used to verify that the location or GPS is enable or not of the user's device.
     */
    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services.
        val locationManager: LocationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
                .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
                .setPositiveButton(
                        "GO TO SETTINGS"
                ) { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancel") { dialog,
                                               _ ->
                    dialog.dismiss()
                }.show()
    }

    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        )
    }

    /**
     * A location callback object of fused location provider client where we will get the current location details.
     * 177192ec0279ce5abfaf5542194f0ca8

     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")

            getLocationWeatherDetails(mLatitude,mLongitude)
        }
    }

    private fun getLocationWeatherDetails(latitude:Double, longitude:Double)
    {
        if(Constants.isNetworkAvailable(this)) {
            //Toast.makeText(this,"Connected",Toast.LENGTH_SHORT).show()
            val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            val service: WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                    latitude, longitude, Constants.APP_ID, Constants.METRIC_UNIT)

            showCustomDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{
                @RequiresApi(Build.VERSION_CODES.N)
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if(response!!.isSuccess)
                    {

                        val weatherList:WeatherResponse=response.body()
                        setUpUI(weatherList)
                        Log.i("Response","$weatherList")
                        hideProgressDialog()
                    }
                    else
                    {
                        when(response.code()){
                            400->
                                Log.e("Error 400","Bad connection")
                            404->
                                Log.e("Error 404","Not Found")
                            else->{
                                Log.e("Error","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    hideProgressDialog()
                    Log.e("Errorrr",t!!.message.toString())
                }

            })
        }
        else
            Toast.makeText(this,"Not Connected",Toast.LENGTH_SHORT).show()

    }

    private fun showCustomDialog()
    {
        mProgressDialog= Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }
    private fun hideProgressDialog()
    {
        mProgressDialog?.dismiss()
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)

    private fun setUpUI(weatherList: WeatherResponse)
    {
        for(i in weatherList.weather.indices)
        {
            tv_main.text=weatherList.weather[i].main
            tv_main_description.text=weatherList.weather[i].description
            tv_temp.text=weatherList.main.temp.toString()+  getUnit(application.resources.configuration.locales.toString())

            tv_sunrise_time.text=unixTime(weatherList.sys.sunrise.toLong())
            tv_sunset_time.text=unixTime(weatherList.sys.sunset.toLong())

            tv_humidity.text = weatherList.main.humidity.toString() + "%"
            tv_min.text = weatherList.main.temp_min.toString() + " min"
            tv_max.text = weatherList.main.temp_max.toString() + " max"
            tv_speed.text = weatherList.wind.speed.toString()
            tv_name.text = weatherList.name
            tv_country.text = weatherList.sys.country

            when(weatherList.weather[i].icon)
            {
                "01d"-> iv_main.setImageResource(R.drawable.sunny)
                "02d" -> iv_main.setImageResource(R.drawable.cloud)
                "03d" -> iv_main.setImageResource(R.drawable.cloud)
                "04d" -> iv_main.setImageResource(R.drawable.cloud)
                "04n" -> iv_main.setImageResource(R.drawable.cloud)
                "10d" -> iv_main.setImageResource(R.drawable.rain)
                "11d" -> iv_main.setImageResource(R.drawable.storm)
                "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                "01n" -> iv_main.setImageResource(R.drawable.cloud)
                "02n" -> iv_main.setImageResource(R.drawable.cloud)
                "03n" -> iv_main.setImageResource(R.drawable.cloud)
                "10n" -> iv_main.setImageResource(R.drawable.cloud)
                "11n" -> iv_main.setImageResource(R.drawable.rain)
                "13n" -> iv_main.setImageResource(R.drawable.snowflake)
            }
        }
    }
    private fun getUnit(value:String): String
    {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun unixTime(timex:Long):String
    {
        val date=Date(timex*1000L)
        val sdf= SimpleDateFormat("HH:mm",Locale.UK)
        sdf.timeZone= TimeZone.getDefault()
        return sdf.format(date)
    }




}