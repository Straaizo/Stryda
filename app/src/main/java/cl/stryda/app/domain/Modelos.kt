package cl.stryda.app.domain

import java.time.LocalDate
import java.time.LocalTime

data class Corral(
    val id: String,
    val nombre: String,
    val preparador: String,
    val hipodromo: String,
)

data class CorralResumen(val corral: Corral, val numeroEjemplares: Int)

enum class Sexo(val clave: String, val etiqueta: String) {
    MACHO("macho", "Macho"),
    HEMBRA("hembra", "Hembra"),
    CASTRADO("castrado", "Castrado");

    companion object {
        fun desde(clave: String?): Sexo? = entries.firstOrNull { it.clave == clave?.trim()?.lowercase() }
    }
}

enum class EstadoCaballo(val clave: String) {
    ACTIVO("activo"),
    LESIONADO("lesionado"),
    DESCANSANDO("descansando"),
    RETIRADO("retirado");

    companion object {
        fun desde(clave: String?): EstadoCaballo =
            entries.firstOrNull { it.clave == clave?.trim()?.lowercase() } ?: ACTIVO
    }
}

data class Caballo(
    val id: String,
    val corralId: String?,
    val nombre: String,
    val sexo: Sexo?,
    val edad: Int?,
    val pelaje: String?,
    val padre: String?,
    val madre: String?,
    val estado: EstadoCaballo,
    val fotoUrl: String?,
    /** Colores de la chaquetilla con que corre. */
    val colores: String? = null,
)

/**
 * [pesoCarga] son las unidades de carga por sesión a esfuerzo medio (3).
 * Supuesto de la demo, a validar con el preparador.
 */
enum class TipoTrabajo(val clave: String, val etiqueta: String, val pesoCarga: Double) {
    VAREO("vareo", "Vareo", 1.0),
    TROTE("trote", "Trote", 2.0),
    GALOPE_SUAVE("galope_suave", "Galope suave", 3.0),
    GALOPE_FUERTE("galope_fuerte", "Galope fuerte", 4.0),
    APRONTE("apronte", "Apronte", 5.0),
    HUINCHADA("huinchada", "Huinchada", 5.0),
    NADO("nado", "Nado", 1.5),
    CAMINADORA("caminadora", "Caminadora", 1.0),
    DESCANSO("descanso", "Descanso", 0.0);

    val esCronometrado: Boolean get() = this == APRONTE || this == HUINCHADA

    companion object {
        fun desde(clave: String?): TipoTrabajo? = entries.firstOrNull { it.clave == clave?.trim()?.lowercase() }
    }
}

data class Sesion(
    val id: String,
    val caballoId: String,
    val fecha: LocalDate,
    val tipo: TipoTrabajo,
    val distanciaM: Int?,
    val tiempoSeg: Double?,
    val vareador: String? = null,
    val pista: String? = null,
    val estadoPista: String? = null,
    val esfuerzo: Int? = null,
    val notas: String? = null,
    val pendiente: Boolean = false,
)

enum class TipoMedicion(val clave: String) {
    PESO_KG("peso_kg"),
    TEMPERATURA("temperatura"),
    FC_REPOSO("fc_reposo");

    companion object {
        fun desde(clave: String?): TipoMedicion? = entries.firstOrNull { it.clave == clave?.trim()?.lowercase() }
    }
}

data class Medicion(
    val id: String,
    val caballoId: String,
    val fecha: LocalDate,
    val tipo: TipoMedicion,
    val valor: Double,
    /** Registrada a mano en el teléfono. */
    val pendiente: Boolean = false,
)

enum class Visibilidad(val clave: String) {
    COMPLETA("completa"),
    RESUMIDA("resumida");

    companion object {
        fun desde(clave: String?): Visibilidad =
            entries.firstOrNull { it.clave == clave?.trim()?.lowercase() } ?: RESUMIDA
    }
}

data class Tratamiento(
    val id: String,
    val caballoId: String,
    val fecha: LocalDate,
    val producto: String,
    val via: String?,
    val veterinario: String?,
    val diasRetiro: Int,
    val visibilidad: Visibilidad,
)

data class Carrera(
    val id: String,
    val hipodromo: String,
    val fecha: LocalDate,
    val hora: LocalTime?,
    val numero: Int?,
    val distanciaM: Int,
    val superficie: String?,
    val condicionPista: String?,
    val serie: String?,
    val premio: Long?,
)

data class Participacion(
    val id: String,
    val carreraId: String,
    val caballoId: String,
    val numero: Int?,
    val jinete: String?,
    val pesoKg: Double?,
    val posicion: Int?,
    val tiempoSeg: Double?,
    val dividendo: Double?,
)

data class Parcial(
    val participacionId: String,
    val metros: Int,
    val tiempoAcumSeg: Double,
    val velocidadMs: Double,
)

data class Propietario(
    val id: String,
    val nombre: String,
    val email: String?,
    val telefono: String?,
)

data class Copropiedad(val propietario: Propietario, val porcentaje: Double)

/** Una carrera del caballo, programada o corrida. */
data class CarreraDelCaballo(
    val carrera: Carrera,
    val participacion: Participacion,
    val caballo: Caballo,
    /** Cuántos corrieron (o están inscritos). Nulo si no se sabe. */
    val corredores: Int? = null,
)
