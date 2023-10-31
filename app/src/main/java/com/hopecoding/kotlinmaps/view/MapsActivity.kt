package com.hopecoding.kotlinmaps.view
import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.hopecoding.kotlinmaps.R
import com.hopecoding.kotlinmaps.databinding.ActivityMapsBinding
import com.hopecoding.kotlinmaps.model.Place
import com.hopecoding.kotlinmaps.roomdb.PlaceDao
import com.hopecoding.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPref: SharedPreferences
    var trackBoolean: Boolean? = null
    private var selectedLatitute: Double? = null
    private var selectedLongitute: Double? = null
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    //Kullan-At Objesi
    val compositeDisposable = CompositeDisposable()
    var placeFromMain: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        registerLauncher()

        binding.saveButton.isEnabled = false
        sharedPref = this.getSharedPreferences("com.hopecoding.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitute = 0.0
        selectedLongitute = 0.0

        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places")
            //.allowMainThreadQueries() işe yarar ama doğru yöntem değil. RxJava ile yapmalıyız.
            .build()

        placeDao = db.placeDao()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new") {
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE // Gözükmediği gibi yerde tutmaz.

            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // 1 kereliğine kontrol ediyoruz program açıldığında.
                    trackBoolean = sharedPref.getBoolean("track", false)
                    if (trackBoolean == false) {
                        println("sharedPref çalıştı.")
                        val userLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        sharedPref.edit().putBoolean("track", true).apply()

                    }
                    println("location " + location.toString())

                }


            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    Snackbar.make(
                        binding.root,
                        "Permission needed for location",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give Permission") {
                        //request Permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                } else {
                    //request Permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                //granted
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    10f,
                    locationListener
                )
                val lastLocation =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null) {
                    val userLastLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation, 15f))
                }
                mMap.isMyLocationEnabled = true
            }
            //latitude, longitude

            //lat-> 48.858093 lon-> 2.294694 Eiffel Tower

            // Add a marker in Sydney and move the camera
//        val eiffel = LatLng(48.858093,2.294694)
//        mMap.addMarker(MarkerOptions().position(eiffel).title("Eiffel Tower"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel,15f))
        } else {
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("location") as? Place

            placeFromMain?.let {
                val latLng = LatLng(it.latitude, it.longitude)

                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
            }

        }
        //casting

    }

    private fun registerLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //permission granted
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000,
                            10f,
                            locationListener
                        )
                        val lastLocation =
                            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (lastLocation != null) {
                            val userLastLocation =
                                LatLng(lastLocation.latitude, lastLocation.longitude)
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    userLastLocation,
                                    15f
                                )
                            )
                        }
                        mMap.isMyLocationEnabled = true

                    }
                } else {
                    Toast.makeText(this@MapsActivity, "Permission Needed!", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()

        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatitute = p0.latitude
        selectedLongitute = p0.longitude
        binding.saveButton.isEnabled = true

    }

    //Gelen Cevabı Ele Al
    private fun handleResponse() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }

    fun save(view: View) {

        //Main Thread UI, Default -> CPU ,IO Thread Internet/Database

        if (selectedLatitute != null && selectedLongitute != null) {
            val place =
                Place(binding.placeText.text.toString(), selectedLatitute!!, selectedLongitute!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }


    }

    fun delete(view: View) {

        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }


    }

    override fun onDestroy() {
        super.onDestroy()


        compositeDisposable.clear()
        println("onDestroy open")
    }


    override fun onBackPressed() {
        super.onBackPressed()
        handleResponse()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

}