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
package org.apache.wiki.tags;

import java.io.IOException;

import org.apache.wiki.WikiSession;
import org.apache.wiki.util.TextUtil;

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
            	StringBuilder sb = new StringBuilder();
                if ( messages.length == 1 )
                {
                    sb.append( "<div class=\"" + m_div + "\">" + m_prefix + TextUtil.replaceEntities(messages[0]) + "</div>" );
                }
                else
                {
                    sb.append( "<div class=\"" + m_div + "\">" + m_prefix );
                    sb.append( "<ul>" );
                    for( int i = 0; i < messages.length; i++ )
                    {
                        sb.append( "<li>" + TextUtil.replaceEntities(messages[i]) + "</li>" );
                    }
                    sb.append( "</ul></div>" );
                }
                pageContext.getOut().println( sb.toString() );
            }
        }
        return SKIP_BODY;
    }
}
