package com.ecyrd.jspwiki.auth.authorize;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ecyrd.jspwiki.InternalWikiException;
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
 * @version $Revision: 1.17 $ $Date: 2006-05-20 05:18:48 $
 * @since 2.3
 */
public class WebContainerAuthorizer implements Authorizer
{
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
            throw new InternalWikiException( e.getClass().getName()+": "+e.getMessage() );
        }
        catch ( JDOMException e )
        {
            log.error("Malformed XML in web.xml",e);
            throw new InternalWikiException( e.getClass().getName()+": "+e.getMessage() );
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
    public boolean isConstrained( String url, Role role ) throws JDOMException
    {
        // Get all constraints that have our URL pattern
        String selector;
        selector = "//web-app/security-constraint[web-resource-collection/url-pattern=\"" + url + "\"]";
        List constraints = XPath.selectNodes( m_webxml, selector);
        
        // Get all constraints that match our Role pattern
        selector = "//web-app/security-constraint[auth-constraint/role-name=\"" + role.getName() + "\"]";
        List roles = XPath.selectNodes( m_webxml, selector );
        
        // If we can't find either one, we must not be constrained
        if ( constraints.size() == 0 )
        {
            return false;
        }
        
        // Shortcut: if the role is ALL, we are constrained
        if ( role.equals( Role.ALL ) )
        {
            return true;
        }
        
        // If no roles, we must not be constrained
        if ( roles.size() == 0 )
        {
            return false;
        }
        
        // If a constraint is contained in both lists, we must be constrained
        for ( Iterator c = constraints.iterator(); c.hasNext(); )
        {
            Element constraint = (Element)c.next();
            for ( Iterator r = roles.iterator(); r.hasNext(); )
            {
                Element roleConstraint = (Element)r.next();
                if ( constraint.equals( roleConstraint ) ) 
                {
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
     * Returns an array of role Principals corresponding to those
     * the container believes are authorized for the current request.
     * The array will contain a proper subset of those enumerated 
     * in the <code>web.xml</code>. This method actually returns 
     * a defensive copy of an internally stored array.
     * @return an array of Principals representing the roles
     */
    public Role[] getRoles( HttpServletRequest request )
    {
        Set roles = new HashSet();
        for ( int i = 0; i < m_containerRoles.length; i++ )
        {
            Role role = m_containerRoles[i];
            if ( request.isUserInRole( role.getName() ) )
            {
                roles.add( role );
            }
        }
        return (Role[]) roles.toArray( new Role[roles.size()] );
    }

    /**
     * Protected method that extracts the roles from JSPWiki's web application
     * deployment descriptor. Each Role is constructed by using the String
     * representation of the Role, for example
     * <code>new Role("Administrator")</code>.
     * @param webxml the web application deployment descriptor
     * @return an array of Role objects
     */
    protected Role[] getRoles( Document webxml ) throws JDOMException
    {
        Set roles = new HashSet();
        String selector = "//web-app/security-constraint/auth-constraint/role-name";
        List nodes = XPath.selectNodes( webxml, selector );
        for( Iterator it = nodes.iterator(); it.hasNext(); )
        {
            String role = ( (Element) it.next() ).getTextTrim();
            roles.add( new Role( role ) );
        }
        return (Role[]) roles.toArray( new Role[roles.size()] );
    }

    /**
     * Returns an {@link org.jdom.Document} representing JSPWiki's web
     * application deployment descriptor. The document is obtained by calling
     * the servlet context's <code>getResource()</code> method and requesting
     * <code>/WEB-INF/web.xml</code>. For non-servlet applications, this
     * method calls this class'
     * {@link ClassLoader#getResource(java.lang.String)} and requesting
     * <code>WEB-INF/web.xml</code>.
     * @return the descriptor
     */
    protected Document getWebXml() throws JDOMException, IOException
    {
        URL url;
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation( false );
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
     * @author Andrew Jaquith
     * @version $Revision: 1.17 $ $Date: 2006-05-20 05:18:48 $
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
         * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
         *      java.lang.String)
         */
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