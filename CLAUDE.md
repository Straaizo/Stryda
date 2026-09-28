# Stryda — Reglas del proyecto (demo Android)

> Fuente: "Stryda — Prompt para Claude Code (demo Android)", 25 sep 2026, @Enzo. Secciones 2 a 9.
> Toda decisión, cambio o supuesto se registra en `bitacora.md` (ver regla al final).

## Contexto y objetivo

Estás construyendo **Stryda**, una **demo funcional** de una aplicación para corrales de caballos fina sangre de carrera en Chile (Club Hípico de Santiago, Hipódromo Chile, Valparaíso Sporting).

**Quién la usa:** el preparador de un corral, desde su teléfono, a las 6 de la mañana, con las manos sucias y mala señal. No es un usuario de software. Tiene entre 40 y 70 años y lleva décadas en el oficio.

**Qué resuelve:** hoy el preparador le manda notas de voz por WhatsApp a los dueños de los caballos para contarles qué se está haciendo con su animal. Esta app registra el trabajo diario en segundos y genera automáticamente un reporte visual para el propietario.

**Qué es esta demo:** una maqueta navegable con datos de prueba, para mostrársela a un preparador real y recoger su opinión sobre qué agregar, cambiar y eliminar. No es producción. No maneja datos reales ni sensibles.

**Cómo se juzga el resultado:** el preparador tiene que poder tomar el teléfono sin explicación previa y entender qué está viendo. Si necesita que alguien le explique una pantalla, esa pantalla está mal.

El producto final se construirá en Kotlin con Jetpack Compose y Supabase, o sea el mismo stack de esta demo. Por eso el sistema de diseño, los cálculos de dominio y las pantallas se escriben con calidad de producción: lo único que cambia después es la capa de datos, de Google Sheets a Supabase.

## Stack y arquitectura

Android nativo. Se compila en Android Studio y se distribuye como APK para instalar en el teléfono.

### Obligatorio

- **Kotlin** + **Jetpack Compose** + **Material 3**
- `minSdk 26`, `targetSdk` la más reciente estable
- **Navigation Compose** para navegación
- **Room** para caché local (la app debe funcionar sin red)
- **Retrofit + OkHttp + kotlinx.serialization** para traer los datos de Sheets
- **Coil** para imágenes
- **Vico** para gráficos (es nativo de Compose; MPAndroidChart está hecho para Views y no corresponde)
- **JUnit + Compose UI Test** para pruebas
- Inyección de dependencias **manual**, con un contenedor simple. Hilt agrega ceremonia y tiempo de compilación que una demo no necesita.

### Prohibido

- Layouts XML. Todo en Compose.
- Librerías de UI de terceros que impongan su propia estética.
- Datos reales de tratamientos, propietarios o veterinarios.
- Dependencias fuera de esta lista sin justificarlo en un comentario.

### Reglas de arquitectura

**1. La capa de datos está aislada, y esto no es negociable.**
Todo lo que sabe de Google Sheets vive en `data/remote/`. El resto de la app consume repositorios que devuelven objetos de dominio. Cuando esto migre a Supabase, se reescribe esa carpeta y nada más.

**2. La app tiene que abrir y funcionar sin señal.**
Requisito duro, porque la demo se muestra en un corral. Tres capas:
- Un `seed.json` en `assets/` con el set completo de datos de prueba. Si Room está vacío al primer arranque, se carga desde ahí.
- Room es la **única** fuente de verdad para la UI. Las pantallas leen de Room vía `Flow`, nunca de la red directamente.
- La sincronización desde Sheets es oportunista: si hay red, actualiza Room en segundo plano. Si falla, no pasa nada y la app sigue andando.

Probar en modo avión antes de dar cualquier pantalla por terminada.

**3. Cálculos derivados, nunca almacenados.**
Velocidad máxima, peso actual, tiempo récord y carga semanal se calculan desde los eventos, en funciones puras dentro de `domain/`. No son columnas.

**4. Estructura de paquetes**

