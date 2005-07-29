package com.ecyrd.jspwiki.auth;

import java.net.URL;
import java.security.AccessController;
import java.security.Policy;
import java.security.PrivilegedAction;

import javax.security.auth.login.Configuration;

import sun.security.provider.PolicyFile;
import sun.security.util.PropertyExpander;

import com.sun.security.auth.login.ConfigFile;

/**
 * <p>
 * Initializes JVM configurations for JAAS and Java 2 security policy. Callers
 * can use the static methods in this class ({@link #isJaasConfigured()}
 * &nbsp;and {@link #isSecurityPolicyConfigured()}) to inquire whether a JAAS
 * login configuration exists, or whether a custom Java security policy is in
 * use. Additional methods allow callers to set the JAAS and security policy
 * configurations to supplied URLs ({@link #setJaasConfiguration(URL)}
 * &nbsp;and {@link #setSecurityPolicy(URL)}).
 * </p>
 * <p>
 * If either the JAAS configuration and security policy are set using methods in
 * this class, the resulting configuration or policy is <i>global</i> to the
 * JVM. Thus, in a multi-webapp scenario, this means that the first webapp to be
 * loaded by the container wins. Thus, for containers hosting multiple wikis,
 * the administrator will need to manually configure the
 * <code>java.security.policy</code> and
 * <code>java.security.auth.login.config properties</code>. In other words,
 * multi-wiki deployments will always require manual (one-time) configuration.
 * </p>
 * <p>
 * The security policy-related methods {@link #isSecurityPolicyConfigured()}
 * &nbsp;and {@link #setSecurityPolicy(URL)}) assume that:
 * </p>
 * <ul>
 * <li>The Policy implementation for the JVM is
 * <code>sun.security.provider.PolicyFile</code>. This should be a safe
 * assumption in most cases, although recent versions of WebSphere and WebLogic
 * use custom Policy implementations. If the Policy implementation is not of
 * type PolicyFile, the policy installation fails. This assumption may be 
 * relaxed in the future; the important question is whether the Policy 
 * implementation parses standard policy files (versus XML, for example), not
 * whether the implementing class is of type PolicyFile <i>per se</i>.</li>
 * <li>The web container doesn't use a "double-equals" command-line assignment
 * to override the security policy ( <i>e.g. </i>,
 * <code>-Djava.security.policy==jspwiki.policy</code>). Note that Tomcat 4
 * and higher, when run using the "-security" option, does this.</li>
 * </ul>
 * <p>
 * To interoperate with <i>any</i> container running a security policy, the
 * container's JVM security policy should include a short set of permission
 * grant similar to the following:
 * </p>
 * <blockquote><code>keystore "jspwiki.jks";<br/>
 * &nbsp;&nbsp;...<br/>
 * grant signedBy "jspwiki" {<br/>
 * &nbsp;&nbsp;permission java.security.SecurityPermission, "getPolicy";<br/>
 * &nbsp;&nbsp;permission java.security.SecurityPermission, "setPolicy";<br/>
 * &nbsp;&nbsp;permission java.util.PropertyPermission "java.security.auth.login.config", "write";<br/>
 * &nbsp;&nbsp;permission java.util.PropertyPermission "java.security.policy", "read,write";<br/>
 * &nbsp;&nbsp;permission javax.security.auth.AuthPermission, "getLoginConfiguration";<br/>
 * &nbsp;&nbsp;permission javax.security.auth.AuthPermission, "setLoginConfiguration";<br/>
 * };</code>
 * </blockquote>
 * <p>
 * The <code>signedBy</code> value should match the alias of a digital
 * certificate in the named keystore ( <i>e.g. </i>, <code>jspwiki.jks</code>).
 * If the full path to the keystore is not suppled, it is assumed to be in the
 * same directory as the policy file.
 * </p>
 * 
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-07-29 14:48:53 $
 * @since 2.3
 */
public class PolicyLoader 
{    
    /**
     * Private constructor to prevent direct instantiation.
     */
    private PolicyLoader() 
    {
    }
    
    /**
     * <p>
     * Returns <code>true</code> if the JAAS login configuration exists.
     * Normally, JAAS is configured by setting the system property
     * <code>java.security.auth.login.config</code> at JVM startup.
     * </p>
     * <p>
     * This method attempts to perform a highly privileged operation. If the JVM
     * runs with a SecurityManager, the following permission must be granted to
     * the codesource containing this class:
     * </p>
     * <code><ul>
     * <li>permission javax.security.auth.AuthPermission,
     * "getLoginConfiguration"</li>
     * </ul></code>
     * 
     * @return <code>true</code> if
     *         {@link javax.security.auth.login.Configuration#getConfiguration()}
     *         is not <code>null</code> ;&nbsp; <code>false</code> otherwise.
     * @throws SecurityException if the codesource containing this class posesses
     *           insufficient permmissions when running with a SecurityManager
     */
    public static boolean isJaasConfigured() throws SecurityException 
    {
        Boolean configured = (Boolean) AccessController
        .doPrivileged(new PrivilegedAction() {
            
            public Object run() 
            {
                boolean isConfigured = false;
                try 
                {
                    Configuration config = Configuration.getConfiguration();
                    isConfigured = (config != null);
                }
                catch (SecurityException e) {}
                return (Boolean.valueOf(isConfigured));
            };
        });
        return configured.booleanValue();
    }
    
