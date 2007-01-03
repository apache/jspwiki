package com.ecyrd.jspwiki.workflow;

import java.security.Principal;

/**
 * System users asociated with workflow Task steps.
 * 
 * @author Andrew Jaquith
 */
public class SystemPrincipal implements Principal
{
    /** The JSPWiki system user */
    public static final Principal SYSTEM_USER = new SystemPrincipal("System User");

    private final String m_name;

    /**
     * Private constructor to prevent direct instantiation.
     * @param name the name of the Principal
     */
    private SystemPrincipal(String name)
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

}
