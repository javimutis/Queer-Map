package com.cursoandroid.queermap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    OnMyLocationButtonClickListener, OnMyLocationClickListener,
    OnMapsSdkInitializedCallback, OnMapClickListener {

    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Inicializar los mapas y el renderizador
        MapsInitializer.initialize(applicationContext, Renderer.LATEST, this)

        // Configurar las opciones del mapa
        val options = GoogleMapOptions()
            .mapType(GoogleMap.MAP_TYPE_NORMAL)
            .compassEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)

        // Crear el fragmento del mapa con las opciones configuradas
        val mapFragment = SupportMapFragment.newInstance(options)
        supportFragmentManager.beginTransaction()
            .add(R.id.mapFragment, mapFragment)
            .commit()

        // Obtener el mapa de forma asíncrona cuando esté listo
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Establecer listeners de eventos en el mapa
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        googleMap.setOnMapClickListener (this)

        // Habilitar la capa "Mi ubicación" en el mapa
        enableMyLocation()

        // Obtener la última ubicación conocida del usuario
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val currentPosition = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))

                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        // Obtener la configuración de la interfaz de usuario del mapa
        val uiSettings = googleMap.uiSettings

        // Habilitar los controles de zoom
        uiSettings.isZoomControlsEnabled = true


}

    // Habilitar la capa "Mi ubicación" en el mapa
    private fun enableMyLocation() {
        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    // Verificar si se tiene permiso de ubicación
    private fun hasLocationPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    // Manejar el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (isPermissionGranted(
                    permissions,
                    grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || isPermissionGranted(
                    permissions,
                    grantResults,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onMapClick(latLng: LatLng) {
        val latitud = latLng.latitude
        val longitud = latLng.longitude
        val intent = Intent(this, PlaceActivity::class.java)
        intent.putExtra("latitud", latitud)
        intent.putExtra("longitud", longitud)
        startActivity(intent)
    }

    // Manejar el evento del botón "Mi ubicación"
    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Botón de Mi ubicación clickeado", Toast.LENGTH_SHORT).show()
        return false
    }

    // Manejar el evento de clic en "Mi ubicación"
    override fun onMyLocationClick(location: Location) {
        val currentPosition = LatLng(location.latitude, location.longitude)

    }



    // Verificar si se otorgó un permiso específico
    private fun isPermissionGranted(
        permissions: Array<String>,
        grantResults: IntArray,
        permission: String
    ): Boolean {
        for (i in permissions.indices) {
            if (permission == permissions[i]) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }

    // Manejar la inicialización del SDK de mapas
    override fun onMapsSdkInitialized(renderer: Renderer) {
        when (renderer) {
            Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
            Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


}
