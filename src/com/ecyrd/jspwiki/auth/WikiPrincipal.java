package com.ecyrd.jspwiki.auth;

import java.security.Principal;

/**
 *  This is the master class for all wiki UserProfiles and WikiGroups.
 */
public abstract class WikiPrincipal
    implements Principal
{
    private String m_name;

    public WikiPrincipal()
    {
    }

    public WikiPrincipal( String name )
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

    public void setName( String arg )
    {
        m_name = arg;
    }

}
