param([string]$Python = "python", [string]$MysqlImage = "mysql:8.4")
$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$runtimeRoot = Join-Path $projectRoot ".local"
New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
$statePath = Join-Path $runtimeRoot "runtime.json"
if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $existing = Get-CimInstance Win32_Process -Filter "ProcessId = $($state.runner_pid)" -ErrorAction SilentlyContinue
    if ($existing -and $existing.CommandLine -and $existing.CommandLine.Contains($projectRoot)) {
        Write-Output "Local runner is already active ($($state.status)). See .local/launcher.log."
        exit 0
    }
}
$env:PYTHONUTF8 = "1"
$env:LOCAL_MYSQL_IMAGE = $MysqlImage
$pythonPath = (Get-Command $Python -ErrorAction Stop).Source
$runnerPath = Join-Path $PSScriptRoot "run.py"
$process = Start-Process -FilePath $pythonPath -ArgumentList @("-u", ('"' + $runnerPath + '"')) -WindowStyle Hidden -PassThru -WorkingDirectory $projectRoot -RedirectStandardOutput (Join-Path $runtimeRoot "launcher.log") -RedirectStandardError (Join-Path $runtimeRoot "launcher-error.log")
Write-Output "Local runner PID: $($process.Id). Startup logs: $runtimeRoot/launcher.log"
