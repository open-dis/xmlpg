/*
 *  Copyright (c) 2013-2015, Naval Postgraduate School
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright 
 * notice, this list of conditions and the following disclaimer in the 
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package edu.nps.moves.xmlpg;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Stack;

/**
 * Generates Javascript source files that marshal and unmarshal to IEEE DIS.
 * 
 * @author DMcG
 */
public class JavascriptGenerator extends Generator
{
    /** Maps the primitive types listed in the XML file to the java types */
    Properties types = new Properties();
    
    /** A property list that contains javascript-specific code generation information, such
     * as package names, imports, etc.
     */
    Properties javascriptProperties;
    
    String namespace = null;
    
    /** What primitive types should be marshalled as. This may be different from
     * the Java get/set methods, ie an unsigned short might have ints as the getter/setter,
     * but is marshalled as a short.
    */
    Properties marshalTypes = new Properties();
    
    /** Similar to above, but used on unmarshalling. There are some special cases (unsigned
     * types) to be handled here.
     */
    Properties unmarshalTypes = new Properties();
    
    /** sizes of various primitive types */
    Properties primitiveSizes = new Properties();
    
    
    public JavascriptGenerator(HashMap pClassDescriptions, Properties pJavascriptProperties)
    {
        super(pClassDescriptions, pJavascriptProperties);

        Properties systemProperties = System.getProperties();
        //System.out.println("System properties:" + systemProperties);
        System.out.println("Javascript properties: " + pJavascriptProperties);
        javascriptProperties = pJavascriptProperties;
        namespace = javascriptProperties.getProperty("namespace");
        super.setDirectory(systemProperties.getProperty("xmlpg.generatedSourceDir"));
        
        //super.setDirectory("javascript/dis");
        System.out.println("Destination directory: " + pJavascriptProperties.getProperty("xmlpg.generatedSourceDir"));

        try
        {
            //Properties systemProperties = System.getProperties();
        }
        catch(Exception e)
        {
            System.out.println("Required property not set. Modify the XML file to include the missing property");
            System.out.println(e);
        }  
        
        // How big various primitive types are
        primitiveSizes.setProperty("unsigned short", "2");
        primitiveSizes.setProperty("unsigned byte", "1");
        primitiveSizes.setProperty("unsigned int", "4");
	primitiveSizes.setProperty("unsigned long", "8");
        
        primitiveSizes.setProperty("byte", "1");
        primitiveSizes.setProperty("short", "2");
        primitiveSizes.setProperty("int", "4");
        primitiveSizes.setProperty("long", "8");
        
        primitiveSizes.setProperty("double", "8");
        primitiveSizes.setProperty("float", "4");
            
        // Set up a mapping between the strings used in the XML file and the strings used
        // in the javascript file. Javascript treats all numbers as basically the
        // same. Initialize them to zero.
        types.setProperty("unsigned short", "0");
        types.setProperty("unsigned byte", "0");
        types.setProperty("unsigned int", "0");
	types.setProperty("unsigned long", "0"); 
        
        types.setProperty("byte", "0");
        types.setProperty("short", "0");
        types.setProperty("int", "0");
        types.setProperty("long", "0");
        
        types.setProperty("double", "double");
        types.setProperty("float", "float");
        
        // Set up a mapping between the strings used in the XML file and the strings used
        // in the java file, specifically the data types. This could be externalized to
        // a properties file, but there's only a dozen or so and an external props file
        // would just add some complexity.
        types.setProperty("unsigned short", "int");
        types.setProperty("unsigned byte", "short");
        types.setProperty("unsigned int", "long");
	types.setProperty("unsigned long", "long"); // This is wrong; java doesn't have an unsigned long. Placeholder for a later BigInt or similar type
        
        types.setProperty("byte", "byte");
        types.setProperty("short", "short");
        types.setProperty("int", "int");
        types.setProperty("long", "long");
        
        types.setProperty("double", "double");
        types.setProperty("float", "float");
        
        // Set up the mapping between primitive types and marshal types.
        
        marshalTypes.setProperty("unsigned short", "UnsignedShort");
        marshalTypes.setProperty("unsigned byte", "UnsignedByte");
        marshalTypes.setProperty("unsigned int", "UnsignedInt");
	marshalTypes.setProperty("unsigned long", "long"); // This is wrong; no unsigned long type in java. Fix with a BigInt or similar
        
        marshalTypes.setProperty("byte", "byte");
        marshalTypes.setProperty("short", "short");
        marshalTypes.setProperty("int", "int");
        marshalTypes.setProperty("long", "long");
        
        marshalTypes.setProperty("double", "float64");
        marshalTypes.setProperty("float", "float32");
        
        // Unmarshalling types
        unmarshalTypes.setProperty("unsigned short", "UShort");
        unmarshalTypes.setProperty("unsigned byte", "UByte");
        unmarshalTypes.setProperty("unsigned int", "UInt");
        unmarshalTypes.setProperty("unsigned long", "long"); // ^^^ This is wrong--should be unsigned
        
        unmarshalTypes.setProperty("byte", "byte");
        unmarshalTypes.setProperty("short", "short");
        unmarshalTypes.setProperty("int", "int");
        unmarshalTypes.setProperty("long", "long");
        
        unmarshalTypes.setProperty("double", "float64");
        unmarshalTypes.setProperty("float", "float32");
        
        // How big various primitive types are
        primitiveSizes.setProperty("unsigned short", "2");
        primitiveSizes.setProperty("unsigned byte", "1");
        primitiveSizes.setProperty("unsigned int", "4");
	primitiveSizes.setProperty("unsigned long", "8");
        
        primitiveSizes.setProperty("byte", "1");
        primitiveSizes.setProperty("short", "2");
        primitiveSizes.setProperty("int", "4");
        primitiveSizes.setProperty("long", "8");
        
        primitiveSizes.setProperty("double", "8");
        primitiveSizes.setProperty("float", "4");
    }
    
