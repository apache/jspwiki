/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiSession;

/**
 * Returns or clears the current messages associated with the user's wiki
 * session. This tag accepts four attributes: <ul> <li><code>action</code> -
 * if "clear", the messsages will be cleared. Otherwise, this tag will always
 * print the set of current messages as either a single &lt;p&gt; tag (if there
 * is only one message) or a bulleted list (if there is more than one).</li>
 * <li><code>prefix</code> - the string to prepend to the list of errors, if
 * there are any; default is empty string</li> <li><code>topic</code> - a
 * collection for messages, for example those associated with a particular web
 * form. If not suppled, defaults to a generic (non-specific) collection</li>
 * <li><code>div</code> - the <code>div</code> class to wrap the
 * messages in; if not supplied, <code>information</code> is assumed</li></ul>
 * @author Andrew Jaquith
 * @since 2.3.54
 */
public class MessagesTag extends WikiTagBase
{
    private static final long   serialVersionUID = 0L;

    private String              m_action         = null;

    private String              m_prefix         = "";

    private String              m_topic          = null;
    
    private String              m_div            = "information";

    private static final String CLEAR            = "clear";

    public void initTag()
    {
        super.initTag();
        m_action = m_topic = null;
        m_prefix = "";
        m_div = "information";
    }


    public void setTopic( String topic )
    {
        m_topic = topic;
    }

    public void setPrefix( String prefix )
    {
        m_prefix = prefix;
    }

    public void setDiv( String div )
    {
        m_div = div;
    }
    
    public void setAction( String action )
    {
        m_action = action.toLowerCase();
    }

    public final int doWikiStartTag() throws IOException
    {
        WikiSession session = m_wikiContext.getWikiSession();
        if ( CLEAR.equals( m_action ) )
        {
            if ( m_topic == null )
            {
                session.clearMessages();
            }
            else
            {
                session.clearMessages( m_topic );
            }
        }
        else
        {
            String[] messages = ( m_topic == null ) ? session.getMessages() : session.getMessages( m_topic );
            if ( messages.length > 0 )
            {
                StringBuffer sb = new StringBuffer();
                if ( messages.length == 1 )
                {
                    sb.append( "<div class=\"" + m_div + "\">" + m_prefix + messages[0] + "</div>" );
                }
                else
                {
                    sb.append( "<div class=\"" + m_div + "\">" + m_prefix );
                    sb.append( "<ul>" );
                    for( int i = 0; i < messages.length; i++ )
                    {
                        sb.append( "<li>" + messages[i] + "</li>" );
                    }
                    sb.append( "</ul></div>" );
                }
                pageContext.getOut().println( sb.toString() );
            }
        }
        return SKIP_BODY;
    }
}
