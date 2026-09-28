# Bitácora — Stryda

Registro cronológico del desarrollo de **Stryda** (gestión y seguimiento de corrales de carrera).

- **Titular y autor intelectual:** Enzo Sabattini
- **Asistencia técnica:** Claude Code (Anthropic), bajo instrucciones del autor
- **Inicio del proyecto:** 25 de septiembre de 2026

## Reglas de esta bitácora

1. Solo se agregan entradas al final. Nunca se edita ni borra una entrada anterior; una corrección es una entrada nueva que referencia a la anterior.
2. Cada entrada lleva: fecha y hora (America/Santiago), autor, resumen, archivos, decisiones, supuestos por confirmar, verificación y un mensaje de commit sugerido.
3. Enzo hace los commits. Se recomienda que el mensaje termine con `(bitácora #NNN)`, así cada entrada se ubica con `git log --grep`.
4. El historial de commits (y su publicación en GitHub, cuando Enzo lo decida) es la evidencia fechada; esta bitácora es el índice legible. Reglas actualizadas en la entrada #007.

---

## #001 — 2026-09-25 13:00 -03:00 — Inicio del proyecto

**Autor:** Enzo Sabattini (con Claude Code)

**Resumen**
Creación del proyecto a partir del documento "Stryda — Prompt para Claude Code (demo Android)" del 25 sep 2026. Se configura la base del repositorio y un esqueleto Android que compila.

**Archivos creados**
- `CLAUDE.md` — reglas del proyecto (secciones 2 a 9 del documento fuente) + regla de bitácora.
- `bitacora.md` — este archivo.
- `.gitignore` — excluye builds, `local.properties`, keystores y `google-services.json`.
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, wrapper Gradle 8.14.
- `app/build.gradle.kts`, `AndroidManifest.xml`, `res/values/strings.xml`, `MainActivity.kt` (placeholder).
- `local.properties` — **no versionado**; contiene `sdk.dir` y `SHEET_ID` (vacío por ahora).

**Decisiones técnicas**
- AGP 8.13.0, Kotlin 2.2.20, Compose BOM 2025.09.00, `compileSdk`/`targetSdk` 36, `minSdk` 26, Java 17.
- JDK de compilación: el JBR incluido en Android Studio (21). El JDK 22 del sistema no se usa para Gradle.
- La build "demo" es el `buildType` **debug** con `applicationIdSuffix = ".demo"` (id `cl.stryda.app.demo`). Se eligió sobre un `productFlavor` para que el comando `./gradlew lint testDebugUnitTest` del documento funcione tal cual.
- `SHEET_ID` se inyecta en `BuildConfig.SHEET_ID` desde `local.properties`.
- Dependencias del stack (Room, Retrofit, Coil, Vico, Navigation, DataStore, Rive) se agregan a medida que cada capa las necesite, no antes.

**Supuestos por confirmar**
- Que la "build de demo" = debug sea aceptable (alternativa: flavor `demo`).
- Correo para commits: se usa el configurado en git global (`sabattinienzo@gmail.com`), no el de La Hornilla. Confirmar cuál debe figurar como autor.

**Verificación**
- `gradlew assembleDebug testDebugUnitTest` → BUILD OK. APK debug generado (≈10 MB).

**Commit:** el que incluye esta entrada → `git log --grep="bitácora #001"`.

---

## #002 — 2026-09-25 15:07 -03:00 — Sistema de diseño, capa de datos y pantallas 1 y 2

**Autor:** Enzo Sabattini (con Claude Code)

**Resumen**
Enzo confirma que Stryda es su proyecto personal y aprueba todas las recomendaciones de UX de la entrada #001. Se construye la base completa de la demo: sistema de diseño, datos de prueba, capa de datos offline, dominio con pruebas, navegación, splash, Mis corrales, Mi corral, Carreras (lista) y Ajustes. El resto de las pantallas quedan navegables como "En construcción".

