package com.ecyrd.jspwiki.util;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

/**
 *  Contains utilities for emailing.
 *
 *  @author Dan Frankowski
 */
public class MailUtil 
{
    protected static final Logger log = Logger.getLogger( MailUtil.class );

    /**
     * Send an email message.
     * 
     * See <a href="http://tomcat.apache.org/tomcat-5.0-doc/jndi-resources-howto.html#JavaMail%20Sessions">Tomcat 5.0 docs</a>,
     * <a href="http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html#JavaMail%20Sessions">Tomcat 5.5 docs</a>,
     * or <a href="http://docs.sun.com/source/819-0079/dgmail.html">Sun docs</a> for
     * a description of using the JavaMail API.
     * 
     * @see Message
     * 
     * @param to       Email address from which the message is sent
     * @param subject  Subject line
     * @param content  Content (plain text)
     * 
     * @throws AddressException
     * @throws MessagingException
     * @throws NamingException
     */
    public static void sendMessage( Properties props, String to, String subject, String content )
            throws AddressException, MessagingException, NamingException 
    {
        Session session = Session.getInstance( props );
        
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
