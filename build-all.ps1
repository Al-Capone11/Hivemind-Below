[CmdletBinding()]
param(
    [switch]$Release,

    [string]$Workspace = (Split-Path -Parent $PSScriptRoot),

    [string]$GradleUserHome
)

$ErrorActionPreference = 'Stop'
$workspacePath = (Resolve-Path -LiteralPath $Workspace).Path
if (!$GradleUserHome) { $GradleUserHome = Join-Path $workspacePath '.gradle-home' }
$GradleUserHome = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($GradleUserHome)
$versions = @('26.3', '26.2', '1.21.1', '1.20.1')
$worktrees = @{}
foreach ($minecraft in $versions) {
    $worktree = Join-Path $workspacePath "Hivemind - $minecraft"
    foreach ($path in @('build-version.ps1', 'verify-core.ps1', 'fabric/gradle.properties', 'neoforge/gradle.properties')) {
        if (!(Test-Path -LiteralPath (Join-Path $worktree $path) -PathType Leaf)) {
            throw "Falta $path en $worktree."
        }
    }
    foreach ($loader in 'fabric', 'neoforge') {
        $metadata = [IO.File]::ReadAllText((Join-Path $worktree "$loader/gradle.properties"))
        if ($metadata -notmatch "(?m)^minecraft_version=$([regex]::Escape($minecraft))\s*$") {
            throw "La variante $loader de $worktree no declara Minecraft $minecraft."
        }
    }
    $worktrees[$minecraft] = $worktree
}

foreach ($minecraft in $versions) {
    Write-Host "Comprobando el núcleo de Minecraft $minecraft"
    & (Join-Path $worktrees[$minecraft] 'verify-core.ps1')
}

$version = '0.1.0'
if (!$Release) {
    $counterPath = Join-Path $worktrees['26.3'] 'beta-sequence.txt'
    $counter = [IO.File]::Open($counterPath, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    try {
        $bytes = New-Object byte[] ([int]$counter.Length)
        $read = $counter.Read($bytes, 0, $bytes.Length)
        $last = [int]([Text.Encoding]::UTF8.GetString($bytes, 0, $read).Trim())
        if ($last -lt 1) { throw 'El contador beta debe ser positivo.' }
        $next = $last + 1
        $encoded = [Text.Encoding]::UTF8.GetBytes("$next`n")
        $counter.Position = 0
        $counter.SetLength(0)
        $counter.Write($encoded, 0, $encoded.Length)
        $counter.Flush($true)
        $version = "0.1.0-beta$next"
    } finally {
        $counter.Dispose()
    }
}

$failed = [System.Collections.Generic.List[string]]::new()
foreach ($minecraft in $versions) {
    try {
        & (Join-Path $worktrees[$minecraft] 'build-version.ps1') -Version $version -GradleUserHome $GradleUserHome -SkipCoreChecks
    } catch {
        Write-Warning $_
        $failed.Add($minecraft)
    }
}
if ($failed.Count -gt 0) { throw "Fallaron estas versiones de Minecraft: $($failed -join ', ')." }
Write-Host "Las ocho variantes de Hivemind $version se compilaron correctamente." -ForegroundColor Green