**Construido**
- **Sistema de diseño** (`ui/theme/`): paleta clara/oscura como `ColorScheme` M3 + `LocalAppColors`, tipografía Inter con cifras tabulares, formas, escala de espacio y duraciones. Sin `dynamicColor`. Tema forzable en Ajustes y guardado en DataStore.
- **Datos de prueba** (`tools/generar_seed.py` → `assets/seed.json`, 571 KB): 1 corral, 12 ejemplares propios + 45 rivales, 2016 sesiones, 478 mediciones, 26 tratamientos, 58 carreras, 593 participaciones y 190 parciales. Cumple el plan: en forma, desmejorando, lesionado, en retiro, debutante, tres dueños, tres carreras en los próximos 6 días, y huecos deliberados.
- **Capa de datos**: Room (10 tablas) como única fuente de verdad; seed al primer arranque; cliente Google Sheets (gviz + Retrofit) aislado en `data/remote/`, detrás de la interfaz `FuenteRemota`; actualización oportunista que nunca bloquea.
- **Dominio** (`domain/`): carga semanal, ratio de carga, velocidad máxima, récords por distancia, peso actual, retiro, elegibilidad e indicador de estado. Todo en funciones puras.
- **UI**: splash con el caballo, Mis corrales (se salta con un solo corral), Mi corral (franja "Corren esta semana", un indicador por fila, botón "Registrar trabajo de hoy"), Carreras, Ajustes, barra inferior de 3 pestañas.
- **Componentes**: avatar con iniciales, indicador con ícono + texto, skeleton con shimmer, estado vacío, indicador de última actualización y `CaballoGalopando`.
- **Ícono de la app**: adaptativo, fondo verde Stryda y caballo de Muybridge en blanco.
- `README.md` con instrucciones para ejecutar y `docs/preguntas-para-el-preparador.md` con los supuestos a validar.

**Decisiones técnicas**
- Dependencias verificadas en Maven: KSP 2.2.20-2.0.4, Room 2.8.1, Navigation 2.9.5, DataStore 1.1.7, Retrofit 3.0.0, OkHttp 4.12.0, kotlinx.serialization 1.9.0, Coil 3.3.0. Se agregó `material-icons-extended`, justificado en un comentario. Vico entra cuando se construyan los gráficos (paso 4).
- **Fechas del seed relativas a hoy**: el seed tiene fecha de referencia y la app desplaza todas las fechas al cargarlo, para que "corre en 2 días" siga siendo cierto el día de la reunión.
- **Caballo animado**: no hay `.riv`, así que se aplicó el respaldo 3 del documento: 15 cuadros de *The Horse in Motion* (Muybridge, 1878, dominio público, vía Wikimedia Commons) convertidos a siluetas teñibles (43 KB en total). Cambiar a Rive toca un solo archivo.
- **Contraste**: `warning #B7791F` da 3.6:1 sobre blanco. En modo claro se usa `#8F5A0A` (5.8:1). El resto de los tokens se verificó con un cálculo de contraste WCAG.
- **Tipografía**: no se usa 12sp. La regla "nunca menos de 14sp" gana sobre la escala.
- Parser de fechas en modo estricto: una prueba detectó que `31/02/2026` se convertía en silencio al 28 de febrero. Corregido.
- Recursos de terceros: Inter (SIL OFL 1.1, licencia en `docs/licencias/`).

**Supuestos por confirmar**
Ver `docs/preguntas-para-el-preparador.md` (unidades de carga, umbral de carga alta, conteo de días de retiro, indicador "En descanso", orden alfabético, ventana de 7 días, peso antiguo sobre 30 días).

**Verificación**
- `gradlew lint testDebugUnitTest connectedDebugAndroidTest` → BUILD SUCCESSFUL, **lint 0 avisos**, **38 pruebas unitarias** y **7 pruebas de UI** OK (emulador Pixel 7).
- **Modo avión desde instalación limpia** (`pm clear`): abre, carga el seed y muestra Mi corral. OK.
- Revisión visual en emulador: modo claro, modo oscuro, Ajustes.

