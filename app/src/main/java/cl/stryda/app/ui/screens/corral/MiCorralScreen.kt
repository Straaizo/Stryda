package cl.stryda.app.ui.screens.corral

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.ui.UiState
import cl.stryda.app.ui.components.AvatarCaballo
import cl.stryda.app.ui.components.EstadoVacio
import cl.stryda.app.ui.components.IndicadorActualizacion
import cl.stryda.app.ui.components.IndicadorChip
import cl.stryda.app.ui.components.ListaSkeleton
import cl.stryda.app.ui.components.hora
import cl.stryda.app.ui.components.textoIndicador
import cl.stryda.app.ui.contenedor
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.diasEntre
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema
import java.time.LocalDate

@Composable
fun PantallaMiCorral(
    corralId: String,
    onAbrirCaballo: (id: String, nombre: String) -> Unit,
    onAbrirCarrera: (CarreraDelCaballo) -> Unit,
    onRegistrarTrabajo: () -> Unit,
    onVerCarreras: () -> Unit = {},
) {
    val c = contenedor()
    val vm: MiCorralViewModel = viewModel(key = "corral-$corralId") { MiCorralViewModel(corralId, c) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    MiCorralContenidoUi(estado, onAbrirCaballo, onAbrirCarrera, onRegistrarTrabajo, vm::buscarDatosNuevos, onVerCarreras)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiCorralContenidoUi(
    estado: UiState<MiCorralContenido>,
    onAbrirCaballo: (id: String, nombre: String) -> Unit,
    onAbrirCarrera: (CarreraDelCaballo) -> Unit,
    onRegistrarTrabajo: () -> Unit,
    onBuscarDatos: () -> Unit,
    onVerCarreras: () -> Unit = {},
) {
    val contenido = (estado as? UiState.Contenido)?.datos
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            contenido?.corral?.nombre ?: "Mi corral",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (contenido != null) IndicadorActualizacion(contenido.datos, contenido.ahora)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (contenido != null) {
                ExtendedFloatingActionButton(
                    onClick = onRegistrarTrabajo,
                    icon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                    text = { Text("Registrar trabajo de hoy", style = MaterialTheme.typography.labelLarge) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (estado) {
            UiState.Cargando -> ListaSkeleton(modifier = Modifier.padding(padding))
            UiState.Vacio -> EstadoVacio(
                icono = Icons.Rounded.Grass,
                titulo = "Todavía no hay ejemplares",
                detalle = "Cuando se agreguen caballos a la planilla del corral, aparecen acá.",
                accion = "Buscar datos nuevos",
                onAccion = onBuscarDatos,
                modifier = Modifier.padding(padding),
            )
            is UiState.Error -> EstadoVacio(
                icono = Icons.Rounded.ErrorOutline,
                titulo = "No se pudo abrir el corral",
                detalle = estado.mensaje,
                modifier = Modifier.padding(padding),
            )
            is UiState.Contenido -> ListaCorral(estado.datos, padding, onAbrirCaballo, onAbrirCarrera, onVerCarreras)
        }
    }
}

@Composable
private fun ListaCorral(
    datos: MiCorralContenido,
    padding: PaddingValues,
    onAbrirCaballo: (id: String, nombre: String) -> Unit,
    onAbrirCarrera: (CarreraDelCaballo) -> Unit,
    onVerCarreras: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            // Espacio para que el botón flotante no tape la última fila
            bottom = padding.calculateBottomPadding() + 96.dp,
        ),
    ) {
        if (datos.correnPronto.isNotEmpty()) {
            item(key = "corren-pronto") {
                FranjaCorrenPronto(datos.correnPronto, datos.hoy, datos.corral.hipodromo, onAbrirCarrera, onVerCarreras)
            }
        }
        item(key = "titulo-ejemplares") {
            Text(
                "Ejemplares (${datos.filas.size})",
                style = MaterialTheme.typography.titleSmall,
                color = StrydaTema.colores.textMuted,
                modifier = Modifier
                    .padding(start = Espacio.l, end = Espacio.l, top = Espacio.l, bottom = Espacio.xs)
                    .semantics { heading() },
            )
        }
        items(datos.filas, key = { it.caballo.id }) { fila ->
            FilaEjemplar(fila, onClick = { onAbrirCaballo(fila.caballo.id, fila.caballo.nombre) })
            HorizontalDivider(color = StrydaTema.colores.border, modifier = Modifier.padding(start = 76.dp))
        }
    }
}

@Composable
private fun FilaEjemplar(fila: FilaCaballo, onClick: () -> Unit) {
    val caballo = fila.caballo
    val detalle = listOfNotNull(caballo.edad?.let { if (it == 1) "1 año" else "$it años" }, caballo.sexo?.etiqueta)
        .joinToString(" · ")
    val descripcion = listOf(caballo.nombre, detalle, textoIndicador(fila.indicador)).filter { it.isNotEmpty() }.joinToString(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClickLabel = "Ver ficha", onClick = onClick)
            .clearAndSetSemantics { contentDescription = descripcion }
            .padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarCaballo(caballo.nombre, caballo.fotoUrl)
        Spacer(Modifier.width(Espacio.m))
        Column(Modifier.weight(1f)) {
            // Dos líneas: con fuente grande el nombre se parte antes que cortarse
            Text(caballo.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (detalle.isNotEmpty()) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
            }
        }
        Spacer(Modifier.width(Espacio.s))
        IndicadorChip(fila.indicador)
    }
}

/** Solo existe si alguien corre en los próximos 7 días. Lista vertical: nada crítico detrás de un scroll lateral. */
@Composable
private fun FranjaCorrenPronto(
    carreras: List<CarreraDelCaballo>,
    hoy: LocalDate,
    hipodromoCorral: String,
    onAbrirCarrera: (CarreraDelCaballo) -> Unit,
    onVerCarreras: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StrydaTema.colores.infoSoft),
        border = BorderStroke(1.dp, StrydaTema.colores.border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espacio.l, vertical = Espacio.s),
    ) {
        Text(
            "Corren esta semana",
            style = MaterialTheme.typography.titleSmall,
            color = StrydaTema.colores.info,
            modifier = Modifier
                .padding(start = Espacio.l, end = Espacio.l, top = Espacio.m)
                .semantics { heading() },
        )
        carreras.take(MAX_EN_FRANJA).forEach { c ->
            val cuando = textoIndicador(IndicadorEstado.CorreEn(diasEntre(hoy, c.carrera.fecha)))
            val detalle = listOfNotNull(
                c.carrera.hora?.let(::hora),
                c.carrera.numero?.let { "${it}ª carrera" },
                "${c.carrera.distanciaM} m",
                // El hipódromo solo se nombra si no es el de siempre
                c.carrera.hipodromo.takeIf { it != hipodromoCorral },
            ).joinToString(" · ")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClickLabel = "Ver próxima carrera") { onAbrirCarrera(c) }
                    .clearAndSetSemantics { contentDescription = "${c.caballo.nombre}, $cuando, $detalle" }
                    .padding(horizontal = Espacio.l, vertical = Espacio.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${c.caballo.nombre} · $cuando", style = MaterialTheme.typography.titleMedium)
                    Text(detalle, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StrydaTema.colores.textMuted)
            }
        }
        if (carreras.size > MAX_EN_FRANJA) {
            TextButton(
                onClick = onVerCarreras,
                modifier = Modifier
                    .padding(horizontal = Espacio.s)
                    .heightIn(min = Espacio.tactil),
            ) { Text("Ver las ${carreras.size} carreras") }
        }
        Spacer(Modifier.padding(bottom = Espacio.xs))
    }
}

/** Con más carreras la franja deja de ser un vistazo y empuja la lista fuera de la pantalla. */
private const val MAX_EN_FRANJA = 3
