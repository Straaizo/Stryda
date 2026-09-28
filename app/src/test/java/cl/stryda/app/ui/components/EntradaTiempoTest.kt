package cl.stryda.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntradaTiempoTest {

    @Test
    fun `formatea mientras se escribe`() {
        assertEquals("", digitosATexto(""))
        assertEquals("0.4", digitosATexto("4"))
        assertEquals("4.8", digitosATexto("48"))
        assertEquals("48.6", digitosATexto("486"))
        assertEquals("1:23.4", digitosATexto("1234"))
        assertEquals("1:02.4", digitosATexto("1024"))
    }

    @Test
    fun `convierte a segundos`() {
        assertEquals(48.6, digitosASegundos("486")!!, 0.001)
        assertEquals(83.4, digitosASegundos("1234")!!, 0.001)
        assertEquals(62.4, digitosASegundos("1024")!!, 0.001)
        assertNull(digitosASegundos(""))
    }

    @Test
    fun `rechaza segundos imposibles con minutos`() {
        assertNull(digitosASegundos("1704")) // 1:70.4
    }
}
