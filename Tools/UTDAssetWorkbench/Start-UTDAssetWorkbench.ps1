[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$address = "http://127.0.0.1:4173"
$repositoryRoot = (Resolve-Path (Join-Path $root "..\..")).Path
$canonicalProject = Join-Path $repositoryRoot "outputs\projectutd-assets-20260711\workbench\workbench.json"
$publicProject = Join-Path $root "public\data\workbench.json"

function Test-WorkbenchReady {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $address -TimeoutSec 1
        return $response.StatusCode -ge 200 -and
            $response.StatusCode -lt 500 -and
            $response.Content -match "UTD Asset Workbench"
    }
    catch {
        return $false
    }
}

if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show(
        "没有找到 Node.js / npm，暂时无法启动资产工作台。请先安装 Node.js，或在 Codex 中让我协助修复。",
        "UTD 资产工作台",
        "OK",
        "Error"
    ) | Out-Null
    exit 1
}

if (-not (Test-Path -LiteralPath (Join-Path $root "node_modules"))) {
    $install = Start-Process -FilePath "npm.cmd" -ArgumentList @("ci") -WorkingDirectory $root -WindowStyle Hidden -Wait -PassThru
    if ($install.ExitCode -ne 0) {
        throw "依赖准备失败（退出码 $($install.ExitCode)）。"
    }
}

if (Test-Path -LiteralPath $canonicalProject) {
    $publicDirectory = Split-Path -Parent $publicProject
    New-Item -ItemType Directory -Path $publicDirectory -Force | Out-Null
    Copy-Item -LiteralPath $canonicalProject -Destination $publicProject -Force
}
elseif (-not (Test-Path -LiteralPath $publicProject)) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show(
        "没有找到誓死坚守的工作台数据。请在 Codex 中让我重新生成 outputs/projectutd-assets-20260711/workbench/workbench.json。",
        "UTD 资产工作台",
        "OK",
        "Error"
    ) | Out-Null
    exit 1
}

$build = Start-Process -FilePath "npm.cmd" -ArgumentList @("run", "build") -WorkingDirectory $root -WindowStyle Hidden -Wait -PassThru
if ($build.ExitCode -ne 0) {
    throw "工作台构建失败（退出码 $($build.ExitCode)）。"
}

if (-not (Test-WorkbenchReady)) {
    Start-Process -FilePath "npm.cmd" -ArgumentList @("run", "preview", "--", "--host", "127.0.0.1", "--port", "4173", "--strictPort") -WorkingDirectory $root -WindowStyle Hidden | Out-Null

    $ready = $false
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        Start-Sleep -Milliseconds 250
        if (Test-WorkbenchReady) {
            $ready = $true
            break
        }
    }

    if (-not $ready) {
        throw "本地页面服务未能在 10 秒内启动。"
    }
}

Start-Process $address | Out-Null
