package com.ecyrd.jspwiki;

import java.util.Properties;
import org.apache.log4j.Category;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 *  This class implements a simple RCS file provider.
 *
 *  @author Janne Jalkanen
 */
public class RCSFileProvider
    extends FileSystemProvider
{
    private String m_checkinCommand  = "ci -q -mx -l -t-none %s";
    private String m_checkoutCommand = "co -l %s";

    private static final Category   log = Category.getInstance(RCSFileProvider.class);

    public static final String    PROP_CHECKIN  = "jspwiki.rcsFileProvider.checkinCommand";
    public static final String    PROP_CHECKOUT = "jspwiki.rcsFileProvider.checkoutCommand";

    public void initialize( Properties props )
        throws NoRequiredPropertyException
    {
        log.debug("Initing RCS");
        super.initialize( props );

        m_checkinCommand = props.getProperty( PROP_CHECKIN, m_checkinCommand );
        m_checkoutCommand = props.getProperty( PROP_CHECKOUT, m_checkoutCommand );

        File rcsdir = new File( getPageDirectory(), "RCS" );

        if( !rcsdir.exists() )
            rcsdir.mkdirs();

        log.info("checkin="+m_checkinCommand);
        log.info("checkout="+m_checkoutCommand);
    }

    public WikiPage getPageInfo( String page )
    {
        WikiPage info = super.getPageInfo( page );

        try
        {
            String cmd = "rlog -h "+page+FILE_EXT;
            String[] env = new String[0];
            log.debug("Command = '"+cmd+"'");

            Process process = Runtime.getRuntime().exec( cmd, env, new File(getPageDirectory()) );

            BufferedReader stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );

            String line;

            // FIXME: Use ORO for this, too.
            while( (line = stdout.readLine()) != null )
            {
                if( line.startsWith( "head:" ) )
                {
                    int cutpoint = line.lastIndexOf('.');

                    String version = line.substring( cutpoint+1 );

                    int vernum = Integer.parseInt( version );

                    info.setVersion( vernum );

                    break;
                }
            }

            process.waitFor();

        }
        catch( Exception e )
        {
            log.warn("Failed to read RCS info",e);
        }

        return info;
    }

    public String getPageText( String page, int version )
    {
        StringBuffer result = new StringBuffer();

        log.debug("Fetching specific version "+version+" of page "+page);
        try
        {
            String cmd = m_checkinCommand;
            String[] env = new String[0];

            cmd = "co -p -r1."+version+" "+page+FILE_EXT;

            log.debug("Command = '"+cmd+"'");

            Process process = Runtime.getRuntime().exec( cmd, env, new File(getPageDirectory()) );

            BufferedReader stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );

            String line;

            while( (line = stdout.readLine()) != null )
            { 
                result.append( line+"\n");
            }            

            process.waitFor();

            log.debug("Done, returned = "+process.exitValue());
        }
        catch( Exception e )
        {
            log.error("RCS checkin failed",e);
        }
        
        return result.toString();
    }

    /**
     *  Puts the page into RCS and makes sure there is a fresh copy in
     *  the directory as well.
     */
    public void putPageText( String page, String text )
    {
        // Writes it in the dir.
        super.putPageText( page, text );

        log.debug( "Checking in text..." );

        try
        {
            String cmd = m_checkinCommand;
            String[] env = new String[0];

            cmd = TranslatorReader.replaceString( cmd, "%s", page+FILE_EXT );

            log.debug("Command = '"+cmd+"'");

            Process process = Runtime.getRuntime().exec( cmd, env, new File(getPageDirectory()) );

            process.waitFor();

            log.debug("Done, returned = "+process.exitValue());
        }
        catch( Exception e )
        {
            log.error("RCS checkin failed",e);
        }
    }

    public Collection getVersionHistory( String page )
    {
        PatternMatcher matcher = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();
        PatternMatcherInput input;

        log.debug("Getting RCS version history");

        ArrayList list = new ArrayList();        

        SimpleDateFormat rcsdatefmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        try
        {
            Pattern revpattern  = compiler.compile("^revision \\d+\\.(\\d+)");
            Pattern datepattern = compiler.compile("^date:\\s*(.*);");

            String[] env = new String[0];

            String cmd = "rlog "+page+FILE_EXT;
            
            Process process = Runtime.getRuntime().exec( cmd, env, new File(getPageDirectory()) );

            BufferedReader stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );

            String line;

            WikiPage info = null;

            while( (line = stdout.readLine()) != null )
            { 
                if( matcher.contains( line, revpattern ) )
                {
                    info = new WikiPage( page );

                    MatchResult result = matcher.getMatch();

                    int vernum = Integer.parseInt( result.group(1) );
                    info.setVersion( vernum );
                    list.add( info );
                }

                if( matcher.contains( line, datepattern ) )
                {
                    MatchResult result = matcher.getMatch();

                    Date d = rcsdatefmt.parse( result.group(1) );

                    info.setLastModified( d );
                }
            }

            process.waitFor();

        }
        catch( Exception e )
        {
            log.error( "RCS log failed", e );
        }

        return list;
    }
}
