param(
    [string]$ReportsDirectory = (Join-Path $PSScriptRoot '../backend/target/surefire-reports')
)

$ErrorActionPreference = 'Stop'
$requiredSuites = @{
    'edu.aiplatform.subjects.infrastructure.SubjectRepositoryIntegrationTest' = 31
    'edu.aiplatform.notifications.infrastructure.NotificationDeliveryIntegrationTest' = 2
}

foreach ($suiteName in $requiredSuites.Keys) {
    $reportPath = Join-Path $ReportsDirectory "TEST-$suiteName.xml"
    if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
        throw "Missing required test report: $suiteName"
    }
    [xml]$report = Get-Content -LiteralPath $reportPath -Raw
    $suite = $report.testsuite
    if ($null -eq $suite -or $suite.name -ne $suiteName -or
        [int]$suite.tests -lt $requiredSuites[$suiteName] -or
        [int]$suite.skipped -ne 0 -or [int]$suite.errors -ne 0 -or [int]$suite.failures -ne 0 -or
        @($suite.testcase).Count -ne [int]$suite.tests -or
        @($suite.SelectNodes('testcase/skipped | testcase/error | testcase/failure')).Count -ne 0) {
        throw "Required suite did not fully pass: $suiteName (tests=$($suite.tests), skipped=$($suite.skipped), errors=$($suite.errors), failures=$($suite.failures))"
    }
    Write-Output "Required suite passed: $suiteName ($($suite.tests) tests, no skips)"
}
