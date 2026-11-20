# Starts the SecureChat server (port 5000 unless you pass another one).
param([int]$Port = 5000)
Set-Location (Split-Path -Parent $PSScriptRoot)
java -cp out securechat.server.ChatServer $Port
