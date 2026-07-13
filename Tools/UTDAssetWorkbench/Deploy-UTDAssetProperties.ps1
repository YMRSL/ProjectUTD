[CmdletBinding()]
param([string]$Candidate)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = (Resolve-Path (Join-Path $root "..\..")).Path
$instanceRoot = Join-Path $repositoryRoot "UtilWeDie-Neo-1.21.1\.minecraft\versions\1.21.1-NeoForge_21.1.233"

Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName System.Windows.Forms

function Stop-IfGameRunning {
    $escaped = [Regex]::Escape($instanceRoot)
    $running = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { ($_.Name -match '^javaw?\.exe$') -and ($_.CommandLine -match $escaped) } |
        Select-Object -First 1
    if ($null -ne $running) {
        [System.Windows.MessageBox]::Show(
            "检测到誓死坚守客户端仍在运行。请先完全退出游戏，再重新部署属性。",
            "UTD 属性部署",
            "OK",
            "Warning"
        ) | Out-Null
        exit 2
    }
}

if (-not $Candidate) {
    $downloads = Join-Path $HOME "Downloads"
    $latest = Get-ChildItem -LiteralPath $downloads -Filter "*.candidate.zip" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -ne $latest) {
        $Candidate = $latest.FullName
    }
    else {
        $picker = New-Object System.Windows.Forms.OpenFileDialog
        $picker.Title = "选择从 UTD 资产工作台导出的候选包"
        $picker.Filter = "UTD 候选包 (*.candidate.zip)|*.candidate.zip|Workbench 项目 (workbench.json)|workbench.json"
        if ($picker.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { exit 0 }
        $Candidate = $picker.FileName
    }
}

$Candidate = (Resolve-Path -LiteralPath $Candidate).Path
Stop-IfGameRunning

$preview = & npm.cmd run cli -- deploy-properties --candidate $Candidate --instance $instanceRoot --dry-run true 2>&1
if ($LASTEXITCODE -ne 0) {
    [System.Windows.MessageBox]::Show(($preview -join "`n"), "候选包校验失败", "OK", "Error") | Out-Null
    exit $LASTEXITCODE
}
$answer = [System.Windows.MessageBox]::Show(
    "候选包已通过哈希与结构校验。`n`n$($preview -join "`n")`n`n是否部署？部署前会自动制作可回滚备份。",
    "UTD 属性部署确认",
    "YesNo",
    "Question"
)
if ($answer -ne [System.Windows.MessageBoxResult]::Yes) { exit 0 }

$result = & npm.cmd run cli -- deploy-properties --candidate $Candidate --instance $instanceRoot 2>&1
if ($LASTEXITCODE -ne 0) {
    [System.Windows.MessageBox]::Show(($result -join "`n"), "属性部署失败", "OK", "Error") | Out-Null
    exit $LASTEXITCODE
}
[System.Windows.MessageBox]::Show(
    "属性部署完成。请重新启动游戏后测试 RarityCore、BlockZ 和 TaCZ；食品覆盖会在一秒内热加载。`n`n$($result -join "`n")",
    "UTD 属性部署",
    "OK",
    "Information"
) | Out-Null
