package cl.stryda.app.ui.screens.caballo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.stryda.app.AppContainer
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.CambioCaballo
import cl.stryda.app.domain.CampoCaballo
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Elegibilidad
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.Medicion
import cl.stryda.app.domain.Parcial
import cl.stryda.app.domain.PesoActual
import cl.stryda.app.domain.ResultadoEstimacion
import cl.stryda.app.domain.Sesion
import cl.stryda.app.domain.TipoMedicion
import cl.stryda.app.domain.TipoTrabajo
import cl.stryda.app.domain.PesoEnCarrera
import cl.stryda.app.domain.Corral
import cl.stryda.app.domain.JineteDelCaballo
import cl.stryda.app.domain.jinetesDelCaballo
import cl.stryda.app.domain.historialPeso
import cl.stryda.app.domain.pesosEnCarreras
import cl.stryda.app.domain.rangoPeso
import cl.stryda.app.domain.Tratamiento
import cl.stryda.app.domain.diasDesdeUltimaCarrera
import cl.stryda.app.domain.elegibilidad
import cl.stryda.app.domain.indicadorEstado
import cl.stryda.app.domain.peakEnCarrera
import cl.stryda.app.domain.pesoActual
import cl.stryda.app.domain.proximaCarrera
import cl.stryda.app.domain.sesionMasRapida
import cl.stryda.app.domain.valorDe
import cl.stryda.app.ui.UiState
import cl.stryda.app.ui.components.decimal
import cl.stryda.app.ui.components.tiempoCarrera
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Peak de velocidad en carrera junto a la carrera donde se midió. */
data class PeakCarrera(val parcial: Parcial, val carrera: CarreraDelCaballo?)

/** Lo que el preparador guardó a mano en este teléfono. Es la base para entrenar un modelo del corral. */
sealed interface RegistroTelefono {
    val fecha: LocalDate

    data class Cambio(val cambio: CambioCaballo, override val fecha: LocalDate) : RegistroTelefono
    data class Medida(val medicion: Medicion) : RegistroTelefono {
        override val fecha: LocalDate get() = medicion.fecha
    }
}

data class FichaContenido(
    val caballo: Caballo,
    val indicador: IndicadorEstado,
    val elegibilidad: Elegibilidad,
    val peso: PesoActual?,
    val sesionMasRapida: Sesion?,
    val peak: PeakCarrera?,
    val ultimaCarrera: CarreraDelCaballo?,
    val diasDesdeUltimaCarrera: Long?,
    val proximaCarrera: CarreraDelCaballo?,
    val estimacion: ResultadoEstimacion,
    val hoy: LocalDate,
    val ultimoCambioEstado: CambioCaballo? = null,
    val registros: List<RegistroTelefono> = emptyList(),
    /** Pesajes del más antiguo al más reciente. */
    val pesos: List<Medicion> = emptyList(),
    val pesosEnCarreras: List<PesoEnCarrera> = emptyList(),
    val rangoPeso: ClosedFloatingPointRange<Double>? = null,
    /** Sus 5 trabajos cronometrados más rápidos. */
    val trabajosRapidos: List<Sesion> = emptyList(),
    /** Todas sus carreras, de la más reciente a la más antigua. */
    val carreras: List<CarreraDelCaballo> = emptyList(),
    val jinetes: List<JineteDelCaballo> = emptyList(),
    val corral: Corral? = null,
)

/** Aviso tras guardar, con su deshacer. */
data class AvisoGuardado(val mensaje: String, val deshacer: suspend () -> Unit)

class FichaViewModel(private val caballoId: String, private val c: AppContainer) : ViewModel() {

    private val hoy = c.reloj.hoy()
    private val zona = java.time.ZoneId.systemDefault()

    private val _avisos = Channel<AvisoGuardado>(Channel.BUFFERED)
    val avisos: Flow<AvisoGuardado> = _avisos.receiveAsFlow()

    /** Evita que un doble toque guarde dos veces. */
    private var guardando = false

    private val base = combine(
        c.caballos.observar(caballoId),
        c.sesiones.observarDelCaballo(caballoId),
        c.salud.observarMediciones(caballoId),
        c.salud.observarTratamientos(caballoId),
        c.carreras.observarDelCaballo(caballoId),
    ) { caballo, sesiones, mediciones, tratamientos, carreras ->
        Datos(caballo, sesiones, mediciones, tratamientos, carreras)
    }

