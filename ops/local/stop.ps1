$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$runtimeRoot = Join-Path $projectRoot ".local"
$statePath = Join-Path $runtimeRoot "runtime.json"
if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    foreach ($taskPid in (@($state.runner_pid) + @($state.processes | ForEach-Object { $_.pid }))) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $taskPid" -ErrorAction SilentlyContinue
        if ($process -and $process.CommandLine -and $process.CommandLine.Contains($projectRoot)) {
            Stop-Process -Id $taskPid -ErrorAction SilentlyContinue
        }
    }
    $state.status = "stopped"
    $state | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $statePath -Encoding utf8
}
# Stop only this project's dependencies. Keep named volumes and user data.
docker compose --env-file (Join-Path $runtimeRoot "docker.env") -f (Join-Path $PSScriptRoot "compose.yml") stop
