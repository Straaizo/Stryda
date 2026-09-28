package cl.stryda.app.ui.screens.caballo

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.Carrera
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Corral
import cl.stryda.app.domain.Elegibilidad
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.Medicion
import cl.stryda.app.domain.Participacion
import cl.stryda.app.domain.ResultadoEstimacion
import cl.stryda.app.domain.Sexo
import cl.stryda.app.domain.TipoMedicion
import cl.stryda.app.domain.jinetesDelCaballo
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FichaPdfTest {

    private val hoy = LocalDate.of(2026, 9, 25)
    private val caballo = Caballo(
        "cg-sos-de-boca", "c01", "Sos de Boca", Sexo.MACHO, 5, "Alazán", "Robert Bruce", null,
        EstadoCaballo.ACTIVO, null, colores = "Naranjo franja y puños dorado",
    )

    private fun carrera(dias: Long, pos: Int?) = CarreraDelCaballo(
        Carrera("c$dias", "Club Hípico de Santiago", hoy.minusDays(dias), null, 9, 1200, "pasto", null, null, null),
        Participacion("p$dias", "c$dias", caballo.id, 2, "Seb. Gonzalez", 55.0, pos, if (pos != null) 70.2 else null, null),
        caballo, corredores = 12,
    )

    @Test
    fun generaUnPdfLegibleConMuchasCarrerasSinCaerse() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        // 60 carreras obligan a pasar de página: prueba la paginación
        val carreras = (1..60).map { carrera(it * 7L, (it % 12) + 1) }
        val pesos = (1..10).map { Medicion("m$it", caballo.id, hoy.minusDays(it * 14L), TipoMedicion.PESO_KG, 460.0 + it) }
        val d = FichaContenido(
            caballo = caballo, indicador = IndicadorEstado.EntrenaNormal, elegibilidad = Elegibilidad.Apto,
            peso = null, sesionMasRapida = null, peak = null, ultimaCarrera = carreras.first(), diasDesdeUltimaCarrera = 7,
            proximaCarrera = null, estimacion = ResultadoEstimacion.Insuficiente(2, 3), hoy = hoy,
            pesos = pesos.sortedBy { it.fecha }, carreras = carreras, jinetes = jinetesDelCaballo(carreras, hoy),
            corral = Corral("c01", "Corral Cristóbal González", "Cristóbal González M.", "Club Hípico de Santiago"),
        )

        val archivo = FichaPdf(contexto).generar(d)

        assertTrue(archivo.length() > 1_000)
        ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { assertTrue("se esperaban al menos 2 páginas", it.pageCount >= 2) }
        }
    }
}
