package cl.stryda.app.domain

import java.time.LocalDate

val HOY: LocalDate = LocalDate.of(2026, 9, 25) // viernes

private var contador = 0

fun sesion(
    fecha: LocalDate,
    tipo: TipoTrabajo = TipoTrabajo.GALOPE_SUAVE,
    distanciaM: Int? = null,
    tiempoSeg: Double? = null,
    esfuerzo: Int? = null,
    caballoId: String = "k01",
) = Sesion("s${contador++}", caballoId, fecha, tipo, distanciaM, tiempoSeg, esfuerzo = esfuerzo)

fun peso(fecha: LocalDate, kg: Double) = Medicion("m${contador++}", "k01", fecha, TipoMedicion.PESO_KG, kg)

fun tratamiento(fecha: LocalDate, diasRetiro: Int) =
    Tratamiento("t${contador++}", "k01", fecha, "Producto X", "oral", "Dra. Prueba", diasRetiro, Visibilidad.RESUMIDA)

fun caballo(estado: EstadoCaballo = EstadoCaballo.ACTIVO) =
    Caballo("k01", "c01", "Gran Retamo", Sexo.MACHO, 4, null, null, null, estado, null)

/** Una sesión diaria de [tipo] entre [desde] y [hasta], ambos inclusive. */
fun sesionesDiarias(desde: LocalDate, hasta: LocalDate, tipo: TipoTrabajo = TipoTrabajo.GALOPE_SUAVE): List<Sesion> =
    generateSequence(desde) { it.plusDays(1) }.takeWhile { !it.isAfter(hasta) }.map { sesion(it, tipo) }.toList()
