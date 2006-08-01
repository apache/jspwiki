<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Logger log = Logger.getLogger("JSPWiki");
    WikiEngine wiki;
%>

<%
    // Create wiki context and check for authorization
	WikiContext wikiContext = wiki.createContext( request, WikiContext.RENAME );
    if(!wikiContext.hasAccess( response )) return;
	
    String renameFrom = wikiContext.getName();
    String renameTo = request.getParameter( "renameto");
    
    boolean changeReferences = false;

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
            wikiSession.addMessage( "New page name empty.<br/>\nClick <b>back</b> on your browser and fill in the new name.");

            log.info("Page rename request failed because new page name was left blank");
        
%>
            <h3>Unable to rename page</h3>

            <dl>
               <dt><b>Reason:</b></dt>
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
                wikiSession.addMessage( "Page names identical.<br/>\nClick <b>back</b> on your browser and change the new name." );
            }
            else
            {
                log.info("Page rename request failed because new page name is already in use");
                wikiSession.addMessage( "Page \"" + renameTo + "\" already exists.<br/>\nClick <b>back</b> on your browser and change the new name or delete the page \"" + renameTo + "\" first." );
            }
        } 
        else 
        {
            wikiSession.addMessage( "An Unknown error occurred (" + e.toString() + ")" );
        }

%>
       <h3>Unable to rename page</h3>

       <dl>
          <dt><b>Reason:</b></dt>
          <dd>
             <wiki:Messages div="error" />
          </dd>      
       </dl>
<%
    }
%>
