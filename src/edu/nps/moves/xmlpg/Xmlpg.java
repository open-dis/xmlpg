package edu.nps.moves.xmlpg;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * A class that reads an XML file in a specific format, and spits out a Java, C#,
 * Objective-C, or C++ classes that do <i>most</i> of the work of the protocol.<p>
 *
 * This can rely on properties set in the XML file for the language. For example,
 * the Java element in the XML file can specify whether Hibernate or JAXB support
 * is included in the generated code.<p>
 * 
 * There is a huge risk of using variable names that have ambiguous meaning
 * here, as many of the terms such as "class" are also used
 * by java or c++. <p>
 *
 * In effect this is reading an XML file and creating an abstract description of
 * the protocol data units. That abstract description is written out as source
 * code in various languages, such as C++, Java, etc.
 *
 * @author DMcG
 */
public class Xmlpg 
{
    /** Contains the database of all the classes described by the XML document */
    protected HashMap generatedClassNames = new HashMap();
    
    /** The language types we generate */
    public enum LanguageType {CPP, JAVA, CSHARP, OBJECTIVEC, JAVASCRIPT, PYTHON, SCHEMA }
    
    /** As we parse the XML document, this is the class we are currently working on */
    private GeneratedClass currentGeneratedClass = null;
    
    /** As we parse the XML document, this is the current attribute */
    private ClassAttribute currentClassAttribute = null;
    
    // The languages may have language-specific properties, such as libraries that they
    // depend on. Each language has its own set of properties.
    
    /** Java properties--imports, packages, etc. */
    Properties javaProperties = new Properties();
    
    /** C++ properties--includes, etc. */
    Properties cppProperties = new Properties();
    
    /** C# properties--using, namespace, etc. */
    Properties csharpProperties = new Properties();

    /** Objective-C properties */
    Properties objcProperties = new Properties();
    
    /** Javascript properties */
    Properties javascriptProperties = new Properties();

    /** source code generation options */
    Properties sourceGenerationOptions;
    
    /** source code generation for python */
    Properties pythonProperties = new Properties();

    /** source code generation for schema */
    Properties schemaProperties = new Properties();

    /** Hash table of all the primitive types we can use (short, long, byte, etc.)*/
    private HashSet primitiveTypes = new HashSet();
    
    /** Directory in which the java class package is created */
    private String javaDirectory = null;
    
    /** Directory in which the C++ classes are created */
    private String cppDirectory = null;
    
    //PES
	/** Directory in which the C# classes are created */
	private String csharpDirectory = null;

    /** Director in which the objc classes are created */
    private String objcDirectory = null;
    
    private int classCount = 0;
   
