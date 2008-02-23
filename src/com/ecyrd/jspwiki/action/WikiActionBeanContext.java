package com.ecyrd.jspwiki.action;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.util.UrlBuilder;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 * {@link net.sourceforge.stripes.action.ActionBeanContext} subclass that
 * contains a convenient reference to the current JSPWiki WikiEngine and the
 * user's HttpServletRequest. The WikiEngine reference is lazily initialized
 * when either {@link #setServletContext(ServletContext)} or
 * {@link #setRequest(HttpServletRequest)} is invoked. The HttpServletRequest
 * reference is set via {@link #setRequest(HttpServletRequest)}.
 * 
 * @author Andrew Jaquith
 */
public class WikiActionBeanContext extends ActionBeanContext
{
    private volatile WikiEngine m_engine = null;
    private Map<Method,EventPermissionInfo> m_methodPermissions = new HashMap<Method,EventPermissionInfo>();

    /**
     * Sets the Permission metadata for an event handler method. If the metadata has already
     * been set for the supplied hander, this method throws an {@linkplain IllegalArgumentException}.
     * @param method the handler method
     * @param permInfo the Permission metadata
     */
    public void addPermissionInfo(Method method, EventPermissionInfo permInfo)
    {
        if ( m_methodPermissions.containsKey(method))
        {
            throw new IllegalArgumentException("Already added permission info for " + method.getName());
        }
        m_methodPermissions.put(method,permInfo);
    }
    
    /**
     * Returns the Permission metdata for a supplied event handler method. If the
     * handler does not have any metadata, this method will return <code>null</code>
     * <em>and calling classes should check for this value</em>.
     * @param method the handler method to look up
     * @return the Permission metadata
     */
    public EventPermissionInfo getPermissionInfo(Method method)
    {
        return m_methodPermissions.get(method);
    }
    
    public WikiSession getWikiSession()
    {
        return WikiSession.getWikiSession(m_engine, getRequest());
    }

    /**
     * Sets the WikiEngine associated with this WikiActionBeanContext.
     * 
     * @param engine
     *            the wiki engine
     */
    public void setWikiEngine(WikiEngine engine)
    {
        m_engine = engine;
    }

    /**
     * Returns the WikiEngine associated with this WikiActionBeanContext.
     * 
     * @return the wiki engine
     */
    public WikiEngine getWikiEngine()
    {
        return m_engine;
    }

    /**
     * Calls the superclass
     * {@link ActionBeanContext#setRequest(HttpServletRequest)} and lazily sets
     * the internal WikiEngine reference, if still <code>null</code>.
     * 
     * @param request
     *            the HTTP request
     */
    @Override
    public void setRequest(HttpServletRequest request)
    {
        super.setRequest(request);
        if (request != null)
        {
            // Lazily set the WikiEngine reference
            if (m_engine == null)
            {
                ServletContext servletContext = request.getSession().getServletContext();
                m_engine = WikiEngine.getInstance(servletContext, null);
            }
        }
        
        // Retrieve the WikiSession, which ensures we log the user in (if needed)
        WikiSession.getWikiSession( m_engine, request );
    }

    /**
     * Calls the superclass
     * {@link ActionBeanContext#setServletContext(ServletContext)} and lazily
     * sets the internal WikiEngine reference, if still <code>null</code>.
     * 
     * @param servletContext
     *            the servlet context
     */
    @Override
    public void setServletContext(ServletContext servletContext)
    {
        super.setServletContext(servletContext);
        if (m_engine == null)
        {
            WikiEngine engine = WikiEngine.getInstance(servletContext, null);
            setWikiEngine(engine);
        }
    }

    /**
     * Returns the URL for accessing a named wiki page via a particular ActionBean, 
     * with a set of appended parameters. The URL will be <em>not</em> be encoded via 
     * the HttpServletResponse object, so callers that need URL re-writing should 
     * use {@link WikiContext#getURL(Class, String, String, boolean)} instead. If the 
     * ActionBean class is not a WikiContext subclass, the value of the 
     * <code>page</code> parameter is ignored.
     * @param beanClass the WikiActionBean subclass to use
     * @param page the wiki page; if <code>null</code>, the front page will be used
     * @param params the query parameters to append to the end of the URL; may be <code>null</code> if no parameters
     * @param absolute If <code>true</code>, will generate an absolute URL regardless of properties setting.
     * @return the URL
     */
    public String getURL( Class<? extends WikiActionBean> beanClass, String page, Map<String,String> params, boolean absolute )
    {
        // Set the page to the default (front page) if not supplied
        if( page == null )
        {
            page = m_engine.getFrontPage();
        }
        
        // Get the path prefix
        String pathPrefix;
        if ( absolute | m_engine.useAbsoluteUrls() )
        {
            pathPrefix = m_engine.getBaseURL();
        }
        else
        {
            pathPrefix = m_engine.getWikiActionBeanFactory().getPathPrefix();
        }
        boolean addParams = false;
        
        // Get the relative URL
        String url;
        if (AttachActionBean.class.equals(beanClass))              // Is this an attachment?
        {
            url = "attach/" + page;
        }
        else if (NoneActionBean.class.equals(beanClass))        // Is this a 'none' context?
        {
            url = page;
        }
        else                                                                                         // Otherwise, chop off the .action and add .jsp
        {
            url = beanClass.getAnnotation(UrlBinding.class).value();
            if ( url.endsWith(".action") )
            {
                url = url.substring( 1, url.length() - 7 ) + ".jsp";
            }
            addParams = true;
        }
        
        // Build the URL with any parameters we need
        UrlBuilder urlBuilder = new UrlBuilder( pathPrefix + url, false );
        
        if ( addParams )
        {
            // If the ActionBean is a WikiContext, add a page parameter
            if ( WikiContext.class.isAssignableFrom( beanClass ) && page != null )
            {
                urlBuilder.addParameter( "page", page + "" );
            }
            
            // Append the other parameters
            if ( params != null )
            {
                for ( Entry<String,String> param : params.entrySet() )
                {
                    urlBuilder.addParameter( param.getKey(), param.getValue() );
                }
            }
        }
        url = urlBuilder.toString();
        url = StringUtils.replace( url, "+", "%20" );
        url = StringUtils.replace( url, "%2F", "/" );
        
        // Encode the URL via the response object, if the response was set
        if ( getResponse() != null )
        {
            return getResponse().encodeURL( url );
        }
        return url;
    }
    
    /**
     * Returns the URL for accessing a named wiki page via a particular ActionBean, 
     * without parameters. The URL will be encoded via the HttpServletResponse object, 
     * which means that any outbound filters will be able to transform 
     * it as needed. The URL returned will be absolute if the WikiEngine
     * was configured to return absolute URLs; otherwise, the URL will be
     * relative to the webapp context root. If the ActionBean class is not a
     * WikiContext subclass, the value of the <code>page</code> parameter is
     * ignored.
     * @param beanClass the WikiActionBean subclass to use
     * @param page the wiki page; if <code>null</code>, the front page will be used
     * @return the URL
     */
    public String getURL( Class<? extends WikiActionBean> beanClass, String page )
    {
        return getURL( beanClass, page, null, m_engine.useAbsoluteUrls() );
    }

    /**
     * Returns the URL for accessing a named wiki page via a particular ActionBean, 
     * without parameters. The URL will be encoded via the HttpServletResponse object, 
     * which means that any outbound filters will be able to transform it as needed. 
     * If the ActionBean class is not a WikiContext subclass, the value of the
     * <code>page</code> parameter is ignored.
     * @param beanClass the WikiActionBean subclass to use
     * @param page the wiki page; if <code>null</code>, the front page will be used.
     * @param params the query parameters to append to the end of the URL; may be <code>null</code> if no parameters
     * @return the URL
     */
    public String getURL( Class<? extends WikiActionBean> beanClass, String page, Map<String,String> params )
    {
        return getURL( beanClass, page, params, m_engine.useAbsoluteUrls() );
    }

}
