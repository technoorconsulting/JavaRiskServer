@echo off

rem Please put below your GFT user id & password 
set USER_ID=demosmarvasti
set PASSWORD=demosmarvasti

rem Uncomment one of URLs below to select target system and protocol

rem secure demo connection
rem set URL=iphs://api.demo.gftforex.com:8080

rem unsecure demo connection
set URL=iph://api.demo.gftforex.com:8080

rem secure live connection
rem set URL=iphs://api.live.gftforex.com:8080

rem unsecure live connection
rem set URL=iph://api.live.gftforex.com:8080

set CP=.\lib\crypto.jar
set CP=%CP%;.\lib\enum11.jar
set CP=%CP%;.\lib\jcookie.jar
set CP=%CP%;.\lib\log4jme.jar
set CP=%CP%;.\lib\pubapi.jar
set CP=%CP%;.\lib\rif.jar
set CP=%CP%;.\lib\storable.jar
set CP=%CP%;.\lib\util.jar
set CP=%CP%;.\lib\rif-transport-se.jar
set CP=%CP%;.\lib\protobuf-java-2.4.1.jar

rem if "%JAVA_HOME%"=="" goto nojava

"%JAVA_HOME%\bin\java" -Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y -classpath %CP%;.\src -Dgft.url=%URL% -Dgft.login=%USER_ID% -Dgft.password=%PASSWORD% gft.api.example.ApiUsage

goto exit

:nojava

echo No JAVA_HOME variable set

:exit

echo Done
