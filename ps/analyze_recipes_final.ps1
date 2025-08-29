# File: analyze_recipes_final.ps1
# Purpose: Scans all recipe files, categorizes and parses them by type, and displays them in tables.
# Version: 4 (Final) - Enhanced debugging and robust error handling.

# --- Settings ---
# Change this path to the root folder containing all recipes in your project
$recipesRootPath = "D:\moddev\neoforge\1.21.1Koniavacraft\src\generated\resources\data\koniava\recipe"

# --- Main Logic ---
Write-Host "--- Koniavacraft Recipe Analyzer v4 ---" -ForegroundColor White
Write-Host "Scanning recipe root path: $recipesRootPath" -ForegroundColor Yellow

if (-not (Test-Path $recipesRootPath)) {
    Write-Host "ERROR: Path not found! Please check the `$recipesRootPath` variable." -ForegroundColor Red
    Write-Host "Script terminated."
    # Pause to allow user to see the error in some environments
    if ($Host.Name -eq "ConsoleHost") { Read-Host "Press Enter to exit" }
    return
}

$foundFiles = Get-ChildItem -Path $recipesRootPath -Filter "*.json" -Recurse
if ($null -eq $foundFiles) {
    Write-Host "WARNING: No .json files were found in the specified path." -ForegroundColor Yellow
    Write-Host "Script terminated."
    if ($Host.Name -eq "ConsoleHost") { Read-Host "Press Enter to exit" }
    return
}

Write-Host "Found $($foundFiles.Count) .json files. Starting analysis..." -ForegroundColor Green

$allParsedRecipes = $foundFiles | ForEach-Object {
    $fileInfo = $_
    Write-Host "  - Processing: $($fileInfo.Name)" -ForegroundColor Gray
    try {
        $content = Get-Content -Path $fileInfo.FullName -Raw | ConvertFrom-Json -ErrorAction Stop

        if ($null -eq $content.type) {
            throw "File '$($fileInfo.Name)' is missing the 'type' field."
        }
        $recipeType = $content.type

        $outputItem = "N/A"
        $details = "N/A"

        switch ($recipeType) {
            "koniava:mana_fuel" {
                $outputItem = if ($null -ne $content.ingredient.item) { $content.ingredient.item } else { "Unknown Ingredient" }
                $burnTime = if ($null -ne $content.burn_time) { $content.burn_time } else { 0 }
                $mana = if ($null -ne $content.mana) { $content.mana } else { 0 }
                $energy = if ($null -ne $content.energy) { $content.energy } else { 0 }
                $details = "Burn: $burnTime, Mana: $mana/t, Energy: $energy/t"
                break
            }
            "minecraft:crafting_shaped" {
                if ($null -ne $content.result) {
                    $resultId = if ($null -ne $content.result.id) { $content.result.id } elseif ($null -ne $content.result.item) { $content.result.item } else { "Unknown Result" }
                    $resultCount = if ($null -ne $content.result.count) { $content.result.count } else { 1 }
                    $outputItem = "$($resultCount)x $($resultId)"
                } else {
                    $outputItem = "ERROR: No result field"
                }
                
                if ($null -ne $content.pattern) {
                    $patternString = ($content.pattern | ForEach-Object { $_.Trim() }) -join " | "
                    $details = "Pattern: $patternString"
                } else {
                    $details = "ERROR: No pattern field"
                }
                break
            }
            default {
                $details = "Unknown recipe type, skipping detailed analysis."
                break
            }
        }

        [PSCustomObject]@{
            FileName   = $fileInfo.Name
            Type       = $recipeType
            Output     = $outputItem
            Details    = $details
        }
    }
    catch {
        Write-Host "  WARNING: Failed to parse file '$($fileInfo.Name)': $($_.Exception.Message)" -ForegroundColor Magenta
    }
}

$validRecipes = $allParsedRecipes | Where-Object { $null -ne $_ }

if ($validRecipes) {
    Write-Host "`nAnalysis complete! Displaying recipe overview by type..." -ForegroundColor Green
    
    $validRecipes | Sort-Object -Property Type, FileName | Group-Object -Property Type | ForEach-Object {
        Write-Host "`n--- Recipe Type: $($_.Name) ---" -ForegroundColor Cyan
        $_.Group | Format-Table -Property FileName, Output, Details -AutoSize
    }
} else {
    Write-Host "`nWARNING: Although files were found, none could be successfully parsed. Please check file contents and script logic." -ForegroundColor Yellow
}

if ($Host.Name -eq "ConsoleHost") {
    Read-Host "Script finished. Press Enter to exit"
}