@echo off
title AI Flashcard Server

:: Add Ollama to PATH
set PATH=%PATH%;%LOCALAPPDATA%\Programs\Ollama

echo ================================================
echo   AI Flashcard Server
echo ================================================
echo.

:: Check Ollama
where ollama >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Ollama chua duoc cai!
    echo    Tai tai: https://ollama.com/download
    pause
    exit /b 1
)

:: Check if gemma2:2b is pulled
ollama list 2>nul | findstr "gemma2" >nul
if errorlevel 1 (
    echo [INFO] Chua co model, dang tai gemma2:2b ~1.6GB...
    ollama pull gemma2:2b
    if errorlevel 1 (
        echo [ERROR] Tai model that bai!
        pause
        exit /b 1
    )
)

:: Install Python deps if needed
python -c "import fastapi, uvicorn, requests" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Dang cai Python packages...
    pip install fastapi uvicorn requests pydantic -q
)

echo.
echo [OK] Ollama san sang
echo [OK] Model gemma2:2b da co
echo.
echo Server dang chay tai: http://localhost:8000
echo Android emulator dung: http://10.0.2.2:8000
echo Nhan Ctrl+C de dung server
echo.

python server.py

pause
