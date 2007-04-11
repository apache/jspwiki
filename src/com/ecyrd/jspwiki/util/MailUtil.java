package com.ecyrd.jspwiki.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;

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
     * @param engine the WikiEngine, supplied to read <code>jspwiki.properties</code>
     * @param to the receiver
     * @param subject the subject line of the message
     * @param content the contents of the mail message
     */
    public static void sendMessage(WikiEngine engine, 
                                   String to, 
                                   String subject, 
                                   String content)
                throws AddressException,
                MessagingException 
    {
        sendMessage(engine, to, getProperty(engine, MAIL_SENDER), subject, content);
    }


    /**
     * Sends a message with subject line to receiver. If account information
     * is indicated in the jspwiki.properties, it uses this information
     * together with the indicated password to authenticate against
     * the mail server.
     * 
     * @param engine the WikiEngine, supplied to read <code>jspwiki.properties</code>
     * @param to the receiver
     * @param from the address from whom the email will be from
     * @param subject the subject line of the message
     * @param content the contents of the mail message
     */
    public static void sendMessage(WikiEngine engine, String to, String from, String subject, String content)
                                                                                                               throws MessagingException
    {
        try
        {
            Properties props = System.getProperties();

            // Overwrite JVM defaults
            String host = getProperty(engine, MAIL_HOST);
            if (host != null)
            {
                props.put(MAIL_HOST, host);
            }

            String port = getProperty(engine, MAIL_PORT);
            if (port != null)
            {
                props.put(MAIL_PORT, port);
            }

            // Set default if not set by the system...
            if (props.getProperty("mail.transport.protocol") == null)
            {
                props.put("mail.transport.protocol", "smtp");
            }

            log.info("send mail using host " + getProperty(engine, MAIL_HOST) );
            log.info("send mail to " + to);
            log.info("send mail from " + from);

            // check if we authenticate:
            String account = getProperty(engine, MAIL_ACCOUNT);
            String password = getProperty(engine, MAIL_PASSWORD);

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
     * We can't use the engines variable manager to retrieve <code>mail.<var>x</var></code> properties.
     * (engine.getVariable()) If we would add this to the var manager we could
     * get into trouble because by some braindamaged mistake a user could use
     * [{$mail.smtp.password}] to display this very information, we do not want
     * to display. TODO: find a better solution
     * 
     * @param engine the WikiEngine
     * @param name the name of the property to retrieve
     * @return the property value
     */
    private static String getProperty(WikiEngine engine, String name) 
    {
        return engine.getWikiProperties().getProperty(name).trim();
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
     * @param to the email address from whom the message will be from
     * @param subject the subject line of the message
     * @param content the contents of the mail message
     * @throws AddressException
     * @throws MessagingException
     * @throws NamingException
     * @see Message
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
 * Simple {@link javax.mail.Authenticator} subclass that authenticates a user to
 * an SMTP server.
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
        else
            return new PasswordAuthentication(login, pass);
    }

}