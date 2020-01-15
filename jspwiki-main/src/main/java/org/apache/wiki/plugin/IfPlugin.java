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

package org.apache.wiki.plugin;

import org.apache.commons.lang3.StringUtils;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.TextUtil;

import java.security.Principal;
import java.util.Map;

/**
 *  The IfPlugin allows parts of a WikiPage to be executed conditionally, and is intended as a flexible way
 *  of customizing a page depending on certain conditions. Do not use it as a security mechanism to conditionally
 *  hide content from users (use page ACLs for that).
 *  
 *  You can also use shorthand "If" to run it.
 *  
 *  Parameters:
 *  <ul>
 *    <li><b>group</b> - A "|" -separated list of group names.
 *    <li><b>user</b>  - A "|" -separated list of user names.
 *    <li><b>ip</b>    - A "|" -separated list of ip addresses.
 *    <li><b>var</b>   - A wiki variable
 *    <li><b>page</b>  - A page name
 *    <li><b>contains</b> - A Perl5 regexp pattern
 *    <li><b>is</b>    - A Perl5 regexp pattern
 *    <li><b>exists</b> - "true" or "false".
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
 *  @since 2.6
 */
public class IfPlugin implements WikiPlugin
{
    /** The parameter name for setting the group to check.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_GROUP    = "group";

    /** The parameter name for setting the user id to check.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_USER     = "user";
    
    /** The parameter name for setting the ip address to check.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_IP       = "ip";
    
    /** The parameter name for setting the page name to check.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_PAGE     = "page";
    
    /** The parameter name for setting the contents of the page to check.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_CONTAINS = "contains";
    
    /** The parameter name for setting the variable name to check.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_VAR      = "var";
    
    /** The parameter name for setting the exact content to check.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_IS       = "is";
    
    /** The parameter name for checking whether a page/var exists.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_EXISTS   = "exists";

    /**
     *  {@inheritDoc}
     */
    public String execute( final WikiContext context, final Map< String, String > params ) throws PluginException {
        return ifInclude( context,params )
                ? context.getEngine().getRenderingManager().textToHTML( context, params.get( DefaultPluginManager.PARAM_BODY ) )
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
     * @throws PluginException If something goes wrong
     * @return True, if the condition holds.
     */
    public static boolean ifInclude( final WikiContext context, final Map< String, String > params ) throws PluginException {
        final String group    = params.get( PARAM_GROUP );
        final String user     = params.get( PARAM_USER );
        final String ip       = params.get( PARAM_IP );
        final String page     = params.get( PARAM_PAGE );
        final String contains = params.get( PARAM_CONTAINS );
        final String var      = params.get( PARAM_VAR );
        final String is       = params.get( PARAM_IS );
        final String exists   = params.get( PARAM_EXISTS );

        boolean include = checkGroup( context, group );
        include |= checkUser(context, user);
        include |= checkIP(context, ip);

        if( page != null ) {
            final String content = context.getEngine().getPageManager().getPureText(page, WikiProvider.LATEST_VERSION).trim();
            include |= checkContains(content,contains);
            include |= checkIs(content,is);
            include |= checkExists(context,page,exists);
        }

        if( var != null ) {
            final String content = context.getEngine().getVariableManager().getVariable(context, var);
            include |= checkContains(content,contains);
            include |= checkIs(content,is);
            include |= checkVarExists(content,exists);
        }

        return include;
    }

    private static boolean checkExists( final WikiContext context, final String page, final String exists ) {
        if( exists == null ) {
            return false;
        }
        return !context.getEngine().getPageManager().wikiPageExists( page ) ^ TextUtil.isPositive(exists);
    }

    private static boolean checkVarExists( final String varContent, final String exists ) {
        if( exists == null ) {
            return false;
        }
        return varContent == null ^ TextUtil.isPositive( exists );
    }

    private static boolean checkGroup( final WikiContext context, final String group ) {
        if( group == null ) {
            return false;
        }
        final String[] groupList = StringUtils.split(group,'|');
        boolean include = false;

        for( final String grp : groupList ) {
            String gname = grp;
            boolean invert = false;
            if( grp.startsWith( "!" ) ) {
                if( grp.length() > 1 ) {
                    gname = grp.substring( 1 );
                }
                invert = true;
            }

            final Principal g = context.getEngine().getAuthorizationManager().resolvePrincipal( gname );

            include |= context.getEngine().getAuthorizationManager().isUserInRole( context.getWikiSession(), g ) ^ invert;
        }
        return include;
    }

    private static boolean checkUser( final WikiContext context, final String user ) {
        if( user == null || context.getCurrentUser() == null ) {
            return false;
        }

        final String[] list = StringUtils.split(user,'|');
        boolean include = false;

        for( final String usr : list ) {
            String userToCheck = usr;
            boolean invert = false;
            if( usr.startsWith( "!" ) ) {
                invert = true;
                // strip !
                if( user.length() > 1 ) {
                    userToCheck = usr.substring( 1 );
                }
            }

            include |= userToCheck.equals( context.getCurrentUser().getName() ) ^ invert;
        }
        return include;
    }

    // TODO: Add subnetwork matching, e.g. 10.0.0.0/8
    private static boolean checkIP( final WikiContext context, final String ipaddr ) {
        if( ipaddr == null || context.getHttpRequest() == null ) {
            return false;
        }

        final String[] list = StringUtils.split(ipaddr,'|');
        boolean include = false;

        for( final String ip : list ) {
            String ipaddrToCheck = ip;
            boolean invert = false;
            if( ip.startsWith( "!" ) ) {
                invert = true;
                // strip !
                if( ip.length() > 1 ) {
                    ipaddrToCheck = ip.substring( 1 );
                }
            }

            include |= ipaddrToCheck.equals( HttpUtil.getRemoteAddress( context.getHttpRequest() ) ) ^ invert;
        }
        return include;
    }

    private static boolean doMatch( final String content, final String pattern ) throws PluginException {
        final PatternCompiler compiler = new Perl5Compiler();
        final PatternMatcher  matcher  = new Perl5Matcher();

        try {
            final Pattern matchp = compiler.compile( pattern, Perl5Compiler.SINGLELINE_MASK );
            return matcher.matches( content, matchp );
        } catch( final MalformedPatternException e ) {
            throw new PluginException( "Faulty pattern " + pattern );
        }

    }

    private static boolean checkContains( final String pagecontent, final String matchPattern ) throws PluginException {
        if( pagecontent == null || matchPattern == null ) {
            return false;
        }

        return doMatch( pagecontent, ".*"+matchPattern+".*" );
    }

    private static boolean checkIs( final String content, final String matchPattern ) throws PluginException {
        if( content == null || matchPattern == null ) {
            return false;
        }
        return doMatch( content, "^" + matchPattern + "$");
    }

}
