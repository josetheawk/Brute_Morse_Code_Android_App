# ========================================
# Fix Remaining com.example References
# ========================================

$oldPackage = "com.example.brutemorse"
$newPackage = "com.awkandtea.brutemorse"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FIXING REMAINING PACKAGE REFERENCES" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Searching for remaining references to: $oldPackage" -ForegroundColor Yellow

# Find all Kotlin files
$ktFiles = Get-ChildItem -Path "app/src" -Filter "*.kt" -Recurse
$totalFixed = 0
$filesFixed = 0

foreach ($file in $ktFiles) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Count occurrences before
    $beforeCount = ([regex]::Matches($content, [regex]::Escape($oldPackage))).Count
    
    if ($beforeCount -gt 0) {
        # Replace ALL occurrences of old package with new package
        $content = $content -replace [regex]::Escape($oldPackage), $newPackage
        
        # Count occurrences after
        $afterCount = ([regex]::Matches($content, [regex]::Escape($oldPackage))).Count
        
        $fixed = $beforeCount - $afterCount
        
        if ($fixed -gt 0) {
            Set-Content $file.FullName $content -NoNewline
            Write-Host "[FIXED] $($file.Name): $fixed references" -ForegroundColor Green
            $totalFixed += $fixed
            $filesFixed++
        }
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Files fixed: " -NoNewline
Write-Host $filesFixed -ForegroundColor Green
Write-Host "Total references fixed: " -NoNewline
Write-Host $totalFixed -ForegroundColor Green

if ($totalFixed -gt 0) {
    Write-Host "`n[SUCCESS] All references updated!" -ForegroundColor Green
    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "  1. In Android Studio: File -> Invalidate Caches -> Invalidate and Restart" -ForegroundColor White
    Write-Host "  2. After restart: Build -> Clean Project" -ForegroundColor White
    Write-Host "  3. Then: Build -> Rebuild Project" -ForegroundColor White
} else {
    Write-Host "`n[INFO] No remaining references found!" -ForegroundColor Cyan
}