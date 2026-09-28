package cl.stryda.app.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class CorralConConteo(
    @Embedded val corral: CorralEntity,
    val ejemplares: Int,
)

data class CarreraParticipacionFila(
    @Embedded(prefix = "c_") val carrera: CarreraEntity,
    @Embedded(prefix = "p_") val participacion: ParticipacionEntity,
    @Embedded(prefix = "k_") val caballo: CaballoEntity,
    val corredores: Int,
)

@Dao
interface CorralDao {
    @Query(
        """
        SELECT corrales.*, COUNT(caballos.id) AS ejemplares FROM corrales
        LEFT JOIN caballos ON caballos.corral_id = corrales.id AND caballos.estado != 'retirado'
        GROUP BY corrales.id ORDER BY corrales.nombre
        """
    )
    fun observarConConteo(): Flow<List<CorralConConteo>>

    @Query("SELECT * FROM corrales WHERE id = :id")
    fun observar(id: String): Flow<CorralEntity?>

    @Query("SELECT * FROM corrales ORDER BY nombre")
    suspend fun todos(): List<CorralEntity>

    @Query("SELECT COUNT(*) FROM corrales")
    suspend fun contar(): Int
}

@Dao
interface CaballoDao {
    // El filtro de retirados se aplica después de los cambios manuales (CaballoRepository)
    @Query("SELECT * FROM caballos WHERE corral_id = :corralId ORDER BY nombre")
    fun observarDelCorral(corralId: String): Flow<List<CaballoEntity>>

    @Query("SELECT * FROM caballos WHERE id = :id")
    fun observar(id: String): Flow<CaballoEntity?>
}

@Dao
interface SesionDao {
    @Query(
        """
        SELECT sesiones.* FROM sesiones JOIN caballos ON caballos.id = sesiones.caballo_id
        WHERE caballos.corral_id = :corralId AND sesiones.fecha >= :desde
        """
    )
    fun observarDelCorralDesde(corralId: String, desde: LocalDate): Flow<List<SesionEntity>>

    @Query("SELECT * FROM sesiones WHERE caballo_id = :caballoId ORDER BY fecha DESC")
    fun observarDelCaballo(caballoId: String): Flow<List<SesionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(sesiones: List<SesionEntity>)

    @Query("DELETE FROM sesiones WHERE id IN (:ids)")
    suspend fun eliminar(ids: List<String>)
}

@Dao
interface SaludDao {
    @Query(
        """
        SELECT tratamientos.* FROM tratamientos JOIN caballos ON caballos.id = tratamientos.caballo_id
        WHERE caballos.corral_id = :corralId
        """
    )
    fun observarTratamientosDelCorral(corralId: String): Flow<List<TratamientoEntity>>

    @Query("SELECT * FROM tratamientos WHERE caballo_id = :caballoId ORDER BY fecha DESC")
    fun observarTratamientos(caballoId: String): Flow<List<TratamientoEntity>>

    @Query("SELECT * FROM mediciones WHERE caballo_id = :caballoId ORDER BY fecha")
    fun observarMediciones(caballoId: String): Flow<List<MedicionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMediciones(mediciones: List<MedicionEntity>)

    @Query("DELETE FROM mediciones WHERE id IN (:ids) AND pendiente = 1")
    suspend fun eliminarMediciones(ids: List<String>)
}

@Dao
interface CambioDao {
    @Query("SELECT * FROM cambios_caballo ORDER BY instante_ms")
    fun observarTodos(): Flow<List<CambioCaballoEntity>>

    @Query("SELECT * FROM cambios_caballo WHERE caballo_id = :caballoId ORDER BY instante_ms")
    fun observarDelCaballo(caballoId: String): Flow<List<CambioCaballoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(cambios: List<CambioCaballoEntity>)

    @Query("DELETE FROM cambios_caballo WHERE id IN (:ids)")
    suspend fun eliminar(ids: List<String>)
}

@Dao
interface CarreraDao {
    @Query(
        """
        SELECT
            c.id AS c_id, c.hipodromo AS c_hipodromo, c.fecha AS c_fecha, c.hora AS c_hora, c.numero AS c_numero,
            c.distancia_m AS c_distancia_m, c.superficie AS c_superficie, c.condicion_pista AS c_condicion_pista,
            c.serie AS c_serie, c.premio AS c_premio,
            p.id AS p_id, p.carrera_id AS p_carrera_id, p.caballo_id AS p_caballo_id, p.numero AS p_numero,
            p.jinete AS p_jinete, p.peso_kg AS p_peso_kg, p.posicion AS p_posicion, p.tiempo_seg AS p_tiempo_seg,
            p.dividendo AS p_dividendo,
            k.id AS k_id, k.corral_id AS k_corral_id, k.nombre AS k_nombre, k.sexo AS k_sexo, k.edad AS k_edad,
            k.pelaje AS k_pelaje, k.padre AS k_padre, k.madre AS k_madre, k.estado AS k_estado, k.foto_url AS k_foto_url,
            k.colores AS k_colores,
            (SELECT COUNT(*) FROM participaciones x WHERE x.carrera_id = c.id) AS corredores
        FROM participaciones p
        JOIN carreras c ON c.id = p.carrera_id
        JOIN caballos k ON k.id = p.caballo_id
        WHERE k.corral_id IS NOT NULL AND (:corralId IS NULL OR k.corral_id = :corralId)
          AND c.fecha >= :desde AND p.posicion IS NULL
        ORDER BY c.fecha, c.hora
        """
    )
    fun observarProximas(corralId: String?, desde: LocalDate): Flow<List<CarreraParticipacionFila>>

