package edu.nps.moves.xmlpg;

import java.io.*;
import java.util.*;

public class SchemaGenerator extends Generator
{
    /** Maps the primitive types listed in the XML file to the schema types */
    Properties types = new Properties();

    /** A property list that contains schema-specific code generation information, such
     * as package names, includes, etc.
     */
    Properties schemaProperties;

    public SchemaGenerator(HashMap pClassDescriptions, Properties pSchemaProperties)
    {
        super(pClassDescriptions, pSchemaProperties);

        Properties systemProperties = System.getProperties();
        String clPduOffset = systemProperties.getProperty("xmlpg.pduOffset");

        String directory = null;
        String clDirectory = systemProperties.getProperty("xmlpg.generatedSourceDir");

        try {
            if(clPduOffset != null && Integer.parseInt(clPduOffset) > 0)
                pSchemaProperties.setProperty("pduOffset", clPduOffset);
        }
        catch(NumberFormatException e)
        {
            System.out.println("PDU offset is not an integer. Modify the XML file to fix value.");
            System.out.println(e);
            System.exit(-1);
        }

        // Directory to place generated source code
        if(clDirectory != null)
            pSchemaProperties.setProperty("directory", clDirectory);

        super.setDirectory(pSchemaProperties.getProperty("directory"));

        // Set up a mapping between the strings used in the XML file and the strings used
        // in the java file, specifically the data types. This could be externalized to
        // a properties file, but there's only a dozen or so and an external props file
        // would just add some complexity.
        types.setProperty("unsigned short", "uint32");
        types.setProperty("unsigned byte", "uint32");
        types.setProperty("unsigned int", "uint32");
        types.setProperty("unsigned long", "uint64");

        types.setProperty("byte", "sint32");
        types.setProperty("short", "sint32");
        types.setProperty("int", "sint32");
        types.setProperty("long", "sint64");

        types.setProperty("double", "double");
        types.setProperty("float", "float");
    }

    /**
     * Generates the schema source code classes
     */
    public void writeClasses()
    {
        this.createDirectory();

        Iterator it = classDescriptions.values().iterator();

        // Loop through all the class descriptions, generating a header file and schema file for each.
        while(it.hasNext())
        {
            try
            {
                GeneratedClass aClass = (GeneratedClass)it.next();
                // System.out.println("Generating class " + aClass.getName());
                this.writeSchemaFile(aClass);
                // this.writeSchemaFile(aClass);
            }
            catch(Exception e)
            {
                System.out.println("error creating source code " + e);
            }

        } // End while

    }

    /**
     * Generate a schema file for the classes
     */
    public void writeSchemaFile(GeneratedClass aClass)
    {
        try
        {
            String name = aClass.getName();
            //System.out.println("Creating schema source code files for " + name);
            String headerFullPath = getDirectory() + "/" + name + ".schema";
            File outputFile = new File(headerFullPath);
            outputFile.createNewFile();
            PrintWriter pw = new PrintWriter(outputFile);

            // Write includes for any classes we may reference. this generates multiple #includes if we
            // use a class multiple times, but that's innocuous. We could sort and do a unqiue to prevent
            // this if so inclined.

            String namespace = languageProperties.getProperty("namespace");
            // Print out namespace, if any
            if(namespace != null)
            {
                pw.println("package " + namespace.toLowerCase() + ";");
                namespace = namespace + "/";
            }
            else
                namespace = "";

            Set attribs = new HashSet<String>();
            for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++)
            {
                ClassAttribute anAttribute = (ClassAttribute)aClass.getClassAttributes().get(idx);

                if (attribs.contains(anAttribute.getType()))
                    continue;

                // If this attribute is a class, we need to do an import on that class
                if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.CLASSREF)
                {
                    pw.println("import \"" + namespace + anAttribute.getType() + ".schema\";");
                }

                // if this attribute is a variable-length list that holds a class, we need to
                // do an import on the class that is in the list.
                if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.VARIABLE_LIST)
                {
                    pw.println("import \"" + namespace + anAttribute.getType() + ".schema\";");
                }

