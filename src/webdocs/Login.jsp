<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.security.Principal" %>
<%@ page import="com.ecyrd.jspwiki.action.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAuthenticationLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.DuplicateUserException" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserProfile" %>
<%@ page import="com.ecyrd.jspwiki.workflow.DecisionRequiredException" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="java.util.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<%!
    Logger log = Logger.getLogger("JSPWiki");
%>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.LoginActionBean"/>
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

%><jsp:include page="LoginForm.jsp" />