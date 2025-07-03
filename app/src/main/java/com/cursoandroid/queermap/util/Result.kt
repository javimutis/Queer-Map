package com.cursoandroid.queermap.util // Asegúrate de que este paquete coincida con la ruta real del archivo

/**
 * Una clase sellada para representar el resultado de una operación que puede
 * ser exitosa o fallar.
 *
 * @param T El tipo de dato en caso de éxito.
 */
sealed class Result<out T> {
    /**
     * Representa un resultado exitoso.
     * @param data El dato resultante de la operación exitosa.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Representa un resultado fallido.
     * @param exception La excepción o Throwable que causó el fallo.
     * @param message Un mensaje opcional que describe el error.
     */
    data class Failure(val exception: Throwable, val message: String? = null) : Result<Nothing>()
}

/**
 * Realiza la acción dada si este Result es un Success.
 *
 * @param action La función a ejecutar con el valor si el resultado es Success.
 * @return El propio objeto Result para encadenamiento.
 */
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Realiza la acción dada si este Result es un Failure.
 *
 * @param action La función a ejecutar con la excepción si el resultado es Failure.
 * @return El propio objeto Result para encadenamiento.
 */
inline fun <T> Result<T>.onFailure(action: (exception: Throwable) -> Unit): Result<T> {
    if (this is Result.Failure) {
        action(exception)
    }
    return this
}

// Puedes considerar añadir estas funciones si las necesitas en otros lugares.
// Son equivalentes a las de kotlin.Result y pueden ser útiles.

/**
 * Retorna el valor en caso de éxito o lanza la excepción si es un fallo.
 * @throws Throwable La excepción almacenada si el resultado es un Failure.
 * @return El valor si es un Success.
 */
fun <T> Result<T>.getOrThrow(): T {
    return when (this) {
        is Result.Success -> data
        is Result.Failure -> throw exception
    }
}

/**
 * Retorna el valor en caso de éxito, o null si es un fallo.
 * @return El valor si es un Success, null si es un Failure.
 */
fun <T> Result<T>.getOrNull(): T? {
    return (this as? Result.Success)?.data
}

/**
 * Retorna la excepción si es un fallo, o null si es un éxito.
 * @return La excepción si es un Failure, null si es un Success.
 */
fun <T> Result<T>.exceptionOrNull(): Throwable? {
    return (this as? Result.Failure)?.exception
}

/**
 * Crea un Result.Success con el valor dado.
 */
fun <T> success(value: T): Result<T> = Result.Success(value)

/**
 * Crea un Result.Failure con la excepción dada.
 */
fun <T> failure(exception: Throwable): Result<T> = Result.Failure(exception)
