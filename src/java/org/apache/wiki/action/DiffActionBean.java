package org.apache.wiki.action;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

/**
 * ActionBean that compares two versions of the same page. If "r1" is null, then
 * assume current version (= -1). If "r2" is null, then assume the previous
 * version (=current version-1)
 */
@UrlBinding( "/Diff.jsp" )
public class DiffActionBean extends AbstractPageActionBean
{
    private int m_r1 = WikiProvider.LATEST_VERSION;

    private int m_r2 = WikiProvider.LATEST_VERSION;

    private String m_diffProvider = "TraditionalDiffProvider";

    /**
     * Returns the newer version to compare.
     * 
     * @return the newer version
     */
    public int getR2()
    {
        return m_r2;
    }

    /**
     * Returns the old version to compare.
     * 
     * @return the old version
     */
    public int getR1()
    {
        return m_r1;
    }

    /**
     * Sets the old version to compare. If not supplied, defaults to the current
     * version.
     * 
     * @param r1 the old version
     */
    @Validate( required = false )
    public void setR1( int r1 )
    {
        m_r1 = r1;
    }

    /**
     * Sets the newer version to compare. If not supplied, defaults to the
     * previous version.
     * 
     * @param r2 the new version
     */
    @Validate( required = false )
    public void setR2( int r2 )
    {
        m_r2 = r2;
    }
    
    /**
     * Returns the DiffProvider used by this wiki.
     * @return the diff provider
     */
    @DontBind
    public String getDiffProvider()
    {
        return m_diffProvider;
    }
    
    /**
     * Event that diffs the current state of the edited page and forwards the
     * user to the template JSP {@code AttachmentInfo.jsp} if the current page is an
     * attachment, or {@code PageInfo.jsp} otherwise.
     * 
     * @return a forward resolution back to the preview page.
     */
    @DefaultHandler
    @HandlesEvent( "diff" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "diff" )
    public Resolution diff() throws ProviderException, PageNotFoundException
    {
        // Set R2 if default not set
        if( getR2() == WikiProvider.LATEST_VERSION )
        {
            int currentVersion = getPage().getVersion();
            if( currentVersion > 1 )
            {
                setR2( currentVersion - 1 );
            }
        }

        // Set the page history collection and DiffProvider
        WikiContext c = getContext();
        m_diffProvider = c.getEngine().getVariable( c, "jspwiki.diffProvider" );

        // Forward to display JSP
        WikiPage page = getPage();
        if ( page.isAttachment() )
        {
            return new ForwardResolution( "/templates/default/AttachmentInfo.jsp" );
        }
        return new ForwardResolution( "/templates/default/PageInfo.jsp" );
    }

}
