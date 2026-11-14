/* 
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
package org.apache.wiki.htmltowiki;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Part of the XHtmlToWikiTranslator.
 * 
 */
// FIXME: Needs a better description as to how it works.
public class WhitespaceTrimWriter extends Writer {

    private final StringBuffer m_result = new StringBuffer();

    private StringBuffer m_buffer = new StringBuffer();

    private boolean m_trimMode = true;

    public static final String NO_TRIMMED_SPACE = "&nbsp;";
    private static final Pattern ONLINE_PATTERN = Pattern.compile( ".*?\\n\\s*?", Pattern.MULTILINE );

    private boolean m_currentlyOnLineBegin = true;

    /**
     *  {@inheritDoc}
     */
    @Override
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
                s = s.replace( NO_TRIMMED_SPACE, " " );
            }
            m_result.append( s );
            m_buffer = new StringBuffer();
        }
    }

    private String replacePluginNewlineBackslashes( String s )
    {
        final Pattern p = Pattern.compile( "\\{\\{\\{(.*?)\\}\\}\\}|\\{\\{(.*?)\\}\\}|\\[\\{", Pattern.DOTALL + Pattern.MULTILINE );
        final Matcher m = p.matcher( s );
        final StringBuilder sb = new StringBuilder();
        int pos = 0;
        while( m.find( pos ) )
        {
            final boolean plugin = s.startsWith( "[{", m.start() );
            final int end = plugin ? endOfPlugin( s, m.start() ) : m.end();
            if( end < 0 )
            {
                sb.append( s, pos, m.end() );
                pos = m.end();
                continue;
            }
            sb.append( s, pos, m.start() );
            final String text = s.substring( m.start(), end );
            sb.append( plugin ? text.replace( "\\\\", "\n" ) : text );
            pos = end;
        }
        sb.append( s, pos, s.length() );
        return sb.toString();
    }

    /**
     *  Returns the index after the closing "}]" of the plugin that starts at {@code start}, counting the plugins nested in it.
     *  When the brackets do not balance it falls back to the first "}]", and returns -1 when there is none.
     */
    private static int endOfPlugin( final String s, final int start )
    {
        int depth = 0;
        for( int i = start; i < s.length() - 1; i++ )
        {
            if( s.startsWith( "[{", i ) )
            {
                depth++;
                i++;
            }
            else if( s.startsWith( "}]", i ) )
            {
                depth--;
                if( depth == 0 )
                {
                    return i + 2;
                }
                i++;
            }
        }
        final int first = s.indexOf( "}]", start + 2 );
        return first < 0 ? -1 : first + 2;
    }

    /**
     *  Returns true, if this Writer is currently trimming any whitespaces.
     *  
     *  @return True, if trimming.
     */
    public boolean isWhitespaceTrimMode()
    {
        return m_trimMode;
    }

    /**
     *  Set the trimming mode on/off.
     *  
     *  @param trimMode True, if you want trimming to be turned on.  False otherwise.
     */
    public void setWhitespaceTrimMode(final boolean trimMode )
    {
        if( m_trimMode != trimMode )
        {
            flush();
            m_trimMode = trimMode;
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void write(final char[] arg0, final int arg1, final int arg2 ) throws IOException
    {
        m_buffer.append( arg0, arg1, arg2 );
        m_currentlyOnLineBegin = ONLINE_PATTERN.matcher( m_buffer ).matches();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {}

    /**
     *  {@inheritDoc}
     */
    @Override
    public String toString()
    {
        flush();
        return m_result.toString();
    }

    /**
     *  Returns true, if the writer is currently writing a line start.
     *  
     *  @return True or false.
     */
    public boolean isCurrentlyOnLineBegin()
    {
        return m_currentlyOnLineBegin;
    }
}