```
cl.stryda.app/
  data/
    remote/      ← lo único que conoce Google Sheets
    local/       ← Room: entidades, DAOs, base
    repository/  ← CaballoRepository, SesionRepository...
  domain/        ← modelos + cálculos puros
  ui/
    theme/       ← colores, tipografía, formas
    components/  ← componentes reutilizables
    screens/     ← un paquete por pantalla
```

`applicationId` = `cl.stryda.app`. Para la build de demo, `applicationIdSuffix = ".demo"`.

**5. Una `ViewModel` por pantalla**, exponiendo un único `StateFlow<UiState>` con estados explícitos de `Cargando`, `Contenido` y `Vacio`. Nada de booleanos sueltos de `isLoading`.

### Distribución

**Firebase App Distribution** para mandarle versiones nuevas al preparador sin juntarse con él.

## Datos: Google Sheets

### Cómo conectarse

Planilla en lectura pública vía endpoint gviz, sin autenticación:

```
https://docs.google.com/spreadsheets/d/{SHEET_ID}/gviz/tq?tqx=out:json&sheet={NOMBRE_HOJA}
```

La respuesta viene envuelta en `google.visualization.Query.setResponse(...)`; recortar prefijo y sufijo antes de parsear. Eso vive en `data/remote/SheetsClient.kt` y en ningún otro lugar.

El `SHEET_ID` va en `local.properties` y se expone vía `BuildConfig`. `local.properties` no se sube al repositorio.

### Hojas y columnas

- **`corrales`** — `id`, `nombre`, `preparador`, `hipodromo`
- **`caballos`** — `id`, `corral_id`, `nombre`, `sexo`, `edad`, `pelaje`, `padre`, `madre`, `estado`, `foto_url`. `estado`: `activo`, `lesionado`, `descansando`, `retirado`.
- **`propietarios`** — `id`, `nombre`, `email`, `telefono`
- **`caballo_propietario`** — `caballo_id`, `propietario_id`, `porcentaje`. Un caballo puede tener varios dueños; la UI muestra los porcentajes.
- **`sesiones`** — `id`, `caballo_id`, `fecha`, `tipo`, `distancia_m`, `tiempo_seg`, `vareador`, `pista`, `estado_pista`, `esfuerzo`, `notas`. `tipo`: `vareo`, `trote`, `galope_suave`, `galope_fuerte`, `apronte`, `huinchada`, `nado`, `caminadora`, `descanso`. `esfuerzo` 1 a 5.
- **`mediciones`** — `id`, `caballo_id`, `fecha`, `tipo`, `valor`. `tipo`: `peso_kg`, `temperatura`, `fc_reposo`.
- **`tratamientos`** — `id`, `caballo_id`, `fecha`, `producto`, `via`, `veterinario`, `dias_retiro`, `visibilidad` (`completa` o `resumida`).
- **`carreras`** — `id`, `hipodromo`, `fecha`, `hora`, `numero`, `distancia_m`, `superficie`, `condicion_pista`, `serie`, `premio`
- **`participaciones`** — `id`, `carrera_id`, `caballo_id`, `numero`, `jinete`, `peso_kg`, `posicion`, `tiempo_seg`, `dividendo`
- **`parciales`** — `participacion_id`, `metros`, `tiempo_acum_seg`, `velocidad_ms`. Simula GMAX cada 200 m. **Solo Club Hípico tiene parciales.** Hipódromo Chile sin filas, a propósito.

### Datos de prueba a generar

Un corral con **12 caballos**, seis meses de historia, realista, no uniforme:
- Uno en gran forma, con tiempos mejorando
- Uno desmejorando visiblemente
- Uno lesionado, sin sesiones hace tres semanas
- Uno en período de retiro por tratamiento
- Un debutante sin carreras aún
- Uno con tres propietarios en porcentajes
- Al menos dos con carrera programada en los próximos tres días
- Huecos deliberados: sesiones sin tiempo, un peso faltante, una carrera sin parciales

Nombres en español chileno, plausibles. Nada de "Caballo 1", "Test Horse".

## Pantallas

Construir en este orden. Una pantalla terminada y revisada antes de empezar la siguiente.

