package org.apache.wiki.action;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.*;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.SecurityVerifier;
import org.apache.wiki.util.TextUtil;

/**
 * Administration actions
 */
public class AdminActionBean extends AbstractActionBean
{
    private SecurityVerifier m_securityVerifier = null;
    
    /**
     * If the security configuration UI is enabled, this method initializes a new
     * {@link SecurityVerifier} and forwards the user to {@code /admin/SecurityConfig.jsp}.
     * @return the resolution
     */
    @DefaultHandler
    @HandlesEvent( "security" )
    public Resolution security()
    {
        WikiEngine engine = getContext().getEngine();
        if( TextUtil.isPositive(engine.getWikiProperties().getProperty("jspwiki-x.securityconfig.enable")) )
        {
            return new StreamingResolution( "text/html" ) {
                public void stream( HttpServletResponse response ) throws Exception
                {
                    PrintWriter out = response.getWriter();
                    out.print( "<html><body><p>Security config is disabled.</p></body></html>" );
                }
            };
        }
        else
        {
            m_securityVerifier = new SecurityVerifier( engine, getContext().getWikiSession() );
            return new ForwardResolution( "/admin/SecurityConfig.jsp" );
        }
    }
    
    /**
     * Returns the initialized SecurityVerifier.
     * @return the verifier
     */
    public SecurityVerifier getVerifier()
    {
        return m_securityVerifier;
    }
}
