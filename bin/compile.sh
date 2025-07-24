#!/bin/sh

# Set Java 21 environment
export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/usr/local/opt/openjdk@21"

echo "Using Java version:"
java -version

echo Creating temporary classes directory
mkdir ../temp_classes
cd ../temp_classes

echo Compiling Comet Way Agent Kernel...
CLASSPATH=../temp_classes
javac -classpath $CLASSPATH -d ../temp_classes `find ../src -name '*.java' -print` 
echo Creating ak.jar...
jar cf ../ak.jar *

echo Removing temporary classes directory
rm -r ../temp_classes

