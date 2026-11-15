<%--
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
--%>

<%@ page import="java.util.*"%>
<%@ page import="java.text.*"%>
<%@ page import="jakarta.mail.*"%>
<%@ page import="jakarta.servlet.jsp.jstl.fmt.*"%>
<%@ page import="org.apache.logging.log4j.Logger" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>
<%@ page import="org.apache.wiki.api.core.Context" %>
<%@ page import="org.apache.wiki.api.core.ContextEnum" %>
<%@ page import="org.apache.wiki.api.core.Engine"%>
<%@ page import="org.apache.wiki.api.core.Session"%>
<%@ page import="org.apache.wiki.api.spi.Wiki"%>
<%@ page import="org.apache.wiki.auth.*"%>
<%@ page import="org.apache.wiki.auth.user.*"%>
<%@ page import="org.apache.wiki.i18n.*"%>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.ui.TemplateManager" %>
<%@ page import="org.apache.wiki.url.URLConstructor"%>
<%@ page import="org.apache.wiki.util.*"%>
<%@ page errorPage="/Error.jsp"%>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%!Logger log = LogManager.getLogger( "JSPWiki" );

    String message = null;

    /*
     * Builds the login URL for the reset e-mails from configuration ("jspwiki.baseURL", the
     * externally reachable base URL of the wiki including the context path), never from request
     * headers (Host / X-Forwarded-*), which are attacker-controlled. If the property is not set,
     * a server-relative URL is used.
     */
    private String buildLoginUrl( Engine wiki ) {
        String loginUrl = wiki.getManager( URLConstructor.class ).makeURL( ContextEnum.PAGE_NONE.getRequestContext(), "Login.jsp", "" );
        String base = TextUtil.getStringProperty( wiki.getWikiProperties(), "jspwiki.baseURL", "" ).trim();
        if( base.isEmpty() ) {
            return loginUrl;
        }
        while( base.endsWith( "/" ) ) {
            base = base.substring( 0, base.length() - 1 );
        }
        String contextPath = wiki.getBaseURL();
        if( !contextPath.isEmpty() && loginUrl.startsWith( contextPath ) ) {
            loginUrl = loginUrl.substring( contextPath.length() );
        }
        return base + loginUrl;
    }

    /*
     * Step 1 of the password reset: mail a single-use, expiring reset token to the account owner.
     * Nothing is disclosed to the requester: the caller reports the same message whether or not
     * the account exists, so this cannot be used as an account enumeration oracle, and the stored
     * credential is not touched here.
     */
    public void requestResetToken( Engine wiki, HttpServletRequest request, ResourceBundle rb ) {
        String name = request.getParameter( "name" );
        UserDatabase userDatabase = wiki.getManager( UserManager.class ).getUserDatabase();

        try {
            UserProfile profile = userDatabase.findByEmail( name );
            String token = PasswordResetTokenStore.getInstance().issue( profile.getLoginName() );
            if( token == null ) {
                // A token is already outstanding for this account (or the store is full): throttled.
                log.info( "Password reset token request throttled." );
                return;
            }

            Object[] args = { profile.getLoginName(), token, buildLoginUrl( wiki ), wiki.getApplicationName() };
            Object[] args2 = { wiki.getApplicationName() };
            MailUtil.sendMessage( wiki.getWikiProperties(),
                                  profile.getEmail(),
                                  MessageFormat.format( rb.getString( "lostpwd.token.subject" ), args2 ),
                                  MessageFormat.format( rb.getString( "lostpwd.token.email" ), args ) );

            log.info( "User " + profile.getLoginName() + " requested and received a password reset token." );
        } catch( NoSuchPrincipalException e ) {
            // Deliberately indistinguishable from success for the requester.
            log.info( "Password reset requested for a non-existent account." );
        } catch( Exception e ) {
            log.error( "Tried to send a password reset token and got an exception: " + e );
        }
    }

    /*
     * Step 2 of the password reset: redeem the emailed token. Only here, with proof of control
     * over the account mailbox, is the stored credential changed.
     */
    public boolean redeemResetToken( Engine wiki, HttpServletRequest request, ResourceBundle rb ) {
        String token = request.getParameter( "resettoken" );
        String loginName = PasswordResetTokenStore.getInstance().redeem( token == null ? null : token.trim() );
        if( loginName == null ) {
            message = rb.getString( "lostpwd.reset.unable" );
            log.info( "Password reset attempted with an invalid, used or expired token." );
            return false;
        }

        UserDatabase userDatabase = wiki.getManager( UserManager.class ).getUserDatabase();
        boolean success = false;

        try {
            UserProfile profile = userDatabase.findByLoginName( loginName );
            String randomPassword = TextUtil.generateRandomPassword();

            // Try sending email first, as that is more likely to fail.

            Object[] args = { profile.getLoginName(), randomPassword, buildLoginUrl( wiki ), wiki.getApplicationName() };

            String mailMessage = MessageFormat.format( rb.getString( "lostpwd.newpassword.email" ), args );

            Object[] args2 = { wiki.getApplicationName() };
            MailUtil.sendMessage( wiki.getWikiProperties(),
            		              profile.getEmail(),
            		              MessageFormat.format( rb.getString( "lostpwd.newpassword.subject" ), args2 ),
                                  mailMessage );

            log.info( "User " + profile.getLoginName() + " redeemed a reset token and received a new password." );

            // Mail succeeded.  Now reset the password.
            // If this fails, we're kind of screwed, because we already emailed.
            profile.setPassword( randomPassword );
            userDatabase.save( profile );
            success = true;
        } catch( SendFailedException e ) {
            message = rb.getString( "lostpwd.nomail" );
            log.error( "Tried to reset password and got SendFailedException: " + e );
        } catch( AuthenticationFailedException e ) {
            message = rb.getString( "lostpwd.nomail" );
            log.error( "Tried to reset password and got AuthenticationFailedException: " + e );
        } catch( Exception e ) {
            message = rb.getString( "lostpwd.nomail" );
            log.error( "Tried to reset password and got another exception: " + e );
        }
        return success;
    }
