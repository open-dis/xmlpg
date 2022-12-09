# XMLPG (XML Multi-Language Protocol Generator)

Define your binary protocol in an XML file and then use this tool to generate network message marshalling code for Java, C++, C#, Objective-C, Python, and JavaScript languages.

An example XML specification file is [DIS2012.xml](DIS2012.xml).

For more info see [XMLPG.md](XMLPG.md).

## Compiling

The XMLPG tool uses the Maven build system, available from Apache.org.

* `mvn compile`: Compiles the XMLPG program itself

## Running

There is a sample script `runXmlpg.sh` that uses the sample `DIS2012.xml` spec and will generate output in `target/generated-sources/`.

## License

XMLPG is released under the BSD license. See [LICENSE.md](LICENSE.md) for details. 
