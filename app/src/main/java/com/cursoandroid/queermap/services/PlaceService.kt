package com.cursoandroid.queermap.services

import android.util.Log
import com.cursoandroid.queermap.models.Place
import com.google.firebase.firestore.FirebaseFirestore

class PlaceService {
    private val db = FirebaseFirestore.getInstance()

    // Función para crear un lugar en la base de datos
    public fun createPlace(place: Place) {
        // Crear un mapa con los datos del lugar
        val placeData = hashMapOf(
            "name" to place.name,
            "description" to place.description,
            "phone" to place.phone,
            "web" to place.website,
            "category" to place.category,
            "latitude" to place.latitude,
            "longitude" to place.longitude
        )
        // Añadir el lugar a la colección "places" en la base de datos
        db.collection("places")
            .add(placeData)
            .addOnSuccessListener { response ->
                Log.d("TAG", "DocumentSnapshot added with ID: ${response.id}")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error adding document", e)
            }
    }

    // Función para obtener todos los lugares de la base de datos
    fun getPlaces(callback: (List<Place>) -> Unit) {
        // Obtener la colección "places" de la base de datos
        db.collection("places")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val places = mutableListOf<Place>()
                for (document in querySnapshot.documents) {
                    // Convertir el documento a un objeto Place
                    val place = document.toObject(Place::class.java)
                    place?.let {
                        places.add(place)
                    }
                }
                // Llamar al callback con la lista de lugares obtenidos
                callback(places)
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error getting places", e)
                // En caso de error, llamar al callback con una lista vacía
                callback(emptyList())
            }
    }
}