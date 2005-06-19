<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Category log = Category.getInstance("JSPWiki");
    WikiEngine wiki;
%>

<%
    String pagereq = wiki.safeGetParameter( request, "page" );

    if( pagereq == null )
    {
        pagereq = wiki.getFrontPage();
    }

    String renameFrom = pagereq;
    String renameTo = wiki.safeGetParameter( request, "renameto");

    
    boolean changeReferences = false;

    if (request.getParameter("references") != null) 
    {
        changeReferences = true;
    }

    WikiContext wikiContext = new WikiContext( wiki, wiki.getPage(pagereq) );
    wikiContext.setRequestContext( WikiContext.ERROR );
    wikiContext.setHttpRequest( request );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT, wikiContext, PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    
    log.info("Page rename request for page '"+pagereq+ "' to new name '"+renameTo+"' from "+request.getRemoteAddr()+" by "+request.getRemoteUser() );
    
    try 
    {
        if (renameTo.length() > 0)
        {
           String renamedTo = wiki.renamePage(renameFrom, renameTo, changeReferences);

           log.info("Page successfully renamed to '"+renamedTo+"'");
     
           //Encoding is necessary in case the page name contains special chars like german Umlaute
           renamedTo = wiki.encodeName(renamedTo);

           response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp?page="+renamedTo);
        }
        else
        {
            String msg = "New page name empty.<br/>\nClick <b>back</b> on your browser and fill in the new name.";
            pageContext.setAttribute( "message", msg, PageContext.REQUEST_SCOPE );

           log.info("Page rename request failed because new page name was left blank");
        
%>
            <h3>Unable to rename page</h3>

            <dl>
               <dt><b>Reason:</b></dt>
               <dd>
                  <%=pageContext.getAttribute("message",PageContext.REQUEST_SCOPE)%>
               </dd>      
            </dl>
<%
        }

    } 
    catch (WikiException e) 
    {
        String msg = null;
        

        if (e.getMessage().equals("Page exists")) 
        {
            if (renameTo.equals( renameFrom ))
            {
                log.info("Page rename request failed because page names are identical");

                msg = "Page names identical.<br/>\nClick <b>back</b> on your browser and change the new name.";
            }
            else
            {
                log.info("Page rename request failed because new page name is already in use");

                msg = "Page \"" + renameTo + "\" already exists.<br/>\nClick <b>back</b> on your browser and change the new name or delete the page \"" + renameTo + "\" first.";
            }
        } 
        else 
        {
            msg = "An Unknown error occurred (" + e.toString() + ")";
        }

        pageContext.setAttribute( "message", msg, PageContext.REQUEST_SCOPE );

%>
       <h3>Unable to rename page</h3>

       <dl>
          <dt><b>Reason:</b></dt>
          <dd>
             <%=pageContext.getAttribute("message",PageContext.REQUEST_SCOPE)%>
          </dd>      
       </dl>
<%
    }
%>
