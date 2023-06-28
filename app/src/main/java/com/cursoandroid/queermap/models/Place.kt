package com.cursoandroid.queermap.models
// Definición de la clase "Place" como un data class para representar un lugar.
data class Place(
    var id: String? = null,
    val name: String,
    val description: String,
    val phone: String? = null,
    val website: String? = null,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    var verified: Boolean = false
) {
    // Constructor primario de la clase que inicializa todos los campos.
    constructor() : this("", "", "", "", "", "", 0.0, 0.0, false)
}
