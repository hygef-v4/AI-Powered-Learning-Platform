<#
.SYNOPSIS
Checks the platform MCP handshake and tool discovery without calling any tool.
.EXAMPLE
.\scripts\check-mcp.ps1
#>
[CmdletBinding()]
param(
    [uri]$Endpoint = 'http://localhost:3000/mcp',
    [Security.SecureString]$Token
)

$ErrorActionPreference = 'Stop'
if (-not $Endpoint.IsAbsoluteUri -or $Endpoint.Scheme -notin @('http', 'https') -or
    $Endpoint.UserInfo -or $Endpoint.Query -or $Endpoint.Fragment -or
    ($Endpoint.Scheme -eq 'http' -and -not $Endpoint.IsLoopback)) {
    throw 'Use HTTPS, or HTTP on localhost; do not put credentials in the URL.'
}
if ($null -eq $Token) { $Token = Read-Host 'Platform MCP token (hidden)' -AsSecureString }
if ($Token.Length -eq 0) { throw 'A platform MCP token is required.' }

$tokenPointer = [IntPtr]::Zero
$headers = @{}
try {
    $tokenPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Token)
    $headers = @{
        Authorization = 'Bearer ' + [Runtime.InteropServices.Marshal]::PtrToStringBSTR($tokenPointer)
        Accept = 'application/json, text/event-stream'
        'MCP-Protocol-Version' = '2025-06-18'
    }
    function Invoke-McpProbe([int]$RequestId, [string]$Method, [hashtable]$Parameters) {
        $body = @{jsonrpc = '2.0'; id = $RequestId; method = $Method; params = $Parameters} |
            ConvertTo-Json -Depth 8 -Compress
        try {
            # Redirects are disabled so the bearer token cannot be forwarded to another endpoint.
            $response = Invoke-RestMethod -Uri $Endpoint -Method Post -Headers $headers `
                -ContentType 'application/json' -Body $body -TimeoutSec 20 -MaximumRedirection 0
        } catch {
            # Do not include remote messages or exception details: they may contain sensitive data.
            $status = if ($null -ne $_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
            throw "MCP $Method failed (HTTP $status). Check endpoint, token expiry, and server health."
        }
        if ($response.jsonrpc -ne '2.0' -or $response.id -ne $RequestId -or
            $null -ne $response.error -or $null -eq $response.result) {
            throw "MCP $Method returned an invalid or unsuccessful JSON-RPC response."
        }
        return $response.result
    }
    $initialized = Invoke-McpProbe 1 'initialize' @{
        protocolVersion = '2025-06-18'; capabilities = @{}
        clientInfo = @{name = 'platform-readiness-check'; version = '1.0.0'}
    }
    if ($initialized.serverInfo.name -ne 'ai-learning-platform' -or
        $initialized.protocolVersion -notin @('2025-06-18', '2025-03-26')) {
        throw 'The endpoint is not the expected learning platform MCP server/protocol.'
    }
    $headers['MCP-Protocol-Version'] = $initialized.protocolVersion
    $null = Invoke-McpProbe 2 'ping' @{}
    $discovery = Invoke-McpProbe 3 'tools/list' @{}
    if ($null -eq $discovery.tools) { throw 'MCP tools/list did not return a tools array.' }
    [pscustomobject]@{
        Server = $initialized.serverInfo.name
        Protocol = $initialized.protocolVersion
        ToolCount = @($discovery.tools).Count
        Tools = @($discovery.tools | Select-Object name, @{Name = 'ReadOnly'; Expression = {$_.annotations.readOnlyHint}})
    }
} finally {
    $headers.Clear()
    if ($tokenPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($tokenPointer)
    }
}
