# Stryda

*Gestión y seguimiento de corrales de carrera.*

Demo Android para preparadores de caballos fina sangre de carrera en Chile: registrar el trabajo diario del corral en segundos y generar un reporte visual para el propietario. Funciona completa sin señal.

> Estado: demo en construcción. Ver `bitacora.md` para el avance y `CLAUDE.md` para las reglas del proyecto.

## Requisitos

- Android Studio (incluye el JDK que usa Gradle y el SDK de Android)
- Un emulador (AVD) o un teléfono Android 8.0 o superior
- Python 3 solo si quieres regenerar los datos de prueba

## Ejecutar la app

### Opción A: Android Studio (recomendada)

1. **File → Open** y elige la carpeta `Stryda`. Espera a que termine el *Gradle sync*.
2. Arriba, en el selector de dispositivos, elige un emulador (por ejemplo `Pixel_7`) o tu teléfono conectado.
3. Botón **Run ▶** (o `Shift+F10`). La app se instala como **Stryda** (id `cl.stryda.app.demo`).

### Opción B: línea de comandos (PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$sdk = "$env:LOCALAPPDATA\Android\Sdk"

# Levantar un emulador (si no usas teléfono)
& "$sdk\emulator\emulator.exe" -list-avds
Start-Process "$sdk\emulator\emulator.exe" -ArgumentList "-avd","Pixel_7"

# Compilar, instalar y abrir
.\gradlew.bat installDebug
& "$sdk\platform-tools\adb.exe" shell monkey -p cl.stryda.app.demo -c android.intent.category.LAUNCHER 1
```

### Generar el APK

```powershell
.\tools\generar_apk.ps1            # deja el APK en dist\Stryda-0.1.0-demo.apk
.\tools\generar_apk.ps1 -Instalar  # además lo instala en el teléfono conectado por USB
```

Para pasarlo al teléfono sin cable: envía el archivo de `dist\` por Google Drive, correo o WhatsApp (como documento), ábrelo en el teléfono y acepta "Instalar apps desconocidas" para esa app. Las versiones nuevas se instalan encima y conservan los datos cargados.

### En tu teléfono

1. En el teléfono: **Ajustes → Acerca del teléfono** → toca 7 veces *Número de compilación* para activar las opciones de desarrollador.
2. **Opciones de desarrollador → Depuración por USB**: activar.
3. Conecta el cable, acepta el aviso en el teléfono y elígelo en Android Studio. **Run ▶**.

También puedes copiar `app/build/outputs/apk/debug/app-debug.apk` al teléfono e instalarlo a mano.

## Verificar

```powershell
.\gradlew.bat lint testDebugUnitTest      # lint + pruebas unitarias
.\gradlew.bat connectedDebugAndroidTest   # pruebas de UI (con emulador o teléfono conectado)
```

## Datos

- Al primer arranque la app carga `app/src/main/assets/seed.json`: el corral real de Cristóbal González M. (17 ejemplares), armado desde los programas oficiales del Club Hípico. Trabajos, dueños y tratamientos son ficticios. Las fechas se ajustan solas al día de hoy.
- Para actualizar los datos reales: guarda los programas PDF en `tools/programas/` (ver `docs/fuentes-club-hipico.md`) y corre `python tools/importar_programas.py && python tools/generar_seed.py`. Requiere `pdftotext`.
- Lo que se cambia a mano en la app (estado, pelaje, pesos...) queda guardado en el teléfono con fecha y hora, y no se pierde al actualizar los datos.
- Para conectar una planilla de Google Sheets (opcional): publícala en modo lectura y agrega a `local.properties`:

  ```
  SHEET_ID=el_id_de_tu_planilla
  ```

  Sin `SHEET_ID` la app usa solo los datos de prueba.

## Estructura

```
app/src/main/java/cl/stryda/app/
  data/remote/      Google Sheets (lo único que cambia al migrar a Supabase)
  data/local/       Room, seed, preferencias
  data/repository/  repositorios que entregan objetos de dominio
  domain/           modelos y cálculos puros (con pruebas)
  ui/theme/         sistema de diseño
  ui/components/    componentes reutilizables
  ui/screens/       una carpeta por pantalla
tools/              generador de datos de prueba
docs/               preguntas para el preparador, licencias
```

## Créditos

- Tipografía [Inter](https://rsms.me/inter/), licencia SIL OFL 1.1 (`docs/licencias/Inter-OFL.txt`).
- Animación e ícono del caballo: *The Horse in Motion*, Eadweard Muybridge (1878), dominio público. Vectorizados desde el escaneo en alta resolución de Wikimedia Commons con `tools/vectorizar_caballo.py`.
