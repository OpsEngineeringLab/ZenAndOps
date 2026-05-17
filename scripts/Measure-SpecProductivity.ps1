<#
.SYNOPSIS
    Measures productivity metrics for each spec (feature/bugfix) in the ZenAndOps project.

.DESCRIPTION
    Analyzes Git history to extract per-spec metrics:
    - Lines added / removed / net
    - Files modified
    - Number of task commits
    - Elapsed time (first task commit to release commit)
    - Lines per hour (estimated)

.PARAMETER OutputFormat
    Output format: "Table" (default), "CSV", or "JSON"

.PARAMETER OutputPath
    Optional file path to save the report. If omitted, outputs to console.

.EXAMPLE
    .\Measure-SpecProductivity.ps1
    .\Measure-SpecProductivity.ps1 -OutputFormat CSV -OutputPath .\report.csv
    .\Measure-SpecProductivity.ps1 -OutputFormat JSON -OutputPath .\report.json
#>

param(
    [ValidateSet("Table", "CSV", "JSON")]
    [string]$OutputFormat = "Table",

    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

# Ensure we are in a git repository
try {
    $null = git rev-parse --is-inside-work-tree 2>&1
} catch {
    Write-Error "This script must be run from within a Git repository."
    exit 1
}

# Get all local branches matching feature-* or bugfix-*
$branches = git branch --list "feature-*" --list "bugfix-*" 2>&1 |
    ForEach-Object { $_.Trim().TrimStart("* ") } |
    Where-Object { $_ -match "^(feature|bugfix)-\d{4}\.\d{6}$" } |
    Sort-Object

if (-not $branches -or $branches.Count -eq 0) {
    Write-Warning "No feature/bugfix branches found matching the expected pattern."
    exit 0
}

# Get all tags for version mapping
$tags = git tag --list 2>&1 | ForEach-Object { $_.Trim() }

# Get spec directory names for description mapping
$specDirs = @()
$specsPath = Join-Path (git rev-parse --show-toplevel 2>&1).Trim() ".kiro\specs"
if (Test-Path $specsPath) {
    $specDirs = Get-ChildItem -Path $specsPath -Directory | Select-Object -ExpandProperty Name
}

function Get-SpecDescription {
    param([string]$BranchName)

    # Extract year.sequential from branch name
    if ($BranchName -match "(feature|bugfix)-(\d{4}\.\d{6})") {
        $specId = $Matches[2]
        $matchingDir = $specDirs | Where-Object { $_ -like "$specId*" } | Select-Object -First 1
        if ($matchingDir -and $matchingDir -match "\]-(.+)$") {
            return $Matches[1]
        }
    }
    return "N/A"
}

function Get-BranchType {
    param([string]$BranchName)
    if ($BranchName -match "^(feature|bugfix)") {
        return $Matches[1]
    }
    return "unknown"
}

# Collect metrics for each branch
$results = @()

foreach ($branch in $branches) {
    Write-Host "Analyzing branch: $branch ..." -ForegroundColor Cyan

    $branchType = Get-BranchType -BranchName $branch
    $specDescription = Get-SpecDescription -BranchName $branch

    # Extract spec identifier (e.g., 2026.000001)
    $specId = ""
    if ($branch -match "(feature|bugfix)-(\d{4}\.\d{6})") {
        $specId = $Matches[2]
    }

    # Get all commits on this branch
    $allCommits = git log --oneline --format="%H|%s|%aI" $branch 2>&1 |
        ForEach-Object {
            $parts = $_ -split "\|", 3
            [PSCustomObject]@{
                Hash    = $parts[0]
                Message = $parts[1]
                Date    = if ($parts[2]) { [DateTime]::Parse($parts[2]) } else { $null }
            }
        }

    # Find task commits (pattern: year.sequential.task: description)
    $taskPattern = [regex]::Escape($specId) + "\.\d+:"
    $taskCommits = @($allCommits | Where-Object { $_.Message -match $taskPattern })

    # Find the version bump commit (chore: bump version to)
    $bumpCommit = $allCommits | Where-Object { $_.Message -match "^chore: bump version to" } | Select-Object -Last 1

    # Find the release commit
    $releaseCommit = $allCommits | Where-Object { $_.Message -match "^release:" } | Select-Object -First 1

    if ($taskCommits.Count -eq 0) {
        Write-Warning "  No task commits found for $branch. Skipping."
        continue
    }

    # Determine the commit range for diff calculation
    # Use the version bump commit's parent as the base (this is the merge-base equivalent)
    # If no bump commit exists, use the parent of the first task commit
    $firstTaskCommit = $taskCommits[-1]  # Last in array = earliest chronologically
    $lastTaskCommit = if ($releaseCommit) { $releaseCommit } else { $taskCommits[0] }

    # Collect all commits that belong to this spec (bump + tasks + release)
    $specCommitHashes = @()
    if ($bumpCommit) { $specCommitHashes += $bumpCommit.Hash }
    $specCommitHashes += $taskCommits | ForEach-Object { $_.Hash }
    if ($releaseCommit) { $specCommitHashes += $releaseCommit.Hash }

    # Calculate diff stats by summing individual commit diffs (only this spec's work)
    $linesAdded = 0
    $linesRemoved = 0
    $allModifiedFiles = @{}

    foreach ($commitHash in $specCommitHashes) {
        $commitDiff = git diff --numstat "$commitHash^" "$commitHash" 2>&1
        foreach ($line in $commitDiff) {
            if ($line -match "^(\d+)\s+(\d+)\s+(.+)$") {
                $linesAdded += [int]$Matches[1]
                $linesRemoved += [int]$Matches[2]
                $allModifiedFiles[$Matches[3]] = $true
            }
            elseif ($line -match "^-\s+-\s+(.+)$") {
                # Binary file
                $allModifiedFiles[$Matches[1]] = $true
            }
        }
    }

    $filesModified = $allModifiedFiles.Count

    $linesNet = $linesAdded - $linesRemoved

    # Calculate elapsed time
    $startTime = $firstTaskCommit.Date
    $endTime = $lastTaskCommit.Date
    $elapsed = if ($startTime -and $endTime -and $endTime -gt $startTime) {
        $endTime - $startTime
    } else {
        [TimeSpan]::Zero
    }

    $elapsedHours = [Math]::Round($elapsed.TotalHours, 2)
    $elapsedFormatted = if ($elapsed.TotalHours -ge 24) {
        "{0}d {1}h {2}m" -f $elapsed.Days, $elapsed.Hours, $elapsed.Minutes
    } elseif ($elapsed.TotalHours -ge 1) {
        "{0}h {1}m" -f [Math]::Floor($elapsed.TotalHours), $elapsed.Minutes
    } else {
        "{0}m" -f [Math]::Max(1, [Math]::Round($elapsed.TotalMinutes))
    }

    # Lines per hour (avoid division by zero)
    $linesPerHour = if ($elapsedHours -gt 0) {
        [Math]::Round($linesAdded / $elapsedHours, 0)
    } else {
        "N/A"
    }

    # Released version
    $releasedVersion = "N/A"
    if ($releaseCommit -and $releaseCommit.Message -match "release:\s*([\d\.]+)") {
        $releasedVersion = $Matches[1]
    }

    $results += [PSCustomObject]@{
        Branch          = $branch
        Type            = $branchType
        SpecId          = $specId
        Description     = $specDescription
        Version         = $releasedVersion
        TaskCommits     = $taskCommits.Count
        FilesModified   = $filesModified
        LinesAdded      = $linesAdded
        LinesRemoved    = $linesRemoved
        LinesNet        = $linesNet
        StartDate       = if ($startTime) { $startTime.ToString("yyyy-MM-dd HH:mm") } else { "N/A" }
        EndDate         = if ($endTime) { $endTime.ToString("yyyy-MM-dd HH:mm") } else { "N/A" }
        ElapsedTime     = $elapsedFormatted
        ElapsedHours    = $elapsedHours
        LinesPerHour    = $linesPerHour
    }
}

# Output results
if ($results.Count -eq 0) {
    Write-Warning "No results to display."
    exit 0
}

# Summary
$totalAdded = ($results | Measure-Object -Property LinesAdded -Sum).Sum
$totalRemoved = ($results | Measure-Object -Property LinesRemoved -Sum).Sum
$totalNet = ($results | Measure-Object -Property LinesNet -Sum).Sum
$totalFiles = ($results | Measure-Object -Property FilesModified -Sum).Sum
$totalTasks = ($results | Measure-Object -Property TaskCommits -Sum).Sum
$totalHours = ($results | Measure-Object -Property ElapsedHours -Sum).Sum

$summary = [PSCustomObject]@{
    TotalSpecs       = $results.Count
    TotalTaskCommits = $totalTasks
    TotalFilesModified = $totalFiles
    TotalLinesAdded  = $totalAdded
    TotalLinesRemoved = $totalRemoved
    TotalLinesNet    = $totalNet
    TotalElapsedHours = [Math]::Round($totalHours, 2)
    AvgLinesPerSpec  = [Math]::Round($totalAdded / $results.Count, 0)
    AvgTasksPerSpec  = [Math]::Round($totalTasks / $results.Count, 1)
}

switch ($OutputFormat) {
    "Table" {
        Write-Host ""
        Write-Host "=" * 80 -ForegroundColor Green
        Write-Host "  SPEC PRODUCTIVITY REPORT" -ForegroundColor Green
        Write-Host "=" * 80 -ForegroundColor Green
        Write-Host ""

        foreach ($r in $results) {
            Write-Host "  $($r.SpecId) | $($r.Description)" -ForegroundColor Yellow
            Write-Host "  Branch: $($r.Branch) ($($r.Type)) | Version: $($r.Version)"
            Write-Host "  Tasks: $($r.TaskCommits) | Files: $($r.FilesModified) | +$($r.LinesAdded) / -$($r.LinesRemoved) (net: $($r.LinesNet))"
            Write-Host "  Period: $($r.StartDate) -> $($r.EndDate) | Elapsed: $($r.ElapsedTime) | Lines/h: $($r.LinesPerHour)"
            Write-Host "  $("-" * 76)" -ForegroundColor DarkGray
        }

        Write-Host ""
        Write-Host "=" * 80 -ForegroundColor Green
        Write-Host "  SUMMARY" -ForegroundColor Green
        Write-Host "=" * 80 -ForegroundColor Green
        Write-Host "  Total Specs:          $($summary.TotalSpecs)"
        Write-Host "  Total Task Commits:   $($summary.TotalTaskCommits)"
        Write-Host "  Total Files Modified: $($summary.TotalFilesModified)"
        Write-Host "  Total Lines Added:    $($summary.TotalLinesAdded)"
        Write-Host "  Total Lines Removed:  $($summary.TotalLinesRemoved)"
        Write-Host "  Total Lines Net:      $($summary.TotalLinesNet)"
        Write-Host "  Total Elapsed Hours:  $($summary.TotalElapsedHours)"
        Write-Host "  Avg Lines/Spec:       $($summary.AvgLinesPerSpec)"
        Write-Host "  Avg Tasks/Spec:       $($summary.AvgTasksPerSpec)"
        Write-Host ""
    }
    "CSV" {
        $csvData = $results | Select-Object SpecId, Branch, Type, Description, Version, TaskCommits, FilesModified, LinesAdded, LinesRemoved, LinesNet, StartDate, EndDate, ElapsedTime, ElapsedHours, LinesPerHour
        if ($OutputPath) {
            $csvData | Export-Csv -Path $OutputPath -NoTypeInformation -Encoding UTF8
            Write-Host "Report saved to: $OutputPath" -ForegroundColor Green
        } else {
            $csvData | ConvertTo-Csv -NoTypeInformation
        }
    }
    "JSON" {
        $jsonOutput = @{
            generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            specs       = $results
            summary     = $summary
        } | ConvertTo-Json -Depth 3

        if ($OutputPath) {
            $jsonOutput | Out-File -FilePath $OutputPath -Encoding UTF8
            Write-Host "Report saved to: $OutputPath" -ForegroundColor Green
        } else {
            $jsonOutput
        }
    }
}
