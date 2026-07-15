<#
.SYNOPSIS
Captures native Minecraft and GPU crash evidence for an Actinium runClient session.

.DESCRIPTION
Starts gradlew.bat runClient --no-daemon, identifies the Minecraft client by walking
the Win32_Process parent/child tree, optionally attaches ProcDump, and optionally
records the WPR GPU profile in memory mode. Game logs and Windows events are read
only after the Minecraft process has exited.

.PARAMETER ProjectRoot
Actinium project root containing gradlew.bat. Defaults to the parent of this script.

.PARAMETER OutputRoot
Directory in which timestamped capture sessions are created. Defaults to
run\crash-captures under ProjectRoot.

.PARAMETER GradleUserHome
GRADLE_USER_HOME used only while starting Gradle. Defaults to D:/gradle.

.PARAMETER EnableWpr
Enables the built-in WPR GPU profile. WPR normally requires an elevated shell.

.PARAMETER ProcDumpPath
Optional path to procdump64.exe or procdump.exe. When omitted, the script searches
PATH. A missing executable is logged and does not fail the capture.

.PARAMETER FlightRecorderMode
Actinium GL flight recorder mode passed to the client. Valid values are off and crash.
The default is crash.

.PARAMETER EnableLwjglDebug
Enables the LWJGL debug context and KHR_debug callback for this client run.

.PARAMETER ExtraClientJvmArgs
Additional text appended to the client extra_jvm_args Gradle property.

.PARAMETER MinecraftDiscoveryTimeoutSeconds
Maximum time to wait for a Minecraft main class in the Gradle process tree.

.PARAMETER DryRun
Validates paths and reports planned tools without starting processes or writing files.

.PARAMETER RunLogicTests
Runs capture-helper logic tests in an isolated temporary directory without starting
Gradle, Minecraft, WPR, or ProcDump.

.EXAMPLE
.\tools\capture-native-crash.ps1 -EnableWpr -EnableLwjglDebug -ProcDumpPath 'D:\Tools\Sysinternals\procdump64.exe'

.EXAMPLE
.\tools\capture-native-crash.ps1 -ProjectRoot . -OutputRoot D:\ActiniumCaptures -DryRun
#>

#Requires -Version 5.1

[CmdletBinding()]
param(
    [Parameter()]
    [string]$ProjectRoot,

    [Parameter()]
    [string]$OutputRoot,

    [Parameter()]
    [string]$GradleUserHome = 'D:/gradle',

    [Parameter()]
    [switch]$EnableWpr,

    [Parameter()]
    [string]$ProcDumpPath,

    [Parameter()]
    [ValidateSet('off', 'crash')]
    [string]$FlightRecorderMode = 'crash',

    [Parameter()]
    [switch]$EnableLwjglDebug,

    [Parameter()]
    [string]$ExtraClientJvmArgs,

    [Parameter()]
    [ValidateRange(10, 3600)]
    [int]$MinecraftDiscoveryTimeoutSeconds = 300,

    [Parameter()]
    [switch]$DryRun,

    [Parameter()]
    [switch]$RunLogicTests
)

Set-StrictMode -Version 3.0
$ErrorActionPreference = 'Stop'

$script:Utf8NoBom = New-Object Text.UTF8Encoding($false)
$script:SessionLogPath = $null

function Write-CaptureLog {
    param(
        [Parameter(Mandatory)]
        [ValidateSet('INFO', 'WARN', 'ERROR')]
        [string]$Level,

        [Parameter(Mandatory)]
        [string]$Message
    )

    $line = '[{0:yyyy-MM-dd HH:mm:ss.fff zzz}] [{1}] {2}' -f [DateTimeOffset]::Now, $Level, $Message
    Write-Host $line
    if ($null -ne $script:SessionLogPath) {
        [IO.File]::AppendAllText($script:SessionLogPath, $line + [Environment]::NewLine, $script:Utf8NoBom)
    }
}

function Write-Utf8NoBomFile {
    param(
        [Parameter(Mandatory)]
        [string]$LiteralPath,

        [Parameter(Mandatory)]
        [AllowNull()]
        [AllowEmptyString()]
        [string]$Content
    )

    if ($null -eq $Content) {
        $Content = ''
    }
    [IO.File]::WriteAllText($LiteralPath, $Content, $script:Utf8NoBom)
}

function Invoke-NativeTool {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,

        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & $FilePath @Arguments 2>&1 | Out-String
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    return [pscustomobject]@{
        ExitCode = $exitCode
        Output = $output.Trim()
    }
}

