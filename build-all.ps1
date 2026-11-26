param([switch]$Release)

$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'verify-core.ps1')
$builds = @(
    @{ Minecraft = '26.3'; Loader = 'fabric' },
    @{ Minecraft = '26.3'; Loader = 'neoforge' },
    @{ Minecraft = '26.2'; Loader = 'fabric' },
    @{ Minecraft = '26.2'; Loader = 'neoforge' },
    @{ Minecraft = '1.21.1'; Loader = 'fabric' },
    @{ Minecraft = '1.21.1'; Loader = 'neoforge' },
    @{ Minecraft = '1.20.1'; Loader = 'fabric' },
    @{ Minecraft = '1.20.1'; Loader = 'neoforge' }
)

if ($Release) {
    $env:HIVEMIND_BATCH_BETA = 'release'
} else {
    $counterPath = Join-Path $PSScriptRoot 'beta-sequence.txt'
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
    } finally { $counter.Dispose() }
    $env:HIVEMIND_BATCH_BETA = [string]$next
}

$failed = [System.Collections.Generic.List[string]]::new()
foreach ($build in $builds) {
    try {
        $edition = if ($Release) { @{ Release = $true } } else { @{ Beta = $next } }
        & (Join-Path $PSScriptRoot 'build-beta.ps1') -Minecraft $build.Minecraft -Loader $build.Loader @edition
    } catch {
        Write-Warning $_
        $failed.Add("$($build.Minecraft)/$($build.Loader)")
    }
}

Remove-Item Env:HIVEMIND_BATCH_BETA -ErrorAction SilentlyContinue
if ($failed.Count -gt 0) {
    throw "Fallaron estas compilaciones: $($failed -join ', ')"
}
Write-Host 'Las ocho compilaciones terminaron correctamente.' -ForegroundColor Green
