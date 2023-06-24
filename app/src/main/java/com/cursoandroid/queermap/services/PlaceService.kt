package com.cursoandroid.queermap.services

import android.util.Log
import com.cursoandroid.queermap.models.Place
import com.google.firebase.firestore.FirebaseFirestore

class PlaceService {
    private val db = FirebaseFirestore.getInstance()

    public fun createPlace(place: Place) {

        val placeData = hashMapOf(
            "name" to place.name,
            "description" to place.description,
            "category" to place.category,
            "latitude" to place.latitude,
            "longitude" to place.longitude
        )
        db.collection("places")
            .add(placeData)
            .addOnSuccessListener { response ->
                Log.d("TAG", "DocumentSnapshot added with ID: ${response.id}")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error adding document", e)
                            }
    }
    fun getPlaces(callback: (List<Place>) -> Unit) {
        db.collection("places")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val places = mutableListOf<Place>()
                for (document in querySnapshot.documents) {
                    val place = document.toObject(Place::class.java)
                    place?.let {
                        places.add(place)
                    }
                }
                callback(places)
                            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error getting places", e)
                callback(emptyList())
            }    }

}