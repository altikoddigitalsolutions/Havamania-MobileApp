$ErrorActionPreference = 'Stop'
if ($env:FIRESTORE_EMULATOR_HOST -ne '127.0.0.1:8086' -or $env:FIREBASE_AUTH_EMULATOR_HOST -ne '127.0.0.1:9096') { throw 'Local demo emulators required' }
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
$owned = @()
try {
 foreach ($port in @(8086,9096)) {
  $existing = @(& $adb reverse --list | Select-String "tcp:$port\s+tcp:")
  if ($existing.Count -eq 0) { & $adb reverse "tcp:$port" "tcp:$port"; if ($LASTEXITCODE -ne 0) {throw 'ADB reverse failed'}; $owned += $port }
  elseif ($existing.Count -ne 1 -or $existing[0].Line -notmatch "tcp:$port\s+tcp:$port$") {throw 'Conflicting reverse mapping'}
 }
 Push-Location (Join-Path $PSScriptRoot '../../mobile/android')
 try {
  & .\gradlew.bat '-Pkotlin.compiler.execution.strategy=in-process' connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.havamania.PersonalizationFirestoreDeviceTest' '-Pandroid.testInstrumentationRunnerArguments.firebaseEmulatorContract=true' --console=plain --max-workers=1 --no-daemon
  if ($LASTEXITCODE -ne 0) { throw 'Android SDK preference contract failed' }
 } finally {Pop-Location}
} finally { foreach ($port in $owned) { & $adb reverse --remove "tcp:$port" } }
