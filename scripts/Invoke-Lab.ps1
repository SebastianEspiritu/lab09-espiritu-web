param(
    [ValidateSet('run', 'verify')]
    [string]$Action = 'run'
)

$ErrorActionPreference = 'Stop'
Get-Command java, mvn -ErrorAction Stop | Out-Null
$start = New-Object System.Diagnostics.ProcessStartInfo
$start.FileName = $env:ComSpec
$start.WorkingDirectory = Split-Path $PSScriptRoot -Parent
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
# El bloque de entorno del hijo conserva DB_PASSWORD incluso cuando está vacía.
if (-not $env:DB_USERNAME) { $start.EnvironmentVariables['DB_USERNAME'] = 'root' }
if (-not $start.EnvironmentVariables.ContainsKey('DB_PASSWORD')) {
    $start.EnvironmentVariables['DB_PASSWORD'] = ''
}
if (-not $env:JWT_SECRET) {
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $bytes = New-Object byte[] 32
        $random.GetBytes($bytes)
        $start.EnvironmentVariables['JWT_SECRET'] = [Convert]::ToBase64String($bytes)
    } finally { $random.Dispose() }
}
if ($Action -eq 'verify') {
    $start.EnvironmentVariables['RUN_MYSQL_TESTS'] = 'true'
    $start.Arguments = '/d /c "mvn -B verify 2>&1"'
} else {
    $start.Arguments = '/d /c "mvn -B spring-boot:run 2>&1"'
}
$process = [System.Diagnostics.Process]::Start($start)
$errorOutput = $process.StandardError.ReadToEndAsync()
while (-not $process.StandardOutput.EndOfStream) {
    Write-Output $process.StandardOutput.ReadLine()
}
$process.WaitForExit()
$errorText = $errorOutput.GetAwaiter().GetResult()
if ($errorText) { Write-Output $errorText }
$result = $process.ExitCode
$process.Dispose()
exit $result
