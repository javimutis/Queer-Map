package com.cursoandroid.queermap.activities

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.models.Place
import com.cursoandroid.queermap.services.PlaceService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener,
    OnMapsSdkInitializedCallback, GoogleMap.OnMapClickListener {

    private lateinit var googleMap: GoogleMap
    private val iconWidth: Int = 65
    private val iconHeight: Int = 75
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var pendingPlaces: List<Place>
    private val verifiedPlaces: MutableList<Place> = mutableListOf()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bottomSheetView: View
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Inicializar los mapas y el renderizador
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)

        // Configurar las opciones del mapa
        val options = GoogleMapOptions()
            .mapType(GoogleMap.MAP_TYPE_NORMAL)
            .compassEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)

        // Crear el fragmento del mapa con las opciones configuradas
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            ?: SupportMapFragment.newInstance(options)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map, mapFragment)
            .commit()

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar el cliente de Places
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        // Obtener el mapa de forma asíncrona cuando esté listo
        mapFragment.getMapAsync(this)


        // Configurar el bottom sheet
        bottomSheetView = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN


        // Establecer un callback para detectar los cambios de estado del bottom sheet
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // El bottom sheet está expandido
                    // Realiza las acciones necesarias
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // El bottom sheet está colapsado
                    // Realiza las acciones necesarias
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // El bottom sheet está deslizándose
                // Realiza las acciones necesarias
            }
        })
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Establecer listeners de eventos en el mapa
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        googleMap.setOnMapClickListener(this)

        // Habilitar la capa "Mi ubicación" en el mapa
        enableMyLocation()

        // Obtener la configuración de la interfaz de usuario del mapa
        val uiSettings = googleMap.uiSettings
        // Habilitar los controles de zoom
        uiSettings.isZoomControlsEnabled = true
        // Establecer el estilo del mapa
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e("MapsActivity", "Error al cargar el estilo del mapa.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapsActivity", "No se pudo encontrar el estilo del mapa. Error: $e")
        }

        val placeService = PlaceService()
        placeService.getPlaces { places ->
            pendingPlaces = places
            for (place in places) {
                if (place.verified) {
                    val latLng = LatLng(place.latitude, place.longitude)
                    val markerOptions = MarkerOptions()
                        .position(latLng)
                        .title(place.name)
                        .snippet(place.id)

                    // Asociar un icono diferente dependiendo de la categoría
                    val iconBitmap = when (place.category) {
                        "Comunidad" -> BitmapFactory.decodeResource(
                            resources,
                            R.drawable.community_icon
                        )

                        "Cultura" -> BitmapFactory.decodeResource(
                            resources,
                            R.drawable.culture_icon
                        )

                        "Salud" -> BitmapFactory.decodeResource(resources, R.drawable.health_icon)
                        "Entretenimiento" -> BitmapFactory.decodeResource(
                            resources,
                            R.drawable.entertainment_icon
                        )

                        "Tiendas" -> BitmapFactory.decodeResource(resources, R.drawable.shops_icon)
                        "Exploración" -> BitmapFactory.decodeResource(
                            resources,
                            R.drawable.exploration_icon
                        )

                        else -> BitmapFactory.decodeResource(resources, R.drawable.default_marker)
                    }
                    // Cambiar el tamaño del icono
                    if (iconBitmap != null) {
                        val scaledIcon =
                            Bitmap.createScaledBitmap(iconBitmap, iconWidth, iconHeight, false)
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(scaledIcon))
                        googleMap.addMarker(markerOptions)
                    } else {
                        googleMap.addMarker(markerOptions)
                    }
                }
            }
        }
        // Configurar la interacción con el mapa para mostrar el bottom sheet al hacer clic en un marcador
        googleMap.setOnMarkerClickListener { marker ->
            val placeId = marker.snippet

            if (!placeId.isNullOrBlank()) {
                // Consultar la base de datos utilizando el ID del marcador
                db.collection("places").document(placeId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val place = document.toObject(Place::class.java)

                            // Actualizar la vista del bottom sheet con los datos obtenidos
                            val bottomName = bottomSheetView.findViewById<TextView>(R.id.bottomName)
                            bottomName.text = place?.name

                            val bottomSpinner =
                                bottomSheetView.findViewById<TextView>(R.id.bottomSpinner)
                            bottomSpinner.text = place?.category

                            val bottomPhone =
                                bottomSheetView.findViewById<ImageButton>(R.id.bottomPhone)
                            bottomPhone.setOnClickListener {
                                val phoneNumber = place?.phone
                                if (!phoneNumber.isNullOrBlank()) {
                                    val intent =
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                                    startActivity(intent)
                                } else {
                                    val message =
                                        "No se ha proporcionado un número de teléfono para este lugar."
                                    Snackbar.make(bottomSheetView, message, Snackbar.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            val bottomWebsite =
                                bottomSheetView.findViewById<ImageButton>(R.id.bottomWebsite)
                            bottomWebsite.setOnClickListener {
                                val website = place?.website
                                if (!website.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                                    val activities = packageManager.queryIntentActivities(
                                        intent,
                                        PackageManager.MATCH_DEFAULT_ONLY
                                    )
                                    if (activities.isNotEmpty()) {
                                        startActivity(intent)
                                    } else {
                                        // No hay aplicaciones de navegador disponibles
                                         Toast.makeText(
                                            this,
                                            "No se encontró una aplicación de navegador.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }


                            val bottomDescription =
                                bottomSheetView.findViewById<TextView>(R.id.bottomDescription)
                            bottomDescription.text = place?.description

                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Manejar la falla en la consulta a la base de datos
                        Log.e(TAG, "Error al consultar la base de datos: ", exception)
                    }
            }

            true // Devolver true para indicar que el evento del clic en el marcador ha sido manejado
        }

        // Obtener la última ubicación conocida del usuario
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val currentPosition = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentPosition,
                                15f
                            )
                        )
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
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT)
                    .show()
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
    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d(
                "MapsDemo",
                "The latest version of the renderer is used."
            )

            MapsInitializer.Renderer.LEGACY -> Log.d(
                "MapsDemo",
                "The legacy version of the renderer is used."
            )
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
