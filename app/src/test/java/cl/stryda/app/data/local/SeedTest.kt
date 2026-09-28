package cl.stryda.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class SeedTest {

    private val texto by lazy {
        // Las pruebas unitarias corren con el módulo app como directorio de trabajo
        File("src/main/assets/seed.json").readText(Charsets.UTF_8)
    }
    private val seed by lazy { parsearSeed(texto) }

    @Test
    fun `desplaza las fechas para que la referencia caiga hoy`() {
        val hoy = LocalDate.of(2027, 3, 10)
        val ultimaSesion = seed.aCatalogo(hoy).sesiones.maxOf { it.fecha }
        assertEquals(hoy.minusDays(1), ultimaSesion)
    }

    @Test
    fun `es el corral real de Cristobal Gonzalez`() {
        assertEquals("Corral Cristóbal González", seed.corrales.single().nombre)
        val propios = seed.caballos.filter { it.corralId != null }
        assertTrue(propios.size >= 12)
        assertTrue(propios.any { it.nombre == "Sos de Boca" })
        assertTrue(propios.all { it.id.startsWith("cg-") })
        // Colores de la chaquetilla sin palabras cortadas por el PDF
        assertTrue(propios.all { it.colores != null })
        assertEquals("Naranjo franja y puños dorado", propios.single { it.nombre == "Sos de Boca" }.colores)
    }

    @Test
    fun `no inventa lesiones, retiros ni parciales sobre caballos reales`() {
        assertTrue(seed.caballos.all { it.estado == "activo" })
        assertTrue(seed.tratamientos.all { it.diasRetiro == 0 })
        assertTrue(seed.parciales.isEmpty())
    }

    @Test
    fun `los pesos son el peso fisico del programa oficial`() {
        assertTrue(seed.mediciones.isNotEmpty())
        assertTrue(seed.mediciones.all { it.id.startsWith("pf-") && it.tipo == "peso_kg" && it.valor in 350.0..650.0 })
    }

    @Test
    fun `mantiene los casos que la demo necesita`() {
        val hoy = LocalDate.parse(seed.fechaReferencia)
        val c = seed.aCatalogo(hoy)
        val propios = c.caballos.filter { it.corralId != null }.map { it.id }.toSet()

        // Al menos dos corren en los próximos tres días
        val proximas = c.participaciones.filter { p ->
            p.posicion == null && p.caballoId in propios &&
                c.carreras.single { it.id == p.carreraId }.fecha.let { !it.isBefore(hoy) && !it.isAfter(hoy.plusDays(3)) }
        }
        assertTrue(proximas.size >= 2)

        // Un debutante sin carreras corridas
        assertTrue(propios.any { k -> c.participaciones.none { it.caballoId == k && it.posicion != null } })

        // Uno con tres propietarios
        assertTrue(c.caballoPropietario.groupBy { it.caballoId }.values.any { it.size == 3 })

        // Rivales reales en las carreras del corral
        assertTrue(c.caballos.count { it.corralId == null } > 50)
    }

    @Test
    fun `la version del encabezado coincide con la del contenido`() {
        assertEquals(seed.version, versionDelSeed(texto))
        assertEquals(1, versionDelSeed("{\"fecha_referencia\":\"2026-09-25\"}"))
    }
}
