package cl.stryda.app.domain

import java.time.LocalDate

/** Pesajes del caballo, del más antiguo al más reciente. */
fun historialPeso(mediciones: List<Medicion>): List<Medicion> =
    mediciones.filter { it.tipo == TipoMedicion.PESO_KG }.sortedBy { it.fecha }

/** Peso que tenía el día de una carrera corrida, junto a su resultado. */
data class PesoEnCarrera(
    val fecha: LocalDate,
    val kg: Double,
    val posicion: Int,
    val corredores: Int?,
    val distanciaM: Int,
)

/** Cruza pesajes con carreras del mismo día. Es lo que permite ver con qué peso corre mejor. */
fun pesosEnCarreras(mediciones: List<Medicion>, carreras: List<CarreraDelCaballo>): List<PesoEnCarrera> {
    val pesoPorFecha = historialPeso(mediciones).associateBy { it.fecha }
    return carreras
        .mapNotNull { c ->
            val posicion = c.participacion.posicion ?: return@mapNotNull null
            val peso = pesoPorFecha[c.carrera.fecha] ?: return@mapNotNull null
            PesoEnCarrera(c.carrera.fecha, peso.valor, posicion, c.corredores, c.carrera.distanciaM)
        }
        .sortedBy { it.fecha }
}

/** Su mejor resultado con peso conocido. A igual posición, el más reciente. */
fun mejorCarreraConPeso(pesos: List<PesoEnCarrera>): PesoEnCarrera? =
    pesos.minWithOrNull(compareBy<PesoEnCarrera> { it.posicion }.thenByDescending { it.fecha })

/** Rango de peso en la ventana indicada. `null` con menos de dos pesajes. */
fun rangoPeso(mediciones: List<Medicion>, hoy: LocalDate, dias: Long = 180): ClosedFloatingPointRange<Double>? {
    val valores = historialPeso(mediciones)
        .filter { !it.fecha.isBefore(hoy.minusDays(dias)) && !it.fecha.isAfter(hoy) }
        .map { it.valor }
    if (valores.size < 2) return null
    return valores.min()..valores.max()
}
