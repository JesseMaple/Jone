# ============================================================================
# Jone 双击启动脚本（由 启动Jone.cmd 调用）
#
# 为什么单独拆一个 ps1：
#   cmd.exe 会按系统代码页（GBK）解析 .bat/.cmd 文件，UTF-8 中文会变成乱码并导致命令解析失败。
#   因此 .cmd 只保留 ASCII 引导，所有中文提示集中在这里（UTF-8 + BOM）。
#
# 重要：请从普通窗口启动（资源管理器双击即可）。
#   若从受限/沙箱终端启动，子进程会继承低完整性令牌，只能写入授权目录。
# ============================================================================

Set-Location $PSScriptRoot

if (-not (Test-Path 'envs\jdk\bin\java.exe')) {
    Write-Host '【错误】未找到项目内 JDK，请确认 envs\jdk 已解压完成。' -ForegroundColor Red
    exit 1
}

Write-Host '【信息】正在启动 Jone……' -ForegroundColor Green
Write-Host '        使用项目内便携 JDK 与 Maven，不会修改系统环境变量。'

# 点源环境脚本（仅当前进程生效）
. .\env.ps1

mvn javafx:run
$退出码 = $LASTEXITCODE

if ($退出码 -ne 0) {
    Write-Host "【错误】启动失败，退出码 $退出码，请查看上方日志。" -ForegroundColor Red
}
exit $退出码