### 1. Mis corrales
Lista de corrales con nombre, hipódromo y número de caballos. **Si hay un solo corral, saltarse esta pantalla y entrar directo a Mi corral.** Dejarla implementada.

### 2. Mi corral
Lista de caballos. Cada fila: foto pequeña (o iniciales), nombre, edad y sexo, y **un solo indicador de estado**. Nada más.
Indicador: `Entrena normal`, `Carga alta`, `En tratamiento`, `Lesionado`, `Corre en N días`. Prioridad: corre pronto > lesionado > tratamiento > carga > normal.
No poner velocidad ni peso en la fila.
Arriba, franja con caballos que corren en los próximos 7 días; si no hay, la franja no existe.
Botón flotante prominente: **Registrar trabajo de hoy**.

### 3. Registrar trabajo (la más importante)
Objetivo: **12 caballos en menos de 2 minutos, sin teclado salvo números.**
1. Selector de tipo con chips grandes.
2. Lista completa de caballos con checkbox, **todos preseleccionados**.
3. Elegir tipo, deseleccionar excepciones, confirmar → sesión para todos los seleccionados.
4. Tocar un caballo para detalle opcional: distancia, tiempo, vareador, esfuerzo, nota, foto.

Numéricos con `KeyboardType.Number`. Tiempo como `mm:ss.d`.

### 4. Ficha del caballo
Pestañas: **Resumen · Entrenamiento · Salud · Campaña · Datos**
- **Resumen.** Foto, nombre, estado. Cuatro números grandes: peso actual con variación vs mes pasado, velocidad máxima, mejor tiempo por distancia, días desde la última carrera. Tarjeta de próxima carrera si corre pronto. Observaciones automáticas.
- **Entrenamiento.** 1) Carga semanal (barras apiladas por tipo, 12 semanas). 2) Evolución de tiempos (aprontes y huinchadas, por distancia, contra su propia línea base; **nunca contra otros caballos**). 3) Peso (línea con banda de rango normal). Debajo, timeline de sesiones.
- **Salud.** Peso, tratamientos con período de retiro, semáforo de elegibilidad.
- **Campaña.** Historial de carreras; detalle con perfil de velocidad si hay parciales, si no *"Sin datos de parciales para esta carrera"*.
- **Datos.** Propietarios con porcentajes, genealogía, corral, fecha de nacimiento.

### 5. Próxima carrera
Hora, hipódromo, número, distancia, superficie, jinete, peso, número de partida.
**Rivales de cuidado**: hasta tres, con índice de forma y una línea de por qué.
Al pie, obligatorio: *"Análisis basado en N de M rivales con datos completos"*.

### 6. Observaciones automáticas
Motor de reglas en `domain/Observaciones.kt`, ventanas móviles vs línea base del propio caballo.
**Observar, nunca recomendar.**
- ❌ "Deberías trabajarlo más largo" · ✅ "En sus 4 mejores actuaciones venía de 3+ galopes largos en las dos semanas previas. Este mes lleva 1."
- ❌ "Está sobrecargado" · ✅ "Su carga de las últimas 2 semanas está 40% sobre su promedio de 3 meses."

Cada observación lleva botón discreto **"No aplica"**.

### 7. Reporte del propietario (vista pública)
Desde la ficha con **Ver como lo ve el dueño**. Pantalla completa, **sin barra de navegación**.
Contiene: nombre, foto, últimas 4 semanas de actividad, los tres gráficos, fotos/videos del preparador, logo del corral arriba.
No contiene: fármacos, dosis, ni nada de otros caballos.

## Sistema de diseño

Construir **primero**. Vive en `ui/theme/`: `ColorScheme` M3 claro y oscuro, `Typography`, `Shapes`. **Ningún Composable usa `Color(0xFF...)` literal**: todo sale de `MaterialTheme.colorScheme` o de `LocalAppColors`.

### Marca
- Se escribe siempre **Stryda** (nunca STRYDA ni Striyda). Bajada: *gestión y seguimiento de corrales de carrera*.
- En la app solo aparece en splash y Ajustes.
- El reporte del propietario lleva arriba el **nombre del corral**; Stryda discreta al pie.

