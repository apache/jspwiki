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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;


import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.UrlBinding;

/**
 * Transforms a JspDocument from standard JSP markup to Stripes markup.
 */
public class StripesJspTransformer extends AbstractJspTransformer
{
    private static final String STRIPES_TAGLIB_URI = "http://stripes.sourceforge.net/stripes.tld";
    
    private Map<Class<? extends ActionBean>, Set<String>> beanProperties = new HashMap<Class<? extends ActionBean>, Set<String>>();

    private Map<String, Class<? extends ActionBean>> beanBindings = new HashMap<String, Class<? extends ActionBean>>();
    
    private boolean migrateForms = false;
    
    /**
     * {@inheritDoc}
     */
    public void initialize( JspMigrator migrator, Set<Class<? extends ActionBean>> beanClasses, Map<String, Object> sharedState )
    {
        // What features should we use?
        migrateForms = migrator.getFeature( JspMigrator.MIGRATE_FORMS );
        
        // Fetch the URL bindings
        initUrlBindingCache( beanClasses );

        // Initialize properties that "should" bind to each class
        initActionBeanPropertyCache( beanClasses );

        System.out.println( "Initialized StripesJspTransformer." );
    }

    /**
     * Using introspection, creates a Map of key/value pairs where the key is an {@link net.sourceforge.stripes.action.UrlBinding} annotation
     * value, and the corresponding value is the {@link net.sourceforge.stripes.action.ActionBean} classes it annotates.
     * @param beanClasses the set of ActionBean classes to inspect
     */
    private void initUrlBindingCache( Set<Class<? extends ActionBean>> beanClasses )
    {
        for( Class<? extends ActionBean> beanClass : beanClasses )
        {
            UrlBinding binding = beanClass.getAnnotation( UrlBinding.class );
            if( binding != null && binding.value() != null )
            {
                beanBindings.put( binding.value(), beanClass );
            }
        }
    }

