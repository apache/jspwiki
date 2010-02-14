package org.apache.wiki.ui.stripes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.OnwardResolution;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.TemplateManager;

/**
 * <p>
 * Forwards the user to a template resource prefixed by
 * <code>/templates/<var>template</var>/</code>. If the resource is not found in
 * the <code>/templates/<var>template</var>/</code> path, the forward will be to
 * <code>/templates/default</code> instead.
 * </p>
 */
public class TemplateResolution extends OnwardResolution<TemplateResolution>
{
    private static final Logger LOG = LoggerFactory.getLogger( TemplateResolution.class ); 
    
    /**
     * Constructs a new TemplateResolution
     * 
     * @param resource the path of the resource, relative to
     *            <code>/templates/<var>template</var>/</code>. {@code resource}
     *            should <em>not</em> start with a slash.
     */
    public TemplateResolution( String resource )
    {
        super( resource );
    }

    public void execute( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        // Figure out what the resolved template path should be
        WikiEngine engine = WikiEngine.getInstance( request.getSession().getServletContext(), null );
        TemplateManager templates = engine.getTemplateManager();
        String path = templates.getTemplateResources().get( getPath() );
        if( path == null )
        {
            path = "/templates/default/" + getPath();
        }
        setPath( path );

        // Get the URL and forward the user
        LOG.debug( "Forwarding user to resolved template resource: " + path );
        String url = this.getUrl( request.getLocale() );
        request.getRequestDispatcher( url ).forward( request, response );
    }

}
