package cl.stryda.app.domain

import java.time.Instant

/** Datos del caballo que el preparador puede corregir o completar desde la app. */
enum class CampoCaballo(val clave: String, val etiqueta: String) {
    ESTADO("estado", "Estado"),
    PELAJE("pelaje", "Pelaje"),
    SEXO("sexo", "Sexo"),
    EDAD("edad", "Edad"),
    PADRE("padre", "Padre"),
    MADRE("madre", "Madre");

    companion object {
        fun desde(clave: String?): CampoCaballo? = entries.firstOrNull { it.clave == clave }
    }
}

data class CambioCaballo(
    val id: String,
    val caballoId: String,
    val instante: Instant,
    val campo: CampoCaballo,
    val valorAnterior: String?,
    val valorNuevo: String?,
    val nota: String? = null,
)

/** Valor actual de un campo en el formato en que se guarda un cambio. */
fun Caballo.valorDe(campo: CampoCaballo): String? = when (campo) {
    CampoCaballo.ESTADO -> estado.clave
    CampoCaballo.PELAJE -> pelaje
    CampoCaballo.SEXO -> sexo?.clave
    CampoCaballo.EDAD -> edad?.toString()
    CampoCaballo.PADRE -> padre
    CampoCaballo.MADRE -> madre
}

/**
 * El caballo vigente = dato base + el último cambio manual de cada campo.
 * Derivado, nunca almacenado: el dato base y el historial completo se conservan.
 */
fun aplicarCambios(caballo: Caballo, cambios: List<CambioCaballo>): Caballo =
    cambios
        .filter { it.caballoId == caballo.id }
        .sortedBy { it.instante }
        .fold(caballo) { k, c ->
            val v = c.valorNuevo?.trim()?.takeIf { it.isNotEmpty() }
            when (c.campo) {
                CampoCaballo.ESTADO -> k.copy(estado = EstadoCaballo.desde(v))
                CampoCaballo.PELAJE -> k.copy(pelaje = v)
                CampoCaballo.SEXO -> k.copy(sexo = Sexo.desde(v))
                CampoCaballo.EDAD -> k.copy(edad = v?.toIntOrNull())
                CampoCaballo.PADRE -> k.copy(padre = v)
                CampoCaballo.MADRE -> k.copy(madre = v)
            }
        }

/** Límites para detectar errores de tipeo, no para decidir por el preparador. */
object RangosMedicion {
    val PESO_KG = 300.0..700.0
    val TEMPERATURA = 35.0..42.5
    val FC_REPOSO = 20.0..90.0
}
