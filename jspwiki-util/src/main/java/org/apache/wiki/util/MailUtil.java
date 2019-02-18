/* 
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
package org.apache.wiki.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;


/**
 * <p>Contains static methods for sending e-mails to recipients using JNDI-supplied
 * <a href="http://java.sun.com/products/javamail/">JavaMail</a>
 * Sessions supplied by a web container (preferred) or configured via
 * <code>jspwiki.properties</code>; both methods are described below.
 * Because most e-mail servers require authentication,
 * for security reasons implementors are <em>strongly</em> encouraged to use
 * container-managed JavaMail Sessions so that passwords are not exposed in
 * <code>jspwiki.properties</code>.</p>
 * <p>To enable e-mail functions within JSPWiki, administrators must do three things:
 * ensure that the required JavaMail JARs are on the runtime classpath, configure
 * JavaMail appropriately, and (recommdended) configure the JNDI JavaMail session factory.</p>
 * <strong>JavaMail runtime JARs</strong>
 * <p>The first step is easy: JSPWiki bundles
 * recent versions of the required JavaMail <code>mail.jar</code> and
 * <code>activation.jar</code> into the JSPWiki WAR file; so, out of the box
 * this is already taken care of. However, when using JNDI-supplied
 * Session factories, these should be moved, <em>not copied</em>, to a classpath location
 * where the JARs can be shared by both the JSPWiki webapp and the container. For example,
 * Tomcat 5 provides the directory <code><var>$CATALINA_HOME</var>/common/lib</code>
 * for storage of shared JARs; move <code>mail.jar</code> and <code>activation.jar</code>
 * there instead of keeping them in <code>/WEB-INF/lib</code>.</p>
 * <strong>JavaMail configuration</strong>
 * <p>Regardless of the method used for supplying JavaMail sessions (JNDI container-managed
 * or via <code>jspwiki.properties</code>, JavaMail needs certain properties
 * set in order to work correctly. Configurable properties are these:</p>
 * <table border="1">
 *   <tr>
 *   <thead>
 *     <th>Property</th>
 *     <th>Default</th>
 *     <th>Definition</th>
 *   <thead>
 *   </tr>
 *   <tr>
 *     <td><code>jspwiki.mail.jndiname</code></td>
 *     <td><code>mail/Session</code></td>
 *     <td>The JNDI name of the JavaMail session factory</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.smtp.host</code></td>
 *     <td><code>127.0.0.1</code></td>
 *     <td>The SMTP mail server from which messages will be sent.</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.smtp.port</code></td>
 *     <td><code>25</code></td>
 *     <td>The port number of the SMTP mail service.</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.smtp.account</code></td>
 *     <td>(not set)</td>
 *     <td>The user name of the sender. If this value is supplied, the JavaMail
 *     session will attempt to authenticate to the mail server before sending
 *     the message. If not supplied, JavaMail will attempt to send the message
 *     without authenticating (i.e., it will use the server as an open relay).
 *     In real-world scenarios, you should set this value.</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.smtp.password</code></td>
 *     <td>(not set)</td>
 *     <td>The password of the sender. In real-world scenarios, you
 *     should set this value.</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.from</code></td>
 *     <td><code><var>${user.name}</var>@<var>${mail.smtp.host}</var>*</code></td>
 *     <td>The e-mail address of the sender.</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.smtp.timeout</code></td>
 *     <td><code>5000*</code></td>
 *     <td>Socket I/O timeout value, in milliseconds. The default is 5 seconds.</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.smtp.connectiontimeout</code></td>
 *     <td><code>5000*</code></td>
 *     <td>Socket connection timeout value, in milliseconds. The default is 5 seconds.</td>
 *   </tr>
 *   <tr>
 *     <td><code>mail.smtp.starttls.enable</code></td>
 *     <td><code>true*</code></td>
 *     <td>If true, enables the use of the STARTTLS command (if
 *     supported by the server) to switch the connection to a
 *     TLS-protected connection before issuing any login commands.
 *     Note that an appropriate trust store must configured so that
 *     the client will trust the server's certificate. By default,
 *     the JRE trust store contains root CAs for most public certificate
 *     authorities.</td>
 *   </tr>
 * </table>
 * <p>*These defaults apply only if the stand-alone Session factory is used
 * (that is, these values are obtained from <code>jspwiki.properties</code>).
 * If using a container-managed JNDI Session factory, the container will
 * likely supply its own default values, and you should probably override
 * them (see the next section).</p>
 * <strong>Container JNDI Session factory configuration</strong>
 * <p>You are strongly encouraged to use a container-managed JNDI factory for
 * JavaMail sessions, rather than configuring JavaMail through <code>jspwiki.properties</code>.
 * To do this, you need to two things: uncomment the <code>&lt;resource-ref&gt;</code> block
 * in <code>/WEB-INF/web.xml</code> that enables container-managed JavaMail, and
 * configure your container's JavaMail resource factory. The <code>web.xml</code>
 * part is easy: just uncomment the section that looks like this:</p>
 * <pre>&lt;resource-ref&gt;
 *   &lt;description>Resource reference to a container-managed JNDI JavaMail factory for sending e-mails.&lt;/description&gt;
 *   &lt;res-ref-name>mail/Session&lt;/res-ref-name&gt;
 *   &lt;res-type>javax.mail.Session&lt;/res-type&gt;
 *   &lt;res-auth>Container&lt;/res-auth&gt;
 * &lt;/resource-ref&gt;</pre>
 * <p>To configure your container's resource factory, follow the directions supplied by
 * your container's documentation. For example, the
 * <a href="http://tomcat.apache.org/tomcat-5.5-doc/jndi-resources-howto.html#JavaMail%20Sessions">Tomcat
 * 5.5 docs</a> state that you need a properly configured <code>&lt;Resource&gt;</code>
 * element inside the JSPWiki webapp's <code>&lt;Context&gt;</code> declaration. Here's an example shows
 * how to do it:</p>
 * <pre>&lt;Context ...&gt;
 * ...
 * &lt;Resource name="mail/Session" auth="Container"
 *           type="javax.mail.Session"
 *           mail.smtp.host="127.0.0.1"/&gt;
 *           mail.smtp.port="25"/&gt;
 *           mail.smtp.account="your-account-name"/&gt;
 *           mail.smtp.password="your-password"/&gt;
 *           mail.from="Snoop Dogg &lt;snoop@dogg.org&gt;"/&gt;
 *           mail.smtp.timeout="5000"/&gt;
 *           mail.smtp.connectiontimeout="5000"/&gt;
 *           mail.smtp.starttls.enable="true"/&gt;
 * ...
 * &lt;/Context&gt;</pre>
 * <p>Note that with Tomcat (and most other application containers) you can also declare the JavaMail
 * JNDI factory as a global resource, shared by all applications, instead of as a local JSPWiki
 * resource as we have done here. For example, the following entry in
 * <code><var>$CATALINA_HOME</var>/conf/server.xml</code> creates a global resource:</p>
 * <pre>&lt;GlobalNamingResources&gt;
 *   &lt;Resource name="mail/Session" auth="Container"
 *             type="javax.mail.Session"
 *             ...
 *             mail.smtp.starttls.enable="true"/&gt;
 * &lt;/GlobalNamingResources&gt;</pre>
 * <p>This approach &#8212; creating a global JNDI resource &#8212; yields somewhat decreased
 * deployment complexity because the JSPWiki webapp no longer needs its own JavaMail resource
 * declaration. However, it is slightly less secure because it means that all other applications
 * can now obtain a JavaMail session if they want to. In many cases, this <em>is</em> what
 * you want.</p>
 * <p>NOTE: Versions of Tomcat 5.5 later than 5.5.17, and up to and including 5.5.23 have a
 * b0rked version of <code><var>$CATALINA_HOME</var>/common/lib/naming-factory.jar</code>
 * that prevents usage of JNDI. To avoid this problem, you should patch your 5.5.23 version
 * of <code>naming-factory.jar</code> with the one from 5.5.17. This is a known issue
 * and the bug report (#40668) is
 * <a href="http://issues.apache.org/bugzilla/show_bug.cgi?id=40668">here</a>.
 *
 */
