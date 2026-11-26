[CmdletBinding(DefaultParameterSetName = 'Beta')]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('26.3', '26.2', '1.21.1', '1.20.1')]
    [string]$Minecraft,

    [Parameter(Mandatory = $true)]
    [ValidateSet('fabric', 'neoforge')]
    [string]$Loader,

    [Parameter(Mandatory = $true, ParameterSetName = 'Beta')]
    [ValidateRange(1, 2147483647)]
    [int]$Beta,

    [Parameter(Mandatory = $true, ParameterSetName = 'Release')]
    [switch]$Release
)

$ErrorActionPreference = 'Stop'
$batch = if ($Release) { 'release' } else { [string]$Beta }
if ($env:HIVEMIND_BATCH_BETA -ne $batch) { throw 'Usa build-all.ps1 para compilar las ocho variantes con la misma versión.' }
$root = $PSScriptRoot
$project = Join-Path $root "$Minecraft/$Loader"
$propertiesPath = Join-Path $project 'gradle.properties'
$javaVersion = if ($Minecraft -in @('26.3', '26.2')) { 25 } else { 21 }
$javaHome = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory |
    Where-Object { $_.Name -like "jdk-$javaVersion.*" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $javaHome) { throw "No se encontró JDK $javaVersion para $Minecraft/$Loader." }

$baseVersion = if ($Release) { '0.1.0' } else { "0.1.0-beta$Beta" }
$version = "$baseVersion+$Minecraft-$Loader"
$properties = [IO.File]::ReadAllText($propertiesPath)
$pattern = '(?m)^mod_version=[^\r\n]*'
if ([regex]::Matches($properties, $pattern).Count -ne 1) { throw "No se encontró una única propiedad mod_version en $propertiesPath." }
$properties = [regex]::Replace($properties, $pattern, "mod_version=$version")
[IO.File]::WriteAllText($propertiesPath, $properties, [Text.UTF8Encoding]::new($false))

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$env:GRADLE_USER_HOME = Join-Path $root '.gradle-home'
Write-Host "Compilando $Minecraft/${Loader}: $version"
Push-Location $project
try {
    & (Join-Path $root 'gradlew.bat') build --no-daemon "-PhivemindBetaAssigned=$version"
    if ($LASTEXITCODE -ne 0) { throw "Falló la compilación $version." }
} finally {
    Pop-Location
}
