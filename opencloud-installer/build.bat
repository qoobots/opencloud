@echo off
chcp 65001 >nul
title OpenCloud 安装工具 - 打包

echo =============================================
echo   OpenCloud 安装工具 - 打包为 .exe
echo =============================================

where node >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Node.js
    pause
    exit /b 1
)

if not exist "node_modules" (
    echo [1/3] 安装依赖...
    npm install --registry=https://registry.npmmirror.com
)

echo [2/3] 开始打包（约需 3~5 分钟）...
npm run build

if exist "dist" (
    echo.
    echo =============================================
    echo [3/3] 打包完成！
    echo 输出目录: dist\
    echo =============================================
    explorer dist
) else (
    echo [错误] 打包失败，请检查输出信息
)

pause
