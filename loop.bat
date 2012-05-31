@echo off
rem Loop batch file and runtime launcher
rem	by wanghaojie
rem
rem Only works on Windows NT platforms
rem --------------------------------------------

rem Get LOOP_HOME
if not "%LOOP_HOME%" == "" goto gotHome
cd..
set LOOP_HOME=%cd%
:gotHome
if exist "%LOOP_HOME%\lib\loop.jar" goto okHome
rem LOOP_HOME is not set correctly
rem Failed to launch ...
:okHome

rem Get parameters string
set PARAMS=%*


java -classpath %LOOP_HOME%\lib\loop.jar:lib\* loop.Loop %PARAMS%