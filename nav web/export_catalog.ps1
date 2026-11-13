Set-Location "D:\nav web"

# 1. Read existing price list text file
$txtFile = Get-ChildItem -Path "D:\nav web" -Filter "*pricelist*.txt" | Select-Object -First 1
if (-not $txtFile) {
    $txtFile = Get-ChildItem -Path "D:\nav web" -Filter "*309*.txt" | Select-Object -First 1
}

$rawLines = Get-Content $txtFile.FullName -Encoding UTF8
$products = @()

foreach ($line in $rawLines) {
    if ($line -match '^(\d+)\.png\s+-->\s+(.+?)\s*\((.+?)\)\s*-\s*Rs\s*(\d+)') {
        $products += [PSCustomObject]@{
            "SKU"       = $matches[1]
            "Image"     = "$($matches[1]).png"
            "Item Name" = $matches[2].Trim()
            "Pack Size" = $matches[3].Trim()
            "Price_INR" = [int]$matches[4]
        }
    }
}

# 2. Export clean CSV file for Excel
$csvPath = "D:\nav web\nav_store_308_catalog.csv"
$products | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

# 3. Export JSON file for Website
$jsonOutput = [ordered]@{
    store          = "Nav Grocery Store"
    location       = "Sandhaur, Punjab"
    website        = "www.navgrocerystore.com"
    phone          = "8847370905"
    total_products = $products.Count
    products       = $products
}

$jsonPath = "D:\nav web\products_catalog_308.json"
$jsonOutput | ConvertTo-Json -Depth 5 | Out-File -FilePath $jsonPath -Encoding UTF8

Write-Host "Success! Created CSV: $csvPath" -ForegroundColor Green
Write-Host "Success! Created JSON: $jsonPath with $($products.Count) items" -ForegroundColor Green

# 4. Open CSV in Excel
Invoke-Item $csvPath
