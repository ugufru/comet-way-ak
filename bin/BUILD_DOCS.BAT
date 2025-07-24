@echo off

REM Compiles javadocs for the Agent Kernel into the javadocs directory.
REM Make sure to set JAVA_HOME and AK_HOME appropriately.

set JAVA_HOME=C:\j2sdk
set AK_HOME=C:\projects\ak

set CLASSPATH=%AK_HOME%\ak.jar
set SOURCEPATH=%AK_HOME%\src
set JDOC=%JAVA_HOME%\bin\javadoc -classpath %CLASSPATH% -sourcepath %SOURCEPATH% -d %AK_HOME%\javadocs


echo Removing old javadocs...
cd %AK_HOME%
rmdir /s /q javadocs
mkdir javadocs

echo Compiling javadocs to %AK_HOME%\javadocs
%JDOC% com.cometway.ak com.cometway.email com.cometway.httpd com.cometway.io com.cometway.jdbc com.cometway.net com.cometway.om com.cometway.props com.cometway.states com.cometway.swing com.cometway.text com.cometway.tools com.cometway.util com.cometway.xml

