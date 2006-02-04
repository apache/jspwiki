package com.ecyrd.jspwiki.auth.authorize;

import java.io.InputStream;
import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Authorizes users by delegating role membership checks to the servlet
 * container. In addition to implementing methods for the
 * <code>Authorizer</code> interface, this class also provides a convenience
 * method {@link #isContainerAuthorized()} that queries the web application
 * descriptor to determine if the container manages authorization.
 * @author Andrew Jaquith
 * @version $Revision: 1.12 $ $Date: 2006-02-04 21:34:38 $
 * @since 2.3
 */
public class WebContainerAuthorizer implements Authorizer
{
    private static final Logger log                   = Logger.getLogger( WebContainerAuthorizer.class );

    private WikiEngine          m_engine;

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
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
        m_containerAuthorized = false;
        Document webxml = getWebXml();
        if ( webxml != null )
        {
            m_containerAuthorized = isConstrained( webxml, "/Delete.jsp", Role.ALL )
                    && isConstrained( webxml, "/Login.jsp", Role.ALL );
        }
        if ( m_containerAuthorized )
        {
            m_containerRoles = getRoles( webxml );
            log.info( "JSPWiki is using container-managed authentication." );
        }
        else
        {
            log.info( "JSPWiki is using custom authentication." );
        }
        
        if ( m_containerRoles.length > 0 )
        {
            String roles = "";
            for( int i = 0; i < m_containerRoles.length; i++ )
            {
                roles = roles + m_containerRoles[i] + " ";
            }
            log.info( " JSPWiki determined the web container manages these roles: " + roles );
        }
        log.info( "Authorizer WebContainerAuthorizer initialized successfully." );
    }

    /**
     * Determines whether the Subject associated with a WikiSession is in a
     * particular role. This method takes two parameters: the WikiSession
     * containing the subject and the desired role ( which may be a Role or a
     * Group). If either parameter is <code>null</code>, this method must
     * return <code>false</code>. 
     * This method extracts the last
     * 
     * @param session the current WikiSession
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     * @see com.ecyrd.jspwiki.auth.Authorizer#isUserInRole(com.ecyrd.jspwiki.WikiSession, java.security.Principal)
     */
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        WikiContext context = session.getLastContext();
        if ( context == null || role == null )
        {
            return false;
        }
        HttpServletRequest request = context.getHttpRequest();
        if ( request == null )
        {
            return false;
        }
        return request.isUserInRole( role.getName() );
    }

    /**
     * Looks up and returns a Role Principal matching a given String. If the
     * Role does not match one of the container Roles identified during
     * initialization, this method returns <code>null</code>.
     * @param role the name of the Role to retrieve
     * @return a Role Principal, or <code>null</code>
     * @see com.ecyrd.jspwiki.auth.Authorizer#initialize(WikiEngine, Properties)
     */
    public Principal findRole( String role )
    {
        for( int i = 0; i < m_containerRoles.length; i++ )
        {
            if ( m_containerRoles[i].getName().equals( role ) )
            {
                return m_containerRoles[i];
            }
        }
        return null;
    }

    /**
     * Returns <code>true</code> if the web container is configured to protect
     * certain JSPWiki resources by requiring authentication. Specifically, this
     * method parses JSPWiki's web application descriptor (<code>web.xml</code>)
     * and identifies whether the string representation of
     * {@link com.ecyrd.jspwiki.auth.authorize.Role#AUTHENTICATED} is required
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
    public Principal[] getRoles()
    {
        return (Principal[]) m_containerRoles.clone();
    }

    /**
     * Protected method that extracts the roles from JSPWiki's web application
     * deployment descriptor. Each Role is constructed by using the String
     * representation of the Role, for example
     * <code>new Role("Administrator")</code>.
     * @param webxml the web application deployment descriptor
     * @return an array of Role objects
     */
    protected Role[] getRoles( Document webxml )
    {
        Set roles = new HashSet();
        Element root = webxml.getDocumentElement();
        NodeList nodes = root.getElementsByTagName( "role-name" );
        for( int i = 0; i < nodes.getLength(); i++ )
        {
            String role = ( (Element) nodes.item( i ) ).getFirstChild().getNodeValue();
            roles.add( new Role( role ) );
        }
        return (Role[]) roles.toArray( new Role[roles.size()] );
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
     * @param webxml the web application deployment descriptor
     * @param url the web resource
     * @param role the role
     * @return <code>true</code> if the resource is constrained to the role,
     *         <code>false</code> otherwise
     */
    
    // FIXME: This would be quite a lot nicer if done with XPath
    
    protected boolean isConstrained( Document webxml, String url, Role role )
    {
        // Loop through constraints, looking for our pattern
        boolean constrained = false;
        boolean rolematch = false;
        Element root = webxml.getDocumentElement();
        NodeList constraints = root.getElementsByTagName( "security-constraint" );
        for( int i = 0; i < constraints.getLength(); i++ )
        {
            // See if the URL pattern is in the list of constrained resources
            Element constraint = (Element) constraints.item( i );
            NodeList resources = constraint.getElementsByTagName( "web-resource-collection" );
            constraintSearch: for( int j = 0; j < resources.getLength(); j++ )
            {
                Element resource = (Element) resources.item( j );
                NodeList patterns = resource.getElementsByTagName( "url-pattern" );
                for( int k = 0; k < patterns.getLength(); k++ )
                {
                    Element pattern = (Element) patterns.item( k );
                    String patternUrl = pattern.getFirstChild().getNodeValue();
                    if ( patternUrl != null && patternUrl.trim().equals( url ) )
                    {
                        constrained = true;
                        break constraintSearch;
                    }
                }
            }
            // See if our role is constained
            if ( role.equals(Role.ALL) )
            {
                rolematch = true;
            }
            else
            {
                NodeList authConstraints = constraint.getElementsByTagName( "auth-constraint" );
                {
                    roleSearch: for( int j = 0; j < authConstraints.getLength(); j++ )
                    {
                        Element authConstraint = (Element) authConstraints.item( j );
                        NodeList roleNames = authConstraint.getElementsByTagName( "role-name" );
                        for( int k = 0; k < roleNames.getLength(); k++ )
                        {
                            Element roleName = (Element) roleNames.item( k );
                            String roleValue = roleName.getFirstChild().getNodeValue();
                            if ( roleValue != null && roleValue.trim().equals( role.getName() ) )
                            {
                                rolematch = true;
                                break roleSearch;
                            }
                        }
                    }
                }
            }
        }
        return ( constrained && rolematch );
    }

    /**
     * Returns a {@link org.w3c.dom.Document} representing JSPWiki's web
     * application deployment descriptor. The document is obtained by calling
     * the servlet context's <code>getResource()</code> method and requesting
     * <code>/WEB-INF/web.xml</code>. For non-servlet applications, this
     * method calls this class'
     * {@link ClassLoader#getResource(java.lang.String)} and requesting
     * <code>WEB-INF/web.xml</code>.
     * @return the descriptor
     */
    protected Document getWebXml()
    {
        URL url;
        Document doc = null;
        try
        {
            if ( m_engine.getServletContext() == null )
            {
                ClassLoader cl = WebContainerAuthorizer.class.getClassLoader();
                url = cl.getResource( "WEB-INF/web.xml" );
                log.info( "Examining " + url.toExternalForm() );
            }
            else
            {
                url = m_engine.getServletContext().getResource( "/WEB-INF/web.xml" );
                log.info( "Examining " + url.toExternalForm() );
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating( false );
            InputStream is = url.openStream();
            doc = factory.newDocumentBuilder().parse( is );
        }
        catch( Exception e )
        {
            log.error( "Cannot parse web.xml: " + e.getMessage() );
            throw new RuntimeException( "Cannot parse web.xml: " + e.getMessage() );
        }
        return doc;
    }

}