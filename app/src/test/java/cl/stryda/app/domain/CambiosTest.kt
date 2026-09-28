package cl.stryda.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class CambiosTest {

    private val t0 = Instant.parse("2026-09-25T09:00:00Z")
    private var n = 0

    private fun cambio(campo: CampoCaballo, nuevo: String?, minutos: Long, caballoId: String = "k01") =
        CambioCaballo("c${n++}", caballoId, t0.plusSeconds(minutos * 60), campo, null, nuevo)

    @Test
    fun `sin cambios queda el dato base`() {
        assertEquals(caballo(), aplicarCambios(caballo(), emptyList()))
    }

    @Test
    fun `gana el ultimo cambio de cada campo sin importar el orden de la lista`() {
        val cambios = listOf(
            cambio(CampoCaballo.ESTADO, "activo", 30),
            cambio(CampoCaballo.ESTADO, "lesionado", 10),
            cambio(CampoCaballo.PELAJE, "Alazán", 5),
        )
        val k = aplicarCambios(caballo(), cambios)
        assertEquals(EstadoCaballo.ACTIVO, k.estado)
        assertEquals("Alazán", k.pelaje)
    }

    @Test
    fun `completa datos que faltaban`() {
        val base = caballo().copy(sexo = null, edad = null, madre = null)
        val k = aplicarCambios(
            base,
            listOf(cambio(CampoCaballo.SEXO, "hembra", 1), cambio(CampoCaballo.EDAD, "3", 2), cambio(CampoCaballo.MADRE, "Hannah G", 3)),
        )
        assertEquals(Sexo.HEMBRA, k.sexo)
        assertEquals(3, k.edad)
        assertEquals("Hannah G", k.madre)
    }

    @Test
    fun `valor vacio borra el dato`() {
        val k = aplicarCambios(caballo().copy(pelaje = "Castaño"), listOf(cambio(CampoCaballo.PELAJE, "  ", 1)))
        assertNull(k.pelaje)
    }

    @Test
    fun `ignora cambios de otros caballos`() {
        val k = aplicarCambios(caballo(), listOf(cambio(CampoCaballo.ESTADO, "lesionado", 1, caballoId = "otro")))
        assertEquals(EstadoCaballo.ACTIVO, k.estado)
    }

    @Test
    fun `valorDe devuelve el formato que se guarda`() {
        val k = caballo().copy(estado = EstadoCaballo.DESCANSANDO, edad = 4, sexo = Sexo.CASTRADO)
        assertEquals("descansando", k.valorDe(CampoCaballo.ESTADO))
        assertEquals("4", k.valorDe(CampoCaballo.EDAD))
        assertEquals("castrado", k.valorDe(CampoCaballo.SEXO))
    }
}
