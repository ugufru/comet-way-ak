#!/bin/sh

echo Removing old javadocs...
rm -r ../javadocs/*

echo Compiling javadocs...

CLASSPATH=../ak.jar
export CLASSPATH

SOURCEPATH=../src
export SOURCEPATH

javadoc -classpath $CLASSPATH -sourcepath $SOURCEPATH -d ../javadocs com.cometway.ak com.cometway.email com.cometway.httpd com.cometway.io com.cometway.jdbc com.cometway.net com.cometway.om com.cometway.props com.cometway.states com.cometway.swing com.cometway.text com.cometway.tools com.cometway.util com.cometway.xml

echo Building `date +"ak_docs-%m-%d-20%y.tar.gz"`
cd ../..
tar cfz `date +"ak_docs-%m-%d-20%y.tar.gz"` ak/docs ak/javadocs
ls -l `date +"ak_docs-%m-%d-20%y.tar.gz"`

