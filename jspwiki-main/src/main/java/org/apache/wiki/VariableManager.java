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
package org.apache.wiki;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.wiki.api.engine.FilterManager;
import org.apache.wiki.api.exceptions.NoSuchVariableException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.preferences.Preferences;

/**
 *  Manages variables.  Variables are case-insensitive.  A list of all
 *  available variables is on a Wiki page called "WikiVariables".
 *
 *  @since 1.9.20.
 */
public class VariableManager
{
    private static Logger log = Logger.getLogger( VariableManager.class );

    // FIXME: These are probably obsolete.
    public static final String VAR_ERROR = "error";
    public static final String VAR_MSG   = "msg";

    /**
     *  Contains a list of those properties that shall never be shown.
     *  Put names here in lower case.
     */

    static final String[] THE_BIG_NO_NO_LIST = {
        "jspwiki.auth.masterpassword"
    };

    /**
     *  Creates a VariableManager object using the property list given.
     *  @param props The properties.
     */
    public VariableManager( Properties props )
    {
    }

    /**
     *  Returns true if the link is really command to insert
     *  a variable.
     *  <P>
     *  Currently we just check if the link starts with "{$".
     *
     *  @param link The link text
     *  @return true, if this represents a variable link.
     *  @deprecated Use {@link LinkParsingOperations#isVariableLink(String)}
     */
    @Deprecated
    public static boolean isVariableLink( String link )
    {
        return new LinkParsingOperations( null ).isVariableLink( link );
    }

    /**
     *  Parses the link and finds a value.  This is essentially used once
     *  {@link LinkParsingOperations#isVariableLink(String)} has found that
     *  the link text actually contains a variable.  For example, you could
     *  pass in "{$username}" and get back "JanneJalkanen".
     *
     *  @param  context The WikiContext
     *  @param  link    The link text containing the variable name.
     *  @return The variable value.
     *  @throws IllegalArgumentException If the format is not valid (does not
     *          start with "{$", is zero length, etc.)
     *  @throws NoSuchVariableException If a variable is not known.
     */
    public String parseAndGetValue( WikiContext context,
                                    String link )
        throws IllegalArgumentException,
               NoSuchVariableException
    {
        if( !link.startsWith("{$") )
            throw new IllegalArgumentException( "Link does not start with {$" );

        if( !link.endsWith("}") )
            throw new IllegalArgumentException( "Link does not end with }" );

        String varName = link.substring(2,link.length()-1);

        return getValue( context, varName.trim() );
    }

    /**
     *  This method does in-place expansion of any variables.  However,
     *  the expansion is not done twice, that is, a variable containing text $variable
     *  will not be expanded.
     *  <P>
     *  The variables should be in the same format ({$variablename} as in the web
     *  pages.
     *
     *  @param context The WikiContext of the current page.
     *  @param source  The source string.
     *  @return The source string with variables expanded.
     */
    // FIXME: somewhat slow.
    public String expandVariables( WikiContext context,
                                   String      source )
    {
    	StringBuilder result = new StringBuilder();

        for( int i = 0; i < source.length(); i++ )
        {
            if( source.charAt(i) == '{' )
            {
                if( i < source.length()-2 && source.charAt(i+1) == '$' )
                {
                    int end = source.indexOf( '}', i );

                    if( end != -1 )
                    {
                        String varname = source.substring( i+2, end );
                        String value;

                        try
                        {
                            value = getValue( context, varname );
                        }
                        catch( NoSuchVariableException e )
                        {
                            value = e.getMessage();
                        }
                        catch( IllegalArgumentException e )
                        {
                            value = e.getMessage();
                        }

                        result.append( value );
                        i = end;
                        continue;
                    }
                }
                else
                {
                    result.append( '{' );
                }
            }
            else
            {
                result.append( source.charAt(i) );
            }
        }

        return result.toString();
    }

    /**
     *  Returns the value of a named variable.  See {@link #getValue(WikiContext, String)}.
     *  The only difference is that this method does not throw an exception, but it
     *  returns the given default value instead.
     *
     *  @param context WikiContext
     *  @param varName The name of the variable
     *  @param defValue A default value.
     *  @return The variable value, or if not found, the default value.
     */
    public String getValue( WikiContext context, String varName, String defValue )
    {
        try
        {
            return getValue( context, varName );
        }
        catch( NoSuchVariableException e )
        {
            return defValue;
        }
    }

