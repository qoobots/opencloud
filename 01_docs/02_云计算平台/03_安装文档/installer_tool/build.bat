@echo off
chcp 65001 >nul
echo =============================================
echo   OpenCloud 安装工具 - 一键打包为 .exe
echo =============================================

REM 检查 Python 是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Python，请先安装 Python 3.10+
    pause
    exit /b 1
)

REM 安装依赖
echo [1/3] 安装 Python 依赖...
pip install -r requirements.txt -q
pip install pyinstaller -q

REM 打包
echo [2/3] 开始打包...
pyinstaller opencloud_installer.spec --noconfirm

REM 检查结果
if exist "dist\OpenCloud安装工具.exe" (
    echo [3/3] 打包成功！
    echo 可执行文件位于: dist\OpenCloud安装工具.exe
    explorer dist
) else (
    echo [错误] 打包失败，请检查 PyInstaller 输出
)

pause
