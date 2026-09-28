package cl.stryda.app.domain

import java.time.Instant
import java.time.LocalDate

/** Fuente de "hoy". Se inyecta para que los cálculos sean verificables en pruebas. */
interface Reloj {
    fun hoy(): LocalDate
    fun ahora(): Instant
}

object RelojSistema : Reloj {
    override fun hoy(): LocalDate = LocalDate.now()
    override fun ahora(): Instant = Instant.now()
}
