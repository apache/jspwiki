package com.ecyrd.jspwiki;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * A mock HttpServletRequest object, used for testing. The following methods
 * work as they should: {@link #getCookies()},{@link #getUserPrincipal()},
 * {@link #getRemoteUser()},{@link #getRemoteAddr()},
 * {@link #isUserInRole(String)},{@link #getSession()},
 * {@link #getSession(boolean)}. All others either return null, or don't work
 * they way they should.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.4 $ $Date: 2006-04-17 10:52:44 $
 */
public class TestHttpServletRequest implements HttpServletRequest
{

    protected Cookie[]    m_cookies       = new Cookie[0];

    protected String      m_remoteAddr    = "127.0.0.1";

    protected String      m_remoteUser    = null;

    protected Set         m_roles         = new HashSet();

    protected Principal   m_userPrincipal = null;

    protected HttpSession m_session       = null;

    /**
     * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
     */
    public Object getAttribute( String arg0 )
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getAuthType()
     */
    public String getAuthType()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getContentLength()
     */
    public int getContentLength()
    {
        return 0;
    }

    /**
     * @see javax.servlet.ServletRequest#getContentType()
     */
    public String getContentType()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getContextPath()
     */
    public String getContextPath()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getCookies()
     */
    public Cookie[] getCookies()
    {
        return m_cookies;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
     */
    public long getDateHeader( String arg0 )
    {
        return 0;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
     */
    public String getHeader( String arg0 )
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
     */
    public Enumeration getHeaderNames()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
     */
    public Enumeration getHeaders( String arg0 )
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getInputStream()
     */
    public ServletInputStream getInputStream() throws IOException
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
     */
    public int getIntHeader( String arg0 )
    {
        return 0;
    }

    /**
     * @see javax.servlet.ServletRequest#getLocale()
     */
    public Locale getLocale()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getLocales()
     */
    public Enumeration getLocales()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getMethod()
     */
    public String getMethod()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
     */
    public String getParameter( String arg0 )
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterMap()
     */
    public Map getParameterMap()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterNames()
     */
    public Enumeration getParameterNames()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues( String arg0 )
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getPathInfo()
     */
    public String getPathInfo()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
     */
    public String getPathTranslated()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getProtocol()
     */
    public String getProtocol()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getQueryString()
     */
    public String getQueryString()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getReader()
     */
    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
     * @deprecated
     */
    public String getRealPath( String arg0 )
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getRemoteAddr()
     */
    public String getRemoteAddr()
    {
        return m_remoteAddr;
    }

    /**
     * @see javax.servlet.ServletRequest#getRemoteHost()
     */
    public String getRemoteHost()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
     */
    public String getRemoteUser()
    {
        return m_remoteUser;
    }

    /**
     * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher( String arg0 )
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
     */
    public String getRequestedSessionId()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getRequestURI()
     */
    public String getRequestURI()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getRequestURL()
     */
    public StringBuffer getRequestURL()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getScheme()
     */
    public String getScheme()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getServerName()
     */
    public String getServerName()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getServerPort()
     */
    public int getServerPort()
    {
        return 0;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getServletPath()
     */
    public String getServletPath()
    {
        return null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getSession()
     */
    public HttpSession getSession()
    {
        return getSession( true );
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
     */
    public HttpSession getSession( boolean arg0 )
    {
        if ( arg0 && m_session == null )
        {
            m_session = new TestHttpSession();
        }
        return m_session;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
     */
    public Principal getUserPrincipal()
    {
        return m_userPrincipal;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
     */
    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
     * @deprecated
     */
    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
     */
    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
     */
    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    /**
     * @see javax.servlet.ServletRequest#isSecure()
     */
    public boolean isSecure()
    {
        return false;
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
     */
    public boolean isUserInRole( String arg0 )
    {
        return m_roles.contains( arg0 );
    }

    /**
     * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
     */
    public void removeAttribute( String arg0 )
    {
    }

    /**
     * @see javax.servlet.ServletRequest#setAttribute(java.lang.String,
     *      java.lang.Object)
     */
    public void setAttribute( String arg0, Object arg1 )
    {
    }

    /**
     * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
     */
    public void setCharacterEncoding( String arg0 ) throws UnsupportedEncodingException
    {
    }

    /**
     * @param cookies The cookies to set.
     */
    public void setCookies( Cookie[] cookies )
    {
        m_cookies = cookies;
    }

    /**
     * @param remoteAddr The remoteAddr to set.
     */
    public void setRemoteAddr( String remoteAddr )
    {
        m_remoteAddr = remoteAddr;
    }

    /**
     * @param remoteUser The remoteUser to set.
     */
    public void setRemoteUser( String remoteUser )
    {
        m_remoteUser = remoteUser;
    }

    /**
     * Sets the roles that this request's user should be considered a member of.
     * The
     * @param roles The roles to set.
     */
    public void setRoles( String[] roles )
    {
        for( int i = 0; i < roles.length; i++ )
        {
            m_roles.add( roles[i] );
        }
    }

    /**
     * @param userPrincipal The userPrincipal to set.
     */
    public void setUserPrincipal( Principal principal )
    {
        m_userPrincipal = principal;
    }

    public int getRemotePort()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getLocalName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLocalAddr()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getLocalPort()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}