    /**
     * Create a new collection of Java objects by reading an XML file; these
     * java objects can be used to generate code templates of any language,
     * once you write the translator.
     */
    public Xmlpg(String xmlDescriptionFileName, 
                 String languageToGenerate)
    {
        // Which languages to generate
        boolean generateJava = false, generateCpp=false, generateCs=false, generateObjc=false, generatePython= false;
        LanguageType toGenerate = null;

        if(languageToGenerate.equalsIgnoreCase("java"))
        {
            toGenerate = Xmlpg.LanguageType.JAVA;
        }
        else if(languageToGenerate.equalsIgnoreCase("cpp"))
        {
            toGenerate = Xmlpg.LanguageType.CPP;
        }
        else if(languageToGenerate.equalsIgnoreCase("objc"))
        {
            toGenerate = Xmlpg.LanguageType.OBJECTIVEC;
        }
        else if(languageToGenerate.equalsIgnoreCase("csharp"))
        {
            toGenerate = Xmlpg.LanguageType.CSHARP;
        }
        else if(languageToGenerate.equalsIgnoreCase("javascript"))
        {
            toGenerate = Xmlpg.LanguageType.JAVASCRIPT;
        }
        else if(languageToGenerate.equalsIgnoreCase("python"))
        {
            toGenerate = Xmlpg.LanguageType.PYTHON;
        }
        else if(languageToGenerate.equalsIgnoreCase("schema"))
        {
            toGenerate = Xmlpg.LanguageType.SCHEMA;
        }

        Properties sourceGenerationOptions = new Properties();

        try
        {
            DefaultHandler handler = new MyHandler();
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.newSAXParser().parse(new File(xmlDescriptionFileName), handler);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        
        Iterator iterator = generatedClassNames.values().iterator();
       
        // This does at least a cursory santity check on the data that has been read in from XML
        // It is far from complete.
        if(!this.astIsPlausible())
        {
            System.out.println("The generated XML file is not internally consistent according to astIsPlausible()");
            System.out.println("There are one or more errors in the XML file. See output for details.");
            System.exit(1);
        }
        
        //System.out.println("putting java files in " + javaDirectory);
        
        // Create a new generator object to write out the source code for all the classes in java
        if(toGenerate == Xmlpg.LanguageType.JAVA)
        {
            JavaGenerator javaGenerator = new JavaGenerator(generatedClassNames, javaProperties );
            javaGenerator.writeClasses();
        }
        
        // Use the same information to generate classes in C++
        if(toGenerate == Xmlpg.LanguageType.CPP)
        {
            CppGenerator cppGenerator = new CppGenerator(generatedClassNames, cppProperties);
            cppGenerator.writeClasses();
        }
        
        if(toGenerate == Xmlpg.LanguageType.CSHARP)
        {
            // Create a new generator object to write out the source code for all the classes in csharp
            CsharpGenerator csharpGenerator = new CsharpGenerator(generatedClassNames, csharpProperties);
            csharpGenerator.writeClasses();
        }

        if(toGenerate == Xmlpg.LanguageType.OBJECTIVEC)
        {
            // create a new generator object for objc
            ObjcGenerator objcGenerator = new ObjcGenerator(generatedClassNames, objcProperties);
            objcGenerator.writeClasses();
        }
        
        if(toGenerate == Xmlpg.LanguageType.JAVASCRIPT)
        {
            // create a new generator object for javascript
            JavascriptGenerator javascriptGenerator = new JavascriptGenerator(generatedClassNames, javascriptProperties);
            javascriptGenerator.writeClasses();
        }
        
        if(toGenerate == Xmlpg.LanguageType.PYTHON)
        {
            // create a new generator object for Python
            PythonGenerator pythonGenerator = new PythonGenerator(generatedClassNames, pythonProperties);
            pythonGenerator.writeClasses();
        }

        if(toGenerate == Xmlpg.LanguageType.SCHEMA)
        {
            // create a new generator object for Python
            SchemaGenerator schemaGenerator = new SchemaGenerator(generatedClassNames, schemaProperties);
            schemaGenerator.writeClasses();
        }
    }
    
    /**
     * entry point. Pass in two arguments, the language you want to generate for and the XML file
     * that describes the classes
     */
    public static void main(String args[])
    {
        String language = null;

        Properties props = System.getProperties();
        //props.list(System.out);
       
        if(args.length < 2 || args.length > 2)
        {
            System.out.println("Usage: Xmlpg xmlFile language"); 
            System.out.println("Allowable languages are java, cpp, objc, python, schema and csharp");
            System.exit(0);
        }
        
        Xmlpg.preflightArgs(args[0], args[1]);
        
	Xmlpg gen = new Xmlpg(args[0], args[1]);   
    } // end of main
    
    /** 
     * Does a sanity check on the args passed in: does the XML file exist, and is
     * the language valid.
     */
    public static void preflightArgs(String xmlFile, String language)
    {
        try 
        {
            FileInputStream fis = new FileInputStream(xmlFile);
            fis.close();
            
            if(!(language.equalsIgnoreCase("java") || language.equalsIgnoreCase("cpp") ||
               language.equalsIgnoreCase("objc") || language.equalsIgnoreCase("csharp") ||
               language.equalsIgnoreCase("javascript") || language.equalsIgnoreCase("python") ||
               language.equalsIgnoreCase("schema") ))
            {
                System.out.println("Not a valid language to generate. The options are java, cpp, objc, javascript, python, schema and csharp");
                System.out.println("Usage: Xmlpg xmlFile language"); 
                System.exit(0);
            }
        }
        catch (FileNotFoundException fnfe) 
        {
            System.out.println("XML file " + xmlFile + " not found. Please check the path and try again");
            System.out.println("Usage: Xmlpg xmlFile language"); 
            System.exit(0);
        }
        catch(Exception e)
        {
            System.out.println("Problem with arguments to Xmlpg. Please check them.");
            System.out.println("Usage: Xmlpg xmlFile language"); 

            System.exit(0);
        }
        
        
    }
    
    /**
     * Returns true if the information parsed from the protocol description XML file
     * is "plausible" in addition to being syntactically correct. This means that:
     * <ul>
     * <li>references to other classes in the file are correct; if a class attribute
     * refers to a "EntityID", there's a class by that name elsewhere in the document;
     * <li> The primitive types belong to a list of known correct primitive types,
     * eg short, unsigned short, etc
     *
     * AST is a reference to "abstract syntax tree", which this really isn't, but
     * sort of is.
     */
    private boolean astIsPlausible()
    {
        
        // Create a list of primitive types we can use to check against
        primitiveTypes.add("byte");
        primitiveTypes.add("short");
        primitiveTypes.add("int");
        primitiveTypes.add("long");
        primitiveTypes.add("unsigned byte");
        primitiveTypes.add("unsigned short");
        primitiveTypes.add("unsigned int");
		primitiveTypes.add("unsigned long");
        primitiveTypes.add("float");
        primitiveTypes.add("double");
        
        // trip through every class specified
        Iterator iterator = generatedClassNames.values().iterator();
        while(iterator.hasNext())
        {
            GeneratedClass aClass = (GeneratedClass)iterator.next();
            
            // Trip through every class attribute in this class and confirm that the type is either a primitive or references
            // another class defined in the document.
            List attribs = aClass.getClassAttributes();
            for(int idx = 0; idx < attribs.size(); idx++)
            {
                ClassAttribute anAttribute = (ClassAttribute)attribs.get(idx);
                
                ClassAttribute.ClassAttributeType kindOfNode = anAttribute.getAttributeKind();
                
                // The primitive type is on the known list of primitives.
                if(kindOfNode == ClassAttribute.ClassAttributeType.PRIMITIVE)
                {
                    if(primitiveTypes.contains(anAttribute.getType()) == false)
                    {
                        System.out.println("Cannot find a primitive type of " + anAttribute.getType() + " in class " + aClass.getName());
                        return false;
                    }
                }
            
                // The class referenced is available elsewehere in the document
                if(kindOfNode == ClassAttribute.ClassAttributeType.CLASSREF)
                {
                    if(generatedClassNames.get(anAttribute.getType()) == null)
                    {
                        System.out.println("Makes reference to a class of name " + anAttribute.getType() + " in class " + aClass.getName() + " but no user-defined class of that type can be found in the document");
                        return false;
                    }
                    
                }
            } // end of trip through one class' attributes
            
            // Run through the list of initial values, ensuring that the initial values mentioned actually exist as attributes
            // somewhere up the inheritance chain.
            
            List initialValues = aClass.getInitialValues();
                       
            for(int idx = 0; idx < initialValues.size(); idx++)
            {
                InitialValue anInitialValue = (InitialValue)initialValues.get(idx);
                GeneratedClass currentClass = aClass;
                boolean found = false;
                
                //System.out.println("----Looking for matches of inital value " + anInitialValue.getVariable());
                while(currentClass != null)
                {
                    List thisClassesAttributes = currentClass.getClassAttributes();
                    for(int jdx = 0; jdx < thisClassesAttributes.size(); jdx++)
                    {
                        ClassAttribute anAttribute = (ClassAttribute)thisClassesAttributes.get(jdx);
                        //System.out.println("--checking " + anAttribute.getName() + " against inital value " + anInitialValue.getVariable());
                        if(anInitialValue.getVariable().equals(anAttribute.getName()))
                        {
                            found = true;
                            break;
                        }
                    }
                    currentClass = (GeneratedClass)generatedClassNames.get(currentClass.getParentClass());
                }
                if(!found)
                {
                    System.out.println("Could not find initial value matching attribute name for " + anInitialValue.getVariable() + " in class " + aClass.getName());
                }
                    
                    
                    
            } // end of for loop thorugh initial values

        } // End of trip through classes
        
        return true;
    } // end of astIsPlausible
    
    /**
     * inner class that handles the SAX parsing of the XML file. This is relatively simnple, if
     * a little verbose. Basically we just create the appropriate objects as we come across the
     * XML elements in the file.
     */
    public class MyHandler extends DefaultHandler
    {
        /** We've come across a start element
        */
        public void startElement(String uri, String localName, String qName, Attributes attributes)
        {
            // Lanaguage-specific elements. All the properties needed to generate code specific
            // to a language should be included in the properties list for that language.
            
            // java element--place all the attributes and values into a property list
            if(qName.equalsIgnoreCase("java"))
            {
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    javaProperties.setProperty(attributes.getQName(idx), attributes.getValue(idx));
                }
               // System.out.println("Got java properties of " + javaProperties);
            }
            
            // c++ element--place all the attributes and values into a property list
            if(qName.equalsIgnoreCase("cpp"))
            {
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    cppProperties.setProperty(attributes.getQName(idx), attributes.getValue(idx));
                }
            }

            // C-sharp element--place all the attributes and values into a property list
            if(qName.equalsIgnoreCase("csharp"))
            {
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    csharpProperties.setProperty(attributes.getQName(idx), attributes.getValue(idx));
                }
            }
            