**Checklist manual — Pantalla 2 (Mi corral)**
1. Modo avión: OK.
2. Pantalla 5": pendiente, falta un AVD chico.
3. Fuente 130%: sin solapes. "Cóndor Andino" se cortaba con puntos suspensivos; ahora el nombre usa hasta 2 líneas.
4. Modo oscuro: OK en la revisión visual.
5. Táctiles ≥ 48dp: filas 72dp, franja 56dp, botón flotante 56dp, pestañas M3. OK por construcción.
6. Datos faltantes: sin foto se muestran iniciales. OK. Sin peso: no aplica en esta pantalla.
7. Rotación: mantiene pantalla y estado. En horizontal se ve apretado pero usable.
8. Doble toque: pendiente (aplica al registro, paso 3).
9. TalkBack: cada fila tiene una descripción única ("Gran Retamo, 4 años · Macho, Corre en 2 días"), cubierta por prueba de UI. Falta probar con TalkBack real.
10. Hoja vacía: estado vacío implementado y probado en UI. Falta probarlo con una planilla real vacía.

**Pendiente**
- Pantallas 3 a 7. La siguiente es Registrar trabajo.
- Firebase App Distribution: necesita un proyecto Firebase de Enzo.
- `caballo.riv` para Rive.
- Puntos 2 y 9 del checklist (pantalla de 5" y TalkBack real).

**Commit:** el que incluye esta entrada → `git log --grep="bitácora #002"`.

---

## #003 — 2026-09-25 15:32 -03:00 — Ficha del caballo, estimación y corral de Cristóbal González

**Autor:** Enzo Sabattini (con Claude Code)

**Pedido de Enzo**
1. En la ficha del caballo, un caballo galopando y abajo su información: nombre, peso, velocidad máxima, peak de velocidad, última carrera, pelaje, número que usó en la última carrera, estado.
2. Una estimación o predicción basada en los últimos datos, idealmente con IA o machine learning entrenado por nosotros.
3. Sacar los datos de la revista del Club Hípico.
4. Que el corral sea el de Cristóbal González, buscando su información.

**Construido**
- **Ficha del caballo** (`ui/screens/caballo/`): cabecera con el caballo galopando (galopa al entrar, desacelera y se detiene; tocarlo galopa otra vez); nombre, pelaje · sexo · edad; indicador de estado y elegibilidad ("Apto para correr", "En período de retiro hasta el 2 de octubre", etc.); cuatro cifras con conteo animado (peso con variación mensual, velocidad máxima en trabajos, peak en carrera GMAX, días desde la última carrera); última carrera (posición de N, tiempo, mandil, jinete, peso); próxima carrera; estimación. Sin dato se muestra una raya y una explicación, nunca un cero falso.
- **Estimación** (`domain/Estimacion.kt`): lleva sus carreras a la distancia objetivo con la fórmula de Riegel y proyecta la tendencia de su propio historial. Entrega un tiempo central y un rango, y dice con cuántas carreras se calculó y que no es un pronóstico. Pide al menos 3 carreras con tiempo. Está detrás de la interfaz `Estimador`, para reemplazarla por un modelo entrenado sin tocar la UI.
- **Corral** renombrado a "Corral Cristóbal González", preparador "Cristóbal González M.".
- **Seed versionado**: la app reemplaza los datos de prueba sola cuando cambia `VERSION_SEED`. Solo lee la versión del encabezado, así que no parsea 571 KB en cada arranque.
- Consultas nuevas en Room: carreras del caballo con número de corredores, y parciales del caballo.

**Investigación** (detalle en `docs/fuentes-club-hipico.md`)
- El Club Hípico publica el programa oficial en PDF sin login, más resultados, GMAX y aprontes en HTML dinámico. No tiene robots.txt. Sus términos no mencionan el scraping, pero reservan la propiedad intelectual.
- Cristóbal González M. es preparador y veterinario en el Club Hípico. Se identificaron 17 ejemplares suyos en los programas de septiembre de 2026.
- Limitaciones: el programa no trae el tiempo individual de cada caballo (solo el del ganador y los cuerpos), ni el pelaje ni la madre (salvo en debutantes).

**Decisiones**
- El caballo en la ficha contradice la regla original de animación. Se aplica porque lo pide Enzo, que es el autor de esa regla. Se registró en `CLAUDE.md` (decisión 9).
- **No se entrenó un modelo de ML.** Con datos sintéticos, un modelo aprendería las reglas de nuestro generador y no el comportamiento real de los caballos: mostraría una precisión falsa frente al preparador. Se entrega un estimador estadístico transparente y queda el camino para ML cuando haya datos reales.
- **Los ejemplares siguen siendo ficticios.** Pasar a los caballos reales de González requiere una decisión de Enzo: datos de terceros del Club Hípico, pelajes desconocidos, y lesiones o tratamientos ficticios que quedarían asociados a animales reales.

**Supuestos por confirmar**
- Velocidades en km/h.
- "Velocidad máxima" = mejor velocidad media en un trabajo cronometrado. "Peak en carrera" = velocidad instantánea más alta medida por GMAX.
- La estimación usa la distancia de la próxima carrera o, si no hay, la de la última.

**Verificación**
- `gradlew lintDebug testDebugUnitTest connectedDebugAndroidTest` → BUILD SUCCESSFUL. **Lint 0**, **48 pruebas unitarias** (8 nuevas de estimación, 1 de tendencia, 2 del seed) y **10 pruebas de UI** (3 nuevas de la ficha).
- Emulador: fichas de Gran Retamo (completa), Cóndor Andino (debutante sin datos) y Lluvia de Mayo (en retiro).
- Actualización del seed sobre una instalación existente (versión 2 → 3 → 2): la app cambió de datos sola en cada paso.

**Pendiente**
- Decisión de Enzo sobre usar los caballos reales de Cristóbal González.
- Pedir permiso al Club Hípico si se quiere ingesta automática.
- Pestañas de la ficha (Entrenamiento con gráficos, Salud, Campaña, Datos) y botón "Ver como lo ve el dueño".
- Registrar trabajo (paso 3).

**Commit:** el que incluye esta entrada → `git log --grep="bitácora #003"`.

---

## #004 — 2026-09-25 15:58 -03:00 — Caballos reales, edición manual con historial y más aire en la ficha

**Autor:** Enzo Sabattini (con Claude Code)

**Pedido de Enzo**
1. Generar los caballos reales de Cristóbal González.
2. Animación del caballo siempre en loop, pero solo en la ficha.
3. Poder cambiar el estado del caballo.
4. Mejorar la experiencia de uso: la ficha estaba "muy pegada".
5. Editar los parámetros del caballo a mano y que quede guardado, para entrenar más adelante un estimador o modelo del corral.

**Construido**
- **Importador de programas** (`tools/importar_programas.py`):
  - Lee los PDF oficiales del Club Hípico con `pdftotext` y extrae carreras (número, hora, distancia, pista, condición) y, por corredor, mandil, padrillo, peso del jinete, edad, jinete, preparador, llegadas, última carrera (cuerpos, tiempo, dividendo) y peso físico.
  - Resultado sobre 6 programas de septiembre de 2026: 108 carreras, 1.188 corredores, 36 inscripciones de C. González M.
- **Seed real** (`VERSION_SEED = 3`): 17 ejemplares de Cristóbal González, 47 carreras, 368 participaciones, 241 rivales reales y 35 pesajes reales (peso físico).
  - Ficticio, como se acordó: trabajos, dueños y tratamientos de rutina.
  - Sin lesiones, retiros ni parciales inventados.
  - Sexo tomado de la condición de carrera cuando dice "hembras" o "machos".
- **Edición manual en la ficha**, con deshacer (5 s) y háptica al guardar:
  - Cambiar estado: activo, lesionado, en descanso o retirado, con nota opcional.
  - Registrar medición: peso, temperatura y frecuencia en reposo, con aviso de error de tipeo (p. ej. "48 kg").
  - Completar o editar datos: pelaje y sexo con botones, más edad, padre y madre.
- **Historial "Registrado en este teléfono"** en la ficha, con fecha y hora de cada cambio.
- **Base de datos v2** con migración real (no destructiva): tabla `cambios_caballo` y columna `mediciones.pendiente`. El dato vigente se deriva de los eventos; lo cargado a mano sobrevive a las recargas del seed o la planilla.
- **Ficha rediseñada:**
  - Secciones con título (Rendimiento, Carreras, Estimación, Registrado en este teléfono) y 24 dp entre secciones.
  - Tarjeta de estado que se puede tocar.
  - Barra de acciones abajo ("Cambiar estado", "Registrar peso").
  - Enlace "Completar pelaje y sexo" cuando faltan.
  - Genealogía ("Por Robert Bruce").
  - Caballo en loop.
- **Mi corral:** la franja "Corren esta semana" muestra máximo 3 carreras y un enlace "Ver las N carreras". Con datos reales corrían 8 el mismo día y la franja tapaba la lista.

**Decisiones**
- Cambios como eventos y no como sobrescritura. El historial con fecha y hora es exactamente el dataset que necesita un modelo del corral; además sirve de respaldo y permite deshacer.
- Sin migración destructiva desde la v2: perder datos cargados por el preparador es inaceptable.
- El tiempo de cada caballo se aproxima con 0,2 s por cuerpo, y la app lo rotula como "Tiempo aprox.".
- Los programas PDF no se versionan (material del Club Hípico). El JSON derivado sí, para que el seed sea reproducible. Si el repositorio se hace público, hay que revisar esto.

**Supuestos por confirmar**
Ver las filas nuevas en `docs/preguntas-para-el-preparador.md`: segundos por cuerpo, "Hipódromo sin dato", pelaje y sexo en blanco, amplitud del rango de la estimación.

**Verificación**
- `gradlew lintDebug testDebugUnitTest connectedDebugAndroidTest` → BUILD SUCCESSFUL. **Lint 0**, **56 pruebas unitarias** (nuevas: cambios manuales y seed real) y **14 pruebas de UI** (nuevas: migración 1→2 y las 3 hojas de edición).
- Recorrido en emulador con Sos de Boca:
  1. Cambio a Lesionado, con deshacer.
  2. Cerrar y abrir la app: el estado se mantiene.
  3. Peso "48" → aviso de error; peso 485,5 guardado y reflejado en la cifra.
  4. Pelaje Alazán y sexo Macho → "Alazán · Macho · 5 años".
  5. El historial muestra los 3 registros.
  6. Modo oscuro OK.

**Pendiente**
- Exportar el historial (CSV) para entrenar el modelo, cuando haya datos del preparador.
- Rivales de cuidado (pantalla 5): los datos reales de rivales ya están cargados.
- Registrar trabajo (paso 3).

**Commit:** el que incluye esta entrada → `git log --grep="bitácora #004"`.

---

## #005 — 2026-09-25 16:16 -03:00 — Cuadros que abren formularios, historial de peso y APK

**Autor:** Enzo Sabattini (con Claude Code)

**Pedido de Enzo**
1. Que al tocar un cuadro (peso, etc.) se abra un formulario en un modal.
2. Historial de pesos, porque también sirve para estimaciones.
3. Poder pasar la app a su teléfono: explicación de cómo generar el APK.

**Construido**
- **Cuadros que se pueden tocar** (con flecha que lo indica) y abren una hoja inferior:
  - **Peso:** formulario de pesaje (temperatura y frecuencia opcionales, detrás de "Agregar temperatura o frecuencia") y el historial de peso: rango de los últimos 6 meses, peso en su mejor resultado reciente, gráfico de evolución y lista de pesajes con variación contra el anterior, marcando los días de carrera y lo registrado en el teléfono.
  - **Velocidad máxima:** registrar un trabajo cronometrado (apronte o huinchada, distancia con botones, tiempo con solo números: `486` = 48.6, `1024` = 1:02.4, con cálculo de km/h en vivo y aviso de error de tipeo) y sus 5 trabajos más rápidos.
  - **Peak en carrera:** explica qué es GMAX y dónde se publica, y muestra el dato si existe.
  - **Días desde su última carrera** y la tarjeta "Última carrera" ("Ver todas"): todas sus carreras con resultado, tiempo aproximado, mandil, jinete y peso de ese día.
- **Dominio** (`domain/Pesos.kt`): historial, cruce de peso con resultado de carrera, mejor carrera con peso conocido y rango de peso. Son observaciones: nunca dice cuánto debería pesar.
- **Gráfico con Vico 2.1.4** (dependencia exigida por el documento, recién ahora usada): una serie en color primario, sin leyenda, valor al tocar un punto; la lista debajo funciona como vista en tabla.
- **Script `tools/generar_apk.ps1`:** compila y deja `dist/Stryda-0.1.0-demo.apk` (21,7 MB). Con `-Instalar`, lo instala por USB. Guardado en UTF-8 con BOM para que Windows PowerShell 5 muestre bien los acentos.

**Decisiones**
- El registro de un trabajo cronometrado desde la ficha adelanta una parte de "Registrar trabajo" (paso 3) para un solo ejemplar. El registro en lote sigue pendiente.
- El APK de demo va firmado con la llave de depuración: sirve para instalar a mano. Para distribuir versiones al preparador, lo correcto sigue siendo Firebase App Distribution o un APK de release firmado con llave propia.

**Verificación**
- `gradlew lintDebug testDebugUnitTest connectedDebugAndroidTest` → BUILD SUCCESSFUL. **Lint 0** (se corrigió un aviso de `mutableIntStateOf`), **64 pruebas unitarias** (nuevas: pesos, entrada de tiempo) y **17 pruebas de UI** (nuevas: historial de peso, apronte por dígitos, abrir peso desde su cuadro).
- Emulador, ficha de Bravo Nino: hojas de peso (gráfico y 3 pesajes reales), velocidad ("486" → 48.6 = 59,3 km/h) y carreras. Se corrigió "1° de 1" en el historial cuando no se conoce el total de corredores.
- `generar_apk.ps1 -Instalar` → instalación correcta en el emulador.

**Pendiente**
- Registro de trabajo en lote (paso 3).
- APK de release firmado y Firebase App Distribution.
- Exportar el historial a CSV para entrenar el modelo.

**Commit:** el que incluye esta entrada → `git log --grep="bitácora #005"`.

---

## #006 — 2026-09-25 17:52 -03:00 — Caballo en alta definición, PDF, colores, jinetes e ícono

**Autor:** Enzo Sabattini (con Claude Code)

**Pedido de Enzo** (tras probar la app en su teléfono)
1. La animación del caballo se ve borrosa y sin fluidez.
2. Los botones "Cambiar estado" y "Registrar peso" sobran: basta con explicar que los cuadros se tocan.
3. Al hacer scroll dentro de las hojas de peso o velocidad, la hoja se cierra sola.
4. Un PDF profesional del caballo, con corral, preparador y jinete.
5. Los colores y números con que corren los caballos.
6. Saber quién es el jinete o quién ha corrido el caballo.
7. El ícono de la app no se ve bien.

**Causa del problema de la animación**
Los cuadros venían de un GIF de 300 px, estirados a ~600 px en el teléfono (borrosos), con siluetas manchadas. Además, cada cambio de cuadro volvía a leer la imagen.

**Construido**
- **Caballo vectorial** (`tools/vectorizar_caballo.py`):
  - Los 11 cuadros del galope se trazan como curvas desde el escaneo en alta resolución de *The Horse in Motion* (5694×3510, dominio público).
  - Todos los cuadros tienen el mismo tamaño, para que la animación no salte.
  - Los 11 cuadros se cargan una sola vez.
  - Velocidad: 45 ms por cuadro, cerca de la velocidad real de Muybridge (1/25 s).
  - Se corrigió un relleno invertido detectado en la revisión visual: potracer rellena lo oscuro.
- **Ícono vectorial:** el mismo trazo, engrosado para que se lea en tamaño chico. Sirve también como ícono monocromo.
- **Ficha sin barra de acciones:**
  - Ayuda en pantalla: "Toca un cuadro para ver su historial o registrar un dato nuevo" y "Toca para cambiar el estado".
- **Hojas inferiores sin gesto de arrastre** (Material 3 1.4.0 fijado por `sheetGesturesEnabled`): el dedo desplaza el contenido. Se cierran con la ✕, con "atrás" o tocando fuera.
- **Colores de la chaquetilla:**
  - El importador lee la columna "STUD - COLORES" del programa.
  - Las descripciones truncadas se completan con la de otro caballo del mismo stud y se limpian las palabras cortadas.
  - Se dibujan con su diseño (`ChaquetillaIcono`): franja, cruz, mangas, lunares, hombreras, puños.
  - El nombre del stud no se muestra.
  - Base de datos v3 con migración `colores`.
- **Jinetes:**
  - Tarjeta "Jinetes que lo han corrido" (montas, última, mejor llegada).
  - El jinete va rotulado en la próxima y la última carrera.
  - El número de mandil se muestra como paño (`NumeroMandil`).
- **Ficha PDF** (botón "Ficha PDF", arriba):
  - Formato A4, generada en el teléfono y compartida con el menú de Android (WhatsApp, Drive, correo…).
  - Contenido: corral y preparador arriba; datos, colores, estado y aptitud, rendimiento, próxima carrera, jinetes, campaña, estimación e historial de peso con gráfico. Stryda discreta al pie.
  - Sin fármacos, notas ni tratamientos.
  - Pagina sola, sin títulos huérfanos, y la ficha de Sos de Boca cabe en una hoja.

**Decisiones**
- Vectores y no imágenes: nítidos en cualquier pantalla, livianos (~6 KB por cuadro) y reproducibles desde el script. Lint advierte sobre el largo de los trazos; se justificó en el propio XML: Compose dibuja cada cuadro una vez y lo reutiliza.
- El stud solo se usa internamente para completar colores; no llega a la app (los dueños siguen siendo ficticios).

**Verificación**
- `gradlew lintDebug testDebugUnitTest connectedDebugAndroidTest` → BUILD SUCCESSFUL. **Lint 0**, **69 pruebas unitarias** (nuevas: jinetes, chaquetilla, colores del seed) y **19 pruebas de UI** (nuevas: migración 2→3 y PDF de 60 carreras en varias páginas, leído con `PdfRenderer`).
- Emulador:
  - Animación nítida.
  - Ícono en el cajón de apps.
  - Ficha de Apura el Paso con colores y jinetes.
  - Hoja de peso con ✕.
  - PDF de Sos de Boca generado en el teléfono, extraído y revisado: 1 página.
  - Menú de compartir con "Ficha Sos de Boca.pdf".

**Pendiente**
- Probar la animación en el teléfono de Enzo (el emulador no reproduce su pantalla).
- Registro de trabajo en lote (paso 3).

**Commit:** el que incluye esta entrada → `git log --grep="bitácora #006"`.

---

## #007 — 2026-09-28 10:01 -03:00 — Historial de git reiniciado; git pasa a manos de Enzo

**Autor:** Enzo Sabattini (con Claude Code)

**Qué pasó**
- Enzo vio commits que no había pedido y pensó que el proyecto se estaba subiendo a GitHub.
- Revisión: el repositorio **nunca tuvo remoto** y no se subió nada a internet.
- Claude sí había hecho 7 commits locales (uno por sesión, entradas #001 a #006) sin pedido explícito, para enlazarlos con la bitácora. Fue un error.

**Qué se hizo**
- A pedido de Enzo se borró la carpeta `.git` y se ejecutó `git init`. Los archivos del proyecto no cambiaron; el repositorio queda sin commits ni remotos.
- Las referencias `git log --grep="bitácora #00N"` de las entradas #001 a #006 ya no apuntan a nada. Las entradas no se editan (regla de esta bitácora); este registro deja constancia.
- Regla nueva en `CLAUDE.md` (decisión 8) y en el encabezado de esta bitácora: Claude no hace commit, push, remotos ni repositorios en GitHub. Al terminar cada tarea, lista los archivos cambiados y sugiere un mensaje de commit.

**Mensaje de commit sugerido para el primer commit de Enzo**
`Stryda demo: estado al 28 sep 2026 (bitácora #001–#007)`
