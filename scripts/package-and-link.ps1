param(
    [switch]$Public
)

$ErrorActionPreference = 'Stop'
$ProjectName = 'mytest'

function Test-Command($name) {
    return $null -ne (Get-Command $name -ErrorAction SilentlyContinue)
}

if (-not (Test-Command 'docker')) {
    Write-Error '[ERROR] 未检测到 docker，请先安装并启动 Docker Desktop。'
    exit 1
}

Write-Host '[1/2] 构建并启动服务（MySQL + Backend + Web）...'
$env:COMPOSE_PROJECT_NAME = $ProjectName
docker compose up -d --build

$hostIp = '127.0.0.1'
try {
    $ipList = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object { $_.IPAddress -ne '127.0.0.1' -and $_.PrefixOrigin -ne 'WellKnown' }
    if ($ipList -and $ipList[0].IPAddress) {
        $hostIp = $ipList[0].IPAddress
    }
} catch {
    $hostIp = '127.0.0.1'
}

Write-Host ''
Write-Host '✅ 打包并启动完成，可通过以下链接访问：'
Write-Host '- 本机链接: http://localhost:8088'
Write-Host "- 局域网链接: http://$hostIp`:8088"
Write-Host ''
Write-Host "停止服务命令：`$env:COMPOSE_PROJECT_NAME='$ProjectName'; docker compose down"
Write-Host '（说明：不会删除数据库卷，学生注册信息和答题记录会持久保留）'

if ($Public) {
    if (-not (Test-Command 'npx')) {
        Write-Warning '[WARN] 未检测到 npx，无法生成公网临时链接。请先安装 Node.js。'
        exit 0
    }

    Write-Host ''
    Write-Host '[2/2] 生成公网临时链接（localtunnel）...'
    Write-Host '提示：按 Ctrl+C 可关闭公网链接（不会停止容器服务）。'
    npx localtunnel --port 8088
}
