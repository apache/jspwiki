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
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.Properties;
import java.util.Iterator;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *  Manages variables.  Variables are case-insensitive.  A list of all
 *  available variables is on a Wiki page called "WikiVariables".
 *
 *  @author Janne Jalkanen
 *  @since 1.9.20.
 */
public class VariableManager
{
    //private static Logger log = Logger.getLogger( VariableManager.class );
   
    public static final String VAR_ERROR = "error";
    public static final String VAR_MSG   = "msg";
    
    /**
     *  Contains a list of those properties that shall never be shown.
     *  Put names here in lower case.
     */
    
    protected static final String[] THE_BIG_NO_NO_LIST = {
        "jspwiki.auth.masterpassword"
    };
    
    public VariableManager( Properties props )
    {
    }

    /**
     *  Returns true if the link is really command to insert
     *  a variable.
     *  <P>
     *  Currently we just check if the link starts with "{$".
     */
    public static boolean isVariableLink( String link )
    {
        return link.startsWith("{$");
    }

    /**
     *  Parses the link and finds a value.
     *  <P>
     *  A variable is inserted using the notation [{$variablename}].
     *
     *  @throws IllegalArgumentException If the format is not valid (does not 
     *          start with {$, is zero length, etc.)
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
     */
    // FIXME: somewhat slow.
    public String expandVariables( WikiContext context,
                                   String      source )
    {
        StringBuffer result = new StringBuffer();

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
     *  @param varName Name of the variable.
     *
     *  @throws IllegalArgumentException If the name is somehow broken.
     *  @throws NoSuchVariableException If a variable is not known.
     */
    // FIXME: Currently a bit complicated.  Perhaps should use reflection
    //        or something to make an easy way of doing stuff.
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
        String res = "";

        for( int i = 0; i < THE_BIG_NO_NO_LIST.length; i++ )
        {
            if( name.equals(THE_BIG_NO_NO_LIST[i]) )
                return ""; // FIXME: Should this be something different?
        }
        
        if( name.equals("pagename") )
        {
            res = context.getPage().getName();
        }
        else if( name.equals("applicationname") )
        {
            res = context.getEngine().getApplicationName();
        }
        else if( name.equals("jspwikiversion") )
        {
            res = Release.getVersionString();
        }
        else if( name.equals("encoding") )
        {
            res = context.getEngine().getContentEncoding();
        }
        else if( name.equals("totalpages") )
        {
            res = Integer.toString(context.getEngine().getPageCount());
        }
        else if( name.equals("pageprovider") )
        {
            res = context.getEngine().getCurrentProvider();
        }
        else if( name.equals("pageproviderdescription") )
        {
            res = context.getEngine().getCurrentProviderInfo();
        }
        else if( name.equals("attachmentprovider") )
        {
            WikiProvider p = context.getEngine().getAttachmentManager().getCurrentProvider();
            res = (p != null) ? p.getClass().getName() : "-";
        }
        else if( name.equals("attachmentproviderdescription") )
        {
            WikiProvider p = context.getEngine().getAttachmentManager().getCurrentProvider();

            res = (p != null) ? p.getProviderInfo() : "-";
        }
        else if( name.equals("interwikilinks") )
        {
            // FIXME: Use StringBuffer
            for( Iterator i = context.getEngine().getAllInterWikiLinks().iterator(); i.hasNext(); )
            {
                String link = (String) i.next();
                res += link+" --&gt; "+context.getEngine().getInterWikiURL(link)+"<br />\n";
            }
        }
        else if( name.equals("inlinedimages") )
        {
            // FIXME: Use StringBuffer
            for( Iterator i = context.getEngine().getAllInlinedImagePatterns().iterator(); i.hasNext(); )
            {
                String ptrn = (String) i.next();
                res += ptrn+"<br />\n";
            }
        }
        else if( name.equals("pluginpath") )
        {
            res = context.getEngine().getPluginSearchPath();

            res = (res == null) ? "-" : res;
        }
        else if( name.equals("baseurl") )
        {
            res = context.getEngine().getBaseURL();
        }
        else if( name.equals("uptime") )
        {
            Date now = new Date();
            long secondsRunning = (now.getTime() - context.getEngine().getStartTime().getTime())/1000L;

            long seconds = secondsRunning % 60;
            long minutes = (secondsRunning /= 60) % 60;
            long hours   = (secondsRunning /= 60) % 24;
            long days    = secondsRunning /= 24;

            return days+"d, "+hours+"h "+minutes+"m "+seconds+"s";
        }
        else if( name.equals("loginstatus") )
        {
            WikiSession session = context.getWikiSession();
            return session.getStatus();
        }
        else if( name.equals("username") )
        {
            Principal wup = context.getCurrentUser();

            return wup != null ? wup.getName() : "not logged in";
        }
        else if( name.equals("requestcontext") )
        {
            return context.getRequestContext();
        }
        else if( name.equals("pagefilters") )
        {
            List filters = context.getEngine().getFilterManager().getFilterList();
            StringBuffer sb = new StringBuffer();

            for( Iterator i = filters.iterator(); i.hasNext(); )
            {
                String f = i.next().getClass().getName();

                //
                //  Skip some known internal filters.
                //  FIXME: Quite a klugde.
                //

                if( f.endsWith("ReferenceManager") || f.endsWith("WikiDatabase$SaveFilter" ) )
                    continue;

                if( sb.length() > 0 ) sb.append(", ");
                sb.append( f );
            }

            return sb.toString();
        }
        else
        {
            // 
            // Check if such a context variable exists,
            // returning its string representation.
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
            if( req != null )
            {
                HttpSession session = req.getSession();

                try
                {
                    if( (res = (String)session.getAttribute( varName )) != null )
                        return res;

                    if( (res = context.getHttpParameter( varName )) != null )
                        return res;
                }
                catch( ClassCastException e ) {}
            }

            // And the final straw: see if the current page has named metadata.
            
            WikiPage pg = context.getPage();
            if( pg != null )
            {
                Object metadata = pg.getAttribute( varName );
                if( metadata != null )
                    return( metadata.toString() );
            }

            // Next-to-final straw: attempt to fetch using property name
            // We don't allow fetching any other properties than those starting
            // with "jspwiki.".  I know my own code, but I can't vouch for bugs
            // in other people's code... :-)
            
            if( varName.startsWith("jspwiki.") )
            {
                Properties props = context.getEngine().getWikiProperties();

                res = props.getProperty( varName );
                if( res != null )
                {
                    return res;
                }
            }
            
            //
            //  Final defaults for some known quantities.
            //

            if( varName.equals( VAR_ERROR ) || varName.equals( VAR_MSG ) )
                return "";
  
            throw new NoSuchVariableException( "No variable "+varName+" defined." );
        }
        
        return res;
    }
}
