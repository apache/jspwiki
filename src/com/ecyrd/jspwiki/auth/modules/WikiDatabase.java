package com.ecyrd.jspwiki.auth.modules;

import com.ecyrd.jspwiki.auth.*;
import com.ecyrd.jspwiki.*;
import java.util.*;
import java.security.Principal;
import org.apache.log4j.Category;

import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.providers.ProviderException;

public class WikiDatabase
    implements UserDatabase
{
    private WikiEngine m_engine;

    static Category log = Category.getInstance( WikiDatabase.class );

    private HashMap m_principals = new HashMap();

    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;

        m_engine.addPageFilter( new SaveFilter(), 1000000 );

        initUserDatabase();
    }

    // This class must contain a large cache for user databases.

    // FIXME: Needs to cache this somehow; this is far too slow!

    public List getGroupsForPrincipal( Principal p )
        throws NoSuchPrincipalException
    {        
        List memberList = new ArrayList();

        log.debug("Finding groups for "+p.getName());

        for( Iterator i = m_principals.values().iterator(); i.hasNext(); )
        {
            Object o = i.next();

            if( o instanceof WikiGroup )
            {
                log.debug("  Checking group: "+o);
                if( ((WikiGroup)o).isMember( p ) )
                {
                    log.debug("     Is member");
                    memberList.add( o );
                }
            }
            else
            {
                log.debug("  Found strange object: "+o.getClass());
            }
        }
        
        return memberList;
    }

    /**
     *  List contains a bunch of Strings to denote members of this group.
     */
    private void updateGroup( String groupName, List memberList )
    {
        WikiGroup group = (WikiGroup)m_principals.get( groupName );

        if( group == null && memberList == null )
        {
            log.debug("No previous group, and no memberList.  Doing nothing.");
            return;
        }
     
        if( group == null && memberList != null )
        {
            log.debug("Adding new group: "+groupName);
            group = new WikiGroup();
            group.setName( groupName );
        }

        if( group != null && memberList == null )
        {
            log.debug("Detected removed group: "+groupName);

            m_principals.remove( groupName );

            return;
        }
        
        for( Iterator j = memberList.iterator(); j.hasNext(); )
        {
            Principal udp = new UndefinedPrincipal( (String)j.next() );
            
            group.addMember( udp );
            
            log.debug("** Added member: "+udp.getName());
        }

        m_principals.put( groupName, group );
    }

    private void initUserDatabase()
    {
        log.info("Initializing user database from wiki pages...");

        try
        {
            Collection allPages = m_engine.getPageManager().getAllPages();

            m_principals.clear();

            for( Iterator i = allPages.iterator(); i.hasNext(); )
            {
                WikiPage p = (WikiPage) i.next();

                List memberList = (List) p.getAttribute("_members");

                if( memberList != null )
                {
                    updateGroup( p.getName(), memberList );
                }
            }
        }
        catch( ProviderException e )
        {
            log.fatal("Cannot start database",e );
        }

        
    }

    public Principal getPrincipal( String name )
    {
        return null;
    }

    /**
     *  This special filter class is used to refresh the database
     *  after a page has been changed.
     */
    
    // FIXME: JSPWiki should really take care of itself that any metadata
    //        relevant to a page is refreshed.
    public class SaveFilter
        extends BasicPageFilter
    {
        public void postSave( WikiContext context, String content )
        {
            WikiPage p = context.getPage();

            log.debug("Skimming through page "+p.getName()+" to see if there are new users...");

            m_engine.textToHTML( context, content );

            List members = (List) p.getAttribute("_members");

            updateGroup( p.getName(), members );
        }
    }
}
