package org.apache.wiki.action;

import java.security.Permission;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.*;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;

/**
 * Generates "sister sites" lists with the names and URLs of all pages the user
 * as has access to, following the SisterSites standard.
 */
@UrlBinding( "/SisterSites.jsp" )
public class SisterSitesActionBean extends AbstractActionBean
{
    /**
     * Generates a {@link StreamingResolution} with the names and URLs of all
     * pages the user as has access to, following the SisterSites standard. This
     * event method respects ACLs on pages.
     * 
     * @see <a href="http://usemod.com/cgi-bin/mb.pl?SisterSitesImplementationGuide">
     *      Sister Sites Implementation Guide</a>
     * @return the streaming resolution
     */
    @DefaultHandler
    @DontBind
    @HandlesEvent( "sisterSites" )
    public Resolution sisterSites()
    {
        Resolution r = new StreamingResolution( "text/plain; charset=UTF-8" ) {
            @SuppressWarnings( "deprecation" )
            @Override
            protected void stream( HttpServletResponse response ) throws Exception
            {
                WikiEngine engine = getContext().getEngine();
                AuthorizationManager mgr = engine.getAuthorizationManager();
                WikiSession session = getContext().getWikiSession();
                Set<String> allPages = engine.getReferenceManager().findCreated();
                for( String page : allPages )
                {
                    if( page.indexOf( "/" ) == -1 )
                    {
                        Permission permission = PermissionFactory.getPagePermission( page, PagePermission.VIEW_ACTION );
                        if( mgr.checkPermission( session, permission ) )
                        {
                            String url = engine.getViewURL( page );
                            response.getWriter().write( url + " " + page + "\n" );
                        }
                    }
                }
            }
        };
        return r;
    }
}
