# Run in an ADMINISTRATOR PowerShell on the SERVER computer.
# Allows incoming TCP connections on port 5000 on PRIVATE networks only.
New-NetFirewallRule -DisplayName "SecureChat Server (TCP 5000)" `
    -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow -Profile Private
Write-Host "Firewall rule created." -ForegroundColor Green
