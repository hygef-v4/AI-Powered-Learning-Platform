$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    $python = Join-Path $PSScriptRoot '.venv/Scripts/python.exe'
    if (!(Test-Path -LiteralPath $python)) {
        uv venv --python 3.14 --no-python-downloads .venv
        if ($LASTEXITCODE -ne 0) { throw 'Python 3.14 is required for this lockfile.' }
    }
    & $python -c 'import struct, sys; sys.exit(not (sys.version_info[:2] == (3, 14) and struct.calcsize("P") == 8))'
    if ($LASTEXITCODE -ne 0) { throw 'The existing environment must use Python 3.14 x64.' }
    uv pip sync --python $python requirements.lock
    if ($LASTEXITCODE -ne 0) { throw 'Could not install the pinned evaluation dependencies.' }
} finally {
    Pop-Location
}
