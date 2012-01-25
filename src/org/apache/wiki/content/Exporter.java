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
package org.apache.wiki.content;

import java.io.*;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.activation.MimetypesFileTypeMap;

import net.sourceforge.stripes.util.Base64;

import org.apache.commons.lang.StringEscapeUtils;

import org.apache.wiki.FileUtil;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiException;
import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AclEntry;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.providers.ProviderException;

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
     *  @param outStream The stream to which the XML data should be written.
     *  @param verbose use verbosity or not
     * 
     *  @throws UnsupportedEncodingException If your platform does not support UTF-8
     */
    public Exporter(OutputStream outStream, boolean verbose) throws UnsupportedEncodingException
    {
        m_out = new PrintWriter( new OutputStreamWriter(outStream,"UTF-8") );
        m_verbose = verbose;
    }
    
    /**
     *  Exports the entire repository using a WikiEngine.
     *  
     *  @throws ProviderException 
     *  @throws IOException 
     */
    public void export(WikiEngine engine) throws ProviderException, IOException
    {
        Collection<WikiPage> allPages = engine.getPageManager().getAllPages();
        
        exportDocumentHeader();
        
        for( WikiPage p : allPages )
        {
            exportPage( engine, p );
        }
        
        exportDocumentFooter();
    }

    protected void export( String dir ) throws IOException
    {
        System.out.println("Exporting a FileSystemProvider/RCSFileProvider/VersioningFileProvider compatible repository.");
        System.out.println("This version does not export attributes, ACLs or attachments. Please use --properties for that.");
        
        File df = new File(dir);
        
        File[] pages = df.listFiles( new FilenameFilter() {

            public boolean accept( File dir, String name )
            {
                return name.endsWith( FileSystemProvider.FILE_EXT );
            }} );
        
        exportDocumentHeader();
        
        for( File f : pages )
        {
            String pageName = f.getName();
            pageName = pageName.replace( ".txt", "" );
            exportPageHeader( pageName, "Main", "TBD", new Date(f.lastModified()), false );
            
            // File content
            
            FileInputStream in = new FileInputStream(f);
            exportProperty( "wiki:content", FileUtil.readContents( in, "UTF-8" ), STRING );
            in.close();
            
            exportPageFooter();
        }
        exportDocumentFooter();
        
        System.out.println("...done");
    }
    
    private void exportDocumentFooter()
    {
        m_out.println("</sv:node> <!-- EOF -->");
        
        m_out.flush();
    }

    private void exportDocumentHeader()
    {
        m_out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //  Some comments
        
        m_out.println("<!--");
        m_out.println("This is an JSR-170 -compliant Document Tree export of a JSPWiki repository.\n"+
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
    private String mkUuid( String wiki, String name ) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        out.write( wiki.getBytes("UTF-8") );
        out.write( name.getBytes("UTF-8") );
        
        return UUID.nameUUIDFromBytes( out.toByteArray() ).toString();
    }
    
    private String guessMimeType( String name, boolean isAttachment )
    {
        if( isAttachment )
            return m_mimeTypes.getContentType( name );
        
        return JSPWIKI_CONTENT_TYPE;
    }
    
    protected void exportPage( WikiEngine engine, WikiPage p ) throws IOException, ProviderException
    {
        String name  = p.getName();
        String title = name;
        boolean isAttachment = p instanceof Attachment;
        
        title = generateTitle( name, title, isAttachment );
        
        exportPageHeader(p.getName(), p.getWiki(), p.getAuthor(), p.getLastModified(), isAttachment);

        Map<String,Object> attrMap = p.getAttributes();
        
        exportAttributes( attrMap );
        
        //
        //  ACLs
        //
        
        Acl acl = p.getAcl();
        
        exportAcl( acl );
        
        //
        //  Export page content
        //
        
        exportProperty( "wiki:content", engine.getPureText( p ), STRING );
        
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
        exportPageFooter();
    }

    private String generateTitle( String name, String title, boolean isAttachment )
    {
        if( title.contains( "/" ) && !isAttachment )
        {
            title = title.replace( '/', '_' );
            System.err.println("Page '"+name+"' will be renamed to '"+title+"', as it contains an illegal character.");
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
            System.err.println( "New case independence rules state that page '"+name+"' will be renamed to '"+tryTitle+"', as there is a conflict already with a page with a similar title." );
            title = tryTitle;
        }
        
        m_exportedPageTitles.add( title );
        
        if( m_verbose )
            System.out.println("Exporting "+title);
        
        return title;
    }

    private void exportPageFooter()
    {
        m_out.println(" </sv:node>");
        
        m_out.flush();
    }

    private void exportAcl( Acl acl )
    {
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
    }

    private void exportAttributes( Map<String, Object> attrMap )
    {
        for( Map.Entry<String, Object> e : attrMap.entrySet() )
        {
            if( e.getKey().equals( WikiPage.CHANGENOTE ) )
                exportProperty( "wiki:changeNote", (String)e.getValue(), STRING );
            else
                exportProperty( e.getKey(), e.getValue().toString(), STRING );
        }
    }
    
    private void exportPageHeader( String title, String wiki, String author, Date lastModified, boolean isAttachment ) throws IOException
    {
        m_out.println(" <sv:node sv:name='"+StringEscapeUtils.escapeXml( title.toLowerCase() )+"'>");
        
        exportProperty( "jcr:primaryType", "nt:unstructured", NAME );
        exportProperty( "jcr:mixinTypes", 
                        new String[] {"mix:referenceable","mix:lockable"}, 
                        NAME );
        exportProperty( "wiki:author", author, STRING );
        exportProperty( "jcr:uuid", mkUuid(wiki,title), STRING); 
   
        exportProperty( "wiki:lastModified", m_isoFormat.format(lastModified), DATE );

        exportProperty( "wiki:contentType", guessMimeType( title, isAttachment ), STRING );
        
        exportProperty( "wiki:title", title, STRING );
    }
    
    protected void exportPage( WikiEngine engine, Attachment att ) throws IOException, ProviderException
    {
        exportPageHeader(att.getName(), att.getWiki(), att.getAuthor(), att.getLastModified(), true);
        
        m_out.println("  <sv:property sv:name='"+att.getFileName()+"' sv:type='"+BINARY+"'>");
        
        m_out.print("<sv:value>");
        
        InputStream binary = engine.getAttachmentManager().getAttachmentStream( att );
       
        Base64.InputStream base64 = new Base64.InputStream(binary, Base64.ENCODE);
        
        FileUtil.copyContents( new InputStreamReader(base64), m_out );
        
        binary.close();
        
        m_out.println("</sv:value>");
        m_out.println("</sv:property>");
        
        exportPageFooter();  
    }
    
    private static final String getParam(String[] s, String p, String defaultParam)
    {
        if( s.length < 2 ) return defaultParam;
        
        if( p == null )
        {
            String p1 = s[s.length-2];
            String p2 = s[s.length-1];
            
            if( p1.startsWith( "-" ) ) return defaultParam;
            
            return p2;
        }
        
        for( int i = 0; i < s.length; i++ )
        {
            if( s[i].equals(p) )
            {
                // Found potential parameter
                
                if( i < s.length-1 )
                {
                    String p2 = s[i+1];
                 
                    // Does the next one start with a "-"?
                    if( p2.startsWith( "-" ) )
                        return "";
                    
                    return p2;
                }
                
                // Last item on list
                return "";
            }
        }
        
        return defaultParam;
    }
    
    private static final void exit()
    {
        System.err.println("Usage: org.apache.wiki.content.Exporter [--properties <path to jspwiki.properties>] [--dir <versioning file provider dir>] <filename>");
        System.exit( 1 );        
    }
    
    // FIXME: Would be useful if this actually had some options checking.
    public static void main( String[] argv ) throws IOException
    {
        String propFile = getParam( argv, "--properties", null);
        String outFile  = getParam( argv, null, null);
        String dir      = getParam( argv, "--dir", null);
        
        if( outFile == null )
        {
            exit();
        }
        
        if( (propFile == null && dir == null) || (propFile != null && dir != null) )
        {
            exit();
        }
        
        if( propFile != null )
        {
            exportWithProperties( propFile, outFile );
        }
        else
        {
            exportWithDir( dir, outFile );
        }
    }

 
    /**
     *  This is a special version of the routine which knows the FileSystemProvider default format.
     *  
     *  @param dir
     *  @param outFile
     *  @throws IOException
     */
    public static void exportWithDir( String dir, String outFile ) throws IOException
    {
        FileOutputStream out = new FileOutputStream(new File(outFile));
        Exporter x = new Exporter( out, true );
        
        x.export( dir );
        
        out.close();
    }
    
    public static void exportWithProperties( String propFile, String outFile ) throws IOException
    {
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
        
            Exporter x = new Exporter(out, true );
        
            x.export(engine);
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
