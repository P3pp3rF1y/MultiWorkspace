param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$BaseUrl = "",
    [ValidateSet("", "emi", "jei", "rei")]
    [string]$Viewer = "",
    [string]$Suite = "sophisticatedbackpacks",
    [int]$Limit = 200,
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
    [switch]$MaximizeClient
)

$ErrorActionPreference = "Stop"
$script:Fixtures = @{}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-BridgeJson {
    param(
        [Parameter(Mandatory = $true)] [string]$Method,
        [Parameter(Mandatory = $true)] [string]$Path,
        [object]$Body = $null
    )

    $uri = "$BaseUrl$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress -Depth 16) -TimeoutSec 20
}

function Get-SuitePath {
    param([string]$SuiteName)

    if (Test-Path $SuiteName -PathType Leaf) {
        return (Resolve-Path $SuiteName).Path
    }
    return Join-Path $PSScriptRoot "recipe-viewer-suites\$SuiteName.json"
}

function Get-Alternatives {
    param([object[]]$Ingredients)

    $alternatives = @()
    foreach ($ingredient in @($Ingredients)) {
        foreach ($alternative in @($ingredient.alternatives)) {
            $alternatives += $alternative
        }
    }
    return $alternatives
}

function Get-ObjectProperty {
    param(
        [object]$Object,
        [string]$Name
    )

    if ($null -eq $Object) {
        return $null
    }
    if ($Object -is [System.Collections.IDictionary] -and $Object.Contains($Name)) {
        return $Object[$Name]
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function Get-PathValue {
    param(
        [object]$Object,
        [string]$Path
    )

    $current = $Object
    foreach ($segment in $Path.Split('.')) {
        $current = Get-ObjectProperty -Object $current -Name $segment
        if ($null -eq $current) {
            return $null
        }
    }
    return $current
}

function Resolve-Selector {
    param([object]$Selector)

    if ($Selector -is [string]) {
        Assert-True $script:Fixtures.ContainsKey($Selector) "Unknown recipe-viewer fixture '$Selector'."
        return $script:Fixtures[$Selector]
    }
    return $Selector
}

function Test-MatchObject {
    param(
        [object]$Object,
        [object]$Match
    )

    if ($null -eq $Match) {
        return $true
    }
    foreach ($property in $Match.PSObject.Properties) {
        $actual = Get-PathValue -Object $Object -Path $property.Name
        if ($null -eq $actual) {
            return $false
        }
        if ($actual -is [array]) {
            if ($actual -notcontains $property.Value) {
                return $false
            }
        } elseif ($actual.ToString() -ne $property.Value.ToString()) {
            return $false
        }
    }
    return $true
}

function Test-NotMatchObject {
    param(
        [object]$Object,
        [object]$Match
    )

    if ($null -eq $Match) {
        return $true
    }
    foreach ($property in $Match.PSObject.Properties) {
        $actual = Get-PathValue -Object $Object -Path $property.Name
        if ($null -eq $actual) {
            continue
        }
        if ($actual -is [array]) {
            if ($actual -contains $property.Value) {
                return $false
            }
        } elseif ($actual.ToString() -eq $property.Value.ToString()) {
            return $false
        }
    }
    return $true
}

function Test-StackMatch {
    param(
        [object]$Stack,
        [object]$Expectation
    )

    $SameAs = Get-ObjectProperty -Object $Expectation -Name "sameAs"
    if ($SameAs) {
        $Expectation = Resolve-Selector -Selector $SameAs
    }
    $AnyOf = Get-ObjectProperty -Object $Expectation -Name "anyOf"
    if ($null -ne $AnyOf) {
        foreach ($alternative in @($AnyOf)) {
            if (Test-StackMatch -Stack $Stack -Expectation $alternative) {
                return $true
            }
        }
        return $false
    }
    $Item = Get-ObjectProperty -Object $Expectation -Name "item"
    $ItemPattern = Get-ObjectProperty -Object $Expectation -Name "itemPattern"
    $ComponentsPattern = Get-ObjectProperty -Object $Expectation -Name "componentsPattern"
    if (-not [string]::IsNullOrWhiteSpace($Item) -and $Stack.id -ne $Item) {
        return $false
    }
    if (-not [string]::IsNullOrWhiteSpace($ItemPattern) -and $Stack.id -notmatch $ItemPattern) {
        return $false
    }
    if (-not [string]::IsNullOrWhiteSpace($ComponentsPattern) -and [string]$Stack.components -notmatch $ComponentsPattern) {
        return $false
    }
    $Absent = Get-ObjectProperty -Object $Expectation -Name "absent"
    if ($null -ne $Absent) {
        foreach ($path in @($Absent)) {
            if ($null -ne (Get-PathValue -Object $Stack -Path $path)) {
                return $false
            }
        }
    }
    if (-not (Test-MatchObject -Object $Stack -Match (Get-ObjectProperty -Object $Expectation -Name "match"))) {
        return $false
    }
    if (-not (Test-NotMatchObject -Object $Stack -Match (Get-ObjectProperty -Object $Expectation -Name "notMatch"))) {
        return $false
    }
    return $true
}

function Test-RecipeContainsStack {
    param(
        [object]$Recipe,
        [ValidateSet("inputs", "outputs")]
        [string]$Side,
        [object]$Expectation
    )

    foreach ($stack in (Get-Alternatives $Recipe.$Side)) {
        if (Test-StackMatch -Stack $stack -Expectation $Expectation) {
            return $true
        }
    }
    return $false
}

function Test-RecipeSlotMatch {
    param(
        [object]$Recipe,
        [ValidateSet("inputs", "outputs")]
        [string]$Side,
        [int]$Index,
        [object]$Expectation
    )

    if ($Index -ge @($Recipe.$Side).Count) {
        return $false
    }
    $empty = Get-ObjectProperty -Object $Expectation -Name "empty"
    if ($null -ne $empty) {
        return [bool]$empty -eq (@(Get-Alternatives @($Recipe.$Side[$Index])).Count -eq 0)
    }
    foreach ($stack in (Get-Alternatives @($Recipe.$Side[$Index]))) {
        if (Test-StackMatch -Stack $stack -Expectation $Expectation) {
            return $true
        }
    }
    return $false
}

function Test-RecipeSlotsMatch {
    param(
        [object]$Recipe,
        [ValidateSet("inputs", "outputs")]
        [string]$Side,
        [object[]]$Expectations
    )

    for ($i = 0; $i -lt @($Expectations).Count; $i++) {
        $expectation = $Expectations[$i]
        if ($null -eq $expectation) {
            continue
        }
        if (-not (Test-RecipeSlotMatch -Recipe $Recipe -Side $Side -Index $i -Expectation $expectation)) {
            return $false
        }
    }
    return $true
}

function Test-RecipeNonEmptySlotCount {
    param(
        [object]$Recipe,
        [ValidateSet("inputs", "outputs")]
        [string]$Side,
        [int]$ExpectedCount
    )

    $actualCount = 0
    foreach ($slot in @($Recipe.$Side)) {
        if (@(Get-Alternatives @($slot)).Count -gt 0) {
            $actualCount++
        }
    }
    return $actualCount -eq $ExpectedCount
}

function Test-RecipeMatch {
    param(
        [object]$Recipe,
        [object]$Expectation
    )

    if (-not [string]::IsNullOrWhiteSpace($Expectation.categoryPattern) -and $Recipe.category -notmatch $Expectation.categoryPattern -and $Recipe.categoryName -notmatch $Expectation.categoryPattern) {
        return $false
    }
    foreach ($input in @($Expectation.inputs)) {
        if (-not (Test-RecipeContainsStack -Recipe $Recipe -Side inputs -Expectation $input)) {
            return $false
        }
    }
    $inputSlots = Get-ObjectProperty -Object $Expectation -Name "inputSlots"
    if ($null -ne $inputSlots -and -not (Test-RecipeSlotsMatch -Recipe $Recipe -Side inputs -Expectations @($inputSlots))) {
        return $false
    }
    $inputSlotCount = Get-ObjectProperty -Object $Expectation -Name "inputNonEmptySlotCount"
    if ($null -ne $inputSlotCount -and -not (Test-RecipeNonEmptySlotCount -Recipe $Recipe -Side inputs -ExpectedCount $inputSlotCount)) {
        return $false
    }
    foreach ($output in @($Expectation.outputs)) {
        if (-not (Test-RecipeContainsStack -Recipe $Recipe -Side outputs -Expectation $output)) {
            return $false
        }
    }
    $outputSlots = Get-ObjectProperty -Object $Expectation -Name "outputSlots"
    if ($null -ne $outputSlots -and -not (Test-RecipeSlotsMatch -Recipe $Recipe -Side outputs -Expectations @($outputSlots))) {
        return $false
    }
    $outputSlotCount = Get-ObjectProperty -Object $Expectation -Name "outputNonEmptySlotCount"
    if ($null -ne $outputSlotCount -and -not (Test-RecipeNonEmptySlotCount -Recipe $Recipe -Side outputs -ExpectedCount $outputSlotCount)) {
        return $false
    }
    $notInputs = Get-ObjectProperty -Object $Expectation -Name "notInputs"
    if ($null -ne $notInputs) {
        foreach ($input in @($notInputs)) {
            if (Test-RecipeContainsStack -Recipe $Recipe -Side inputs -Expectation $input) {
                return $false
            }
        }
    }
    $notOutputs = Get-ObjectProperty -Object $Expectation -Name "notOutputs"
    if ($null -ne $notOutputs) {
        foreach ($output in @($notOutputs)) {
            if (Test-RecipeContainsStack -Recipe $Recipe -Side outputs -Expectation $output) {
                return $false
            }
        }
    }
    return $true
}

function Get-MatchingRecipes {
    param(
        [object[]]$Recipes,
        [object]$Expectation
    )

    $matches = @()
    foreach ($recipe in @($Recipes)) {
        if (Test-RecipeMatch -Recipe $recipe -Expectation $Expectation) {
            $matches += $recipe
        }
    }
    return $matches
}

function Invoke-Query {
    param([object]$Test)

    $body = @{ limit = $Limit }
    if ($Test.focus) {
        $body.focus = Resolve-Selector -Selector $Test.focus
    } else {
        $body.item = $Test.item
    }
    $focusDescription = if ($Test.focus) { $Test.focus } else { $Test.item }
    $deadline = (Get-Date).AddSeconds([Math]::Min($TimeoutSeconds, 60))
    do {
        $query = Invoke-BridgeJson -Method Post -Path "/recipe-viewer/query" -Body $body
        Assert-True $query.ok "Query failed for '$focusDescription': $($query.error)"
        if (($query.recipeCount + $query.useCount) -gt 0) {
            return $query
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    Assert-True $query.ok "Query failed for '$focusDescription': $($query.error)"
    return $query
}

function Stop-AutomationClient {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        return
    }
    try {
        Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    } catch {
        Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)"
    }
}

function Test-RecipeExpectation {
    param([object]$Test)

    $query = Invoke-Query -Test $Test
    $matches = Get-MatchingRecipes -Recipes $query.recipes -Expectation $Test.expect
    $matchCount = @($matches).Count
    $expectedMatches = Get-ObjectProperty -Object $Test.expect -Name "expectedMatches"
    if ($null -ne $expectedMatches) {
        Assert-True ($matchCount -eq $expectedMatches) "Recipe expectation failed for '$($Test.name)': expected $expectedMatches matches but got $matchCount. Matches: $(@($matches | ForEach-Object { $_.id + ' [' + $_.category + ']' }) -join '; ')"
        return $matches
    }
    Assert-True ($matchCount -gt 0) "Recipe expectation failed for '$($Test.name)'. Candidate recipes: $(@($query.recipes | ForEach-Object { $_.id + ' [' + $_.category + ']' }) -join '; ')"
    return $matches
}

function Test-UseExpectation {
    param([object]$Test)

    $query = Invoke-Query -Test $Test
    $matches = Get-MatchingRecipes -Recipes $query.uses -Expectation $Test.expect
    $matchCount = @($matches).Count
    $expectedMatches = Get-ObjectProperty -Object $Test.expect -Name "expectedMatches"
    if ($null -ne $expectedMatches) {
        Assert-True ($matchCount -eq $expectedMatches) "Use expectation failed for '$($Test.name)': expected $expectedMatches matches but got $matchCount. Matches: $(@($matches | ForEach-Object { $_.id + ' [' + $_.category + ']' }) -join '; ')"
        return $matches
    }
    Assert-True ($matchCount -gt 0) "Use expectation failed for '$($Test.name)'. Candidate uses: $(@($query.uses | ForEach-Object { $_.id + ' [' + $_.category + ']' }) -join '; ')"
    return $matches
}

function Test-ChainExpectation {
    param([object]$Test)

    $matches = @()
    foreach ($step in @($Test.steps)) {
        $query = Invoke-Query -Test ([pscustomobject]@{ item = $step.source; focus = $step.focus })
        $expectation = [pscustomobject]@{
            categoryPattern = $step.categoryPattern
            inputs = @([pscustomobject]@{ item = $step.source; itemPattern = ""; componentsPattern = ""; sameAs = $step.sameAsSource })
            outputs = @()
        }
        if (-not [string]::IsNullOrWhiteSpace($step.result)) {
            $expectation.outputs = @([pscustomobject]@{ item = $step.result; itemPattern = ""; componentsPattern = "" })
        }
        $match = Get-MatchingRecipes -Recipes $query.uses -Expectation $expectation
        $targetDescription = if ([string]::IsNullOrWhiteSpace($step.result)) { "a $($step.categoryPattern) usage" } else { "'$($step.result)'" }
        Assert-True (@($match).Count -gt 0) "Chain expectation failed for '$($Test.name)': '$($step.source)' did not lead to $targetDescription."
        $matches += $match
    }
    return $matches
}

function Test-TransferExpectation {
    param([object]$Test)

    $body = @{}
    if ($Test.focus) {
        $body.focus = Resolve-Selector -Selector $Test.focus
    } else {
        $body.item = $Test.item
    }
    $transfer = Invoke-BridgeJson -Method Post -Path "/recipe-viewer/backpack-crafting-transfer" -Body $body
    Assert-True $transfer.ok "Transfer expectation failed for '$($Test.name)': $($transfer.error)"
    $expect = Get-ObjectProperty -Object $Test -Name "expect"
    if ($null -ne $expect) {
        foreach ($property in $expect.PSObject.Properties) {
            $actual = Get-PathValue -Object $transfer -Path $property.Name
            Assert-True ($null -ne $actual) "Transfer expectation failed for '$($Test.name)': missing property '$($property.Name)'."
            Assert-True ($actual.ToString() -eq $property.Value.ToString()) "Transfer expectation failed for '$($Test.name)': expected '$($property.Name)' to be '$($property.Value)' but got '$actual'."
        }
    }
    return @($transfer)
}

$startedClient = $false

try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true }
        if ($MaximizeClient) {
            $readyArgs.Maximize = $true
        }
        if (-not [string]::IsNullOrWhiteSpace($Viewer)) {
            $readyArgs.RecipeViewer = $Viewer
        }
        $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
        $BaseUrl = $ready.baseUrl
        $startedClient = $true
    }

    $suitePath = Get-SuitePath -SuiteName $Suite
    Assert-True (Test-Path $suitePath) "Recipe viewer regression suite not found: $suitePath"
    $suiteData = Get-Content $suitePath -Raw | ConvertFrom-Json
    $script:Fixtures = @{}
    if ($suiteData.fixtures) {
        foreach ($fixture in $suiteData.fixtures.PSObject.Properties) {
            $script:Fixtures[$fixture.Name] = $fixture.Value
        }
    }

    $state = Invoke-BridgeJson -Method Get -Path "/recipe-viewer/state"
    Assert-True $state.ok "Recipe viewer is not ready: $($state.error)"
    if (-not [string]::IsNullOrWhiteSpace($Viewer)) {
        Assert-True ($state.viewer -eq $Viewer) "Expected viewer '$Viewer' but active viewer is '$($state.viewer)'."
    }

    $results = @()
    foreach ($test in @($suiteData.tests)) {
        if ($test.viewers -and @($test.viewers) -notcontains $state.viewer) {
            continue
        }
        switch ($test.type) {
            "recipe" { $matched = Test-RecipeExpectation -Test $test }
            "use" { $matched = Test-UseExpectation -Test $test }
            "chain" { $matched = Test-ChainExpectation -Test $test }
            "transfer" { $matched = Test-TransferExpectation -Test $test }
            default { throw "Unknown recipe viewer regression test type: $($test.type)" }
        }
        $results += [pscustomobject]@{ name = $test.name; type = $test.type; passed = $true; matched = $matched }
        Write-Host "PASS $($test.name)"
    }

    [pscustomobject]@{
        ok = $true
        suite = $suiteData.name
        viewer = $state.viewer
        baseUrl = $BaseUrl
        passed = $results.Count
        results = $results
    }
} finally {
    if ($startedClient) {
        Stop-AutomationClient
    }
}