public final class MailUtil {

    private static final String JAVA_COMP_ENV = "java:comp/env";

    private static final String FALSE = "false";

    private static final String TRUE = "true";

    private static boolean c_useJndi = true;

    private static final String PROP_MAIL_AUTH = "mail.smtp.auth";

    protected static final Logger log = Logger.getLogger(MailUtil.class);

    protected static final String DEFAULT_MAIL_JNDI_NAME       = "mail/Session";

    protected static final String DEFAULT_MAIL_HOST            = "localhost";

    protected static final String DEFAULT_MAIL_PORT            = "25";

    protected static final String DEFAULT_MAIL_TIMEOUT         = "5000";

    protected static final String DEFAULT_MAIL_CONN_TIMEOUT    = "5000";

    protected static final String DEFAULT_SENDER               = "jspwiki@localhost";

    protected static final String PROP_MAIL_JNDI_NAME          = "jspwiki.mail.jndiname";

    protected static final String PROP_MAIL_HOST               = "mail.smtp.host";

    protected static final String PROP_MAIL_PORT               = "mail.smtp.port";

    protected static final String PROP_MAIL_ACCOUNT            = "mail.smtp.account";

    protected static final String PROP_MAIL_PASSWORD           = "mail.smtp.password";

    protected static final String PROP_MAIL_TIMEOUT            = "mail.smtp.timeout";