     /**
     * Generate the classes and write them to a directory
     */
    @Override
    public void writeClasses()
    {

        this.createDirectory();
        
        Iterator it = classDescriptions.values().iterator();
        
        while(it.hasNext())
        {
            try
           {
              GeneratedClass aClass = (GeneratedClass)it.next();
              String name = aClass.getName();
              
              // Create package structure, if any
              String pack = languageProperties.getProperty("namespace");
              String fullPath;
              
              // If we have a package specified, replace the dots in the package name (edu.nps.moves.dis)
              // with slashes (edu/nps/moves/dis and create that directory
              if(pack != null)
              {
                  pack = pack.replace(".", "/");
                  fullPath = getDirectory() + "/" + pack + "/" + name + ".js";
                  //System.out.println("full path is " + fullPath);
              }
              else
             {
                   fullPath = getDirectory() + "/" + name + ".js";
             }
             System.out.println("Creating Javascript source code file for " + fullPath);
              
              // Create the new, empty file, and create printwriter object for output to it
              File outputFile = new File(fullPath);
              outputFile.getParentFile().mkdirs();
              outputFile.createNewFile();
              PrintWriter pw = new PrintWriter(outputFile);
              
              // print the source code of the class to the file
              this.writeClass(pw, aClass);
           }
           catch(Exception e)
           {
               System.out.println("error creating source code " + e);
               e.printStackTrace();
           }
            
        } // End while
        
        // Write out a require.js exports file. This is later incorporated
        // into the dis.js file via ant.
        // (Commented out; trying to keep the exports in each generated file now)
        /*
        try
        {
            String fullPath = getDirectory() + "/exports.js";
            File outputFile = new File(fullPath);
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
            PrintWriter pw = new PrintWriter(outputFile);
            
            pw.println();
            pw.println("// Exports for the dis module, used in require.js");
            pw.println("// You should delete this if not using with require.js");
            pw.println();
            
            it = classDescriptions.values().iterator();
            while(it.hasNext())
            {
                 GeneratedClass aClass = (GeneratedClass)it.next();
                 pw.println("exports." + aClass.getName() + " = dis." + aClass.getName() + ";");
            }
            pw.println();
            pw.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        */
    
    } // End write classes
    
    /**
     * Generate a source code file with a psuedo-classical constructor. 
     * No getters or setters; that's sorta not the style in javascript.
     */
    private void writeClass(PrintWriter pw, GeneratedClass aClass)
    {
        this.writeClassComments(pw, aClass);
	pw.flush();
        this.writeClassDeclaration(pw, aClass);
	pw.flush();
        this.writeIvars(pw, aClass);
	pw.flush();
        
        this.writeDecoder(pw, aClass);
        this.writeEncoder(pw, aClass);
        pw.flush();
        
        this.writeFlagMethods(pw, aClass);
        pw.flush();
        
        pw.println("}; // end of class");
        pw.println();
        pw.println(" // node.js module support");
        pw.println("exports." + aClass.getName() + " = " + namespace + "." + aClass.getName() + ";");
        pw.println();
        pw.println("// End of " + aClass.getName() + " class");
        pw.println();
        pw.flush();
        pw.close();
    }
    
