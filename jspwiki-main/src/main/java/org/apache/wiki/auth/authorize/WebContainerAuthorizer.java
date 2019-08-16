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
package org.apache.wiki.auth.authorize;

import org.apache.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Authorizes users by delegating role membership checks to the servlet
 * container. In addition to implementing methods for the
 * <code>Authorizer</code> interface, this class also provides a convenience
 * method {@link #isContainerAuthorized()} that queries the web application
 * descriptor to determine if the container manages authorization.
 * @since 2.3
 */
public class WebContainerAuthorizer implements WebAuthorizer  {

    private static final String J2EE_SCHEMA_25_NAMESPACE = "http://xmlns.jcp.org/xml/ns/javaee";

    protected static final Logger log                   = Logger.getLogger( WebContainerAuthorizer.class );

    protected WikiEngine          m_engine;

    /**
     * A lazily-initialized array of Roles that the container knows about. These
     * are parsed from JSPWiki's <code>web.xml</code> web application
     * deployment descriptor. If this file cannot be read for any reason, the
     * role list will be empty. This is a hack designed to get around the fact
     * that we have no direct way of querying the web container about which
     * roles it manages.
     */
    protected Role[]            m_containerRoles      = new Role[0];

    /**
     * Lazily-initialized boolean flag indicating whether the web container
     * protects JSPWiki resources.
     */
    protected boolean           m_containerAuthorized = false;

    private Document            m_webxml = null;

    /**
     * Constructs a new instance of the WebContainerAuthorizer class.
     */
    public WebContainerAuthorizer()
    {
        super();
    }

    /**
     * Initializes the authorizer for.
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     */
    @Override
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
        m_containerAuthorized = false;

        // FIXME: Error handling here is not very verbose
        try
        {
            m_webxml = getWebXml();
            if ( m_webxml != null )
            {
                // Add the J2EE 2.4 schema namespace
                m_webxml.getRootElement().setNamespace( Namespace.getNamespace( J2EE_SCHEMA_25_NAMESPACE ) );

                m_containerAuthorized = isConstrained( "/Delete.jsp", Role.ALL )
                        && isConstrained( "/Login.jsp", Role.ALL );
            }
            if ( m_containerAuthorized )
            {
                m_containerRoles = getRoles( m_webxml );
                log.info( "JSPWiki is using container-managed authentication." );
            }
            else
            {
                log.info( "JSPWiki is using custom authentication." );
            }
        }
        catch ( IOException e )
        {
            log.error("Initialization failed: ",e);
            throw new InternalWikiException( e.getClass().getName()+": "+e.getMessage() , e);
        }
        catch ( JDOMException e )
        {
            log.error("Malformed XML in web.xml",e);
            throw new InternalWikiException( e.getClass().getName()+": "+e.getMessage() , e);
        }

        if ( m_containerRoles.length > 0 )
        {
            String roles = "";
            for( Role containerRole : m_containerRoles )
            {
                roles = roles + containerRole + " ";
            }
            log.info( " JSPWiki determined the web container manages these roles: " + roles );
        }
        log.info( "Authorizer WebContainerAuthorizer initialized successfully." );
    }

    /**
     * Determines whether a user associated with an HTTP request possesses
     * a particular role. This method simply delegates to
     * {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)}
     * by converting the Principal's name to a String.
     * @param request the HTTP request
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     */
    @Override
    public boolean isUserInRole( HttpServletRequest request, Principal role )
    {
        return request.isUserInRole( role.getName() );
    }

    /**
     * Determines whether the Subject associated with a WikiSession is in a
     * particular role. This method takes two parameters: the WikiSession
     * containing the subject and the desired role ( which may be a Role or a
     * Group). If either parameter is <code>null</code>, this method must
     * return <code>false</code>.
     * This method simply examines the WikiSession subject to see if it
     * possesses the desired Principal. We assume that the method
     * {@link org.apache.wiki.ui.WikiServletFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
     * previously executed, and that it has set the WikiSession
     * subject correctly by logging in the user with the various login modules,
     * in particular {@link org.apache.wiki.auth.login.WebContainerLoginModule}}.
     * This is definitely a hack,
     * but it eliminates the need for WikiSession to keep dangling
     * references to the last WikiContext hanging around, just
     * so we can look up the HttpServletRequest.
     *
     * @param session the current WikiSession
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     * @see org.apache.wiki.auth.Authorizer#isUserInRole(org.apache.wiki.WikiSession, java.security.Principal)
     */
    @Override
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        if ( session == null || role == null )
        {
            return false;
        }
        return session.hasPrincipal( role );
    }

    /**
     * Looks up and returns a Role Principal matching a given String. If the
     * Role does not match one of the container Roles identified during
     * initialization, this method returns <code>null</code>.
     * @param role the name of the Role to retrieve
     * @return a Role Principal, or <code>null</code>
     * @see org.apache.wiki.auth.Authorizer#initialize(WikiEngine, Properties)
     */
    @Override
    public Principal findRole( String role )
    {
        for( Role containerRole : m_containerRoles )
        {
            if ( containerRole.getName().equals( role ) )
            {
                return containerRole;
            }
        }
        return null;
    }

    /**
     * <p>
     * Protected method that identifies whether a particular webapp URL is
     * constrained to a particular Role. The resource is considered constrained
     * if:
     * </p>
     * <ul>
     * <li>the web application deployment descriptor contains a
     * <code>security-constraint</code> with a child
     * <code>web-resource-collection/url-pattern</code> element matching the
     * URL, <em>and</em>:</li>
     * <li>this constraint also contains an
     * <code>auth-constraint/role-name</code> element equal to the supplied
     * Role's <code>getName()</code> method. If the supplied Role is Role.ALL,
     * it matches all roles</li>
     * </ul>
     * @param url the web resource
     * @param role the role
     * @return <code>true</code> if the resource is constrained to the role,
     *         <code>false</code> otherwise
     */
    public boolean isConstrained( final String url, final Role role ) {
        final Element root = m_webxml.getRootElement();
        final Namespace jeeNs = Namespace.getNamespace( "j", J2EE_SCHEMA_25_NAMESPACE );

        // Get all constraints that have our URL pattern
        // (Note the crazy j: prefix to denote the 2.4 j2ee schema)
        final String constrainsSelector = "//j:web-app/j:security-constraint[j:web-resource-collection/j:url-pattern=\"" + url + "\"]";
        final List< Element > constraints = XPathFactory.instance()
                                                        .compile( constrainsSelector, Filters.element(), null, jeeNs )
                                                        .evaluate( root );

        // Get all constraints that match our Role pattern
        final String rolesSelector = "//j:web-app/j:security-constraint[j:auth-constraint/j:role-name=\"" + role.getName() + "\"]";
        final List< Element > roles = XPathFactory.instance()
                                                  .compile( rolesSelector, Filters.element(), null, jeeNs )
                                                  .evaluate( root );

        // If we can't find either one, we must not be constrained
        if ( constraints.size() == 0 ) {
            return false;
        }

        // Shortcut: if the role is ALL, we are constrained
        if ( role.equals( Role.ALL ) ) {
            return true;
        }

        // If no roles, we must not be constrained
        if ( roles.size() == 0 ) {
            return false;
        }

        // If a constraint is contained in both lists, we must be constrained
        for ( Iterator< Element > c = constraints.iterator(); c.hasNext(); ) {
            final Element constraint = c.next();
            for ( Iterator< Element > r = roles.iterator(); r.hasNext(); ) {
                final Element roleConstraint = r.next();
                if ( constraint.equals( roleConstraint ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the web container is configured to protect
     * certain JSPWiki resources by requiring authentication. Specifically, this
     * method parses JSPWiki's web application descriptor (<code>web.xml</code>)
     * and identifies whether the string representation of
     * {@link org.apache.wiki.auth.authorize.Role#AUTHENTICATED} is required
     * to access <code>/Delete.jsp</code> and <code>LoginRedirect.jsp</code>.
     * If the administrator has uncommented the large
     * <code>&lt;security-constraint&gt;</code> section of <code>web.xml</code>,
     * this will be true. This is admittedly an indirect way to go about it, but
     * it should be an accurate test for default installations, and also in 99%
     * of customized installs.
     * @return <code>true</code> if the container protects resources,
     *         <code>false</code> otherwise
     */
    public boolean isContainerAuthorized()
    {
        return m_containerAuthorized;
    }

    /**
     * Returns an array of role Principals this Authorizer knows about.
     * This method will return an array of Role objects corresponding to
     * the logical roles enumerated in the <code>web.xml</code>.
     * This method actually returns a defensive copy of an internally stored
     * array.
     * @return an array of Principals representing the roles
     */
    @Override
    public Principal[] getRoles()
    {
        return m_containerRoles.clone();
    }

    /**
     * Protected method that extracts the roles from JSPWiki's web application
     * deployment descriptor. Each Role is constructed by using the String
     * representation of the Role, for example
     * <code>new Role("Administrator")</code>.
     * @param webxml the web application deployment descriptor
     * @return an array of Role objects
     */
    protected Role[] getRoles( final Document webxml ) {
        final Set<Role> roles = new HashSet<>();
        final Element root = webxml.getRootElement();
        final Namespace jeeNs = Namespace.getNamespace( "j", J2EE_SCHEMA_25_NAMESPACE );

        // Get roles referred to by constraints
        final String constrainsSelector = "//j:web-app/j:security-constraint/j:auth-constraint/j:role-name";
        final List< Element > constraints = XPathFactory.instance()
                                                        .compile( constrainsSelector, Filters.element(), null, jeeNs )
                                                        .evaluate( root );
        for( final Iterator< Element > it = constraints.iterator(); it.hasNext(); ) {
            final String role = ( it.next() ).getTextTrim();
            roles.add( new Role( role ) );
        }

        // Get all defined roles
        final String rolesSelector = "//j:web-app/j:security-role/j:role-name";
        final List< Element > nodes = XPathFactory.instance()
                                                  .compile( rolesSelector, Filters.element(), null, jeeNs )
                                                  .evaluate( root );
        for( final Iterator< Element > it = nodes.iterator(); it.hasNext(); ) {
            final String role = ( it.next() ).getTextTrim();
            roles.add( new Role( role ) );
        }

        return roles.toArray( new Role[roles.size()] );
    }

    /**
     * Returns an {@link org.jdom2.Document} representing JSPWiki's web
     * application deployment descriptor. The document is obtained by calling
     * the servlet context's <code>getResource()</code> method and requesting
     * <code>/WEB-INF/web.xml</code>. For non-servlet applications, this
     * method calls this class'
     * {@link ClassLoader#getResource(java.lang.String)} and requesting
     * <code>WEB-INF/web.xml</code>.
     * @return the descriptor
     * @throws IOException if the deployment descriptor cannot be found or opened
     * @throws JDOMException if the deployment descriptor cannot be parsed correctly
     */
    protected Document getWebXml() throws JDOMException, IOException
    {
        URL url;
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory( XMLReaders.NONVALIDATING );
        builder.setEntityResolver( new LocalEntityResolver() );
        Document doc = null;
        if ( m_engine.getServletContext() == null )
        {
            ClassLoader cl = WebContainerAuthorizer.class.getClassLoader();
            url = cl.getResource( "WEB-INF/web.xml" );
            if( url != null )
                log.info( "Examining " + url.toExternalForm() );
        }
        else
        {
            url = m_engine.getServletContext().getResource( "/WEB-INF/web.xml" );
            if( url != null )
                log.info( "Examining " + url.toExternalForm() );
        }
        if( url == null )
            throw new IOException("Unable to find web.xml for processing.");

        log.debug( "Processing web.xml at " + url.toExternalForm() );
        doc = builder.build( url );
        return doc;
    }

    /**
     * <p>XML entity resolver that redirects resolution requests by JDOM, JAXP and
     * other XML parsers to locally-cached copies of the resources. Local
     * resources are stored in the <code>WEB-INF/dtd</code> directory.</p>
     * <p>For example, Sun Microsystem's DTD for the webapp 2.3 specification is normally
     * kept at <code>http://java.sun.com/dtd/web-app_2_3.dtd</code>. The
     * local copy is stored at <code>WEB-INF/dtd/web-app_2_3.dtd</code>.</p>
     */
    public class LocalEntityResolver implements EntityResolver
    {
        /**
         * Returns an XML input source for a requested external resource by
         * reading the resource instead from local storage. The local resource path
         * is <code>WEB-INF/dtd</code>, plus the file name of the requested
         * resource, minus the non-filename path information.
         * @param publicId the public ID, such as
         *            <code>-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN</code>
         * @param systemId the system ID, such as
         *            <code>http://java.sun.com/dtd/web-app_2_3.dtd</code>
         * @return the InputSource containing the resolved resource
         * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
         *      java.lang.String)
         * @throws SAXException if the resource cannot be resolved locally
         * @throws IOException if the resource cannot be opened
         */
        @Override
        public InputSource resolveEntity( String publicId, String systemId ) throws SAXException, IOException
        {
            String file = systemId.substring( systemId.lastIndexOf( '/' ) + 1 );
            URL url;
            if ( m_engine.getServletContext() == null )
            {
                ClassLoader cl = WebContainerAuthorizer.class.getClassLoader();
                url = cl.getResource( "WEB-INF/dtd/" + file );
            }
            else
            {
                url = m_engine.getServletContext().getResource( "/WEB-INF/dtd/" + file );
            }

            if( url != null )
            {
                InputSource is = new InputSource( url.openStream() );
                log.debug( "Resolved systemID=" + systemId + " using local file " + url );
                return is;
            }

            //
            //  Let's fall back to default behaviour of the container, and let's
            //  also let the user know what is going on.  This caught me by surprise
            //  while running JSPWiki on an unconnected laptop...
            //
            //  The DTD needs to be resolved and read because it contains things like
            //  entity definitions...
            //
            log.info("Please note: There are no local DTD references in /WEB-INF/dtd/"+file+"; falling back to default behaviour."+
                     " This may mean that the XML parser will attempt to connect to the internet to find the DTD."+
                     " If you are running JSPWiki locally in an unconnected network, you might want to put the DTD files in place to avoid nasty UnknownHostExceptions.");


            // Fall back to default behaviour
            return null;
        }
    }

}