    /**
     *  Returns a value of the named variable.  The resolving order is
     *  <ol>
     *    <li>Known "constant" name, such as "pagename", etc.  This is so
     *        that pages could not override certain constants.
     *    <li>WikiContext local variable.  This allows a programmer to
     *        set a parameter which cannot be overridden by user.
     *    <li>HTTP Session
     *    <li>HTTP Request parameters
     *    <li>WikiPage variable.  As set by the user with the SET directive.
     *    <li>jspwiki.properties
     *  </ol>
     *
     *  Use this method only whenever you really need to have a parameter that
     *  can be overridden by anyone using the wiki.
     *
     *  @param context The WikiContext
     *  @param varName Name of the variable.
     *
     *  @return The variable value.
     *
     *  @throws IllegalArgumentException If the name is somehow broken.
     *  @throws NoSuchVariableException If a variable is not known.
     */
    public String getValue( WikiContext context,
                            String      varName )
        throws IllegalArgumentException,
               NoSuchVariableException
    {
        if( varName == null )
            throw new IllegalArgumentException( "Null variable name." );

        if( varName.length() == 0 )
            throw new IllegalArgumentException( "Zero length variable name." );

        // Faster than doing equalsIgnoreCase()
        String name = varName.toLowerCase();

        for( int i = 0; i < THE_BIG_NO_NO_LIST.length; i++ )
        {
            if( name.equals(THE_BIG_NO_NO_LIST[i]) )
                return ""; // FIXME: Should this be something different?
        }

        try
        {
            //
            //  Using reflection to get system variables adding a new system variable
            //  now only involves creating a new method in the SystemVariables class
            //  with a name starting with get and the first character of the name of
            //  the variable capitalized. Example:
            //    public String getMysysvar(){
            //      return "Hello World";
            //    }
            //
            SystemVariables sysvars = new SystemVariables(context);
            String methodName = "get"+Character.toUpperCase(name.charAt(0))+name.substring(1);
            Method method = sysvars.getClass().getMethod(methodName);
            return (String)method.invoke(sysvars);
        }
        catch( NoSuchMethodException e1 )
        {
            //
            //  It is not a system var. Time to handle the other cases.
            //
            //  Check if such a context variable exists,
            //  returning its string representation.
            //
            if( (context.getVariable( varName )) != null )
            {
                return context.getVariable( varName ).toString();
            }

            //
            //  Well, I guess it wasn't a final straw.  We also allow
            //  variables from the session and the request (in this order).
            //

            HttpServletRequest req = context.getHttpRequest();
            if( req != null && req.getSession() != null )
            {
                HttpSession session = req.getSession();

                try
                {
                    String s;

                    if( (s = (String)session.getAttribute( varName )) != null )
                        return s;

                    if( (s = context.getHttpParameter( varName )) != null )
                        return s;
                }
                catch( ClassCastException e ) {}
            }

            //
            // And the final straw: see if the current page has named metadata.
            //

            WikiPage pg = context.getPage();
            if( pg != null )
            {
                Object metadata = pg.getAttribute( varName );
                if( metadata != null )
                    return metadata.toString();
            }

            //
            // And the final straw part 2: see if the "real" current page has
            // named metadata. This allows a parent page to control a inserted
            // page through defining variables
            //
            WikiPage rpg = context.getRealPage();
            if( rpg != null )
            {
                Object metadata = rpg.getAttribute( varName );
                if( metadata != null )
                    return metadata.toString();
            }

            //
            // Next-to-final straw: attempt to fetch using property name
            // We don't allow fetching any other properties than those starting
            // with "jspwiki.".  I know my own code, but I can't vouch for bugs
            // in other people's code... :-)
            //

            if( varName.startsWith("jspwiki.") )
            {
                Properties props = context.getEngine().getWikiProperties();

                String s = props.getProperty( varName );
                if( s != null )
                {
                    return s;
                }
            }

            //
            //  Final defaults for some known quantities.
            //

            if( varName.equals( VAR_ERROR ) || varName.equals( VAR_MSG ) )
                return "";

            throw new NoSuchVariableException( "No variable "+varName+" defined." );
        }
        catch( Exception e )
        {
            log.info("Interesting exception: cannot fetch variable value",e);
        }
        return "";
    }

