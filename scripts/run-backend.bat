@echo off
setlocal

for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
set "BACKEND_DIR=%PROJECT_ROOT%\backend"
set "ENV_FILE=%PROJECT_ROOT%\infra\.env.local"

if not exist "%ENV_FILE%" (
  echo Missing %ENV_FILE%
  echo Copy infra\.env.example to infra\.env.local and configure the local secrets first.
  pause
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install a JDK and add it to PATH.
  pause
  exit /b 1
)

set "MAVEN_BIN="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MAVEN_BIN set "MAVEN_BIN=%%M"
if not defined MAVEN_BIN (
  for /f "delims=" %%M in ('where /r "%USERPROFILE%\.m2\wrapper\dists" mvn.cmd 2^>nul') do if not defined MAVEN_BIN set "MAVEN_BIN=%%M"
)
if not defined MAVEN_BIN (
  echo Maven was not found. Install Apache Maven or populate its wrapper cache.
  pause
  exit /b 1
)

powershell.exe -NoProfile -Command "$probe = New-Object Net.Sockets.TcpClient; try { $probe.Connect('127.0.0.1',8080); exit 1 } catch { exit 0 } finally { $probe.Dispose() }"
if errorlevel 1 (
  echo Port 8080 is in use. Stop the previous backend before starting this one.
  echo Also stop the previous Docker worker so AI jobs use this checkout's code.
  pause
  exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
  if not "%%A"=="" set "%%A=%%B"
)

set "SPRING_PROFILES_ACTIVE=local,worker"
set "POSTGRES_HOST=localhost"
set "POSTGRES_PORT=5432"
set "POSTGRES_DB=platform"
set "POSTGRES_APP_USER=app"
set "POSTGRES_MIGRATOR_USER=migrator"
set "REDIS_HOST=localhost"
set "REDIS_PORT=6379"
set "RABBITMQ_HOST=localhost"
set "RABBITMQ_PORT=5672"
set "RABBITMQ_USER=app"
set "RABBITMQ_VHOST=/platform"
set "SMTP_HOST=localhost"
set "SMTP_PORT=1025"
set "SMTP_AUTH_ENABLED=false"
set "SMTP_STARTTLS_ENABLED=false"
set "U03_REQUIRE_DRIVE=false"

cd /d "%BACKEND_DIR%"
if errorlevel 1 (
  echo Could not open backend directory: %BACKEND_DIR%
  pause
  exit /b 1
)

echo Starting API and background worker together with profiles local,worker on port 8080...
echo Stop the old backend AND old worker containers before using this local dev process.
echo Rebuilding backend classes to load the latest grading and AI contracts.
call "%MAVEN_BIN%" clean spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo Backend exited with code %EXIT_CODE%.
  pause
)
exit /b %EXIT_CODE%
