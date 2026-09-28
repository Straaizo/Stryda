package cl.stryda.app.data.remote

import cl.stryda.app.data.local.Catalogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

internal interface SheetsApi {
    @GET("spreadsheets/d/{id}/gviz/tq")
    suspend fun hoja(
        @Path("id") sheetId: String,
        @Query("sheet") nombre: String,
        @Query("tqx") formato: String = "out:json",
    ): ResponseBody
}

/**
 * Lo único en la app que sabe que los datos vienen de Google Sheets.
 * Al migrar a Supabase se reemplaza este paquete manteniendo [FuenteRemota].
 */
interface FuenteRemota {
    val configurada: Boolean

    /** Descarga todas las hojas. Lanza excepción si cualquiera falla: nunca se aplica un catálogo a medias. */
    suspend fun descargarCatalogo(): Catalogo
}

class SheetsClient(private val sheetId: String) : FuenteRemota {

    override val configurada: Boolean get() = sheetId.isNotBlank()

    private val api: SheetsApi by lazy {
        val http = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://docs.google.com/")
            .client(http)
            .build()
            .create(SheetsApi::class.java)
    }

    override suspend fun descargarCatalogo(): Catalogo = withContext(Dispatchers.IO) {
        check(configurada) { "SHEET_ID no configurado" }
        val hojas = coroutineScope {
            Hojas.todas.map { nombre ->
                async { nombre to parsearRespuestaGviz(api.hoja(sheetId, nombre).use { it.string() }) }
            }.awaitAll()
        }.toMap()
        mapearCatalogo(hojas)
    }
}