            // javascript element--place all the attributes and values into a property list
            if(qName.equalsIgnoreCase("javascript"))
            {
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    javascriptProperties.setProperty(attributes.getQName(idx), attributes.getValue(idx));
                }
                
                //System.out.println("In parse, javascript properties are " + javascriptProperties);
            }

            // objc element--place all the attributes and values into a property list
            if(qName.equalsIgnoreCase("objc"))
            {
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    objcProperties.setProperty(attributes.getQName(idx), attributes.getValue(idx));
                }
            }
            
            // python element--place all the attributes and values into a property list
            if(qName.equalsIgnoreCase("python"))
            {
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    pythonProperties.setProperty(attributes.getQName(idx), attributes.getValue(idx));
                }
            }

            // schema element--place all the attributes and values into a property list
            if(qName.equalsIgnoreCase("schema"))
            {
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    schemaProperties.setProperty(attributes.getQName(idx), attributes.getValue(idx));
                }
            }

            // We've hit the start of a class element. Pick up the attributes of this (name, and any comments)
            // and then prepare for reading attributes.
            if(qName.compareToIgnoreCase("class") == 0)
            {
               classCount++;
               //System.out.println("classCount is" + classCount);
               
                currentGeneratedClass = new GeneratedClass();
                
                // The default is that this inherits from Object
                currentGeneratedClass.setParentClass("root");
                
                // Trip through all the attributes of the class tag
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    // class name
                    if(attributes.getQName(idx).compareToIgnoreCase("name") == 0)
                    {
                        //System.out.println("--->Processing class named " + attributes.getValue(idx));
                        currentGeneratedClass.setName(attributes.getValue(idx));
                    }
                    
                    // Class comment
                    if(attributes.getQName(idx).compareToIgnoreCase("comment") == 0)
                    {
                        //System.out.println("comment is " + attributes.getValue(idx));
                        currentGeneratedClass.setComment(attributes.getValue(idx));
                    }
                    
                    // Inherits from
                    if(attributes.getQName(idx).compareToIgnoreCase("inheritsFrom") == 0)
                    {
                        //System.out.println("inherits from " + attributes.getValue(idx));
                        currentGeneratedClass.setParentClass(attributes.getValue(idx));
                    }
                    
                     // XML root element--used for marshalling to XML with JAXB
                    if(attributes.getQName(idx).equalsIgnoreCase("xmlRootElement"))
                    {
                        //System.out.println("is root element " + attributes.getValue(idx));
                        if(attributes.getValue(idx).equalsIgnoreCase("true"))
                        {
                            currentGeneratedClass.setXmlRootElement(true);
                        }
                        // by default it is false unless specified otherwise
                            
                    }
                    
                }
            }
            
            // We've hit an initial value element. This is used to initialize attributes in the
            // constructor. 
            if(qName.equalsIgnoreCase("initialValue"))
            {
                String anAttributeName = null;
                String anInitialValue = null;
                
                // Attributes on the initial value tag
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    // Name of class attribute
                    if(attributes.getQName(idx).compareToIgnoreCase("name") == 0)
                    {
                        anAttributeName = attributes.getValue(idx);
                        //System.out.println("   in attribute " + attributes.getValue(idx));
                    }
                    
                    // Initial value
                    if(attributes.getQName(idx).compareToIgnoreCase("value") == 0)
                    {
                        anInitialValue = attributes.getValue(idx);
                    }
                }
                
                if((anAttributeName != null) && (anInitialValue != null))
                {
                    InitialValue aValue = new InitialValue(anAttributeName, anInitialValue);
                    currentGeneratedClass.addInitialValue(aValue);
                    //System.out.println("---Added intial value named " + anAttributeName + " in class " + currentGeneratedClass.getName());
                }
                
                
                
            }
            
            // We've hit an Attribute element. Read in the value, then the attributes associated
            // with it (name and comments).
            if(qName.compareToIgnoreCase("attribute") == 0)
            {
                currentClassAttribute = new ClassAttribute();
                
                // Attributes on the attribute tag.
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    // Name of class attribute
                    if(attributes.getQName(idx).compareToIgnoreCase("name") == 0)
                    {
                        //System.out.println("    in attribute " + attributes.getValue(idx));
                        currentClassAttribute.setName(attributes.getValue(idx));
                    }
                    
                    // Comment on class attribute
                    if(attributes.getQName(idx).compareToIgnoreCase("comment") == 0)
                    {
                        //System.out.println("        attribute comment:" + attributes.getValue(idx));
                        currentClassAttribute.setComment(attributes.getValue(idx));
                    }
                    
                    if(attributes.getQName(idx).compareToIgnoreCase("serialize") == 0)
                    {
                        String shouldSerialize = attributes.getValue(idx);
                        if(shouldSerialize.equalsIgnoreCase("false"))
                        {
                            currentClassAttribute.shouldSerialize=false;
                        }
                    }
                }
            }
            
            // We've hit a flag element, add that to the attribute.
            if(qName.compareToIgnoreCase("flag") == 0)
            {
                
                String flagName=null,  flagComment=null;
                String flagMask = "0";
                
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    // Name of class attribute
                    if(attributes.getQName(idx).compareToIgnoreCase("name") == 0)
                    {
                        flagName = attributes.getValue(idx);
                    }
                    
                    // comment of class attribute
                    if(attributes.getQName(idx).compareToIgnoreCase("comment") == 0)
                    {
                        flagComment = attributes.getValue(idx);
                    }
                    
                    if(attributes.getQName(idx).compareToIgnoreCase("mask") == 0)
                    {
                        // Should parse "0x80" or "31" equally well.
                        String text = attributes.getValue(idx);
                        flagMask = text;
                    }
                } // end of loop through attribtes of flag
                
                BitField bitField = new BitField(flagName, flagMask, flagComment, currentClassAttribute);
                currentClassAttribute.bitFieldList.add(bitField);
                
            } // end of flag element
            
            // We've hit a primitive description type. This may be either a simple primitive, or
            // nested inside of a list element. To trap this situation we check to see if the
            // attribute kind (primitive, classRef, list) has already been set. If so, we
            // leave it alone.
            
            if(qName.compareToIgnoreCase("primitive") == 0)
            {
               if(currentClassAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.UNSET)
               {
                   currentClassAttribute.setAttributeKind(ClassAttribute.ClassAttributeType.PRIMITIVE);
                   currentClassAttribute.setUnderlyingTypeIsPrimitive(true);
               }
               else
               {
                   currentClassAttribute.setUnderlyingTypeIsPrimitive(true);
               }
                
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    if(attributes.getQName(idx).equalsIgnoreCase("type"))
                    {
                        currentClassAttribute.setType(attributes.getValue(idx));
                    }
                    
                    if(attributes.getQName(idx).equalsIgnoreCase("defaultValue"))
                    {
                        currentClassAttribute.setDefaultValue(attributes.getValue(idx));
                    }
                }
            }
            
            // A reference to another class in the same document
            if(qName.compareToIgnoreCase("classRef") == 0)
            {
                // The classref may occur inside a List element; if that's the case, we want to 
                // respect the existing list type.
                if(currentClassAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.UNSET)
                {
                    currentClassAttribute.setAttributeKind(ClassAttribute.ClassAttributeType.CLASSREF);
                    currentClassAttribute.setUnderlyingTypeIsPrimitive(false);
                }
                
                
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    if(attributes.getQName(idx).compareToIgnoreCase("name") == 0)
                    {
                        currentClassAttribute.setType(attributes.getValue(idx));
                    }
                }
            }
            
            // A variable lenght list attribute (a list of some sort).
            if(qName.compareToIgnoreCase("variablelist") == 0)
            {
                currentClassAttribute.setAttributeKind(ClassAttribute.ClassAttributeType.VARIABLE_LIST);
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                    // Variable list length fields require a name of another field that contains how many
                    // there are. This is used in unmarshalling.
                    if(attributes.getQName(idx).equalsIgnoreCase("countFieldName"))
                    {
                        currentClassAttribute.setCountFieldName(attributes.getValue(idx));
                        
                        // We also want to inform the attribute associated with countFieldName that
                        // it is keeping track of a list--this modifies the getter method and
                        // eliminates the setter method. This code assumes that the count field
                        // attribute has already been processed.
                        List ats = currentGeneratedClass.getClassAttributes();
                        boolean atFound = false;
                        
                        for(int jdx = 0; jdx < ats.size(); jdx++)
                        {
                            ClassAttribute at = (ClassAttribute)ats.get(jdx);
                            if(at.getName().equals(attributes.getValue(idx)))
                            {
                                at.setIsDynamicListLengthField(true);
                                at.setDynamicListClassAttribute(currentClassAttribute);
                                atFound = true;
                                break;
                            }
                        }
                        
                        if(atFound == false)
                        {
                            System.out.println("Could not find a matching attribute for the length field for " + attributes.getValue(idx));
                        }
                        
                    }
                    
                }
            }
                
            
            // A list element, of either fixed length (generally an array) or variable length (a list of some sort).
            if(qName.compareToIgnoreCase("fixedlist") == 0)
            {
                currentClassAttribute.setAttributeKind(ClassAttribute.ClassAttributeType.FIXED_LIST);
                
                for(int idx = 0; idx < attributes.getLength(); idx++)
                {
                  
                    if(attributes.getQName(idx).equalsIgnoreCase("couldBeString"))
                    {
                        String val = attributes.getValue(idx);
                        
                        if(val.equalsIgnoreCase("true"))
                           {
                              currentClassAttribute.setCouldBeString(true);  
                           }
                    }
                    
                    
                    if(attributes.getQName(idx).equalsIgnoreCase("length"))
                    {
                        String length = attributes.getValue(idx);
                        
                        try
                        {
                           int listLen = Integer.parseInt(length);
                            currentClassAttribute.setListLength(listLen);
                        }
                        catch(Exception e)
                        {
                            System.out.println("Invalid list length found. Bad format for integer " + length);
                            currentClassAttribute.setListLength(0);
                        }
                    } // end of attribute length
                } // End of element list
            }
           
        } // end of startElement
        
        public void endElement(String uri, String localName, String qName) 
        {
            // We've reached the end of a class element. The class should be complete; add it to the hash table.
            if(qName.compareToIgnoreCase("class") == 0)
            {
                classCount--;
                //System.out.println("classCount is " + classCount);
                //System.out.println("---#End of class" + currentGeneratedClass.getName());
                generatedClassNames.put(currentGeneratedClass.getName(), currentGeneratedClass);
            }
            
            // Reached the end on an attribute. Add the attribute to whatever the current class is.
            if(qName.compareToIgnoreCase("attribute") == 0)
            {
                //System.out.println("     end attribute");
                currentGeneratedClass.addClassAttribute(currentClassAttribute);
            }
        }
    }

}
