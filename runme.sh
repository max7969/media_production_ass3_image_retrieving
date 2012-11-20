#! /bin/sh

cd src/
classpath="../jar/lucene-core-3.6.0.jar:../jar/lire.jar:../jar/sanselan-0.97-incubator.jar:../jar/xmpcore_java6.jar:."
javac -cp $classpath ImageIndexer.java
java -cp $classpath ImageIndexer
cd ..