    /**
     * Write a function that encodes dis binary data into an array
     * 
     * @param pw
     * @param aClass 
     */
    private void writeEncoder(PrintWriter pw, GeneratedClass aClass)
    {
        // Get all the attributes of the class in the correct order
        
        List classHierarchy = new ArrayList();
        GeneratedClass otherClass = aClass;
        do
        {
            classHierarchy.add(otherClass);
            if(otherClass.getParentClass().equalsIgnoreCase("root"))
                break;
            String pClassName = otherClass.getParentClass();
            otherClass = (GeneratedClass)classDescriptions.get(pClassName);
        }
        while(true);
        
        List allAttributes = new ArrayList();
        for(int jdx = classHierarchy.size() -1; jdx >= 0; jdx--)
        {
            GeneratedClass thisLevel = (GeneratedClass)classHierarchy.get(jdx);
            List ivars = thisLevel.getClassAttributes();
            allAttributes.addAll(ivars);
        }
       
        // Start writing the function
        pw.println();
        //pw.println("  " + aClass.getName()+ ".prototype. encodeToBinary = function(outputStream)");
        pw.println("  " + namespace + "." + aClass.getName()+ ".prototype.encodeToBinary = function(outputStream)");
        pw.println("  {");
        
        for(int idx = 0; idx < allAttributes.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)allAttributes.get(idx);
            
            // Write out a method call to deserialize a primitive type
            if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE)
            {
                String marshalType = marshalTypes.getProperty(anAttribute.getType());
                String capped = this.initialCap(marshalType);
                
                // Encode primitive types
                if(marshalType.equalsIgnoreCase("UnsignedByte"))
                    pw.println("       outputStream.writeUByte(this."+ anAttribute.getName() + ");");
                else if (marshalType.equalsIgnoreCase("UnsignedShort"))
                    pw.println("       outputStream.writeUShort(this." + anAttribute.getName()+ ");");
                else if (marshalType.equalsIgnoreCase("UnsignedInt"))
                    pw.println("       outputStream.writeUInt(this." + anAttribute.getName() + ");");
		else if(marshalType.equalsIgnoreCase("UnsignedLong"))
		    pw.println("       outputStream.writeLong" + "(this." + anAttribute.getName() + ");"); // ^^^This is wrong; need to read unsigned here
                else
                    pw.println("       outputStream.write" + capped + "(this." + anAttribute.getName() + ");");
                pw.flush();
            }
            
            // Write out a method call to encode a class.
            if( anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.CLASSREF)
            {                
                pw.println("       this." + anAttribute.getName() + ".encodeToBinary(outputStream);" );
            }
            
