@echo off
setlocal enabledelayedexpansion

set DIR=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIR%

set DEFAULT_JVM_OPTS=
set WRAPPER_VERSION=8.9
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set WRAPPER_DISTS=https://services.gradle.org/distributions/gradle-%WRAPPER_VERSION%-all.zip https://downloads.gradle.org/distributions/gradle-%WRAPPER_VERSION%-all.zip

if exist "%WRAPPER_JAR%" goto findJavaFromJavaHome

echo Gradle wrapper JAR not found. Attempting to download %WRAPPER_VERSION% distribution...
set TMP_ZIP=%TEMP%\gradle-%RANDOM%.zip
set DOWNLOAD_SUCCESS=
for %%U in (%WRAPPER_DISTS%) do (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '%%U' -OutFile '%TMP_ZIP%'; exit 0 } catch { exit 1 }"
    if not errorlevel 1 (
        set DOWNLOAD_SUCCESS=true
        goto :extractWrapper
    ) else (
        echo Failed to download from %%U
    )
)

echo ERROR: Unable to download Gradle wrapper - ensure PowerShell web access is permitted.
exit /b 1

:extractWrapper
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Add-Type -AssemblyName 'System.IO.Compression.FileSystem'; $zip = [System.IO.Compression.ZipFile]::OpenRead('%TMP_ZIP%'); $entry = $zip.GetEntry('gradle-%WRAPPER_VERSION%/lib/gradle-wrapper-%WRAPPER_VERSION%.jar'); if ($entry -eq $null) { throw 'Wrapper JAR not found in distribution.' } $targetDir = [System.IO.Path]::GetDirectoryName('%WRAPPER_JAR%'); if (-not [System.IO.Directory]::Exists($targetDir)) { [System.IO.Directory]::CreateDirectory($targetDir) } $entry.ExtractToFile('%WRAPPER_JAR%', $true); $zip.Dispose(); Remove-Item '%TMP_ZIP%' -Force; } catch { Write-Error $_; exit 1 }"
if not exist "%WRAPPER_JAR%" (
    echo ERROR: Failed to extract Gradle wrapper JAR.
    exit /b 1
)

echo Gradle wrapper JAR downloaded to %WRAPPER_JAR%

:findJavaFromJavaHome
if defined JAVA_HOME goto useJavaHome

set JAVA_EXE=java.exe
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%

:useJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto init

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
exit /b 1

:init
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