function ConvertTo-WindowsCommandLineArgument {
    param(
        [Parameter(Mandatory)]
        [AllowEmptyString()]
        [string]$Argument
    )

    if ($Argument.Length -gt 0 -and $Argument -notmatch '[\s"]') {
        return $Argument
    }

    $result = New-Object Text.StringBuilder
    [void]$result.Append('"')
    $backslashCount = 0
    foreach ($character in $Argument.ToCharArray()) {
        if ($character -eq '\') {
            $backslashCount++
            continue
        }

        if ($character -eq '"') {
            [void]$result.Append(('\' * (($backslashCount * 2) + 1)))
            [void]$result.Append('"')
        }
        else {
            [void]$result.Append(('\' * $backslashCount))
            [void]$result.Append($character)
        }
        $backslashCount = 0
    }

    [void]$result.Append(('\' * ($backslashCount * 2)))
    [void]$result.Append('"')
    return $result.ToString()
}

function Get-CaptureStatusAfterDiagnosticFailure {
    param(
        [Parameter(Mandatory)]
        [ValidateSet('running', 'completed', 'captured-nonzero-exit', 'capture-partial', 'failed')]
        [string]$CurrentStatus
    )

    if ($CurrentStatus -eq 'failed') {
        return 'failed'
    }
    return 'capture-partial'
}

function Get-NormalizedPath {
    param(
        [Parameter(Mandatory)]
        [string]$Path,

        [Parameter()]
        [string]$BasePath = (Get-Location).Path
    )

    if ([IO.Path]::IsPathRooted($Path)) {
        return [IO.Path]::GetFullPath($Path)
    }

    return [IO.Path]::GetFullPath((Join-Path $BasePath $Path))
}

function Get-ProcessSnapshot {
    $processes = Get-CimInstance -ClassName Win32_Process -ErrorAction Stop
    return @($processes | Select-Object ProcessId, ParentProcessId, Name, ExecutablePath, CommandLine, CreationDate)
}

function Get-DescendantProcesses {
    param(
        [Parameter(Mandatory)]
        [object[]]$ProcessSnapshot,

        [Parameter(Mandatory)]
        [uint32]$RootProcessId
    )

    $childrenByParent = @{}
    foreach ($process in $ProcessSnapshot) {
        $parentId = [uint32]$process.ParentProcessId
        if (-not $childrenByParent.ContainsKey($parentId)) {
            $childrenByParent[$parentId] = New-Object Collections.ArrayList
        }
        [void]$childrenByParent[$parentId].Add($process)
    }

    $result = New-Object Collections.ArrayList
    $pending = New-Object 'Collections.Generic.Queue[uint32]'
    $visited = @{}
    $pending.Enqueue($RootProcessId)

    while ($pending.Count -gt 0) {
        $parentId = $pending.Dequeue()
        if ($visited.ContainsKey($parentId)) {
            continue
        }
        $visited[$parentId] = $true

        if (-not $childrenByParent.ContainsKey($parentId)) {
            continue
        }

        foreach ($child in $childrenByParent[$parentId]) {
            [void]$result.Add($child)
            $pending.Enqueue([uint32]$child.ProcessId)
        }
    }

    return @($result)
}

function Get-ProcessTreeTerminationOrder {
    param(
        [Parameter(Mandatory)]
        [object[]]$ProcessSnapshot,

        [Parameter(Mandatory)]
        [uint32]$RootProcessId
    )

    $descendants = @(Get-DescendantProcesses -ProcessSnapshot $ProcessSnapshot -RootProcessId $RootProcessId)
    $tree = New-Object Collections.ArrayList
    foreach ($process in $descendants) {
        [void]$tree.Add($process)
    }
    $root = @($ProcessSnapshot | Where-Object { [uint32]$_.ProcessId -eq $RootProcessId })
    if ($root.Count -gt 1) {
        throw "Process snapshot contains duplicate PID $RootProcessId."
    }
    if ($root.Count -eq 1) {
        [void]$tree.Add($root[0])
    }

    $byId = @{}
    foreach ($process in $tree) {
        $byId[[uint32]$process.ProcessId] = $process
    }

    $depthById = @{}
    foreach ($process in $tree) {
        $depth = 0
        $current = $process
        $visited = @{}
        while ([uint32]$current.ProcessId -ne $RootProcessId) {
            $currentId = [uint32]$current.ProcessId
            if ($visited.ContainsKey($currentId)) {
                throw "Cycle found in process tree at PID $currentId."
            }
            $visited[$currentId] = $true
            $parentId = [uint32]$current.ParentProcessId
            $depth++
            if ($parentId -eq $RootProcessId) {
                break
            }
            if (-not $byId.ContainsKey($parentId)) {
                throw "PID $currentId is not connected to expected root PID $RootProcessId."
            }
            $current = $byId[$parentId]
        }
        $depthById[[uint32]$process.ProcessId] = $depth
    }

    return @($tree | Sort-Object `
        @{ Expression = { $depthById[[uint32]$_.ProcessId] }; Descending = $true }, `
        @{ Expression = { [uint32]$_.ProcessId }; Descending = $true })
}

function Stop-GradleProcessTree {
    param(
        [Parameter(Mandatory)]
        [Diagnostics.Process]$GradleProcess
    )

    $errors = New-Object Collections.ArrayList
    $stopped = New-Object Collections.ArrayList
    try {
        $GradleProcess.Refresh()
        if ($GradleProcess.HasExited) {
            Write-CaptureLog -Level INFO -Message "Gradle root PID $($GradleProcess.Id) already exited; process-tree cleanup will not inspect or stop reused PIDs."
            return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
        }
        $expectedRootStartUtc = $GradleProcess.StartTime.ToUniversalTime()
        $expectedRootExecutable = [IO.Path]::GetFullPath($GradleProcess.MainModule.FileName)
    }
    catch {
        $message = "Could not verify the capture-owned Gradle root process: $($_.Exception.Message)"
        Write-CaptureLog -Level ERROR -Message $message
        [void]$errors.Add([pscustomobject]@{ message = $message })
        return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
    }

    $snapshot = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            $snapshot = @(Get-ProcessSnapshot)
            break
        }
        catch {
            $message = "Process-tree cleanup snapshot attempt $attempt failed: $($_.Exception.Message)"
            Write-CaptureLog -Level WARN -Message $message
            if ($attempt -eq 3) {
                [void]$errors.Add([pscustomobject]@{ message = $message })
            }
            else {
                Start-Sleep -Milliseconds 250
            }
        }
    }

    if ($null -eq $snapshot) {
        return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
    }

    $rootCandidates = @($snapshot | Where-Object { [uint32]$_.ProcessId -eq [uint32]$GradleProcess.Id })
    if ($rootCandidates.Count -ne 1) {
        $message = "Expected one live CIM root for capture-owned Gradle PID $($GradleProcess.Id), found $($rootCandidates.Count); cleanup aborted."
        Write-CaptureLog -Level ERROR -Message $message
        [void]$errors.Add([pscustomobject]@{ message = $message })
        return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
    }
    $rootStartDifference = [Math]::Abs(($rootCandidates[0].CreationDate.ToUniversalTime() - $expectedRootStartUtc).TotalSeconds)
    if ($rootStartDifference -gt 1.0) {
        $message = "CIM root PID $($GradleProcess.Id) creation time differs from the capture-owned Gradle process by $rootStartDifference seconds; cleanup aborted to avoid PID reuse."
        Write-CaptureLog -Level ERROR -Message $message
        [void]$errors.Add([pscustomobject]@{ message = $message })
        return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
    }
    if ([string]::IsNullOrWhiteSpace([string]$rootCandidates[0].ExecutablePath) -or
        -not [string]::Equals(
            [IO.Path]::GetFullPath([string]$rootCandidates[0].ExecutablePath),
            $expectedRootExecutable,
            [StringComparison]::OrdinalIgnoreCase
        )) {
        $message = "CIM root PID $($GradleProcess.Id) executable does not match the capture-owned Gradle process; cleanup aborted."
        Write-CaptureLog -Level ERROR -Message $message
        [void]$errors.Add([pscustomobject]@{ message = $message })
        return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
    }

    try {
        $order = @(Get-ProcessTreeTerminationOrder -ProcessSnapshot $snapshot -RootProcessId ([uint32]$GradleProcess.Id))
    }
    catch {
        $message = "Could not validate the Gradle process tree rooted at PID $($GradleProcess.Id): $($_.Exception.Message)"
        Write-CaptureLog -Level ERROR -Message $message
        [void]$errors.Add([pscustomobject]@{ message = $message })
        return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
    }

    $GradleProcess.Refresh()
    if ($GradleProcess.HasExited) {
        Write-CaptureLog -Level INFO -Message "Gradle root PID $($GradleProcess.Id) exited during validation; cleanup aborted before stopping any descendants."
        return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
    }

    foreach ($candidate in $order) {
        $candidateId = [uint32]$candidate.ProcessId
        try {
            if ($candidateId -eq [uint32]$GradleProcess.Id) {
                $GradleProcess.Refresh()
                if ($GradleProcess.HasExited) {
                    Write-CaptureLog -Level INFO -Message "Gradle root PID $candidateId already exited during cleanup."
                    continue
                }
            }

            $current = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $candidateId" -ErrorAction Stop
            if ($null -eq $current) {
                Write-CaptureLog -Level INFO -Message "Process PID $candidateId already exited during cleanup."
                continue
            }
            if ($current.CreationDate.ToUniversalTime().Ticks -ne $candidate.CreationDate.ToUniversalTime().Ticks) {
                throw "PID $candidateId was reused; creation time no longer matches the validated process tree."
            }

            Write-CaptureLog -Level WARN -Message "Stopping capture-owned process PID $candidateId ($($candidate.Name)); parent PID $($candidate.ParentProcessId)."
            Stop-Process -Id $candidateId -Force -ErrorAction Stop
            [void]$stopped.Add($candidateId)
        }
        catch {
            $message = "Failed to stop capture-owned PID ${candidateId}: $($_.Exception.Message)"
            Write-CaptureLog -Level ERROR -Message $message
            [void]$errors.Add([pscustomobject]@{ message = $message; processId = $candidateId })
        }
    }

    return [pscustomobject]@{ stopped = @($stopped); errors = @($errors) }
}

function Test-IsMinecraftClientProcess {
    param(
        [Parameter(Mandatory)]
        [object]$Process
    )

    if ($Process.Name -notin @('java.exe', 'javaw.exe')) {
        return $false
    }

    $commandLine = [string]$Process.CommandLine
    if ([string]::IsNullOrWhiteSpace($commandLine)) {
        return $false
    }

    if ($commandLine -match '(?i)GradleDaemon|GradleWrapperMain|GradleWorkerMain|org\.gradle\.') {
        return $false
    }

    return $commandLine -match '(?i)(com\.cleanroommc\.boot\.MainClient|net\.minecraft\.client\.main\.Main|net\.minecraft\.launchwrapper\.Launch)'
}

function Find-MinecraftClientProcess {
    param(
        [Parameter(Mandatory)]
        [uint32]$GradleProcessId
    )

    $snapshot = Get-ProcessSnapshot
    $descendants = Get-DescendantProcesses -ProcessSnapshot $snapshot -RootProcessId $GradleProcessId
    $matches = @($descendants | Where-Object { Test-IsMinecraftClientProcess -Process $_ })

    if ($matches.Count -gt 1) {
        $details = ($matches | ForEach-Object { '{0}: {1}' -f $_.ProcessId, $_.CommandLine }) -join [Environment]::NewLine
        throw "Multiple Minecraft client candidates were found; refusing to choose one:$([Environment]::NewLine)$details"
    }

    if ($matches.Count -eq 1) {
        return $matches[0]
    }

    return $null
}

function Wait-MinecraftClientDiscovery {
    param(
        [Parameter(Mandatory)]
        [Diagnostics.Process]$GradleProcess,

        [Parameter(Mandatory)]
        [int]$TimeoutSeconds
    )

    $deadline = [DateTimeOffset]::Now.AddSeconds($TimeoutSeconds)
    while ([DateTimeOffset]::Now -lt $deadline) {
        $minecraft = Find-MinecraftClientProcess -GradleProcessId ([uint32]$GradleProcess.Id)
        if ($null -ne $minecraft) {
            return $minecraft
        }

        $GradleProcess.Refresh()
        if ($GradleProcess.HasExited) {
            throw "Gradle exited with code $($GradleProcess.ExitCode) before a Minecraft client process was discovered."
        }

        Start-Sleep -Milliseconds 500
    }

    throw "No Minecraft client process appeared in the Gradle process tree within $TimeoutSeconds seconds."
}

function Wait-ProcessExitById {
    param(
        [Parameter(Mandatory)]
        [uint32]$ProcessId
    )

    while ($null -ne (Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction Stop)) {
        Start-Sleep -Milliseconds 500
    }
}

function Wait-GradleProcessExitCode {
    param(
        [Parameter(Mandatory)]
        $GradleProcess
    )

    if ($GradleProcess -isnot [Diagnostics.Process]) {
        throw 'GradleProcess must be a System.Diagnostics.Process instance.'
    }

    $GradleProcess.WaitForExit()
    $GradleProcess.Refresh()
    if (-not $GradleProcess.HasExited) {
        throw "Gradle process PID $($GradleProcess.Id) did not exit after WaitForExit returned."
    }
    return [int]($GradleProcess.ExitCode)
}

function Resolve-ProcDumpExecutable {
    param(
        [Parameter()]
        [string]$RequestedPath
    )

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        $normalized = Get-NormalizedPath -Path $RequestedPath
        if (Test-Path -LiteralPath $normalized -PathType Leaf) {
            return $normalized
        }
        return $null
    }

    foreach ($name in @('procdump64.exe', 'procdump.exe')) {
        $command = Get-Command -Name $name -CommandType Application -ErrorAction SilentlyContinue
        if ($null -ne $command) {
            return $command.Source
        }
    }

    return $null
}

function Get-DumpFileSnapshot {
    $roots = @(
        (Join-Path $env:LOCALAPPDATA 'CrashDumps'),
        (Join-Path $env:SystemRoot 'LiveKernelReports\WATCHDOG')
    )
    $files = New-Object Collections.ArrayList

    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root -PathType Container)) {
            continue
        }

        try {
            foreach ($file in @(Get-ChildItem -LiteralPath $root -Filter '*.dmp' -File -ErrorAction Stop)) {
                [void]$files.Add([pscustomobject]@{
                    FullName = $file.FullName
                    Length = $file.Length
                    CreationTimeUtc = $file.CreationTimeUtc
                    LastWriteTimeUtc = $file.LastWriteTimeUtc
                })
            }
        }
        catch {
            Write-CaptureLog -Level WARN -Message "Could not enumerate dump directory '$root': $($_.Exception.Message)"
        }
    }

    return @($files)
}

