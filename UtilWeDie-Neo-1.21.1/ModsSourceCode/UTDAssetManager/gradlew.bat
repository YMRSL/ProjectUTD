@echo off
setlocal
set "HOST_WRAPPER=%~dp0..\FirstPersonFoodEating\neoforge-1.21.1\gradlew.bat"
if not exist "%HOST_WRAPPER%" (
  echo Workspace NeoForge wrapper not found: %HOST_WRAPPER%
  exit /b 1
)
call "%HOST_WRAPPER%" -p "%~dp0." %*
exit /b %ERRORLEVEL%