    @Query(
        """
        SELECT
            c.id AS c_id, c.hipodromo AS c_hipodromo, c.fecha AS c_fecha, c.hora AS c_hora, c.numero AS c_numero,
            c.distancia_m AS c_distancia_m, c.superficie AS c_superficie, c.condicion_pista AS c_condicion_pista,
            c.serie AS c_serie, c.premio AS c_premio,
            p.id AS p_id, p.carrera_id AS p_carrera_id, p.caballo_id AS p_caballo_id, p.numero AS p_numero,
            p.jinete AS p_jinete, p.peso_kg AS p_peso_kg, p.posicion AS p_posicion, p.tiempo_seg AS p_tiempo_seg,
            p.dividendo AS p_dividendo,
            k.id AS k_id, k.corral_id AS k_corral_id, k.nombre AS k_nombre, k.sexo AS k_sexo, k.edad AS k_edad,
            k.pelaje AS k_pelaje, k.padre AS k_padre, k.madre AS k_madre, k.estado AS k_estado, k.foto_url AS k_foto_url,
            k.colores AS k_colores,
            (SELECT COUNT(*) FROM participaciones x WHERE x.carrera_id = c.id) AS corredores
        FROM participaciones p
        JOIN carreras c ON c.id = p.carrera_id
        JOIN caballos k ON k.id = p.caballo_id
        WHERE p.caballo_id = :caballoId
        ORDER BY c.fecha DESC
        """
    )
    fun observarDelCaballo(caballoId: String): Flow<List<CarreraParticipacionFila>>

    @Query(
        """
        SELECT parciales.* FROM parciales JOIN participaciones p ON p.id = parciales.participacion_id
        WHERE p.caballo_id = :caballoId ORDER BY parciales.participacion_id, parciales.metros
        """
    )
    fun observarParcialesDelCaballo(caballoId: String): Flow<List<ParcialEntity>>
}

@Dao
interface CatalogoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarCorrales(filas: List<CorralEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarCaballos(filas: List<CaballoEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarPropietarios(filas: List<PropietarioEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarCopropiedad(filas: List<CaballoPropietarioEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarSesiones(filas: List<SesionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarMediciones(filas: List<MedicionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarTratamientos(filas: List<TratamientoEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarCarreras(filas: List<CarreraEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarParticipaciones(filas: List<ParticipacionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarParciales(filas: List<ParcialEntity>)

    @Query("DELETE FROM corrales") suspend fun borrarCorrales()
    @Query("DELETE FROM caballos") suspend fun borrarCaballos()
    @Query("DELETE FROM propietarios") suspend fun borrarPropietarios()
    @Query("DELETE FROM caballo_propietario") suspend fun borrarCopropiedad()
    @Query("DELETE FROM sesiones WHERE pendiente = 0") suspend fun borrarSesionesNoPendientes()
    @Query("DELETE FROM mediciones WHERE pendiente = 0") suspend fun borrarMedicionesNoPendientes()
    @Query("DELETE FROM tratamientos") suspend fun borrarTratamientos()
    @Query("DELETE FROM carreras") suspend fun borrarCarreras()
    @Query("DELETE FROM participaciones") suspend fun borrarParticipaciones()
    @Query("DELETE FROM parciales") suspend fun borrarParciales()

    /** Reemplaza todo lo que vino de afuera. Lo registrado en el teléfono (sesiones, mediciones, cambios) se conserva. */
    @Transaction
    suspend fun reemplazar(catalogo: Catalogo) {
        borrarCorrales(); borrarCaballos(); borrarPropietarios(); borrarCopropiedad()
        borrarSesionesNoPendientes(); borrarMedicionesNoPendientes(); borrarTratamientos()
        borrarCarreras(); borrarParticipaciones(); borrarParciales()
        insertarCorrales(catalogo.corrales)
        insertarCaballos(catalogo.caballos)
        insertarPropietarios(catalogo.propietarios)
        insertarCopropiedad(catalogo.caballoPropietario)
        insertarSesiones(catalogo.sesiones)
        insertarMediciones(catalogo.mediciones)
        insertarTratamientos(catalogo.tratamientos)
        insertarCarreras(catalogo.carreras)
        insertarParticipaciones(catalogo.participaciones)
        insertarParciales(catalogo.parciales)
    }
}
