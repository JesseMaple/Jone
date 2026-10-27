# ============================================================================
# Jone 项目环境启动脚本（只在当前 PowerShell 会话内生效）
#
# 用法：在项目根目录执行
#       Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
#       . .\env.ps1
#   第二行里的“点 + 空格”表示点源执行，否则环境变量不会留在当前会话；
#   第一行只对当前进程生效（不写注册表、不改系统设置），用于绕过系统默认的脚本执行限制。
#
# 设计原则（对应 AGENTS.md 红线）：
#   - 绝不使用 setx、绝不修改注册表、绝不写系统/用户级环境变量；
#   - 只改动当前进程的 $env: 变量，关闭终端即自动失效；
#   - JDK 与 Maven 全部来自项目内 envs 目录，不依赖宿主机任何已安装环境。
# ============================================================================

$项目根 = $PSScriptRoot

$jdk目录 = Join-Path $项目根 'envs\jdk'
$maven目录 = Join-Path $项目根 'envs\maven'
$java程序 = Join-Path $jdk目录 'bin\java.exe'
$mvn程序 = Join-Path $maven目录 'bin\mvn.cmd'

if (-not (Test-Path -LiteralPath $java程序)) {
    Write-Host "【错误】未找到项目内 JDK：$java程序" -ForegroundColor Red
    Write-Host "       请先确认 envs\jdk 已解压完成。" -ForegroundColor Red
    return
}

if (-not (Test-Path -LiteralPath $mvn程序)) {
    Write-Host "【错误】未找到项目内 Maven：$mvn程序" -ForegroundColor Red
    return
}

# 仅在当前会话内生效的环境变量
$env:JAVA_HOME = $jdk目录
$env:MAVEN_HOME = $maven目录
$env:PATH = "$jdk目录\bin;$maven目录\bin;$env:PATH"
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8'
$env:JAVA_TOOL_OPTIONS = $null

# 让中文在终端里正确显示：
# 1) chcp 65001 只影响当前控制台窗口，不改注册表、不改系统区域设置；
# 2) 同步设置 .NET 侧的输出编码，避免 PowerShell 解码错位。
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    if ($Host.Name -eq 'ConsoleHost') {
        chcp 65001 | Out-Null
    }
} catch {
    Write-Host "【提示】当前终端不支持设置 UTF-8 输出编码，中文可能显示异常。" -ForegroundColor Yellow
}

Write-Host "【成功】Jone 项目环境已在当前会话生效（不会污染系统环境变量）：" -ForegroundColor Green
Write-Host "        JAVA_HOME  = $env:JAVA_HOME"
Write-Host "        MAVEN_HOME = $env:MAVEN_HOME"
Write-Host "        Maven 仓库 = $(Join-Path $项目根 'envs\m2repo')"
Write-Host "        常用命令   ：mvn -v / mvn test / mvn javafx:run"
