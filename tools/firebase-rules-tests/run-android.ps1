param([switch]$ProcessRestart, [switch]$TransportCut, [switch]$ReplyLoss, [switch]$ProfileContract)
if (@($ProcessRestart, $TransportCut, $ReplyLoss, $ProfileContract | Where-Object { $_ }).Count -gt 1) { throw "Select one special test mode" }
$usesProxy = $TransportCut -or $ReplyLoss
$ErrorActionPreference = 'Stop'
if ($env:FIRESTORE_EMULATOR_HOST -ne '127.0.0.1:8086' -or $env:FIREBASE_AUTH_EMULATOR_HOST -ne '127.0.0.1:9096') {
    throw 'Only localhost demo emulators are permitted'
}
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
$devices = @(& $adb devices | Select-String '^([^\s]+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
if ($devices.Count -ne 1) { throw 'Connect exactly one authorized test device' }
$serial = $devices[0]
$ownedPorts = @()
$init = Join-Path $env:TEMP 'havamania-firebase-sdk-test.gradle'
$outputDir = (Join-Path $env:TEMP 'havamania-city-delete-output-20261116').Replace('\', '/')
$initContent = "gradle.beforeProject { p -> p.plugins.withId('com.android.application') { p.extensions.getByName('androidComponents').finalizeDsl { dsl -> dsl.defaultConfig.applicationId = 'com.havamania' } } }`ngradle.beforeProject { p -> if (p.path == ':app') { p.layout.buildDirectory.set(new File('$outputDir')) } }"
[IO.File]::WriteAllText($init, $initContent)
$exitCode = 1
try {
    $ports = @(8086, 9096)
    if ($usesProxy) { $ports += 9296 }
    if ($ReplyLoss) { $ports += 8088 }
    if ($ProfileContract) { $ports += 9396 }
    foreach ($port in $ports) {
        $targetPort = if ($usesProxy -and $port -eq 8086) { 8087 } elseif ($ReplyLoss -and $port -eq 8088) { 8086 } else { $port }
        $existing = @(& $adb -s $serial reverse --list | Select-String "tcp:$port\s+tcp:")
        if ($existing.Count -gt 0) {
            if ($existing.Count -ne 1 -or $existing[0].Line -notmatch "tcp:$port\s+tcp:$targetPort$") { throw 'Existing reverse mapping conflicts with emulator port' }
        } else {
            & $adb -s $serial reverse "tcp:$port" "tcp:$targetPort"
            if ($LASTEXITCODE -ne 0) { throw 'ADB reverse failed' }
            $ownedPorts += $port
        }
    }
    Push-Location (Join-Path $PSScriptRoot '../../mobile/android')
    try {
        if ($ProcessRestart) {
            & .\gradlew.bat assembleDebug assembleDebugAndroidTest --init-script $init --console=plain --max-workers=2 --no-daemon
            if ($LASTEXITCODE -ne 0) { throw 'QA test build failed' }
            foreach ($apk in @('outputs/apk/debug/app-debug.apk', 'outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
                & $adb -s $serial install -r -t (Join-Path $outputDir $apk)
                if ($LASTEXITCODE -ne 0) { throw 'QA APK installation failed' }
            }
            $scenario = [guid]::NewGuid().ToString()
            foreach ($phase in @('prepare', 'resume')) {
                if ($phase -eq 'resume') {
                    & $adb -s $serial shell am force-stop com.havamania
                    if ($LASTEXITCODE -ne 0) { throw 'QA force-stop failed' }
                    $running = & $adb -s $serial shell pidof com.havamania
                    if ($running) { throw 'QA process is still running' }
                }
                Write-Output "Restart acceptance phase: $phase"
                $result = & $adb -s $serial shell am instrument -w -r -e class 'com.havamania.FirebaseCityContractDeviceTest#processRestartMaintainsDeletionFence' -e firebaseEmulatorContract true -e cityRestartPhase $phase -e cityRestartId $scenario 'com.havamania.test/androidx.test.runner.AndroidJUnitRunner'
                $instrumentExit = $LASTEXITCODE
                $result | Write-Output
                if ($instrumentExit -ne 0 -or ($result -join "`n") -notmatch 'OK \(1 test\)') { throw "Restart phase $phase failed" }
            }
            $exitCode = 0
        } else {
            $testClass = 'com.havamania.FirebaseCityContractDeviceTest'
            $extraArgs = @()
            if ($ProfileContract) {
                if ($env:CITY_PROFILE_TEST_NONCE -notmatch '^[0-9a-f-]{36}$') { throw 'Missing local profile fixture nonce' }
                $testClass += '#serverPremiumMirrorUpdatesAndRevokesWithoutProfileOverwrite,com.havamania.FirebaseCityContractDeviceTest#profileAccountSwitchRejectsQueuedOldOwnerEvents,com.havamania.FirebaseCityContractDeviceTest#productionThemeViewModelIsolatesRealPreferencesAndPremiumAcrossOwners,com.havamania.FirebaseProfileActivityDeviceTest'
                $extraArgs += '-Pandroid.testInstrumentationRunnerArguments.cityProfileContract=true'
                $extraArgs += "-Pandroid.testInstrumentationRunnerArguments.cityProfileNonce=$env:CITY_PROFILE_TEST_NONCE"
            }
            if ($usesProxy) {
                if ($env:CITY_NETWORK_TEST_NONCE -notmatch '^[0-9a-f-]{36}$') { throw 'Missing local proxy nonce' }
                if ($ReplyLoss) {
                    $testClass += '#lostCommitReplyKeepsQueueUntilIdempotentConfirmation'
                    $extraArgs += '-Pandroid.testInstrumentationRunnerArguments.cityReplyLoss=true'
                } else {
                    $testClass += '#transportCutPreservesReservationAndRejectsLateDelete'
                    $extraArgs += '-Pandroid.testInstrumentationRunnerArguments.cityNetworkCut=true'
                }
                $extraArgs += "-Pandroid.testInstrumentationRunnerArguments.cityNetworkNonce=$env:CITY_NETWORK_TEST_NONCE"
            }
            & .\gradlew.bat connectedDebugAndroidTest --init-script $init "-Pandroid.testInstrumentationRunnerArguments.class=$testClass" '-Pandroid.testInstrumentationRunnerArguments.firebaseEmulatorContract=true' @extraArgs --console=plain --max-workers=2 --no-daemon
            $exitCode = $LASTEXITCODE
        }
    } finally { Pop-Location }
} finally {
    foreach ($port in $ownedPorts) { & $adb -s $serial reverse --remove "tcp:$port" }
}
exit $exitCode