            // Write out the method call to encode a fixed length list, aka an array.
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.FIXED_LIST) )
            {
                pw.println("       for(var idx = 0; idx < " + anAttribute.getListLength() + "; idx++)");
                pw.println("       {");
                
                if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                {
                    String marshalType = unmarshalTypes.getProperty(anAttribute.getType());
                    String capped = this.initialCap(marshalType);
                
                    pw.println("          outputStream.write" + capped + "(this." + anAttribute.getName() + "[ idx ] );");
                }
                else if(anAttribute.listIsClass() == true) 
                {
                    pw.println("          this." + anAttribute.getName() + "[ idx ].encodeToBinary(outputStream);");
                }
                
                pw.println("       }");
            }
            
             // Variable length list
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.VARIABLE_LIST) )
            {
                pw.println("       for(var idx = 0; idx < this." + anAttribute.getName() + ".length; idx++)");
                pw.println("       {");
                
                // This is some sleaze. We're an array, but an array of what? We could be either a
                // primitive or a class. We need to figure out which. This is done via the expedient
                // but not very reliable way of trying to do a lookup on the type. If we don't find
                // it in our map of primitives to marshal types, we assume it is a class.
                
                String marshalType = marshalTypes.getProperty(anAttribute.getType());
                
                if(marshalType == null) // It's a class
                {
                    pw.println("           " + anAttribute.getName() + "[idx].encodeToBinary(outputStream);");
                }
                else // It's a primitive
                {
                    String capped = this.initialCap(marshalType);
                    pw.println("           outputStream.write" + capped + "(" + anAttribute.getName() + ");");
                }
                pw.println("       }");
                pw.println();
            } // end of unmarshalling a variable list
            
            
        } // End of loop through class attributes
        
        pw.println("  };");
        
    }
    /**
     * Write a function that decodes binary data into a javascript object
     * @param pw
     * @param aClass 
     */
    private void writeDecoder(PrintWriter pw, GeneratedClass aClass)
    {
        
        List classHierarchy = new ArrayList();
        GeneratedClass otherClass = aClass;
        do
        {
            classHierarchy.add(otherClass);
            if(otherClass.getParentClass().equalsIgnoreCase("root"))
                break;
            String pClassName = otherClass.getParentClass();
            otherClass = (GeneratedClass)classDescriptions.get(pClassName);
        }
        while(true);
        
        List allAttributes = new ArrayList();
        for(int jdx = classHierarchy.size() -1; jdx >= 0; jdx--)
        {
            GeneratedClass thisLevel = (GeneratedClass)classHierarchy.get(jdx);
            List ivars = thisLevel.getClassAttributes();
            allAttributes.addAll(ivars);
        }
                
        //pw.println("  this.initFromBinary = function(inputStream)");
        pw.println("  " + namespace + "." + aClass.getName() + ".prototype.initFromBinary = function(inputStream)");
        pw.println("  {");
        
        for(int idx = 0; idx < allAttributes.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)allAttributes.get(idx);
            
            // Write out a method call to deserialize a primitive type
            if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE)
            {
                String marshalType = unmarshalTypes.getProperty(anAttribute.getType());
                String capped = this.initialCap(marshalType);
                
                if(marshalType.equalsIgnoreCase("UnsignedByte"))
                    pw.println("       this." + anAttribute.getName() + " = inputStream.readUByte();");
                else if (marshalType.equalsIgnoreCase("UnsignedShort"))
                    pw.println("       this." + anAttribute.getName() + " = inputStream.readUShort();");
				else if(marshalType.equalsIgnoreCase("UnsignedLong"))
					pw.println("       this." + anAttribute.getName() + " = inputStream.readLong" + "();"); // ^^^This is wrong; need to read unsigned here
                else
                    pw.println("       this." + anAttribute.getName() + " = inputStream.read" + capped + "();");
                pw.flush();
            }
            
            // Write out a method call to deserialize a class.
            if( anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.CLASSREF)
            {
                String marshalType = anAttribute.getType();
                
                pw.println("       this." + anAttribute.getName() + ".initFromBinary(inputStream);" );
            }
            
                
            // Write out the method call to unmarshal a fixed length list, aka an array.
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.FIXED_LIST) )
            {
               
                
                pw.println("       for(var idx = 0; idx < " + anAttribute.getListLength() + "; idx++)");
                pw.println("       {");
                
                if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                {
                    String marshalType = unmarshalTypes.getProperty(anAttribute.getType());
                    String capped = this.initialCap(marshalType);
                
                    pw.println("          this." + anAttribute.getName() + "[ idx ] = inputStream.read" + capped + "();");
                }
                else if(anAttribute.listIsClass() == true) 
                {
                    pw.println("          this." + anAttribute.getName() + "[ idx ].initFromBinary(inputStream);");
                }
                
                pw.println("       }");
            }
            
            // Variable length list
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.VARIABLE_LIST) )
            {
                pw.println("       for(var idx = 0; idx < this." + anAttribute.getCountFieldName() + "; idx++)");
                pw.println("       {");
                
                // This is some sleaze. We're an array, but an array of what? We could be either a
                // primitive or a class. We need to figure out which. This is done via the expedient
                // but not very reliable way of trying to do a lookup on the type. If we don't find
                // it in our map of primitives to marshal types, we assume it is a class.
                
                String marshalType = marshalTypes.getProperty(anAttribute.getType());
                
                if(marshalType == null) // It's a class
                {
                    pw.println("           var anX = new " + namespace + "." + anAttribute.getType() + "();");
                    pw.println("           anX.initFromBinary(inputStream);");
                    pw.println("           this." + anAttribute.getName() + ".push(anX);");
                }
                else // It's a primitive
                {
                    String capped = this.initialCap(marshalType);
                    pw.println("           inputStream.read" + capped + "(" + anAttribute.getName() + ");");
                }
                pw.println("       }");
                pw.println();
            } // end of unmarshalling a variable list
        }
            
        pw.println("  };");
    }
    
    
    private void writeClassComments(PrintWriter pw, GeneratedClass aClass)
    {
         // Print class comments header
         pw.println("/**");
         if(aClass.getClassComments() != null)
          {
              pw.println(" * " + aClass.getClassComments());
              pw.println(" *");
          }
         
 
        pw.println(" * Copyright (c) 2008-2015, MOVES Institute, Naval Postgraduate School. All rights reserved.");
        pw.println(" * This work is licensed under the BSD open source license, available at https://www.movesinstitute.org/licenses/bsd.html");
        pw.println(" *");
        pw.println(" * @author DMcG");
        pw.println(" */");
    }
    
    /**
     * Writes the class declaration, including any inheritence and interfaces
     * 
     * @param pw
     * @param aClass
     */
    private void writeClassDeclaration(PrintWriter pw, GeneratedClass aClass)
    {
        pw.println("// On the client side, support for a  namespace.");
        pw.println("if (typeof " + namespace +  " === \"undefined\")\n " +  namespace + " = {};\n");
        pw.println();
        pw.println("// Support for node.js style modules. Ignored if used in a client context.");
        pw.println("// See http://howtonode.org/creating-custom-modules");
        pw.println("if (typeof exports === \"undefined\")\n exports = {};\n");
        pw.println();
        // Class declaration
         pw.println(namespace + "." + aClass.getName() + " = function()");
         pw.println("{");
    }
    
    /**
     * Some fields have integers with bit fields defined, eg an integer where 
     * bits 0-2 represent some value, while bits 3-4 represent another value, 
     * and so on. This writes accessor and mutator methods for those fields.
     * 
     * @param pw
     * @param aClass 
     */
    public void writeFlagMethods(PrintWriter pw, GeneratedClass aClass)
    {
        List attributes = aClass.getClassAttributes();
                
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)attributes.get(idx);

            switch(anAttribute.getAttributeKind())
            {
                
                // Anything with bitfields _must_ be a primitive type
                case PRIMITIVE:
                    
                    List bitfields = anAttribute.bitFieldList;
   
                    for(int jdx = 0; jdx < bitfields.size(); jdx++)
                    {
                        BitField bitfield = (BitField)bitfields.get(jdx);
                        String capped = this.initialCap(bitfield.name);
                        int shiftBits = this.getBitsToShift(anAttribute, bitfield.mask);
                        
                        // write getter
                        pw.println();
                        if(bitfield.comment != null)
                        {
                            pw.println("/** " + bitfield.comment + " */");
                        }
                        pw.println("" + namespace + "." + aClass.getName() + ".prototype.get" + capped + " = function()");
                        pw.println("{");
                        
                        pw.println("   var val = this." + bitfield.parentAttribute.getName() + " & " + bitfield.mask);
                        pw.println("   return val >> " + shiftBits);
                        pw.println("};");

                        pw.println();
                        
                        // Write the setter/mutator
                        
                        pw.println();
                        if(bitfield.comment != null)
                        {
                            pw.println("/** " + bitfield.comment +  " */");
                        }
                        pw.println("" + namespace + "." + aClass.getName() + ".prototype.set" + capped + "= function(val)");
                        pw.println("{");
                        pw.println("  var aVal = 0");
                        pw.println("  this." + bitfield.parentAttribute.getName() + " &= ~" + bitfield.mask + "; // Zero existing bits");
                        pw.println("  val = val << " + shiftBits);
                        pw.println("  this." + bitfield.parentAttribute.getName() + " = this." + bitfield.parentAttribute.getName() + " | val; " );
                        pw.println("};");
                        //pw.println(INDENT + INDENT + bitfield.parentAttribute.getName() + " = val & ~" + mask);
                        pw.println();
                    }
                    
                    break;
                    
                default:
                    bitfields = anAttribute.bitFieldList;
                    if(!bitfields.isEmpty())
                    {
                        System.out.println("Attempted to use bit flags on a non-primitive field");
                        System.out.println( "Field: " + anAttribute.getName() );
                    }
            }
        
        }
    }
    
    private void writeIvars(PrintWriter pw, GeneratedClass aClass)
    {
        //List ivars = aClass.getClassAttributes();
        List classHierarchy = new ArrayList();
        GeneratedClass otherClass = aClass;
        do
        {
            classHierarchy.add(otherClass);
            if(otherClass.getParentClass().equalsIgnoreCase("root"))
                break;
            String pClassName = otherClass.getParentClass();
            otherClass = (GeneratedClass)classDescriptions.get(pClassName);
        }
        while(true);
       
        
        for(int jdx = classHierarchy.size() -1; jdx >= 0; jdx--)
        {
            GeneratedClass thisLevel = (GeneratedClass)classHierarchy.get(jdx);
            List ivars = thisLevel.getClassAttributes();
        
        for(int idx = 0; idx < ivars.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)ivars.get(idx);
            
            // This attribute is a primitive. 
            if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE)
            {
                // The primitive type--we need to do a lookup from the abstract type in the
                // xml to the javascript-specific type.
                
                String attributeType = types.getProperty(anAttribute.getType());
                if(anAttribute.getComment() != null)
                {
                    pw.println("   /** " + anAttribute.getComment() + " */");
                }
               
                pw.print("   this." + anAttribute.getName() + " = ");
                
                String iv = this.getInitialValue(anAttribute.getName(), classHierarchy);
                if(iv == null)
                {
                    iv = anAttribute.getDefaultValue();
                    if(iv == null)
                        pw.print("0");
                    else
                        pw.print(iv);
                }
                else
                {
                    pw.print(iv);
                }
                pw.println(";\n");
            } // end of primitive attribute type
            
            // this attribute is a reference to another class defined in the XML document, The output should look like
            //
            // /** This is a comment */
            // protected AClass foo = new AClass();
            //
            if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.CLASSREF)
            {
                String attributeType = anAttribute.getType();
                if(anAttribute.getComment() != null)
                {
                    pw.println("   /** " + anAttribute.getComment() + " */");
                }
                
                pw.println( "   this." + anAttribute.getName() + " = new " + namespace + "." + attributeType + "(); \n");
            }
        
            // The attribute is a fixed list, ie an array of some type--maybe primitve, maybe a class.
            
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.FIXED_LIST) )
            {
                String attributeType = anAttribute.getType();
                int listLength = anAttribute.getListLength();
                String listLengthString = (new Integer(listLength)).toString();
                
                if(anAttribute.getComment() != null)
                {
                    pw.println("   /** " + anAttribute.getComment() + " */");
                    pw.print("   this." + anAttribute.getName() + " = new Array(");
                    for(int kdx = 0; kdx < anAttribute.getListLength(); kdx++)
                    {
                        pw.print("0");
                        if(kdx < anAttribute.getListLength() - 1)
                            pw.print(", ");
                    }
                    pw.println( ");\n");
                }
                
        
                if(anAttribute.listIsClass() == true) 
                {
                    pw.println("   this." + anAttribute.getName() + " = new Array();\n");
                }
            }
            
            // The attribute is a variable list of some kind. 
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.VARIABLE_LIST) )
            {
                String attributeType = anAttribute.getType();
                int listLength = anAttribute.getListLength();
                String listLengthString = (new Integer(listLength)).toString();
                
                if(anAttribute.getComment() != null)
                {
                    pw.println("   /** " + anAttribute.getComment() + " */");
                }
                
                pw.println("    this." + anAttribute.getName() + " = new Array();\n ");
            }
        } // End of loop through ivars
        } // end of loop through class hierarchy stack
    }
    
  private String getInitialValue(String attributeName, List classHierarchy)
  {
      boolean log=false;
      if(attributeName.equals("protocolVersion"))
      {
         log = true;
      }
      
      
      // Start looking at the lowest level of the class hierarchy and
      // work up.
      for(int idx = 0; idx < classHierarchy.size(); idx++)
      {
          GeneratedClass aClass = (GeneratedClass)classHierarchy.get(idx);
          if(log) System.out.println("Class name: " + aClass.getName());
          
          List initialValues = aClass.getInitialValues();
          for(int jdx = 0; jdx < initialValues.size(); jdx++)
          {
              InitialValue aVal = (InitialValue)initialValues.get(jdx);
              if(aVal.getVariable().equalsIgnoreCase(attributeName))
              {
                  if(log) System.out.println("Variable name:" + aVal.getVariable());

                  if(log) System.out.println("found match in " + aClass.getName() + ":" + aVal.getVariable());

                  return aVal.getVariableValue();
              }
              
           }
          
      }
      return null;
  }


}
