@echo off

REM Compiles the Agent Kernel and creates the ak.jar file.
REM Make sure to set JAVA_HOME and AK_HOME appropriately.

echo Building the Comet Way Agent Kernel...

set JAVA_HOME=C:\PROGRA~1\Java\JDK15~1.0_0
echo JAVA_HOME=%JAVA_HOME%

set AK_HOME=C:\PROGRA~1\cometway\ak
echo AK_HOME=%AK_HOME%

set CLASSPATH=.;%AK_HOME%\temp_classes;%AK_HOME%\src
echo CLASSPATH=%CLASSPATH%

set JAVAC=%JAVA_HOME%\bin\javac -classpath %CLASSPATH% -d %AK_HOME%\temp_classes
echo JAVAC=%JAVAC%

set JAR=%JAVA_HOME%\bin\jar
echo JAR=%JAR%

echo Creating %AK_HOME%\temp_classes directory
cd %AK_HOME%
mkdir temp_classes

REM Jakarta-ORO no longer needed - using native java.util.regex

echo Compiling Comet Way Agents in %AK_HOME%\src\com\cometway
cd %AK_HOME%\src\
%JAVAC% -target 1.5 com\cometway\ak\*.java com\cometway\email\*.java com\cometway\httpd\*.java com\cometway\io\*.java com\cometway\jdbc\*.java com\cometway\net\*.java com\cometway\om\*.java com\cometway\props\*.java com\cometway\states\*.java com\cometway\swing\*.java com\cometway\text\*.java com\cometway\tools\*.java com\cometway\util\*.java com\cometway\xml\*.java

echo Creating %AK_HOME%\ak.jar...
cd %AK_HOME%\temp_classes
%JAR% cmf %AK_HOME%\bin\MANIFEST.MF %AK_HOME%\ak.jar *

echo Removing %AK_HOME%\temp_classes directory
cd %AK_HOME%
rmdir /s /q temp_classes

cd %AK_HOME%\bin\
