package cl.stryda.app.ui.screens.caballo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cl.stryda.app.domain.interpretarChaquetilla
import cl.stryda.app.ui.components.ChaquetillaIcono
import cl.stryda.app.ui.components.NumeroMandil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.stryda.app.domain.CampoCaballo
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Elegibilidad
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.ResultadoEstimacion
import cl.stryda.app.domain.Sexo
import cl.stryda.app.domain.TipoMedicion
import cl.stryda.app.domain.TipoTrabajo
import cl.stryda.app.ui.UiState
import cl.stryda.app.ui.components.CaballoGalopando
import cl.stryda.app.ui.components.CifraAnimada
import cl.stryda.app.ui.components.EstadoVacio
import cl.stryda.app.ui.components.IndicadorChip
import cl.stryda.app.ui.components.aKmH
import cl.stryda.app.ui.components.decimal
import cl.stryda.app.ui.components.fechaLarga
import cl.stryda.app.ui.components.fechaRelativaFutura
import cl.stryda.app.ui.components.fechaRelativaPasada
import cl.stryda.app.ui.components.hora
import cl.stryda.app.ui.components.momento
import cl.stryda.app.ui.components.tiempoCarrera
import cl.stryda.app.ui.contenedor
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MS_DESHACER = 5_000L
private const val REGISTROS_VISIBLES = 5

private enum class Hoja { NINGUNA, ESTADO, PESO, VELOCIDAD, PEAK, CARRERAS, DATOS }

