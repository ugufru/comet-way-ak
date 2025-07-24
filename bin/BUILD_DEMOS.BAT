@echo off

REM Compiles the Agent Kernel Demos located in the demos directory.
REM Make sure to set JAVA_HOME and AK_HOME appropriately.

set JAVA_HOME=C:\j2sdk
set AK_HOME=C:\projects\ak

set CLASSPATH=%AK_HOME%\ak.jar
set JAVAC=%JAVA_HOME%\bin\javac -classpath %CLASSPATH% -d %AK_HOME%\classes

echo Compiling Demos in %AK_HOME%\demos\
cd %AK_HOME%\demos\ 
%JAVAC% app_loader\*.java biolife\*.java demo_service\*.java directory_scanner\*.java email\*.java hello_world\*.java lightbox_demo\*.java monitors\*.java object_manager\*.java replicator\*.java reporters\*.java robot\*.java sample_agent\*.java scheduler\*.java sessions\*.java system_properties\*.java webserver\src\*.java xml_weather\*.java

echo Completed.
