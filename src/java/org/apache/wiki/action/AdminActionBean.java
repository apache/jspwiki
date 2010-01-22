package org.apache.wiki.action;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.SecurityVerifier;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.ui.admin.AdminBean;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.util.TextUtil;

/**
 * Administrative actions, including {@link AdminBean} execution and security
 * configuration.
 */
public class AdminActionBean extends AbstractActionBean
{
    private AdminBean m_bean = null;

    private SecurityVerifier m_securityVerifier = null;

    /**
     * If the security configuration UI is enabled, this method initializes a
     * new {@link SecurityVerifier} and forwards the user to {@code
     * /admin/SecurityConfig.jsp}.
     * 
     * @return the resolution
     */
    @HandlesEvent( "security" )
    public Resolution security()
    {
        WikiEngine engine = getContext().getEngine();
        if( TextUtil.isPositive( engine.getWikiProperties().getProperty( "jspwiki-x.securityconfig.enable" ) ) )
        {
            return new StreamingResolution( "text/html" ) {
                public void stream( HttpServletResponse response ) throws Exception
                {
                    PrintWriter out = response.getWriter();
                    out.print( "<html><body><p>Security config is disabled.</p></body></html>" );
                }
            };
        }
        m_securityVerifier = new SecurityVerifier( engine, getContext().getWikiSession() );
        return new ForwardResolution( "/admin/SecurityConfig.jsp" );
    }

    /**
     * Returns the initialized SecurityVerifier.
     * 
     * @return the verifier
     */
    public SecurityVerifier getVerifier()
    {
        return m_securityVerifier;
    }

    /**
     * Return the AdminBean set for this AdminActionBean, which may be {@code
     * null}.
     * 
     * @return the admin bean
     */
    public AdminBean getBean()
    {
        return m_bean;
    }

    /**
     * Sets the AdminBean for this AdminActionBean.
     * 
     * @param bean the admin bean
     */
    @Validate( required = true, on = "admin" )
    public void setBean( AdminBean bean )
    {
        m_bean = bean;
    }

    /**
     * Returns {@code true} of the Admin UI is enabled.
     * 
     * @return the result
     */
    private boolean isAdminUiEnabled()
    {
        // Temporary!
        return true;
    }

    /**
     * If the admin UI is enabled, forwards the user to {@code /admin/Admin.jsp}
     * .
     * 
     * @return the resolution
     */
    @DefaultHandler
    @HandlesEvent( "view" )
    public Resolution view()
    {
        if( !isAdminUiEnabled() )
        {
            return new StreamingResolution( "text/html" ) {
                public void stream( HttpServletResponse response ) throws Exception
                {
                    PrintWriter out = response.getWriter();
                    out.print( "<html><body><p>Admin UI is disabled.</p></body></html>" );
                }
            };
        }
        return new ForwardResolution( "/admin/Admin.jsp" );
    }

    /**
     * If the admin UI is enabled, this method executes
     * {@link AdminBean#doPost(org.apache.wiki.WikiContext)} for the current
     * AdminBean and forwards the user to {@code /admin/Admin.jsp}.
     * 
     * @return the resolution
     */
    @HandlesEvent( "admin" )
    public Resolution admin()
    {
        if( !isAdminUiEnabled() )
        {
            return new StreamingResolution( "text/html" ) {
                public void stream( HttpServletResponse response ) throws Exception
                {
                    PrintWriter out = response.getWriter();
                    out.print( "<html><body><p>Admin UI is disabled.</p></body></html>" );
                }
            };
        }
        m_bean.doPost( getContext() );
        return new ForwardResolution( "/admin/Admin.jsp" );
    }
}