    protected static final String PROP_MAIL_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";

    protected static final String PROP_MAIL_TRANSPORT          = "smtp";

    protected static final String PROP_MAIL_SENDER             = "mail.from";

    protected static final String PROP_MAIL_STARTTLS           = "mail.smtp.starttls.enable";

    private static String c_fromAddress = null;
    
    /**
     *  Private constructor prevents instantiation.
     */
    private MailUtil()
    {
    }

    /**
     * <p>Sends an e-mail to a specified receiver using a JavaMail Session supplied
     * by a JNDI mail session factory (preferred) or a locally initialized
     * session based on properties in <code>jspwiki.properties</code>.
     * See the top-level JavaDoc for this class for a description of
     * required properties and their default values.</p>
     * <p>The e-mail address used for the <code>to</code> parameter must be in
     * RFC822 format, as described in the JavaDoc for {@link javax.mail.internet.InternetAddress}
     * and more fully at
     * <a href="http://www.freesoft.org/CIE/RFC/822/index.htm">http://www.freesoft.org/CIE/RFC/822/index.htm</a>.
     * In other words, e-mail addresses should look like this:</p>
     * <blockquote><code>Snoop Dog &lt;snoop.dog@shizzle.net&gt;<br/>
     * snoop.dog@shizzle.net</code></blockquote>
     * <p>Note that the first form allows a "friendly" user name to be supplied
     * in addition to the actual e-mail address.</p>
     *
     * @param props the properties that contain mail session properties
     * @param to the receiver
     * @param subject the subject line of the message
     * @param content the contents of the mail message, as plain text
     * @throws AddressException If the address is invalid
     * @throws MessagingException If the message cannot be sent.
     */
    public static void sendMessage( Properties props, String to, String subject, String content)
        throws AddressException, MessagingException
    {
        Session session = getMailSession( props );
        getSenderEmailAddress(session, props);

        try
        {
            // Create and address the message
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(c_fromAddress));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            msg.setSubject(subject, "UTF-8");
            msg.setText(content, "UTF-8");
            msg.setSentDate(new Date());

            // Send and log it
            Transport.send(msg);
            if (log.isInfoEnabled())
            {
                log.info("Sent e-mail to=" + to + ", subject=\"" + subject + "\", used "
                         + (c_useJndi ? "JNDI" : "standalone") + " mail session.");
            }
        }
        catch (MessagingException e)
        {
            log.error(e);
            throw e;
        }
    }
    
    // --------- JavaMail Session Helper methods  --------------------------------

    /**
     * Gets the Sender's email address from JNDI Session if available, otherwise
     * from the jspwiki.properties or lastly the default value.
     * @param pSession <code>Session</code>
     * @param pProperties <code>Properties</code>
     * @return <code>String</code>
     */
    protected static String getSenderEmailAddress(Session pSession, Properties pProperties)
    {
        if( c_fromAddress == null )
        {
            // First, attempt to get the email address from the JNDI Mail
            // Session.
            if( pSession != null && c_useJndi )
            {
                c_fromAddress = pSession.getProperty( MailUtil.PROP_MAIL_SENDER );
            }
            // If unsuccessful, get the email address from the properties or
            // default.
            if( c_fromAddress == null )
            {
                c_fromAddress = pProperties.getProperty( PROP_MAIL_SENDER, DEFAULT_SENDER ).trim();
                if( log.isDebugEnabled() )
                    log.debug( "Attempt to get the sender's mail address from the JNDI mail session failed, will use \""
                               + c_fromAddress + "\" (configured via jspwiki.properties or the internal default)." );
            }
            else
            {
                if( log.isDebugEnabled() )
                    log.debug( "Attempt to get the sender's mail address from the JNDI mail session was successful (" + c_fromAddress
                               + ")." );
            }
        }
        return c_fromAddress;
    }

    /**
     * Returns the Mail Session from either JNDI or creates a stand-alone.
     * @param props a the properties that contain mail session properties
     * @return <code>Session</code>
     */
    private static Session getMailSession(Properties props)
    {
        Session result = null;
        String jndiName = props.getProperty(PROP_MAIL_JNDI_NAME, DEFAULT_MAIL_JNDI_NAME).trim();

        if (c_useJndi)
        {
            // Try getting the Session from the JNDI factory first
            if ( log.isDebugEnabled() )
                log.debug("Try getting a mail session via JNDI name \"" + jndiName + "\".");
            try
            {
                result = getJNDIMailSession(jndiName);
            }
            catch (NamingException e)
            {
                // Oops! JNDI factory must not be set up
                c_useJndi = false;
                if ( log.isInfoEnabled() )
                    log.info("Unable to get a mail session via JNDI, will use custom settings at least until next startup.");
            }
        }

        // JNDI failed; so, get the Session from the standalone factory
        if (result == null)
        {
            if ( log.isDebugEnabled() )
                log.debug("Getting a standalone mail session configured by jspwiki.properties and/or internal default values.");
            result = getStandaloneMailSession(props);
        }
        return result;
    }

    /**
     * Returns a stand-alone JavaMail Session by looking up the correct
     * mail account, password and host from a supplied set of properties.
     * If the JavaMail property {@value #PROP_MAIL_ACCOUNT} is set to
     * a value that is non-<code>null</code> and of non-zero length, the
     * Session will be initialized with an instance of
     * {@link javax.mail.Authenticator}.
     * @param props the properties that contain mail session properties
     * @return the initialized JavaMail Session
     */
    protected static Session getStandaloneMailSession( Properties props ) {
        // Read the JSPWiki settings from the properties
        String host     = props.getProperty( PROP_MAIL_HOST, DEFAULT_MAIL_HOST );
        String port     = props.getProperty( PROP_MAIL_PORT, DEFAULT_MAIL_PORT );
        String account  = props.getProperty( PROP_MAIL_ACCOUNT );
        String password = props.getProperty( PROP_MAIL_PASSWORD );
        String timeout  = props.getProperty( PROP_MAIL_TIMEOUT, DEFAULT_MAIL_TIMEOUT);
        String conntimeout = props.getProperty( PROP_MAIL_CONNECTION_TIMEOUT, DEFAULT_MAIL_CONN_TIMEOUT );
        boolean starttls = TextUtil.getBooleanProperty( props, PROP_MAIL_STARTTLS, true);
        
        boolean useAuthentication = account != null && account.length() > 0;

        Properties mailProps = new Properties();

        // Set JavaMail properties
        mailProps.put( PROP_MAIL_HOST, host );
        mailProps.put( PROP_MAIL_PORT, port );
        mailProps.put( PROP_MAIL_TIMEOUT, timeout );
        mailProps.put( PROP_MAIL_CONNECTION_TIMEOUT, conntimeout );
        mailProps.put( PROP_MAIL_STARTTLS, starttls ? TRUE : FALSE );

        // Add SMTP authentication if required
        Session session = null;
        if ( useAuthentication )
        {
            mailProps.put( PROP_MAIL_AUTH, TRUE );
            SmtpAuthenticator auth = new SmtpAuthenticator( account, password );

            session = Session.getInstance( mailProps, auth );
        }
        else
        {
            session = Session.getInstance( mailProps );
        }

        if ( log.isDebugEnabled() )
        {
            String mailServer = host + ":" + port + ", account=" + account + ", password not displayed, timeout="
            + timeout + ", connectiontimeout=" + conntimeout + ", starttls.enable=" + starttls
            + ", use authentication=" + ( useAuthentication ? TRUE : FALSE );
            log.debug( "JavaMail session obtained from standalone mail factory: " + mailServer );
        }
        return session;
    }


    /**
     * Returns a JavaMail Session instance from a JNDI container-managed factory.
     * @param jndiName the JNDI name for the resource. If <code>null</code>, the default value
     * of <code>mail/Session</code> will be used
     * @return the initialized JavaMail Session
     * @throws NamingException if the Session cannot be obtained; for example, if the factory is not configured
     */
    protected static Session getJNDIMailSession( String jndiName ) throws NamingException
    {
        Session session = null;
        try
        {
            Context initCtx = new InitialContext();
            Context ctx = (Context) initCtx.lookup( JAVA_COMP_ENV );
            session = (Session) ctx.lookup( jndiName );
        }
        catch( NamingException e )
        {
            log.warn( "JNDI mail session initialization error: " + e.getMessage() );
            throw e;
        }
        if ( log.isDebugEnabled() )
        {
            log.debug( "mail session obtained from JNDI mail factory: " + jndiName );
        }
        return session;
    }

    /**
     * Simple {@link javax.mail.Authenticator} subclass that authenticates a user to
     * an SMTP server.
     */
    protected static class SmtpAuthenticator extends Authenticator {

        private static final String BLANK = "";
        private final String m_pass;
        private final String m_login;

        /**
         * Constructs a new SmtpAuthenticator with a supplied username and password.
         * @param login the user name
         * @param pass the password
         */
        public SmtpAuthenticator(String login, String pass)
        {
            super();
            m_login =   login == null ? BLANK : login;
            m_pass =     pass == null ? BLANK : pass;
        }

        /**
         * Returns the password used to authenticate to the SMTP server.
         * @return <code>PasswordAuthentication</code>
         */
        public PasswordAuthentication getPasswordAuthentication()
        {
            if ( BLANK.equals(m_pass) )
            {
                return null;
            }

            return new PasswordAuthentication( m_login, m_pass );
        }

    }

}
