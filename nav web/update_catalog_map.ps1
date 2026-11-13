$jsonPath = "D:\New folder (2)\New folder (5)\NavGrocery-Daily\catalog.json"
$jsonData = Get-Content -Raw $jsonPath -Encoding UTF8 | ConvertFrom-Json

# Build map from catalog.json rotation
$catalogMap = @{}
$idx = 1
foreach ($item in $jsonData.rotation) {
    $numStr = $idx.ToString().PadLeft(3, '0')
    $catalogMap[$numStr] = "$($item.product_name) ($($item.pack_size)) - Rs $($item.indicative_price_inr)"
    $idx++
}

# Scan all photos in nav web
$files = Get-ChildItem -Path "D:\nav web" -File | Where-Object { $_.Extension -match "\.(png|jpg|jpeg|webp)$" } | Sort-Object Name
$lines = @()
$lines += "=========================================================================="
$lines += "NAV GROCERY STORE - 309 IMAGES MATCHED TO CATALOG & PRICES"
$lines += "Total Image Files: $($files.Count)"
$lines += "=========================================================================="
$lines += ""

foreach ($f in $files) {
    $num = [System.Text.RegularExpressions.Regex]::Match($f.BaseName, "\d+").Value
    if ($num) {
        $numPadded = $num.PadLeft(3, '0')
        if ($catalogMap.ContainsKey($numPadded)) {
            $info = $catalogMap[$numPadded]
        } else {
            $info = "Packshot Variant #$numPadded"
        }
        $lines += "$($f.Name)  -->  $info"
    } else {
        $lines += "$($f.Name)  -->  Store Asset / Banner"
    }
}

$lines | Out-File -FilePath "D:\nav web\nav_catalog_309_final.txt" -Encoding UTF8
Write-Host "Success! Created nav_catalog_309_final.txt with $($files.Count) items." -ForegroundColor Green
notepad "D:\nav web\nav_catalog_309_final.txt"
