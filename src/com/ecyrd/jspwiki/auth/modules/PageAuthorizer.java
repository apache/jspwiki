package com.ecyrd.jspwiki.auth.modules;

import java.util.Properties;
import org.apache.log4j.Category;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.acl.AccessControlList;
import com.ecyrd.jspwiki.auth.WikiAuthorizer;

/**
 *  This is a simple authorizer that just simply takes the permissions
 *  from a page.
 *
 *  @author Janne Jalkanen
 */
public class PageAuthorizer
    implements WikiAuthorizer
{
    private WikiEngine m_engine;

    static Category log = Category.getInstance( PageAuthorizer.class );

    // FIXME: Should be settable.

    public static final String DEFAULT_PERMISSIONPAGE = "DefaultPermissions";

    public void initialize( WikiEngine engine,
                            Properties properties )
    {
        m_engine = engine;
    }

    public AccessControlList getPermissions( WikiPage page )
    {
        AccessControlList acl = page.getAcl();

        //
        //  If the ACL has not yet been parsed, we'll do it here.
        //
        if( acl == null )
        {
            WikiContext context = new WikiContext( m_engine, page );
            String html = m_engine.getHTML( context, page );

            acl = page.getAcl();
        }

        log.debug( "page="+page.getName()+"\n"+acl );

        return acl;
    }

    public AccessControlList getDefaultPermissions()
    {
        WikiPage p = m_engine.getPage( DEFAULT_PERMISSIONPAGE );

        if( p != null )
        {
            return getPermissions( p );
        }

        return null;
    }

}
