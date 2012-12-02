#!/bin/bash

rm target/loop-*-jar-with-dependencies.jar
mvn -DskipTests=true package assembly:single && java -jar ./target/loop-1.0-jar-with-dependencies.jar
cp ./target/loop-1.0-jar-with-dependencies.jar ./loop.jar
