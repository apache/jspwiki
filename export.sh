#!/bin/sh

# Create a classpath

CP="build:tests/etc:etc/i18n:tests/lib/stripes-1.5.jar:src:tests/build"

for i in lib/*.jar
do
    CP=$i:${CP}
done

java -classpath $CP org.apache.wiki.content.Exporter $@
