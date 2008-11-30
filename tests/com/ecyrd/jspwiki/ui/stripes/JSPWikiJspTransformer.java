package com.ecyrd.jspwiki.ui.stripes;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.stripes.action.ActionBean;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.HandlerInfo;
import com.ecyrd.jspwiki.action.WikiActionBean;

/**
 * Transforms a JspDocument from standard JSP markup to Stripes markup.
 */
public class JSPWikiJspTransformer extends AbstractJspTransformer
{
    private static final Pattern CONTEXT_PATTERN = Pattern.compile( "\\.createContext\\(.*?WikiContext.([A-Z]*?)\\s*\\);" );

    private Map<String,HandlerInfo> m_contextMap = new HashMap<String,HandlerInfo>();

    /**
     * {@inheritDoc}
     */
    public void initialize( Set<Class<? extends ActionBean>> beanClasses, Map<String, Object> sharedState )
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

                // Advise user about <input type="hidden"> or <stripes:hidden> tags
                boolean isTypeHidden = false;
                if ( tag.getType() != NodeType.END_TAG )
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
                        if ( paramName != null && paramValue != null )
                        {
                            message( tag, "NOTE: hidden form input sets parameter " + paramName
                                     + "=\"" + paramValue + "\". This should probably correspond to a Stripes ActionBean getter/settter. Refactor?" );
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

            // Look for WikiEngine.createContext() statements, and add matching <stripes:useActionBean> tag
            else if ( node.getType() == NodeType.JSP_DECLARATION || node.getType() == NodeType.SCRIPTLET )
            {
                String scriptlet = node.getValue();
                Matcher m = CONTEXT_PATTERN.matcher( scriptlet );
                if (m.find()) {
                    String context = m.group(1).trim();     // EDIT, COMMENT etc.
                    HandlerInfo handler = m_contextMap.get( context );
                    if ( handler != null )
                    {
                        // Add the <stripes:useActionBean> tag
                        addUseActionBeanTag( doc, handler.getActionBeanClass(), handler.getEventName() );
                        
                        // Now add the Stripes taglib declaration
                        if ( StripesJspTransformer.addStripesTaglib( doc ) )
                        {
                            message( doc.getRoot(), "Added Stripes taglib directive." );
                        }
                    }
                }
                
            }
        }
    }

    private void addUseActionBeanTag( JspDocument doc, Class<? extends ActionBean> beanClass, String event )
    {
        // Create Tag
        Tag tag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        tag.setName( "stripes:useActionBean" );
        tag.addAttribute( new Attribute( doc, "beanClass", beanClass.getName() ) );
        if ( event != null )
        {
            tag.addAttribute( new Attribute( doc, "event", event ) );
        }
        
        // Create linebreak
        Text linebreak = new Text( doc );
        linebreak.setValue( System.getProperty( "line.separator" ) );
        Node root = doc.getRoot();
        linebreak.setParent( root );
        
        // Figure out where to put it
        List<Node> directives = doc.getNodes( NodeType.JSP_DIRECTIVE );
        if ( directives.size() == 0 )
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
        message( doc.getRoot(), "Added <stripes:useActionBean beanClass=\"" + beanClass.getName() + "\" event=\"" + event + "\" />" );
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
     * Using introspection, creates a cached Map of with request context field names as keys, and ActionBean classes as values.
     */
    @SuppressWarnings("unchecked")
    private Map<String,HandlerInfo> cacheRequestContexts( Set<Class<? extends ActionBean>> beanClasses )
    {
        // Create a map with of all String constant; key: constant value, value: constant name
        // e.g., "login", "LOGIN"
        Map<String,String> fields = new HashMap<String,String>(); 
        for ( Field field : WikiContext.class.getDeclaredFields() )
        {
            if ( String.class.equals( field.getType() ) )
            {
                String fieldName = field.getName();
                String fieldValue = null;
                try
                {
                    fieldValue = (String)field.get( null );
                    fields.put( fieldValue, fieldName );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }
        
        // Match WikiRequestContext annotations with WikiContext field values
        Map<String,HandlerInfo> contextMap = new HashMap<String,HandlerInfo>();
        for ( Class<? extends ActionBean> beanClass : beanClasses )
        {
            Collection<HandlerInfo> handlers = HandlerInfo.getHandlerInfoCollection( (Class<? extends WikiActionBean>)beanClass ).values();
            
            for ( HandlerInfo handler : handlers )
            {
                String eventName = handler.getRequestContext();
                String fieldName = fields.get( eventName );
                if ( fieldName != null )
                {
                    contextMap.put( fieldName, handler );
                }
            }
        }
        return contextMap;
    }

}
