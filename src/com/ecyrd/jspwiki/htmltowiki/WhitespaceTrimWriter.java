package com.ecyrd.jspwiki.htmltowiki;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Part of the XHtmlToWikiTranslator
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class WhitespaceTrimWriter extends Writer
{

    private StringBuffer m_result = new StringBuffer();

    private StringBuffer m_buffer = new StringBuffer();

    private boolean m_trimMode = true;

    private static final Pattern ONLINE_PATTERN = Pattern.compile( ".*?\\n\\s*?", Pattern.MULTILINE );

    private boolean m_currentlyOnLineBegin = true;

    public void flush()
    {
        if( m_buffer.length() > 0 )
        {
            String s = m_buffer.toString();
            s = s.replaceAll( "\r\n", "\n" );
            if( m_trimMode )
            {
                s = s.replaceAll( "(\\w+) \\[\\?\\|Edit\\.jsp\\?page=\\1\\]", "[$1]" );
                s = s.replaceAll( "\n{2,}", "\n\n" );
                s = s.replaceAll( "\\p{Blank}+", " " );
                s = s.replaceAll( "[ ]*\n[ ]*", "\n" );
                s = replacePluginNewlineBackslashes( s );
            }
            m_result.append( s );
            m_buffer = new StringBuffer();
        }
    }

    private String replacePluginNewlineBackslashes( String s )
    {
        Pattern p = Pattern.compile( "\\{\\{\\{(.*?)\\}\\}\\}|\\{\\{(.*?)\\}\\}|\\[\\{(.*?)\\}\\]", Pattern.DOTALL
                                                                                                    + Pattern.MULTILINE );
        Matcher m = p.matcher( s );
        StringBuffer sb = new StringBuffer();
        while( m.find() )
        {
            String groupEscaped = m.group().replaceAll( "\\\\|\\$", "\\\\$0" );
            if( m.group( 3 ) != null )
            {
                m.appendReplacement( sb, groupEscaped.replaceAll( "\\\\\\\\\\\\\\\\", "\n" ) );
            }
            else
            {
                m.appendReplacement( sb, groupEscaped );
            }
        }
        m.appendTail( sb );
        s = sb.toString();
        return s;
    }

    public boolean isWhitespaceTrimMode()
    {
        return m_trimMode;
    }

    public void setWhitespaceTrimMode( boolean trimMode )
    {
        if( m_trimMode != trimMode )
        {
            flush();
            m_trimMode = trimMode;
        }
    }

    public void write( char[] arg0, int arg1, int arg2 ) throws IOException
    {
        m_buffer.append( arg0, arg1, arg2 );
        m_currentlyOnLineBegin = ONLINE_PATTERN.matcher( m_buffer ).matches();
    }

    public void close() throws IOException
    {}

    public String toString()
    {
        flush();
        return m_result.toString();
    }

    public boolean isCurrentlyOnLineBegin()
    {
        return m_currentlyOnLineBegin;
    }
}