### Paleta
Verde profundo, lejos del `#5B9B59` de Club Hípico.

| Token | Claro | Oscuro |
| --- | --- | --- |
| `bg` | `#FBFCFB` | `#0D1411` |
| `surface` | `#FFFFFF` | `#161F1A` |
| `border` | `#E4E9E5` | `#26332C` |
| `text` | `#12201A` | `#E8EFEA` |
| `text-muted` | `#5C6B63` | `#94A69B` |
| `primary` | `#0E7A4F` | `#3DD68C` |
| `primary-hover` | `#0A6141` | `#5FE3A3` |
| `primary-soft` | `#E6F4ED` | `#152A20` |

Semánticos (ajustar luminosidad por modo): `success #0E7A4F` · `warning #B7791F` · `danger #B42318` · `info #1E6FA8`

### Reglas de color
- El verde es para acción y lo positivo. Nunca para todo.
- El color nunca es el único portador de significado (ícono + texto).
- Modo oscuro real, fondos con tinte verde.
- Sigue al sistema (`isSystemInDarkTheme()`), forzable en Ajustes, persistido en `DataStore`.
- **No usar `dynamicColor`.**

### Tipografía
**Inter** (respaldo: sistema). Números con `FontFeatureSetting("tnum")`.
Escala `12 · 14 · 16 · 20 · 24 · 32 · 48` sp. Cuerpo 16sp, **nunca menos de 14sp**. `sp` para texto, `dp` para todo lo demás.
Números importantes en 32 o 48, semibold.

### Espaciado y forma
Escala 4dp: `4 · 8 · 12 · 16 · 24 · 32 · 48`. Radio: `8dp` tarjetas, `12dp` modales/hojas, `999dp` chips. Preferir borde 1dp sobre elevación.

### Objetivos táctiles
**Mínimo 48×48dp. Sin excepciones.** `Modifier.minimumInteractiveComponentSize()` o padding explícito.

## Reglas de UX/UI

Si una decisión de diseño choca con una de estas, gana la regla.

### Contexto de uso
6 AM, de pie, una mano, sol directo o poca luz, guantes, mala señal.
- Acciones primarias en el tercio inferior. Navegación en barra inferior, nunca menú lateral.
- Nada crítico detrás de scroll horizontal ni gestos a descubrir.
- **Sin señal es el caso normal.** Nunca pantalla de error por red; indicador discreto de última actualización.

### Jerarquía
- Un solo elemento dominante por pantalla.
- Máximo tres niveles: Corral → Caballo → Detalle.
- Números grandes; etiquetas chicas en `text-muted`.

### Estados
Cada pantalla y componente con datos implementa **carga, contenido, vacío y error**. Vacío = texto explicativo + botón de acción. Cargas con **skeletons** (shimmer), no spinners.

### Escritura
- Español de Chile, vocabulario del turf: ejemplar, corral, preparador, vareador, apronte, huinchada, jornada, partidor, campaña.
- Sin jerga de software ("sincronizando", "registro creado exitosamente", "error 404").
- Mensajes dicen qué pasó y qué hacer: `"No se pudo guardar. Queda pendiente y se sube cuando haya señal."`
- Fechas relativas: *hoy*, *ayer*, *hace 3 días*, luego `12 mar`.

### Confirmaciones y errores
- Acción destructiva = se ejecuta + snackbar **Deshacer** 5 s. Sin diálogos de confirmación.
- Guardado optimista; pendiente nunca se pierde ni bloquea.
- Un error jamás borra lo que el usuario escribió.

### Retroalimentación
Respuesta visible < 100 ms. Háptica suave al confirmar registro.

### Accesibilidad
Contraste ≥ 4.5:1 en ambos temas. `contentDescription` en todo interactivo; probar con TalkBack. Fuente al 130% sin cortes. Respetar animación reducida.

### Visibilidad de tratamientos (regla de producto)
- **Preparador** ve todo: producto, dosis, vía, veterinario, fecha.
- **Propietario** ve solo el estado: `En tratamiento`, `En período de retiro hasta el 14 de marzo`, `Apto para correr`. **Nunca fármaco ni dosis.**

