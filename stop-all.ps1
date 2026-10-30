<#
.SYNOPSIS
  Stops everything started by start-all.ps1. Add -KeepInfra to leave MongoDB and Mailpit running.
#>
param([switch]$KeepInfra)
$root = $PSScriptRoot
$pidFile = Join-Path $root 'logs\pids.txt'

if (Test-Path $pidFile) {
    foreach ($line in Get-Content $pidFile) {
        $name, $id = $line -split ' '
        # /T also stops child processes (npm -> node, ...).
        taskkill /PID $id /T /F 2>$null | Out-Null
        Write-Host "durduruldu: $name"
    }
    Remove-Item $pidFile
} else {
    Write-Host 'Çalışan servis kaydı yok (logs\pids.txt bulunamadı).'
}

if (-not $KeepInfra) {
    Push-Location $root
    docker compose stop mongo mailpit
    Pop-Location
}
