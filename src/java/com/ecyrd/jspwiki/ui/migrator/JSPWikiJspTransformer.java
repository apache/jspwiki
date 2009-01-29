/* 
    JSPWiki - a JSP-based WikiWiki clone.

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

package com.ecyrd.jspwiki.ui.migrator;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.stripes.action.ActionBean;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.action.WikiContextFactory;
import com.ecyrd.jspwiki.search.SearchResult;
import com.ecyrd.jspwiki.ui.stripes.HandlerInfo;
import com.ecyrd.jspwiki.util.FileUtil;
import com.ecyrd.jspwiki.util.TextUtil;

/**
 * Transforms a JspDocument from standard JSP markup to Stripes markup.
 * Known limitations: will not modify Java code inside of tag attributes.
 */
public class JSPWikiJspTransformer extends AbstractJspTransformer
{
    private static final Pattern CONTEXT_PATTERN = Pattern.compile( "\\.createContext\\(.*?WikiContext.([A-Z]*?)\\s*\\);" );

    private static final Pattern HASACCESS_PATTERN = Pattern
        .compile( "if\\s*\\(\\s*\\!(wikiContext|context|ctx)\\.hasAccess\\(.*?\\)\\s*\\)\\s*return;" );

    private static final Pattern PAGE_GETNAME_PATTERN = Pattern.compile( "(wikiContext|context|ctx)\\.getName\\(\\)" );

    private static final Pattern FINDCONTEXT_PATTERN = Pattern.compile( "WikiContext\\.findContext\\((.*?)\\)" );

    private Map<String, HandlerInfo> m_contextMap = new HashMap<String, HandlerInfo>();

    /**
     * {@inheritDoc}
     */
    public void initialize( JspMigrator migrator, Set<Class<? extends ActionBean>> beanClasses, Map<String, Object> sharedState )
    {
        m_contextMap = cacheRequestContexts( beanClasses );
        System.out.println( "Initialized JSPWikiJspTransformer." );
    }

    /**
     * {@inheritDoc}
     */
    public void transform( Map<String, Object> sharedState, JspDocument doc )
    {
        List<Node> nodes = doc.getNodes();

        for( Node node : nodes )
        {
            // For all HTML tags...
            if( node.isHtmlNode() )
            {
                Tag tag = (Tag) node;

                // Check any form or stripes:form elements
                if( "form".equals( tag.getName() ) || "stripes:form".equals( tag.getName() ) )
                {
                    processFormTag( tag );
                }
                else if( "fmt:setBundle".equals( tag.getName() ) )
                {
                    removeSetBundle( tag );
                }

                // Advise user about <input type="hidden"> or <stripes:hidden>
                // tags
                boolean isTypeHidden = false;
                if( tag.getType() != NodeType.END_TAG )
                {
                    isTypeHidden = "stripes:hidden".equals( tag.getName() );
                    if( "input".equals( tag.getName() ) )
                    {
                        Attribute attribute = tag.getAttribute( "type" );
                        isTypeHidden = "hidden".equals( attribute.getValue() );
                    }
                    if( isTypeHidden )
                    {
                        String paramName = tag.hasAttribute( "name" ) ? tag.getAttribute( "name" ).getValue() : null;
                        String paramValue = tag.hasAttribute( "value" ) ? tag.getAttribute( "value" ).getValue() : null;
                        if( paramName != null && paramValue != null )
                        {
                            message( tag, "NOTE: hidden form input sets parameter " + paramName + "=\"" + paramValue
                                          + "\". This should probably correspond to a Stripes ActionBean getter/settter. Refactor?" );
                        }
                    }
                }

                // Tell user about <wiki:Messages> tags.
                if( "wiki:Messages".equals( tag.getName() ) )
                {
                    message( tag,
                             "Consider using <stripes:errors> tags instead of <wiki:Messages> for displaying validation errors." );
                }
            }

            // Look for WikiEngine.createContext() statements, and add matching
            // <stripes:useActionBean> tag
            else if( node.getType() == NodeType.JSP_DECLARATION || 
                         node.getType() == NodeType.SCRIPTLET || 
                         node.getType() == NodeType.JSP_EXPRESSION ||
                         node.getType() == NodeType.CDATA )
            {
                String scriptlet = node.getValue();
                Matcher m = CONTEXT_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String context = m.group( 1 ).trim(); // EDIT, COMMENT
                                                            // etc.
                    HandlerInfo handler = m_contextMap.get( context );
                    if( handler != null )
                    {
                        // Add the <stripes:useActionBean> tag
                        addUseActionBeanTag( doc, handler.getActionBeanClass(), handler.getEventName() );

                        // Now add the Stripes taglib declaration
                        if( StripesJspTransformer.addStripesTaglib( doc ) )
                        {
                            message( doc.getRoot(), "Added Stripes taglib directive." );
                        }
                    }
                }

                // Remove any WikiContext.hasAccess() statements
                m = HASACCESS_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String hasAccess = m.group( 0 );
                    scriptlet = scriptlet.replace( hasAccess, "" );
                    node.setValue( scriptlet );
                    message( node, "Removed WikiContext.hasAccess() statement." );
                }

                // Change WikiContext.getName() to
                // WikiContext.getPage().getName();
                m = PAGE_GETNAME_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String getName = m.group( 0 );
                    String ctx = m.group( 1 ).trim();
                    scriptlet = scriptlet.replace( getName, ctx + ".getPage().getName()" );
                    node.setValue( scriptlet );
                    message( node, "Changed WikiContext.getName() statement to WikiContext.getPage().getName()." );
                }

