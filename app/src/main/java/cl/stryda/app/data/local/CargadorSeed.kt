package cl.stryda.app.data.local

import android.content.Context
import cl.stryda.app.domain.Reloj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Carga `assets/seed.json` si Room está vacío (primer arranque) o si trae una versión nueva
 * y el teléfono sigue con datos de prueba. Nunca necesita red.
 */
class CargadorSeed(
    private val context: Context,
    private val db: StrydaDatabase,
    private val preferencias: Preferencias,
    private val reloj: Reloj,
) {
    suspend fun cargarSiVacio() = withContext(Dispatchers.IO) {
        val texto = context.assets.open(ARCHIVO).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val vacio = db.corralDao().contar() == 0
        val seedNuevo = preferencias.estadoDatos.first().origen == OrigenDatos.PRUEBA &&
            preferencias.versionSeed.first() != versionDelSeed(texto)
        if (!vacio && !seedNuevo) return@withContext
        // El parseo completo solo ocurre cuando de verdad hay que cargar
        val seed = parsearSeed(texto)
        db.catalogoDao().reemplazar(seed.aCatalogo(reloj.hoy()))
        preferencias.guardarVersionSeed(seed.version)
        preferencias.registrarActualizacion(reloj.ahora(), OrigenDatos.PRUEBA)
    }

    private companion object {
        const val ARCHIVO = "seed.json"
    }
}
