package com.ecyrd.jspwiki.providers;

import java.util.*;
import com.ecyrd.jspwiki.*;

public class VerySimpleProvider implements WikiPageProvider
{
    public String m_latestReq = null;
    public int    m_latestVers = -123989;

    public void initialize( Properties props )
    {
    }

    public String getProviderInfo()
    {
        return "Very Simple Provider.";
    }

    public void putPageText( WikiPage page, String text )
        throws ProviderException
    {
    }

    public boolean pageExists( String page )
    {
        return true;
    }

    public Collection findPages( QueryItem[] query )
    {
        return null;
    }

    public WikiPage getPageInfo( String page, int version )
    {
        m_latestReq  = page;
        m_latestVers = version;

        WikiPage p = new WikiPage( page );
        p.setVersion( 5 );
        p.setAuthor( "default-author" );
        return p;
    }

    public Collection getAllPages()
    {
        Vector v = new Vector();
        v.add( getPageInfo( "foo", 5 ) );
        return v;
    }

    public Collection getAllChangedSince( Date date )
    {
        return new Vector();
    }

    public int getPageCount()
    {
        return 1;
    }

    public Collection getVersionHistory( String page )
    {
        return new Vector();
    }

    public String getPageText( String page, int version )
    {
        m_latestReq  = page;
        m_latestVers = version;

        return "";
    }
}