    /**
     * Using introspection, creates a Map of key/value pairs where the key is an ActionBean class, and its corresponding
     * value is a Set of property names the bean has (as Strings).
     * @param beanClasses the set of ActionBean classes to inspect
     */
    private void initActionBeanPropertyCache( Set<Class<? extends ActionBean>> beanClasses )
    {
        for( Class<? extends ActionBean> beanClass : beanClasses )
        {
            PropertyDescriptor[] pds;
            Set<String> properties = new HashSet<String>();
            try
            {
                pds = Introspector.getBeanInfo( beanClass ).getPropertyDescriptors();
                for( PropertyDescriptor pd : pds )
                {
                    String propertyName = pd.getName();
                    boolean hasSetter = pd.getWriteMethod() != null;
                    boolean hasField = false;
                    try
                    {
                        Field field = beanClass.getDeclaredField( propertyName );
                        hasField = (field != null);
                    }
                    catch( NoSuchFieldException e )
                    {
                    }
                    if( hasSetter || hasField )
                    {
                        properties.add( propertyName );
                    }
                }
                if( properties.size() > 0 )
                {
                    beanProperties.put( beanClass, properties );
                }
            }
            catch( IntrospectionException e )
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void transform( Map<String, Object> sharedState, JspDocument doc )
    {
        boolean migrated = false;

        // Process HTML nodes
        List<Node> nodes = doc.getNodes();
        for( Node node : nodes )
        {
            // For all HTML tags...
            if( node.isHtmlNode() )
            {
                Tag tag = (Tag) node;

                // Change <form> to <stripes:form>
                if( migrateForms && "form".equals( tag.getName() ) )
                {
                    migrated = migrateFormTag( tag ) || migrated;
                }

                // Change <input type="*"> tags to <stripes:*>
                else if( migrateForms && "input".equals( tag.getName() ) )
                {
                    migrated = migrateInputTag( tag ) || migrated;
                }

                // Change <textarea> to <stripes:textarea>
                else if( migrateForms && "textarea".equals( tag.getName() ) )
                {
                    migrated = migrateTextArea( tag ) || migrated;
                }

                else if( migrateForms && "label".equals( tag.getName() ) )
                {
                    migrated = migrateLabel( tag ) || migrated;
                }

                // Remove any <fmt:setLocale> tags (and their children)
                else if( "fmt:setLocale".equals( tag.getName() ) )
                {
                    removeSetLocale( tag );
                }
            }
        }

        // If we did any work here, add Stripes taglib entry
        if( migrated )
        {
            if ( addStripesTaglib( doc ) )
            {
                message( doc.getRoot(), "Added Stripes taglib directive." );
            }
        }
    }

    /**
     * Verifies the presence of the Stripes taglib directive, and adds it to the
     * JspDocument if not present. For the taglib to be considered "present,"
     * the JspDocument must contain a JSP <code>taglib</code> directive with
     * the <code>prefix</code> set to <code>stripes</code>; any
     * <code>uri</code> value is acceptable. If the taglib declaration is
     * added, it is given the prefix <code>stripes</code> and the URI
     * <code>http://stripes.sourceforge.net/stripes.tld</code>.
     * 
     * @param doc the JspDocument to process
     * @return <code>true</code> if the Stripes taglib declaration was
     * actually added, <code>false</code> if it already exists
     */
    static protected boolean addStripesTaglib( JspDocument doc )
    {
        // Add the Stripes taglib declaration if it's not there already
        List<Tag> nodes = doc.getTaglibDirective( "*", "stripes" );
        if( nodes.size() == 0 )
        {
            doc.addTaglibDirective( STRIPES_TAGLIB_URI, "stripes" );
            return true;
        }
        for ( Tag tag : nodes )
        {
            Attribute attr = tag.getAttribute( "uri" );
            if ( attr == null )
            {
                tag.addAttribute( new Attribute( doc, "uri", STRIPES_TAGLIB_URI ) );
            }
            else if ( !STRIPES_TAGLIB_URI.equals( attr.getValue() ) )
            {
                attr.setValue( STRIPES_TAGLIB_URI );
            }
        }
        return true;
    }

    /**
     * <p>
     * Migrates an existing &lt;label&gt; element to &lt;stripes:label&gt;.
     * Migration can happen in two ways:
     * </p>
     * <ul>
     * <li>If the label tag has an a single <code>fmt:message</code> child
     * element whose <code>key</code> attribute contains a value, that value
     * will become the <code>name</code> attribute of the
     * <code>stripes:label</code>element. </li>
     * <li>In all other cases, the <code>label</code> element is simply
     * re-named to <code>stripes:label</code>.</li>
     * </ul>
     * <p>
     * For example, the ordinary HTML tag <code>&lt;label
     * for="assertedName"&gt;&lt;fmt:message
     * key="prefs.assertedname"/&gt;&lt;/label&gt;</code>
     * will be migrated to &lt;stripes:label name="prefs.assertedname"
     * for="assertedName"&gt;.
     * </p>
     * 
     * @param tag
     * @return
     */
    private boolean migrateLabel( Tag tag )
    {
        // Change the name to <stripes:label>
        tag.setName( "stripes:label" );

        // If child fmt:message, pull into element
        migrateMessageTag( tag );
        message( tag, "Changed <label> to <stripes:label>." );

        return true;
    }

    private boolean migrateMessageTag( Tag tag )
    {
        // Not a start tag, we're done
        if( tag.getType() != NodeType.START_TAG )
        {
            return false;
        }

        // Do we have children <fmt:message/> or <fmt:message></fmt:message>?
        List<Node> children = tag.getChildren();
        boolean hasOneTag = children.size() == 1 && children.get( 0 ).getType() == NodeType.EMPTY_ELEMENT_TAG
                            && "fmt:message".equals( children.get( 0 ).getName() );
        boolean hasTwoTags = children.size() == 2 && children.get( 0 ).getType() == NodeType.START_TAG
                             && children.get( 1 ).getType() == NodeType.END_TAG
                             && "fmt.message".equals( children.get( 0 ).getName() )
                             && "fmt.message".equals( children.get( 1 ).getName() );

        if( ( hasOneTag || hasTwoTags ) )
        {
            if ( tag.hasAttribute( "name" ) )
            {
                message( children.get( 0 ), "NOTE: did not migrate <fmt:message> tag because parent <" + tag.getName() + "> already has 'name' attribute. Refactor?" );
            }
            else
            {
                // Move the fmt:message tag's key attribute to stripes:label
                // name
                Tag message = (Tag) children.get( 0 );
                if( message.hasAttribute( "key" ) )
                {
                    Attribute key = message.getAttribute( "key" );
                    key.setName( "name" );
                    message.removeAttribute( key );
                    tag.addAttribute( key );
                    
                    // Delete all of the children
                    for ( Node child : tag.getChildren() )
                    {
                        tag.removeChild( child );
                    }
                }
                
                // Change parent to an empty end tag
                tag.setType( NodeType.EMPTY_ELEMENT_TAG );

                // Delete the matching end tag
                int i = tag.getParent().getChildren().indexOf( tag );
                Node endTag = tag.getParent().getChildren().get( i + 1 );
                tag.getParent().removeChild( endTag );
                
                // Tell user what we did
                message( tag, "Moved <fmt:message> tag into parent <" + tag.getName() + ">." );
                return true;
            }
        }
        return false;
    }

    /**
     * Migrates the &lt;form&gt; tag.
     * 
     * @param tag the Tag that represents the form tag being processed.
     */
    private boolean migrateFormTag( Tag tag )
    {
        message( tag, "Changed name to <stripes:form>." );
        tag.setName( "stripes:form" );

        // Change "accept-charset" attribute to acceptcharset
        Node attribute = tag.getAttribute( "accept-charset" );
        if( attribute != null )
        {
            message( attribute, "Changed name to \"acceptcharset\"." );
            attribute.setName( "acceptcharset" );
        }

        // If URL has parameters, add them as child <stripes:param>
        // elements
        // e.g., param in <form action="Login.jsp?tab=profile>
        // becomes <stripes:param name="tab" value="profile">
        attribute = tag.getAttribute( "action" );
        if( attribute != null )
        {
            String actionUrl = attribute.getValue();
            if( actionUrl != null )
            {
                int qmark = actionUrl.indexOf( '?' );
                if( qmark != -1 && qmark < actionUrl.length() - 1 )
                {
                    // Change "action" attribute"
                    String trimmedPath = actionUrl.substring( 0, qmark );
                    message( attribute, "Trimmed value to \"" + trimmedPath + "\"" );
                    attribute.setValue( trimmedPath );

                    // Split the parameters and add a new
                    // <stripes:param> child element for each
                    String[] params = actionUrl.substring( qmark + 1 ).split( "&" );
                    for( String param : params )
                    {
                        if( param.length() > 1 )
                        {
                            JspDocument doc = tag.getJspDocument();
                            String name = param.substring( 0, param.indexOf( '=' ) );
                            String value = param.substring( name.length() + 1 );
                            Tag stripesParam = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
                            stripesParam.setName( "stripes:param" );
                            Attribute nameAttribute = new Attribute( doc );
                            nameAttribute.setName( "name" );
                            nameAttribute.setValue( name );
                            stripesParam.addAttribute( nameAttribute );
                            Attribute valueAttribute = new Attribute( doc );
                            valueAttribute.setName( "value" );
                            valueAttribute.setValue( value );
                            stripesParam.addAttribute( valueAttribute );
                            tag.addChild( stripesParam );
                            message( tag, "Created <stripes:form> child element <stripes:param name=\"" + name + "\"" + " value=\""
                                          + value + "\"/>." );
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Migrates the &lt;input&gt; tag.
     * 
     * @param tag the Tag that represents the form tag being processed.
     */
    private boolean migrateInputTag( Tag tag )
    {
        boolean migrated = false;

        // Move 'type' attribute value to the localname
        Attribute attribute = tag.getAttribute( "type" );
        if( attribute != null )
        {
            // If a submit input, tell user to change the "name"
            // value to something useful for Stripes
            if( "submit".equals( attribute.getValue() ) )
            {
                Node nameAttribute = tag.getAttribute( "name" );
                String nameValue = nameAttribute == null ? "(not set)" : nameAttribute.getName();
                message( nameAttribute, "NOTE: name=\"" + nameValue + "\"" );
            }

            // Move type attribute to qname
            String type = attribute.getValue();
            message( attribute, "Changed <input type=\"" + type + "\"> to <stripes:" + type + ">." );
            tag.setName( "stripes:" + type );
            tag.removeAttribute( attribute );
            migrated = true;
        }

        // If the value attribute contains embedded tags, move to child nodes
        migrated = migrateValueAttribute( tag ) || migrated;

        // If child fmt:message, pull into element
        return migrateMessageTag( tag ) || migrated;
    }

    private boolean migrateTextArea( Tag tag )
    {
        boolean migrated = false;

        // Only migrate textarea if 'name' attribute is present
        Attribute name = tag.getAttribute( "name" );
        if( name != null )
        {
            // Change the name to stripes:textarea
            tag.setName( "stripes:textarea" );
            message( tag, "Changed <textarea> to <stripes:textarea>. NOTE: Stripes will attempt to bind request parameter \""
                          + name.getValue() + "\" to this element." );

            // If the value attribute contains embedded tags, move to child
            // nodes
            migrateValueAttribute( tag );
            migrated = true;
        }
        else
        {
            message( tag, "NOTE: <textarea> did not contain a \"name\" attribute, so it was not migrated." );
        }
        return migrated;
    }

    /**
     * Moves the contents of an HTML form tag with a <code>value</code>
     * attribute to child nodes of the tag, if <code>value</code> contains
     * anything other than a simple text string. If the <code>value</code>
     * attribute is not present, or its contents is a simple text string, this
     * method leaves the attribute as-is.
     * 
     * @param tag the tag to migrate
     * @return <code>true</code> if this method changed the JspDocument, and
     *         <code>false</code> if not
     */
    private boolean migrateValueAttribute( Tag tag )
    {
        boolean migrated = false;

        // If embedded markup in "value" attribute, move to child
        // nodes
        Attribute attribute = tag.getAttribute( "value" );
        if( attribute != null )
        {
            List<Node> attributeNodes = attribute.getChildren();
            if( attributeNodes.size() > 1 || (attributeNodes.size() == 1 && attributeNodes.get( 0 ).getType() != NodeType.TEXT) )
            {
                // Remove the attribute
                tag.removeAttribute( attribute );
                // Move all of the attribute's nodes to the
                // children nodes
                for( Node valueNode : attribute.getChildren() )
                {
                    tag.addChild( valueNode );
                }
                message( attribute, "Moved embedded tag(s) in <" + tag.getName()
                                    + "> \"value\" attribute to the tag body. These are now child element(s)." );
                migrated = true;
            }
        }
        return migrated;
    }

    /**
     * Removes the &lt;fmt:setLocale&gt; tag and advises the user.
     * 
     * @param tag the tag to remove
     */
    private void removeSetLocale( Tag tag )
    {
        Node parent = tag.getParent();
        parent.removeChild( tag );
        message( tag, "Removed <fmt:setLocale> tag because Stripes LocalePicker does this instead." );
    }
}