    val estado: StateFlow<UiState<FichaContenido>> = combine(
        base,
        c.carreras.observarParcialesDelCaballo(caballoId),
        c.caballos.observarCambios(caballoId),
        c.corrales.observarCorrales(),
    ) { d, parciales, cambios, corrales ->
        val caballo = d.caballo ?: return@combine UiState.Vacio
        val proxima = proximaCarrera(d.carreras, hoy)
        val corridas = d.carreras.filter { it.participacion.posicion != null && !it.carrera.fecha.isAfter(hoy) }
        val ultima = corridas.maxByOrNull { it.carrera.fecha }
        // Se estima para la distancia de la próxima carrera; si no hay, para la última que corrió
        val distanciaObjetivo = proxima?.carrera?.distanciaM ?: ultima?.carrera?.distanciaM ?: 0
        val peak = peakEnCarrera(parciales)?.let { p ->
            PeakCarrera(p, d.carreras.firstOrNull { it.participacion.id == p.participacionId })
        }
        val registros = (
            cambios.map { RegistroTelefono.Cambio(it, it.instante.atZone(zona).toLocalDate()) } +
                d.mediciones.filter { it.pendiente }.map { RegistroTelefono.Medida(it) }
            ).sortedWith(
            compareByDescending<RegistroTelefono> { it.fecha }
                .thenByDescending { (it as? RegistroTelefono.Cambio)?.cambio?.instante?.toEpochMilli() ?: 0L },
        )

        UiState.Contenido(
            FichaContenido(
                caballo = caballo,
                indicador = indicadorEstado(caballo, proxima?.carrera?.fecha, d.tratamientos, d.sesiones, hoy),
                elegibilidad = elegibilidad(caballo, d.tratamientos, hoy),
                peso = pesoActual(d.mediciones, hoy),
                sesionMasRapida = sesionMasRapida(d.sesiones),
                peak = peak,
                ultimaCarrera = ultima,
                diasDesdeUltimaCarrera = diasDesdeUltimaCarrera(d.carreras, hoy),
                proximaCarrera = proxima,
                estimacion = c.estimador.estimar(d.carreras, distanciaObjetivo, hoy),
                hoy = hoy,
                ultimoCambioEstado = cambios.lastOrNull { it.campo == CampoCaballo.ESTADO },
                registros = registros,
                pesos = historialPeso(d.mediciones),
                pesosEnCarreras = pesosEnCarreras(d.mediciones, d.carreras),
                rangoPeso = rangoPeso(d.mediciones, hoy),
                trabajosRapidos = d.sesiones
                    .filter { (it.distanciaM ?: 0) > 0 && (it.tiempoSeg ?: 0.0) > 0 }
                    .sortedByDescending { it.distanciaM!! / it.tiempoSeg!! }
                    .take(5),
                carreras = d.carreras.sortedByDescending { it.carrera.fecha },
                jinetes = jinetesDelCaballo(d.carreras, hoy),
                corral = corrales.firstOrNull { it.corral.id == caballo.corralId }?.corral,
            ),
        )
    }
        .catch { emit(UiState.Error("No se pudieron leer los datos del ejemplar guardados en el teléfono.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Cargando)

    private fun caballoActual(): Caballo? = (estado.value as? UiState.Contenido)?.datos?.caballo

    fun cambiarEstado(nuevo: EstadoCaballo, nota: String?) {
        val k = caballoActual() ?: return
        if (nuevo == k.estado) return
        guardarCambios(
            listOf(CampoCaballo.ESTADO to nuevo.clave),
            nota,
            "${k.nombre}: ${etiquetaEstado(nuevo)}",
        )
    }

    fun editarDatos(nuevos: Map<CampoCaballo, String?>) {
        val k = caballoActual() ?: return
        val reales = nuevos.filter { (campo, v) -> k.valorDe(campo) != v }
        if (reales.isEmpty()) return
        val mensaje = if (reales.size == 1) "Guardado: ${reales.keys.first().etiqueta.lowercase()}" else "Guardados ${reales.size} datos"
        guardarCambios(reales.toList(), null, mensaje)
    }

    private fun guardarCambios(valores: List<Pair<CampoCaballo, String?>>, nota: String?, mensaje: String) {
        val k = caballoActual() ?: return
        if (guardando) return
        guardando = true
        val ahora = c.reloj.ahora()
        val cambios = valores.map { (campo, v) ->
            CambioCaballo(UUID.randomUUID().toString(), k.id, ahora, campo, k.valorDe(campo), v, nota)
        }
        // En el scope de la app: el guardado no se corta aunque el usuario salga de la ficha
        c.scopeApp.launch {
            try {
                c.caballos.registrarCambios(cambios)
                _avisos.send(AvisoGuardado(mensaje) { c.caballos.deshacerCambios(cambios.map { it.id }) })
            } finally {
                guardando = false
            }
        }
    }

    fun registrarMedicion(peso: Double?, temperatura: Double?, fc: Double?) {
        if (guardando) return
        val mediciones = listOfNotNull(
            peso?.let { TipoMedicion.PESO_KG to it },
            temperatura?.let { TipoMedicion.TEMPERATURA to it },
            fc?.let { TipoMedicion.FC_REPOSO to it },
        ).map { (tipo, valor) -> Medicion(UUID.randomUUID().toString(), caballoId, hoy, tipo, valor, pendiente = true) }
        if (mediciones.isEmpty()) return
        guardando = true
        val mensaje = peso?.let { "Peso guardado: ${decimal(it)} kg" } ?: "Medición guardada"
        c.scopeApp.launch {
            try {
                c.salud.registrarMediciones(mediciones)
                _avisos.send(AvisoGuardado(mensaje) { c.salud.deshacerMediciones(mediciones.map { it.id }) })
            } finally {
                guardando = false
            }
        }
    }

    fun registrarTrabajo(tipo: TipoTrabajo, distanciaM: Int, tiempoSeg: Double) {
        if (guardando) return
        guardando = true
        val sesion = Sesion(
            id = UUID.randomUUID().toString(), caballoId = caballoId, fecha = hoy, tipo = tipo,
            distanciaM = distanciaM, tiempoSeg = tiempoSeg, pendiente = true,
        )
        c.scopeApp.launch {
            try {
                c.sesiones.registrar(listOf(sesion))
                _avisos.send(
                    AvisoGuardado("${tipo.etiqueta} guardado: ${distanciaM} m en ${tiempoCarrera(tiempoSeg)}") {
                        c.sesiones.eliminar(listOf(sesion.id))
                    },
                )
            } finally {
                guardando = false
            }
        }
    }

    fun deshacer(aviso: AvisoGuardado) {
        c.scopeApp.launch { aviso.deshacer() }
    }

    private data class Datos(
        val caballo: Caballo?,
        val sesiones: List<Sesion>,
        val mediciones: List<Medicion>,
        val tratamientos: List<Tratamiento>,
        val carreras: List<CarreraDelCaballo>,
    )
}
