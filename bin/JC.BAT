@echo off

REM Compiles Java source (.java files) to the %AK_HOME%\classes directory
REM Make sure to set JAVA_HOME and AK_HOME appropriately.

set JAVA_HOME=C:\PROGRA~1\Java\JDK16~1.0
set AK_HOME=C:\PROGRA~1\cometway\ak

set CLASSPATH=.;%AK_HOME%\classes;%AK_HOME%\src;%AK_HOME%\ak.jar
set JAVAC=%JAVA_HOME%\bin\javac -classpath %CLASSPATH% -d %AK_HOME%\classes

if "%1"=="" goto all
	echo Compiling...
	dir %* /b/w/n

	%JAVAC% %*
	goto eof
:all
	echo Compiling *.java
	dir *.java /b/w/n
	%JAVAC% *.java
:eof
