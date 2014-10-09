#!/bin/sh
TMPFOLDER=/tmp/JDecafTempFolder
if [ ! -e bin/JDecafCompiler.class ]; then
   echo "This is not the right folder. Exiting..."
   exit -1
elif [ -e $TMPFOLDER ]; then
   echo "$TMPFOLDER exists. Exiting..."
   exit -2
fi
mkdir ${TMPFOLDER}
jar cfmv ${TMPFOLDER}/JDecafCompiler.jar manifest.txt -C bin .
cp -rv javadecaf.bat javadecaf.sh lib   ${TMPFOLDER}
cd ${TMPFOLDER}
zip -r JDecafCompiler.zip *
mv JDecafCompiler.zip ..
cd ..
rm -rf $TMPFOLDER
echo "Done!"
echo
echo "Created /tmp/JDecafCompiler.zip."
