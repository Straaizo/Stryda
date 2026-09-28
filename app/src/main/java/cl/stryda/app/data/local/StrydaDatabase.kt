package cl.stryda.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

class Convertidores {
    @TypeConverter fun desdeEpochDay(valor: Long?): LocalDate? = valor?.let(LocalDate::ofEpochDay)
    @TypeConverter fun aEpochDay(fecha: LocalDate?): Long? = fecha?.toEpochDay()
}

@Database(
    entities = [
        CorralEntity::class, CaballoEntity::class, PropietarioEntity::class, CaballoPropietarioEntity::class,
        SesionEntity::class, MedicionEntity::class, TratamientoEntity::class, CarreraEntity::class,
        ParticipacionEntity::class, ParcialEntity::class, CambioCaballoEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class StrydaDatabase : RoomDatabase() {
    abstract fun corralDao(): CorralDao
    abstract fun caballoDao(): CaballoDao
    abstract fun sesionDao(): SesionDao
    abstract fun saludDao(): SaludDao
    abstract fun carreraDao(): CarreraDao
    abstract fun catalogoDao(): CatalogoDao
    abstract fun cambioDao(): CambioDao

    companion object {
        fun crear(context: Context): StrydaDatabase =
            Room.databaseBuilder(context, StrydaDatabase::class.java, "stryda.db")
                // Desde v2 hay datos cargados a mano: cada cambio de esquema lleva su migración, nunca se borra
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()

        /** v3: colores de la chaquetilla. */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE caballos ADD COLUMN colores TEXT")
            }
        }

        /** v2: mediciones registradas a mano y cambios manuales del caballo. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mediciones ADD COLUMN pendiente INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cambios_caballo (
                        id TEXT NOT NULL PRIMARY KEY,
                        caballo_id TEXT NOT NULL,
                        instante_ms INTEGER NOT NULL,
                        campo TEXT NOT NULL,
                        valor_anterior TEXT,
                        valor_nuevo TEXT,
                        nota TEXT,
                        pendiente INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cambios_caballo_caballo_id ON cambios_caballo (caballo_id)")
            }
        }
    }
}
