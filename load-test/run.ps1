<#
.SYNOPSIS
    ZenAndOps Load Test Runner

.DESCRIPTION
    Runs k6 load tests against the ZenAndOps API using a Docker container.
    Requires Docker to be running and the ZenAndOps stack to be up.

.PARAMETER BaseUrl
    Gateway base URL. Defaults to http://host.docker.internal:8080 (Docker-to-host).

.PARAMETER OutputFormat
    k6 output format: json, csv, or none. Defaults to json.

.PARAMETER VerboseOutput
    Show full k6 output instead of summary only.

.EXAMPLE
    .\run.ps1
    .\run.ps1 -BaseUrl "http://host.docker.internal:8080"
    .\run.ps1 -OutputFormat csv -VerboseOutput
#>
param(
    [string]$BaseUrl = "http://zenandops-gateway-service:8080",
    [ValidateSet("json", "csv", "none")]
    [string]$OutputFormat = "json",
    [switch]$VerboseOutput
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
$K6_IMAGE      = "grafana/k6:0.57.0"
$SCRIPT_DIR    = $PSScriptRoot
$RESULTS_DIR   = Join-Path $SCRIPT_DIR "results"
$TIMESTAMP     = Get-Date -Format "yyyyMMdd-HHmmss"

# Auto-detect the Docker network used by the ZenAndOps stack
$NETWORK_NAME  = (docker inspect zenandops-gateway-service --format "{{range .NetworkSettings.Networks}}{{.NetworkID}}{{end}}" 2>$null)
if ($NETWORK_NAME) {
    $NETWORK_NAME = (docker network ls --filter "id=$NETWORK_NAME" --format "{{.Name}}" 2>$null)
}
if (-not $NETWORK_NAME) {
    $NETWORK_NAME = (docker network ls --filter "name=zenandops" --format "{{.Name}}" 2>$null | Select-Object -First 1)
}
if (-not $NETWORK_NAME) {
    Write-Host "[ERROR] Could not find ZenAndOps Docker network." -ForegroundColor Red
    exit 1
}

# ---------------------------------------------------------------------------
# Pre-flight checks
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ZenAndOps Load Test Runner" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker
try {
    docker info 2>&1 | Out-Null
} catch {
    Write-Host "[ERROR] Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if the ZenAndOps stack is up
$gateway = docker ps --filter "name=zenandops-gateway-service" --format "{{.Status}}" 2>&1
if (-not $gateway -or $gateway -notmatch "Up") {
    Write-Host "[ERROR] zenandops-gateway-service is not running." -ForegroundColor Red
    Write-Host "        Run 'docker compose up -d' first." -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Gateway service is running." -ForegroundColor Green

# Quick health check
try {
    $health = docker exec zenandops-gateway-service java -cp /app/healthcheck HealthCheck http://localhost:8080/q/health 2>&1
    if ($LASTEXITCODE -ne 0) { throw "unhealthy" }
    Write-Host "[OK] Gateway health check passed." -ForegroundColor Green
} catch {
    Write-Host "[WARN] Gateway health check failed. Proceeding anyway..." -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------
# Prepare results directory
# ---------------------------------------------------------------------------
if (-not (Test-Path $RESULTS_DIR)) {
    New-Item -ItemType Directory -Path $RESULTS_DIR -Force | Out-Null
}

# ---------------------------------------------------------------------------
# Build k6 command
# ---------------------------------------------------------------------------
$dockerArgs = @(
    "run", "--rm"
    "--name", "zenandops-k6-loadtest"
    "--network", $NETWORK_NAME
    "-v", "${SCRIPT_DIR}:/scripts:ro"
    "-e", "BASE_URL=$BaseUrl"
)

# Output file
if ($OutputFormat -ne "none") {
    $outputFile = "results/loadtest-${TIMESTAMP}.${OutputFormat}"
    $dockerArgs += "-v", "${RESULTS_DIR}:/scripts/results"
    $dockerArgs += $K6_IMAGE
    $dockerArgs += "run"

    if ($OutputFormat -eq "json") {
        $dockerArgs += "--out", "json=/scripts/$outputFile"
    } elseif ($OutputFormat -eq "csv") {
        $dockerArgs += "--out", "csv=/scripts/$outputFile"
    }
} else {
    $dockerArgs += $K6_IMAGE
    $dockerArgs += "run"
}

if (-not $VerboseOutput) {
    $dockerArgs += "--quiet"
}

$dockerArgs += "/scripts/script.js"

# ---------------------------------------------------------------------------
# Run
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "Configuration:" -ForegroundColor Cyan
Write-Host "  Base URL     : $BaseUrl"
Write-Host "  k6 Image     : $K6_IMAGE"
Write-Host "  Output       : $OutputFormat"
Write-Host "  Network      : $NETWORK_NAME"
Write-Host ""
Write-Host "Starting load test..." -ForegroundColor Yellow
Write-Host "  Warm-up    :   0s -  30s (1 VU, 5 iterations)"
Write-Host "  Sustained  :  30s - 240s (ramp to 10 VUs)"
Write-Host "  Spike      : 245s - 295s (30 VUs burst)"
Write-Host ""
Write-Host "----------------------------------------" -ForegroundColor DarkGray

$startTime = Get-Date

docker @dockerArgs

$exitCode = $LASTEXITCODE
$elapsed  = (Get-Date) - $startTime

Write-Host "----------------------------------------" -ForegroundColor DarkGray
Write-Host ""

if ($exitCode -eq 0) {
    Write-Host "[PASS] Load test completed successfully." -ForegroundColor Green
} elseif ($exitCode -eq 99) {
    Write-Host "[FAIL] Load test completed but thresholds were breached." -ForegroundColor Red
} else {
    Write-Host "[ERROR] Load test failed with exit code $exitCode." -ForegroundColor Red
}

Write-Host "  Duration: $([math]::Round($elapsed.TotalSeconds, 1))s" -ForegroundColor Cyan

if ($OutputFormat -ne "none") {
    $fullPath = Join-Path $RESULTS_DIR "loadtest-${TIMESTAMP}.${OutputFormat}"
    if (Test-Path $fullPath) {
        $size = [math]::Round((Get-Item $fullPath).Length / 1KB, 1)
        Write-Host "  Results : $fullPath (${size} KB)" -ForegroundColor Cyan
    }
}

Write-Host ""
exit $exitCode
