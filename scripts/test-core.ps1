param([string]$GradleCache = "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1", [string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (!$JavaHome) { $JavaHome = 'C:/Program Files/Android/Android Studio/jbr' }
function CachedJar([string]$path, [string]$pattern = '*.jar') {
    $file = Get-ChildItem (Join-Path $GradleCache $path) -Recurse -Filter $pattern |
        Where-Object Name -NotMatch 'sources|javadoc' | Select-Object -First 1 -ExpandProperty FullName
    if (!$file) { throw "Missing dependency: $path. Sync Gradle first." }
    return $file
}
$compilerJar = CachedJar 'org.jetbrains.kotlin/kotlin-compiler-embeddable/2.0.21'
$standardJar = CachedJar 'org.jetbrains.kotlin/kotlin-stdlib/2.0.21'
$jsoupJar = CachedJar 'org.jsoup/jsoup/1.18.1'
$okhttpJar = CachedJar 'com.squareup.okhttp3/okhttp/4.12.0'
$okioJar = CachedJar 'com.squareup.okio/okio-jvm/3.9.0'
$coroutinesJar = CachedJar 'org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.1'
$testClasspath = "$standardJar;$jsoupJar;$okhttpJar;$okioJar;$coroutinesJar"
$runtimeJars = @($compilerJar, $standardJar, (CachedJar 'org.jetbrains.intellij.deps/trove4j'),
    (CachedJar 'org.jetbrains/annotations'), (CachedJar 'org.jetbrains.kotlin/kotlin-reflect/2.0.21'),
    (CachedJar 'org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.6.4'))
$projectRoot = Split-Path $PSScriptRoot -Parent
$output = Join-Path $projectRoot 'build/core-tests'
New-Item -ItemType Directory -Force $output | Out-Null
$sourceFiles = @('app/src/main/java/app/vibedeck/data/model/GameIdentity.kt', 'app/src/main/java/app/vibedeck/data/model/CommunityModels.kt',
    'app/src/main/java/app/vibedeck/data/metadata/MetadataParser.kt', 'app/src/main/java/app/vibedeck/data/metadata/PublicNetworkPolicy.kt',
    'app/src/main/java/app/vibedeck/auth/AuthSession.kt', 'app/src/main/java/app/vibedeck/data/CatalogRefresher.kt',
    'tests/CoreTests.kt', 'tests/AuthSessionTests.kt', 'tests/CatalogRefreshTests.kt') | ForEach-Object { Join-Path $projectRoot $_ }
$compilerArguments = @('-cp', ($runtimeJars -join ';'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect',
    '-jvm-target', '17', '-classpath', $testClasspath, '-d', "$output/tests.jar") + $sourceFiles
$argumentFile = Join-Path $output 'compiler.args'
[System.IO.File]::WriteAllLines($argumentFile, [string[]]($compilerArguments | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }),
    (New-Object System.Text.UTF8Encoding($false)))
& "$JavaHome/bin/java.exe" ('@' + $argumentFile)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$JavaHome/bin/java.exe" '-cp' "$output/tests.jar;$testClasspath" 'CoreTestsKt'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$JavaHome/bin/java.exe" '-cp' "$output/tests.jar;$testClasspath" 'AuthSessionTestsKt'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$JavaHome/bin/java.exe" '-cp' "$output/tests.jar;$testClasspath" 'CatalogRefreshTestsKt'
exit $LASTEXITCODE