    /**
     *  This class provides the implementation for the different system variables.
     *  It is called via Reflection - any access to a variable called $xxx is mapped
     *  to getXxx() on this class.
     *  <p>
     *  This is a lot neater than using a huge if-else if branching structure
     *  that we used to have before.
     *  <p>
     *  Note that since we are case insensitive for variables, and VariableManager
     *  calls var.toLowerCase(), the getters for the variables do not have
     *  capitalization anywhere.  This may look a bit odd, but then again, this
     *  is not meant to be a public class.
     *
     *  @since 2.7.0
     */
    @SuppressWarnings( "unused" )
    private static class SystemVariables
    {
        private WikiContext m_context;

        public SystemVariables(WikiContext context)
        {
            m_context=context;
        }

        public String getPagename()
        {
            return m_context.getPage().getName();
        }

        public String getApplicationname()
        {
            return m_context.getEngine().getApplicationName();
        }

        public String getJspwikiversion()
        {
            return Release.getVersionString();
        }

        public String getEncoding()
        {
            return m_context.getEngine().getContentEncoding();
        }

        public String getTotalpages()
        {
            return Integer.toString(m_context.getEngine().getPageCount());
        }

        public String getPageprovider()
        {
            return m_context.getEngine().getCurrentProvider();
        }

        public String getPageproviderdescription()
        {
            return m_context.getEngine().getCurrentProviderInfo();
        }

        public String getAttachmentprovider()
        {
            WikiProvider p = m_context.getEngine().getAttachmentManager().getCurrentProvider();
            return (p != null) ? p.getClass().getName() : "-";
        }

        public String getAttachmentproviderdescription()
        {
            WikiProvider p = m_context.getEngine().getAttachmentManager().getCurrentProvider();

            return (p != null) ? p.getProviderInfo() : "-";
        }

        public String getInterwikilinks()
        {
        	StringBuilder res = new StringBuilder();

            for( String link : m_context.getEngine().getAllInterWikiLinks() )
            {
                if( res.length() > 0 ) {
                    res.append(", ");
                }
                res.append( link );
                res.append( " --> " );
                res.append( m_context.getEngine().getInterWikiURL(link) );
            }
            return res.toString();
        }

        public String getInlinedimages()
        {
        	StringBuilder res = new StringBuilder();

            for( String ptrn : m_context.getEngine().getAllInlinedImagePatterns() )
            {
                if( res.length() > 0 ) {
                    res.append(", ");
                }

                res.append(ptrn);
            }

            return res.toString();
        }

        public String getPluginpath()
        {
            String s = m_context.getEngine().getPluginManager().getPluginSearchPath();

            return (s == null) ? "-" : s;
        }

        public String getBaseurl()
        {
            return m_context.getEngine().getBaseURL();
        }

        public String getUptime()
        {
            Date now = new Date();
            long secondsRunning = (now.getTime() - m_context.getEngine().getStartTime().getTime()) / 1000L;

            long seconds = secondsRunning % 60;
            long minutes = (secondsRunning /= 60) % 60;
            long hours = (secondsRunning /= 60) % 24;
            long days = secondsRunning /= 24;

            return days + "d, " + hours + "h " + minutes + "m " + seconds + "s";
        }

        public String getLoginstatus()
        {
            WikiSession session = m_context.getWikiSession();
            return Preferences.getBundle( m_context, InternationalizationManager.CORE_BUNDLE ).getString( "varmgr." + session.getStatus());
        }

        public String getUsername()
        {
            Principal wup = m_context.getCurrentUser();

            ResourceBundle rb = Preferences.getBundle( m_context, InternationalizationManager.CORE_BUNDLE );
            return wup != null ? wup.getName() : rb.getString( "varmgr.not.logged.in" );
        }

        public String getRequestcontext()
        {
            return m_context.getRequestContext();
        }

        public String getPagefilters()
        {
            FilterManager fm = m_context.getEngine().getFilterManager();
            List<PageFilter> filters = fm.getFilterList();
            StringBuilder sb = new StringBuilder();

            for (PageFilter pf : filters )
            {
                String f = pf.getClass().getName();

                if( pf instanceof InternalModule )
                    continue;

                if( sb.length() > 0 )
                    sb.append(", ");
                sb.append(f);
            }

            return sb.toString();
        }
    }

}
