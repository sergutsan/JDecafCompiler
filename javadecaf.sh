#!/bin/sh
# TODO: this should be put somewhere as part of a proper installation
#    mechanism, maybe with a .deb package, but for now we will just 
#    assume that everything is on the same folder. 
BASEDIR=$(dirname $0)
java -jar ${BASEDIR}/JDecafCompiler.jar $*