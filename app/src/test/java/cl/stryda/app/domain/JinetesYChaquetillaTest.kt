package cl.stryda.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JinetesYChaquetillaTest {

    private var n = 0

    private fun carrera(diasAtras: Long, jinete: String?, posicion: Int?): CarreraDelCaballo {
        val id = "car${n++}"
        return CarreraDelCaballo(
            Carrera(id, "Club Hípico de Santiago", HOY.minusDays(diasAtras), null, 5, 1200, null, null, null, null),
            Participacion("p$id", id, "k01", 4, jinete, 56.0, posicion, null, null),
            caballo(),
        )
    }

    @Test
    fun `ordena por cantidad de montas y a igualdad por la mas reciente`() {
        val j = jinetesDelCaballo(
            listOf(
                carrera(40, "A. Apablaza", 4),
                carrera(30, "Seb. Gonzalez", 1),
                carrera(20, "A. Apablaza", 7),
                carrera(10, "K. Espina", 2),
            ),
            HOY,
        )
        assertEquals(listOf("A. Apablaza", "K. Espina", "Seb. Gonzalez"), j.map { it.nombre })
        assertEquals(2, j.first().montas)
        assertEquals(4, j.first().mejorPosicion)
    }

    @Test
    fun `la carrera de hoy sin resultado no cuenta como monta y sin jinete se ignora`() {
        val j = jinetesDelCaballo(listOf(carrera(0, "R. Cisternas", null), carrera(5, null, 3)), HOY)
        assertEquals(emptyList<JineteDelCaballo>(), j)
    }

    @Test
    fun `carrera pasada sin resultado cargado igual cuenta la monta`() {
        val j = jinetesDelCaballo(listOf(carrera(4, "B. Troncoso", null)), HOY).single()
        assertEquals(1, j.montas)
        assertNull(j.mejorPosicion)
    }

    @Test
    fun `interpreta chaquetillas de los programas`() {
        assertEquals(Chaquetilla("naranjo", "dorado", Diseno.FRANJA, punos = true), interpretarChaquetilla("Naranjo franja y puños dorado"))
        assertEquals(Chaquetilla("verde", "blanco", Diseno.MANGAS, punos = false), interpretarChaquetilla("Verde mangas blanco"))
        assertEquals(Chaquetilla("negro", "naranjo", Diseno.CRUZ, punos = false), interpretarChaquetilla("Negro cruz naranjo"))
        assertEquals(Chaquetilla("turquesa", null, Diseno.LUNARES, punos = false), interpretarChaquetilla("Turquesa lunares"))
        assertEquals(Chaquetilla("blanco", null, Diseno.LISA, punos = false), interpretarChaquetilla("Blanco"))
        assertEquals(Chaquetilla("turquesa", "rojo", Diseno.HOMBRERAS, punos = false), interpretarChaquetilla("Turquesa hombreras y gorra rojo"))
    }

    @Test
    fun `sin colores reconocibles no inventa chaquetilla`() {
        assertNull(interpretarChaquetilla(null))
        assertNull(interpretarChaquetilla("Franja y mangas"))
    }
}
