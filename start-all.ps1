<#
.SYNOPSIS
  Starts MongoDB + Mailpit (Docker), all Spring Boot services and the React frontend in the background.
.EXAMPLE
  .\start-all.ps1                # build, then start everything
  .\start-all.ps1 -SkipBuild     # start with the jars already in */target
  .\start-all.ps1 -NoFrontend    # backend only
  .\stop-all.ps1                 # stop everything again
#>
param(
    [switch]$SkipBuild,
    [switch]$NoFrontend
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$logs = Join-Path $root 'logs'
$pidFile = Join-Path $logs 'pids.txt'

$services = [ordered]@{
    'auth-service'         = 8081
    'member-service'       = 8082
    'catalog-service'      = 8083
    'loan-service'         = 8084
    'notification-service' = 8085
    'api-gateway'          = 8080
}

if (Test-Path $pidFile) {
    Write-Host "Servisler zaten çalışıyor görünüyor. Önce .\stop-all.ps1 çalıştırın." -ForegroundColor Yellow
    exit 1
}

# A port held by another program (often the full "docker compose up" stack) would make a service fail,
# and the health check below would then talk to that other program instead.
$ports = @($services.Values)
if (-not $NoFrontend) { $ports += 5173 }
$busy = foreach ($port in $ports) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { "    :$port  ->  $((Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue).ProcessName) (pid $($c.OwningProcess))" }
}
if ($busy) {
    Write-Host 'Bu portlar başka bir program tarafından kullanılıyor:' -ForegroundColor Red
    $busy | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    Write-Host 'Docker ile tüm yığın çalışıyorsa önce: docker compose down' -ForegroundColor Yellow
    exit 1
}

New-Item -ItemType Directory -Force $logs | Out-Null
Push-Location $root
try {
    Write-Host '1/4 MongoDB ve Mailpit başlatılıyor (docker compose)...' -ForegroundColor Cyan
    docker compose up -d mongo mailpit
    if ($LASTEXITCODE -ne 0) { throw 'docker compose başarısız. Docker Desktop açık mı?' }

    if (-not $SkipBuild) {
        Write-Host '2/4 Maven ile derleniyor...' -ForegroundColor Cyan
        mvn -q -B package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'Maven derlemesi başarısız.' }
    } else {
        Write-Host '2/4 Derleme atlandı (-SkipBuild).' -ForegroundColor Cyan
    }

    Write-Host '3/4 Servisler başlatılıyor...' -ForegroundColor Cyan
    $procs = @{}
    foreach ($name in $services.Keys) {
        $jar = Join-Path $root "$name\target\app.jar"
        if (-not (Test-Path $jar)) { throw "$jar yok. -SkipBuild olmadan çalıştırın." }
        $p = Start-Process java -ArgumentList '-jar', "`"$jar`"" -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logs "$name.log") -RedirectStandardError (Join-Path $logs "$name.err.log")
        Add-Content $pidFile "$name $($p.Id)"
        $procs[$name] = $p
    }

    if (-not $NoFrontend) {
        $frontend = Join-Path $root 'frontend'
        if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
            Write-Host '    npm install...' -ForegroundColor DarkGray
            Push-Location $frontend; npm install --no-audit --no-fund; Pop-Location
        }
        $p = Start-Process npm.cmd -ArgumentList 'run', 'dev' -WorkingDirectory $frontend -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logs 'frontend.log') -RedirectStandardError (Join-Path $logs 'frontend.err.log')
        Add-Content $pidFile "frontend $($p.Id)"
    }

    Write-Host '4/4 Servislerin hazır olması bekleniyor...' -ForegroundColor Cyan
    $deadline = (Get-Date).AddMinutes(3)
    $pending = [System.Collections.ArrayList]@($services.Keys)
    while ($pending.Count -gt 0 -and (Get-Date) -lt $deadline) {
        foreach ($name in @($pending)) {
            if ($procs[$name].HasExited) {
                Write-Host ("    {0,-22} :{1}  HATA, süreç kapandı -> logs\{0}.log" -f $name, $services[$name]) -ForegroundColor Red
                $pending.Remove($name)
                continue
            }
            try {
                $h = Invoke-RestMethod "http://localhost:$($services[$name])/actuator/health" -TimeoutSec 2
                if ($h.status -eq 'UP') {
                    Write-Host ("    {0,-22} :{1}  hazır" -f $name, $services[$name]) -ForegroundColor Green
                    $pending.Remove($name)
                }
            } catch { }
        }
        if ($pending.Count -gt 0) { Start-Sleep -Seconds 2 }
    }
    foreach ($name in $pending) {
        Write-Host ("    {0,-22} :{1}  BAŞLAMADI -> logs\{0}.log" -f $name, $services[$name]) -ForegroundColor Red
    }

    Write-Host ''
    if (-not $NoFrontend) { Write-Host 'Arayüz:      http://localhost:5173  (admin@library.local / admin123)' }
    Write-Host 'Gateway:     http://localhost:8080'
    Write-Host 'E-postalar:  http://localhost:8025'
    Write-Host 'Loglar:      logs\   |   Durdurmak için: .\stop-all.ps1'
} finally {
    Pop-Location
}
