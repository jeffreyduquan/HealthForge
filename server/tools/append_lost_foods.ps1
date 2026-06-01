# P7.S3 Slice 1 Hotfix-3: Verlorene DE-Foods nachpflegen
# REQ-DATA-CURATION-002 Hotfix / 2026-05-31
# - Rename fdc=171116 (war faelschlich "Haehnchen ganz", tatsaechlich Chicken ground raw)
# - Append 4 neue Rows aus usda_fdc.csv mit korrekten DE-Namen
#
# Limitation: 17 weitere gewuenschte Foods (Halloumi, Marzipan, Tzatziki, Gnocchi,
# Sourdough, Smoked Salmon, Udon, Erythrit, Sauerteig, Bohnenkraut, Schwarzkuemmel,
# Fischsauce, Enoki, Maitake, Walnussmus, Haselnusscreme, Kokosmus) sind im
# USDA-FDC-Voll-Seed nicht enthalten. Folge-Slice braucht externe Quelle (DGE/BLS).

param(
    [string]$FullSeed = "$PSScriptRoot\..\src\main\resources\seed\usda_fdc.csv",
    [string]$Curated  = "$PSScriptRoot\..\src\main\resources\seed\usda_fdc_curated.csv"
)

# Map fdc_id -> gewuenschter name_de
$newNames = @{
    '171314'  = 'Ghee (Butterschmalz)'
    '174301'  = 'Sojaproteinkonzentrat (Sojaschnetzel-Ersatz)'
    '168063'  = 'Milchreis (Arroz con leche)'
    '171852'  = 'Mehrkornbagel'
}

# 1) Rename in curated
$curLines = [System.IO.File]::ReadAllLines($Curated, [System.Text.Encoding]::UTF8)
$renamed = $false
for ($i = 1; $i -lt $curLines.Length; $i++) {
    $line = $curLines[$i]
    if (-not $line) { continue }
    $cols = $line.Split(';')
    if ($cols[0].Trim() -eq '171116') {
        $oldName = $cols[1]
        $cols[1] = 'Haehnchenhack roh'
        $curLines[$i] = ($cols -join ';')
        Write-Host "Renamed fdc=171116: '$oldName' -> 'Haehnchenhack roh'" -ForegroundColor Yellow
        $renamed = $true
        break
    }
}
if (-not $renamed) { Write-Warning "fdc=171116 not found in curated (already removed?)" }

# 2) Append new rows from full seed
$enc = New-Object System.Text.UTF8Encoding $false
$existingIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($l in $curLines) {
    if ($l) { [void]$existingIds.Add(($l.Split(';'))[0].Trim()) }
}

$appended = @()
$reader = [System.IO.File]::OpenText($FullSeed)
try {
    [void]$reader.ReadLine()  # header
    while (-not $reader.EndOfStream) {
        $line = $reader.ReadLine()
        if (-not $line) { continue }
        $cols = $line.Split(';')
        $id = $cols[0].Trim()
        if ($newNames.ContainsKey($id)) {
            if ($existingIds.Contains($id)) {
                Write-Warning ("fdc={0} already in curated -- skipping append" -f $id)
                continue
            }
            $oldName = $cols[1]
            $cols[1] = $newNames[$id]
            $newLine = $cols -join ';'
            $appended += $newLine
            Write-Host ("Appended fdc={0}: '{1}' (was DE '{2}' / EN '{3}')" -f $id, $newNames[$id], $oldName, $cols[2]) -ForegroundColor Green
            [void]$existingIds.Add($id)
        }
    }
} finally { $reader.Close() }

# 3) Write back
$final = New-Object System.Collections.Generic.List[string]
foreach ($l in $curLines) { $final.Add($l) }
foreach ($l in $appended) { $final.Add($l) }
[System.IO.File]::WriteAllLines($Curated, $final, $enc)
Write-Host ""
Write-Host "Curated CSV final length: $($final.Count) lines (header + data)" -ForegroundColor Cyan
Write-Host "Appended $($appended.Count) new rows" -ForegroundColor Cyan
