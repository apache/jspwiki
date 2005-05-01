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

    private StringBuffer result = new StringBuffer();

    private StringBuffer buffer = new StringBuffer();

    private boolean trimMode = true;

    private Pattern ps = Pattern.compile( ".*?\\n\\s*?", Pattern.MULTILINE );

    private boolean currentlyOnLineBegin = true;

    public void flush()
    {
        if( buffer.length() > 0 )
        {
            String s = buffer.toString();
            s = s.replaceAll( "\r\n", "\n" );
            if( trimMode )
            {
                s = s.replaceAll( "(\\w+) \\[\\?\\|Edit\\.jsp\\?page=\\1\\]", "[$1]" );
                s = s.replaceAll( "\n{2,}", "\n\n" );
                s = s.replaceAll( "\\p{Blank}+", " " );
                s = s.replaceAll( "[ ]*\n[ ]*", "\n" );
                s = replacePluginNewlineBackslashes( s );
            }
            result.append( s );
            buffer = new StringBuffer();
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
        return trimMode;
    }

    public void setWhitespaceTrimMode( boolean trimMode )
    {
        if( this.trimMode != trimMode )
        {
            flush();
            this.trimMode = trimMode;
        }
    }

    public void write( char[] arg0, int arg1, int arg2 ) throws IOException
    {
        buffer.append( arg0, arg1, arg2 );
        currentlyOnLineBegin = ps.matcher( buffer ).matches();
        //    System.out.println("\""+PropertiesUtils.saveConvert(buffer.toString(),true)+"\">>
        // "+currentlyOnLineBegin);
    }

    public void close() throws IOException
    {}

    public String toString()
    {
        flush();
        return result.toString();
    }

    public boolean isCurrentlyOnLineBegin()
    {
        return currentlyOnLineBegin;
    }
}
