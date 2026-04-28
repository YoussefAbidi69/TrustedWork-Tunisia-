@echo off
set "ROOT=%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -Command "& '%ROOT%\.venv\Scripts\python.exe' '%~dp0api.py'"