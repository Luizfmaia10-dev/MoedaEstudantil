[CmdletBinding()]
param(
    [switch]$Check
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$sourceRoot = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot 'frontend'))
$targetRoot = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot 'demo (2)\demo\src\main\resources\static'))

if (-not $sourceRoot.StartsWith($repositoryRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
    -not $targetRoot.StartsWith($repositoryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Os diretorios de sincronizacao precisam estar dentro do repositorio.'
}

function Get-RelativeFileMap([string]$root) {
    $map = @{}
    Get-ChildItem -LiteralPath $root -File -Recurse |
        Where-Object { $_.Name -notlike '*.test.js' } |
        ForEach-Object {
        $relative = $_.FullName.Substring($root.TrimEnd('\').Length).TrimStart('\')
        $map[$relative] = $_.FullName
    }
    return $map
}

$sourceFiles = Get-RelativeFileMap $sourceRoot
$targetFiles = Get-RelativeFileMap $targetRoot
$differences = [System.Collections.Generic.List[string]]::new()

foreach ($relative in $sourceFiles.Keys) {
    if (-not $targetFiles.ContainsKey($relative)) {
        $differences.Add("ausente: $relative")
        continue
    }

    $sourceHash = (Get-FileHash -LiteralPath $sourceFiles[$relative] -Algorithm SHA256).Hash
    $targetHash = (Get-FileHash -LiteralPath $targetFiles[$relative] -Algorithm SHA256).Hash
    if ($sourceHash -ne $targetHash) {
        $differences.Add("divergente: $relative")
    }
}

foreach ($relative in $targetFiles.Keys) {
    if (-not $sourceFiles.ContainsKey($relative)) {
        $differences.Add("orfao: $relative")
    }
}

if ($Check) {
    if ($differences.Count -gt 0) {
        throw "Frontend fora de sincronia:`n$($differences -join "`n")"
    }
    Write-Output "Frontend sincronizado ($($sourceFiles.Count) arquivos)."
    return
}

foreach ($relative in $sourceFiles.Keys) {
    $destination = Join-Path $targetRoot $relative
    $destinationDirectory = Split-Path -Parent $destination
    if (-not (Test-Path -LiteralPath $destinationDirectory)) {
        New-Item -ItemType Directory -Path $destinationDirectory | Out-Null
    }
    Copy-Item -LiteralPath $sourceFiles[$relative] -Destination $destination -Force
}

foreach ($relative in $targetFiles.Keys) {
    if (-not $sourceFiles.ContainsKey($relative)) {
        $orphan = [System.IO.Path]::GetFullPath($targetFiles[$relative])
        if (-not $orphan.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Arquivo orfao fora do destino permitido: $orphan"
        }
        Remove-Item -LiteralPath $orphan -Force
    }
}

& $PSCommandPath -Check
