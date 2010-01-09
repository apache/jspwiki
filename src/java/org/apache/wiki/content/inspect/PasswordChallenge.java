package org.apache.wiki.content.inspect;

import java.io.IOException;
import java.util.Locale;

import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;

/**
 * Challenge class that verifies that a user's password is correct. This
 * challenge can only be used when JSPWiki built-in authentication is used. No
 * form elements will be rendered by {@link #formContent(WikiActionBeanContext)}
 * if container authentication is in effect, and
 * {@link #check(WikiActionBeanContext)} will return {@code true} in this case.
 */
public class PasswordChallenge implements Challenge
{

    /**
     * Validates the user's password based on the contents of the request
     * parameter {@code j_password}. If the user is not authenticated, or if
     * container authentication is configured, this method will always return
     * {@code false}.
     */
    public boolean check( WikiActionBeanContext context ) throws IOException
    {
        AuthenticationManager authMgr = context.getEngine().getAuthenticationManager();
        WikiSession wikiSession = context.getWikiSession();
        if( !wikiSession.isAuthenticated() || authMgr.isContainerAuthenticated() )
        {
            return false;
        }

        // Get the user's password
        String loginName = wikiSession.getLoginPrincipal().getName();
        String password = context.getRequest().getParameter( "j_password" );
        if ( loginName == null || password == null )
        {
            return false;
        }
        
        UserDatabase db = context.getEngine().getUserManager().getUserDatabase();
        return db.validatePassword( loginName, password );
    }

    private static final String PASSWORD_DESCRIPTION_KEY = "org.apache.wiki.content.inspect.PasswordChallenge.description";

    private static final String PASSWORD_KEY = "password";

    /**
     * Generates a password <code>&lt;input&gt;</code> tag called {@code
     * j_password} to capture the user's password.
     */
    public String formContent( WikiActionBeanContext context ) throws IOException
    {
        boolean isContainerAuthentication = context.getEngine().getAuthenticationManager().isContainerAuthenticated();
        if( isContainerAuthentication )
        {
            return null;
        }

        // Get the localized text
        Locale locale = context.getLocale();
        InternationalizationManager i18n = context.getEngine().getInternationalizationManager();
        String description = i18n.get( InternationalizationManager.CORE_BUNDLE, locale, PASSWORD_DESCRIPTION_KEY );
        String password = i18n.get( InternationalizationManager.CORE_BUNDLE, locale, PASSWORD_KEY );

        StringBuilder b = new StringBuilder();
        b.append( "<div class=\"password\">" + description + "</div>" );
        b.append( "<label for=\"j_password\" value=\"" + password + "\" />\n" );
        b.append( "<input name=\"j_password\" id=\"j_password\" type=\"password\" />\n" );
        return b.toString();
    }

}
