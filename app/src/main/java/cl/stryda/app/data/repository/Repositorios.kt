package cl.stryda.app.data.repository

import cl.stryda.app.data.local.CaballoDao
import cl.stryda.app.data.local.CambioDao
import cl.stryda.app.data.local.CarreraDao
import cl.stryda.app.data.local.CorralDao
import cl.stryda.app.data.local.ModoTema
import cl.stryda.app.data.local.Preferencias
import cl.stryda.app.data.local.SaludDao
import cl.stryda.app.data.local.SesionDao
import cl.stryda.app.data.local.aDominio
import cl.stryda.app.data.local.aEntidad
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.CambioCaballo
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.aplicarCambios
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Corral
import cl.stryda.app.domain.CorralResumen
import cl.stryda.app.domain.Medicion
import cl.stryda.app.domain.Parcial
import cl.stryda.app.domain.Sesion
import cl.stryda.app.domain.Tratamiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class CorralRepository(private val dao: CorralDao) {
    fun observarCorrales(): Flow<List<CorralResumen>> =
        dao.observarConConteo().map { lista -> lista.map { CorralResumen(it.corral.aDominio(), it.ejemplares) } }

    fun observarCorral(id: String): Flow<Corral?> = dao.observar(id).map { it?.aDominio() }

    suspend fun corrales(): List<Corral> = dao.todos().map { it.aDominio() }
}

/** Entrega cada caballo con sus cambios manuales ya aplicados: toda la app ve el mismo valor vigente. */
class CaballoRepository(private val dao: CaballoDao, private val cambios: CambioDao) {

    private val todosLosCambios = cambios.observarTodos().map { l -> l.mapNotNull { it.aDominio() } }

    fun observarDelCorral(corralId: String): Flow<List<Caballo>> =
        combine(dao.observarDelCorral(corralId), todosLosCambios) { lista, cs ->
            val porCaballo = cs.groupBy { it.caballoId }
            lista.map { aplicarCambios(it.aDominio(), porCaballo[it.id].orEmpty()) }
                .filter { it.estado != EstadoCaballo.RETIRADO }
        }

    fun observar(id: String): Flow<Caballo?> =
        combine(dao.observar(id), observarCambios(id)) { k, cs -> k?.let { aplicarCambios(it.aDominio(), cs) } }

    /** Historial de cambios manuales del caballo, del más antiguo al más reciente. */
    fun observarCambios(caballoId: String): Flow<List<CambioCaballo>> =
        cambios.observarDelCaballo(caballoId).map { l -> l.mapNotNull { it.aDominio() } }

    suspend fun registrarCambios(lista: List<CambioCaballo>) = cambios.insertar(lista.map { it.aEntidad() })

    suspend fun deshacerCambios(ids: List<String>) = cambios.eliminar(ids)
}

class SesionRepository(private val dao: SesionDao) {
    fun observarDelCorralDesde(corralId: String, desde: LocalDate): Flow<List<Sesion>> =
        dao.observarDelCorralDesde(corralId, desde).map { lista -> lista.mapNotNull { it.aDominio() } }

    fun observarDelCaballo(caballoId: String): Flow<List<Sesion>> =
        dao.observarDelCaballo(caballoId).map { lista -> lista.mapNotNull { it.aDominio() } }

    suspend fun registrar(sesiones: List<Sesion>) = dao.insertar(sesiones.map { it.aEntidad() })

    suspend fun eliminar(ids: List<String>) = dao.eliminar(ids)
}

class SaludRepository(private val dao: SaludDao) {
    fun observarTratamientosDelCorral(corralId: String): Flow<List<Tratamiento>> =
        dao.observarTratamientosDelCorral(corralId).map { lista -> lista.map { it.aDominio() } }

    fun observarTratamientos(caballoId: String): Flow<List<Tratamiento>> =
        dao.observarTratamientos(caballoId).map { lista -> lista.map { it.aDominio() } }

    fun observarMediciones(caballoId: String): Flow<List<Medicion>> =
        dao.observarMediciones(caballoId).map { lista -> lista.mapNotNull { it.aDominio() } }

    suspend fun registrarMediciones(mediciones: List<Medicion>) =
        dao.insertarMediciones(mediciones.map { it.copy(pendiente = true).aEntidad() })

    /** Solo borra mediciones registradas en el teléfono, nunca las que vienen de la planilla. */
    suspend fun deshacerMediciones(ids: List<String>) = dao.eliminarMediciones(ids)
}

class CarreraRepository(private val dao: CarreraDao) {
    fun observarProximasDelCorral(corralId: String, desde: LocalDate): Flow<List<CarreraDelCaballo>> =
        dao.observarProximas(corralId, desde).map { lista -> lista.map { it.aDominio() } }

    /** Próximas carreras de todos los ejemplares propios (de cualquier corral). */
    fun observarProximasPropias(desde: LocalDate): Flow<List<CarreraDelCaballo>> =
        dao.observarProximas(null, desde).map { lista -> lista.map { it.aDominio() } }

    /** Todas las carreras del caballo, corridas y programadas, de la más reciente a la más antigua. */
    fun observarDelCaballo(caballoId: String): Flow<List<CarreraDelCaballo>> =
        dao.observarDelCaballo(caballoId).map { lista -> lista.map { it.aDominio() } }

    fun observarParcialesDelCaballo(caballoId: String): Flow<List<Parcial>> =
        dao.observarParcialesDelCaballo(caballoId).map { lista -> lista.map { it.aDominio() } }
}

class AjustesRepository(private val preferencias: Preferencias) {
    val modoTema: Flow<ModoTema> = preferencias.modoTema
    suspend fun guardarModoTema(modo: ModoTema) = preferencias.guardarModoTema(modo)
}
