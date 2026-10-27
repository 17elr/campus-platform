param(
    [string]$Gateway = 'http://127.0.0.1:8088',
    [string]$Registry = 'http://127.0.0.1:8761',
    [string]$RedisCli = 'redis-cli',
    [int]$RedisPort = 6379,
    [string]$Username = 'admin',
    [string]$Password = $env:SMOKE_PASSWORD
)
$ErrorActionPreference = 'Stop'
if (-not $Password) { throw 'Set SMOKE_PASSWORD or pass -Password for a test account.' }
$apps = Invoke-RestMethod "$Registry/eureka/apps" -Headers @{Accept = 'application/json' }
foreach ($name in @('CAMPUS-SERVICE', 'NOTICE-SERVICE', 'GATEWAY-SERVICE')) {
    $app = $apps.applications.application | Where-Object name -EQ $name
    if (-not ($app.instance | Where-Object status -EQ 'UP')) { throw "$name is not UP in Eureka" }
    Write-Output "PASS registered: $name"
}
# Remove only the public notice cache to exercise the Feign discovery path.
& $RedisCli -p $RedisPort DEL 'notices::SimpleKey []' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Redis cache reset failed' }
$notices = Invoke-RestMethod "$Gateway/api/notices"
if ($notices.code -ne 200 -or $null -eq $notices.data) { throw 'Gateway -> backend -> Feign notices failed' }
$ttl = [int](& $RedisCli -p $RedisPort TTL 'notices::SimpleKey []')
if ($LASTEXITCODE -ne 0 -or $ttl -le 0 -or $ttl -gt 300) { throw 'Redis notice cache was not populated' }
Write-Output "PASS gateway -> discovery -> notices; Redis TTL=$ttl"
$preflight = Invoke-WebRequest "$Gateway/api/auth/login" -Method Options -Headers @{
    Origin='http://localhost:5173'; 'Access-Control-Request-Method'='POST'; 'Access-Control-Request-Headers'='authorization,content-type'
} -UseBasicParsing
if ($preflight.Headers['Access-Control-Allow-Origin'] -ne 'http://localhost:5173') { throw 'CORS preflight failed' }
Write-Output 'PASS gateway CORS preflight'
$body = @{username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod "$Gateway/api/auth/login" -Method Post -ContentType 'application/json' -Body $body
if ($login.code -ne 200 -or -not $login.data.token) { throw 'Gateway login failed' }
$headers = @{Authorization = "Bearer $($login.data.token)" }
try {
    $me = Invoke-RestMethod "$Gateway/api/auth/me" -Headers $headers
    if ($me.code -ne 200 -or $me.data.username -ne $Username) { throw 'Authorization forwarding failed' }
    $invalid = Invoke-RestMethod "$Gateway/api/auth/me" -Headers @{Authorization = 'Bearer invalid' }
    if ($invalid.code -eq 200) { throw 'Invalid token was accepted' }
    Write-Output 'PASS login, Authorization forwarding, invalid-token rejection'
}
finally {
    Invoke-RestMethod "$Gateway/api/auth/logout" -Method Post -Headers $headers | Out-Null
}
