package cl.stryda.app.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Ruta {
    @Serializable data object Corrales : Ruta
    @Serializable data class Corral(val corralId: String) : Ruta
    @Serializable data class Caballo(val caballoId: String) : Ruta
    @Serializable data object Carreras : Ruta
    @Serializable data object Ajustes : Ruta

    /** Pantallas del plan que aún no se construyen: se ven, se navegan y dicen qué vendrá. */
    @Serializable data class Pendiente(val titulo: String, val detalle: String) : Ruta
}
