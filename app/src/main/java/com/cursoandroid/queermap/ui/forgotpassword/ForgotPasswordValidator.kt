package com.cursoandroid.queermap.ui.forgotpassword

import com.cursoandroid.queermap.common.EmailValidator
import javax.inject.Inject
import javax.inject.Singleton

// Convertir a una clase inyectable para usarla en ViewModel
@Singleton // Marcar como Singleton si no tiene estado y quieres una sola instancia
class ForgotPasswordValidator @Inject constructor() {
    fun isValidEmail(email: String): Boolean {
        return EmailValidator.isValidEmail(email)
    }
    // isValidPassword se eliminó ya que no es relevante para el flujo de recuperación de contraseña
}