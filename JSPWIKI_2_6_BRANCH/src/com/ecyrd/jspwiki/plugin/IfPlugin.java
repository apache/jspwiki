/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

package com.ecyrd.jspwiki.plugin;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiProvider;

/**
 *  The IfPlugin allows parts of a WikiPage to be executed conditionally.
 *  You can also use shorthand "If" to run it.
 *  Parameters:
 *  <ul>
 *    <li>group - A "|" -separated list of group names.
 *    <li>user  - A "|" -separated list of user names.
 *    <li>ip    - A "|" -separated list of ip addresses.
 *    <li>var   - A wiki variable
 *    <li>page  - A page name
 *    <li>contains - A Perl5 regexp pattern
 *    <li>is    - A Perl5 regexp pattern
 *    <li>exists - "true" or "false".
 *  </ul>
 *
 *  <p>If any of them match, the body of the plugin is executed.  You can
 *  negate the content by prefixing it with a "!".  For example, to greet
 *  all admins, put the following in your LeftMenu:</p>
 *  <pre>
 *  [{If group='Admin'
 *
 *  Hello, Admin, and your mighty powers!}]
 *  </pre>
 *
 *  <p>In order to send a message to everybody except Jack use</p>
 *  <pre>
 *  [{If user='!Jack'
 *
 *  %%warning
 *  Jack's surprise birthday party at eleven!
 *  %%}]
 *  </pre>
 *
 *  <p>Note that you can't use "!Jack|!Jill", because for Jack, !Jill matches;
 *  and for Jill, !Jack matches.  These are not regular expressions (though
 *  they might become so in the future).<p>
 *
 *  <p>To check for page content, use</p>
 *  <pre>
 *  [{If page='TestPage' contains='xyzzy'
 *
 *  Page contains the text "xyzzy"}]
 *  </pre>
 *
 *  <p>The difference between "contains" and "is" is that "is" is always an exact match,
 *  whereas "contains" just checks if a pattern is available.</p>
 *
 *  <p>To check for page existence, use</p>
 *  <pre>
 *  [{If page='TestPage' exists='true'
 *
 *  Page "TestPage" exists.}]
 *  </pre>
 *  <p>With the same mechanism, it's also possible to test for the existence
 *  of a variable - just use "var" instead of "page".</p>
 *  
 *  <p>Another caveat is that the plugin body content is not counted
 *  towards ReferenceManager links.  So any links do not appear on any reference
 *  lists.  Depending on your position, this may be a good or a bad
 *  thing.</p>
 *
 *  <h3>Calling Externally</h3>
 *
 *  <p>The functional, decision-making part of this plugin may be called from
 *  other code (e.g., other plugins) since it is available as a static method
 *  {@link #ifInclude(WikiContext,Map)}. Note that the plugin body may contain
 *  references to other plugins.</p>
 *
 *  @author Janne Jalkanen
 *  @author Murray Altheim
 *  @since 2.6
 */
public class IfPlugin implements WikiPlugin
{
    public static final String PARAM_GROUP    = "group";
    public static final String PARAM_USER     = "user";
    public static final String PARAM_IP       = "ip";
    public static final String PARAM_PAGE     = "page";
    public static final String PARAM_CONTAINS = "contains";
    public static final String PARAM_VAR      = "var";
    public static final String PARAM_IS       = "is";
    public static final String PARAM_EXISTS   = "exists";

    /**
     *  {@inheritDoc}
     */
    public String execute(WikiContext context, Map params) throws PluginException
    {
        return ifInclude(context,params)
                ? context.getEngine().textToHTML(
                        context,(String)params.get(PluginManager.PARAM_BODY) )
                : "" ;
    }