                attribs.add(anAttribute.getType());
            }

            // if we inherit from another class we need to do an include on it
            if (!aClass.getParentClass().isEmpty() &&
                    !aClass.getParentClass().equalsIgnoreCase("root") &&
                    !aClass.getParentClass().equalsIgnoreCase("Pdu")) {
                pw.println("import \"" + namespace + aClass.getParentClass() + ".schema\";");
            }

            pw.println();

            // Print out the class comments, if any
            if(aClass.getClassComments() != null)
            {
                pw.println("// " + aClass.getClassComments() );
            }

            /*
            pw.println();
            pw.println("// Copyright (c) 2007-2012, MOVES Institute, Naval Postgraduate School. All rights reserved. ");
            pw.println("// Licensed under the BSD open source license. See http://www.movesinstitute.org/licenses/bsd.html");
            pw.println("//");
            pw.println("// @author DMcG, jkg");
            pw.println();
            */

            int pdu = 0;
            for(int idx = 0; idx < aClass.getInitialValues().size(); idx++) {
                InitialValue aValue = (InitialValue)aClass.getInitialValues().get(idx);

                if (aValue.getVariable().equalsIgnoreCase("pduType")) {
                    pdu = Integer.parseInt(aValue.getVariableValue());
                    String pduOffset = languageProperties.getProperty("pduOffset");
                    if (pduOffset != null)
                        pdu += Integer.parseInt(pduOffset);
                }
            }

            if (pdu > 0) {
                pw.println("component " + aClass.getName() + " {");
                pw.println("  id = " + pdu + ";");
                pw.println();
            } else {
                pw.println("type " + aClass.getName() + " {");
            }

            int id = 1;
            if (!aClass.getParentClass().isEmpty() &&
                !aClass.getParentClass().equalsIgnoreCase("root") &&
                !aClass.getParentClass().equalsIgnoreCase("Pdu")) {
                pw.println("  " + "/** Schema does not support inheritance, this is as close as we can get. */");
                pw.println("  " + aClass.getParentClass() + " super = " + id + ";");
                pw.println();
                id++;
            }

            for(int idx = 0; idx < aClass.getClassAttributes().size(); idx++, id++)
            {
                ClassAttribute anAttribute = (ClassAttribute)aClass.getClassAttributes().get(idx);

                if (idx > 0)
                    pw.println();

                if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE)
                {
                    if(anAttribute.getComment() != null)
                        pw.println("  " + "/** " + anAttribute.getComment() + " */");

                    pw.println("  " + types.get(anAttribute.getType()) + " " + makeSnakeCase(anAttribute.getName()) + " = " + (id) + ";");

                }

                if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.CLASSREF)
                {
                    if(anAttribute.getComment() != null)
                        pw.println("  " + "/** " + anAttribute.getComment() + " */");

                    pw.println("  " + anAttribute.getType() + " " + makeSnakeCase(anAttribute.getName()) + " = " + id + ";");
                }

                if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.FIXED_LIST)
                {
                    if(anAttribute.getComment() != null)
                        pw.println("  " + "/** " + anAttribute.getComment() + " */");

                    pw.println("  list<" + types.get(anAttribute.getType()) + "> " + makeSnakeCase(anAttribute.getName()) + " = " + id + ";");
                }

                if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.VARIABLE_LIST)
                {
                    if(anAttribute.getComment() != null)
                        pw.println("  " + "/** " + anAttribute.getComment() + " */");

                    pw.println("  list<" + anAttribute.getType() + "> " + makeSnakeCase(anAttribute.getName()) + " = " + id + ";");
                }
            }


            pw.println("}");
            pw.println();

            this.writeLicenseNotice(pw);

            pw.flush();
            pw.close();
        } // End of try
        catch(Exception e)
        {
            System.out.println(e);
        }

    } // End write header file

    static final String makeSnakeCase(String in) {
        return in.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
    }

    private void writeLicenseNotice(PrintWriter pw)
    {
        pw.println("// Copyright (c) 1995-2009 held by the author(s).  All rights reserved.");

        pw.println("// Redistribution and use in source and binary forms, with or without");
        pw.println("// modification, are permitted provided that the following conditions");
        pw.println("//  are met:");
        pw.println("// ");
        pw.println("//  * Redistributions of source code must retain the above copyright");
        pw.println("// notice, this list of conditions and the following disclaimer.");
        pw.println("// * Redistributions in binary form must reproduce the above copyright");
        pw.println("// notice, this list of conditions and the following disclaimer");
        pw.println("// in the documentation and/or other materials provided with the");
        pw.println("// distribution.");
        pw.println("// * Neither the names of the Naval Postgraduate School (NPS)");
        pw.println("//  Modeling Virtual Environments and Simulation (MOVES) Institute");
        pw.println("// (http://www.nps.edu and http://www.MovesInstitute.org)");
        pw.println("// nor the names of its contributors may be used to endorse or");
        pw.println("//  promote products derived from this software without specific");
        pw.println("// prior written permission.");
        pw.println("// ");
        pw.println("// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS");
        pw.println("// AS IS AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT");
        pw.println("// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS");
        pw.println("// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE");
        pw.println("// COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,");
        pw.println("// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,");
        pw.println("// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;");
        pw.println("// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER");
        pw.println("// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT");
        pw.println("// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN");
        pw.println("// ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
        pw.println("// POSSIBILITY OF SUCH DAMAGE.");

    }
}
