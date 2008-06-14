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
package com.ecyrd.jspwiki.providers;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import org.apache.log4j.Logger;
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
 *  <p>
 *  If you decide to dabble with the default commands, please make sure
 *  that you do not check the default archive suffix ",v".  File deletion
 *  depends on it.
 */
// FIXME: Not all commands read their format from the property file yet.
public class RCSFileProvider
    extends AbstractFileProvider
{
    private String m_checkinCommand  = "ci -m\"author=%u;changenote=%c\" -l -t-none %s";
    private String m_checkoutCommand = "co -l %s";
    private String m_logCommand      = "rlog -zLT -r %s";
    private String m_fullLogCommand  = "rlog -zLT %s";
    private String m_checkoutVersionCommand = "co -p -r1.%v %s";
    private String m_deleteVersionCommand = "rcs -o1.%v %s";

    private static final Logger   log = Logger.getLogger(RCSFileProvider.class);

    /** Property name for the checkin command.  Value is <tt>{@value}</tt>. */
    public static final String    PROP_CHECKIN  = "jspwiki.rcsFileProvider.checkinCommand";
    
    /** Property name for the checkout command.  Value is <tt>{@value}</tt>. */
    public static final String    PROP_CHECKOUT = "jspwiki.rcsFileProvider.checkoutCommand";
    
    /** Property name for the log command.  Value is <tt>{@value}</tt>. */
    public static final String    PROP_LOG      = "jspwiki.rcsFileProvider.logCommand";
    
    /** Property name for the full log command.  Value is <tt>{@value}</tt>. */
    public static final String    PROP_FULLLOG  = "jspwiki.rcsFileProvider.fullLogCommand";
    
    /** Property name for the checkout version command.  Value is <tt>{@value}</tt>. */
    public static final String    PROP_CHECKOUTVERSION = "jspwiki.rcsFileProvider.checkoutVersionCommand";

    private static final String   PATTERN_DATE      = "^date:\\s*(.*\\d);";
    private static final String   PATTERN_AUTHOR    = "^\"?author=([\\w\\.\\s\\+\\.\\%]*)\"?";
    private static final String   PATTERN_CHANGENOTE= ";changenote=([\\w\\.\\s\\+\\.\\%]*)\"?";
    private static final String   PATTERN_REVISION  = "^revision \\d+\\.(\\d+)";

    private static final String   RCSFMT_DATE       = "yyyy-MM-dd HH:mm:ss";
    private static final String   RCSFMT_DATE_UTC   = "yyyy/MM/dd HH:mm:ss";

    // Date format parsers, placed here to save on object creation
    private SimpleDateFormat m_rcsdatefmt     = new SimpleDateFormat( RCSFMT_DATE );
    private SimpleDateFormat m_rcsdatefmtUTC = new SimpleDateFormat( RCSFMT_DATE_UTC );

    /**
     *  {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties props )
        throws NoRequiredPropertyException,
               IOException
    {
        log.debug("Initing RCS");
        super.initialize( engine, props );

        m_checkinCommand = props.getProperty( PROP_CHECKIN, m_checkinCommand );
        m_checkoutCommand = props.getProperty( PROP_CHECKOUT, m_checkoutCommand );
        m_logCommand     = props.getProperty( PROP_LOG, m_logCommand );
        m_fullLogCommand = props.getProperty( PROP_FULLLOG, m_fullLogCommand );
        m_checkoutVersionCommand = props.getProperty( PROP_CHECKOUTVERSION, m_checkoutVersionCommand );

        File rcsdir = new File( getPageDirectory(), "RCS" );

        if( !rcsdir.exists() )
        {
            rcsdir.mkdirs();
        }

        log.debug("checkin="+m_checkinCommand);
        log.debug("checkout="+m_checkoutCommand);
        log.debug("log="+m_logCommand);
        log.debug("fulllog="+m_fullLogCommand);
        log.debug("checkoutversion="+m_checkoutVersionCommand);
    }

    /**
     *  {@inheritDoc}
     */
    // NB: This is a very slow method.

    public WikiPage getPageInfo( String page, int version )
        throws ProviderException
    {
        PatternMatcher  matcher  = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();
        BufferedReader  stdout   = null;

        WikiPage info = super.getPageInfo( page, version );

        if( info == null ) return null;

        try
        {
            String   cmd = m_fullLogCommand;

            cmd = TextUtil.replaceString( cmd, "%s", mangleName(page)+FILE_EXT );

            Process process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );

            // FIXME: Should this use encoding as well?
            stdout = new BufferedReader( new InputStreamReader(process.getInputStream() ) );

            String line;
            Pattern headpattern = compiler.compile( PATTERN_REVISION );
            // This complicated pattern is required, since on Linux RCS adds
            // quotation marks, but on Windows, it does not.
            Pattern userpattern = compiler.compile( PATTERN_AUTHOR );
            Pattern datepattern = compiler.compile( PATTERN_DATE );
            Pattern notepattern = compiler.compile( PATTERN_CHANGENOTE );

            boolean found = false;

            while( (line = stdout.readLine()) != null )
            {
                if( matcher.contains( line, headpattern ) )
                {
                    MatchResult result = matcher.getMatch();

                    try
                    {
                        int vernum = Integer.parseInt( result.group(1) );

                        if( vernum == version || version == WikiPageProvider.LATEST_VERSION )
                        {
                            info.setVersion( vernum );
                            found = true;
                        }
                    }
                    catch( NumberFormatException e )
                    {
                        log.info("Failed to parse version number from RCS log: ",e);
                        // Just continue reading through
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
                else if( found && line.startsWith("----")  )
                {
                    // End of line sign from RCS
                    break;
                }

                if( found && matcher.contains( line, userpattern ) )
                {
                    MatchResult result = matcher.getMatch();
                    info.setAuthor( TextUtil.urlDecodeUTF8(result.group(1)) );
                }

                if( found && matcher.contains( line, notepattern ) )
                {
                    MatchResult result = matcher.getMatch();

                    info.setAttribute( WikiPage.CHANGENOTE, TextUtil.urlDecodeUTF8(result.group(1)) );
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

            // we must close all by exec(..) opened streams: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
            process.getInputStream().close();
            process.getOutputStream().close();
            process.getErrorStream().close();

        }
        catch( Exception e )
        {
            // This also occurs when 'info' was null.
            log.warn("Failed to read RCS info",e);
        }
        finally
        {
            try
            {
                if( stdout != null ) stdout.close();
            }
            catch( IOException e ) {}
        }

        return info;
    }

    /**
     *  {@inheritDoc}
     */
    public String getPageText( String page, int version )
        throws ProviderException
    {
        String result = null;
        InputStream stdout = null;
        BufferedReader stderr = null;
        Process process = null;

        // Let parent handle latest fetches, since the FileSystemProvider
        // can do the file reading just as well.

        if( version == WikiPageProvider.LATEST_VERSION )
            return super.getPageText( page, version );

        log.debug("Fetching specific version "+version+" of page "+page);

        try
        {
            PatternMatcher  matcher           = new Perl5Matcher();
            PatternCompiler compiler          = new Perl5Compiler();
            int             checkedOutVersion = -1;
            String          line;
            String          cmd               = m_checkoutVersionCommand;

            cmd = TextUtil.replaceString( cmd, "%s", mangleName(page)+FILE_EXT );
            cmd = TextUtil.replaceString( cmd, "%v", Integer.toString(version ) );

            log.debug("Command = '"+cmd+"'");

            process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );
            stdout = process.getInputStream();
            result = FileUtil.readContents( stdout, m_encoding );

            stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            Pattern headpattern = compiler.compile( PATTERN_REVISION );

            while( (line = stderr.readLine()) != null )
            {
                if( matcher.contains( line, headpattern ) )
                {
                    MatchResult mr = matcher.getMatch();
                    checkedOutVersion = Integer.parseInt( mr.group(1) );
                }
            }

            process.waitFor();

            int exitVal = process.exitValue();

            log.debug("Done, returned = "+exitVal);

            //
            //  If fetching failed, assume that this is because of the user
            //  has just migrated from FileSystemProvider, and check
            //  if he's getting version 1.  Else he might be trying to find
            //  a version that has been deleted.
            //
            if( exitVal != 0 || checkedOutVersion == -1 )
            {
                if( version == 1 )
                {
                    result = super.getPageText( page, WikiProvider.LATEST_VERSION );
                }
                else
                {
                    throw new NoSuchVersionException( "Page: "+page+", version="+version);
                }
            }
            else
            {
                //
                //  Check which version we actually got out!
                //

                if( checkedOutVersion != version )
                {
                    throw new NoSuchVersionException( "Page: "+page+", version="+version);
                }
            }

        }
        catch( MalformedPatternException e )
        {
            throw new InternalWikiException("Malformed pattern in RCSFileProvider!");
        }
        catch( InterruptedException e )
        {
            // This is fine, we'll just log it.
            log.info("RCS process was interrupted, we'll just return whatever we found.");
        }
        catch( IOException e )
        {
            log.error("RCS checkout failed",e);
        }
        finally
        {
            try
            {
                // we must close all by exec(..) opened streams: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
                if( stdout != null ) stdout.close();
                if( stderr != null ) stderr.close();
                if( process != null ) process.getInputStream().close();
            }
            catch( Exception e )
            {
                log.error("Unable to close streams!");
            }
        }

        return result;
    }

    /**
     *  Puts the page into RCS and makes sure there is a fresh copy in
     *  the directory as well.
     *  
     *  @param page {@inheritDoc}
     *  @param text {@inheritDoc}
     *  @throws {@inheritDoc}
     */
    public void putPageText( WikiPage page, String text )
        throws ProviderException
    {
        Process process = null;
        String pagename = page.getName();
        // Writes it in the dir.
        super.putPageText( page, text );

        log.debug( "Checking in text..." );

        try
        {
            String cmd = m_checkinCommand;

            String author = page.getAuthor();
            if( author == null ) author = "unknown"; // Should be localized but cannot due to missing WikiContext

            String changenote = (String)page.getAttribute(WikiPage.CHANGENOTE);
            if( changenote == null ) changenote = "";

            cmd = TextUtil.replaceString( cmd, "%s", mangleName(pagename)+FILE_EXT );
            cmd = TextUtil.replaceString( cmd, "%u", TextUtil.urlEncodeUTF8(author) );
            cmd = TextUtil.replaceString( cmd, "%c", TextUtil.urlEncodeUTF8(changenote) );
            log.debug("Command = '"+cmd+"'");

            process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );

            process.waitFor();

            //
            //  Collect possible error output
            //
            BufferedReader error = null;
            String elines = "";
            error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = null;
            while ((line = error.readLine()) != null)
            {
                elines = elines + line +"\n";
            }

            log.debug("Done, returned = "+process.exitValue());
            log.debug(elines);
            if (process.exitValue() != 0)
            {
                throw new ProviderException(cmd+"\n"+"Done, returned = "+process.exitValue()+"\n"+elines);
            }
        }
        catch( Exception e )
        {
            log.error("RCS checkin failed",e);
            ProviderException pe = new ProviderException("RCS checkin failed");
            pe.initCause(e);
            throw pe;
        }
        finally
        {
            // we must close all by exec(..) opened streams: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
            if (process != null)
            {
                try
                {
                    if( process.getOutputStream() != null ) process.getOutputStream().close();
                }
                catch( Exception e ) {}
                try
                {
                    if( process.getInputStream() != null ) process.getInputStream().close();
                }
                catch( Exception e ) {}
                try
                {
                    if( process.getErrorStream() != null ) process.getErrorStream().close();
                }
                catch( Exception e ) {}
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    // FIXME: Put the rcs date formats into properties as well.
    public List getVersionHistory( String page )
    {
        PatternMatcher matcher = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();
        BufferedReader stdout  = null;

        log.debug("Getting RCS version history");

        ArrayList<WikiPage> list = new ArrayList<WikiPage>();

        try
        {
            Pattern revpattern  = compiler.compile( PATTERN_REVISION );
            Pattern datepattern = compiler.compile( PATTERN_DATE );
            // This complicated pattern is required, since on Linux RCS adds
            // quotation marks, but on Windows, it does not.
            Pattern userpattern = compiler.compile( PATTERN_AUTHOR );

            Pattern notepattern = compiler.compile( PATTERN_CHANGENOTE );

            String cmd = TextUtil.replaceString( m_fullLogCommand,
                                                 "%s",
                                                 mangleName(page)+FILE_EXT );

            Process process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );

            // FIXME: Should this use encoding as well?
            stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );

            String line;

            WikiPage info = null;

            while( (line = stdout.readLine()) != null )
            {
                if( matcher.contains( line, revpattern ) )
                {
                    info = new WikiPage( m_engine, page );

                    MatchResult result = matcher.getMatch();

                    int vernum = Integer.parseInt( result.group(1) );
                    info.setVersion( vernum );

                    list.add( info );
                }

                if( matcher.contains( line, datepattern ) && info != null )
                {
                    MatchResult result = matcher.getMatch();

                    Date d = parseDate( result.group(1) );

                    info.setLastModified( d );
                }

                if( matcher.contains( line, userpattern ) && info != null )
                {
                    MatchResult result = matcher.getMatch();

                    info.setAuthor( TextUtil.urlDecodeUTF8(result.group(1)) );
                }

                if( matcher.contains( line, notepattern ) && info != null )
                {
                    MatchResult result = matcher.getMatch();

                    info.setAttribute( WikiPage.CHANGENOTE, TextUtil.urlDecodeUTF8(result.group(1)) );
                }
            }

            process.waitFor();

            // we must close all by exec(..) opened streams: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
            process.getInputStream().close();
            process.getOutputStream().close();
            process.getErrorStream().close();

            //
            // FIXME: This is very slow
            //
            for( Iterator i = list.iterator(); i.hasNext(); )
            {
                WikiPage p = (WikiPage) i.next();

                String content = getPageText( p.getName(), p.getVersion() );

                p.setSize( content.length() );
            }
        }
        catch( Exception e )
        {
            log.error( "RCS log failed", e );
        }
        finally
        {
            try
            {
                if( stdout != null ) stdout.close();
            }
            catch( IOException e ) {}
        }

        return list;
    }

    /**
     *  Removes the page file and the RCS archive from the repository.
     *  This method assumes that the page archive ends with ",v".
     *  
     *  @param page {@inheritDoc}
     *  @throws {@inheritDoc}
     */
    public void deletePage( String page )
        throws ProviderException
    {
        log.debug( "Deleting page "+page );
        super.deletePage( page );

        File rcsdir  = new File( getPageDirectory(), "RCS" );

        if( rcsdir.exists() && rcsdir.isDirectory() )
        {
            File rcsfile = new File( rcsdir, mangleName(page)+FILE_EXT+",v" );

            if( rcsfile.exists() )
            {
                if( rcsfile.delete() == false )
                {
                    log.warn( "Deletion of RCS file "+rcsfile.getAbsolutePath()+" failed!" );
                }
            }
            else
            {
                log.info( "RCS file does not exist for page: "+page );
            }
        }
        else
        {
            log.info( "No RCS directory at "+rcsdir.getAbsolutePath() );
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void deleteVersion( String page, int version )
    {
        String         line = "<rcs not run>";
        BufferedReader stderr  = null;
        boolean        success = false;
        String         cmd     = m_deleteVersionCommand;

        log.debug("Deleting version "+version+" of page "+page);

        cmd = TextUtil.replaceString( cmd, "%s", mangleName(page)+FILE_EXT );
        cmd = TextUtil.replaceString( cmd, "%v", Integer.toString( version ) );

        log.debug("Running command "+cmd);
        Process process = null;

        try
        {
            process = Runtime.getRuntime().exec( cmd, null, new File(getPageDirectory()) );

            //
            // 'rcs' command outputs to stderr methinks.
            //

            // FIXME: Should this use encoding as well?

            stderr = new BufferedReader( new InputStreamReader(process.getErrorStream() ) );

            while( (line = stderr.readLine()) != null )
            {
                log.debug( "LINE="+line );
                if( line.equals("done") )
                {
                    success = true;
                }
            }
        }
        catch( IOException e )
        {
            log.error("Page deletion failed: ",e);
        }
        finally
        {
            try
            {
                if( stderr != null ) stderr.close();
                if( process != null )
                {
                    process.getInputStream().close();
                    process.getOutputStream().close();
                }
            }
            catch( IOException e )
            {
                log.error("Cannot close streams for process while deleting page version.");
            }
        }

        if( !success )
        {
            log.error("Version deletion failed. Last info from RCS is: "+line);
        }
    }

    /**
     *  util method to parse a date string in Local and UTC formats.  This method is synchronized
     *  because SimpleDateFormat is not thread-safe.
     *
     *  @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335">Sun bug 4228335</a>
     */
    private synchronized Date parseDate( String str )
    {
        Date d = null;

        try
        {
            d = m_rcsdatefmt.parse( str );
            return d;
        }
        catch ( ParseException pe )
        { }

        try
        {
            d = m_rcsdatefmtUTC.parse( str );
            return d;
        }
        catch ( ParseException pe )
        { }

        return d;
    }

    /**
     *  {@inheritDoc}
     */
    public void movePage( String from,
                          String to )
        throws ProviderException
    {
        // XXX: Error checking could be better throughout this method.
        File fromFile = findPage( from );
        File toFile = findPage( to );

        fromFile.renameTo( toFile );

        String fromRCSName = "RCS/"+mangleName( from )+FILE_EXT+",v";
        String toRCSName = "RCS/"+mangleName( to )+FILE_EXT+",v";

        File fromRCSFile = new File( getPageDirectory(), fromRCSName );
        File toRCSFile = new File( getPageDirectory(), toRCSName );

        fromRCSFile.renameTo( toRCSFile );
    }
}
