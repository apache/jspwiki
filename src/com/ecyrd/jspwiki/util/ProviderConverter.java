/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.AbstractFileProvider;
import com.ecyrd.jspwiki.providers.VersioningFileProvider;
import com.ecyrd.jspwiki.providers.WikiPageProvider;

/**
 *  A command-line application that allows you to convert from
 *  one provider to another.  Currently this only supports
 *  conversion from RCSFileProvider to VersioningFileProvider.
 *  <p>
 *  This class is mostly a hack, so do not trust it very much.
 *  It leaves the converted directory in /tmp/converter-tmp/ 
 *  and does not touch the original in any way.
 *  
 *  @author jalkanen
 *
 *  @since
 */
public class ProviderConverter
{
    private String m_rcsSourceDir;
    
    protected void setRCSSourceDir( String dir )
    {
        m_rcsSourceDir = dir;
    }

    private static final String[] WINDOWS_DEVICE_NAMES =
    {
        "con", "prn", "nul", "aux", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9"
    };
    
    protected String mangleName( String pagename )
    {
        pagename = TextUtil.urlEncode( pagename, "UTF-8" );
        
        pagename = TextUtil.replaceString( pagename, "/", "%2F" );

        if( SystemUtils.IS_OS_WINDOWS )
        {
            String pn = pagename.toLowerCase();
            for( int i = 0; i < WINDOWS_DEVICE_NAMES.length; i++ )
            {
                if( WINDOWS_DEVICE_NAMES[i].equals(pn) )
                {
                    pagename = "$$$" + pagename;
                }
            }
        }
        
        return pagename;
    }

    protected void convert()
        throws WikiException, IOException
    {
        Properties props = new Properties();
        
        props.setProperty( WikiEngine.PROP_APPNAME, "JSPWikiConvert" );
        props.setProperty( AbstractFileProvider.PROP_PAGEDIR, m_rcsSourceDir );
        props.setProperty( PageManager.PROP_PAGEPROVIDER, "RCSFileProvider" );
        props.setProperty( PageManager.PROP_USECACHE, "false" );
        props.setProperty( "log4j.appender.outlog", "org.apache.log4j.ConsoleAppender" );
        props.setProperty( "log4j.appender.outlog.layout", "org.apache.log4j.PatternLayout" );
        props.setProperty( "jspwiki.useLucene", "false" );
        props.setProperty( "log4j.rootCategory", "INFO,outlog" );
        WikiEngine engine = new WikiEngine( props );

        WikiPageProvider sourceProvider = engine.getPageManager().getProvider();
        
        File tmpDir = new File( SystemUtils.JAVA_IO_TMPDIR, "converter-tmp" );
        
        props.setProperty( AbstractFileProvider.PROP_PAGEDIR, tmpDir.getAbsolutePath() );
        WikiPageProvider destProvider = new VersioningFileProvider();
        
        destProvider.initialize( engine, props );
        
        Collection allPages = sourceProvider.getAllPages();
        
        int idx = 1;
        
        for( Iterator i = allPages.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage)i.next();
            
            System.out.println("Converting page: "+p.getName()+" ("+idx+"/"+allPages.size()+")");
            Collection pageHistory = engine.getVersionHistory( p.getName() );
            
            for( Iterator v = pageHistory.iterator(); v.hasNext(); )
            {
                WikiPage pv = (WikiPage)v.next();
                
                String text = engine.getPureText( pv.getName(), pv.getVersion() );
                
                destProvider.putPageText( pv, text );
            }
            
            //
            //  Do manual setting now
            //
            
            for( Iterator v = pageHistory.iterator(); v.hasNext(); )
            {
                WikiPage pv = (WikiPage)v.next();

                File f = new File( tmpDir, "OLD" );
                f = new File( f, mangleName(pv.getName()) );
                f = new File( f, pv.getVersion()+".txt" );
                
                System.out.println("   Setting old version "+pv.getVersion()+" to date "+pv.getLastModified() );
                f.setLastModified( pv.getLastModified().getTime() );
            }
            
            idx++;
        }
        
        tmpDir.delete();
    }
    
    /**
     * @param args
     */
    public static void main( String[] args )
        throws Exception
    {
        ProviderConverter c = new ProviderConverter();
        
        c.setRCSSourceDir( args[0] );
        
        c.convert();
    }

}