@Composable
fun PantallaFicha(
    caballoId: String,
    onVolver: () -> Unit,
    onAbrirCarrera: (CarreraDelCaballo) -> Unit,
) {
    val c = contenedor()
    val vm: FichaViewModel = viewModel(key = "ficha-$caballoId") { FichaViewModel(caballoId, c) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptica = LocalHapticFeedback.current
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.avisos.collect { aviso ->
            haptica.performHapticFeedback(HapticFeedbackType.Confirm)
            launch {
                // Deshacer disponible 5 segundos, como pide la regla de acciones reversibles
                val cierre = launch { delay(MS_DESHACER); snackbar.currentSnackbarData?.dismiss() }
                val r = snackbar.showSnackbar(aviso.mensaje, actionLabel = "Deshacer", duration = SnackbarDuration.Indefinite)
                cierre.cancel()
                if (r == SnackbarResult.ActionPerformed) vm.deshacer(aviso)
            }
        }
    }

    FichaContenidoUi(
        estado = estado,
        snackbar = snackbar,
        onVolver = onVolver,
        onAbrirCarrera = onAbrirCarrera,
        onCambiarEstado = vm::cambiarEstado,
        onRegistrarMedicion = vm::registrarMedicion,
        onEditarDatos = vm::editarDatos,
        onRegistrarTrabajo = vm::registrarTrabajo,
        onCompartirPdf = { d ->
            alcance.launch {
                try {
                    val archivo = withContext(Dispatchers.IO) { FichaPdf(contexto).generar(d) }
                    compartirPdf(contexto, archivo, d.caballo.nombre)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    snackbar.showSnackbar("No se pudo preparar el PDF. Intenta de nuevo.")
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaContenidoUi(
    estado: UiState<FichaContenido>,
    onVolver: () -> Unit,
    onAbrirCarrera: (CarreraDelCaballo) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onCambiarEstado: (EstadoCaballo, String?) -> Unit = { _, _ -> },
    onRegistrarMedicion: (Double?, Double?, Double?) -> Unit = { _, _, _ -> },
    onEditarDatos: (Map<CampoCaballo, String?>) -> Unit = {},
    onRegistrarTrabajo: (TipoTrabajo, Int, Double) -> Unit = { _, _, _ -> },
    onCompartirPdf: (FichaContenido) -> Unit = {},
) {
    var hoja by rememberSaveable { mutableStateOf(Hoja.NINGUNA) }
    val contenido = (estado as? UiState.Contenido)?.datos

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver al corral")
                    }
                },
                actions = {
                    if (contenido != null) {
                        TextButton(onClick = { onCompartirPdf(contenido) }, modifier = Modifier.heightIn(min = Espacio.tactil)) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Espacio.s))
                            Text("Ficha PDF")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (estado) {
            UiState.Cargando -> CabeceraCaballo(Modifier.padding(padding))
            UiState.Vacio -> EstadoVacio(
                icono = Icons.Rounded.ErrorOutline,
                titulo = "Ejemplar no encontrado",
                detalle = "Puede que se haya quitado de la planilla. Vuelve al corral para ver la lista actual.",
                accion = "Volver al corral",
                onAccion = onVolver,
                modifier = Modifier.padding(padding),
            )
            is UiState.Error -> EstadoVacio(
                icono = Icons.Rounded.ErrorOutline,
                titulo = "No se pudo abrir la ficha",
                detalle = estado.mensaje,
                modifier = Modifier.padding(padding),
            )
            is UiState.Contenido -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = Espacio.l, end = Espacio.l, bottom = Espacio.xxl),
                verticalArrangement = Arrangement.spacedBy(Espacio.xl),
            ) {
                val d = estado.datos
                CabeceraCaballo()
                Identidad(d, onEditar = { hoja = Hoja.DATOS })
                TarjetaEstado(d, onCambiar = { hoja = Hoja.ESTADO })
                Seccion("Rendimiento", ayuda = "Toca un cuadro para ver su historial o registrar un dato nuevo.") { Cifras(d, onAbrir = { hoja = it }) }
                if (d.ultimaCarrera != null || d.proximaCarrera != null) {
                    Seccion("Carreras") {
                        d.proximaCarrera?.let { TarjetaProximaCarrera(it, d, onAbrirCarrera) }
                        d.ultimaCarrera?.let { TarjetaUltimaCarrera(it, d, onVerTodas = { hoja = Hoja.CARRERAS }) }
                        if (d.jinetes.isNotEmpty()) TarjetaJinetes(d)
                    }
                }
                Seccion("Estimación") { TarjetaEstimacion(d) }
                Seccion("Registrado en este teléfono") { Historial(d) }
            }
        }
    }

    if (contenido != null) {
        when (hoja) {
            Hoja.ESTADO -> HojaEstado(contenido.caballo, onGuardar = { e, nota -> hoja = Hoja.NINGUNA; onCambiarEstado(e, nota) }, onCerrar = { hoja = Hoja.NINGUNA })
            Hoja.PESO -> HojaPeso(contenido, onGuardar = { p, t, f -> hoja = Hoja.NINGUNA; onRegistrarMedicion(p, t, f) }, onCerrar = { hoja = Hoja.NINGUNA })
            Hoja.VELOCIDAD -> HojaVelocidad(contenido, onGuardar = { tp, m, s -> hoja = Hoja.NINGUNA; onRegistrarTrabajo(tp, m, s) }, onCerrar = { hoja = Hoja.NINGUNA })
            Hoja.PEAK -> HojaPeak(contenido, onCerrar = { hoja = Hoja.NINGUNA })
            Hoja.CARRERAS -> HojaCarreras(contenido, onCerrar = { hoja = Hoja.NINGUNA })
            Hoja.DATOS -> HojaDatos(contenido.caballo, onGuardar = { hoja = Hoja.NINGUNA; onEditarDatos(it) }, onCerrar = { hoja = Hoja.NINGUNA })
            Hoja.NINGUNA -> Unit
        }
    }
}

/** El caballo galopa mientras la ficha está abierta. Con animación reducida queda quieto. */
@Composable
private fun CabeceraCaballo(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CaballoGalopando(color = MaterialTheme.colorScheme.primary, cargando = true, modifier = Modifier.width(200.dp))
    }
}

@Composable
private fun Seccion(titulo: String, ayuda: String? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                color = StrydaTema.colores.textMuted,
                modifier = Modifier.semantics { heading() },
            )
            if (ayuda != null) Text(ayuda, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
        }
        contenido()
    }
}

