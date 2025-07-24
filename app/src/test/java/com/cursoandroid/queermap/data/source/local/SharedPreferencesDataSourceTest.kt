package com.cursoandroid.queermap.data.source.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class SharedPreferencesDataSourceTest {

    // Mock del Context de Android
    private val mockContext: Context = mockk(relaxed = true)

    // Mock del SharedPreferences
    private val mockSharedPreferences: SharedPreferences = mockk(relaxed = true)

    // Mock del Editor de SharedPreferences
    private val mockEditor: SharedPreferences.Editor = mockk(relaxed = true)

    // Instancia de la clase a testear
    private lateinit var sharedPreferencesDataSource: SharedPreferencesDataSource

    @Before
    fun setUp() {
        // Configura los mocks antes de cada test
        // Cuando se llame a getSharedPreferences en el mockContext, devuelve mockSharedPreferences
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences

        // Cuando se llame a edit() en el mockSharedPreferences, devuelve mockEditor
        every { mockSharedPreferences.edit() } returns mockEditor

        // Asegúrate de que las llamadas a putString y apply en el editor devuelvan el propio editor o Unit
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        every { mockEditor.clear() } returns mockEditor // Para el test de clearCredentials

        // Inicializa la clase con los mocks
        sharedPreferencesDataSource = SharedPreferencesDataSource(mockContext)
    }

    @Test
    fun `when saveCredentials is called then credentials are saved correctly`() {
        // Given: credenciales de prueba
        val email = "test@example.com"
        val password = "password123"

        // When: se llama a saveCredentials
        sharedPreferencesDataSource.saveCredentials(email, password)

        // Then: se verifica que putString se llamó con los valores correctos y apply fue llamado
        verify(exactly = 1) {
            mockEditor.putString("email", email)
            mockEditor.putString("password", password)
            mockEditor.apply()
        }
    }

    @Test
    fun `when loadSavedCredentials is called then saved credentials are returned`() {
        // Given: credenciales que se simulan estar guardadas
        val savedEmail = "saved@example.com"
        val savedPassword = "savedPassword"

        // Configura el mock de SharedPreferences para devolver los valores esperados
        every { mockSharedPreferences.getString("email", "") } returns savedEmail
        every { mockSharedPreferences.getString("password", "") } returns savedPassword

        // When: se llama a loadSavedCredentials
        val result = sharedPreferencesDataSource.loadSavedCredentials()

        // Then: se verifica que los valores devueltos son los esperados
        assertEquals(Pair(savedEmail, savedPassword), result)
    }

    @Test
    fun `when loadSavedCredentials is called and no credentials exist then empty strings are returned`() {
        // Given: no hay credenciales guardadas (devuelve los valores por defecto "")
        every { mockSharedPreferences.getString("email", "") } returns ""
        every { mockSharedPreferences.getString("password", "") } returns ""

        // When: se llama a loadSavedCredentials
        val result = sharedPreferencesDataSource.loadSavedCredentials()

        // Then: se verifica que se devuelven strings vacíos
        assertEquals(Pair("", ""), result)
    }

    @Test
    fun `when clearCredentials is called then credentials are cleared`() {
        // When: se llama a clearCredentials
        sharedPreferencesDataSource.clearCredentials()

        // Then: se verifica que clear y apply fueron llamados en el editor
        verify(exactly = 1) {
            mockEditor.clear()
            mockEditor.apply()
        }
    }
}