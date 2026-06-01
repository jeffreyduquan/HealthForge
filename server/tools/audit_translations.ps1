# Translation-Mismatch-Audit fuer seed/usda_fdc_curated.csv
# REQ-DATA-TRANSLATE-001 / 2026-05-31
# Scannt alle Rows nach Pattern, bei denen name_de inhaltlich nicht zu name_en passt.
# Output: Mismatch-Kandidaten als Tabelle, gruppiert nach Konfidenz.

param(
    [string]$CsvPath = "$PSScriptRoot\..\src\main\resources\seed\usda_fdc_curated.csv",
    [string]$OutPath = "$PSScriptRoot\..\..\translation_audit.txt"
)

if (-not (Test-Path $CsvPath)) { Write-Error "CSV not found: $CsvPath"; exit 1 }

# Mismatch-Rules: wenn name_en eines der Trigger-Worte enthaelt,
# muss name_de mindestens eines der Required-Worte enthalten (case-insensitive, unaccent).
$rules = @(
    @{ name = "Berry";        en = @('berry','berries');                                              de = @('beere','beeren','frucht') }
    @{ name = "Milk";         en = @('milk','cream','heavy cream','sour cream','condensed milk');     de = @('milch','sahne','rahm','schmand') }
    @{ name = "Yogurt";       en = @('yogurt','yogurts','yoghurt');                                   de = @('joghurt','jogurt','yogurt') }
    @{ name = "Cheese";       en = @('cheese','cheddar','mozzarella','feta','ricotta','parmesan');    de = @('kaese','käse','feta','ricotta','mozzarella','parmesan','quark','frischk') }
    @{ name = "Butter";       en = @('butter');                                                       de = @('butter','margarine') }
    @{ name = "Rice";         en = @('rice','jasmine rice','basmati');                                de = @('reis') }
    @{ name = "Noodles/Pasta";en = @('noodle','noodles','pasta','spaghetti','macaroni','lasagna');    de = @('nudel','nudeln','pasta','spaghetti','makkaroni','lasagne') }
    @{ name = "Bread";        en = @('bread','bagel','baguette','toast','croissant');                 de = @('brot','bagel','baguette','toast','croissant','broetchen','brötchen','gebaeck','gebäck') }
    @{ name = "Salmon";       en = @('salmon ','salmon,','salmon)','smoked salmon');                  de = @('lachs') }
    @{ name = "Tuna";         en = @('tuna');                                                         de = @('thunfisch','tuna') }
    @{ name = "Cod";          en = @('cod,','cod ','codfish');                                        de = @('kabeljau','dorsch') }
    @{ name = "Chicken";      en = @('chicken');                                                      de = @('huhn','huehn','hühn','hahn','hähn','poulet','chicken') }
    @{ name = "Beef";         en = @('beef');                                                         de = @('rind','beef') }
    @{ name = "Pork";         en = @('pork');                                                         de = @('schwein') }
    @{ name = "Egg";          en = @('eggs,','egg,','egg ','egg white','egg yolk');                   de = @('ei ','eier','ei,','eigelb','eiweiss','eiweiß','omelett') }
    @{ name = "Apple";        en = @('apple','apples');                                               de = @('apfel','aepfel','äpfel') }
    @{ name = "Banana";       en = @('banana','bananas');                                             de = @('banane','bananen') }
    @{ name = "Tomato";       en = @('tomato','tomatoes');                                            de = @('tomate','tomaten') }
    @{ name = "Potato";       en = @('potato','potatoes');                                            de = @('kartoffel') }
    @{ name = "Carrot";       en = @('carrot','carrots');                                             de = @('karotte','moehre','möhre') }
    @{ name = "Onion";        en = @('onion','onions');                                               de = @('zwiebel') }
    @{ name = "Garlic";       en = @('garlic');                                                       de = @('knoblauch') }
    @{ name = "Mushroom";     en = @('mushroom','mushrooms');                                         de = @('pilz','champig','steinpilz') }
    @{ name = "Spinach";      en = @('spinach');                                                      de = @('spinat') }
    @{ name = "Lettuce";      en = @('lettuce');                                                      de = @('salat') }
    @{ name = "Soy";          en = @('soybean','soybeans','soy sauce','soymilk','tofu');              de = @('soja','tofu','sojabohne','sojamilch') }
    @{ name = "Beans";        en = @('beans','bean,','black beans','kidney beans','navy beans');      de = @('bohne','bohnen') }
    @{ name = "Lentil";       en = @('lentil','lentils');                                             de = @('linse','linsen') }
    @{ name = "Peas";         en = @('peas','pea ');                                                  de = @('erbse','erbsen') }
    @{ name = "Honey";        en = @('honey');                                                        de = @('honig') }
    @{ name = "Sugar";        en = @('sugar,','sugar ','sucrose');                                    de = @('zucker') }
    @{ name = "Oil";          en = @('oil,','oil ','olive oil','sunflower oil','canola');             de = @('oel','öl','olivenöl','olivenoel') }
    @{ name = "Vinegar";      en = @('vinegar');                                                      de = @('essig','balsamico') }
    @{ name = "Chocolate";    en = @('chocolate');                                                    de = @('schokolade','schoko','kakao') }
    @{ name = "Coffee";       en = @('coffee');                                                       de = @('kaffee') }
    @{ name = "Tea";          en = @('tea,','tea ','tea(');                                           de = @('tee','grüntee','gruentee','schwarztee') }
)

