@echo off
setlocal enabledelayedexpansion
title 海龟汤 AI - 启动器
cd /d "%~dp0"

echo ============================================
echo    海龟汤 AI  启动器
echo ============================================
echo.

rem ---------- 1. 找到 Java ----------
set "JAVA_EXE=java"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

rem 常见 JDK17 安装位置兜底
if not defined JAVA_HOME (
    for %%P in (
        "C:\Program Files\Java\jdk-17\bin\java.exe"
        "C:\Program Files\Java\jdk17\bin\java.exe"
        "C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe"
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.8.1-hotspot\bin\java.exe"
    ) do (
        if exist %%P set "JAVA_EXE=%%~P"
    )
)

"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 没有找到可用的 Java。
    echo        请先安装 JDK 17：https://adoptium.net/temurin/releases/?version=17
    echo        或设置 JAVA_HOME 指向你的 JDK 17 目录。
    pause
    exit /b 1
)

echo [1/3] 使用 Java: %JAVA_EXE%

rem ---------- 2. 找到 jar ----------
set "JAR_FILE="
for %%F in (target\turtle-soup-ai-*.jar) do set "JAR_FILE=%%F"
if not defined JAR_FILE (
    echo [错误] 没有找到可运行文件 target\turtle-soup-ai-*.jar
    echo        请先在项目目录执行：mvnw.cmd clean package
    pause
    exit /b 1
)

echo [2/3] 使用文件: %JAR_FILE%

rem ---------- 3. API Key ----------
if "%DASHSCOPE_API_KEY%"=="" (
    echo [3/3] 未检测到环境变量 DASHSCOPE_API_KEY。
    echo       请输入 DashScope API Key 后回车（本次运行有效，不写盘）。
    echo       如果不需要 AI 功能，直接回车跳过。
    set /p "INPUT_KEY="
    if not "!INPUT_KEY!"=="" set "DASHSCOPE_API_KEY=!INPUT_KEY!"
)

echo.
echo 正在启动，请用浏览器访问 http://localhost:8080
echo 关闭本窗口即可停止服务。
echo ============================================
echo.

"%JAVA_EXE%" -jar "%JAR_FILE%"

echo.
echo 服务已停止。
pause
