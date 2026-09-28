package cl.stryda.app.data.remote

import cl.stryda.app.data.local.CaballoEntity
import cl.stryda.app.data.local.CaballoPropietarioEntity
import cl.stryda.app.data.local.CarreraEntity
import cl.stryda.app.data.local.Catalogo
import cl.stryda.app.data.local.CorralEntity
import cl.stryda.app.data.local.MedicionEntity
import cl.stryda.app.data.local.ParcialEntity
import cl.stryda.app.data.local.ParticipacionEntity
import cl.stryda.app.data.local.PropietarioEntity
import cl.stryda.app.data.local.SesionEntity
import cl.stryda.app.data.local.TratamientoEntity

/** Nombres de hoja en la planilla. */
object Hojas {
    const val CORRALES = "corrales"
    const val CABALLOS = "caballos"
    const val PROPIETARIOS = "propietarios"
    const val CABALLO_PROPIETARIO = "caballo_propietario"
    const val SESIONES = "sesiones"
    const val MEDICIONES = "mediciones"
    const val TRATAMIENTOS = "tratamientos"
    const val CARRERAS = "carreras"
    const val PARTICIPACIONES = "participaciones"
    const val PARCIALES = "parciales"

    val todas = listOf(
        CORRALES, CABALLOS, PROPIETARIOS, CABALLO_PROPIETARIO, SESIONES,
        MEDICIONES, TRATAMIENTOS, CARRERAS, PARTICIPACIONES, PARCIALES,
    )
}

/** Filas incompletas (sin id, sin fecha válida, etc.) se descartan: mejor un dato menos que un dato falso. */
fun mapearCatalogo(hojas: Map<String, TablaSheets>): Catalogo {
    fun filas(hoja: String) = hojas[hoja]?.filas.orEmpty()

    return Catalogo(
        corrales = filas(Hojas.CORRALES).mapNotNull { f ->
            CorralEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                nombre = f.texto("nombre") ?: return@mapNotNull null,
                preparador = f.texto("preparador").orEmpty(),
                hipodromo = f.texto("hipodromo").orEmpty(),
            )
        },
        caballos = filas(Hojas.CABALLOS).mapNotNull { f ->
            CaballoEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                corralId = f.texto("corral_id"),
                nombre = f.texto("nombre") ?: return@mapNotNull null,
                sexo = f.texto("sexo"),
                edad = f.entero("edad"),
                pelaje = f.texto("pelaje"),
                padre = f.texto("padre"),
                madre = f.texto("madre"),
                estado = f.texto("estado") ?: "activo",
                fotoUrl = f.texto("foto_url"),
                colores = f.texto("colores"),
            )
        },
        propietarios = filas(Hojas.PROPIETARIOS).mapNotNull { f ->
            PropietarioEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                nombre = f.texto("nombre") ?: return@mapNotNull null,
                email = f.texto("email"),
                telefono = f.texto("telefono"),
            )
        },
        caballoPropietario = filas(Hojas.CABALLO_PROPIETARIO).mapNotNull { f ->
            CaballoPropietarioEntity(
                caballoId = f.texto("caballo_id") ?: return@mapNotNull null,
                propietarioId = f.texto("propietario_id") ?: return@mapNotNull null,
                porcentaje = f.decimal("porcentaje") ?: return@mapNotNull null,
            )
        },
        sesiones = filas(Hojas.SESIONES).mapNotNull { f ->
            SesionEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                caballoId = f.texto("caballo_id") ?: return@mapNotNull null,
                fecha = f.fecha("fecha") ?: return@mapNotNull null,
                tipo = f.texto("tipo") ?: return@mapNotNull null,
                distanciaM = f.entero("distancia_m"),
                tiempoSeg = f.decimal("tiempo_seg"),
                vareador = f.texto("vareador"),
                pista = f.texto("pista"),
                estadoPista = f.texto("estado_pista"),
                esfuerzo = f.entero("esfuerzo")?.takeIf { it in 1..5 },
                notas = f.texto("notas"),
            )
        },
        mediciones = filas(Hojas.MEDICIONES).mapNotNull { f ->
            MedicionEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                caballoId = f.texto("caballo_id") ?: return@mapNotNull null,
                fecha = f.fecha("fecha") ?: return@mapNotNull null,
                tipo = f.texto("tipo") ?: return@mapNotNull null,
                valor = f.decimal("valor") ?: return@mapNotNull null,
            )
        },
        tratamientos = filas(Hojas.TRATAMIENTOS).mapNotNull { f ->
            TratamientoEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                caballoId = f.texto("caballo_id") ?: return@mapNotNull null,
                fecha = f.fecha("fecha") ?: return@mapNotNull null,
                producto = f.texto("producto") ?: return@mapNotNull null,
                via = f.texto("via"),
                veterinario = f.texto("veterinario"),
                diasRetiro = f.entero("dias_retiro") ?: 0,
                visibilidad = f.texto("visibilidad") ?: "resumida",
            )
        },
        carreras = filas(Hojas.CARRERAS).mapNotNull { f ->
            CarreraEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                hipodromo = f.texto("hipodromo") ?: return@mapNotNull null,
                fecha = f.fecha("fecha") ?: return@mapNotNull null,
                hora = f.texto("hora"),
                numero = f.entero("numero"),
                distanciaM = f.entero("distancia_m") ?: return@mapNotNull null,
                superficie = f.texto("superficie"),
                condicionPista = f.texto("condicion_pista"),
                serie = f.texto("serie"),
                premio = f.largo("premio"),
            )
        },
        participaciones = filas(Hojas.PARTICIPACIONES).mapNotNull { f ->
            ParticipacionEntity(
                id = f.texto("id") ?: return@mapNotNull null,
                carreraId = f.texto("carrera_id") ?: return@mapNotNull null,
                caballoId = f.texto("caballo_id") ?: return@mapNotNull null,
                numero = f.entero("numero"),
                jinete = f.texto("jinete"),
                pesoKg = f.decimal("peso_kg"),
                posicion = f.entero("posicion"),
                tiempoSeg = f.decimal("tiempo_seg"),
                dividendo = f.decimal("dividendo"),
            )
        },
        parciales = filas(Hojas.PARCIALES).mapNotNull { f ->
            ParcialEntity(
                participacionId = f.texto("participacion_id") ?: return@mapNotNull null,
                metros = f.entero("metros") ?: return@mapNotNull null,
                tiempoAcumSeg = f.decimal("tiempo_acum_seg") ?: return@mapNotNull null,
                velocidadMs = f.decimal("velocidad_ms") ?: return@mapNotNull null,
            )
        },
    )
}
