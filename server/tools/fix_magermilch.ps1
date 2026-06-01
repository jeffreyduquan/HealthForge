$path = "C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\usda_fdc_curated.csv"
$lines = [System.IO.File]::ReadAllLines($path, [System.Text.Encoding]::UTF8)
$out = New-Object System.Collections.Generic.List[string]
$inserted = $false
foreach ($l in $lines) {
  if ($l.StartsWith("172195;Magermilch;")) {
    # Rename 172195 to Magermilchpulver
    $patched = $l -replace "^172195;Magermilch;", "172195;Magermilchpulver;"
    $out.Add($patched)
    if (-not $inserted) {
      # Append fluid Magermilch right after
      $micros = '{"calcium":122.0,"eisen":0.03,"kalium":156.0,"kupfer":0.013,"magnesium":11.0,"mangan":0.003,"natrium":42.0,"phosphor":101.0,"selen":3.1,"vitamin_a":61.0,"vitamin_b1":0.045,"vitamin_b12":0.5,"vitamin_b2":0.182,"vitamin_b3":0.094,"vitamin_b5":0.357,"vitamin_b6":0.037,"vitamin_b9":5.0,"vitamin_c":0.0,"vitamin_d":1.2,"vitamin_e":0.01,"vitamin_k":0.0,"zink":0.42}'
      $microsEsc = $micros -replace '"', '""'
      $newRow = '171269;Magermilch;Milk, nonfat, fluid, with added vitamin A and vitamin D (fat free or skim);;;34;3.37;4.96;5.09;0.08;0.056;0;0.105;"' + $microsEsc + '"'
      $out.Add($newRow)
      $inserted = $true
    }
  } else {
    $out.Add($l)
  }
}
[System.IO.File]::WriteAllLines($path, $out.ToArray(), (New-Object System.Text.UTF8Encoding $false))
Copy-Item $path C:\Users\jawra\Documents\Projects\HealthForge\server\build\resources\main\seed\usda_fdc_curated.csv -Force
"CSV updated, total lines: {0}, inserted fluid Magermilch: {1}" -f $out.Count, $inserted
