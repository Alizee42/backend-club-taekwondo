# Charge backend-club-taekwondo\.env et lance le backend avec le profil "local"

$envFile = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFile)) {
    Write-Host "Fichier .env introuvable : $envFile" -ForegroundColor Red
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $key, $value = $line -split "=", 2
        [System.Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim())
    }
}

$env:SPRING_PROFILES_ACTIVE = "local"

Write-Host "Variables .env chargées. Démarrage du backend (profil local, port 8082)..." -ForegroundColor Cyan
Write-Host "  Super admin : $env:BOOTSTRAP_SUPER_ADMIN_EMAIL / $env:BOOTSTRAP_SUPER_ADMIN_PASSWORD"
Write-Host ""

Set-Location (Join-Path $PSScriptRoot "..")
.\mvnw.cmd spring-boot:run
