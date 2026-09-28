package cl.stryda.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigracionTest {

    @get:Rule
    val ayudante = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), StrydaDatabase::class.java)

    @Test
    fun migracion1a2ConservaLosDatosYAgregaLasTablasNuevas() {
        ayudante.createDatabase(BASE, 1).use { db ->
            db.execSQL("INSERT INTO mediciones (id, caballo_id, fecha, tipo, valor) VALUES ('m1', 'k1', 20000, 'peso_kg', 485.5)")
        }
        ayudante.runMigrationsAndValidate(BASE, 2, true, StrydaDatabase.MIGRACION_1_2).use { db ->
            db.query("SELECT valor, pendiente FROM mediciones WHERE id = 'm1'").use { c ->
                c.moveToFirst()
                assertEquals(485.5, c.getDouble(0), 0.001)
                assertEquals(0, c.getInt(1))
            }
            db.execSQL(
                "INSERT INTO cambios_caballo (id, caballo_id, instante_ms, campo, valor_anterior, valor_nuevo, nota) " +
                    "VALUES ('c1', 'k1', 1, 'estado', 'activo', 'lesionado', NULL)",
            )
            db.query("SELECT pendiente FROM cambios_caballo WHERE id = 'c1'").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
        }
    }

    @Test
    fun migracion2a3AgregaColoresSinPerderCaballos() {
        ayudante.createDatabase(BASE, 2).use { db ->
            db.execSQL("INSERT INTO caballos (id, corral_id, nombre, estado) VALUES ('k1', 'c01', 'Sos de Boca', 'activo')")
        }
        ayudante.runMigrationsAndValidate(BASE, 3, true, StrydaDatabase.MIGRACION_2_3).use { db ->
            db.query("SELECT nombre, colores FROM caballos WHERE id = 'k1'").use { c ->
                c.moveToFirst()
                assertEquals("Sos de Boca", c.getString(0))
                assertEquals(true, c.isNull(1))
            }
        }
    }

    private companion object {
        const val BASE = "migracion-test.db"
    }
}
