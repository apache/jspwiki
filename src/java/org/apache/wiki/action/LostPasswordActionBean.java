/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.action;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import javax.mail.AuthenticationFailedException;
import javax.mail.SendFailedException;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.TemplateResolution;
import org.apache.wiki.util.MailUtil;
import org.apache.wiki.util.TextUtil;

/**
 * Resets user passwords.
 */
@UrlBinding( "/LostPassword.jsp" )
public class LostPasswordActionBean extends AbstractActionBean
{
    private static final Logger log = LoggerFactory.getLogger( LostPasswordActionBean.class );

    private String m_email = null;

    /**
     * Returns the e-mail address.
     * 
     * @return the e-mail address
     */
    public String getEmail()
    {
        return m_email;
    }

    /**
     * Event handler that resets the user's password, based on the e-mail
     * address returned by {@link #getEmail()}.
     * 
     * @return always forwards the user to the template JSP
     */
    @HandlesEvent( "reset" )
    public Resolution reset()
    {
        String messageKey = null;
        ResourceBundle rb = getContext().getBundle( "CoreResources" );

        // Reset pw for account name
        WikiEngine engine = getContext().getEngine();
        boolean success = false;

        try
        {
            // Look up the e-mail supplied by the user
            UserDatabase userDatabase = engine.getUserManager().getUserDatabase();
            UserProfile profile = userDatabase.findByEmail( m_email );
            String email = profile.getEmail();
            String randomPassword = TextUtil.generateRandomPassword();

            // Compose the message e-mail body
            Object[] args = { profile.getLoginName(), randomPassword,
                             engine.getURLConstructor().makeURL( WikiContext.NONE, "Login.jsp", true, "" ),
                             engine.getApplicationName() };
            String mailMessage = MessageFormat.format( rb.getString( "lostpwd.newpassword.email" ), args );

            // Compose the message subject line
            args = new Object[] { engine.getApplicationName() };
            String mailSubject = MessageFormat.format( rb.getString( "lostpwd.newpassword.subject" ), args );

            // Send the message.
            MailUtil.sendMessage( engine, email, mailSubject, mailMessage );
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
            messageKey = "lostpwd.nouser";
            log.info( "Tried to reset password for non-existent user '" + m_email + "'" );
        }
        catch( SendFailedException e )
        {
            messageKey = "lostpwd.nomail";
            log.error( "Tried to reset password and got SendFailedException: " + e );
        }
        catch( AuthenticationFailedException e )
        {
            messageKey = "lostpwd.nomail";
            log.error( "Tried to reset password and got AuthenticationFailedException: " + e );
        }
        catch( Exception e )
        {
            messageKey = "lostpwd.nomail";
            log.error( "Tried to reset password and got another exception: " + e );
        }

        if( success )
        {
            List<Message> messages = getContext().getMessages( "reset" );
            messages.add( new SimpleMessage( rb.getString( "lostpwd.emailed" ) ) );
        }
        else
        // Error
        {
            ValidationErrors errors = getContext().getValidationErrors();
            errors.addGlobalError( new LocalizableError( messageKey, m_email ) );
        }

        return new TemplateResolution( "Login.jsp" ).addParameter( "tab", "reset" );
    }

    /**
     * Sets the e-mail property. Used by the {@link #reset()} event.
     * 
     * @param email the e-mail address
     */
    @Validate( required = true, on = "reset", converter = net.sourceforge.stripes.validation.EmailTypeConverter.class )
    public void setEmail( String email )
    {
        m_email = email;
    }

    /**
     * Default handler that forwards the user to the template JSP.
     * 
     * @return the resolution
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "view" )
    public Resolution view()
    {
        return new TemplateResolution( "Login.jsp" ).addParameter( "tab", "reset" );
    }

}
