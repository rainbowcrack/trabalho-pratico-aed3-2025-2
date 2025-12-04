#!/bin/bash
# Script para iniciar o servidor PetMatch com todas as dependências

CLASSPATH="Codigo/target/classes"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/org/apache/commons/commons-compress/1.26.2/commons-compress-1.26.2.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/commons-codec/commons-codec/1.16.1/commons-codec-1.16.1.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/commons-io/commons-io/2.15.1/commons-io-2.15.1.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar"

java -cp "$CLASSPATH" br.com.mpet.InterfaceWithServer
