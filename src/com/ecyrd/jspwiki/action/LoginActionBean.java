package com.ecyrd.jspwiki.action;

import java.security.Principal;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.mail.AuthenticationFailedException;
import javax.mail.SendFailedException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.util.UrlBuilder;
import net.sourceforge.stripes.validation.*;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.CookieAuthenticationLoginModule;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;
import com.ecyrd.jspwiki.util.MailUtil;
import com.ecyrd.jspwiki.util.TextUtil;

@UrlBinding( "/Login.action" )
public class LoginActionBean extends AbstractActionBean
{
    private static final Logger log = LoggerFactory.getLogger( LoginActionBean.class );

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
        Principal principal = getContext().getWikiSession().getUserPrincipal();
        CookieAssertionLoginModule.setUserCookie( getContext().getResponse(), principal.getName() );

        // Set "remember me?" cookie
        if( rememberMe )
        {
            CookieAuthenticationLoginModule.setLoginCookie( getContext().getEngine(), getContext().getResponse(), principal
                .getName() );
        }

        UrlBuilder builder = new UrlBuilder( getContext().getLocale(), ViewActionBean.class, false );
        if( pageName != null )
        {
            builder.addParameter( "page", pageName );
        }
        return new RedirectResolution( builder.toString() );
    }

    private String m_email = null;

    private String m_username = null;

    private boolean m_remember = false;

    private String m_password;

    private String m_redirect = null;

    /**
     * Returns the e-mail address.
     * 
     * @return the e-mail address
     */
    public String getEmail()
    {
        return m_email;
    }

    public String getJ_password()
    {
        return m_password;
    }

    public boolean getRemember()
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

    @ValidationMethod( on = "login", when = ValidationState.NO_ERRORS )
    public void validateCredentials()
    {
        WikiSession wikiSession = getContext().getWikiSession();
        ValidationErrors errors = getContext().getValidationErrors();

        log.debug( "Attempting to authenticate user " + m_username );

        // Log the user in!
        try
        {
            if( !getContext().getEngine().getAuthenticationManager().login( wikiSession, m_username, m_password ) )
            {
            }
        }
        catch ( LoginException e )
        {
            log.info( "Failed to authenticate user " + m_username + ", reason: " + e.getMessage());
            errors.addGlobalError( new LocalizableError( "login.error.password" ) );
        }
        catch( WikiSecurityException e )
        {
            errors.addGlobalError( new LocalizableError( "login.error", e.getMessage() ) );
        }
    }

    @HandlesEvent( "login" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    public Resolution login()
    {
        // Set cookies as needed and redirect
        log.info( "Successfully authenticated user " + m_username + " (custom auth)" );
        Resolution r = saveCookiesAndRedirect( m_redirect, m_remember );
        return r;
    }

    @HandlesEvent( "logout" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    @WikiRequestContext( "logout" )
    public Resolution logout()
    {
        WikiEngine engine = getContext().getEngine();
        HttpServletRequest request = getContext().getRequest();
        HttpServletResponse response = getContext().getResponse();
        engine.getAuthenticationManager().logout( request );

        // Clear the asserted name cookie
        CookieAssertionLoginModule.clearUserCookie( response );

        // Delete the authentication cookie
        if( engine.getAuthenticationManager().allowsCookieAuthentication() )
        {
            CookieAuthenticationLoginModule.clearLoginCookie( engine, request, response );
        }

        return new RedirectResolution( ViewActionBean.class );
    }

    /**
     * Sets the e-mail property. Used by the {@link #resetPassword()} event.
     * 
     * @param email the e-mail address
     */
    @Validate( required = true, on = "resetPassword", converter = net.sourceforge.stripes.validation.EmailTypeConverter.class )
    public void setEmail( String email )
    {
        m_email = email;
    }

    @Validate( required = true, on = "login", minlength = 1, maxlength = 128 )
    public void setJ_password( String password )
    {
        m_password = password;
    }

    @Validate()
    public void setRemember( boolean remember )
    {
        m_remember = remember;
    }

    @Validate( required = true, on = "login", minlength = 1, maxlength = 128 )
    public void setJ_username( String username )
    {
        m_username = username;
    }

    @Validate()
    public void setRedirect( String redirect )
    {
        m_redirect = redirect;
        getContext().setVariable( "redirect", redirect );
    }

    /**
     * Default handler that executes when the login page is viewed. It checks to
     * see if the user is already authenticated, and if so, sends a "forbidden"
     * message.
     * 
     * @return the resolution
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    @WikiRequestContext( "login" )
    public Resolution view()
    {
        ValidationErrors errors = getContext().getValidationErrors();

        // If user got here and is already authenticated, it means
        // they just aren't allowed access to what they asked for.
        // Weepy tears and hankies all 'round.
        if( getContext().getWikiSession().isAuthenticated() )
        {
            errors.addGlobalError( new LocalizableError( "login.error.noaccess" ) );
            return new RedirectResolution( MessageActionBean.class );
        }

        if( getContext().getEngine().getAuthenticationManager().isContainerAuthenticated() )
        {
            //
            // Have we already been submitted? If yes, then we can assume that
            // we have been logged in before.
            //

            HttpSession session = getContext().getRequest().getSession();
            Object seen = session.getAttribute( "_redirect" );
            if( seen != null )
            {
                getContext().getValidationErrors().addGlobalError( new LocalizableError( "login.error.noaccess" ) );
                return new RedirectResolution( MessageActionBean.class );
            }
            session.setAttribute( "_redirect", "I love Outi" ); // Any marker
            // will do

            // If using container auth, the container will have automatically
            // attempted to log in the user before Login.jsp was loaded.
            // Thus, if we got here, the container must have authenticated
            // the user already. All we do is simply record that fact.
            // Nice and easy.

            Principal user = getContext().getWikiSession().getLoginPrincipal();
            log.info( "Successfully authenticated user " + user.getName() + " (container auth)" );
        }

        // The user hasn't logged in yet, so send them to the login page
        ForwardResolution r = new ForwardResolution( "/Login.jsp" );
        r.addParameter( "tab", "logincontent" );
        return r;
    }

    /**
     * Event handler that resets the user's password, based on the e-mail
     * address returned by {@link #getEmail()}.
     * 
     * @return always returns <code>null</code>
     */
    @HandlesEvent( "resetPassword" )
    public Resolution resetPassword()
    {
        String message = null;
        ResourceBundle rb = getContext().getBundle( "CoreResources" );

        // Reset pw for account name
        WikiEngine wiki = getContext().getEngine();
        WikiSession wikiSession = getContext().getWikiSession();
        UserDatabase userDatabase = wiki.getUserManager().getUserDatabase();
        boolean success = false;

        try
        {
            // Look up the e-mail supplied by the user
            UserProfile profile = userDatabase.findByEmail( m_email );
            String email = profile.getEmail();
            String randomPassword = TextUtil.generateRandomPassword();

            // Compose the message e-mail body
            Object[] args = { profile.getLoginName(), randomPassword,
                             wiki.getURLConstructor().makeURL( WikiContext.NONE, "Login.jsp", true, "" ), wiki.getApplicationName() };
            String mailMessage = MessageFormat.format( rb.getString( "lostpwd.newpassword.email" ), args );

            // Compose the message subject line
            args = new Object[] { wiki.getApplicationName() };
            String mailSubject = MessageFormat.format( rb.getString( "lostpwd.newpassword.subject" ), args );

            // Send the message.
            MailUtil.sendMessage( wiki, email, mailSubject, mailMessage );
            log.info( "User " + email + " requested and received a new password." );

            // Mail succeeded. Now reset the password.
            // If this fails, we're kind of screwed, because we already mailed
            // it.
            profile.setPassword( randomPassword );
            userDatabase.save( profile );
            success = true;
        }
        catch( NoSuchPrincipalException e )
        {
            Object[] args = { m_email };
            message = MessageFormat.format( rb.getString( "lostpwd.nouser" ), args );
            log.info( "Tried to reset password for non-existent user '" + m_email + "'" );
        }
        catch( SendFailedException e )
        {
            message = rb.getString( "lostpwd.nomail" );
            log.error( "Tried to reset password and got SendFailedException: " + e );
        }
        catch( AuthenticationFailedException e )
        {
            message = rb.getString( "lostpwd.nomail" );
            log.error( "Tried to reset password and got AuthenticationFailedException: " + e );
        }
        catch( Exception e )
        {
            message = rb.getString( "lostpwd.nomail" );
            log.error( "Tried to reset password and got another exception: " + e );
        }

        if( success )
        {
            wikiSession.addMessage( "resetpwok", rb.getString( "lostpwd.emailed" ) );
            getContext().getRequest().setAttribute( "passwordreset", "done" );
        }
        else
        // Error
        {
            wikiSession.addMessage( "resetpw", message );
        }

        return new ForwardResolution( "/LostPassword.jsp" );
    }

}
