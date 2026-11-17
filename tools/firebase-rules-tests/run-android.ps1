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
    foreach ($port in @(8086, 9096)) {
        $existing = @(& $adb -s $serial reverse --list | Select-String "tcp:$port\s+tcp:")
        if ($existing.Count -gt 0) {
            if ($existing.Count -ne 1 -or $existing[0].Line -notmatch "tcp:$port\s+tcp:$port$") { throw 'Existing reverse mapping conflicts with emulator port' }
        } else {
            & $adb -s $serial reverse "tcp:$port" "tcp:$port"
            if ($LASTEXITCODE -ne 0) { throw 'ADB reverse failed' }
            $ownedPorts += $port
        }
    }
    Push-Location (Join-Path $PSScriptRoot '../../mobile/android')
    try {
        & .\gradlew.bat connectedDebugAndroidTest --init-script $init '-Pandroid.testInstrumentationRunnerArguments.class=com.havamania.FirebaseCityContractDeviceTest' '-Pandroid.testInstrumentationRunnerArguments.firebaseEmulatorContract=true' --console=plain --max-workers=2 --no-daemon
        $exitCode = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    foreach ($port in $ownedPorts) { & $adb -s $serial reverse --remove "tcp:$port" }
}
exit $exitCode
