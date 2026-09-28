# Fuentes de datos hípicos y perfil del preparador

Investigación del 25 sep 2026. Solo información pública y profesional.

## Qué publica el Club Hípico de Santiago

| Fuente | URL | Qué trae | Formato |
| --- | --- | --- | --- |
| Programa oficial (la "revista") | `https://static.clubhipico.cl/archivos/volantes/DD-MM-AAAA.pdf` | Por carrera: hora, distancia, pista, condición, premios. Por ejemplar: mandil, nombre, padrillo, peso del jinete, edad, jinete, preparador, índice, últimas 3 llegadas, datos de su última carrera (fecha, distancia, serie, cuerpos, tiempo, dividendo), peso físico, stud. En debutantes, padre y madre. | PDF, sin login. Se lee bien como texto. |
| Programa y resultados | https://www.clubhipico.cl/carreras/programa-y-resultados/ | Posición, mandil, jinete, preparador, peso, tiempo, cuerpos, dividendos, parciales | HTML dinámico (JavaScript) |
| GMAX | https://www.clubhipico.cl/carreras/gmax/ | Tiempo y velocidad cada 200 m | HTML dinámico. Muestra "No hay datos para esta carrera" cuando falta |
| Aprontes y huinchadas | https://www.clubhipico.cl/carreras/lista-de-aprontes-y-trabajos/ | Trabajos: ejemplar, distancia, tiempo | HTML dinámico. Estaba en mantenimiento al revisarlo |
| Estadísticas | https://www.clubhipico.cl/carreras/estadisticas/ | Ejemplares, jinetes, preparadores, studs | HTML dinámico y PDF histórico |

Otros sitios: Hipódromo Chile y elTurf (robots.txt permite acceso), Teletrak (programas), La Huasca (prensa).

### Condiciones de uso

- `clubhipico.cl/robots.txt` no existe.
- Los términos de uso **no mencionan el scraping**, pero reservan la propiedad intelectual: no conceden licencia sobre sus contenidos.
- Conclusión: está bien leer los programas para cargar a mano los datos del corral de un preparador que nos da su autorización, para una demo privada. Para **ingesta automática o redistribución** hay que pedir permiso al Club Hípico. Eso además podría convertirse en una alianza.

### Limitaciones para entrenar un modelo

- El programa trae el tiempo del ganador y los cuerpos de diferencia, **no el tiempo individual** de cada caballo. Se puede aproximar (≈ 0,2 s por cuerpo), pero es un supuesto.
- El pelaje y la madre no vienen, salvo en debutantes.
- GMAX y resultados se cargan por JavaScript. Extraerlos requiere un navegador automatizado, y eso entra de lleno en la pregunta de permisos.

## Cómo se usan en la app

1. Se descargan a mano los programas PDF y se guardan en `tools/programas/DD-MM-AAAA.pdf`. Esa carpeta no se sube a git.
2. `python tools/importar_programas.py` los convierte en `tools/datos/programas_chs.json` (carreras, corredores, llegadas, peso físico).
3. `python tools/generar_seed.py` arma el corral: los ejemplares de "C. Gonzalez M." con sus carreras y rivales reales.
4. Para refrescar antes de una reunión: descargar los programas nuevos, repetir los pasos 2 y 3, y subir `VERSION_SEED`.

## Cristóbal González M.

- **Preparador y veterinario** en el Club Hípico de Santiago. En los programas aparece como "C. Gonzalez M.". No encontramos a otro preparador con ese nombre en el CHS.
- Prepara principalmente para el stud Charito, y también para otros studs.
- Su jinete más frecuente es A. Apablaza. Tiene entre 4 y 8 inscripciones por reunión en septiembre de 2026.
- En el remate de Haras Paso Nevado compró a Kite (Tiz The Law) para el stud Charito, en un precio récord según [La Huasca](https://www.lahuasca.cl/los-hijos-de-tiz-the-law-marcaron-el-pulso).
- Preparó a Mr Casper, ganador del Handicap Hammurabi (CHS, 15-12-2025), según [Haras Convento Viejo](https://harasconventoviejo.com/noticias-ver?id_articulo=1362922).
- **No encontramos:** nombre propio del corral, estadísticas de temporada ni clásicos ganados.

### Ejemplares que prepara (programas oficiales de septiembre de 2026)

Sos de Boca (Robert Bruce, 5) · Bravo Nino (Cunco, 5) · Salertia (Tapitdoro, 4) · Pasión por Ella (Flyer, 5) · Golfista (Ivan Denisovich, 4) · Sontra (Dubai Sky, 4) · Classic Lia (Classic Empire, 4) · Qué Terrible (Seeking The Diamond, 6) · Canelo Dorado (Flameaway, 4) · Apura el Paso (Lookin At Lucky, 3, macho) · Oblak (War Command, 6) · Mr Yo (Mr Speaker, 6) · Mr Casper (Cupid, 6, macho alazán) · Lonwhite (Mendelssohn, 5) · Estilo Antiguo (Flameaway, 4) · Alta Gama (Tiz The Law, 3, hembra) · Anita la Ratita (Tiz The Law, 3, hembra, debuta 25-09).

Entre paréntesis va el padrillo y la edad. El detalle de las carreras recientes (fecha, distancia, mandil, jinete, peso, posición) está en los programas PDF de las fechas 04, 07, 11, 13, 21 y 25 de septiembre de 2026.

> Datos del Club Hípico: esta lista es solo de referencia interna. Si el repositorio de GitHub se hace público, hay que sacarla. Tampoco se redistribuyen sin permiso.
