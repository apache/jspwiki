package com.ecyrd.jspwiki.action;

import java.security.Principal;
import java.util.ResourceBundle;

import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.util.UrlBuilder;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.CookieAuthenticationLoginModule;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.workflow.DecisionRequiredException;

@UrlBinding( "/Login.jsp" )
public class LoginActionBean extends AbstractUserProfileActionBean
{
    private String m_redirect = null;

    private String m_username = null;

    private boolean m_remember = false;

    private String m_password;

    private static final Logger log = Logger.getLogger( LoginActionBean.class );

    public String getJ_userName()
    {
        return m_username;
    }

    public String getJ_password()
    {
        return m_password;
    }

    public void setJ_username( String username )
    {
        m_username = username;
    }

    public void setJ_password( String password )
    {
        m_password = password;
    }

    public void setJ_remember( boolean remember )
    {
        m_remember = remember;
    }

    public boolean getJ_remember()
    {
        return m_remember;
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

        // If user got here and is already authenticated, it means
        // they just aren't allowed access to what they asked for.
        // Weepy tears and hankies all 'round.
        if( getWikiSession().isAuthenticated() )
        {
            ResourceBundle rb = getBundle( "CoreResources" );
            String message = rb.getString( "login.error.noaccess" );
            getContext().getValidationErrors().addGlobalError( new SimpleError( message ) );
            return new RedirectResolution( MessageActionBean.class ).flash( this );
        }

        log.debug( "Attempting to authenticate user " + m_username );

        // Log the user in!
        Resolution r = null;
        ValidationErrors errors = getContext().getValidationErrors();
        try
        {
            if( getEngine().getAuthenticationManager().login( wikiSession, m_username, m_password ) )
            {
                // Set cookies as needed and redirect
                log.info( "Successfully authenticated user " + m_username + " (custom auth)" );
                r = redirectToOriginalPage();
            }
            else
            {
                log.info( "Failed to authenticate user " + m_username );
                errors.addGlobalError( new SimpleError( "login.error.password" ) );

            }
        }
        catch( WikiSecurityException e )
        {
            errors.addGlobalError( new SimpleError( "login.error", e.getMessage() ) );
        }

        // Any errors?
        if( !errors.isEmpty() )
        {
            UrlBuilder builder = new UrlBuilder( getContext().getLocale(), LoginActionBean.class, false );
            builder.addParameter( "tab", "logincontent" );
            r = new RedirectResolution( builder.toString() );
        }

        return r;
    }

    @HandlesEvent( "logout" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    @WikiRequestContext( "logout" )
    public Resolution logout()
    {
        return null;
    }

    public void setRedirect( String redirect )
    {
        m_redirect = redirect;
        setVariable( "redirect", redirect );
    }

    /**
     * Attempts to save the user profile, after all other validation routines
     * have executed. If the save executes without error, this method will
     * return <code>null</code>. Otherwise, any error conditions will return
     * a non-null Resolution. For example, if an approval is required to save
     * the profile, a RedirectResolution redirecting the user to the
     * <code>ApprovalRequiredForUserProfiles</code> will be returned. Other
     * validation errors will cause redirection back to the profile tab.
     * 
     * @return
     */
    @HandlesEvent( "saveProfile" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${engine.applicationName}", actions = WikiPermission.EDIT_PROFILE_ACTION )
    public Resolution save()
    {
        WikiActionBeanContext context = getContext();
        Resolution r = null;
        try
        {
            // Save the profile
            WikiEngine engine = getEngine();
            engine.getUserManager().setUserProfile( getWikiSession(), m_profile );
            CookieAssertionLoginModule.setUserCookie( context.getResponse(), m_profile.getFullname() );
        }

        // Not so fast my friend! Approvals are required...
        catch( DecisionRequiredException e )
        {
            UrlBuilder builder = new UrlBuilder( this.getContext().getLocale(), ViewActionBean.class, false );
            builder.addParameter( "page", "ApprovalRequiredForUserProfiles" );
            r = new RedirectResolution( builder.toString() );
        }

        // Any other errors are either UI or config problems, so let the user
        // know about them
        catch( Exception e )
        {
            ValidationErrors errors = context.getValidationErrors();
            errors.addGlobalError( new LocalizableError( "saveError", e.getMessage() ) );
            UrlBuilder builder = new UrlBuilder( this.getContext().getLocale(), LoginActionBean.class, false );
            builder.addParameter( "tab", "profile" );
            r = new RedirectResolution( builder.toString() );
        }

        // Set cookies as needed and redirect
        r = redirectToOriginalPage();

        return r;
    }

    /**
     * Executes after a successful profile creation or login
     */
    private Resolution redirectToOriginalPage()
    {
        // Set user cookie
        Principal principal = getWikiSession().getUserPrincipal();
        CookieAssertionLoginModule.setUserCookie( getContext().getResponse(), principal.getName() );

        // Set "remember me?" cookie
        if( m_remember )
        {
            CookieAuthenticationLoginModule.setLoginCookie( getEngine(), getContext().getResponse(), principal.getName() );
        }

        Resolution r = null;
        if( m_redirect != null )
        {
            UrlBuilder builder = new UrlBuilder( this.getContext().getLocale(), ViewActionBean.class, false );
            builder.addParameter( "page", m_redirect );
            r = new RedirectResolution( builder.toString() );
        }
        return r;
    }

    /**
     * Default handler that executes when the login page is viewed. It checks to
     * see if the user is already authenticated, and if so, sents a "forbidden"
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
            r = redirectToOriginalPage();
            if( r == null )
            {
                r = new RedirectResolution( ViewActionBean.class );
            }
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
                String message = rb.getString( "login.error.noaccess" );
                getContext().getValidationErrors().addGlobalError( new SimpleError( message ) );
                return new RedirectResolution( MessageActionBean.class ).flash( this );
            }
            session.setAttribute( "_redirect", "I love Outi" ); // Just any
                                                                // marker will
                                                                // do

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