function Get-NewDumpFiles {
    param(
        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]]$Before,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]]$After
    )

    $known = @{}
    foreach ($file in $Before) {
        $known[[string]$file.FullName] = '{0}:{1:o}' -f $file.Length, $file.LastWriteTimeUtc
    }

    return @($After | Where-Object {
        $path = [string]$_.FullName
        $signature = '{0}:{1:o}' -f $_.Length, $_.LastWriteTimeUtc
        -not $known.ContainsKey($path) -or $known[$path] -ne $signature
    })
}

function Export-RelatedWindowsEvents {
    param(
        [Parameter(Mandatory)]
        [DateTime]$StartTime,

        [Parameter(Mandatory)]
        [DateTime]$EndTime,

        [Parameter(Mandatory)]
        [string]$SessionDirectory
    )

    $events = New-Object Collections.ArrayList
    foreach ($logName in @('Application', 'System')) {
        try {
            $logEvents = Get-WinEvent -FilterHashtable @{
                LogName = $logName
                StartTime = $StartTime
                EndTime = $EndTime
            } -ErrorAction Stop

            foreach ($event in $logEvents) {
                $provider = [string]$event.ProviderName
                $message = [string]$event.Message
                $isRelevantProvider = $provider -match '(?i)NVIDIA OpenGL Driver|nvlddmkm|Application Error|Windows Error Reporting|Display'
                $isRelevantMessage = $message -match '(?i)java\.exe|nvoglv64\.dll|nvlddmkm|LiveKernelEvent|0xC0000409|c0000409'
                if ($isRelevantProvider -or $isRelevantMessage) {
                    [void]$events.Add([pscustomobject]@{
                        TimeCreated = $event.TimeCreated
                        Id = $event.Id
                        LevelDisplayName = $event.LevelDisplayName
                        LogName = $event.LogName
                        ProviderName = $provider
                        RecordId = $event.RecordId
                        MachineName = $event.MachineName
                        Message = $message
                    })
                }
            }
        }
        catch {
            Write-CaptureLog -Level WARN -Message "Could not read the Windows '$logName' event log: $($_.Exception.Message)"
        }
    }

    $ordered = @($events | Sort-Object TimeCreated)
    Write-Utf8NoBomFile -LiteralPath (Join-Path $SessionDirectory 'windows-events.json') -Content (ConvertTo-Json -InputObject $ordered -Depth 4)

    $text = ($ordered | Format-List TimeCreated, Id, LevelDisplayName, LogName, ProviderName, RecordId, MachineName, Message | Out-String -Width 4096)
    Write-Utf8NoBomFile -LiteralPath (Join-Path $SessionDirectory 'windows-events.txt') -Content $text
    return $ordered.Count
}

