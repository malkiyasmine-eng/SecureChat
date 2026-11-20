# Compiles every .java file into the "out" folder. Run from anywhere.
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $files
if ($LASTEXITCODE -eq 0) { Write-Host "Compilation successful." -ForegroundColor Green }
else { Write-Host "Compilation FAILED." -ForegroundColor Red }
