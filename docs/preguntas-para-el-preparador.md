# Preguntas y supuestos para validar con el preparador

Lista viva. Cada supuesto que tomamos al construir queda acá para confirmarlo en la reunión o durante la semana de prueba.

## Preguntas abiertas

- ¿Mandaría una **nota de voz** como detalle del trabajo de un ejemplar? Hoy usa audios de WhatsApp.
- ¿Reconoce a los caballos por su nombre oficial o por un **nombre de establo / apodo**?
- ¿Cuánto se demora hoy en anotar el trabajo de todo el corral? ¿En qué lo anota?
- ¿Qué le manda a los dueños, cada cuánto y cuánto tiempo le toma?
- ¿Le interesa que el dueño vea todo lo que hace el caballo?

## Supuestos tomados en la demo

| Supuesto | Dónde vive | Cómo confirmarlo |
| --- | --- | --- |
| Unidades de carga por tipo: descanso 0, caminadora 1, vareo 1, nado 1.5, trote 2, galope suave 3, galope fuerte 4, apronte 5, huinchada 5. El esfuerzo (1–5) escala la carga; sin esfuerzo cuenta como 3. | `domain/Modelos.kt`, `domain/Carga.kt` | ¿Un apronte "pesa" lo mismo que una huinchada? ¿El nado cuenta como trabajo? |
| "Carga alta" = últimas 2 semanas ≥ 30% sobre su promedio quincenal de los 3 meses previos. | `domain/Carga.kt` | ¿Qué porcentaje le parecería razonable para llamarle la atención? |
| Retiro: el día del tratamiento es el día 0. Con 10 días aplicados el 1 de marzo, el retiro dura hasta el 11 y queda apto el 12. | `domain/Salud.kt` | Confirmar cómo cuentan los días de retiro en la práctica y en el reglamento. |
| Indicador extra "En descanso" para ejemplares con estado `descansando` (el documento no lo lista). | `domain/IndicadorEstado.kt` | ¿Lo quiere ver así o como "Entrena normal"? |
| Lista de Mi corral en orden alfabético (estable) en vez de por urgencia. La franja y los indicadores marcan lo urgente. | `data/local/Daos.kt` | ¿Prefiere que los que necesitan atención vayan arriba? |
| Ejemplares `retirado` no aparecen en Mi corral. | `data/local/Daos.kt` | ¿Los quiere ver en alguna parte? |
| "Corre pronto" = carrera en los próximos 7 días (hoy incluido). | `domain/IndicadorEstado.kt` | ¿Siete días es la ventana correcta? |
| Peso "antiguo" = última medición de hace más de 30 días. | `domain/Rendimiento.kt` | ¿Cada cuánto pesan en su corral? |
| La variación de peso compara con la última medición de hace 28 días o más. | `domain/Rendimiento.kt` | — |
| Warning de la paleta se oscureció a `#8F5A0A` en modo claro: `#B7791F` no cumple contraste 4.5:1. | `ui/theme/Color.kt` | Decisión de accesibilidad, no requiere confirmación. |
| El 12sp de la escala tipográfica no se usa: manda la regla de nunca bajar de 14sp. | `ui/theme/Type.kt` | — |
| Tiempo de cada caballo = tiempo de referencia del programa + 0,2 s por cuerpo de diferencia. El programa no trae el tiempo individual. | `tools/generar_seed.py` | ¿Cuántos segundos por cuerpo usa él? ¿Tiene acceso a los tiempos oficiales de cada caballo? |
| Las carreras anteriores a los programas cargados aparecen como "Hipódromo sin dato": el programa no dice dónde se corrió esa última carrera. | `tools/generar_seed.py` | Confirmar con él o con los resultados del Club Hípico. |
| Pelaje y sexo quedan en blanco salvo que el programa o una fuente pública los indique. Él los completa desde la ficha ("Completar pelaje y sexo"). | Ficha | ¿Le molesta completarlos o prefiere que vengan cargados? |
| La estimación mezcla distancias (Riegel). Con pocas carreras a distancias distintas, el rango sale amplio. | `domain/Estimacion.kt` | ¿Le sirve ver el rango o lo confunde? |
| El conteo de ejemplares en "Mis corrales" usa el estado de la planilla, no el editado a mano. Solo se nota con más de un corral. | `data/local/Daos.kt` | — |