function Copy-GameLogs {
    param(
        [Parameter(Mandatory)]
        [string]$NormalizedProjectRoot,

        [Parameter(Mandatory)]
        [string]$SessionDirectory
    )

    $copied = New-Object Collections.ArrayList
    $logDirectory = Join-Path $NormalizedProjectRoot 'run\client\logs'
    foreach ($name in @('latest.log', 'debug.log')) {
        $source = Join-Path $logDirectory $name
        if (Test-Path -LiteralPath $source -PathType Leaf) {
            $destination = Join-Path $SessionDirectory $name
            Copy-Item -LiteralPath $source -Destination $destination -Force -ErrorAction Stop
            [void]$copied.Add($destination)
            Write-CaptureLog -Level INFO -Message "Copied game log: $source"
        }
        else {
            Write-CaptureLog -Level WARN -Message "Game log does not exist: $source"
        }
    }

    return @($copied)
}

function Get-FlightRecorderFileSnapshot {
    param(
        [Parameter(Mandatory)]
        [string]$NormalizedProjectRoot
    )

    $diagnosticsDirectory = Join-Path $NormalizedProjectRoot 'run\client\diagnostics'
    if (-not (Test-Path -LiteralPath $diagnosticsDirectory -PathType Container)) {
        return @()
    }

    return @(Get-ChildItem -LiteralPath $diagnosticsDirectory -Filter 'gl-flight-*.bin' -File -ErrorAction Stop | ForEach-Object {
        [pscustomobject]@{
            FullName = $_.FullName
            Length = $_.Length
            CreationTimeUtc = $_.CreationTimeUtc
            LastWriteTimeUtc = $_.LastWriteTimeUtc
        }
    })
}

function Copy-NewFlightRecorderFiles {
    param(
        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]]$Before,

        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]]$After,

        [Parameter(Mandatory)]
        [string]$SessionDirectory
    )

    $newFiles = @(Get-NewDumpFiles -Before $Before -After $After)
    if ($newFiles.Count -eq 0) {
        return @()
    }

    $destinationDirectory = Join-Path $SessionDirectory 'diagnostics'
    [void](New-Item -ItemType Directory -Path $destinationDirectory -Force)
    $copied = New-Object Collections.ArrayList
    foreach ($file in $newFiles) {
        $destination = Join-Path $destinationDirectory (Split-Path -Leaf $file.FullName)
        Copy-Item -LiteralPath $file.FullName -Destination $destination -Force -ErrorAction Stop
        [void]$copied.Add([pscustomobject]@{
            sourcePath = $file.FullName
            sessionPath = $destination
            length = $file.Length
            creationTimeUtc = $file.CreationTimeUtc
            lastWriteTimeUtc = $file.LastWriteTimeUtc
        })
        Write-CaptureLog -Level INFO -Message "Copied GL flight recorder: $($file.FullName)"
    }

    return @($copied)
}

