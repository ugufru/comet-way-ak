#!/bin/sh

echo Creating temporary classes directory
mkdir ../temp_classes

echo Compiling Comet Way Agent Kernel with Java 21...
# Set JAVA_HOME and PATH to use Java 21
export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/usr/local/opt/openjdk@21"

javac -d ../temp_classes `find ../src -name '*.java' -print`

echo Creating ak.jar...
jar cf ../ak.jar *

echo Removing temporary classes directory
rm -r ../temp_classes

