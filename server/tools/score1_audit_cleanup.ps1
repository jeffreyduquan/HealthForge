$path = "C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi.csv"
$lines = [System.IO.File]::ReadAllLines($path, [System.Text.Encoding]::UTF8)
$remove = @(
  "Sriracha;1;Gewuerze",
  "Mayonnaise;1;Gewuerze",
  "Currypulver;1;Gewuerze",
  "Sumach;1;Gewuerze",
  "Lupinen;1;Gemuese",
  "Veggie Burger;1;Fleisch",
  "Nougat;1;Suess",
  "Energy Drink;1;Getraenke",
  "Rosine;1;Fruechte"
)
$out = New-Object System.Collections.Generic.List[string]
foreach ($l in $lines) {
  $t = $l.Trim()
  if ($remove -contains $t) { continue }
  if ($t -eq "Schwertfisch;1;Fisch") { $out.Add("Schwertfisch;3;Fisch"); continue }
  if ($t -eq "BBQ;1;Gewuerze") { $out.Add("BBQ;3;Gewuerze"); continue }
  if ($t -eq "Pesto;1;Gewuerze") { $out.Add("Pesto;3;Gewuerze"); continue }
  $out.Add($l)
}
[System.IO.File]::WriteAllLines($path, $out.ToArray(), (New-Object System.Text.UTF8Encoding $false))
Copy-Item $path C:\Users\jawra\Documents\Projects\HealthForge\server\build\resources\main\seed\sighi.csv -Force
"sighi.csv updated, total {0} lines" -f $out.Count
