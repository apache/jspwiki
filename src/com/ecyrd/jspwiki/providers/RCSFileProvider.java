/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.providers;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import org.apache.log4j.Category;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.*;

/**
 *  This class implements a simple RCS file provider.  NOTE: You MUST
 *  have the RCS package installed for this to work.  They must also
 *  be in your path...
 *
 *  <P>
 *  The RCS file provider extends from the FileSystemProvider, which
 *  means that it provides the pages in the same way.  The only difference
 *  is that it implements the version history commands, and also in each
 *  checkin it writes the page to the RCS repository as well.
 *
 *  @author Janne Jalkanen
 */
// FIXME: Not all commands read their format from the property file yet.
public class RCSFileProvider
    extends FileSystemProvider
{
    private String m_checkinCommand  = "ci -q -m\"author=%u\" -l -t-none %s";
    private String m_checkoutCommand = "co -l %s";
    private String m_logCommand      = "rlog -zLT -r %s";
    private String m_fullLogCommand  = "rlog -zLT %s";
    private String m_checkoutVersionCommand = "co -p -r1.%v %s";
    
    private static final Category   log = Category.getInstance(RCSFileProvider.class);

    public static final String    PROP_CHECKIN  = "jspwiki.rcsFileProvider.checkinCommand";
    public static final String    PROP_CHECKOUT = "jspwiki.rcsFileProvider.checkoutCommand";
    public static final String    PROP_LOG      = "jspwiki.rcsFileProvider.logCommand";
    public static final String    PROP_FULLLOG  = "jspwiki.rcsFileProvider.fullLogCommand";
    public static final String    PROP_CHECKOUTVERSION = "jspwiki.rcsFileProvider.checkoutVersionCommand";

    private static final String   PATTERN_DATE      = "^date:\\s*(.*\\d);";
    private static final String   PATTERN_AUTHOR    = "^\"?author=([\\w\\.\\s\\+\\.\\%]*)\"?";
    private static final String   PATTERN_REVISION  = "^revision \\d+\\.(\\d+)";

    private static final String   RCSFMT_DATE       = "yyyy-MM-dd HH:mm:ss";
    private static final String   RCSFMT_DATE_UTC   = "yyyy/MM/dd HH:mm:ss";

    // Date format parsers, placed here to save on object creation
    private SimpleDateFormat m_rcsdatefmt     = new SimpleDateFormat( RCSFMT_DATE );
    private SimpleDateFormat m_rcsdatefmt_utc = new SimpleDateFormat( RCSFMT_DATE_UTC );

    public void initialize( Properties props )
        throws NoRequiredPropertyException,
               IOException
    {
        log.debug("Initing RCS");
        super.initialize( props );

        m_checkinCommand = props.getProperty( PROP_CHECKIN, m_checkinCommand );
        m_checkoutCommand = props.getProperty( PROP_CHECKOUT, m_checkoutCommand );
        m_logCommand     = props.getProperty( PROP_LOG, m_logCommand );
        m_fullLogCommand = props.getProperty( PROP_FULLLOG, m_fullLogCommand );
        m_checkoutVersionCommand = props.getProperty( PROP_CHECKOUTVERSION, m_checkoutVersionCommand );
        
        File rcsdir = new File( getPageDirectory(), "RCS" );

        if( !rcsdir.exists() )
            rcsdir.mkdirs();

        log.debug("checkin="+m_checkinCommand);
        log.debug("checkout="+m_checkoutCommand);
        log.debug("log="+m_logCommand);
        log.debug("fulllog="+m_fullLogCommand);
        log.debug("checkoutversion="+m_checkoutVersionCommand);
    }

    // NB: This is a very slow method.

    public WikiPage getPageInfo( String page, int version )
        throws ProviderException
    {
        PatternMatcher  matcher  = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();
        PatternMatcherInput input;

        WikiPage info = super.getPageInfo( page, version );

        try
        {
            String   cmd = m_fullLogCommand;

            cmd = TextUtil.replaceString( cmd, "%s", mangleName(page)+FILE_EXT );

            Process process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );

            // FIXME: Should this use encoding as well?
            BufferedReader stdout = new BufferedReader( new InputStreamReader(process.getInputStream() ) );

            String line;
            Pattern headpattern = compiler.compile( PATTERN_REVISION );
            // This complicated pattern is required, since on Linux RCS adds
            // quotation marks, but on Windows, it does not.
            Pattern userpattern = compiler.compile( PATTERN_AUTHOR );
            Pattern datepattern = compiler.compile( PATTERN_DATE );
            boolean found = false;

            while( (line = stdout.readLine()) != null )
            {
                if( matcher.contains( line, headpattern ) )
                {                    
                    MatchResult result = matcher.getMatch();
                    int vernum = Integer.parseInt( result.group(1) );

                    if( vernum == version || version == WikiPageProvider.LATEST_VERSION )
                    {
                        info.setVersion( vernum );
                        found = true;
                    }
                }
                else if( matcher.contains( line, datepattern ) && found )
                {
                    MatchResult result = matcher.getMatch();
                    Date d = parseDate( result.group(1) );

                    if( d != null )
                    {
                        info.setLastModified( d );
                    }
                    else
                    {
                        log.info("WikiPage "+info.getName()+
                                 " has null modification date for version "+
                                 version);
                    }
                }
                else if( matcher.contains( line, userpattern ) && found )
                {
                    MatchResult result = matcher.getMatch();
                    info.setAuthor( TextUtil.urlDecodeUTF8(result.group(1)) );
                }
                else if( found && line.startsWith("----")  )
                {
                    // End of line sign from RCS
                    break;
                }
            }

            //
            //  Especially with certain versions of RCS on Windows,
            //  process.waitFor() hangs unless you read all of the
            //  standard output.  So we make sure it's all emptied.
            //

            while( (line = stdout.readLine()) != null ) 
            { 
            }

            process.waitFor();

        }
        catch( Exception e )
        {
            // This also occurs when 'info' was null.
            log.warn("Failed to read RCS info",e);
        }

        return info;
    }

    public String getPageText( String page, int version )
        throws ProviderException
    {
        String result = null;

        // Let parent handle latest fetches, since the FileSystemProvider
        // can do the file reading just as well.

        if( version == WikiPageProvider.LATEST_VERSION )
            return super.getPageText( page, version );

        log.debug("Fetching specific version "+version+" of page "+page);
        try
        {
            String cmd = m_checkoutVersionCommand;

            cmd = TextUtil.replaceString( cmd, "%s", mangleName(page)+FILE_EXT );
            cmd = TextUtil.replaceString( cmd, "%v", Integer.toString(version ) );

            log.debug("Command = '"+cmd+"'");

            Process process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );
            result = FileUtil.readContents( process.getInputStream(),
                                            m_encoding );

            process.waitFor();

            int exitVal = process.exitValue();
            
            log.debug("Done, returned = "+exitVal);

            //
            //  If fetching failed, assume that this is because of the user
            //  has just migrated from FileSystemProvider, and check
            //  if he's getting version 1.
            //
            if( exitVal != 0 && version == 1 )
            {
                result = super.getPageText( page, version );
            }
        }
        catch( Exception e )
        {
            log.error("RCS checkout failed",e);
        }
        
        return result;
    }

    /**
     *  Puts the page into RCS and makes sure there is a fresh copy in
     *  the directory as well.
     */
    public void putPageText( WikiPage page, String text )
    {
        String pagename = page.getName();
        // Writes it in the dir.
        super.putPageText( page, text );

        log.debug( "Checking in text..." );

        try
        {
            String cmd = m_checkinCommand;
            
            String author = page.getAuthor();
            if( author == null ) author = "unknown";

            cmd = TextUtil.replaceString( cmd, "%s", mangleName(pagename)+FILE_EXT );
            cmd = TextUtil.replaceString( cmd, "%u", TextUtil.urlEncodeUTF8(author) );

            log.debug("Command = '"+cmd+"'");

            Process process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );

            process.waitFor();

            log.debug("Done, returned = "+process.exitValue());
        }
        catch( Exception e )
        {
            log.error("RCS checkin failed",e);
        }
    }

    // FIXME: Put the rcs date formats into properties as well.
    public List getVersionHistory( String page )
    {
        PatternMatcher matcher = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();
        PatternMatcherInput input;

        log.debug("Getting RCS version history");

        ArrayList list = new ArrayList();        

        try
        {
            Pattern revpattern  = compiler.compile( PATTERN_REVISION );
            Pattern datepattern = compiler.compile( PATTERN_DATE );
            // This complicated pattern is required, since on Linux RCS adds
            // quotation marks, but on Windows, it does not.
            Pattern userpattern = compiler.compile( PATTERN_AUTHOR );

            String cmd = TextUtil.replaceString( m_fullLogCommand,
                                                 "%s",
                                                 mangleName(page)+FILE_EXT );
            
            Process process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );

            // FIXME: Should this use encoding as well?
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

                    Date d = parseDate( result.group(1) );

                    info.setLastModified( d );
                }

                if( matcher.contains( line, userpattern ) )
                {
                    MatchResult result = matcher.getMatch();

                    info.setAuthor( TextUtil.urlDecodeUTF8(result.group(1)) );
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

    /**
     *  util method to parse a date string in Local and UTC formats
     */
    private Date parseDate( String str )
    {
        Date d = null;

        try
        {
            d = m_rcsdatefmt.parse( str );
            return d;
        }
        catch ( ParseException pe ) { }

        try
        {
            d = m_rcsdatefmt_utc.parse( str );
            return d;
        }
        catch ( ParseException pe ) { }

        return d;
    }
}
