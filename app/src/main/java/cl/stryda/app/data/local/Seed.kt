package cl.stryda.app.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Formato de `assets/seed.json`. Mismas columnas que la planilla. */
@Serializable
data class SeedDto(
    val version: Int = 1,
    @SerialName("fecha_referencia") val fechaReferencia: String,
    val corrales: List<CorralDto>,
    val caballos: List<CaballoDto>,
    val propietarios: List<PropietarioDto>,
    @SerialName("caballo_propietario") val caballoPropietario: List<CaballoPropietarioDto>,
    val sesiones: List<SesionDto>,
    val mediciones: List<MedicionDto>,
    val tratamientos: List<TratamientoDto>,
    val carreras: List<CarreraDto>,
    val participaciones: List<ParticipacionDto>,
    val parciales: List<ParcialDto>,
)

@Serializable
data class CorralDto(val id: String, val nombre: String, val preparador: String, val hipodromo: String)

@Serializable
data class CaballoDto(
    val id: String,
    @SerialName("corral_id") val corralId: String? = null,
    val nombre: String,
    val sexo: String? = null,
    val edad: Int? = null,
    val pelaje: String? = null,
    val padre: String? = null,
    val madre: String? = null,
    val estado: String = "activo",
    @SerialName("foto_url") val fotoUrl: String? = null,
    val colores: String? = null,
)

@Serializable
data class PropietarioDto(val id: String, val nombre: String, val email: String? = null, val telefono: String? = null)

@Serializable
data class CaballoPropietarioDto(
    @SerialName("caballo_id") val caballoId: String,
    @SerialName("propietario_id") val propietarioId: String,
    val porcentaje: Double,
)

@Serializable
data class SesionDto(
    val id: String,
    @SerialName("caballo_id") val caballoId: String,
    val fecha: String,
    val tipo: String,
    @SerialName("distancia_m") val distanciaM: Int? = null,
    @SerialName("tiempo_seg") val tiempoSeg: Double? = null,
    val vareador: String? = null,
    val pista: String? = null,
    @SerialName("estado_pista") val estadoPista: String? = null,
    val esfuerzo: Int? = null,
    val notas: String? = null,
)

@Serializable
data class MedicionDto(
    val id: String,
    @SerialName("caballo_id") val caballoId: String,
    val fecha: String,
    val tipo: String,
    val valor: Double,
)

@Serializable
data class TratamientoDto(
    val id: String,
    @SerialName("caballo_id") val caballoId: String,
    val fecha: String,
    val producto: String,
    val via: String? = null,
    val veterinario: String? = null,
    @SerialName("dias_retiro") val diasRetiro: Int = 0,
    val visibilidad: String = "resumida",
)

@Serializable
data class CarreraDto(
    val id: String,
    val hipodromo: String,
    val fecha: String,
    val hora: String? = null,
    val numero: Int? = null,
    @SerialName("distancia_m") val distanciaM: Int,
    val superficie: String? = null,
    @SerialName("condicion_pista") val condicionPista: String? = null,
    val serie: String? = null,
    val premio: Long? = null,
)

@Serializable
data class ParticipacionDto(
    val id: String,
    @SerialName("carrera_id") val carreraId: String,
    @SerialName("caballo_id") val caballoId: String,
    val numero: Int? = null,
    val jinete: String? = null,
    @SerialName("peso_kg") val pesoKg: Double? = null,
    val posicion: Int? = null,
    @SerialName("tiempo_seg") val tiempoSeg: Double? = null,
    val dividendo: Double? = null,
)

@Serializable
data class ParcialDto(
    @SerialName("participacion_id") val participacionId: String,
    val metros: Int,
    @SerialName("tiempo_acum_seg") val tiempoAcumSeg: Double,
    @SerialName("velocidad_ms") val velocidadMs: Double,
)

private val json = Json { ignoreUnknownKeys = true }

fun parsearSeed(texto: String): SeedDto = json.decodeFromString(SeedDto.serializer(), texto)

private val regexVersion = Regex(""""version"\s*:\s*(\d+)""")

/** Lee la versión del encabezado sin parsear todo el archivo. Sin versión = 1. */
fun versionDelSeed(texto: String): Int =
    regexVersion.find(texto.take(200))?.groupValues?.get(1)?.toIntOrNull() ?: 1

/**
 * Convierte el seed a entidades desplazando todas las fechas para que [SeedDto.fechaReferencia]
 * caiga en [hoy]. Así "corre en 2 días" sigue siendo cierto el día que se muestra la demo.
 */
fun SeedDto.aCatalogo(hoy: LocalDate): Catalogo {
    val desfase = ChronoUnit.DAYS.between(LocalDate.parse(fechaReferencia), hoy)
    fun f(fecha: String): LocalDate = LocalDate.parse(fecha).plusDays(desfase)
    return Catalogo(
        corrales = corrales.map { CorralEntity(it.id, it.nombre, it.preparador, it.hipodromo) },
        caballos = caballos.map {
            CaballoEntity(it.id, it.corralId, it.nombre, it.sexo, it.edad, it.pelaje, it.padre, it.madre, it.estado, it.fotoUrl, it.colores)
        },
        propietarios = propietarios.map { PropietarioEntity(it.id, it.nombre, it.email, it.telefono) },
        caballoPropietario = caballoPropietario.map { CaballoPropietarioEntity(it.caballoId, it.propietarioId, it.porcentaje) },
        sesiones = sesiones.map {
            SesionEntity(
                it.id, it.caballoId, f(it.fecha), it.tipo, it.distanciaM, it.tiempoSeg,
                it.vareador, it.pista, it.estadoPista, it.esfuerzo, it.notas,
            )
        },
        mediciones = mediciones.map { MedicionEntity(it.id, it.caballoId, f(it.fecha), it.tipo, it.valor) },
        tratamientos = tratamientos.map {
            TratamientoEntity(it.id, it.caballoId, f(it.fecha), it.producto, it.via, it.veterinario, it.diasRetiro, it.visibilidad)
        },
        carreras = carreras.map {
            CarreraEntity(
                it.id, it.hipodromo, f(it.fecha), it.hora, it.numero, it.distanciaM,
                it.superficie, it.condicionPista, it.serie, it.premio,
            )
        },
        participaciones = participaciones.map {
            ParticipacionEntity(it.id, it.carreraId, it.caballoId, it.numero, it.jinete, it.pesoKg, it.posicion, it.tiempoSeg, it.dividendo)
        },
        parciales = parciales.map { ParcialEntity(it.participacionId, it.metros, it.tiempoAcumSeg, it.velocidadMs) },
    )
}
