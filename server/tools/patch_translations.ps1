# Patch name_de in seed/usda_fdc_curated.csv basierend auf manueller Triage
# REQ-DATA-TRANSLATE-001 Hotfix / 2026-05-31
# 26 semantisch komplett falsche DeepL-Uebersetzungen werden ueberschrieben.
# Strategie: name_de matcht die TATSAECHLICHE FDC-Quelle (name_en), nicht die urspruengliche Suche.
# Verlorene "kanonische" deutsche Foods (Raeucherlachs, Marzipan etc.) muessen in spaeterem
# Slice als neue Rows von echten FDC-Eintragen ergaenzt werden.

param(
    [string]$CsvPath = "$PSScriptRoot\..\src\main\resources\seed\usda_fdc_curated.csv"
)

# fdc_id -> neuer name_de
$overrides = @{
    '168048'  = 'Salmonbeere roh'
    '169222'  = 'Spargelbohne roh'
    '167991'  = 'Jelly Beans'
    '171832'  = 'Bohnendip'
    '171599'  = 'Rinderbratensosse (HEINZ)'
    '175041'  = 'Weinstein (Cream of Tartar)'
    '171287'  = 'Huehnerei roh ganz'
    '171714'  = 'Brotfrucht roh'
    '172335'  = 'Muskatbutteroel'
    '171421'  = 'Kakaobutteroel'
    '173411'  = 'Butter geschlagen gesalzen'
    '169334'  = 'Pestwurz gekocht'
    '173347'  = 'Rice-A-Roni Haehnchen'
    '171468'  = 'Haehnchenfett roh'
    '169886'  = 'Veggie-Haehnchenhack'
    '168911'  = 'Spaghetti mit Spinat trocken'
    '167944'  = 'Kaesebrot'
    '171628'  = 'Schinken-Kaese-Laib'
    '2259796' = 'Feta Vollmilch zerkruemelt'
    '175140'  = 'Sardine in Tomatensauce (Dose)'
    '169243'  = 'Portobello-Pilze gegrillt'
    '169403'  = 'Maitake-Pilze roh'
    '168828'  = 'Kokoscreme-Pudding Trockenmischung'
    '168880'  = 'Reis weiss gekocht'
    '168905'  = 'Chow-Mein-Nudeln (chinesisch)'
    '174268'  = 'Fleischstrecker (Soja)'
}

if (-not (Test-Path $CsvPath)) { Write-Error "CSV not found"; exit 1 }

# UTF-8 read/write
$lines = [System.IO.File]::ReadAllLines($CsvPath, [System.Text.Encoding]::UTF8)
$patched = 0
$notFound = New-Object System.Collections.Generic.HashSet[string]
foreach ($k in $overrides.Keys) { [void]$notFound.Add($k) }

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    if (-not $line -or $line.StartsWith('#') -or $i -eq 0) { continue }
    $cols = $line.Split(';')
    if ($cols.Count -lt 5) { continue }
    $fdcId = $cols[0].Trim()
    if ($overrides.ContainsKey($fdcId)) {
        $oldName = $cols[1]
        $newName = $overrides[$fdcId]
        $cols[1] = $newName
        $lines[$i] = ($cols -join ';')
        Write-Host "  fdc=$fdcId : '$oldName' -> '$newName'"
        $patched++
        [void]$notFound.Remove($fdcId)
    }
}

if ($notFound.Count -gt 0) {
    Write-Warning "Not found in CSV: $($notFound -join ', ')"
}

# Write back with UTF-8 BOM (matches usual CSV consumer expectation; importer handles both)
$enc = New-Object System.Text.UTF8Encoding $false  # no BOM (matches existing file)
[System.IO.File]::WriteAllLines($CsvPath, $lines, $enc)
Write-Host ""
Write-Host "Patched $patched of $($overrides.Count) rows in $CsvPath" -ForegroundColor Green
