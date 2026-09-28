package cl.stryda.app.ui.screens.caballo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.CampoCaballo
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.RangosMedicion
import cl.stryda.app.domain.Sexo
import cl.stryda.app.ui.components.decimal
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.FormaPill
import cl.stryda.app.ui.theme.StrydaTema

val PELAJES = listOf("Alazán", "Castaño", "Colorado", "Mulato", "Tordillo", "Zaino", "Rosillo", "Negro")

data class OpcionEstado(val estado: EstadoCaballo, val titulo: String, val detalle: String, val icono: ImageVector)

val OPCIONES_ESTADO = listOf(
    OpcionEstado(EstadoCaballo.ACTIVO, "Activo", "Entrena y puede correr", Icons.Rounded.CheckCircle),
    OpcionEstado(EstadoCaballo.LESIONADO, "Lesionado", "No entrena ni corre por ahora", Icons.Rounded.Healing),
    OpcionEstado(EstadoCaballo.DESCANSANDO, "En descanso", "Parado por decisión del corral", Icons.Rounded.Hotel),
    OpcionEstado(EstadoCaballo.RETIRADO, "Retirado", "Deja de aparecer en la lista del corral", Icons.Rounded.Inventory2),
)

fun etiquetaEstado(e: EstadoCaballo) = OPCIONES_ESTADO.first { it.estado == e }.titulo

@Composable
fun colorEstado(e: EstadoCaballo): Color = when (e) {
    EstadoCaballo.ACTIVO -> StrydaTema.colores.success
    EstadoCaballo.LESIONADO -> StrydaTema.colores.danger
    EstadoCaballo.DESCANSANDO -> StrydaTema.colores.warning
    EstadoCaballo.RETIRADO -> StrydaTema.colores.textMuted
}

/** Acepta coma o punto: "485,5" → 485.5 */
fun leerNumero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Hoja(titulo: String, onCerrar: () -> Unit, contenido: @Composable () -> Unit) {
    // Sin gestos de arrastre: el dedo desplaza el contenido y la hoja no se cierra por accidente.
    // Se cierra con la ✕, con "atrás" o tocando fuera.
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.l)
                .padding(bottom = Espacio.xl)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Espacio.m)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                }
            }
            contenido()
        }
    }
}

@Composable
internal fun BotonGuardar(texto: String, habilitado: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun HojaEstado(
    caballo: Caballo,
    onGuardar: (EstadoCaballo, String?) -> Unit,
    onCerrar: () -> Unit,
) {
    var elegido by rememberSaveable { mutableStateOf(caballo.estado) }
    var nota by rememberSaveable { mutableStateOf("") }

    Hoja("Estado de ${caballo.nombre}", onCerrar) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            OPCIONES_ESTADO.forEach { op ->
                val seleccionado = elegido == op.estado
                val color = colorEstado(op.estado)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .border(
                            width = if (seleccionado) 2.dp else 1.dp,
                            color = if (seleccionado) color else StrydaTema.colores.border,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .selectable(selected = seleccionado, role = Role.RadioButton) { elegido = op.estado }
                        .padding(horizontal = Espacio.l, vertical = Espacio.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(op.icono, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(Espacio.m))
                    Column(Modifier.weight(1f)) {
                        Text(op.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(op.detalle, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
                    }
                    RadioButton(selected = seleccionado, onClick = null)
                }
            }
        }
        OutlinedTextField(
            value = nota,
            onValueChange = { nota = it },
            label = { Text("Nota (opcional)") },
            supportingText = { Text("Por ejemplo: \"Tendón mano derecha, revisión el lunes\"") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        BotonGuardar(
            texto = if (elegido == caballo.estado) "Sin cambios" else "Guardar: ${etiquetaEstado(elegido)}",
            habilitado = elegido != caballo.estado,
        ) { onGuardar(elegido, nota.trim().ifEmpty { null }) }
    }
}

internal fun errorRango(texto: String, rango: ClosedFloatingPointRange<Double>, unidad: String): String? {
    if (texto.isBlank()) return null
    val v = leerNumero(texto) ?: return "Escribe solo números"
    return if (v in rango) null else "${decimal(v)} $unidad parece un error de tipeo. Revísalo."
}

@Composable
internal fun CampoNumero(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    ayuda: String,
    decimales: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> if (nuevo.length <= 6 && nuevo.all { it.isDigit() || (decimales && (it == ',' || it == '.')) }) onCambio(nuevo) },
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = { Text(error ?: ayuda) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimales) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        textStyle = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HojaDatos(
    caballo: Caballo,
    onGuardar: (Map<CampoCaballo, String?>) -> Unit,
    onCerrar: () -> Unit,
) {
    var pelaje by rememberSaveable { mutableStateOf(caballo.pelaje) }
    var sexo by rememberSaveable { mutableStateOf(caballo.sexo?.clave) }
    var edad by rememberSaveable { mutableStateOf(caballo.edad?.toString().orEmpty()) }
    var padre by rememberSaveable { mutableStateOf(caballo.padre.orEmpty()) }
    var madre by rememberSaveable { mutableStateOf(caballo.madre.orEmpty()) }

    val nuevos = mapOf(
        CampoCaballo.PELAJE to pelaje,
        CampoCaballo.SEXO to sexo,
        CampoCaballo.EDAD to edad.trim().ifEmpty { null },
        CampoCaballo.PADRE to padre.trim().ifEmpty { null },
        CampoCaballo.MADRE to madre.trim().ifEmpty { null },
    )
    val actuales = mapOf(
        CampoCaballo.PELAJE to caballo.pelaje,
        CampoCaballo.SEXO to caballo.sexo?.clave,
        CampoCaballo.EDAD to caballo.edad?.toString(),
        CampoCaballo.PADRE to caballo.padre,
        CampoCaballo.MADRE to caballo.madre,
    )
    val cambiados = nuevos.filter { (campo, v) -> v != actuales[campo] }
    val edadInvalida = edad.isNotBlank() && edad.trim().toIntOrNull()?.let { it !in 1..30 } != false

    Hoja("Datos de ${caballo.nombre}", onCerrar) {
        Etiqueta("Pelaje")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            PELAJES.forEach { p ->
                FilterChip(
                    selected = pelaje == p,
                    onClick = { pelaje = if (pelaje == p) null else p },
                    label = { Text(p, style = MaterialTheme.typography.labelLarge) },
                    shape = FormaPill,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Etiqueta("Sexo")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Sexo.entries.forEach { s ->
                FilterChip(
                    selected = sexo == s.clave,
                    onClick = { sexo = if (sexo == s.clave) null else s.clave },
                    label = { Text(s.etiqueta, style = MaterialTheme.typography.labelLarge) },
                    shape = FormaPill,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        OutlinedTextField(
            value = edad,
            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) edad = it },
            label = { Text("Edad (años)") },
            isError = edadInvalida,
            supportingText = if (edadInvalida) ({ Text("Revisa la edad") }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = padre,
            onValueChange = { padre = it },
            label = { Text("Padre") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = madre,
            onValueChange = { madre = it },
            label = { Text("Madre") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        BotonGuardar(
            texto = when (cambiados.size) {
                0 -> "Sin cambios"
                1 -> "Guardar 1 cambio"
                else -> "Guardar ${cambiados.size} cambios"
            },
            habilitado = cambiados.isNotEmpty() && !edadInvalida,
        ) { onGuardar(cambiados) }
    }
}

@Composable
internal fun Etiqueta(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, color = StrydaTema.colores.textMuted)
}
