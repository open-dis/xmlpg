# Example for running xmlpg and generating code for DIS6 protocol.
# This uses exec-maven-plugin to run, but you could run from xmlpg.jar too.

#schema=../DISDescription/DIS6.xml
schema=DIS2012.xml

mvn clean compile

for lang in java cpp python javascript objc csharp; do

mvn exec:java -Dexec.mainClass=edu.nps.moves.xmlpg.Xmlpg  -Dxmlpg.useHibernate=false -Dxmlpg.useJaxb=false -Dxmlpg.generatedSourceDir=target/generated-sources/xmlpg-`basename $schema .xml`-$lang/ -Dexec.args="$schema $lang"

done;