@Composable
private fun Identidad(d: FichaContenido, onEditar: () -> Unit) {
    val k = d.caballo
    val detalle = listOfNotNull(k.pelaje, k.sexo?.etiqueta, k.edad?.let { if (it == 1) "1 año" else "$it años" })
        .joinToString(" · ")
    val faltan = listOfNotNull("pelaje".takeIf { k.pelaje == null }, "sexo".takeIf { k.sexo == null })
    val genealogia = listOfNotNull(k.padre, k.madre).joinToString(" y ")

    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Text(k.nombre, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (detalle.isNotEmpty()) Text(detalle, style = MaterialTheme.typography.bodyLarge, color = StrydaTema.colores.textMuted)
        if (genealogia.isNotEmpty()) {
            Text("Por $genealogia", style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
        }
        k.colores?.let { colores ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                interpretarChaquetilla(colores)?.let { ChaquetillaIcono(it, colores, tamano = 40.dp) }
                Column {
                    Text("Colores", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted)
                    Text(colores, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        // El indicador del corral solo se muestra si agrega algo que la tarjeta de estado no dice
        val indicador = d.indicador
        if (indicador !is IndicadorEstado.Lesionado && indicador !is IndicadorEstado.EnDescanso && indicador !is IndicadorEstado.EntrenaNormal) {
            IndicadorChip(indicador)
        }
        TextButton(onClick = onEditar, modifier = Modifier.heightIn(min = Espacio.tactil)) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Espacio.s))
            Text(if (faltan.isEmpty()) "Editar datos" else "Completar ${faltan.joinToString(" y ")}")
        }
    }
}

@Composable
private fun TarjetaEstado(d: FichaContenido, onCambiar: () -> Unit) {
    val e = d.caballo.estado
    val op = OPCIONES_ESTADO.first { it.estado == e }
    val color = colorEstado(e)
    Card(
        onClick = onCambiar,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, StrydaTema.colores.border),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Estado: ${op.titulo}. Tocar para cambiarlo" },
    ) {
        Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(op.icono, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(Espacio.m))
                Column(Modifier.weight(1f)) {
                    Text("Estado", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted)
                    Text(op.titulo, style = MaterialTheme.typography.titleLarge, color = color)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StrydaTema.colores.textMuted)
            }
            LineaElegibilidad(d.elegibilidad)
            Text(
                d.ultimoCambioEstado?.let { c -> listOfNotNull("Cambiado ${momento(c.instante, d.hoy)}", c.nota).joinToString(" · ") }
                    ?: "Toca para cambiar el estado",
                style = MaterialTheme.typography.bodySmall,
                color = StrydaTema.colores.textMuted,
            )
        }
    }
}

