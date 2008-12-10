<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.action.*" %>
<%@ page import="com.ecyrd.jspwiki.util.*" %>
<%@ page import="org.apache.commons.lang.time.StopWatch" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.ViewActionBean" event="view" executeResolution="true" />
