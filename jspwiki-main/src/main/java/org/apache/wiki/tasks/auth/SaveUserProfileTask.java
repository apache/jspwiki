package org.apache.wiki.tasks.auth;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.WikiException;
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
    private static final Logger LOG = Logger.getLogger( SaveUserProfileTask.class );
    private final WikiEngine m_engine;
    private final Locale m_loc;

    /**
     * Constructs a new Task for saving a user profile.
     * @param engine the wiki engine
     */
    public SaveUserProfileTask( final WikiEngine engine, final Locale loc ) {
        super( TasksManager.USER_PROFILE_SAVE_TASK_MESSAGE_KEY );
        m_engine = engine;
        m_loc = loc;
    }

    /**
     * Saves the user profile to the user database.
     * @return {@link org.apache.wiki.workflow.Outcome#STEP_COMPLETE} if the
     * task completed successfully
     * @throws WikiException if the save did not complete for some reason
     */
    @Override
    public Outcome execute() throws WikiException {
        // Retrieve user profile
        final UserProfile profile = ( UserProfile )getWorkflow().getAttribute( WorkflowManager.WF_UP_CREATE_SAVE_ATTR_SAVED_PROFILE );

        // Save the profile (userdatabase will take care of timestamps for us)
        m_engine.getUserManager().getUserDatabase().save( profile );

        // Send e-mail if user supplied an e-mail address
        if ( profile.getEmail() != null ) {
            try {
                final InternationalizationManager i18n = m_engine.getInternationalizationManager();
                final String app = m_engine.getApplicationName();
                final String to = profile.getEmail();
                final String subject = i18n.get( InternationalizationManager.DEF_TEMPLATE, m_loc,
                                                 "notification.createUserProfile.accept.subject", app );

                final String content = i18n.get( InternationalizationManager.DEF_TEMPLATE, m_loc,
                                                 "notification.createUserProfile.accept.content", app,
                                                 profile.getLoginName(),
                                                 profile.getFullname(),
                                                 profile.getEmail(),
                                                 m_engine.getURL( WikiContext.LOGIN, null, null ) );
                MailUtil.sendMessage( m_engine.getWikiProperties(), to, subject, content);
            } catch ( final AddressException e) {
                LOG.debug( e.getMessage(), e );
            } catch ( final MessagingException me ) {
                LOG.error( "Could not send registration confirmation e-mail. Is the e-mail server running?", me );
            }
        }

        return Outcome.STEP_COMPLETE;
    }

}
