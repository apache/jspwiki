/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import net.sourceforge.stripes.mock.MockServletContext;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageAlreadyExistsException;

/**
 *  Imports an XML file from the Export routines.
 */
// FIXME: Needs to be localized.
public final class Import
{
    /**
     *  Private constructor to prevent instantiation.
     */
    private Import()
    {}
    
    private static void usage()
    {
        System.out.println("Usage: java "+Import.class.getName()+" -p <propertyfile> <xmlfile>");
        System.exit( 1 );
    }
    
    /**
     *  Finds a parameter from given string array. Known formats:
     *  [-<param> parameter] and [-<param>parameter].
     *  
     *  @param params
     *  @param option Option switch string (e.g. "-p"). If null, finds a optionless parameter.
     *  @return Parameter string, if it exists. Empty string, if there was the switch but no param. null, if there was no switch.
     */
    private static String getArg(String[] params, String option)
    {
        for( int i = 0; i < params.length; i++ )
        {
            if( option == null )
            {
                // Skip next.
                if( params[i].startsWith("-") )
                {
                    ++i;
                }
                else
                {
                    return params[i];
                }
            }
            else if( params[i].equals(option) )
            {
                if( i < params.length-1 )
                {
                    return params[i+1];
                }
                else if( params[i+1].startsWith("-") )
                {
                    return "";
                }
                else
                {
                    return "";
                }
            }
            else if( params[i].startsWith(option) )
            {
                String opt = params[i].substring( option.length() );
                
                return opt;
            }
        }
        
        return null;
    }
    
    /**
     *  Main entry point.
     *  
     * @param argv
     * @throws FileNotFoundException
     * @throws IOException
     * @throws WikiException
     * @throws LoginException
     * @throws RepositoryException
     * @throws InterruptedException
     * @throws PageAlreadyExistsException
     */
    // FIXME: Exception handling is vague at best.
    public static void main(String[] argv) throws FileNotFoundException, IOException, WikiException, LoginException, RepositoryException, InterruptedException, PageAlreadyExistsException
    {
        if( argv.length < 2 )
            usage();

        String propfile = getArg(argv,"-p");
        if( propfile == null ) usage();

        System.out.println("Using propertyfile '"+propfile+"' to configure JSPWiki");

        String xmlfile = getArg(argv,null);
        if( xmlfile == null ) usage();
        
        System.out.println("Reading contents of repository from '"+xmlfile+"'");
        
        Properties props = new CommentedProperties();
        try
        {
            props.load( new FileInputStream(propfile) );
        }
        catch( IOException e) 
        {
            System.err.println("Could not open property file "+e.getMessage());
            System.exit(5);
        }
        
        WikiEngine engine = null;
        try
        {
            engine = WikiEngine.getInstance( new MockServletContext("JSPWiki"), props );
        }
        catch( Exception e )
        {
            System.err.println("Error starting JSPWiki: "+e.getMessage());
            e.printStackTrace( System.err );
            System.exit(5);
        }

        Thread.sleep(10); // Workaround for JSPWIKI-610 for now.
        
        try
        {
            ContentManager mgr = engine.getContentManager();
    
            mgr.importXML( xmlfile );
        }
        finally
        {
            engine.shutdown();
        }
        
        System.out.println("Importing completed.");
    }
}
