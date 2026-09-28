package cl.stryda.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "corrales")
data class CorralEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val preparador: String,
    val hipodromo: String,
)

@Entity(tableName = "caballos", indices = [Index("corral_id")])
data class CaballoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "corral_id") val corralId: String?,
    val nombre: String,
    val sexo: String?,
    val edad: Int?,
    val pelaje: String?,
    val padre: String?,
    val madre: String?,
    val estado: String,
    @ColumnInfo(name = "foto_url") val fotoUrl: String?,
    /** Colores de la chaquetilla, como los describe el programa ("Naranjo franja y puños dorado"). */
    val colores: String? = null,
)

@Entity(tableName = "propietarios")
data class PropietarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val email: String?,
    val telefono: String?,
)

@Entity(
    tableName = "caballo_propietario",
    primaryKeys = ["caballo_id", "propietario_id"],
    indices = [Index("propietario_id")],
)
data class CaballoPropietarioEntity(
    @ColumnInfo(name = "caballo_id") val caballoId: String,
    @ColumnInfo(name = "propietario_id") val propietarioId: String,
    val porcentaje: Double,
)

/** [pendiente] = registrada en el teléfono y aún no enviada (la demo no escribe a Sheets). */
@Entity(tableName = "sesiones", indices = [Index("caballo_id"), Index("fecha")])
data class SesionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "caballo_id") val caballoId: String,
    val fecha: LocalDate,
    val tipo: String,
    @ColumnInfo(name = "distancia_m") val distanciaM: Int?,
    @ColumnInfo(name = "tiempo_seg") val tiempoSeg: Double?,
    val vareador: String?,
    val pista: String?,
    @ColumnInfo(name = "estado_pista") val estadoPista: String?,
    val esfuerzo: Int?,
    val notas: String?,
    val pendiente: Boolean = false,
)

/** [pendiente] = registrada a mano en el teléfono; sobrevive a las recargas del seed o la planilla. */
@Entity(tableName = "mediciones", indices = [Index("caballo_id")])
data class MedicionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "caballo_id") val caballoId: String,
    val fecha: LocalDate,
    val tipo: String,
    val valor: Double,
    @ColumnInfo(defaultValue = "0") val pendiente: Boolean = false,
)

/**
 * Cambio manual a un dato del caballo (estado, pelaje, sexo...). Nunca se sobrescribe el dato base:
 * el valor vigente se deriva aplicando el último cambio de cada campo. Queda como historial con fecha y hora.
 */
@Entity(tableName = "cambios_caballo", indices = [Index("caballo_id")])
data class CambioCaballoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "caballo_id") val caballoId: String,
    @ColumnInfo(name = "instante_ms") val instanteMs: Long,
    val campo: String,
    @ColumnInfo(name = "valor_anterior") val valorAnterior: String?,
    @ColumnInfo(name = "valor_nuevo") val valorNuevo: String?,
    val nota: String?,
    @ColumnInfo(defaultValue = "1") val pendiente: Boolean = true,
)

@Entity(tableName = "tratamientos", indices = [Index("caballo_id")])
data class TratamientoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "caballo_id") val caballoId: String,
    val fecha: LocalDate,
    val producto: String,
    val via: String?,
    val veterinario: String?,
    @ColumnInfo(name = "dias_retiro") val diasRetiro: Int,
    val visibilidad: String,
)

@Entity(tableName = "carreras", indices = [Index("fecha")])
data class CarreraEntity(
    @PrimaryKey val id: String,
    val hipodromo: String,
    val fecha: LocalDate,
    val hora: String?,
    val numero: Int?,
    @ColumnInfo(name = "distancia_m") val distanciaM: Int,
    val superficie: String?,
    @ColumnInfo(name = "condicion_pista") val condicionPista: String?,
    val serie: String?,
    val premio: Long?,
)

@Entity(tableName = "participaciones", indices = [Index("carrera_id"), Index("caballo_id")])
data class ParticipacionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "carrera_id") val carreraId: String,
    @ColumnInfo(name = "caballo_id") val caballoId: String,
    val numero: Int?,
    val jinete: String?,
    @ColumnInfo(name = "peso_kg") val pesoKg: Double?,
    val posicion: Int?,
    @ColumnInfo(name = "tiempo_seg") val tiempoSeg: Double?,
    val dividendo: Double?,
)

@Entity(tableName = "parciales", primaryKeys = ["participacion_id", "metros"])
data class ParcialEntity(
    @ColumnInfo(name = "participacion_id") val participacionId: String,
    val metros: Int,
    @ColumnInfo(name = "tiempo_acum_seg") val tiempoAcumSeg: Double,
    @ColumnInfo(name = "velocidad_ms") val velocidadMs: Double,
)

/** Todas las tablas que vienen de afuera (seed o planilla), listas para reemplazar de una vez. */
data class Catalogo(
    val corrales: List<CorralEntity>,
    val caballos: List<CaballoEntity>,
    val propietarios: List<PropietarioEntity>,
    val caballoPropietario: List<CaballoPropietarioEntity>,
    val sesiones: List<SesionEntity>,
    val mediciones: List<MedicionEntity>,
    val tratamientos: List<TratamientoEntity>,
    val carreras: List<CarreraEntity>,
    val participaciones: List<ParticipacionEntity>,
    val parciales: List<ParcialEntity>,
)
