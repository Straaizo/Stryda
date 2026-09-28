package cl.stryda.app.data.local

import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.CambioCaballo
import cl.stryda.app.domain.CampoCaballo
import cl.stryda.app.domain.Carrera
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Corral
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.Medicion
import cl.stryda.app.domain.Parcial
import cl.stryda.app.domain.Participacion
import cl.stryda.app.domain.Sesion
import cl.stryda.app.domain.Sexo
import cl.stryda.app.domain.TipoMedicion
import cl.stryda.app.domain.TipoTrabajo
import cl.stryda.app.domain.Tratamiento
import cl.stryda.app.domain.Visibilidad
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeParseException

fun CorralEntity.aDominio() = Corral(id, nombre, preparador, hipodromo)

fun CaballoEntity.aDominio() = Caballo(
    id = id,
    corralId = corralId,
    nombre = nombre,
    sexo = Sexo.desde(sexo),
    edad = edad,
    pelaje = pelaje,
    padre = padre,
    madre = madre,
    estado = EstadoCaballo.desde(estado),
    fotoUrl = fotoUrl?.takeIf { it.isNotBlank() },
    colores = colores?.takeIf { it.isNotBlank() },
)

/** Filas con un tipo desconocido se descartan en vez de romper la pantalla. */
fun SesionEntity.aDominio(): Sesion? = TipoTrabajo.desde(tipo)?.let {
    Sesion(id, caballoId, fecha, it, distanciaM, tiempoSeg, vareador, pista, estadoPista, esfuerzo, notas, pendiente)
}

fun Sesion.aEntidad() = SesionEntity(
    id, caballoId, fecha, tipo.clave, distanciaM, tiempoSeg, vareador, pista, estadoPista, esfuerzo, notas, pendiente,
)

fun MedicionEntity.aDominio(): Medicion? = TipoMedicion.desde(tipo)?.let { Medicion(id, caballoId, fecha, it, valor, pendiente) }

fun Medicion.aEntidad() = MedicionEntity(id, caballoId, fecha, tipo.clave, valor, pendiente)

fun CambioCaballoEntity.aDominio(): CambioCaballo? = CampoCaballo.desde(campo)?.let {
    CambioCaballo(id, caballoId, Instant.ofEpochMilli(instanteMs), it, valorAnterior, valorNuevo, nota)
}

fun CambioCaballo.aEntidad() = CambioCaballoEntity(
    id, caballoId, instante.toEpochMilli(), campo.clave, valorAnterior, valorNuevo, nota,
)

fun TratamientoEntity.aDominio() = Tratamiento(
    id, caballoId, fecha, producto, via, veterinario, diasRetiro, Visibilidad.desde(visibilidad),
)

fun CarreraEntity.aDominio() = Carrera(
    id, hipodromo, fecha, hora?.let(::parsearHora), numero, distanciaM, superficie, condicionPista, serie, premio,
)

fun ParticipacionEntity.aDominio() = Participacion(id, carreraId, caballoId, numero, jinete, pesoKg, posicion, tiempoSeg, dividendo)

fun CarreraParticipacionFila.aDominio() =
    CarreraDelCaballo(carrera.aDominio(), participacion.aDominio(), caballo.aDominio(), corredores)

fun ParcialEntity.aDominio() = Parcial(participacionId, metros, tiempoAcumSeg, velocidadMs)

private fun parsearHora(texto: String): LocalTime? = try {
    val (h, m) = texto.trim().split(":").map { it.toInt() }
    LocalTime.of(h, m)
} catch (_: RuntimeException) {
    null
} catch (_: DateTimeParseException) {
    null
}
