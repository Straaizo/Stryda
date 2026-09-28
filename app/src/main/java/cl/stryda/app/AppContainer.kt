package cl.stryda.app

import android.content.Context
import cl.stryda.app.data.local.CargadorSeed
import cl.stryda.app.data.local.Preferencias
import cl.stryda.app.data.local.StrydaDatabase
import cl.stryda.app.data.remote.SheetsClient
import cl.stryda.app.data.repository.AjustesRepository
import cl.stryda.app.data.repository.CaballoRepository
import cl.stryda.app.data.repository.CarreraRepository
import cl.stryda.app.data.repository.CorralRepository
import cl.stryda.app.data.repository.DatosRepository
import cl.stryda.app.data.repository.SaludRepository
import cl.stryda.app.data.repository.SesionRepository
import cl.stryda.app.domain.Estimador
import cl.stryda.app.domain.EstimadorEstadistico
import cl.stryda.app.domain.Reloj
import cl.stryda.app.domain.RelojSistema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Inyección de dependencias manual: un solo lugar donde se arma el grafo. */
class AppContainer(context: Context, val reloj: Reloj = RelojSistema) {
    /** Trabajo que no debe morir con una pantalla (actualización oportunista, guardados). */
    val scopeApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val db = StrydaDatabase.crear(context)
    private val preferencias = Preferencias(context)

    val datos = DatosRepository(
        db = db,
        cargadorSeed = CargadorSeed(context, db, preferencias, reloj),
        remota = SheetsClient(BuildConfig.SHEET_ID),
        preferencias = preferencias,
        reloj = reloj,
    )
    val corrales = CorralRepository(db.corralDao())
    val caballos = CaballoRepository(db.caballoDao(), db.cambioDao())
    val sesiones = SesionRepository(db.sesionDao())
    val salud = SaludRepository(db.saludDao())
    val carreras = CarreraRepository(db.carreraDao())
    val ajustes = AjustesRepository(preferencias)

    /** Se reemplaza por un modelo entrenado cuando haya datos reales suficientes. */
    val estimador: Estimador = EstimadorEstadistico
}
