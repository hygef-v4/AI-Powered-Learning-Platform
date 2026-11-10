param(
    [string]$ReportsDirectory = (Join-Path $PSScriptRoot '../backend/target/surefire-reports')
)

$ErrorActionPreference = 'Stop'
$requiredSuites = @{
    'edu.aiplatform.subjects.repository.SubjectRepositoryIntegrationTest' = 31
    'edu.aiplatform.notifications.integration.NotificationDeliveryIntegrationTest' = 2
    'edu.aiplatform.grading.repository.EvaluationVersionIntegrationTest' = 10
    'edu.aiplatform.grading.repository.GradeDistributionVisibilityRepositoryTest' = 1
    'edu.aiplatform.requirements.PlatformWorkflowIntegrationTest' = 24
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

$manifestPath = Join-Path $PSScriptRoot '../aidlc-docs/construction/build-and-test/2026-11-08-integration-test-cases.json'
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
if (@($manifest.cases).Count -eq 0) {
    throw 'Integration traceability manifest has no cases.'
}

foreach ($group in ($manifest.cases | Group-Object suite)) {
    $reportPath = Join-Path $ReportsDirectory "TEST-$($group.Name).xml"
    if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
        throw "Missing traced integration suite: $($group.Name)"
    }
    [xml]$report = Get-Content -LiteralPath $reportPath -Raw
    $suite = $report.testsuite
    if ($null -eq $suite -or $suite.name -ne $group.Name -or [int]$suite.tests -le 0 -or
        [int]$suite.skipped -ne 0 -or [int]$suite.errors -ne 0 -or [int]$suite.failures -ne 0 -or
        @($suite.testcase).Count -ne [int]$suite.tests -or
        @($suite.SelectNodes('testcase/skipped | testcase/error | testcase/failure')).Count -ne 0) {
        throw "Traced integration suite did not fully pass: $($group.Name)"
    }
    foreach ($case in $group.Group) {
        $methodPattern = '^' + [regex]::Escape($case.method) + '(?:\(|\[|$)'
        $executions = @($suite.testcase | Where-Object { $_.name -match $methodPattern })
        if ($executions.Count -eq 0) {
            throw "Missing traced integration case: $($case.id) -> $($case.method)"
        }
    }
}
Write-Output "Traced integration cases passed: $(@($manifest.cases).Count) cases; no missing or skipped executions."
