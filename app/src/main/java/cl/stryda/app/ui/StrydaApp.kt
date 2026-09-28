package cl.stryda.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.ui.navigation.Ruta
import cl.stryda.app.ui.screens.ajustes.PantallaAjustes
import cl.stryda.app.ui.screens.caballo.PantallaFicha
import cl.stryda.app.ui.screens.carreras.PantallaCarreras
import cl.stryda.app.ui.screens.corral.PantallaMiCorral
import cl.stryda.app.ui.screens.corrales.PantallaMisCorrales
import cl.stryda.app.ui.screens.inicio.InicioViewModel
import cl.stryda.app.ui.screens.inicio.PantallaInicio
import cl.stryda.app.ui.theme.Duracion
import cl.stryda.app.ui.theme.LocalAnimacionReducida
import cl.stryda.app.ui.theme.StrydaTema

@Composable
fun StrydaApp() {
    val c = contenedor()
    val inicio: InicioViewModel = viewModel { InicioViewModel(c) }
    val destino by inicio.destino.collectAsStateWithLifecycle()

    Crossfade(targetState = destino, animationSpec = tween(Duracion.PANTALLA), label = "inicio") { ruta ->
        if (ruta == null) PantallaInicio() else Navegacion(ruta)
    }
}

private enum class Pestana(val etiqueta: String, val icono: ImageVector) {
    CORRAL("Corral", Icons.Rounded.Grass),
    CARRERAS("Carreras", Icons.Rounded.Flag),
    AJUSTES("Ajustes", Icons.Rounded.Settings),
}

private fun NavDestination?.pestana(): Pestana = when {
    this?.hasRoute<Ruta.Carreras>() == true -> Pestana.CARRERAS
    this?.hasRoute<Ruta.Ajustes>() == true -> Pestana.AJUSTES
    else -> Pestana.CORRAL
}

@Composable
private fun Navegacion(rutaInicio: Ruta) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val actual = entrada?.destination.pestana()
    val reducida = LocalAnimacionReducida.current
    val duracion = if (reducida) 0 else Duracion.PANTALLA

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column {
                HorizontalDivider(color = StrydaTema.colores.border)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Pestana.entries.forEach { p ->
                        NavigationBarItem(
                            selected = actual == p,
                            onClick = { nav.irAPestana(p, rutaInicio) },
                            icon = { Icon(p.icono, contentDescription = null) },
                            label = { Text(p.etiqueta, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = StrydaTema.colores.primarySoft,
                                unselectedIconColor = StrydaTema.colores.textMuted,
                                unselectedTextColor = StrydaTema.colores.textMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val soloAbajo = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = nav,
            startDestination = rutaInicio,
            modifier = Modifier.padding(soloAbajo).consumeWindowInsets(soloAbajo),
            enterTransition = { fadeIn(tween(duracion)) },
            exitTransition = { fadeOut(tween(duracion)) },
        ) {
            composable<Ruta.Corrales> {
                PantallaMisCorrales(onAbrirCorral = { nav.navigate(Ruta.Corral(it)) })
            }
            composable<Ruta.Corral> { e ->
                PantallaMiCorral(
                    corralId = e.toRoute<Ruta.Corral>().corralId,
                    onAbrirCaballo = { id, _ -> nav.navigate(Ruta.Caballo(id)) },
                    onAbrirCarrera = { nav.navigate(pendienteCarrera(it)) },
                    onVerCarreras = { nav.irAPestana(Pestana.CARRERAS, rutaInicio) },
                    onRegistrarTrabajo = {
                        nav.navigate(Ruta.Pendiente("Registrar trabajo", "Acá se registra el trabajo del día de todo el corral en un par de toques. Es el paso 3, el siguiente."))
                    },
                )
            }
            composable<Ruta.Caballo> { e ->
                PantallaFicha(
                    caballoId = e.toRoute<Ruta.Caballo>().caballoId,
                    onVolver = { nav.popBackStack() },
                    onAbrirCarrera = { nav.navigate(pendienteCarrera(it)) },
                )
            }
            composable<Ruta.Carreras> {
                PantallaCarreras(onAbrirCarrera = { nav.navigate(pendienteCarrera(it)) })
            }
            composable<Ruta.Ajustes> { PantallaAjustes() }
            composable<Ruta.Pendiente> { e ->
                val r = e.toRoute<Ruta.Pendiente>()
                cl.stryda.app.ui.screens.pendiente.PantallaPendiente(r.titulo, r.detalle, onVolver = { nav.popBackStack() })
            }
        }
    }
}

private fun pendienteCarrera(c: CarreraDelCaballo) = Ruta.Pendiente(
    titulo = "Carrera de ${c.caballo.nombre}",
    detalle = "Acá va la próxima carrera: hora, jinete, peso, número de partida y rivales de cuidado. Se construye en el paso 5.",
)

private fun NavHostController.irAPestana(p: Pestana, rutaInicio: Ruta) {
    val destino = when (p) {
        Pestana.CORRAL -> rutaInicio
        Pestana.CARRERAS -> Ruta.Carreras
        Pestana.AJUSTES -> Ruta.Ajustes
    }
    navigate(destino) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