    /**
     * <p>
     * Returns <code>true</code> if a custom Java security policy configuration
     * exists. Normally, the Java security policy is configured by setting the
     * system property <code>java.security.policy</code> at JVM startup.
     * </p>
     * <p>
     * This method attempts to perform a highly privileged operation. If the JVM
     * runs with a SecurityManager, the following permission must be granted to
     * the codesource containing this class:
     * </p>
     * <code><ul>
     * <li>permission java.util.PropertyPermission
     * "java.security.policy", "read"</li>
     * </ul></code>
     * 
     * @return <code>true</code> if the system property
     *         <code>java.security.policy</code> is not <code>null</code>;
     *         &nbsp; <code>false</code> otherwise.
     * @throws SecurityException if the codesource containing this class posesses
     *           insufficient permmissions when running with a SecurityManager
     */
    public static boolean isSecurityPolicyConfigured() throws SecurityException 
    {
        String policy = (String) AccessController
        .doPrivileged(new PrivilegedAction() {
            
            public Object run() 
            {
                return System.getProperty("java.security.policy");
            }
        });
        return (policy != null);
    }
    
    /**
     * Sets the JAAS login configuration file, overwriting the existing
     * configuration. If the configuration file pointed to by the URL does not
     * exist, a SecurityException is thrown.
     * <p>
     * This method attempts to perform several highly privileged operations. If
     * the JVM runs with a SecurityManager, the following permissions must be
     * granted to the codesource containing this class:
     * </p>
     * <code><ul>
     * <li>permission java.util.PropertyPermission
     * "java.security.auth.login.config", "write"</li>
     * <li>permission javax.security.auth.AuthPermission,
     * "getLoginConfiguration"</li>
     * <li>permission javax.security.auth.AuthPermission,
     * "setLoginConfiguration"</li>
     * </ul></code>
     * 
     * @param url the URL of the login configuration file. If the URL contains
     *          properties such as <code>${java.home}</code>, they will be
     *          expanded.
     * @throws SecurityException if:
     *           <ul>
     *           <li>the supplied URL is <code>null</code></li>
     *           <li>properties cannot be expanded</li>
     *           <li>the codesource containing this class does not posesses
     *           sufficient permmissions when running with a SecurityManager</li>
     *           </ul>
     */
    public static void setJaasConfiguration(final URL url)
    throws SecurityException 
    {
        if (url == null) 
        {
            throw new SecurityException("URL for JAAS configuration cannot be null.");
        }
        
        final String new_url;
        try 
        {
            new_url = PropertyExpander.expand(url.toExternalForm());
        }
        catch (PropertyExpander.ExpandException peee) 
        {
            throw new SecurityException("Cannot expand: " + peee.getMessage());
        }
        
        // Now, set the new config
        AccessController.doPrivileged(new PrivilegedAction() {
            
            public Object run() 
            {
                System.setProperty("java.security.auth.login.config", new_url);
                Configuration config = new ConfigFile();
                Configuration.setConfiguration(config);
                return null;
            }
        });
    }
    
    /**
     * <p>
     * Sets the Java security policy, overwriting any custom policy settings. This
     * method sets the value of the system property
     * <code>java.security.policy</code> to the supplied URL, then calls
     * {@link java.security.Policy#setPolicy(java.security.Policy)}&nbsp;with a
     * newly-instantiated instance of
     * <code>sun.security.provider.PolicyFile</code> (the J2SE default
     * implementation). The new Policy, once set, reloads the default system
     * policies enumerated by the <code>policy.url.<i>n</i></code> entries in
     * <code><i>JAVA_HOME</i>/lib/security/java.policy</code>, followed by the
     * user-supplied policy file.
     * </p>
     * <p>
     * This method attempts to perform several highly privileged operations. If
     * the JVM runs with a SecurityManager, the following permissions must be
     * granted to the codesource containing this class:
     * </p>
     * <code><ul>
     * <li>permission java.security.SecurityPermission, "getPolicy"
     * </li>
     * <li>permission java.security.SecurityPermission, "setPolicy"
     * </li>
     * <li>permission java.util.PropertyPermission}
     * "java.security.policy", "write"</li>
     * </ul></code>
     * 
     * @param url the URL of the security policy file. If the URL contains
     *          properties such as <code>${java.home}</code>, they will be
     *          expanded.
     * @throws SecurityException if:
     *           <ul>
     *           <li>the supplied URL is <code>null</code></li>
     *           <li>properties cannot be expanded</li>
     *           <li>the codesource containing this class does not posesses
     *           sufficient permmissions when running with a SecurityManager</li>
     *           <li>the JVM's current Policy implementation is not of type
     *           <code>sun.security.provider.PolicyFile</code></li>
     *           </ul>
     */
    public static void setSecurityPolicy(URL url) throws SecurityException 
    {
        if (url == null) 
        {
            throw new SecurityException("URL for security policy cannot be null.");
        }
        
        final String new_url;
        try 
        {
            new_url = PropertyExpander.expand(url.toExternalForm());
        }
        catch (PropertyExpander.ExpandException peee) 
        {
            throw new SecurityException("Cannot expand: " + peee.getMessage());
        }
        
        if (!(Policy.getPolicy() instanceof PolicyFile)) 
        {
            throw new SecurityException(
            "Policy implementation must be of type sun.security.provider.PolicyFile.");
        }
        
        // Now, set the new policy
        AccessController.doPrivileged(new PrivilegedAction() {
            
            public Object run() 
            {
                Policy.setPolicy(null);
                System.setProperty("java.security.policy", new_url);
                Policy policy = new PolicyFile();
                Policy.setPolicy(policy);
                return null;
            }
        });
    }
    
}