function Invoke-FlightRecorderDecoder {
    param(
        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [object[]]$FlightRecorderFiles,

        [Parameter(Mandatory)]
        [AllowNull()]
        [AllowEmptyString()]
        [string]$JavaExecutable,

        [Parameter(Mandatory)]
        [string]$NormalizedProjectRoot
    )

    $records = New-Object Collections.ArrayList
    $errors = New-Object Collections.ArrayList
    if ($FlightRecorderFiles.Count -eq 0) {
        return [pscustomobject]@{ records = @($records); errors = @($errors) }
    }

    $classesDirectory = Join-Path $NormalizedProjectRoot 'build\classes\java\main'
    if (-not (Test-Path -LiteralPath $classesDirectory -PathType Container)) {
        $message = "GL flight recorder decoder classes do not exist: $classesDirectory. Compile the main classes before capture."
        Write-CaptureLog -Level ERROR -Message $message
        [void]$errors.Add([pscustomobject]@{ message = $message })
        foreach ($file in $FlightRecorderFiles) {
            [void]$records.Add([pscustomobject]@{
                binaryPath = $file.sessionPath
                jsonPath = [IO.Path]::ChangeExtension([string]$file.sessionPath, '.json')
                decoderErrorPath = $null
                decoded = $false
            })
        }
        return [pscustomobject]@{ records = @($records); errors = @($errors) }
    }

    if (-not (Test-Path -LiteralPath $JavaExecutable -PathType Leaf)) {
        $message = "Minecraft Java executable does not exist for flight recorder decoding: $JavaExecutable"
        Write-CaptureLog -Level ERROR -Message $message
        [void]$errors.Add([pscustomobject]@{ message = $message })
        foreach ($file in $FlightRecorderFiles) {
            [void]$records.Add([pscustomobject]@{
                binaryPath = $file.sessionPath
                jsonPath = [IO.Path]::ChangeExtension([string]$file.sessionPath, '.json')
                decoderErrorPath = $null
                decoded = $false
            })
        }
        return [pscustomobject]@{ records = @($records); errors = @($errors) }
    }

    $decoderClass = 'com.dhj.actinium.debug.flight.GlFlightRecordingDecoder'
    foreach ($file in $FlightRecorderFiles) {
        $binaryPath = [string]$file.sessionPath
        $jsonPath = [IO.Path]::ChangeExtension($binaryPath, '.json')
        $decoderErrorPath = [IO.Path]::ChangeExtension($binaryPath, '.decoder-stderr.log')
        $decoderArguments = @('-cp', $classesDirectory, $decoderClass, $binaryPath)
        $decoderStartArguments = (($decoderArguments | ForEach-Object {
            ConvertTo-WindowsCommandLineArgument -Argument $_
        }) -join ' ')

        try {
            Write-CaptureLog -Level INFO -Message "Decoding GL flight recorder: $binaryPath"
            $decoderProcess = Start-Process -FilePath $JavaExecutable `
                -ArgumentList $decoderStartArguments `
                -WorkingDirectory $NormalizedProjectRoot `
                -RedirectStandardOutput $jsonPath `
                -RedirectStandardError $decoderErrorPath `
                -NoNewWindow `
                -Wait `
                -PassThru
            if ($decoderProcess.ExitCode -ne 0) {
                throw "Decoder exited with code $($decoderProcess.ExitCode); see $decoderErrorPath"
            }

            [void]$records.Add([pscustomobject]@{
                binaryPath = $binaryPath
                jsonPath = $jsonPath
                decoderErrorPath = $decoderErrorPath
                decoded = $true
            })
        }
        catch {
            $message = "Failed to decode GL flight recorder '$binaryPath': $($_.Exception.Message)"
            Write-CaptureLog -Level ERROR -Message $message
            [void]$errors.Add([pscustomobject]@{ message = $message; binaryPath = $binaryPath })
            [void]$records.Add([pscustomobject]@{
                binaryPath = $binaryPath
                jsonPath = $jsonPath
                decoderErrorPath = $decoderErrorPath
                decoded = $false
            })
        }
    }

    return [pscustomobject]@{ records = @($records); errors = @($errors) }
}

function Assert-CaptureLogic {
    param(
        [Parameter(Mandatory)]
        [bool]$Condition,

        [Parameter(Mandatory)]
        [string]$Message
    )

    if (-not $Condition) {
        throw "Capture logic test failed: $Message"
    }
}

function Invoke-CaptureLogicTests {
    $testRoot = Join-Path ([IO.Path]::GetTempPath()) ('actinium-capture-tests-' + [Guid]::NewGuid().ToString('N'))
    $sessionDirectory = Join-Path $testRoot 'session'
    try {
        [void](New-Item -ItemType Directory -Path $sessionDirectory -Force)

        $emptyDelta = @(Get-NewDumpFiles -Before @() -After @())
        Assert-CaptureLogic -Condition ($emptyDelta.Count -eq 0) -Message 'empty snapshots must produce an empty delta'

        $emptySnapshot = @(Get-FlightRecorderFileSnapshot -NormalizedProjectRoot $testRoot)
        Assert-CaptureLogic -Condition ($emptySnapshot.Count -eq 0) -Message 'a missing diagnostics directory must produce an empty snapshot'

        $sourceDirectory = Join-Path $testRoot 'source'
        [void](New-Item -ItemType Directory -Path $sourceDirectory -Force)
        $sourcePath = Join-Path $sourceDirectory 'gl-flight-test.bin'
        [IO.File]::WriteAllBytes($sourcePath, [byte[]](1, 2, 3, 4))
        $sourceFile = Get-Item -LiteralPath $sourcePath
        $after = @([pscustomobject]@{
            FullName = $sourceFile.FullName
            Length = $sourceFile.Length
            CreationTimeUtc = $sourceFile.CreationTimeUtc
            LastWriteTimeUtc = $sourceFile.LastWriteTimeUtc
        })
        $copied = @(Copy-NewFlightRecorderFiles -Before @() -After $after -SessionDirectory $sessionDirectory)
        Assert-CaptureLogic -Condition ($copied.Count -eq 1) -Message 'one new recording must be detected and copied'
        Assert-CaptureLogic -Condition (Test-Path -LiteralPath $copied[0].sessionPath -PathType Leaf) -Message 'the copied recording must exist'

        $decoded = Invoke-FlightRecorderDecoder -FlightRecorderFiles @() -JavaExecutable $null -NormalizedProjectRoot $testRoot
        Assert-CaptureLogic -Condition ($decoded.records.Count -eq 0) -Message 'an empty decoder input must produce no records'
        Assert-CaptureLogic -Condition ($decoded.errors.Count -eq 0) -Message 'an empty decoder input must produce no errors'

        $exitScript = Join-Path $testRoot 'exit-one.cmd'
        $exitStdout = Join-Path $testRoot 'exit-one-stdout.log'
        $exitStderr = Join-Path $testRoot 'exit-one-stderr.log'
        [IO.File]::WriteAllText(
            $exitScript,
            "@ping.exe -n 2 127.0.0.1 >nul`r`n@exit /b 1",
            [Text.Encoding]::ASCII
        )
        $exitCommandArguments = '/d /s /c ""' + $exitScript + '""'
        $exitProcess = Start-Process -FilePath $env:ComSpec `
            -ArgumentList $exitCommandArguments `
            -RedirectStandardOutput $exitStdout `
            -RedirectStandardError $exitStderr `
            -WindowStyle Hidden `
            -PassThru
        [void]$exitProcess.Handle
        $exitCode = Wait-GradleProcessExitCode -GradleProcess $exitProcess
        Assert-CaptureLogic `
            -Condition ($exitCode -eq 1) `
            -Message "a redirected batch process exit code must be recorded as 1 (actual: '$exitCode')"

        Write-Host 'Capture logic tests passed.'
    }
    finally {
        if (Test-Path -LiteralPath $testRoot -PathType Container) {
            Remove-Item -LiteralPath $testRoot -Recurse -Force
        }
    }
}

if ($RunLogicTests) {
    Invoke-CaptureLogicTests
    return
}

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Split-Path -Parent $PSScriptRoot
}
$normalizedProjectRoot = Get-NormalizedPath -Path $ProjectRoot
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $normalizedProjectRoot 'run\crash-captures'
}
$normalizedOutputRoot = Get-NormalizedPath -Path $OutputRoot -BasePath $normalizedProjectRoot
$normalizedGradleUserHome = Get-NormalizedPath -Path $GradleUserHome -BasePath $normalizedProjectRoot
$gradleWrapper = Join-Path $normalizedProjectRoot 'gradlew.bat'