                // Change WikiContext.findContext() to
                // WikiContextFactory.findContext()
                m = FINDCONTEXT_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String findContext = m.group( 0 );
                    String ctx = m.group( 1 ).trim();
                    scriptlet = scriptlet.replace( findContext, "WikiContextFactory.findContext( " + ctx + " )" );
                    node.setValue( scriptlet );
                    message( node, "Changed WikiContext.findContext() statement to WikiContextFactory.findContext()." );

                    // Make sure we have a page import statement!
                    doc.addPageImportDirective( WikiContextFactory.class );
                }
                
            }
            
            // Make sure we have imports for any classes that moved
            verifyImports( doc, SearchResult.class, FileUtil.class, TextUtil.class );
            
        }
    }

    /**
     * Verifies that JSP page imports are available for a variable array of Classes.
     * If a class does not have a corresponding import (either for the class
     * specifically or for its enclosing package), one will be added.
     * @param doc the JspDocument to check
     * @param clazzes the classes to verify imports for
     */
    private void verifyImports( JspDocument doc, Class... clazzes )
    {
        // Build the regex Pattern to search for, and a lookup Map
        Map<String,Class> classNames = new HashMap<String,Class>();
        StringBuilder s = new StringBuilder();
        s.append( '(' );
        for ( Class clazz : clazzes )
        {
            classNames.put( clazz.getSimpleName(), clazz );
            s.append( clazz.getSimpleName() );
            s.append( '|' );
        }
        s.deleteCharAt( s.length() - 1 );
        s.append( ')' );
        Pattern searchPattern = Pattern.compile( s.toString() );

        // Iterate through each script node and look for the classes that match
        List<Node> nodes = doc.getScriptNodes();
        for ( Node node : nodes )
        {
            Matcher m = searchPattern.matcher( node.getValue() );
            while( m.find() )
            {
                String found= m.group( 1 ).trim();
                Class foundClass = classNames.get( found );
                if ( foundClass != null )
                {
                    if ( doc.addPageImportDirective( foundClass ) )
                    {
                        message( node, "Added page import for " + foundClass + "." );
                    }
                }
            }
        }
    }

    private void addUseActionBeanTag( JspDocument doc, Class<? extends ActionBean> beanClass, String event )
    {
        // If UseActionBean tag already added, bail
        List<Node> nodes = doc.getNodes();
        for( Node node : nodes )
        {
            if( "stripes:useActionBean".equals( node.getName() ) )
            {
                return;
            }
        }

        // Create Tag
        Tag tag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        tag.setName( "stripes:useActionBean" );
        tag.addAttribute( new Attribute( doc, "beanclass", beanClass.getName() ) );
        if( event != null )
        {
            tag.addAttribute( new Attribute( doc, "event", event ) );
        }
        tag.addAttribute( new Attribute( doc, "id", "wikiActionBean" ) );

        // Create linebreak
        Text linebreak = new Text( doc );
        linebreak.setValue( System.getProperty( "line.separator" ) );
        Node root = doc.getRoot();
        linebreak.setParent( root );

        // Figure out where to put it
        List<Node> directives = doc.getNodes( NodeType.JSP_DIRECTIVE );
        if( directives.size() == 0 )
        {
            root.addChild( linebreak, 0 );
            root.addChild( tag, 0 );
        }
        else
        {
            Node lastDirective = directives.get( directives.size() - 1 );
            lastDirective.addSibling( tag );
            lastDirective.addSibling( linebreak );
        }
        message( doc.getRoot(), "Added <stripes:useActionBean beanclass=\"" + beanClass.getName() + "\" event=\"" + event + "\" />" );
    }

    /**
     * Removes the &lt;fmt:setBundle&gt; tag and advises the user.
     * 
     * @param tag the tag to remove
     */
    private void removeSetBundle( Tag tag )
    {
        Node parent = tag.getParent();
        parent.removeChild( tag );
        message( tag, "Removed <fmt:setBundle> tag because it is automatically set in web.xml." );
    }

    /**
     * For &lt;form&gt; and &lt;stripes:form&gt; tags, changes
     * <code>accept-charset</code> or <code>acceptcharset</code> attribute
     * value to "UTF-8", and removes any <code>onsubmit</code> function calls.
     * 
     * @param tag the form tag
     */
    private void processFormTag( Tag tag )
    {
        // Change "accept-charset" or "acceptcharset" values to UTF-8
        Attribute attribute = tag.getAttribute( "accept-charset" );
        if( attribute == null )
        {
            attribute = tag.getAttribute( "acceptcharset" );
        }
        if( attribute != null )
        {
            message( attribute, "Changed value to \"UTF-8\"." );
            attribute.setValue( "UTF-8" );
        }

        // Remove onsubmit() attribute and warn the user
        attribute = tag.getAttribute( "onsubmit" );
        if( attribute != null )
        {
            String value = attribute.getValue();
            message( attribute, "Removed JavaScript call \"" + value + "\". REASON: it probably does not work with Stripes." );
            tag.removeAttribute( attribute );
        }
    }

    /**
     * Using introspection, creates a cached Map of with request context field
     * names as keys, and ActionBean classes as values.
     */
    @SuppressWarnings( "unchecked" )
    private Map<String, HandlerInfo> cacheRequestContexts( Set<Class<? extends ActionBean>> beanClasses )
    {
        // Create a map with of all String constant; key: constant value, value:
        // constant name
        // e.g., "login", "LOGIN"
        Map<String, String> fields = new HashMap<String, String>();
        for( Field field : WikiContext.class.getDeclaredFields() )
        {
            if( String.class.equals( field.getType() ) )
            {
                String fieldName = field.getName();
                String fieldValue = null;
                try
                {
                    fieldValue = (String) field.get( null );
                    fields.put( fieldValue, fieldName );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }

        // Match WikiRequestContext annotations with WikiContext field values
        Map<String, HandlerInfo> contextMap = new HashMap<String, HandlerInfo>();
        for( Class<? extends ActionBean> beanClass : beanClasses )
        {
            Collection<HandlerInfo> handlers = HandlerInfo.getHandlerInfoCollection( (Class<? extends WikiActionBean>) beanClass )
                .values();

            for( HandlerInfo handler : handlers )
            {
                String eventName = handler.getRequestContext();
                String fieldName = fields.get( eventName );
                if( fieldName != null )
                {
                    contextMap.put( fieldName, handler );
                }
            }
        }
        return contextMap;
    }

}
