package com.cursoandroid.queermap.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cursoandroid.queermap.R

/*Aquí implementarás la lógica del registro de nuevos usuarios, como validar los campos de entrada, crear una cuenta utilizando Firebase Authentication y manejar las opciones de registro con Google, Facebook o Instagram.*/
/*Implementa la lógica del registro de nuevos usuarios.
En el método onCreate, configura el Data Binding para el diseño XML de activity_signin.xml. Vincula los elementos de la interfaz de usuario, como los EditText para el correo electrónico y la contraseña, y el botón para registrar.
Implementa los métodos para manejar eventos de botones, como el método onClickListener para el botón de registro.
En el método onClickListener del botón de registro, obtén los valores de los campos de entrada, valida los datos utilizando la clase ValidationUtils, y luego utiliza los métodos de Firebase Authentication para crear una nueva cuenta de usuario.
Para las opciones de registro social (Google, Facebook, Instagram), utiliza las correspondientes API y SDK proporcionados por cada plataforma.*/
class SigninActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)
    }
}