package edu.nps.moves.xmlpg;

import java.io.*;
import java.util.*;

/**
 * Abstract superclass for all the concrete language generators, such as java, c++, etc.
 *
 * @author DMcG
 */

public abstract class Generator 
{
    /** Contains abstract descriptions of all the classes, key = name, value = object
    */
    protected HashMap classDescriptions;
    
    /** Directory in which to write the class code */
    public String  directory;
    
    protected Properties languageProperties;
    
    /**
     * Constructor
     */
    public Generator(HashMap pClassDescriptions, Properties pLanguageProperties)
    {
        classDescriptions = pClassDescriptions;
        languageProperties = pLanguageProperties;

        // Directory is set in the subclasses

        /*
        try
        {
            directory = (String)languageProperties.getProperty("directory");
        }
        catch(Exception e)
        {
            System.out.println("Missing language property, probably the directory in which the source code should be placed");
            System.out.println("add directory = aDir in the properties for the language");
            System.out.println(e);
        }
         
         */
    }
    
    /**
     * Overridden by the subclasses to generate the code specific to that language.
     */
    public abstract void writeClasses();
    
    /**
     * Create the directory in which to put the generated source code files
     */
    protected void createDirectory()
    {
        System.out.println("creating directory");
        System.out.println("directory=" + this.getDirectory());
        boolean success = (new File(this.getDirectory())).mkdirs();
        
    }

    /**
     * Directory in which to write the class code
     * @return the directory
     */
    public String getDirectory()
    {
        return directory;
    }

    /**
     * Directory in which to write the class code
     * @param directory the directory to set
     */
    public void setDirectory(String directory)
    {
        this.directory = directory;
    }
    
    /** 
     * returns a string with the first letter capitalized. 
     */
    public String initialCap(String aString)
    {
        StringBuffer stb = new StringBuffer(aString);
        stb.setCharAt(0, Character.toUpperCase(aString.charAt(0)));
        
        return new String(stb);
    }
    /**
     * returns a string with the first letter lower case.
     */
    public String initialLower(String aString)
    {
        StringBuffer stb = new StringBuffer(aString);
        stb.setCharAt(0, Character.toLowerCase(aString.charAt(0)));

        return new String(stb);
    }
    
    /** This is ugly and brute force, but I don't see an easier way to do it.
     * Given a mask (like 0xf0) we want to know how many bits to shift an
     * integer when masking in a new value. 
     * @param anAttribute
     * @param mask
     * @return 
     */
    protected int getBitsToShift(ClassAttribute anAttribute, String mask)
    {
        String fieldType = anAttribute.getType();
        
       int intMask = Integer.decode(mask);
       
       int maxBits = 0;
       if(fieldType.equalsIgnoreCase("unsigned byte") || fieldType.equalsIgnoreCase("byte"))
       {
           maxBits = 8;
       }
       else if(fieldType.equalsIgnoreCase("unsigned short") || fieldType.equalsIgnoreCase("short"))
       {
           maxBits = 16;
       }
       else if(fieldType.equalsIgnoreCase("unsigned int") || fieldType.equalsIgnoreCase("int"))
       {
           maxBits = 32;
       }
       else if(fieldType.equalsIgnoreCase("unsigned long") || fieldType.equalsIgnoreCase("long"))
       {
           maxBits = 64;
       }
       else if(fieldType.equalsIgnoreCase("float") ) // Bit flags in a float field? Shouldn't happen, but be careful
       {
           maxBits = 32;
       }
       else if(fieldType.equalsIgnoreCase("double") ) // ditto
       {
           maxBits = 64;
       }
       
       int count = 0, startBit = 0, endBit = 0;
       boolean started = false, ended = false;
       
       if(maxBits == 0)
           return 0;
       
       for(int idx = 0; idx < maxBits; idx++)
       {
           int result = intMask & 0x1;
           if(result == 1)
           {
               if(!started)
               {
                    started = true;
                    startBit = idx;
               }
           }
           
           if( (result == 0) && (started == true) )
           {
                 endBit = idx -1;
                 ended = true;
                 break;
           }
                       
           if( (started == true) && (result == 1) && (idx == maxBits-1))
           {
               endBit = idx;
               ended = true;
               break;
           }
           
          // Zero-fill the left-most slot when shifting. Otherwise a sign 
          // can result in a 1 in the leftmost slot
          intMask = intMask >>> 1;
           
       }    // end of loop through bits   
             
       
       return startBit;
        
    }
    
    

}
