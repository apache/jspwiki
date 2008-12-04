package com.ecyrd.jspwiki.action;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

/**
 * <p>
 * Renames a wiki page. This ActionBean parses and extracts two request
 * parameters:
 * </p>
 * <h3>Request parameters</h3>
 * <ul>
 * <li><code>page</code> - the existing page name. Returned by
 * {@link #getPage()}
 * <li><code>renameTo</code> - the proposed new name for the page. This
 * parameter is required, and it is a validation error if not</li>
 * <li><code>changeReferences</code> - whether to change all referring pages'
 * references to this page also. If not supplied, this parameter defaults to
 * <code>false</code></li>
 * </ul>
 * <h3>Actions</h3>
 * <ul>
 * <li><code>rename</code> - executes the rename action, using the value of
 * field <code>renameTo</code> as the new name</li>
 * </ul>
 * <h3>Special validation</h3>
 * <p>
 * Before the <code>rename</code> handler executes, the validation method
 * {@link #validateBeforeRename(ValidationErrors)} checks to make sure that the
 * proposed page name (supplied by {@link #setRenameTo(String)} is not already
 * used by an existing wiki page.
 * </p>
 * 
 * @author Andrew Jaquith
 */
@UrlBinding( "/Rename.action" )
public class RenameActionBean extends AbstractActionBean
{
    private static final Logger log = LoggerFactory.getLogger( RenameActionBean.class );

    private boolean m_changeReferences = false;
    
    private WikiPage m_page = null;

    private String m_renameTo = null;

    /**
     * Returns the WikiPage; defaults to <code>null</code>.
     * @return the page
     */
    public WikiPage getPage()
    {
        return m_page;
    }
    
    /**
     * Returns the proposed new name for the page; defaults to <code>null</code>
     * if not set.
     * 
     * @return the proposed new page name
     */
    public String getRenameTo()
    {
        return m_renameTo;
    }

    /**
     * Returns <code>true</code> if the rename operation should also change
     * references to/from the page; <code>false</code> otherwise.
     * 
     * @return the result
     */
    public boolean isChangeReferences()
    {
        return m_changeReferences;
    }

    /**
     * Handler method that renames the current wiki page. If the rename
     * operation does not succeed for any reason, this method throws a
     * {@link com.ecyrd.jspwiki.WikiException}.
     * 
     * @return a redirection to the renamed wiki page
     * @throws WikiException if the page cannot be renamed
     */
    @HandlesEvent( "rename" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.RENAME_ACTION )
    @WikiRequestContext( "rename" )
    public Resolution rename() throws WikiException
    {
        WikiEngine engine = getContext().getEngine();
        String renameFrom = getContext().getPage().getName();
        HttpServletRequest request = getContext().getRequest();
        log.info( "Page rename request for page '" + renameFrom + "' to new name '" + m_renameTo + "' from "
                  + request.getRemoteAddr() + " by " + request.getRemoteUser() );
        String renamedTo = engine.renamePage( getContext(), renameFrom, m_renameTo, m_changeReferences );
        log.info( "Page successfully renamed to '" + renamedTo + "'" );
        RedirectResolution r = new RedirectResolution( ViewActionBean.class );
        r.addParameter( "page", renamedTo );
        return r;
    }

    /**
     * Tells JSPWiki to change references to/from the page when the
     * {@link #rename()} handler is executed. If not supplied, defaults to
     * <code>false</code>.
     * 
     * @param changeReferences the decision
     */
    @Validate( required = false )
    public void setChangeReferences( boolean changeReferences )
    {
        m_changeReferences = changeReferences;
    }

    /**
     * Sets the page.
     * @param page the wiki page.
     */
    @Validate( required = true )
    public void setPage( WikiPage page )
    {
        m_page = page;
        getContext().setPage( page );
    }
    
    /**
     * Sets the new name for the page, which will be set when the
     * {@link #rename()} handler is executed.
     * 
     * @param pageName the new page name
     */
    @Validate( required = true, minlength = 1, maxlength = 100, expression = "page.name != renameTo" )
    public void setRenameTo( String pageName )
    {
        m_renameTo = pageName;
    }

    /**
     * Before the {@link #rename()} handler method executes, this method
     * validates that the proposed new name does not collide with an existing
     * page.
     * 
     * @param errors the current set of validation errors for this ActionBean
     */
    @ValidationMethod( on = "rename" )
    public void validateBeforeRename( ValidationErrors errors )
    {
        if( getContext().getEngine().pageExists( m_renameTo ) )
        {
            errors.add( "renameTo", new SimpleError( "The page name '" + m_renameTo + "' already exists. Choose another." ) );
        }
    }

}