### Prohibiciones
Modales para cosas que caben en pantalla · tooltips · scroll infinito en listas filtrables · placeholders como etiquetas · párrafos centrados · emojis como íconos.

## Animación

### El caballo corriendo
Solo en **splash** y **cargas > ~400 ms**. **Rive** (`app.rive:rive-android`, init en `Application`, `res/raw/caballo.riv`). Respaldos: Lottie, luego sprite sheet de Muybridge.
- Máquina de estados: `cargando` (bool) + `intensidad` (0–1). Desacelera al terminar, sin cortes.
- Envuelto en `CaballoGalopando`; el resto de la app no sabe que existe Rive.
- Con animación reducida: primer fotograma estático. Verificarlo.
- Precargar el `.riv`.

### Micro-animación
Gráficos crecen al entrar (≈600 ms, ease-out, una vez) · números cuentan hacia arriba · pestañas con `AnimatedContent` · chips con escala leve · skeletons con shimmer.

### Duraciones
Micro 150–250 ms · pantallas 250–350 ms · gráficos ≤ 600 ms. **Nada supera 600 ms.**

### Prohibiciones
Caballo fuera de splash/carga · rebotes, elásticos, overshoot · parallax o animaciones ligadas al scroll. Con animación reducida: instantáneo o fade mínimo.

## QA y testing

No se entrega una pantalla sin probarla.

### Unitarias (obligatorias en `domain/`)
- Carga semanal con semanas vacías intercaladas
- Velocidad máxima sin ninguna sesión con tiempo
- Peso actual con última medición de hace tres meses
- Tiempo récord por distancia con una sola muestra
- Días de retiro que cruzan de mes
- Índice de forma con datos parciales o ausentes
- Cada regla de observaciones: caso que dispara y caso que no
- Parseo de Sheets: celda vacía, coma decimal, fecha mal formada, fila incompleta

### UI (Compose UI Test)
- Registro en lote: tipo, deseleccionar dos, confirmar, verificar sesiones exactas
- Navegación completa: corral → caballo → pestañas → próxima carrera → volver
- Cambio de tema sin crash
- Estado vacío de cada pantalla

### Checklist manual por pantalla (reportar cada punto)
1. Modo avión (bloqueante) · 2. Pantalla 5" densidad alta · 3. Fuente 130% · 4. Modo oscuro · 5. Táctiles ≥ 48dp · 6. Datos faltantes · 7. Rotación · 8. Doble toque · 9. TalkBack · 10. Hoja vacía

### Build
`./gradlew lint testDebugUnitTest` limpio. Sin `TODO`, sin `println`, sin logs de depuración.

### Reporte por pantalla
Qué se construyó, qué pruebas pasaron, qué quedó pendiente y **qué supuestos hay que confirmar**.

## Criterios de aceptación

**Funcional:** APK abre < 2 s en gama media · completo en modo avión desde la primera apertura · 12 caballos en < 2 min cronometrado · 7 pantallas sin crashes · gráficos verificables contra la planilla · reporte del propietario sin fármacos.

**Calidad:** lint + tests limpios · cero crashes en recorrido completo · claro y oscuro revisados · checklist de 10 puntos por pantalla.

**La prueba real:** alguien sin contexto registra el trabajo de un caballo y encuentra cuándo corre, sin ayuda.

### Fuera de alcance
Login/cuentas · backend propio · ingesta real de hipódromos · push · **escritura a Sheets (demo solo lectura; registros nuevos viven en Room)** · ML · subida de video.
Si algo parece necesario, anotarlo como pregunta en el reporte.

## Decisiones aprobadas por Enzo (25 sep 2026)

Complementan las secciones anteriores. Si chocan, gana esta sección.

