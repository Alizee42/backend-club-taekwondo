$env:BOOTSTRAP_SUPER_ADMIN_EMAIL = "superadmin@taekwondo.local"
$env:BOOTSTRAP_SUPER_ADMIN_PASSWORD = "TkdSuperAdmin2026!"

Write-Host "Starting backend with local super-admin bootstrap:"
Write-Host "  Email: $env:BOOTSTRAP_SUPER_ADMIN_EMAIL"
Write-Host "  Password: $env:BOOTSTRAP_SUPER_ADMIN_PASSWORD"
Write-Host ""
Write-Host "The account is created only if this email does not already exist."

.\mvnw.cmd spring-boot:run
