# Sucht im Voll-Seed nach FDC-Quellen fuer die 12 wichtigsten verlorenen DE-Foods
$f="C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\usda_fdc.csv"
$patterns = @(
    @{label='Smoked-Salmon';     pat='Fish, salmon, smoked'}
    @{label='Salmon-Raw';        pat='salmon, atlantic, wild, raw'}
    @{label='Ghee';              pat='clarified butter'}
    @{label='Halloumi';          pat='Halloumi'}
    @{label='Marzipan';          pat='Marzipan'}
    @{label='Tzatziki';          pat='Tzatziki'}
    @{label='Gnocchi';           pat='Gnocchi'}
    @{label='Sourdough';         pat='Bread, sourdough'}
    @{label='Multigrain';        pat='multi-grain|multigrain'}
    @{label='ChickenGround';     pat='Chicken, ground, raw'}
    @{label='SoyProtein';        pat='soy protein|textured vegetable protein'}
    @{label='Udon';              pat='Noodles, japanese, udon'}
    @{label='RicePudding';       pat='Rice pudding'}
)
foreach ($p in $patterns) {
    Write-Host "=== $($p.label) :: $($p.pat) ===" -ForegroundColor Cyan
    $matches = Select-String -Path $f -Pattern $p.pat -CaseSensitive:$false | Select-Object -First 4
    foreach ($m in $matches) {
        $cols = $m.Line.Split(';')
        if ($cols.Count -ge 6) {
            Write-Host ("  fdc={0,-10} kcal={1,-6} | {2} | {3}" -f $cols[0], $cols[5], $cols[1], $cols[2])
        }
    }
    Write-Host ""
}
