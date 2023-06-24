package com.cursoandroid.queermap.models

data class Place (
    val id:String?,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
)
{ constructor(): this(null, "", "", "", 0.0, 0.0)
}