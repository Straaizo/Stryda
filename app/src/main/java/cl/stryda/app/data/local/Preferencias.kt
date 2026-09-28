package cl.stryda.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant

enum class ModoTema { SISTEMA, CLARO, OSCURO }

enum class OrigenDatos { PRUEBA, PLANILLA }

data class EstadoDatos(val ultimaActualizacion: Instant?, val origen: OrigenDatos)

private val Context.dataStore by preferencesDataStore(name = "preferencias")

class Preferencias(private val context: Context) {

    private val claveTema = stringPreferencesKey("modo_tema")
    private val claveActualizacion = longPreferencesKey("ultima_actualizacion")
    private val claveOrigen = stringPreferencesKey("origen_datos")
    private val claveVersionSeed = androidx.datastore.preferences.core.intPreferencesKey("version_seed")

    private val datos = context.dataStore.data.catch { e ->
        if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e
    }

    val modoTema: Flow<ModoTema> = datos.map { prefs ->
        prefs[claveTema]?.let { valor -> ModoTema.entries.firstOrNull { it.name == valor } } ?: ModoTema.SISTEMA
    }

    val estadoDatos: Flow<EstadoDatos> = datos.map { prefs ->
        EstadoDatos(
            ultimaActualizacion = prefs[claveActualizacion]?.let(Instant::ofEpochMilli),
            origen = prefs[claveOrigen]?.let { v -> OrigenDatos.entries.firstOrNull { it.name == v } } ?: OrigenDatos.PRUEBA,
        )
    }

    val versionSeed: Flow<Int?> = datos.map { it[claveVersionSeed] }

    suspend fun guardarVersionSeed(version: Int) {
        context.dataStore.edit { it[claveVersionSeed] = version }
    }

    suspend fun guardarModoTema(modo: ModoTema) {
        context.dataStore.edit { it[claveTema] = modo.name }
    }

    suspend fun registrarActualizacion(instante: Instant, origen: OrigenDatos) {
        context.dataStore.edit {
            it[claveActualizacion] = instante.toEpochMilli()
            it[claveOrigen] = origen.name
        }
    }
}
