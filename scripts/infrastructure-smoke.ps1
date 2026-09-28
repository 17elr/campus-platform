$ErrorActionPreference = 'Stop'
$notices = Invoke-RestMethod 'http://127.0.0.1:8080/api/notices'
if ($notices.code -ne 200 -or $notices.data.Count -lt 1) { throw 'Feign notice request failed' }
$ttl = [int](& redis-cli ttl 'notices::SimpleKey []')
if ($ttl -le 0) { throw 'Redis notice cache was not populated' }
Write-Output "Feign notices: $($notices.data.Count); Redis TTL: $ttl seconds"
