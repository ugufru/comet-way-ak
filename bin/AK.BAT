@echo off

REM Starts the Comet Way Agent Kernel.
REM Make sure to set JAVA_HOME and AK_HOME appropriately.

set JAVA_HOME=C:\PROGRA~1\Java\JDK16~1.0
set AK_HOME=C:\PROGRA~1\cometway\ak

set CLASSPATH=.;%AK_HOME%\classes;%AK_HOME%\ak.jar
set JAVA=%JAVA_HOME%\bin\java -classpath %CLASSPATH%

%JAVA% com.cometway.ak.AK %*
