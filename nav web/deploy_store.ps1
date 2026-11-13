Set-Location "D:\nav web"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "NAV GROCERY STORE - COMPLETE 308 SKU WEB DEPLOYMENT" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Target Web Project Directory
$targetStoreDir = "D:\nav g store"
$publicImagesDir = "$targetStoreDir\public\images\products"
$publicDataDir   = "$targetStoreDir\src\data"

# Fallback: if default structure differs, create standard web assets folder
if (-not (Test-Path $publicImagesDir)) {
    New-Item -ItemType Directory -Force -Path $publicImagesDir | Out-Null
}
if (-not (Test-Path $publicDataDir)) {
    New-Item -ItemType Directory -Force -Path $publicDataDir | Out-Null
}

# 2. Copy Verified JSON Catalog
$sourceJson = "D:\nav web\products_catalog_308.json"
Copy-Item $sourceJson -Destination "$publicDataDir\products.json" -Force
Copy-Item $sourceJson -Destination "$targetStoreDir\catalog.json" -Force
Write-Host "[✓] Deployed catalog JSON to web directory" -ForegroundColor Green

# 3. Synchronize All 308 Product Images
$images = Get-ChildItem -Path "D:\nav web" -Filter "*.png"
$count = 0
foreach ($img in $images) {
    Copy-Item $img.FullName -Destination $publicImagesDir -Force
    $count++
}
Write-Host "[✓] Synchronized $count product images to $publicImagesDir" -ForegroundColor Green

# 4. Summary Status
Write-Host ""
Write-Host "Deployment Completed Successfully!" -ForegroundColor Yellow
Write-Host "Store: Nav Grocery Store, Sandhaur" -ForegroundColor White
Write-Host "Catalog File: $publicDataDir\products.json" -ForegroundColor White
Write-Host "Total Live SKUs: $count" -ForegroundColor White
Write-Host "Images Directory: $publicImagesDir" -ForegroundColor White
