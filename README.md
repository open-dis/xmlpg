# XMLPG (XML Multi-Language Protocol Generator)

Xmlpg allows you to describe a binary message protocol in an XML file and then generate source code to parse the messages in one or more programming languages; Java, C++, C#, Objective-C, Python, and JavaScript languages.

For info about the language see [XMLPG.md](XMLPG.md).

# Relation to Open DIS

Xmlpg was used to auto-generate the initial source code for the PDU's of the following Open DIS projects:
- [open-dis-java](https://github.com/open-dis/open-dis-java)
- [open-dis-cpp](https://github.com/open-dis/open-dis-cpp)
- [open-dis-javascript](https://github.com/open-dis/open-dis-javascript)
- [open-dis-python](https://github.com/open-dis/open-dis-python)
- [open-dis-csharp](https://github.com/open-dis/open-dis-csharp)
- [open-dis-objectivec](https://github.com/open-dis/open-dis-objectivec)

The original XML specification file for Open DIS is [DIS2012.xml](DIS2012.xml).
This specification file is currently not maintained.
Any corrections to the generated Open DIS source code are now being made directly to the downstream projects referenced above.

## Compiling

The XMLPG tool uses the Maven build system, available from Apache.org.

* `mvn compile`: Compiles the XMLPG program itself

## Running

There is a sample script `runXmlpg.sh` that uses the sample `DIS2012.xml` spec and will generate output in `target/generated-sources/`.

## License

XMLPG is released under the BSD license. See [LICENSE.md](LICENSE.md) for details. 
