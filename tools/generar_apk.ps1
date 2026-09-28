# Genera el APK de la demo en dist\Stryda-<version>-demo.apk.
# Con -Instalar, además lo instala en el teléfono o emulador conectado por USB (depuración USB activa).
#
# Uso (desde la carpeta del proyecto):
#   .\tools\generar_apk.ps1
#   .\tools\generar_apk.ps1 -Instalar
param([switch]$Instalar)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

# Gradle necesita el JDK que trae Android Studio
$jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $jbr) { $env:JAVA_HOME = $jbr }

try {
    & .\gradlew.bat assembleDebug --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw "La compilación falló." }

    $version = (Select-String -Path "app\build.gradle.kts" -Pattern 'versionName = "(.+)"').Matches[0].Groups[1].Value
    New-Item -ItemType Directory -Force "dist" | Out-Null
    $destino = "dist\Stryda-$version-demo.apk"
    Copy-Item "app\build\outputs\apk\debug\app-debug.apk" $destino -Force
    $mb = [math]::Round((Get-Item $destino).Length / 1MB, 1)
    Write-Host "APK listo: $destino ($mb MB)"

    if ($Instalar) {
        $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
        $dispositivos = & $adb devices | Select-String "\tdevice$"
        if (-not $dispositivos) { throw "No hay teléfono conectado. Conéctalo por USB y acepta la depuración en la pantalla." }
        & $adb install -r $destino
        if ($LASTEXITCODE -ne 0) { throw "No se pudo instalar." }
        Write-Host "Instalado. Busca Stryda en el teléfono."
    }
}
catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