$clientJvmArgumentParts = New-Object Collections.ArrayList
[void]$clientJvmArgumentParts.Add("-Dactinium.glFlightRecorder=$FlightRecorderMode")
if ($EnableLwjglDebug) {
    [void]$clientJvmArgumentParts.Add('-Dactinium.lwjglDebug=true')
}
$generatedClientJvmArgs = $clientJvmArgumentParts -join ' '
$finalClientJvmArgs = if ([string]::IsNullOrWhiteSpace($ExtraClientJvmArgs)) {
    $generatedClientJvmArgs
}
else {
    $generatedClientJvmArgs + ' ' + $ExtraClientJvmArgs.Trim()
}
$gradlePropertyArgument = "-Pextra_jvm_args=$finalClientJvmArgs"
$gradleArguments = @('runClient', $gradlePropertyArgument, '--no-daemon')
$gradleStartProcessArgumentList = (($gradleArguments | ForEach-Object {
    ConvertTo-WindowsCommandLineArgument -Argument $_
}) -join ' ')
$displayCommand = (ConvertTo-WindowsCommandLineArgument -Argument $gradleWrapper) + ' ' + $gradleStartProcessArgumentList
$gradleCmdArgumentList = '/d /s /c "' + $displayCommand + '"'

if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle Wrapper was not found: $gradleWrapper"
}

$resolvedProcDump = Resolve-ProcDumpExecutable -RequestedPath $ProcDumpPath
$wprCommand = Get-Command -Name 'wpr.exe' -CommandType Application -ErrorAction SilentlyContinue

if ($DryRun) {
    Write-Host 'Dry run: Gradle, WPR, and ProcDump will not start, and no session directory will be created.'
    Write-Host "Project root: $normalizedProjectRoot"
    Write-Host "Output root:  $normalizedOutputRoot"
    Write-Host "Gradle home:  $normalizedGradleUserHome"
    Write-Host "Gradle:       $displayCommand"
    Write-Host "Client JVM:   $finalClientJvmArgs"
    Write-Host "GL recorder:  $FlightRecorderMode"
    Write-Host "LWJGL debug:  $([bool]$EnableLwjglDebug)"
    Write-Host "WPR:          $(if ($EnableWpr) { if ($null -ne $wprCommand) { "enabled ($($wprCommand.Source), memory-mode GPU profile)" } else { 'enabled, but wpr.exe was not found' } } else { 'disabled' })"
    Write-Host "ProcDump:     $(if ($null -ne $resolvedProcDump) { $resolvedProcDump } elseif ([string]::IsNullOrWhiteSpace($ProcDumpPath)) { 'not found; capture will continue without it' } else { "requested path not found: $ProcDumpPath; capture will continue without it" })"
    return
}

$sessionStart = [DateTimeOffset]::Now
$sessionId = $sessionStart.ToString('yyyyMMdd-HHmmss-fff')
$sessionDirectory = Join-Path $normalizedOutputRoot $sessionId
[void](New-Item -ItemType Directory -Path $sessionDirectory -Force)
$script:SessionLogPath = Join-Path $sessionDirectory 'capture.log'
Write-Utf8NoBomFile -LiteralPath $script:SessionLogPath -Content ''

$manifest = [ordered]@{
    schemaVersion = 2
    sessionId = $sessionId
    status = 'running'
    sessionDirectory = $sessionDirectory
    startedAt = $sessionStart
    endedAt = $null
    projectRoot = $normalizedProjectRoot
    command = $displayCommand
    host = [ordered]@{
        computerName = $env:COMPUTERNAME
        userName = $env:USERNAME
        osVersion = [Environment]::OSVersion.VersionString
        powerShellVersion = $PSVersionTable.PSVersion.ToString()
    }
    gradle = [ordered]@{
        processId = $null
        exitCode = $null
        userHome = $normalizedGradleUserHome
        arguments = $gradleArguments
        startProcessArgumentList = $gradleCmdArgumentList
        stdout = (Join-Path $sessionDirectory 'gradle-stdout.log')
        stderr = (Join-Path $sessionDirectory 'gradle-stderr.log')
    }
    clientJvm = [ordered]@{
        flightRecorderMode = $FlightRecorderMode
        lwjglDebug = [bool]$EnableLwjglDebug
        requestedExtraArguments = $ExtraClientJvmArgs
        finalArguments = $finalClientJvmArgs
        gradlePropertyArgument = $gradlePropertyArgument
    }
    minecraft = [ordered]@{
        processId = $null
        executablePath = $null
        commandLine = $null
        discoveredAt = $null
        exitedAt = $null
    }
    wpr = [ordered]@{
        requested = [bool]$EnableWpr
        started = $false
        tracePath = (Join-Path $sessionDirectory 'gpu-trace.etl')
        stopResult = $null
    }
    procdump = [ordered]@{
        requestedPath = $ProcDumpPath
        executablePath = $resolvedProcDump
        attached = $false
        processId = $null
        outputDirectory = (Join-Path $sessionDirectory 'procdump')
    }
    processTreeCleanup = [ordered]@{
        attempted = $false
        stoppedProcessIds = @()
    }
    gameLogs = @()
    flightRecorderFiles = @()
    windowsEventCount = 0
    newDumps = @()
    errors = @()
}

$wprStarted = $false
$gradleProcess = $null
$minecraftProcess = $null
$procDumpProcess = $null
$baselineDumps = @()
$baselineFlightRecorderFiles = @()
$caughtError = $null
$capturePartial = $false

Write-CaptureLog -Level INFO -Message "Session directory: $sessionDirectory"

