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
package com.ecyrd.jspwiki.content;

import java.io.*;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.activation.MimetypesFileTypeMap;

import net.sourceforge.stripes.util.Base64;

import org.apache.commons.lang.StringEscapeUtils;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.acl.AclEntry;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Exports the contents of the wiki in a JCR System Tree format.
 *  
 *  UUIDs that this class generates are based on the jspwiki application
 *  name set in jspwiki.properties, and the name of the page.  This means
 *  that it is possible to get collisions, if you have two wikis with
 *  the same appname.
 *  <p>
 *  The exported WikiSpace name is always "main".  You can edit the XML
 *  file by hand if you wish to import it to a different wikispace.
 */
public class Exporter
{
    private WikiEngine m_engine;
    private PrintWriter m_out;
    private boolean     m_verbose = false;
    
    private MimetypesFileTypeMap m_mimeTypes = new MimetypesFileTypeMap();
    
    private TreeSet<String>   m_exportedPageTitles = new TreeSet<String>();
    
    private static final String NS_JSPWIKI = "http://www.jspwiki.org/ns#";
    private static final String STRING = "String";
    private static final String JSPWIKI_CONTENT_TYPE = "text/x-wiki.jspwiki";
    private static final String NAME   = "Name";
    private static final String BINARY = "Binary";
    private static final String DATE   = "Date";
    
    private SimpleDateFormat m_isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     *  Create a new wiki exporter.
     *  
     *  @param engine The WikiEngine to export
     *  @param outStream The stream to which the XML data should be written.
     * 
     *  @throws UnsupportedEncodingException If your platform does not support UTF-8
     */
    public Exporter(WikiEngine engine, OutputStream outStream, boolean verbose) throws UnsupportedEncodingException
    {
        m_engine = engine;
        m_out = new PrintWriter( new OutputStreamWriter(outStream,"UTF-8") );
        m_verbose = verbose;
    }
    
    /**
     *  Exports the entire repository.
     *  
     *  @throws ProviderException 
     *  @throws IOException 
     */
    public void export() throws ProviderException, IOException
    {
        Collection<WikiPage> allPages = m_engine.getPageManager().getAllPages();
        
        
        m_out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //  Some comments
        
        m_out.println("<!--");
        m_out.println("This is an JSR-170 -compliant Document Tree export of a jspwiki 2.8 repository.\n"+
                      "It is meant to be imported to the /pages/ node of the JCR repository, as it\n"+
                      "describes an entire wiki space.");
        m_out.println("-->");
        
        //  Page storage node
        m_out.println("<sv:node xmlns:jcr='http://www.jcp.org/jcr/1.0' " + 
                      "xmlns:nt='http://www.jcp.org/jcr/nt/1.0' " + 
                      "xmlns:mix='http://www.jcp.org/jcr/mix/1.0' "+ 
                      "xmlns:sv='http://www.jcp.org/jcr/sv/1.0' "+ 
                      "xmlns:wiki='"+NS_JSPWIKI+"'\n"+ 
                      "         sv:name='main'>" );
        
        for( WikiPage p : allPages )
        {
            exportPage( p );
        }
        
        m_out.println("</sv:node> <!-- EOF -->");
        
        m_out.flush();
    }
    
    private void exportProperty( String name, String value, String type )
    {
        m_out.println("  <sv:property sv:name='"+StringEscapeUtils.escapeXml(name)+"' sv:type='"+type+"'>");
        m_out.print("    <sv:value>");
        m_out.print( StringEscapeUtils.escapeXml( value ) );
        m_out.println("</sv:value>");
        m_out.println("  </sv:property>");
        
    }
    
    private void exportProperty( String name, String[] values, String type )
    {
        m_out.println("  <sv:property sv:name='"+StringEscapeUtils.escapeXml(name)+"' sv:type='"+type+"'>");
        for( String value : values )
        {
            m_out.print("    <sv:value>");
            m_out.print( StringEscapeUtils.escapeXml( value ) );
            m_out.println("</sv:value>");
        }
        m_out.println("  </sv:property>");        
    }
    
    /**
     *  The generated UUID of a page is based on the Wiki name and the
     *  page name (or path).
     *  
     *  @param p
     *  @return
     *  @throws IOException
     */
    private String mkUuid( WikiPage p ) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        out.write( p.getWiki().getBytes("UTF-8") );
        out.write( p.getName().getBytes("UTF-8") );
        
