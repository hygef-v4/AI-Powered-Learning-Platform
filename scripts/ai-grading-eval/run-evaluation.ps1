param(
    [ValidateSet('automated', 'replay', 'offline', 'live', 'full', 'report', 'doctor')]
    [string] $Mode = 'offline',
    [ValidateSet('auto', 'codex', 'claude', 'gemini', 'opencode', 'ollama')]
    [string] $Judge = 'auto',
    [ValidateSet('codex', 'claude', 'gemini', 'opencode')]
    [string[]] $AllowHosted = @(),
    [string] $Results,
    [string] $Case,
    [int] $Run,
    [string] $Model,
    [string] $GeminiModel,
    [int] $Repeats,
    [int] $MaxCalls,
    [string] $Output,
    [string] $InputReport,
    [string] $Baseline,
    [string] $JavaHome,
    [string] $Maven,
    [string] $BackendXml,
    [string] $FrontendJson,
    [switch] $AllowLive,
    [switch] $NoSemantic,
    [switch] $All,
    [switch] $CollectOnly,
    [switch] $MavenOnline,
    [switch] $Share,
    [string] $Dataset
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$python = Join-Path $PSScriptRoot '.venv/Scripts/python.exe'
if (!(Test-Path -LiteralPath $python)) { throw 'Run scripts/ai-grading-eval/setup.ps1 first.' }
$evaluationArgs = @('--mode', $Mode, '--judge', $Judge)
$valueArgs = @{
    Results='--results'; Case='--case'; Run='--run'; Model='--model'; GeminiModel='--gemini-model'
    Repeats='--repeats'; MaxCalls='--max-calls'; Output='--output'; InputReport='--input'
    Baseline='--baseline'; Dataset='--dataset'; JavaHome='--java-home'; Maven='--maven'; BackendXml='--backend-xml'
    FrontendJson='--frontend-json'
}
foreach ($key in $valueArgs.Keys) {
    if ($PSBoundParameters.ContainsKey($key)) { $evaluationArgs += @($valueArgs[$key], [string] $PSBoundParameters[$key]) }
}
$switchArgs = @{
    AllowLive='--allow-live'; NoSemantic='--no-semantic'; All='--all'
    CollectOnly='--collect-only'; MavenOnline='--maven-online'; Share='--share'
}
foreach ($key in $switchArgs.Keys) {
    if ($PSBoundParameters[$key]) { $evaluationArgs += $switchArgs[$key] }
}
foreach ($provider in $AllowHosted) { $evaluationArgs += @('--allow-hosted', $provider) }
& $python (Join-Path $PSScriptRoot 'evaluate.py') @evaluationArgs
exit $LASTEXITCODE
