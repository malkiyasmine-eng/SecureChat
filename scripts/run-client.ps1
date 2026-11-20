# Starts one SecureChat client window. Run it again for more clients.
Set-Location (Split-Path -Parent $PSScriptRoot)
java -cp out securechat.client.ChatClient
