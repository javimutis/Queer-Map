package com.cursoandroid.queermap

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.GoogleMap.OnPoiClickListener
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.PointOfInterest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    OnMyLocationButtonClickListener, OnMyLocationClickListener, OnPoiClickListener,
    OnMapsSdkInitializedCallback {

    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        MapsInitializer.initialize(applicationContext, Renderer.LATEST, this)

        val options = GoogleMapOptions()
            .mapType(GoogleMap.MAP_TYPE_NORMAL)
            .compassEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)

        val mapFragment = SupportMapFragment.newInstance(options)
        supportFragmentManager.beginTransaction()
            .add(R.id.map, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)
    }
    data class MarkerCategory(val name: String, val icon: Float)
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        googleMap.setOnPoiClickListener(this)

        // Habilitar la capa Mi ubicación
        enableMyLocation()

        // Obtener los ajustes de interfaz de usuario del mapa
        val uiSettings = googleMap.uiSettings

        // Habilitar los controles de zoom
        uiSettings.isZoomControlsEnabled = true

        // Agregar marcadores con categorías
        val organization = MarkerCategory("Organizaciones", BitmapDescriptorFactory.HUE_RED)
        val culture = MarkerCategory("Cultura", BitmapDescriptorFactory.HUE_ORANGE)
        val health = MarkerCategory("Salud", BitmapDescriptorFactory.HUE_YELLOW)
        val entertainment = MarkerCategory("Entretenimiento", BitmapDescriptorFactory.HUE_GREEN)
        val shops = MarkerCategory("Tiendas", BitmapDescriptorFactory.HUE_BLUE)
        val exploration = MarkerCategory("Exploración", BitmapDescriptorFactory.HUE_VIOLET)

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.01133938329604, -71.54251642642323))
                .title("Prevención Viña")
                .icon(BitmapDescriptorFactory.defaultMarker(organization.icon))
        )

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.04744191035794, -71.60834929999501))
                .title("Teatro Municipal de Valparaíso")
                .icon(BitmapDescriptorFactory.defaultMarker(culture.icon))
        )

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.01074603885636, -71.54784261280741))
                .title("Médico Ginecológico Obstetra -  María Cindy Díaz Díaz")
                .icon(BitmapDescriptorFactory.defaultMarker(health.icon))
        )

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.043516724940865, -71.61742898440514))
                .title("Pagano")
                .icon(BitmapDescriptorFactory.defaultMarker(entertainment.icon))
        )

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.02039838048769, -71.55795668873755))
                .title("Mo Gastrobar")
                .icon(BitmapDescriptorFactory.defaultMarker(entertainment.icon))
        )

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.0216340714256, -71.55631179242113))
                .title("SexShop 'Tentación y deseo'")
                .icon(BitmapDescriptorFactory.defaultMarker(shops.icon))
        )

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.04500143204479, -71.50136662854236))
                .title("Zona de cruising en 'Jardín Botánico de Viña del Mar'")
                .icon(BitmapDescriptorFactory.defaultMarker(exploration.icon))
        )
        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(-33.038436316953494, -71.62810017643785))
                .title("Atención psicologíca, 'Patricia Casanova'")
                .icon(BitmapDescriptorFactory.defaultMarker(health.icon))
        )
            }

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

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Botón de Mi ubicación clickeado", Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        val currentPosition = LatLng(location.latitude, location.longitude)
        googleMap.addMarker(
            MarkerOptions()
                .position(currentPosition)
                .title("Mi ubicación actual")
        )
    }

    override fun onPoiClick(poi: PointOfInterest) {
        Toast.makeText(
            this,
            "Clicked: ${poi.name}\nPlace ID: ${poi.placeId}\nLatitude: ${poi.latLng.latitude} Longitude: ${poi.latLng.longitude}",
            Toast.LENGTH_SHORT
        ).show()


    }

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
