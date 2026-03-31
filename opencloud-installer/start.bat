@echo off
chcp 65001 >nul
title OpenCloud 安装工具 - 开发模式

echo =============================================
echo   OpenCloud 安装工具 - 环境检查
echo =============================================

where node >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Node.js，请先安装 Node.js 18+
    echo 下载地址: https://nodejs.org/
    pause
    exit /b 1
)

for /f "tokens=1 delims=v" %%i in ('node -v') do set NODE_VER=%%i
echo [OK] Node.js 已安装

if not exist "node_modules" (
    echo [1/2] 安装依赖包（首次运行需要几分钟）...
    npm install --registry=https://registry.npmmirror.com
    if errorlevel 1 (
        echo [错误] 依赖安装失败
        pause
        exit /b 1
    )
)

echo [2/2] 启动应用...
npm start
