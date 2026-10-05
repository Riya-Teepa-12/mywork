$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $root "backend.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -ne "" -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            if ($parts.Count -eq 2) {
                $key = $parts[0].Trim()
                $value = $parts[1].Trim()
                [Environment]::SetEnvironmentVariable($key, $value, "Process")
            }
        }
    }
}
$services = @(
    @{ Name = "service-registry"; Dir = "service-registry"; Port = 8761 },
    @{ Name = "auth-service"; Dir = "Auth-service"; Port = 8081 },
    @{ Name = "post-service"; Dir = "post-service"; Port = 8082 },
    @{ Name = "comment-service"; Dir = "comment-service"; Port = 8083 },
    @{ Name = "category-service"; Dir = "category-service"; Port = 8084 },
    @{ Name = "media-service"; Dir = "media-service"; Port = 8085 },
    @{ Name = "newsletter-service"; Dir = "newsletter-service"; Port = 8086 },
    @{ Name = "notification-service"; Dir = "notification-service"; Port = 8087 },
    @{ Name = "api-gateway"; Dir = "api-gateway"; Port = 8080 }
)

$logDir = Join-Path $root "log"
if (!(Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}
# Force a single absolute log location for Spring Boot logging.file.name across all services.
[Environment]::SetEnvironmentVariable("INKWELL_LOG_DIR", $logDir, "Process")

$pids = @()
foreach ($service in $services) {
    $workingDirectory = Join-Path $root $service.Dir
    $outLog = Join-Path $logDir "$($service.Name).out.log"
    $errLog = Join-Path $logDir "$($service.Name).err.log"
    $process = Start-Process -FilePath "cmd.exe" -ArgumentList "/c mvnw.cmd -q -DskipTests spring-boot:run" -WorkingDirectory $workingDirectory -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    $started = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 1
        if ($process.HasExited) {
            break
        }
        $listen = Get-NetTCPConnection -State Listen -LocalPort $service.Port -ErrorAction SilentlyContinue
        if ($listen) {
            $started = $true
            break
        }
    }
    if (-not $started) {
        if (-not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
        throw "Failed to start $($service.Name). Check $outLog and $errLog"
    }
    $pids += [PSCustomObject]@{
        Name = $service.Name
        Dir = $workingDirectory
        Port = $service.Port
        Pid = $process.Id
    }
    Write-Host "$($service.Name) started on port $($service.Port)"
}

$pidFile = Join-Path $root ".backend-pids.json"
$pids | ConvertTo-Json | Set-Content -Encoding UTF8 $pidFile
Write-Host "All backend services are running."
