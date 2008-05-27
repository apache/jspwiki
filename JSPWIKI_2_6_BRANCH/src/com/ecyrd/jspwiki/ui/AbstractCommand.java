/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.ui;

import com.ecyrd.jspwiki.TextUtil;


/**
 * Abstract, immutable Command implementation class. All of the fields in this
 * class are <code>final</code>. This class is thread-safe.
 * @author Andrew Jaquith
 * @since 2.4.22
 */
public abstract class AbstractCommand implements Command
{
    private static final Command[] ALL_COMMANDS = new Command[] {
        PageCommand.ATTACH,
        PageCommand.COMMENT,
        PageCommand.CONFLICT,
        PageCommand.DELETE,
        PageCommand.DIFF,
        PageCommand.EDIT,
        PageCommand.INFO,
        PageCommand.NONE,
        PageCommand.OTHER,
        PageCommand.PREVIEW,
        PageCommand.RENAME,
        PageCommand.RSS,
        PageCommand.UPLOAD,
        PageCommand.VIEW,
        GroupCommand.DELETE_GROUP,
        GroupCommand.EDIT_GROUP,
        GroupCommand.VIEW_GROUP,
        WikiCommand.CREATE_GROUP,
        WikiCommand.ERROR,
        WikiCommand.FIND,
        WikiCommand.INSTALL,
        WikiCommand.LOGIN,
        WikiCommand.LOGOUT,
        WikiCommand.MESSAGE,
        WikiCommand.PREFS,
        WikiCommand.WORKFLOW,
        WikiCommand.ADMIN,
        RedirectCommand.REDIRECT
    };

    private static final String    HTTPS = "HTTPS://";

    private static final String    HTTP = "HTTP://";

    private final String           m_jsp;

    private final String           m_jspFriendlyName;

    private final String           m_urlPattern;

    private final String           m_requestContext;

    private final String           m_contentTemplate;

    private final Object           m_target;

    /**
     * Constructs a new Command with a specified wiki context, URL pattern,
     * content template and target. The URL pattern is used to derive
     * the JSP; if it is a "local" JSP (that is, it does not contain
     * the <code>http://</code> or <code>https://</code> prefixes),
     * then the JSP will be a cleansed version of the URL pattern;
     * symbols (such as <code>%u</code>) will removed. If it the supplied
     * URL pattern points to a non-local destination, the JSP will be set
     * to the value supplied, unmodified.
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param contentTemplate the content template; may be <code>null</code>
     * @param target the target of the command, such as a WikiPage; may be
     *            <code>null</code>
     * @throws IllegalArgumentException if the request content or URL pattern is
     *             <code>null</code>
     */
    protected AbstractCommand( String requestContext, String urlPattern, String contentTemplate, Object target )
    {
        if ( requestContext == null || urlPattern == null )
        {
            throw new IllegalArgumentException( "Request context, URL pattern and type must not be null." );
        }

        m_requestContext = requestContext;

        if ( urlPattern.toUpperCase().startsWith( HTTP ) ||
             urlPattern.toUpperCase().endsWith( HTTPS ) )
        {
            // For an HTTP/HTTPS url, pass it through without modification
            m_jsp = urlPattern;
            m_jspFriendlyName = "Special Page";
        }
        else
        {
            // For local JSPs, take everything to the left of ?, then
            // delete all variable substitutions
            String jsp = urlPattern;
            int qPosition = urlPattern.indexOf( '?' );
            if ( qPosition != -1 )
            {
                jsp = jsp.substring( 0, qPosition );
            }
            m_jsp = removeSubstitutions(jsp);

            // Calculate the "friendly name" for the JSP
            if ( m_jsp.toUpperCase().endsWith( ".JSP" ) )
            {
                m_jspFriendlyName = TextUtil.beautifyString( m_jsp.substring( 0, m_jsp.length() - 4 ) );
            }
            else
            {
                m_jspFriendlyName = m_jsp;
            }
        }

        m_urlPattern = urlPattern;

        m_contentTemplate = contentTemplate;

        m_target = target;
    }

    //
    //  This is just *so* much faster than doing String.replaceAll().  It would, in fact,
    //  be worth to cache this value.
    //
    private String removeSubstitutions(String jsp)
    {
        //return jsp.replaceAll( "\u0025[a-z|A-Z]", "" );
        StringBuffer newjsp = new StringBuffer( jsp.length() );
        for( int i = 0; i < jsp.length(); i++ )
        {
            char c = jsp.charAt(i);
            if( c == '%' && i < jsp.length()-1 && Character.isLetterOrDigit( jsp.charAt(i+1) ) )
            {
                i++;
                continue;
            }
            newjsp.append( c );
        }
        return newjsp.toString();
    }

    /**
     * Returns a defensively-created array of all
     * static Commands.
     * @return the array of commands
     */
    public static final Command[] allCommands()
    {
        return (Command[])ALL_COMMANDS.clone();
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#targetedCommand(Object)
     */
    public abstract Command targetedCommand( Object target );

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getContentTemplate()
     */
    public final String getContentTemplate()
    {
        return m_contentTemplate;
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getJSP()
     */
    public final String getJSP()
    {
        return m_jsp;
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getName()
     */
    public abstract String getName();

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getRequestContext()
     */
    public final String getRequestContext()
    {
        return m_requestContext;
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getTarget()
     */
    public final Object getTarget()
    {
        return m_target;
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getURLPattern()
     */
    public final String getURLPattern()
    {
        return m_urlPattern;
    }

    /**
     * Returns the "friendly name" for this command's JSP, namely
     * a beatified version of the JSP's name without the .jsp suffix.
     * @return the friendly name
     */
    protected final String getJSPFriendlyName()
    {
        return m_jspFriendlyName;
    }

    /**
     * Returns a String representation of the Command.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return "Command" +
               "[context=" + m_requestContext + "," +
               "urlPattern=" + m_urlPattern + "," +
               "jsp=" +  m_jsp +
               ( m_target == null ? "" : ",target=" + m_target + m_target.toString() ) +
               "]";
    }

}
