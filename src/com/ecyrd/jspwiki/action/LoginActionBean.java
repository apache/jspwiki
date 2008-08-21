package com.ecyrd.jspwiki.action;

import java.security.Principal;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.util.UrlBuilder;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.CookieAuthenticationLoginModule;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

@UrlBinding( "/Login.action" )
public class LoginActionBean extends AbstractActionBean
{
    private static final Logger log = Logger.getLogger( LoginActionBean.class );

    /**
     * Sets cookies and redirects the user to a wiki page after a successful
     * profile creation or login.
     * 
     * @param bean the WikiActionBean currently executing
     * @param pageName the page to redirect to after login or profile creation
     * @param rememberMe whether to set the "remember me?" cookie
     */
    private Resolution saveCookiesAndRedirect( String pageName, boolean rememberMe )
    {
        // Set user cookie
        Principal principal = getWikiSession().getUserPrincipal();
        CookieAssertionLoginModule.setUserCookie( getContext().getResponse(), principal.getName() );

        // Set "remember me?" cookie
        if( rememberMe )
        {
            CookieAuthenticationLoginModule.setLoginCookie( getEngine(), getContext().getResponse(), principal.getName() );
        }

        UrlBuilder builder = new UrlBuilder( getContext().getLocale(), ViewActionBean.class, false );
        if( pageName != null )
        {
            builder.addParameter( "page", pageName );
        }
        return new RedirectResolution( builder.toString() );
    }

    private String m_username = null;

    private boolean m_remember = false;

    private String m_password;

    private String m_redirect;

    public String getJ_password()
    {
        return m_password;
    }

    public boolean getJ_remember()
    {
        return m_remember;
    }

    public String getJ_username()
    {
        return m_username;
    }

    public String getRedirect()
    {
        return m_redirect;
    }

    @HandlesEvent( "login" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    public Resolution login()
    {
        WikiSession wikiSession = this.getWikiSession();
        ValidationErrors errors = getContext().getValidationErrors();
        ResourceBundle rb = getBundle( "CoreResources" );

        // If user got here and is already authenticated, it means
        // they just aren't allowed access to what they asked for.
        // Weepy tears and hankies all 'round.
        if( getWikiSession().isAuthenticated() )
        {
            errors.addGlobalError( new SimpleError( rb.getString( "login.error.noaccess" ) ) );
            getContext().flash( this );
            return new RedirectResolution( MessageActionBean.class );
        }

        log.debug( "Attempting to authenticate user " + m_username );

        // Log the user in!
        Resolution r = null;
        try
        {
            if( getEngine().getAuthenticationManager().login( wikiSession, m_username, m_password ) )
            {
                // Set cookies as needed and redirect
                log.info( "Successfully authenticated user " + m_username + " (custom auth)" );
                r = saveCookiesAndRedirect( m_redirect, m_remember );
            }
            else
            {
                log.info( "Failed to authenticate user " + m_username );
                errors.addGlobalError( new SimpleError( rb.getString( "login.error.password" ) ) );
            }
        }
        catch( WikiSecurityException e )
        {
            errors.addGlobalError( new SimpleError( rb.getString( "login.error" ), e.getMessage() ) );
        }

        // Any errors?
        if( !errors.isEmpty() )
        {
            UrlBuilder builder = new UrlBuilder( getContext().getLocale(), "/Login.jsp", false );
            builder.addParameter( "tab", "logincontent" );
            getContext().flash( this );
            r = new RedirectResolution( builder.toString() );
        }

        return r;
    }

    @HandlesEvent( "logout" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    @WikiRequestContext( "logout" )
    public Resolution logout()
    {
        WikiEngine engine = getEngine();
        HttpServletRequest request = getContext().getRequest();
        HttpServletResponse response = getContext().getResponse();
        engine.getAuthenticationManager().logout( request );
        
        // Clear the asserted name cookie
        CookieAssertionLoginModule.clearUserCookie( response );

        // Delete the authentication cookie
        if ( engine.getAuthenticationManager().allowsCookieAuthentication() )
        {
            CookieAuthenticationLoginModule.clearLoginCookie( engine, request, response );
        }
        
        return new RedirectResolution( ViewActionBean.class );
    }

    @Validate( required = true, on = "login", minlength = 1, maxlength = 128 )
    public void setJ_password( String password )
    {
        m_password = password;
    }

    public void setJ_remember( boolean remember )
    {
        m_remember = remember;
    }

    @Validate( required = true, on = "login", minlength = 1, maxlength = 128 )
    public void setJ_username( String username )
    {
        m_username = username;
    }

    public void setRedirect( String redirect )
    {
        m_redirect = redirect;
        setVariable( "redirect", redirect );
    }

    /**
     * Default handler that executes when the login page is viewed. It checks to
     * see if the user is already authenticated, and if so, sends a "forbidden"
     * message.
     * 
     * @return the resolution
     */
    @DefaultHandler
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    @WikiRequestContext( "login" )
    public Resolution view()
    {
        Resolution r = null;

        if( getWikiSession().isAuthenticated() )
        {
            // Set cookies as needed and redirect
            r = saveCookiesAndRedirect( m_redirect, m_remember );
        }

        if( getEngine().getAuthenticationManager().isContainerAuthenticated() )
        {
            //
            // Have we already been submitted? If yes, then we can assume that
            // we have been logged in before.
            //

            HttpSession session = getContext().getRequest().getSession();
            Object seen = session.getAttribute( "_redirect" );
            if( seen != null )
            {
                ResourceBundle rb = getBundle( "CoreResources" );
                getContext().getValidationErrors().addGlobalError( new SimpleError( rb.getString( "login.error.noaccess" ) ) );
                getContext().flash( this );
                return new RedirectResolution( MessageActionBean.class );
            }
            session.setAttribute( "_redirect", "I love Outi" ); // Any marker
            // will do

            // If using container auth, the container will have automatically
            // attempted to log in the user before Login.jsp was loaded.
            // Thus, if we got here, the container must have authenticated
            // the user already. All we do is simply record that fact.
            // Nice and easy.

            Principal user = getWikiSession().getLoginPrincipal();
            log.info( "Successfully authenticated user " + user.getName() + " (container auth)" );
        }

        return r;
    }

}