1. **Registrar trabajo: excepciones con otro tipo.** Además de desmarcar, tocar una fila permite cambiarle el tipo solo a ese ejemplar en el mismo lote. El botón resume: *"Registrar · 9 vareo, 2 apronte"*.
2. **Teclado propio para el tiempo.** Teclado numérico grande dentro de la pantalla; `1234` se formatea solo como `1:23.4`. Reemplaza al teclado del sistema **solo para el tiempo**; distancia y demás números siguen con `KeyboardType.Number`.
3. **Tipo preseleccionado inteligente.** Se sugiere el tipo más usado ese mismo día de la semana en las últimas semanas.
4. **Barra inferior de 3 pestañas:** Corral · Carreras · Ajustes. "Registrar trabajo de hoy" es botón flotante centrado.
5. **Preguntas para la reunión** (no se construyen): se anotan en `docs/preguntas-para-el-preparador.md`.
6. **Build de demo = buildType debug** con `applicationIdSuffix = ".demo"`.
7. **Caballo animado:** mientras no exista `caballo.riv`, se usa el respaldo de Muybridge detrás de `CaballoGalopando`: 11 cuadros **vectoriales** (`res/drawable/caballo_galope_*.xml`) trazados con `tools/vectorizar_caballo.py` desde el escaneo en alta resolución. Nítidos en cualquier densidad.
8. **Proyecto personal de Enzo.** **Git lo maneja Enzo:** Claude no hace `commit`, `push`, remotos ni repositorios en GitHub. Al terminar una tarea, lista los archivos cambiados y sugiere un mensaje de commit. Sus commits van con su correo personal, no el de La Hornilla.

### 25 sep 2026, segunda sesión

9. **El caballo galopando también va en la cabecera de la ficha** (reemplaza la prohibición de la sección Animación). Galopa ~2,4 s al entrar, desacelera y se detiene; tocarlo lo hace galopar otra vez. Sigue fuera de la lista, los gráficos y los fondos.
10. **La ficha muestra, en este orden:** caballo, nombre, pelaje · sexo · edad, indicador de estado, elegibilidad; peso, velocidad máxima en trabajos, peak en carrera (GMAX), días desde la última carrera; última carrera (posición, corredores, tiempo, número de mandil, jinete, peso); próxima carrera; estimación. Velocidades en km/h.
11. **Estimación:** `domain/Estimacion.kt`, estadística sobre el propio historial del caballo (Riegel + tendencia, mínimo 3 carreras con tiempo). Siempre dice con cuántas carreras se calculó y que no es un pronóstico. Detrás de la interfaz `Estimador`, para enchufar un modelo entrenado (ML) cuando existan datos reales suficientes. No se entrena ML con datos sintéticos.
12. **Corral:** "Corral Cristóbal González", preparador Cristóbal González M. (preparador real del Club Hípico). Ejemplares reales desde el 25 sep (decisión 17).
13. **Seed versionado:** subir `VERSION_SEED` en `tools/generar_seed.py` al cambiar los datos; la app lo reemplaza sola si el teléfono sigue con datos de prueba.

### 25 sep 2026, tercera sesión

14. **El caballo galopa en loop mientras la ficha esté abierta** (reemplaza la decisión 9). Solo en la ficha y el splash; con animación reducida queda quieto.
15. **Edición manual desde la ficha:**
    - Qué se edita: estado, pelaje, sexo, edad, padre y madre.
    - Cómo se guarda: cada cambio es un evento en la tabla `cambios_caballo`. El dato vigente se deriva aplicando el último cambio de cada campo (`aplicarCambios`); el dato base nunca se sobrescribe.
    - Mediciones manuales (peso, temperatura, frecuencia en reposo): se guardan con `pendiente = 1`.
    - Nada de esto se borra al recargar el seed o la planilla.
    - Todo cambio se puede deshacer durante 5 s.
    - Este historial es la base del futuro modelo entrenado para el corral.
16. **Migraciones:** desde la v2 hay datos cargados a mano. Nunca se usa migración destructiva al subir de versión: cada cambio de esquema lleva su `Migration` y su prueba en `MigracionTest`.
17. **Datos reales:**
    - Los 17 ejemplares de Cristóbal González salen de los programas oficiales del Club Hípico: PDF en `tools/programas/` (no se versionan) → `tools/importar_programas.py` → `tools/datos/programas_chs.json` → `tools/generar_seed.py`.
    - Reales: ejemplares, padrillo, edad, carreras, posiciones, mandil, jinete, peso físico y rivales.
    - Aproximado: el tiempo de cada caballo (0,2 s por cuerpo).
    - Ficticios: trabajos diarios, dueños y tratamientos de rutina.
    - **Nunca inventar lesiones, retiros ni parciales sobre caballos reales.**
