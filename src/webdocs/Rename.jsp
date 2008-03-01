<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<fmt:setBundle basename="CoreResources"/>
<%!
    Logger log = Logger.getLogger("JSPWiki");
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
	WikiContext wikiContext = wiki.createContext( request, WikiContext.RENAME );
    if(!wikiContext.hasAccess( response )) return;

    String renameFrom = wikiContext.getName();
    String renameTo = request.getParameter( "renameto");

    boolean changeReferences = false;

    ResourceBundle rb = wikiContext.getBundle("CoreResources");

    if (request.getParameter("references") != null)
    {
        changeReferences = true;
    }

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    log.info("Page rename request for page '"+renameFrom+ "' to new name '"+renameTo+"' from "+request.getRemoteAddr()+" by "+request.getRemoteUser() );

    WikiSession wikiSession = wikiContext.getWikiSession();
    try
    {
        if (renameTo.length() > 0)
        {
            String renamedTo = wiki.renamePage(wikiContext, renameFrom, renameTo, changeReferences);

            log.info("Page successfully renamed to '"+renamedTo+"'");

            response.sendRedirect( wikiContext.getURL( WikiContext.VIEW, renamedTo ) );
            return;
        }
        else
        {
            wikiSession.addMessage(rb.getString("rename.empty"));

            log.info("Page rename request failed because new page name was left blank");

%>
            <h3><fmt:message key="rename.error.title"/></h3>

            <dl>
               <dt><b><fmt:message key="rename.error.reason"/></b></dt>
               <dd>
                  <wiki:Messages div="error" />
               </dd>
            </dl>
<%
        }

    }
    catch (WikiException e)
    {
        if (e.getMessage().equals("Page exists"))
        {
            if (renameTo.equals( renameFrom ))
            {
                log.info("Page rename request failed because page names are identical");
                wikiSession.addMessage( rb.getString("rename.identical") );
            }
            else
            {
                log.info("Page rename request failed because new page name is already in use");
                Object[] args = { renameTo };
                wikiSession.addMessage(MessageFormat.format(rb.getString("rename.exists"),args));
            }
        }
        else
        {
            Object[] args = { e.toString() };
            wikiSession.addMessage( MessageFormat.format(rb.getString("rename.unknownerror"),args));
        }

%>
       <h3><fmt:message key="rename.error.title"/></h3>

       <dl>
          <dt><b><fmt:message key="rename.error.reason"/></b></dt>
          <dd>
             <wiki:Messages div="error" />
          </dd>
       </dl>
<%
    }
%>
