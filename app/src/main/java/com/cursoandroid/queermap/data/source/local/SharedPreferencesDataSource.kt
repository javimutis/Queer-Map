package com.cursoandroid.queermap.data.source.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPreferencesDataSource @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences = context.getSharedPreferences("login", Context.MODE_PRIVATE)

    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit()
            .putString("email", email)
            .putString("password", password)
            .apply()
    }

    fun loadSavedCredentials(): Pair<String?, String?> {
        val email = sharedPreferences.getString("email", "")
        val password = sharedPreferences.getString("password", "")
        return Pair(email, password)
    }

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }
}