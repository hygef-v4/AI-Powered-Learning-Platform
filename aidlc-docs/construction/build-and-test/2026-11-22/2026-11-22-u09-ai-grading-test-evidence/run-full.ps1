$ErrorActionPreference = 'Stop'
$env:OPENBLAS_NUM_THREADS = '1'
$env:OMP_NUM_THREADS = '1'
$base = (Get-Content -LiteralPath '.tmp/u09-luna-sol-latest.txt').Trim()
$retest = Split-Path -Parent $PSScriptRoot
$java = 'C:/Program Files/Java/jdk-17/bin/java.exe'
$javaArgs = '@' + (Join-Path $retest 'run.args')
$python = (Resolve-Path 'scripts/ai-grading-eval/.venv/Scripts/python.exe').Path
$datasets = [ordered]@{}
foreach ($entry in (Get-ChildItem -Path (Join-Path $base '*/results.json') -File | Sort-Object FullName)) {
    $dataset = $entry.Directory.Name
    $datasets[$dataset] = @((Get-Content -LiteralPath $entry.FullName -Raw | ConvertFrom-Json).runs.caseKey)
    & $java $javaArgs prepare (Join-Path $PSScriptRoot $dataset) $dataset ($datasets[$dataset] -join ',')
    if ($LASTEXITCODE -ne 0) { throw "Could not prepare $dataset" }
}

$proof = foreach ($dataset in $datasets.Keys) {
    foreach ($case in $datasets[$dataset]) {
        $casePath = "$dataset/$case"
        $testInput = Join-Path $PSScriptRoot $casePath
        $before = Join-Path $retest "confirmation/$casePath"
        if (-not (Test-Path -LiteralPath $before)) { $before = Join-Path $retest "confirmation-remaining/$casePath" }
        $schema = (Get-FileHash -LiteralPath (Join-Path $testInput 'grading-schema.json')).Hash
        if ($schema -ne (Get-FileHash -LiteralPath (Join-Path $before 'grading-schema.json')).Hash) {
            throw "Schema differs for $casePath"
        }
        $previousPrompt = Get-Content -LiteralPath (Join-Path $before 'prompt.txt') -Raw -Encoding UTF8
        $newPrompt = Get-Content -LiteralPath (Join-Path $testInput 'prompt.txt') -Raw -Encoding UTF8
        $oldInput = $previousPrompt.Substring($previousPrompt.IndexOf("`n`n") + 2)
        $newInput = $newPrompt.Substring($newPrompt.IndexOf("`n`n") + 2)
        if ($oldInput -cne $newInput -or $previousPrompt -ceq $newPrompt) {
            throw "Expected only grading instructions to change for $casePath"
        }
        $focused = Join-Path $retest "prompt-followup-flows/$casePath/prompt.txt"
        $promptHash = (Get-FileHash -LiteralPath (Join-Path $testInput 'prompt.txt')).Hash
        $targetedCase = Test-Path -LiteralPath $focused
        if ($targetedCase -and $promptHash -ne (Get-FileHash -LiteralPath $focused).Hash) {
            throw "Prompt differs from the latest targeted run for $casePath"
        }
        [PSCustomObject]@{dataset=$dataset; case=$case; schemaSha256=$schema; promptSha256=$promptHash; targetedCase=$targetedCase}
    }
}
if (@($proof).Count -ne 20 -or @($proof | Where-Object targetedCase).Count -ne 3) {
    throw 'Expected 20 cases, including 3 matching prompt checks from the targeted run'
}
$proof | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'input-verification.json') -Encoding UTF8
Write-Output 'All 20 fixture inputs and schemas match the prior run. The 3 targeted prompts match this version byte for byte.'

& $python (Join-Path $PSScriptRoot 'grade-inputs.py') $base *> (Join-Path $PSScriptRoot 'luna.log')
if ($LASTEXITCODE -ne 0) { throw 'Luna runner unexpectedly exited' }
Get-Content -LiteralPath (Join-Path $PSScriptRoot 'luna.log') -Tail 1

foreach ($dataset in $datasets.Keys) {
    & $java $javaArgs validate (Join-Path $PSScriptRoot $dataset) $dataset ($datasets[$dataset] -join ',')
    if ($LASTEXITCODE -ne 0) { throw "Could not validate $dataset" }
}
& $python (Join-Path $base 'judge-saved.py') --judge sol --workers 6 --run-dir $PSScriptRoot *> (Join-Path $PSScriptRoot 'sol.log')
if ($LASTEXITCODE -ne 0) { throw 'Sol judge unexpectedly exited' }
Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sol.log') -Tail 8
