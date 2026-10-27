# ============================================================================
# 一键运行单元测试
#
# 用法（在项目根目录）：.\tests\run-tests.ps1
# 说明：只对当前进程放开脚本执行限制，不写注册表；环境变量同样仅当前会话有效。
# ============================================================================

$ErrorActionPreference = 'Stop'

Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$项目根 = Split-Path -Parent $PSScriptRoot
Write-Host "【信息】项目根目录：$项目根"

Push-Location $项目根
try {
    . .\env.ps1
    Write-Host "【信息】开始执行单元测试……"
    mvn -B -ntp test
    $退出码 = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($退出码 -eq 0) {
    Write-Host "【通过】全部单元测试通过。" -ForegroundColor Green
} else {
    Write-Host "【失败】存在失败的测试，退出码 $退出码，请查看上方日志。" -ForegroundColor Red
}
exit $退出码
