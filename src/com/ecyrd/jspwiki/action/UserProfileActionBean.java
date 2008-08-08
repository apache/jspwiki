package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.util.UrlBuilder;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.ValidationErrors;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.workflow.DecisionRequiredException;

/**
 * @author Andrew Jaquith
 */
@UrlBinding( "/UserProfile.jsp" )
public class UserProfileActionBean extends AbstractUserProfileActionBean
{

    /**
     * Attempts to save the user profile, after all other validation routines have executed.
     * If the save executes without error, this method will return <code>null</code>.
     * Otherwise, any error conditions will return a non-null Resolution.
     * For example, if an approval is required to save the profile, a RedirectResolution
     * redirecting the user to the <code>ApprovalRequiredForUserProfiles</code>
     * will be returned. Other validation errors will cause redirection back to the profile tab.
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
            engine.getUserManager().setUserProfile( getWikiSession(), m_profile);
            CookieAssertionLoginModule.setUserCookie(context.getResponse(), m_profile.getFullname());
        }
        
        // Not so fast my friend! Approvals are required...
        catch( DecisionRequiredException e )
        {
            UrlBuilder builder = new UrlBuilder( this.getContext().getLocale(), ViewActionBean.class, false );
            builder.addParameter( "page", "ApprovalRequiredForUserProfiles" );
            r = new RedirectResolution( builder.toString() );
        }
        
        // Any other errors are either UI or config problems, so let the user know about them
        catch (Exception e)
        {
            ValidationErrors errors = context.getValidationErrors();
            errors.addGlobalError( new LocalizableError( "saveError", e.getMessage() ) );
            UrlBuilder builder = new UrlBuilder( this.getContext().getLocale(), UserProfileActionBean.class, false );
            builder.addParameter( "tab", "profile" );
            r = new RedirectResolution( builder.toString() );
        }
        
        // The save executed correctly and without errors, so return nothing
        return r;
    }

    /**
     * Default handler that forwards the user back to itself.
     * 
     * @return the resolution
     */
    @HandlesEvent( "view" )
    @DefaultHandler
    @WikiRequestContext( "profile" )
    public Resolution view()
    {
        return new ForwardResolution( ViewActionBean.class );
    }

}
