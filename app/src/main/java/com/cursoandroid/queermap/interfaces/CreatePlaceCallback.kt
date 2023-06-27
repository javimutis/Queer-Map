package com.cursoandroid.queermap.interfaces

import com.cursoandroid.queermap.models.Place

interface CreatePlaceCallback {
    fun onPlaceCreated(place: Place)
    fun onVerificationComplete(success: Boolean)
}
