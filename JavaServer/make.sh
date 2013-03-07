#!/bin/sh

CP=lib/log4jme.jar:\
lib/enum11.jar:\
lib/storable.jar:\
lib/rif.jar:\
lib/pubapi.jar:\
lib/rif-transport-se.jar:\
lib/util.jar:lib/protobuf-java-2.4.1.jar

javac -classpath ${CP} src/gft/api/example/*.java

