[CmdletBinding()]
param(
    [ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+(?:-beta[1-9][0-9]*)?$')]
    [string]$Version = '0.1.0',

    [string]$GradleUserHome,

    [switch]$SkipCoreChecks
)

$ErrorActionPreference = 'Stop'
$loaders = @('fabric', 'neoforge')
$properties = @{}
foreach ($loader in $loaders) {
    $path = Join-Path $PSScriptRoot "$loader/gradle.properties"
    $content = [IO.File]::ReadAllText($path)
    $matches = [regex]::Matches($content, '(?m)^minecraft_version=([0-9.]+)\s*$')
    if ($matches.Count -ne 1) { throw "La versión de Minecraft no es inequívoca en $path." }
    $properties[$loader] = $matches[0].Groups[1].Value
}
$minecraft = $properties['fabric']
if ($properties['neoforge'] -ne $minecraft) { throw 'Los dos loaders deben usar la misma versión de Minecraft.' }

$javaVersion = if ($minecraft -in @('26.3', '26.2')) { 25 } else { 21 }
$javaHome = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory |
    Where-Object { $_.Name -like "jdk-$javaVersion.*" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $javaHome) { throw "No se encontró JDK $javaVersion para Minecraft $minecraft." }

if (!$GradleUserHome) {
    $GradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $PSScriptRoot '.gradle-home' }
}
$GradleUserHome = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($GradleUserHome)

if (!$SkipCoreChecks) { & (Join-Path $PSScriptRoot 'verify-core.ps1') }

$previousJava = $env:JAVA_HOME
$previousPath = $env:Path
$previousGradle = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;$previousPath"
    $env:GRADLE_USER_HOME = $GradleUserHome
    $wrapper = Join-Path $PSScriptRoot 'gradlew.bat'
    foreach ($loader in $loaders) {
        $versionWithTarget = "$Version+$minecraft-$loader"
        $displayLoader = if ($minecraft -eq '1.20.1' -and $loader -eq 'neoforge') { 'Forge (LegacyForge)' } else { $loader }
        Write-Host "Compilando $minecraft/$displayLoader ($versionWithTarget)"
        Push-Location (Join-Path $PSScriptRoot $loader)
        try {
            & $wrapper build --no-daemon "-Pmod_version=$versionWithTarget" "-PhivemindBetaAssigned=$versionWithTarget"
            if ($LASTEXITCODE -ne 0) { throw "Falló la compilación $versionWithTarget." }
        } finally {
            Pop-Location
        }
    }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:Path = $previousPath
    $env:GRADLE_USER_HOME = $previousGradle
}

Write-Host "Las dos variantes de Minecraft $minecraft se compilaron correctamente." -ForegroundColor Green
