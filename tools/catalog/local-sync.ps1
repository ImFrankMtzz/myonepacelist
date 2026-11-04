<#
Actualiza catalog.json leyendo onepace.net DESDE ESTA PC y lo sube a GitHub.

Por que aqui y no en GitHub Actions: onepace.net (detras de Cloudflare) responde 403 a las IP de
GitHub, pero atiende bien desde una conexion normal. Hace UNA sola consulta por ejecucion.

Pensado para correr en un clon DEDICADO del repo (no en el que se usa para programar): si hay otros
cambios sin guardar, se detiene sin tocar nada. Solo publica catalog.json y el de assets, y solo si
cambiaron. El log queda en local-sync.log (ignorado por git).

Uso:  powershell -NoProfile -ExecutionPolicy Bypass -File tools\catalog\local-sync.ps1
#>
param([string]$Repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path)

$ErrorActionPreference = 'Stop'
Set-Location $Repo
$Files = @('catalog.json', 'app/src/main/assets/catalog.json')
$Log = Join-Path $Repo 'local-sync.log'

function Say([string]$m) {
    $line = '{0:yyyy-MM-dd HH:mm:ss}  {1}' -f (Get-Date), $m
    Write-Host $line
    Add-Content -Path $Log -Value $line -Encoding UTF8
}

# Aviso visible cuando algo falla (la tarea corre oculta). Si el aviso mismo falla, no importa: ya quedo en el log.
function Alert([string]$msg) {
    try {
        Add-Type -AssemblyName System.Windows.Forms, System.Drawing
        $n = New-Object System.Windows.Forms.NotifyIcon
        $n.Icon = [System.Drawing.SystemIcons]::Warning
        $n.Visible = $true
        if ($msg.Length -gt 240) { $msg = $msg.Substring(0, 240) }
        $n.ShowBalloonTip(15000, 'MyOnePaceList', $msg, [System.Windows.Forms.ToolTipIcon]::Warning)
        Start-Sleep -Seconds 16
        $n.Dispose()
    }
    catch { }
}

function Check([string]$what) {
    if ($LASTEXITCODE -ne 0) { throw "$what fallo (codigo $LASTEXITCODE)" }
}

try {
    Say 'Inicio'

    $branch = (& git rev-parse --abbrev-ref HEAD).Trim()
    if ($branch -ne 'main') { throw "Estoy en la rama '$branch', no en main; no toco nada" }

    # Si quedaron cambios de una corrida anterior interrumpida, se descartan; cualquier otro cambio detiene el script.
    $other = @(& git status --porcelain | Where-Object { $_ -and ($Files -notcontains $_.Substring(3).Trim()) })
    if ($other.Count -gt 0) { throw "Hay otros cambios sin guardar, no toco nada: $($other -join '; ')" }
    & git checkout -- $Files
    Check 'git checkout'

    & git pull --ff-only origin main | Out-Null
    Check 'git pull'

    Push-Location (Join-Path $Repo 'tools/catalog')
    try {
        & npm.cmd test | Out-Null
        Check 'Las pruebas del lector'
        $out = (& npm.cmd run sync) -join ' '
        Check 'Generar el catalogo (onepace.net no respondio bien?)'
        Say ($out -replace '\s+', ' ')
    }
    finally { Pop-Location }

    $changed = @(& git status --porcelain -- $Files)
    if ($changed.Count -eq 0) { Say 'Sin cambios: el catalogo del repo ya esta al dia'; exit 0 }

    & git add -- $Files
    Check 'git add'
    & git commit -q -m 'Catalogo actualizado desde onepace.net'
    Check 'git commit'

    & git push -q origin main
    if ($LASTEXITCODE -ne 0) {
        # Alguien subio algo mientras tanto: se reintenta una vez sobre lo nuevo.
        & git pull --rebase origin main | Out-Null
        Check 'git pull --rebase'
        & git push -q origin main
        Check 'git push'
    }
    Say 'Catalogo actualizado y publicado'
    exit 0
}
catch {
    Say ('ERROR: ' + $_.Exception.Message)
    Alert ('No se pudo actualizar el catalogo: ' + $_.Exception.Message)
    exit 1
}