function Strip([string]$s) {
    if ($null -eq $s) { return '' }
    $s = $s.ToLowerInvariant()
    $s = $s.Replace('ä','ae').Replace('ö','oe').Replace('ü','ue').Replace('ß','ss')
    return $s
}

# CSV parsen — Semikolon-separiert, Anführungszeichen können kommen
$raw = Get-Content $CsvPath -Raw -Encoding UTF8
# Header skip
$lines = $raw -split "`r?`n" | Where-Object { $_ -and -not $_.StartsWith('#') }
$header = $lines[0]
$dataLines = $lines | Select-Object -Skip 1 | Where-Object { $_ }

$mismatches = @()
$total = 0
foreach ($line in $dataLines) {
    # Naive split (genug fuer ersten 5 Spalten — micronutrients_json kommt erst spaeter)
    # name_en kann selbst Kommas haben aber kein Semikolon
    $cols = $line.Split(';')
    if ($cols.Count -lt 5) { continue }
    $total++
    $fdcId = $cols[0]
    $nameDe = $cols[1]
    $nameEn = $cols[2]
    $deN = Strip $nameDe
    $enN = Strip $nameEn

    foreach ($rule in $rules) {
        $enHit = $false
        foreach ($t in $rule.en) { if ($enN.Contains($t.ToLowerInvariant())) { $enHit = $true; break } }
        if (-not $enHit) { continue }
        $deHit = $false
        foreach ($r in $rule.de) { if ($deN.Contains($r.ToLowerInvariant())) { $deHit = $true; break } }
        if (-not $deHit) {
            $mismatches += [pscustomobject]@{
                Rule   = $rule.name
                FdcId  = $fdcId
                NameDe = $nameDe
                NameEn = $nameEn
            }
            break  # eine Regel pro Row reicht
        }
    }
}

Write-Host "=== Translation-Mismatch-Audit ===" -ForegroundColor Cyan
Write-Host "Total rows scanned: $total"
Write-Host "Mismatch candidates: $($mismatches.Count)"
Write-Host ""

# Gruppiert nach Rule
$grouped = $mismatches | Group-Object -Property Rule | Sort-Object Count -Descending
Write-Host "=== Mismatches per Rule ===" -ForegroundColor Yellow
$grouped | Format-Table @{l='Rule';e='Name'}, Count -AutoSize

Write-Host "=== Full Mismatch List ===" -ForegroundColor Yellow
$mismatches | Sort-Object Rule, NameDe | Format-Table -AutoSize | Out-String -Width 200

# Write to file
$out = @()
$out += "# Translation-Mismatch-Audit - $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
$out += "# Total scanned: $total, Mismatches: $($mismatches.Count)"
$out += ""
$out += "## Per-Rule-Count"
$out += ($grouped | Format-Table @{l='Rule';e='Name'}, Count -AutoSize | Out-String).Trim()
$out += ""
$out += "## Full List (sorted by Rule, NameDe)"
$out += ($mismatches | Sort-Object Rule, NameDe | Format-Table -AutoSize | Out-String -Width 200).Trim()
$out -join "`n" | Out-File -FilePath $OutPath -Encoding UTF8

Write-Host "Wrote $OutPath" -ForegroundColor Green
