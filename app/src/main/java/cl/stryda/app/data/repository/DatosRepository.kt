package cl.stryda.app.data.repository

import cl.stryda.app.data.local.CargadorSeed
import cl.stryda.app.data.local.EstadoDatos
import cl.stryda.app.data.local.OrigenDatos
import cl.stryda.app.data.local.Preferencias
import cl.stryda.app.data.local.StrydaDatabase
import cl.stryda.app.data.remote.FuenteRemota
import cl.stryda.app.domain.Reloj
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

sealed interface ResultadoActualizacion {
    data object Actualizado : ResultadoActualizacion
    data object SinPlanilla : ResultadoActualizacion
    data object SinSenal : ResultadoActualizacion
}

/** Coordina las tres capas: seed local, Room y actualización oportunista desde la planilla. */
class DatosRepository(
    private val db: StrydaDatabase,
    private val cargadorSeed: CargadorSeed,
    private val remota: FuenteRemota,
    private val preferencias: Preferencias,
    private val reloj: Reloj,
) {
    val estado: Flow<EstadoDatos> = preferencias.estadoDatos

    suspend fun prepararDatosLocales() = cargadorSeed.cargarSiVacio()

    /** Nunca lanza: si no hay señal la app sigue con lo que tiene en Room. */
    suspend fun actualizarDesdeRemoto(): ResultadoActualizacion {
        if (!remota.configurada) return ResultadoActualizacion.SinPlanilla
        return try {
            db.catalogoDao().reemplazar(remota.descargarCatalogo())
            preferencias.registrarActualizacion(reloj.ahora(), OrigenDatos.PLANILLA)
            ResultadoActualizacion.Actualizado
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ResultadoActualizacion.SinSenal
        }
    }
}