%>
<%
    Engine wiki = Wiki.engine().find( getServletConfig() );

    //Create wiki context like in Login.jsp:
    //don't check for access permissions: if you have lost your password you cannot login!
    Context wikiContext = ( Context )pageContext.getAttribute( Context.ATTR_CONTEXT, PageContext.REQUEST_SCOPE );

    // If no context, it means we're using container auth.  So, create one anyway
    if( wikiContext == null ) {
        wikiContext = Wiki.context().create( wiki, request, ContextEnum.WIKI_LOGIN.getRequestContext() ); /* reuse login context ! */
        pageContext.setAttribute( Context.ATTR_CONTEXT, wikiContext, PageContext.REQUEST_SCOPE );
    }

    ResourceBundle rb = Preferences.getBundle( wikiContext, "CoreResources" );

    Session wikiSession = wikiContext.getWikiSession();
    String action = request.getParameter( "action" );

    boolean done = false;

    // State changes only happen on POST: GET never mutates anything, and the
    // CsrfProtectionFilter validates the anti-CSRF token on every POST.
    if( "resetPassword".equals( action ) && "POST".equalsIgnoreCase( request.getMethod() ) ) {
        String resetToken = request.getParameter( "resettoken" );
        if( resetToken != null && !resetToken.trim().isEmpty() ) {
            // Step 2: redeem the emailed token; only now is the stored credential changed.
            if( redeemResetToken( wiki, request, rb ) ) {
                done = true;
                wikiSession.addMessage( "resetpwok", rb.getString( "lostpwd.emailed" ) );
                pageContext.setAttribute( "passwordreset", "done" );
            } else {
                // Error
                wikiSession.addMessage( "resetpw", message );
            }
        } else {
            // Step 1: always report the same outcome, whether or not the account exists.
            requestResetToken( wiki, request, rb );
            wikiSession.addMessage( "resetpwok", rb.getString( "lostpwd.tokenmailed" ) );
        }
    }

    response.setContentType( "text/html; charset=" + wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );

    String contentPage = wiki.getManager( TemplateManager.class ).findJSP( pageContext, wikiContext.getTemplate(), "ViewTemplate.jsp" );
%>
<wiki:Include page="<%=contentPage%>" />
