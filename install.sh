#!/bin/sh
# This installer script should be executed from the ak root directory.

echo Installing Comet Way Agent Kernel Scripts

BIN_DIR=${HOME}/bin
echo Creating ${BIN_DIR} directory
mkdir ${BIN_DIR}

CLASSES_DIR=${HOME}/classes
echo Creating ${CLASSES_DIR} directory
mkdir ${CLASSES_DIR}

AK_JAR=`pwd`/ak.jar

JC_FILE=${HOME}/bin/jc
echo Creating ${JC_FILE}
echo "#! /bin/sh" > ${JC_FILE}
echo "echo Compiling..." >> ${JC_FILE}
echo "ls \$*" >> ${JC_FILE}
echo "CLASSPATH=${CLASSES_DIR}:${AK_JAR}" >> ${JC_FILE}
echo "javac -classpath \${CLASSPATH} -d ${CLASSES_DIR} \$*" >> ${JC_FILE}
chmod u+rwx ${JC_FILE}

AK_FILE=${HOME}/bin/ak
echo Creating ${AK_FILE}
echo "#! /bin/sh" > ${AK_FILE}
echo "CLASSPATH=${CLASSES_DIR}:${AK_JAR}" >> ${AK_FILE}
echo "java -classpath \${CLASSPATH} com.cometway.ak.AK \$*" >> ${AK_FILE}
chmod u+rwx ${AK_FILE}

AB_FILE=${HOME}/bin/ab
echo Creating ${AB_FILE}
echo "#! /bin/sh" > ${AB_FILE}
echo "CLASSPATH=${CLASSES_DIR}:${AK_JAR}" >> ${AB_FILE}
echo "java -classpath \${CLASSPATH} com.cometway.ak.AK -startup_agent com.cometway.swing.StartupEditor" >> ${AB_FILE}
chmod u+rwx ${AB_FILE}



