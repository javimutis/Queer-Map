package com.cursoandroid.queermap.models
// Definición de la clase "Place" como un data class para representar un lugar.
data class Place(
    val id: String?, // Identificador del lugar
    val name: String, // Nombre del lugar
    val description: String, // Descripción del lugar
    val phone: String?, // Telefono del lugar
    val website: String?, // Web del lugar
    val category: String, // Categoría del lugar
    val latitude: Double, // Latitud del lugar
    val longitude: Double // Longitud del lugar
) {

    // Constructor primario de la clase que inicializa todos los campos.
    constructor() : this(null, "", "", "", "", "", 0.0, 0.0)
}
