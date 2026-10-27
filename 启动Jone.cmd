@echo off
rem Jone launcher: keeps this file ASCII-only so cmd.exe never misparses it.
rem All Chinese messages are printed by launch-jone.ps1 (UTF-8 with BOM).
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0launch-jone.ps1"
if errorlevel 1 pause
