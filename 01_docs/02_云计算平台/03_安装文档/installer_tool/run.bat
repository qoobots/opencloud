@echo off
chcp 65001 >nul
echo 正在启动 OpenCloud 安装工具...
cd /d "%~dp0"
python main.py
if errorlevel 1 (
    echo.
    echo [错误] 启动失败，请确保已安装依赖：
    echo   pip install -r requirements.txt
    pause
)
