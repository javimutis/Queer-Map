package com.cursoandroid.queermap.services
import com.cursoandroid.queermap.models.Place
import com.google.firebase.firestore.FirebaseFirestore

class PlaceService {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val placesCollection = firestore.collection("places")

    // Función para obtener todos los lugares pendientes de verificación
    fun getPendingPlaces(callback: (List<Place>) -> Unit) {
        placesCollection
            .whereEqualTo("verified", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val pendingPlaces = querySnapshot.toObjects(Place::class.java)
                callback(pendingPlaces)
            }
            .addOnFailureListener { exception ->
                callback(emptyList())
            }
    }

    fun getPlaces(callback: (List<Place>) -> Unit) {
        placesCollection
            .get()
            .addOnSuccessListener { querySnapshot ->
                val places = querySnapshot.toObjects(Place::class.java)
                callback(places)
            }
            .addOnFailureListener { exception ->
                callback(emptyList())
            }
    }

    fun addPlace(place: Place) {
        placesCollection
            .add(place)
            .addOnSuccessListener { documentReference ->
                val newPlaceId = documentReference.id
                place.id = newPlaceId
                documentReference.update("id", newPlaceId)
            }
            .addOnFailureListener { exception ->
                // Manejar el error al agregar el lugar
            }
    }

    // Función para verificar un lugar
    fun verifyPlace(place: Place, callback: (Boolean) -> Unit) {
        place.id?.let { placeId ->
            placesCollection
                .document(placeId)
                .update("verified", true)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener { exception ->
                    callback(false)
                }
        }
    }
}
