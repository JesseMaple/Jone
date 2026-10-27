# ============================================================================
# 界面交互自检脚本
#
# 用法（在项目根目录）：.\tests\run-ui-checks.ps1
#
# 做四件事，全部通过真实启动 JavaFX 应用来验证：
#   1. 侧边栏：模拟鼠标进入左侧感应区，0.2 秒后应展开；
#   2. 时钟：实测时钟容器占页面宽高比例应为 33% / 30%；
#   3. 主题：热切换主题后界面能重建且不崩溃；
#   4. 转换：经转换页执行 CSV→JSON，并验证同名不覆盖时生成 _1 文件。
#
# 脚本会在开始前备份 config.json，结束时原样还原，避免污染你的个人配置。
# ============================================================================

$ErrorActionPreference = 'Stop'
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$项目根 = Split-Path -Parent $PSScriptRoot
$配置文件 = Join-Path $项目根 'config.json'
$备份文件 = Join-Path $项目根 'target\config.json.ui-check-bak'

Push-Location $项目根
try {
    . .\env.ps1

    New-Item -ItemType Directory -Path (Join-Path $项目根 'target') -Force | Out-Null
    if (Test-Path $配置文件) { Copy-Item $配置文件 $备份文件 -Force }

    $测试样例 = Join-Path $项目根 'test_data\学生表.csv'
    $结果列表 = @()

    function 运行自检([string]$名称, [hashtable]$环境, [scriptblock]$判定) {
        # 清理上一轮可能残留的开关
        foreach ($键 in @('JONE_UI_SMOKE', 'JONE_SMOKE_FILE', 'JONE_SMOKE_OVERWRITE',
                          'JONE_SNAPSHOT_DIR', 'JONE_SNAPSHOT_NAME', 'JONE_SNAPSHOT_SIDEBAR',
                          'JONE_SNAPSHOT_FILE')) {
            Remove-Item "Env:$键" -ErrorAction SilentlyContinue
        }
        foreach ($键 in $环境.Keys) { Set-Item "Env:$键" $环境[$键] }

        Write-Host "【执行】$名称" -ForegroundColor Cyan
        # Maven 会往 stderr 写 SLF4J 之类的提示，不能把 stderr 当致命错误处理
        $旧偏好 = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            $输出 = (mvn -B -ntp javafx:run 2>&1 | Out-String)
        } finally {
            $ErrorActionPreference = $旧偏好
        }
        $自检行 = ($输出 -split "`r?`n") | Where-Object { $_ -like '*【自检】*' } | Select-Object -Last 1
        $通过 = $false
        if ($自检行 -and (& $判定 $自检行)) { $通过 = $true }

        if ($自检行) { Write-Host "        $($自检行.Trim())" }
        else { Write-Host "        未捕获到自检输出" -ForegroundColor Yellow }
        $script:结果列表 += [PSCustomObject]@{ 检查项 = $名称; 结果 = $(if ($通过) { '通过' } else { '失败' }) }
    }

    运行自检 '侧边栏悬停 0.2 秒展开' @{ JONE_UI_SMOKE = 'sidebar' } {
        param($行) $行.Contains('通过')
    }

    运行自检 '大时钟 33% / 30% 比例' @{ JONE_UI_SMOKE = 'clock' } {
        param($行) $行.Contains('宽比 0.330') -and $行.Contains('高比 0.300')
    }

    运行自检 '主题热切换' @{ JONE_UI_SMOKE = 'theme' } {
        param($行) $行.Contains('通过')
    }

    运行自检 '格式切换并持久化' @{ JONE_UI_SMOKE = 'formats' } {
        param($行) $行.Contains('通过')
    }

    运行自检 '窗口按钮（最小化/最大化/关闭）' @{ JONE_UI_SMOKE = 'titlebar' } {
        param($行) $行.Contains('可触发') -and $行.Contains('正常')
    }

    运行自检 '覆盖弹窗选「否」生成序号文件' @{
        JONE_UI_SMOKE = 'confirm'
        JONE_SMOKE_FILE = $测试样例
        JONE_SMOKE_CHOICE = '否'
    } {
        param($行) $行.Contains('转换成功')
    }

    运行自检 '格式转换（同名不覆盖生成序号）' @{
        JONE_UI_SMOKE = 'convert'
        JONE_SMOKE_FILE = $测试样例
        JONE_SMOKE_OVERWRITE = '0'
    } {
        param($行) $行.Contains('转换成功')
    }
} finally {
    # 还原配置并清理
    if (Test-Path $备份文件) {
        Copy-Item $备份文件 $配置文件 -Force
        Remove-Item $备份文件 -Force
    }
    Pop-Location
}

Write-Host ''
Write-Host '================ 自检结果 ================' -ForegroundColor Cyan
$结果列表 | Format-Table -AutoSize
$失败数 = @($结果列表 | Where-Object { $_.结果 -eq '失败' }).Count
if ($失败数 -eq 0) {
    Write-Host '【通过】全部界面自检通过。' -ForegroundColor Green
    exit 0
} else {
    Write-Host "【失败】有 $失败数 项未通过。" -ForegroundColor Red
    exit 1
}