@Composable
private fun LineaElegibilidad(e: Elegibilidad) {
    val c = StrydaTema.colores
    val (icono, texto, color) = when (e) {
        Elegibilidad.Apto -> Triple(Icons.Rounded.CheckCircle, "Apto para correr", c.success)
        is Elegibilidad.EnRetiro -> Triple(Icons.Rounded.HourglassTop, "En período de retiro hasta el ${fechaLarga(e.hasta)}", c.warning)
        Elegibilidad.Lesionado -> Triple(Icons.Rounded.Healing, "No puede correr mientras esté lesionado", c.danger)
        Elegibilidad.NoDisponible -> Triple(Icons.Rounded.ErrorOutline, "Retirado de las carreras", c.textMuted)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun Cifras(d: FichaContenido, onAbrir: (Hoja) -> Unit) {
    val c = StrydaTema.colores
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            val peso = d.peso
            TarjetaCifra(
                etiqueta = "Peso",
                valor = peso?.valorKg,
                unidad = "kg",
                detalle = when {
                    peso == null -> "Sin pesajes registrados"
                    peso.esAntiguo -> "Último pesaje hace ${peso.diasDesdeMedicion} días"
                    peso.variacionMesKg != null -> {
                        val v = peso.variacionMesKg
                        "${if (v >= 0) "+" else "−"}${decimal(abs(v))} kg vs. mes pasado"
                    }
                    else -> "Pesado ${fechaRelativaPasada(peso.fecha, d.hoy)}"
                },
                colorDetalle = if (peso?.esAntiguo == true) c.warning else c.textMuted,
                onClick = { onAbrir(Hoja.PESO) },
                accion = "Ver historial y registrar peso",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            val rapida = d.sesionMasRapida
            TarjetaCifra(
                etiqueta = "Velocidad máxima",
                valor = rapida?.let { aKmH(it.distanciaM!! / it.tiempoSeg!!) },
                unidad = "km/h",
                detalle = rapida?.let { "${tipoCorto(it.tipo)} ${it.distanciaM} m · ${fechaRelativaPasada(it.fecha, d.hoy)}" }
                    ?: "Sin trabajos cronometrados",
                onClick = { onAbrir(Hoja.VELOCIDAD) },
                accion = "Ver trabajos y registrar uno",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            val peak = d.peak
            TarjetaCifra(
                etiqueta = "Peak en carrera",
                valor = peak?.let { aKmH(it.parcial.velocidadMs) },
                unidad = "km/h",
                detalle = peak?.let { p ->
                    listOfNotNull("GMAX a los ${p.parcial.metros} m", p.carrera?.let { fechaRelativaPasada(it.carrera.fecha, d.hoy) })
                        .joinToString(" · ")
                } ?: "Sin datos de parciales",
                onClick = { onAbrir(Hoja.PEAK) },
                accion = "Ver qué es el peak en carrera",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            TarjetaCifra(
                etiqueta = "Desde su última carrera",
                valor = d.diasDesdeUltimaCarrera?.toDouble(),
                unidad = if (d.diasDesdeUltimaCarrera == 1L) "día" else "días",
                decimales = 0,
                detalle = if (d.diasDesdeUltimaCarrera == null) "Aún no debuta" else null,
                onClick = { onAbrir(Hoja.CARRERAS) },
                accion = "Ver todas sus carreras",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

private fun tipoCorto(tipo: TipoTrabajo) = when (tipo) {
    TipoTrabajo.APRONTE -> "Apronte"
    TipoTrabajo.HUINCHADA -> "Huinchada"
    else -> tipo.etiqueta
}

/** Etiqueta chica y apagada, número grande. Sin dato: guion y explicación, nunca un cero falso. Toda la tarjeta abre su detalle. */
@Composable
private fun TarjetaCifra(
    etiqueta: String,
    valor: Double?,
    unidad: String,
    detalle: String?,
    onClick: () -> Unit,
    accion: String,
    modifier: Modifier = Modifier,
    decimales: Int = 1,
    colorDetalle: Color = StrydaTema.colores.textMuted,
) {
    val descripcion = listOfNotNull(etiqueta, valor?.let { "${decimal(it, decimales)} $unidad" } ?: "sin dato", detalle)
        .joinToString(", ")
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, StrydaTema.colores.border),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = descripcion
                onClick(label = accion) { onClick(); true }
            },
    ) {
        Column(
            Modifier
                .padding(Espacio.l)
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted, modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = StrydaTema.colores.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                if (valor != null) {
                    CifraAnimada(valor, { decimal(it, decimales) }, StrydaTema.cifras.media)
                    Spacer(Modifier.width(Espacio.xs))
                    Text(
                        unidad,
                        style = MaterialTheme.typography.bodyLarge,
                        color = StrydaTema.colores.textMuted,
                        modifier = Modifier.padding(bottom = Espacio.xs),
                    )
                } else {
                    Text("—", style = StrydaTema.cifras.media, color = StrydaTema.colores.textMuted)
                }
            }
            if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall, color = colorDetalle)
        }
    }
}

@Composable
private fun TarjetaUltimaCarrera(c: CarreraDelCaballo, d: FichaContenido, onVerTodas: () -> Unit) {
    val p = c.participacion
    val lugar = listOfNotNull(
        fechaRelativaPasada(c.carrera.fecha, d.hoy).replaceFirstChar { it.uppercase() },
        c.carrera.hipodromo,
        listOfNotNull("${c.carrera.distanciaM} m", c.carrera.superficie).joinToString(" "),
    ).joinToString(" · ")
    val detalle = listOfNotNull(p.jinete?.let { "Jinete: $it" }, p.numero?.let { "Nº $it" }, p.pesoKg?.let { "${decimal(it, 0)} kg" })
        .joinToString(" · ")

    Tarjeta(onClick = onVerTodas) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Última carrera", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted, modifier = Modifier.weight(1f))
            Text("Ver todas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${p.posicion}°", style = StrydaTema.cifras.media)
            // Solo se conoce el total de corredores en carreras con el programa completo
            if ((c.corredores ?: 0) >= 2) {
                Spacer(Modifier.width(Espacio.s))
                Text(
                    "de ${c.corredores}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StrydaTema.colores.textMuted,
                    modifier = Modifier.padding(bottom = Espacio.xs),
                )
            }
            Spacer(Modifier.weight(1f))
            p.tiempoSeg?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tiempo aprox.", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted)
                    Text(tiempoCarrera(it), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Text(lugar, style = MaterialTheme.typography.bodyLarge)
        if (detalle.isNotEmpty()) Text(detalle, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
    }
}

@Composable
private fun TarjetaProximaCarrera(c: CarreraDelCaballo, d: FichaContenido, onAbrir: (CarreraDelCaballo) -> Unit) {
    val cuando = listOfNotNull(
        fechaRelativaFutura(c.carrera.fecha, d.hoy).replaceFirstChar { it.uppercase() },
        c.carrera.hora?.let(::hora),
    ).joinToString(", ")
    val carrera = listOfNotNull(c.carrera.numero?.let { "${it}ª carrera" }, "${c.carrera.distanciaM} m", c.carrera.superficie, c.carrera.hipodromo)
        .joinToString(" · ")
    val partida = listOfNotNull(c.participacion.jinete?.let { "Jinete: $it" }, c.participacion.pesoKg?.let { "${decimal(it, 0)} kg" })
        .joinToString(" · ")

    Card(
        onClick = { onAbrir(c) },
        colors = CardDefaults.cardColors(containerColor = StrydaTema.colores.infoSoft),
        border = BorderStroke(1.dp, StrydaTema.colores.border),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Próxima carrera: $cuando, $carrera, $partida" },
    ) {
        Row(Modifier.heightIn(min = 72.dp).padding(Espacio.l), verticalAlignment = Alignment.CenterVertically) {
            c.participacion.numero?.let {
                NumeroMandil(it)
                Spacer(Modifier.width(Espacio.m))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                Text("Próxima carrera", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.info)
                Text(cuando, style = MaterialTheme.typography.titleMedium)
                Text(carrera, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
                if (partida.isNotEmpty()) Text(partida, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StrydaTema.colores.textMuted)
        }
    }
}

/** Quiénes lo han corrido: el que más lo montó arriba. */
@Composable
private fun TarjetaJinetes(d: FichaContenido) {
    Tarjeta {
        Text("Jinetes que lo han corrido", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted)
        d.jinetes.take(4).forEachIndexed { i, j ->
            if (i > 0) HorizontalDivider(color = StrydaTema.colores.border)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(j.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(if (j.montas == 1) "1 carrera" else "${j.montas} carreras", "última ${fechaRelativaPasada(j.ultimaMonta, d.hoy)}")
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = StrydaTema.colores.textMuted,
                    )
                }
                j.mejorPosicion?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Mejor", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted)
                        Text("$it°", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

/** Observa, nunca recomienda. Y siempre dice con cuántos datos se calculó. */
@Composable
private fun TarjetaEstimacion(d: FichaContenido) {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Icon(Icons.Rounded.Insights, contentDescription = null, tint = StrydaTema.colores.info, modifier = Modifier.size(20.dp))
            Text("Tiempo esperado", style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.textMuted)
        }
        when (val r = d.estimacion) {
            is ResultadoEstimacion.Insuficiente -> Text(
                if (r.carrerasConTiempo == 0) {
                    "Todavía no tiene carreras con tiempo. La estimación aparece cuando haya corrido al menos ${r.minimo}."
                } else {
                    "Necesita al menos ${r.minimo} carreras con tiempo para estimar. Lleva ${r.carrerasConTiempo}."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            is ResultadoEstimacion.Disponible -> {
                val e = r.estimacion
                val para = if (d.proximaCarrera != null) "de su próxima carrera" else "de su última carrera"
                Text("Para los ${e.distanciaM} m $para", style = MaterialTheme.typography.bodyLarge)
                Text(tiempoCarrera(e.tiempoCentralSeg), style = StrydaTema.cifras.media)
                Text(
                    "Entre ${tiempoCarrera(e.tiempoMinSeg)} y ${tiempoCarrera(e.tiempoMaxSeg)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StrydaTema.colores.textMuted,
                )
                Spacer(Modifier.height(Espacio.xs))
                Text(textoTendencia(e.tendenciaSegPorCarrera, e.carrerasUsadas), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Calculado con sus últimas ${e.carrerasUsadas} carreras, comparado solo con él mismo. " +
                        "Es una estimación estadística, no un pronóstico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrydaTema.colores.textMuted,
                )
            }
        }
    }
}

internal fun textoTendencia(segPorCarrera: Double, carreras: Int): String {
    val magnitud = decimal(abs(segPorCarrera))
    return when {
        abs(segPorCarrera) < 0.1 -> "Sus tiempos se mantienen parejos en las últimas $carreras carreras."
        segPorCarrera < 0 -> "Sus tiempos vienen bajando unos $magnitud s por carrera."
        else -> "Sus tiempos vienen subiendo unos $magnitud s por carrera."
    }
}

@Composable
private fun Historial(d: FichaContenido) {
    Tarjeta {
        if (d.registros.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = StrydaTema.colores.textMuted)
                Text(
                    "Lo que cambies o registres acá queda guardado en el teléfono con fecha y hora, aunque no haya señal.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StrydaTema.colores.textMuted,
                )
            }
            return@Tarjeta
        }
        d.registros.take(REGISTROS_VISIBLES).forEachIndexed { i, r ->
            if (i > 0) HorizontalDivider(color = StrydaTema.colores.border, modifier = Modifier.padding(vertical = Espacio.s))
            FilaRegistro(r, d)
        }
        val resto = d.registros.size - REGISTROS_VISIBLES
        if (resto > 0) {
            Spacer(Modifier.height(Espacio.s))
            Text(
                if (resto == 1) "y 1 registro más" else "y $resto registros más",
                style = MaterialTheme.typography.bodySmall,
                color = StrydaTema.colores.textMuted,
            )
        }
    }
}

@Composable
private fun FilaRegistro(r: RegistroTelefono, d: FichaContenido) {
    val (titulo, cuando, nota) = when (r) {
        is RegistroTelefono.Cambio -> Triple(
            "${r.cambio.campo.etiqueta}: ${valorLegible(r.cambio.campo, r.cambio.valorAnterior)} → ${valorLegible(r.cambio.campo, r.cambio.valorNuevo)}",
            momento(r.cambio.instante, d.hoy),
            r.cambio.nota,
        )
        is RegistroTelefono.Medida -> Triple(
            when (r.medicion.tipo) {
                TipoMedicion.PESO_KG -> "Peso: ${decimal(r.medicion.valor)} kg"
                TipoMedicion.TEMPERATURA -> "Temperatura: ${decimal(r.medicion.valor)} °C"
                TipoMedicion.FC_REPOSO -> "Frecuencia en reposo: ${decimal(r.medicion.valor, 0)} lpm"
            },
            fechaRelativaPasada(r.medicion.fecha, d.hoy),
            null,
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge)
        Text(listOfNotNull(cuando.replaceFirstChar { it.uppercase() }, nota).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
    }
}

private fun valorLegible(campo: CampoCaballo, valor: String?): String = when {
    valor.isNullOrBlank() -> "sin dato"
    campo == CampoCaballo.ESTADO -> etiquetaEstado(EstadoCaballo.desde(valor))
    campo == CampoCaballo.SEXO -> Sexo.desde(valor)?.etiqueta ?: valor
    campo == CampoCaballo.EDAD -> "$valor años"
    else -> valor
}

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val borde = BorderStroke(1.dp, StrydaTema.colores.border)
    if (onClick != null) {
        Card(onClick = onClick, colors = colores, border = borde, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.xs), content = contenido)
        }
    } else {
        Card(colors = colores, border = borde, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.xs), content = contenido)
        }
    }
}
