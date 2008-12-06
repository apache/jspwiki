<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="org.apache.jspwiki.api.WikiException" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<%@ page import="com.ecyrd.jspwiki.util.TextUtil" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.RenameActionBean" event="rename" />
<%!
    Logger log = LoggerFactory.getLogger("JSPWiki");
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
	WikiContext wikiContext = wiki.createContext( request, WikiContext.RENAME );

    String renameFrom = wikiContext.getPage().getName();
    String renameTo = request.getParameter("renameto");

    boolean changeReferences = false;

    ResourceBundle rb = wikiContext.getBundle("CoreResources");

    if (request.getParameter("references") != null)
    {
        changeReferences = true;
    }

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
            wikiSession.addMessage("rename", rb.getString("rename.empty"));

            log.info("Page rename request failed because new page name was left blank");
        }
    }
    catch (WikiException e)
    {
        if (e.getMessage().equals("You cannot rename the page to itself"))
        {
            log.info("Page rename request failed because page names are identical");
            wikiSession.addMessage("rename", rb.getString("rename.identical") );
        }
        else if (e.getMessage().startsWith("Page already exists "))
        {
            log.info("Page rename request failed because new page name is already in use");
            Object[] args = { renameTo };
            wikiSession.addMessage("rename", MessageFormat.format(rb.getString("rename.exists"),args));
        }
        else
        {
            Object[] args = { e.toString() };
            wikiSession.addMessage("rename",  MessageFormat.format(rb.getString("rename.unknownerror"),args));
        }

    }

    pageContext.setAttribute( "renameto",
                      TextUtil.replaceEntities( renameTo ),
                      PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                    wikiContext.getTemplate(),
                                                    "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />