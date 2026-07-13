[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = (Resolve-Path (Join-Path $root "..\..")).Path
$instanceRoot = Join-Path $repositoryRoot "UtilWeDie-Neo-1.21.1\.minecraft\versions\1.21.1-NeoForge_21.1.233"
Add-Type -AssemblyName PresentationFramework

$answer = [System.Windows.MessageBox]::Show(
    "是否回滚最近一次 UTD 属性部署？`n`n如果部署后的文件被其他工具改动，回滚器会停止而不会覆盖那些改动。",
    "UTD 属性回滚确认",
    "YesNo",
    "Warning"
)
if ($answer -ne [System.Windows.MessageBoxResult]::Yes) { exit 0 }

$result = & npm.cmd run cli -- rollback-properties --instance $instanceRoot 2>&1
if ($LASTEXITCODE -ne 0) {
    [System.Windows.MessageBox]::Show(($result -join "`n"), "属性回滚失败", "OK", "Error") | Out-Null
    exit $LASTEXITCODE
}
[System.Windows.MessageBox]::Show(
    "最近一次属性部署已回滚。请重新启动游戏。`n`n$($result -join "`n")",
    "UTD 属性回滚",
    "OK",
    "Information"
) | Out-Null
