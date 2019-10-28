cls
@ECHO OFF
: 设置你的链接KEY即可
SET LANPROXY_CLIENT_KEY=你的CLIENTKEY放到这里即可
: LAN PROXY SERVER HOST
SET LANPROXY_SERVER_HOST=jp.cdjxt.net
: LAN PROXY SERVER PORT
SET LANPROXY_SERVER_PORT=5993
: 以下配置不用修改
SET LANPROXY_PATH=./
SET LANPROXY_DIR=./
color 0a
TITLE lanproxy Management
GOTO MENU
:MENU
CLS
ECHO.
ECHO. * * * *  lanproxy Management  * * * * * * * * * * *
ECHO. * *
ECHO. * 1 start *
ECHO. * *
ECHO. * 2 close *
ECHO. * *
ECHO. * 3 restart*
ECHO. * *
ECHO. * 4 exit *
ECHO. * *
ECHO. * * * * * * * * * * * * * * * * * * * * * * * *
ECHO.
ECHO.plase choose list:
set /p ID=
IF "%id%"=="1" GOTO cmd1
IF "%id%"=="2" GOTO cmd2
IF "%id%"=="3" GOTO cmd3
IF "%id%"=="4" EXIT
PAUSE
:cmd1
ECHO.
ECHO.start lanproxy......
IF NOT EXIST %LANPROXY_DIR%client_windows_amd64.exe ECHO %LANPROXY_DIR%client_windows_amd64.exe不存在
IF EXIST %LANPROXY_DIR% start %LANPROXY_DIR%client_windows_amd64.exe -s %LANPROXY_SERVER_HOST% -p %LANPROXY_SERVER_PORT% -k %LANPROXY_CLIENT_KEY% -ssl true
ECHO.OK
PAUSE
GOTO MENU
:cmd2
ECHO.
ECHO.close lanproxy......
taskkill /F /IM client_windows_amd64.exe > nul
ECHO.OK
PAUSE
GOTO MENU
:cmd3
ECHO.
ECHO.close lanproxy......
taskkill /F /IM client_windows_amd64.exe > nul
ECHO.OK
GOTO cmd1
GOTO MENU
