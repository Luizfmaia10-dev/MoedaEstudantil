[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$syncScript = Join-Path $PSScriptRoot 'sync-frontend.ps1'

if (-not (Test-Path -LiteralPath $syncScript -PathType Leaf)) {
    throw "Script de sincronizacao nao encontrado: $syncScript"
}

& $syncScript -Check
