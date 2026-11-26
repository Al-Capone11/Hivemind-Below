$ErrorActionPreference = 'Stop'
$jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory |
    Where-Object { $_.Name -like 'jdk-21.*' } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $jdk) { throw 'No se encontró JDK 21 para comprobar el núcleo.' }

$output = Join-Path $PSScriptRoot '.assetgen/core-check-classes'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$main = @(Get-ChildItem (Join-Path $PSScriptRoot 'Core/src/main/java') -Filter '*.java' -Recurse -File | ForEach-Object FullName)
$checks = @(Get-ChildItem (Join-Path $PSScriptRoot 'Core/src/test/java/com/hivemind/core') -Filter '*Check.java' -File | Sort-Object Name)
if ($main.Count -eq 0 -or $checks.Count -eq 0) { throw 'No se encontraron fuentes o comprobaciones del núcleo.' }
$sources = $main + @($checks | ForEach-Object FullName)
& (Join-Path $jdk 'bin/javac.exe') -encoding UTF-8 -d $output $sources
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron compilar las comprobaciones del núcleo.' }
foreach ($check in $checks) {
    $class = 'com.hivemind.core.' + $check.BaseName
    & (Join-Path $jdk 'bin/java.exe') -cp $output $class
    if ($LASTEXITCODE -ne 0) { throw "Falló la comprobación $class." }
}
Write-Host 'Las comprobaciones del núcleo finalizaron correctamente.' -ForegroundColor Green
