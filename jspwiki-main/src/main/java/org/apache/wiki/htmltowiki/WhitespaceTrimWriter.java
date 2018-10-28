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
public class WhitespaceTrimWriter extends Writer
{

    private StringBuffer m_result = new StringBuffer();

    private StringBuffer m_buffer = new StringBuffer();

    private boolean m_trimMode = true;

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
    public void setWhitespaceTrimMode( boolean trimMode )
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
    public void write( char[] arg0, int arg1, int arg2 ) throws IOException
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
