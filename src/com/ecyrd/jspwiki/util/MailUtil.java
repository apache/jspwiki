package com.ecyrd.jspwiki.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.*;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.WikiContext;

/**
 * Contains utilities for emailing.
 * 
 * @author Christoph Sauer, Dan Frankowski
 */
public class MailUtil
{
    protected static final Logger log = Logger.getLogger(MailUtil.class);

    public static String MAIL_HOST = "mail.smtp.host";

    public static String MAIL_PORT = "mail.smtp.port";

    public static String MAIL_ACCOUNT = "mail.smtp.account";

    public static String MAIL_PASSWORD = "mail.smtp.password";

    public static String MAIL_SENDER = "mail.from";

    /**
     * Conveniance method, sets the default "to" sender indicated
     * in the jspwiki.properties file. Authenticates against mailserver
     * 
     * @author Christoph Sauer
     *  
     * @param context - in order to read jspwiki.properties
     * @param to - receiver
     * @param subject - subjectline of message
     * @param content - the actual mail message
     */
    public static void sendMessage(WikiContext context, 
                                   String to, 
                                   String subject, 
                                   String content)
                throws AddressException,
                MessagingException 
    {
        sendMessage(context, to, getProperty(context, MAIL_SENDER), subject, content);
    }


    /**
     * Sends a message with subject line to receiver. If account information
     * is indicated in the jspwiki.properties, it uses this information
     * together with the indicated password to authenticate against
     * the mail server.
     * 
     * @author Christoph Sauer
     * 
     * @param context - Used to read jspwiki.properties
     * @param to - Receiver
     * @param from - The address from which the email appears to come 
     * @param subject - Subjectline of message
     * @param content - The actual mail message
     */
    public static void sendMessage(WikiContext context, String to, String from, String subject, String content)
                                                                                                               throws MessagingException
    {
        try
        {
            Properties props = System.getProperties();

            // Overwrite JVM defaults
            String host = getProperty(context, MAIL_HOST);
            if (host != null)
            {
                props.put(MAIL_HOST, host);
            }

            String port = getProperty(context, MAIL_PORT);
            if (port != null)
            {
                props.put(MAIL_PORT, port);
            }

            // Set default if not set by the system...
            if (props.getProperty("mail.transport.protocol") == null)
            {
                props.put("mail.transport.protocol", "smtp");
            }

            log.info("send mail using host " + getProperty(context, MAIL_HOST) );
            log.info("send mail to " + to);
            log.info("send mail from " + from);

            // check if we authenticate:
            String account = getProperty(context, MAIL_ACCOUNT);
            String password = getProperty(context, MAIL_PASSWORD);

            Session session = null;
            if (account != null)
            {

                log.info("send mail using authentication account " + account);

                // we do not allow unauthenticated sending
                props.put("mail.smtp.auth", "true");

                SmtpAuthenticator auth = new SmtpAuthenticator(account, password);

                session = Session.getDefaultInstance(props, auth);
            }
            else
            {

                log.info("send mail using no authentication");

                session = Session.getInstance(props);
            }

            // -- Create a new message --
            Message msg = new MimeMessage(session);

            // -- Set the FROM and TO fields --
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));

            // -- We could include CC recipients too --
            // if (cc != null)
            // msg.setRecipients(Message.RecipientType.CC
            // ,InternetAddress.parse(cc, false));

            // -- Set the subject and body text --
            msg.setSubject(subject);
            msg.setText(content);

            // -- Set some other header information --
            // msg.setHeader("X-Mailer", "JuwiMail");
            msg.setSentDate(new Date());

            // -- Send the message --
            Transport.send(msg);

            log.info("Message sent to " + to + " OK.");
        }
        catch (MessagingException e)
        {
            log.error(e);
            throw e;
        }
    }
    
    /**
     * We can't use the engines variable manager to retrieve mail.x properties.
     * (engine.getVariable()) If we would add this to the var manager we could
     * get into trouble because by some braindamaged mistake a user could use
     * [{$mail.smtp.password}] to display this very information, we do not want
     * to display. TODO: find a better solution
     * 
     * @param context
     * @param name
     * @return
     */
    private static String getProperty(WikiContext context, String name) 
    {
        return context.getEngine().getWikiProperties().getProperty(name).trim();
    }

    /**
     * Send an email message. This is the original Implementation by Dan
     * Frankowski. authentication support is missing. 
     * 
     * Use this if you don't need to authenticate. Be aware that mail servers
     * that do not require authentications are ament to be used as an open relay.
     * Use this in intranet environments.
     * 
     * See <a
     * href="http://tomcat.apache.org/tomcat-5.0-doc/jndi-resources-howto.html#JavaMail%20Sessions">Tomcat
     * 5.0 docs</a>, <a
     * href="http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html#JavaMail%20Sessions">Tomcat
     * 5.5 docs</a>, or <a
     * href="http://docs.sun.com/source/819-0079/dgmail.html">Sun docs</a> for
     * a description of using the JavaMail API.
     * 
     * @author Dan Frankowski
     * @see Message
     * @param to
     *            Email address from which the message is sent
     * @param subject
     *            Subject line
     * @param content
     *            Content (plain text)
     * @throws AddressException
     * @throws MessagingException
     * @throws NamingException
     */
    public static void sendMessage(Properties props, String to, String subject, String content)
                                                                                               throws AddressException,
                                                                                                   MessagingException,
                                                                                                   NamingException
    {
        Session session = Session.getInstance(props);

        Message message = new MimeMessage(session);
        message.setFrom();
        InternetAddress toAddress[] = new InternetAddress[1];
        toAddress[0] = new InternetAddress(to);
        message.setRecipients(Message.RecipientType.TO, toAddress);
        message.setSubject(subject);
        message.setContent(content, "text/plain");
        Transport.send(message);
    }
}

/**
 * 
 * @author Christoph Sauer
 *
 */
class SmtpAuthenticator extends javax.mail.Authenticator
{

    String pass = "";

    String login = "";

    public SmtpAuthenticator()
    {
        super();
    }

    public SmtpAuthenticator(String login, String pass)
    {
        super();
        this.login = login;
        this.pass = pass;
    }

    public PasswordAuthentication getPasswordAuthentication()
    {
        if (pass.equals(""))
            return null;
        
        return new PasswordAuthentication(login, pass);
    }

}