18. **Franja "Corren esta semana":** máximo 3 carreras y un enlace "Ver las N carreras" a la pestaña Carreras. No repite el hipódromo cuando es el del corral.

### 25 sep 2026, cuarta sesión

19. **Los cuadros de la ficha se pueden tocar** y abren una hoja inferior:
    - **Peso:** formulario de pesaje (temperatura y frecuencia opcionales) y debajo el historial: rango en 6 meses, peso en su mejor carrera, gráfico Vico y lista con variaciones.
    - **Velocidad máxima:** registrar un trabajo cronometrado (apronte o huinchada, distancia con botones, tiempo con solo números: `486` = 48.6) y sus 5 trabajos más rápidos.
    - **Peak en carrera:** qué es GMAX y el dato, si lo hay.
    - **Días desde su última carrera:** todas sus carreras.
    - La tarjeta "Última carrera" también abre la lista de carreras.
20. **Gráficos con Vico 2.1.4.** Una sola serie en el color primario, sin leyenda; tocar un punto muestra el valor; la lista debajo es la vista en tabla.
21. **APK:** `.\tools\generar_apk.ps1` deja `dist\Stryda-<versión>-demo.apk`, y con `-Instalar` lo instala por USB. `dist/` no se versiona.

### 25 sep 2026, quinta sesión

22. **Sin barra de acciones abajo en la ficha:** los cuadros y la tarjeta de estado ya son las acciones. Se explica en pantalla ("Toca un cuadro…", "Toca para cambiar el estado").
23. **Hojas inferiores sin gesto de arrastre** (`sheetGesturesEnabled = false`, Material 3 1.4.0 fijado): el dedo desplaza el contenido y la hoja no se cierra por accidente. Se cierran con la ✕, con "atrás" o tocando fuera.
24. **Ficha en PDF** (`FichaPdf.kt`):
    - Se genera en el teléfono (A4) y se comparte con el menú de Android.
    - Arriba: corral y preparador. Stryda discreta al pie.
    - Contenido: datos, colores, estado y aptitud, cifras, próxima carrera, jinetes, campaña, historial de peso y estimación.
    - **Nunca** lleva tratamientos, fármacos ni notas.
25. **Colores de la chaquetilla:** se leen del programa (columna STUD - COLORES). Se completa la versión truncada con la de otro caballo del mismo stud y se dibujan con `ChaquetillaIcono`. El nombre del stud (dueño) no se muestra.
26. **Jinetes:** tarjeta "Jinetes que lo han corrido" (montas, última, mejor llegada). En la próxima carrera y la última, el jinete va rotulado; el número de mandil se muestra como `NumeroMandil`.
27. **Ícono de la app:** vector (`drawable/ic_launcher_foreground.xml`), silueta engrosada para que se lea en tamaño chico. También sirve como ícono monocromo.

## Cómo verificar

- JDK: el JBR de Android Studio (`C:\Program Files\Android\Android Studio\jbr`), no el JDK 22 del sistema.
- `./gradlew lint testDebugUnitTest` (unitarias) y `./gradlew connectedDebugAndroidTest` (UI, requiere emulador o teléfono).
- `python tools/importar_programas.py && python tools/generar_seed.py` regenera `assets/seed.json` (determinista). Las fechas del seed se desplazan a "hoy" al cargarse. Subir `VERSION_SEED` si cambian los datos.

## Bitácora (obligatorio)

Cada sesión de trabajo agrega una entrada al final de `bitacora.md`: fecha y hora (America/Santiago), autor, qué se hizo, archivos tocados, decisiones y supuestos, resultado de build/tests y un mensaje de commit sugerido (el commit lo hace Enzo). Nunca editar ni borrar entradas anteriores: las correcciones van como entrada nueva.