        return UUID.nameUUIDFromBytes( out.toByteArray() ).toString();
    }
    
    private String guessMimeType( WikiPage p )
    {
        if( p instanceof Attachment )
            return m_mimeTypes.getContentType( ((Attachment)p).getFileName() );
        
        return JSPWIKI_CONTENT_TYPE;
    }
    
    protected void exportPage( WikiPage p ) throws IOException, ProviderException
    {
        String title = p.getName();
        
        if( title.contains( "/" ) && !(p instanceof Attachment) )
        {
            title = title.replace( '/', '_' );
            System.err.println("Page '"+p.getName()+"' will be renamed to '"+title+"', as it contains an illegal character.");
        }
     
        title = title.toLowerCase();
        
        String tryTitle = title;
        int idx = 1;
        while( m_exportedPageTitles.contains( tryTitle ) )
        {
            tryTitle = title + "-" + idx++;
        }
        
        if( !tryTitle.equals( title ) )
        {
            System.err.println( "New case independence rules state that page '"+p.getName()+"' will be renamed to '"+tryTitle+"', as there is a conflict already with a page with a similar title." );
            title = tryTitle;
        }
        
        m_exportedPageTitles.add( title );
        
        if( m_verbose )
            System.out.println("Exporting "+title);
        
        exportCommonHeader(title, p);

        Map<String,Object> attrMap = p.getAttributes();
        
        for( Map.Entry<String, Object> e : attrMap.entrySet() )
        {
            if( e.getKey().equals( WikiPage.CHANGENOTE ) )
                exportProperty( "wiki:changeNote", (String)e.getValue(), STRING );
            else
                exportProperty( e.getKey(), e.getValue().toString(), STRING );
        }
        
        //
        //  ACLs
        //
        
        Acl acl = p.getAcl();
        
        if( acl != null )
        {
            ArrayList<String> propval = new ArrayList<String>();
            for( Enumeration<AclEntry> ee = acl.entries(); ee.hasMoreElements(); )
            {
                AclEntry ae = ee.nextElement();
                
                StringBuilder sb = new StringBuilder();
                
                sb.append("ALLOW \"");
                sb.append(ae.getPrincipal().getName());
                sb.append("\" ");
                
                for( Enumeration<Permission> permissions = ae.permissions(); permissions.hasMoreElements(); )
                {
                    Permission perm = permissions.nextElement();
                    
                    sb.append( perm.getActions() );
                    sb.append( "," );
                }
                
                propval.add( sb.toString() );
            }
            
            exportProperty("wiki:acl", propval.toArray( new String[propval.size()] ), STRING);
        }
        
        //
        //  Export page content
        //
        
        exportProperty( "wiki:content", m_engine.getPureText( p ), STRING );
        
        //
        //  Finally, list attachment.  According to JCR rules, these must be last.
        //
        /*
        Collection<Attachment> atts = m_engine.getAttachmentManager().listAttachments( p );
        
        for( Attachment a : atts )
        {
            exportPage( a );
        }
        */
        m_out.println(" </sv:node>");
        
        m_out.flush();
    }
    
    private void exportCommonHeader( String title, WikiPage p ) throws IOException
    {
        m_out.println(" <sv:node sv:name='"+StringEscapeUtils.escapeXml( title )+"'>");
        
        exportProperty( "jcr:primaryType", "nt:unstructured", NAME );
        exportProperty( "jcr:mixinTypes", 
                        new String[] {"mix:referenceable","mix:versionable","mix:lockable"}, 
                        NAME );
        exportProperty( "wiki:author", p.getAuthor(), STRING );
        exportProperty( "jcr:uuid", mkUuid(p), STRING); 
   
        exportProperty( "wiki:lastModified", m_isoFormat.format(p.getLastModified()), DATE );

        exportProperty( "wiki:contentType", guessMimeType( p ), STRING );
        
        exportProperty( "wiki:title", p.getName(), STRING );
    }
    
    protected void exportPage( Attachment att ) throws IOException, ProviderException
    {
        exportCommonHeader(att.getName().toLowerCase(), att);
        
        m_out.println("  <sv:property sv:name='"+att.getFileName()+"' sv:type='"+BINARY+"'>");
        
        m_out.print("<sv:value>");
        
        InputStream binary = m_engine.getAttachmentManager().getAttachmentStream( att );
       
        Base64.InputStream base64 = new Base64.InputStream(binary, Base64.ENCODE);
        
        FileUtil.copyContents( new InputStreamReader(base64), m_out );
        
        binary.close();
        
        m_out.println("</sv:value>");
        m_out.println("</sv:property>");
        
        m_out.println(" </sv:node>");
        
        m_out.flush();  
    }
    
    // FIXME: Would be useful if this actually had some options checking.
    public static void main( String[] argv ) throws IOException
    {
        if( argv.length < 2 )
        {
            System.err.println("Usage: com.ecyrd.jspwiki.content.Exporter <path to jspwiki.properties> <filename>");
            System.exit( 1 );
        }
        
        String propFile = argv[0];
        String outFile  = argv[1];
        
        Properties props = new Properties();
        try
        {
            props.load( new FileInputStream(propFile) );
        }
        catch( FileNotFoundException e )
        {
            System.err.println("Property file not found: "+propFile);
            System.exit(2);
        }
        catch( IOException e )
        {
            System.err.println("Unable to read properties: "+propFile);
            System.exit(2);
        }

        OutputStream out = null;
        
        try
        {
            out = new BufferedOutputStream( new FileOutputStream(outFile) );
            WikiEngine engine = new WikiEngine(props);
            
            Exporter x = new Exporter(engine, out, true );
            
            x.export();
        }
        catch( WikiException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace(System.err);
            System.exit(3);
        }
        catch( IOException e )
        {
            e.printStackTrace(System.err);
            System.exit(3);
        }
        finally
        {
            if( out != null ) out.close();
            
            // Make sure JSPWiki dies too
            System.exit(0);
        }
    }
}
