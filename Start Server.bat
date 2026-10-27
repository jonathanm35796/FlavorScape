@echo off
setlocal
powershell.exe -NoExit -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-local.ps1" -Component Servers -Visible