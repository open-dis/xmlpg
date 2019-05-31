# xmlpg

[![Build Status](https://travis-ci.org/open-dis/xmlpg.svg?branch=master)](https://travis-ci.org/open-dis/xmlpg)

XML Multi-Language Protocol Generator (XMLPG) is a XML-based network protocol implementation generator. Write an XML file and generate network message code.

XMLPG generates Java, C++, C#, Objective-C, Python, and JavaScript language protocol implementations from an abstract description of the protocol in XML. This release includes an example partial implementation of the Distributed Interactive Simulation (DIS) protocol.

Making use of XMLPG is typically a multi-step process. First of all, you need to write an XML file that describes the protocol. An example of this is the file `DIS2012.xml`. This file is used as input to the XMLPG program, which uses it to generate Java and C++ language implementations of the protocol. These language files need to be compiled themselves to generate a usable protocol.

## Compiling

The XMLPG system uses the Maven build system, available from Apache.org.

* `mvn compile`: Compiles the XMLPG program itself
* `dist`: default task, does most operations.

## Running

There is a sample script `runXmlpg.sh` that uses a compiled version of xmlpg to generate code for several languages.

## License

XMLPG is released under the BSD license. See `LICENSE.md` for details. 

## Other

Convert XML to JSON at

http://codebeautify.org/xmltojson


