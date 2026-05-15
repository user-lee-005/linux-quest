@rem
@rem LinuxQuest Gradle Wrapper Bootstrap
@rem
@rem This script downloads the Gradle wrapper JAR and delegates to it.
@rem Alternatively, open this project in Android Studio which handles
@rem the wrapper automatically.
@rem

@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
set APP_NAME=Gradle
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set JAVA_EXE=java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

goto execute

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

:execute
if not exist "%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" (
    echo Downloading Gradle wrapper...
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.7-bin.zip' -OutFile '%TEMP%\gradle.zip'; Expand-Archive '%TEMP%\gradle.zip' -DestinationPath '%TEMP%\gradle-dist' -Force"
    copy "%TEMP%\gradle-dist\gradle-8.7\lib\gradle-wrapper-*.jar" "%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" >nul 2>&1
    echo NOTE: For best results, open this project in Android Studio.
)

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -jar "%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" %*

:end
endlocal