try {
    $baselineDumps = @(Get-DumpFileSnapshot)
    $baselineFlightRecorderFiles = @(Get-FlightRecorderFileSnapshot -NormalizedProjectRoot $normalizedProjectRoot)

    if ($EnableWpr) {
        if ($null -eq $wprCommand) {
            throw 'WPR was requested, but wpr.exe was not found. Ensure Windows Performance Recorder is available.'
        }

        Write-CaptureLog -Level INFO -Message 'Starting the WPR GPU profile in memory mode.'
        $wprStartResult = Invoke-NativeTool -FilePath $wprCommand.Source -Arguments @('-start', 'GPU')
        if ($wprStartResult.ExitCode -ne 0) {
            throw "WPR start failed (exit $($wprStartResult.ExitCode)): $($wprStartResult.Output)"
        }
        $wprStarted = $true
        $manifest.wpr.started = $true
        Write-CaptureLog -Level INFO -Message 'WPR GPU tracing started.'
    }

    if ($null -eq $resolvedProcDump) {
        if ([string]::IsNullOrWhiteSpace($ProcDumpPath)) {
            Write-CaptureLog -Level WARN -Message 'ProcDump was not found. Capture will continue without an additional user-mode full dump.'
        }
        else {
            Write-CaptureLog -Level WARN -Message "The requested ProcDump path does not exist: $ProcDumpPath. Capture will continue without ProcDump."
        }
    }

    Write-CaptureLog -Level INFO -Message "Starting Gradle with client JVM arguments: $finalClientJvmArgs"
    $previousGradleUserHome = [Environment]::GetEnvironmentVariable('GRADLE_USER_HOME', 'Process')
    try {
        [Environment]::SetEnvironmentVariable('GRADLE_USER_HOME', $normalizedGradleUserHome, 'Process')
        $gradleProcess = Start-Process -FilePath $env:ComSpec `
            -ArgumentList $gradleCmdArgumentList `
            -WorkingDirectory $normalizedProjectRoot `
            -RedirectStandardOutput $manifest.gradle.stdout `
            -RedirectStandardError $manifest.gradle.stderr `
            -WindowStyle Hidden `
            -PassThru
        [void]$gradleProcess.Handle
    }
    finally {
        [Environment]::SetEnvironmentVariable('GRADLE_USER_HOME', $previousGradleUserHome, 'Process')
    }
    $manifest.gradle.processId = $gradleProcess.Id

    $minecraftProcess = Wait-MinecraftClientDiscovery -GradleProcess $gradleProcess -TimeoutSeconds $MinecraftDiscoveryTimeoutSeconds
    $manifest.minecraft.processId = [uint32]$minecraftProcess.ProcessId
    $manifest.minecraft.executablePath = [string]$minecraftProcess.ExecutablePath
    $manifest.minecraft.commandLine = [string]$minecraftProcess.CommandLine
    $manifest.minecraft.discoveredAt = [DateTimeOffset]::Now
    Write-CaptureLog -Level INFO -Message "Identified Minecraft client PID $($minecraftProcess.ProcessId): $($minecraftProcess.ExecutablePath)"

    if ($null -ne $resolvedProcDump) {
        [void](New-Item -ItemType Directory -Path $manifest.procdump.outputDirectory -Force)
        Write-CaptureLog -Level INFO -Message "Attaching ProcDump: $resolvedProcDump"
        $quotedProcDumpOutputDirectory = '"{0}"' -f $manifest.procdump.outputDirectory
        $procDumpProcess = Start-Process -FilePath $resolvedProcDump `
            -ArgumentList @('-accepteula', '-ma', '-e', '-t', [string]$minecraftProcess.ProcessId, $quotedProcDumpOutputDirectory) `
            -WorkingDirectory $sessionDirectory `
            -RedirectStandardOutput (Join-Path $sessionDirectory 'procdump-stdout.log') `
            -RedirectStandardError (Join-Path $sessionDirectory 'procdump-stderr.log') `
            -WindowStyle Hidden `
            -PassThru
        $manifest.procdump.attached = $true
        $manifest.procdump.processId = $procDumpProcess.Id
    }

    Write-CaptureLog -Level INFO -Message 'Waiting for the Minecraft client process to exit completely. Game logs will not be read before then.'
    Wait-ProcessExitById -ProcessId ([uint32]$minecraftProcess.ProcessId)
    $manifest.minecraft.exitedAt = [DateTimeOffset]::Now
    Write-CaptureLog -Level INFO -Message 'The Minecraft client process has exited completely. Starting evidence collection.'

    if ($null -ne $procDumpProcess) {
        if (-not $procDumpProcess.WaitForExit(30000)) {
            Write-CaptureLog -Level WARN -Message 'ProcDump did not exit within 30 seconds after the client. Stopping the ProcDump process started by this session.'
            Stop-Process -Id $procDumpProcess.Id -Force -ErrorAction Stop
        }
    }

    $manifest.gradle.exitCode = Wait-GradleProcessExitCode -GradleProcess $gradleProcess
    Write-CaptureLog -Level INFO -Message "Gradle process exit code: $($gradleProcess.ExitCode)"

    Start-Sleep -Seconds 5
    $manifest.gameLogs = @(Copy-GameLogs -NormalizedProjectRoot $normalizedProjectRoot -SessionDirectory $sessionDirectory)
    $afterFlightRecorderFiles = @(Get-FlightRecorderFileSnapshot -NormalizedProjectRoot $normalizedProjectRoot)
    $copiedFlightRecorderFiles = @(Copy-NewFlightRecorderFiles -Before $baselineFlightRecorderFiles -After $afterFlightRecorderFiles -SessionDirectory $sessionDirectory)
    try {
        $decoderResult = Invoke-FlightRecorderDecoder `
            -FlightRecorderFiles $copiedFlightRecorderFiles `
            -JavaExecutable $manifest.minecraft.executablePath `
            -NormalizedProjectRoot $normalizedProjectRoot
        $manifest.flightRecorderFiles = @($decoderResult.records)
        if ($decoderResult.errors.Count -gt 0) {
            $manifest.errors = @($manifest.errors) + @($decoderResult.errors)
            $capturePartial = $true
        }
    }
    catch {
        $decoderMessage = "Unexpected GL flight recorder decoder failure: $($_.Exception.Message)"
        Write-CaptureLog -Level ERROR -Message $decoderMessage
        $manifest.errors = @($manifest.errors) + @([pscustomobject]@{ message = $decoderMessage })
        $manifest.flightRecorderFiles = @($copiedFlightRecorderFiles | ForEach-Object {
            [pscustomobject]@{
                binaryPath = $_.sessionPath
                jsonPath = [IO.Path]::ChangeExtension([string]$_.sessionPath, '.json')
                decoderErrorPath = $null
                decoded = $false
            }
        })
        $capturePartial = $true
    }
    if ($FlightRecorderMode -ne 'off' -and $copiedFlightRecorderFiles.Count -eq 0) {
        Write-CaptureLog -Level WARN -Message 'No new GL flight recorder file was found after the client exited.'
    }
    $eventEnd = [DateTimeOffset]::Now
    $manifest.windowsEventCount = Export-RelatedWindowsEvents -StartTime $sessionStart.LocalDateTime -EndTime $eventEnd.LocalDateTime -SessionDirectory $sessionDirectory

    $afterDumps = @(Get-DumpFileSnapshot)
    $manifest.newDumps = @(Get-NewDumpFiles -Before $baselineDumps -After $afterDumps)
    Write-Utf8NoBomFile -LiteralPath (Join-Path $sessionDirectory 'new-dumps.json') -Content (ConvertTo-Json -InputObject $manifest.newDumps -Depth 4)

    $manifest.status = if ($capturePartial) {
        'capture-partial'
    }
    elseif ($manifest.gradle.exitCode -eq 0) {
        'completed'
    }
    else {
        'captured-nonzero-exit'
    }
}
catch {
    $caughtError = $_
    $manifest.status = 'failed'
    $manifest.errors = @($manifest.errors) + @([pscustomobject]@{
        message = $_.Exception.Message
        category = [string]$_.CategoryInfo.Category
        scriptStackTrace = $_.ScriptStackTrace
    })
    Write-CaptureLog -Level ERROR -Message $_.Exception.Message

    if ($null -ne $minecraftProcess) {
        Write-CaptureLog -Level WARN -Message 'Capture failed after client discovery. Waiting for the Minecraft client to exit before reading logs.'
        try {
            Wait-ProcessExitById -ProcessId ([uint32]$minecraftProcess.ProcessId)
            $manifest.minecraft.exitedAt = [DateTimeOffset]::Now
            $manifest.gradle.exitCode = Wait-GradleProcessExitCode -GradleProcess $gradleProcess
            Start-Sleep -Seconds 5
            $manifest.gameLogs = @(Copy-GameLogs -NormalizedProjectRoot $normalizedProjectRoot -SessionDirectory $sessionDirectory)
            $afterFlightRecorderFiles = @(Get-FlightRecorderFileSnapshot -NormalizedProjectRoot $normalizedProjectRoot)
            $copiedFlightRecorderFiles = @(Copy-NewFlightRecorderFiles -Before $baselineFlightRecorderFiles -After $afterFlightRecorderFiles -SessionDirectory $sessionDirectory)
            $decoderResult = Invoke-FlightRecorderDecoder `
                -FlightRecorderFiles $copiedFlightRecorderFiles `
                -JavaExecutable $manifest.minecraft.executablePath `
                -NormalizedProjectRoot $normalizedProjectRoot
            $manifest.flightRecorderFiles = @($decoderResult.records)
            $manifest.errors = @($manifest.errors) + @($decoderResult.errors)
        }
        catch {
            $followUpMessage = "Post-failure process wait or log collection failed: $($_.Exception.Message)"
            $manifest.errors = @($manifest.errors) + @([pscustomobject]@{ message = $followUpMessage })
            Write-CaptureLog -Level ERROR -Message $followUpMessage
        }
    }
    elseif ($null -ne $gradleProcess) {
        $manifest.processTreeCleanup.attempted = $true
        $cleanupResult = Stop-GradleProcessTree -GradleProcess $gradleProcess
        $manifest.processTreeCleanup.stoppedProcessIds = @($cleanupResult.stopped)
        $manifest.errors = @($manifest.errors) + @($cleanupResult.errors)
        $gradleProcess.Refresh()
        if (-not $gradleProcess.HasExited) {
            [void]$gradleProcess.WaitForExit(30000)
            $gradleProcess.Refresh()
        }
        if ($gradleProcess.HasExited) {
            $manifest.gradle.exitCode = [int]($gradleProcess.ExitCode)
        }
        else {
            $message = "Gradle root PID $($gradleProcess.Id) was still running 30 seconds after process-tree cleanup."
            $manifest.errors = @($manifest.errors) + @([pscustomobject]@{ message = $message })
            Write-CaptureLog -Level ERROR -Message $message
        }
    }
}
finally {
    if ($null -ne $procDumpProcess -and -not $procDumpProcess.HasExited) {
        try {
            Stop-Process -Id $procDumpProcess.Id -Force -ErrorAction Stop
            Write-CaptureLog -Level WARN -Message 'Stopped the ProcDump process started by this session because it was still running.'
        }
        catch {
            $message = "Could not stop the ProcDump process started by this session: $($_.Exception.Message)"
            $manifest.errors = @($manifest.errors) + @([pscustomobject]@{ message = $message })
            Write-CaptureLog -Level ERROR -Message $message
        }
    }

    if ($wprStarted) {
        try {
            Write-CaptureLog -Level INFO -Message "Stopping WPR and saving the trace: $($manifest.wpr.tracePath)"
            $wprStopResult = Invoke-NativeTool -FilePath $wprCommand.Source -Arguments @('-stop', $manifest.wpr.tracePath)
            if ($wprStopResult.ExitCode -ne 0) {
                throw "WPR stop failed (exit $($wprStopResult.ExitCode)): $($wprStopResult.Output)"
            }
            $manifest.wpr.stopResult = 'stopped-and-saved'
        }
        catch {
            $stopMessage = $_.Exception.Message
            Write-CaptureLog -Level ERROR -Message $stopMessage
            $manifest.errors = @($manifest.errors) + @([pscustomobject]@{ message = $stopMessage })
            $manifest.status = Get-CaptureStatusAfterDiagnosticFailure -CurrentStatus $manifest.status
            try {
                $wprCancelResult = Invoke-NativeTool -FilePath $wprCommand.Source -Arguments @('-cancel')
                if ($wprCancelResult.ExitCode -ne 0) {
                    throw "WPR cancel failed (exit $($wprCancelResult.ExitCode)): $($wprCancelResult.Output)"
                }
                $manifest.wpr.stopResult = 'stop-failed-cancelled'
            }
            catch {
                $cancelMessage = $_.Exception.Message
                $manifest.wpr.stopResult = 'stop-and-cancel-failed'
                $manifest.errors = @($manifest.errors) + @([pscustomobject]@{ message = $cancelMessage })
                $manifest.status = Get-CaptureStatusAfterDiagnosticFailure -CurrentStatus $manifest.status
                Write-CaptureLog -Level ERROR -Message $cancelMessage
            }
        }
    }

    $manifest.endedAt = [DateTimeOffset]::Now
    Write-Utf8NoBomFile -LiteralPath (Join-Path $sessionDirectory 'manifest.json') -Content ($manifest | ConvertTo-Json -Depth 8)
    Write-CaptureLog -Level INFO -Message "Capture session ended: $sessionDirectory"
}

if ($null -ne $caughtError) {
    throw $caughtError
}

Write-Output $sessionDirectory
