$ErrorActionPreference = 'Stop'
$previous = $env:PYTEST_DISABLE_PLUGIN_AUTOLOAD
Push-Location -LiteralPath $PSScriptRoot
try {
    $env:PYTEST_DISABLE_PLUGIN_AUTOLOAD = '1'
    & .venv/Scripts/python.exe -m pytest -q tests
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    $env:PYTEST_DISABLE_PLUGIN_AUTOLOAD = $previous
    Pop-Location
}
