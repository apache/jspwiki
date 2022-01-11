package org.apache.wiki.tasks.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.tasks.TasksManager;
import org.apache.wiki.util.MailUtil;
import org.apache.wiki.workflow.Outcome;
import org.apache.wiki.workflow.Task;
import org.apache.wiki.workflow.WorkflowManager;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import java.util.Locale;


/**
 * Handles the actual profile save action. 
 */
public class SaveUserProfileTask extends Task {

    private static final long serialVersionUID = 6994297086560480285L;
    private static final Logger LOG = LogManager.getLogger( SaveUserProfileTask.class );
    private final Locale m_loc;

    /**
     * Constructs a new Task for saving a user profile.
     */
    public SaveUserProfileTask( final Locale loc ) {
        super( TasksManager.USER_PROFILE_SAVE_TASK_MESSAGE_KEY );
        m_loc = loc;
    }

    /**
     * Saves the user profile to the user database.
     *
     * @return {@link org.apache.wiki.workflow.Outcome#STEP_COMPLETE} if the task completed successfully
     * @throws WikiException if the save did not complete for some reason
     */
    @Override
    public Outcome execute( final Context context ) throws WikiException {
        // Retrieve user profile
        final UserProfile profile = ( UserProfile )getWorkflowContext().get( WorkflowManager.WF_UP_CREATE_SAVE_ATTR_SAVED_PROFILE );

        // Save the profile (userdatabase will take care of timestamps for us)
        context.getEngine().getManager( UserManager.class ).getUserDatabase().save( profile );

        // Send e-mail if user supplied an e-mail address
        if ( profile != null && profile.getEmail() != null ) {
            try {
                final InternationalizationManager i18n = context.getEngine().getManager( InternationalizationManager.class );
                final String app = context.getEngine().getApplicationName();
                final String to = profile.getEmail();
                final String subject = i18n.get( InternationalizationManager.DEF_TEMPLATE, m_loc,
                                                 "notification.createUserProfile.accept.subject", app );

                final String loginUrl = context.getEngine().getURL( ContextEnum.WIKI_LOGIN.getRequestContext(), null, null );
                final String content = i18n.get( InternationalizationManager.DEF_TEMPLATE, m_loc,
                                                 "notification.createUserProfile.accept.content", app,
                                                 profile.getLoginName(),
                                                 profile.getFullname(),
                                                 profile.getEmail(),
                                                 loginUrl );
                MailUtil.sendMessage( context.getEngine().getWikiProperties(), to, subject, content );
            } catch ( final AddressException e) {
                LOG.debug( e.getMessage(), e );
            } catch ( final MessagingException me ) {
                LOG.error( "Could not send registration confirmation e-mail. Is the e-mail server running?", me );
            }
        }

        return Outcome.STEP_COMPLETE;
    }

}