    /**
     *  Returns a boolean result based on processing the WikiContext and
     *  parameter Map as according to the rules stated in the IfPlugin
     *  documentation. 
     *  As a static method this may be called by other classes.
     *
     * @param context   The current WikiContext.
     * @param params    The parameter Map which contains key-value pairs.
     */
    public static boolean ifInclude( WikiContext context, Map params ) throws PluginException
    {
        boolean include = false;

        String group    = (String)params.get(PARAM_GROUP);
        String user     = (String)params.get(PARAM_USER);
        String ip       = (String)params.get(PARAM_IP);
        String page     = (String)params.get(PARAM_PAGE);
        String contains = (String)params.get(PARAM_CONTAINS);
        String var      = (String)params.get(PARAM_VAR);
        String is       = (String)params.get(PARAM_IS);
        String exists   = (String)params.get(PARAM_EXISTS);

        include |= checkGroup(context, group);
        include |= checkUser(context, user);
        include |= checkIP(context, ip);

        if( page != null )
        {
            String content = context.getEngine().getPureText(page, WikiProvider.LATEST_VERSION).trim();
            include |= checkContains(content,contains);
            include |= checkIs(content,is);
            include |= checkExists(context,page,exists);
        }

        if( var != null )
        {
            String content = context.getEngine().getVariable(context, var);
            include |= checkContains(content,contains);
            include |= checkIs(content,is);
            include |= checkVarExists(content,exists);
        }

        return include;
    }

    private static boolean checkExists( WikiContext context, String page, String exists )
    {
        if( exists == null ) return false;
        return !context.getEngine().pageExists(page) ^ TextUtil.isPositive(exists);
    }

    private static boolean checkVarExists( String varContent, String exists )
    {
        if( exists == null ) return false;
        return (varContent == null ) ^ TextUtil.isPositive(exists);
    }

    private static boolean checkGroup( WikiContext context, String group )
    {
        if( group == null ) return false;
        String[] groupList = StringUtils.split(group,'|');
        boolean include = false;

        for( int i = 0; i < groupList.length; i++ )
        {
            String gname = groupList[i];
            boolean invert = false;
            if( groupList[i].startsWith("!") )
            {
                gname = groupList[i].substring(1);
                invert = true;
            }

            Principal g = context.getEngine().getAuthorizationManager().resolvePrincipal(gname);

            include |= context.getEngine().getAuthorizationManager().isUserInRole( context.getWikiSession(), g ) ^ invert;
        }
        return include;
    }

    private static boolean checkUser( WikiContext context, String user )
    {
        if( user == null || context.getCurrentUser() == null ) return false;

        String[] list = StringUtils.split(user,'|');
        boolean include = false;

        for( int i = 0; i < list.length; i++ )
        {
            boolean invert = false;
            if( list[i].startsWith("!") )
            {
                invert = true;
            }

            include |= user.equals( context.getCurrentUser().getName() ) ^ invert;
        }
        return include;
    }

    // TODO: Add subnetwork matching, e.g. 10.0.0.0/8
    private static boolean checkIP( WikiContext context, String ipaddr )
    {
        if( ipaddr == null || context.getHttpRequest() == null ) return false;

        String[] list = StringUtils.split(ipaddr,'|');
        boolean include = false;

        for( int i = 0; i < list.length; i++ )
        {
            boolean invert = false;
            if( list[i].startsWith("!") )
            {
                invert = true;
            }

            include |= ipaddr.equals( context.getHttpRequest().getRemoteAddr() ) ^ invert;
        }
        return include;
    }

    private static boolean doMatch( String content, String pattern )
        throws PluginException
    {
        PatternCompiler compiler = new Perl5Compiler();
        PatternMatcher  matcher  = new Perl5Matcher();

        try
        {
            Pattern matchp = compiler.compile( pattern, Perl5Compiler.SINGLELINE_MASK );
            // m_exceptPattern = compiler.compile( exceptPattern, Perl5Compiler.SINGLELINE_MASK );
            return matcher.matches( content, matchp );
        }
        catch( MalformedPatternException e )
        {
            throw new PluginException("Faulty pattern "+pattern);
        }

    }

    private static boolean checkContains( String pagecontent, String matchPattern )
        throws PluginException
    {
        if( pagecontent == null || matchPattern == null ) return false;

        return doMatch( pagecontent, ".*"+matchPattern+".*" );
    }

    private static boolean checkIs( String content, String matchPattern )
        throws PluginException
    {
        if( content == null || matchPattern == null ) return false;

        matchPattern = "^"+matchPattern+"$";

        return doMatch(content, matchPattern);
    }
}
