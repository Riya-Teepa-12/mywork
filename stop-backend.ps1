$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$pidFile = Join-Path $root ".backend-pids.json"

if (Test-Path $pidFile) {
    $entries = Get-Content -Raw $pidFile | ConvertFrom-Json
    foreach ($entry in $entries) {
        if ($entry.Pid) {
            Stop-Process -Id $entry.Pid -Force -ErrorAction SilentlyContinue
        }
        Get-CimInstance Win32_Process | Where-Object {
            $_.Name -match '^java(\\.exe)?$' -and $_.CommandLine -like "*$($entry.Dir)*"
        } | ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Write-Host "$($entry.Name) stopped"
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match '^java(\\.exe)?$' -and $_.CommandLine -like "*Desktop\\Inkwell*"
} | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
}

Write-Host "Backend services stopped."
