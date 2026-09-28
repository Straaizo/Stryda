package cl.stryda.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SheetsParserTest {

    private fun envolver(tabla: String) =
        "/*O_o*/\ngoogle.visualization.Query.setResponse({\"version\":\"0.6\",\"status\":\"ok\",\"table\":$tabla});"

    private val cols = """[{"id":"A","label":"id","type":"string"},{"id":"B","label":"caballo_id","type":"string"},
        {"id":"C","label":"fecha","type":"date"},{"id":"D","label":"tipo","type":"string"},
        {"id":"E","label":"tiempo_seg","type":"number"}]"""

    @Test
    fun `recorta el envoltorio y lee columnas por nombre`() {
        val t = parsearRespuestaGviz(
            envolver("""{"cols":$cols,"rows":[{"c":[{"v":"s1"},{"v":"k01"},{"v":"Date(2026,8,25)","f":"25-09-2026"},{"v":"apronte"},{"v":48.6}]}]}"""),
        )
        val f = t.filas.single()
        assertEquals("s1", f.texto("id"))
        assertEquals(LocalDate.of(2026, 9, 25), f.fecha("fecha")) // meses base 0 en gviz
        assertEquals(48.6, f.decimal("tiempo_seg")!!, 0.001)
    }

    @Test
    fun `celda vacia es null`() {
        val t = parsearRespuestaGviz(
            envolver("""{"cols":$cols,"rows":[{"c":[{"v":"s1"},{"v":"k01"},{"v":"Date(2026,8,25)"},{"v":"vareo"},null]}]}"""),
        )
        assertNull(t.filas.single().decimal("tiempo_seg"))
        val t2 = parsearRespuestaGviz(
            envolver("""{"cols":$cols,"rows":[{"c":[{"v":"s1"},{"v":"k01"},{"v":"Date(2026,8,25)"},{"v":"vareo"},{"v":null}]}]}"""),
        )
        assertNull(t2.filas.single().decimal("tiempo_seg"))
    }

    @Test
    fun `numero con coma decimal escrito como texto`() {
        val t = parsearRespuestaGviz(
            envolver("""{"cols":$cols,"rows":[{"c":[{"v":"s1"},{"v":"k01"},{"v":"25-09-2026"},{"v":"apronte"},{"v":"48,6"}]}]}"""),
        )
        assertEquals(48.6, t.filas.single().decimal("tiempo_seg")!!, 0.001)
    }

    @Test
    fun `fecha mal formada es null y la fila se descarta al mapear`() {
        val t = parsearRespuestaGviz(
            envolver("""{"cols":$cols,"rows":[{"c":[{"v":"s1"},{"v":"k01"},{"v":"31/02/2026"},{"v":"apronte"},{"v":50}]},
                {"c":[{"v":"s2"},{"v":"k01"},{"v":"ayer"},{"v":"apronte"},{"v":50}]}]}"""),
        )
        assertTrue(t.filas.all { it.fecha("fecha") == null })
        assertTrue(mapearCatalogo(mapOf(Hojas.SESIONES to t)).sesiones.isEmpty())
    }

    @Test
    fun `fila incompleta se completa con null y sin id se descarta`() {
        val t = parsearRespuestaGviz(
            envolver("""{"cols":$cols,"rows":[{"c":[{"v":"s1"},{"v":"k01"},{"v":"2026-09-20"},{"v":"trote"}]},
                {"c":[null,{"v":"k01"},{"v":"2026-09-21"},{"v":"trote"}]}]}"""),
        )
        assertNull(t.filas.first().decimal("tiempo_seg"))
        val sesiones = mapearCatalogo(mapOf(Hojas.SESIONES to t)).sesiones
        assertEquals(listOf("s1"), sesiones.map { it.id })
    }

    @Test
    fun `numeros enteros no arrastran decimal`() {
        val colsEdad = """[{"id":"A","label":"id","type":"string"},{"id":"B","label":"edad","type":"number"}]"""
        val t = parsearRespuestaGviz(envolver("""{"cols":$colsEdad,"rows":[{"c":[{"v":"k01"},{"v":4.0}]}]}"""))
        assertEquals(4, t.filas.single().entero("edad"))
    }

    @Test
    fun `cabecera no detectada se toma de la primera fila`() {
        val sinEtiquetas = """[{"id":"A","label":"","type":"string"},{"id":"B","label":"","type":"string"}]"""
        val t = parsearRespuestaGviz(
            envolver("""{"cols":$sinEtiquetas,"rows":[{"c":[{"v":"id"},{"v":"nombre"}]},{"c":[{"v":"c01"},{"v":"Corral Los Aromos"}]}]}"""),
        )
        assertEquals("Corral Los Aromos", t.filas.single().texto("nombre"))
    }

    @Test
    fun `hoja vacia devuelve cero filas sin romper`() {
        val t = parsearRespuestaGviz(envolver("""{"cols":$cols,"rows":[]}"""))
        assertTrue(t.filas.isEmpty())
        assertTrue(mapearCatalogo(mapOf(Hojas.CORRALES to t)).corrales.isEmpty())
    }

    @Test(expected = RespuestaSheetsInvalida::class)
    fun `respuesta sin json lanza error controlado`() {
        parsearRespuestaGviz("<html>Inicia sesión</html>")
    }
}
